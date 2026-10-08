package com.example.testejuerpg.game3d

import android.os.SystemClock
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Locale
import com.example.testejuerpg.offline.OfflineCombatRules
import com.example.testejuerpg.offline.OfflineLootRules
import com.example.testejuerpg.offline.OfflineProgressionRules
import com.example.testejuerpg.offline.OfflineSaveCodec
import com.example.testejuerpg.offline.EngineSaveData
import com.example.testejuerpg.offline.LootRarity
import com.example.testejuerpg.offline.OfflineEmote
import com.example.testejuerpg.offline.OfflineMode
import com.example.testejuerpg.offline.systems.OfflineBossCatalog
import com.example.testejuerpg.offline.systems.OfflineBiomeCatalog
import com.example.testejuerpg.offline.systems.OfflineVisualCatalog
import com.example.testejuerpg.offline.systems.OfflineSquadCatalog
import com.example.testejuerpg.offline.systems.OfflineEmoteCatalog
import com.example.testejuerpg.offline.systems.RooftopScreen

internal fun Game3DEngine.spawnEnemyAreaTelegraph(center: V3, radius: Float, damage: Float) {
        if (player.x-center.x <= radius && player.x-center.x >= -radius && player.z-center.z <= radius && player.z-center.z >= -radius) {
            takeDamage(damage)
        }
        spawnBurst(center, radius*0.12f, floatArrayOf(0.95f,0.34f,0.70f))
    }
internal fun Game3DEngine.dtSafe(): Float = (SystemClock.elapsedRealtime() - lastTick).coerceIn(1L,33L) / 1000f
internal fun Game3DEngine.spawnBossProjectile(boss: EnemyEntity) {
        val dx = player.x - boss.pos.x
        val dz = player.z - boss.pos.z
        val distance = max(0.001f, sqrt(dx * dx + dz * dz))
        projectiles += Projectile(
            V3(boss.pos.x, boss.pos.y, boss.pos.z),
            V3(dx / distance * 7.0f, 0f, dz / distance * 7.0f),
            OfflineBossCatalog.forId(boss.bossProfileId).projectileDamage,
            2.8f,
            false
        )
    }
internal fun Game3DEngine.spawnEnemy(elite: Boolean = false) {
        val choices = when {
            huntKills < 4 -> listOf(
                EnemyKind.AETHER_SLIME, EnemyKind.NEON_STALKER, EnemyKind.PRISM_MOTH,
                EnemyKind.THORN_LING
            )
            huntKills < 10 -> listOf(
                EnemyKind.AETHER_SLIME, EnemyKind.NEON_STALKER, EnemyKind.SCRAP_GOLEM,
                EnemyKind.SCRAP_DRONE, EnemyKind.PLASMA_EEL, EnemyKind.MAGNET_HARE,
                EnemyKind.THORN_LING, EnemyKind.MOSS_MENDER, EnemyKind.SAND_BOMBER,
                EnemyKind.PHASE_MOTH
            )
            else -> EnemyKind.entries.filter { it != EnemyKind.OVERLOAD_TITAN }
        }
        spawnEnemyOfKind(choices[random.nextInt(choices.size)], elite)
    }
internal fun Game3DEngine.spawnEnemyOfKind(kind: EnemyKind, elite: Boolean = false) {
        if (enemies.count { !it.dead } >= performanceGovernor.allowedEnemies()) return
        var sx = 0f
        var sz = 0f
        var placed = false
        repeat(8) {
            val angle = random.nextFloat() * 6.283f
            val distance = 8f + random.nextFloat() * 8f
            val tx = cos(angle) * distance
            val tz = sin(angle) * distance
            if (mapRuntime.canSpawn(tx, tz, kind.radius)) {
                sx = tx
                sz = tz
                placed = true
                return@repeat
            }
        }
        if (!placed) return
        val pos = V3(
            sx,
            if (kind == EnemyKind.SCRAP_GOLEM || kind == EnemyKind.OVERLOAD_TITAN) 1.0f else 0.65f,
            sz
        )
        enemies += EnemyEntity(
            nextEnemyId++,
            kind,
            pos,
            kind.hp * if (elite) 1.35f else 1f,
            elite = elite
        )
    }
internal fun Game3DEngine.spawnBoss() {
        bossSpawned = true
        bossActive = true
        val difficulty = hunterDirector.rift()?.difficulty ?: 1
        val profile = if (activityMode == OfflineMode.STORY) {
            OfflineBossCatalog.forStoryName(storyCampaign.currentChapter().targetId)
        } else {
            OfflineBossCatalog.all[((difficulty - 1).coerceIn(0, OfflineBossCatalog.all.lastIndex))]
        }
        bossMaxHp = profile.maxHp + if (activityMode == OfflineMode.RIFT_BOSS) difficulty * 120f else 0f
        bossHp = bossMaxHp
        enemies += EnemyEntity(
            nextEnemyId++,
            EnemyKind.OVERLOAD_TITAN,
            V3(0f, 1.55f, -9f),
            bossMaxHp,
            bossProfileId = profile.id
        )
        spawnBurst(V3(0f, 1f, -9f), 1f, rgbColor(profile.aura))
        vibrate(90)
    }
internal fun Game3DEngine.killEnemy(e: EnemyEntity) {
        if (e.dead) return
        e.dead = true
        kills += 1
        huntKills += 1
        addXp(if (e.kind == EnemyKind.OVERLOAD_TITAN) 300f else if (e.elite) 28f else 18f)
        rooftopController.recordBattleKill(weaponIndex, e.elite, e.kind == EnemyKind.OVERLOAD_TITAN)
        if (activityMode == OfflineMode.STORY) {
            if (storyCampaign.currentChapter().objectiveKind == "ARENA") {
                storyCampaign.recordArenaWave()
            } else {
                storyCampaign.recordKill(storyTargetId(e), e.elite, e.kind == EnemyKind.OVERLOAD_TITAN)
            }
            checkStoryObjective()
        }
        gold += if (e.kind == EnemyKind.OVERLOAD_TITAN) 250 else if (e.elite) 20 else 8

        if (e.kind == EnemyKind.ECHO_SPLITTER) {
            repeat(2) { spawnEnemyOfKind(EnemyKind.AETHER_SLIME, false) }
        }
        val loot = OfflineLootRules.roll(random.nextFloat(), random.nextFloat(), e.elite, e.kind == EnemyKind.OVERLOAD_TITAN)
        drops += Drop(V3(e.pos.x, 0.3f, e.pos.z), 0, loot.cores, loot.rarity.id)
        drops += Drop(V3(e.pos.x + 0.34f, 0.3f, e.pos.z), 1, loot.gold, loot.rarity.id)

        spawnBurst(
            e.pos,
            if (e.kind == EnemyKind.OVERLOAD_TITAN) 1.0f else 0.32f,
            if (e.kind == EnemyKind.OVERLOAD_TITAN) floatArrayOf(0.8f, 0.24f, 1f)
            else floatArrayOf(0.55f, 0.75f, 1f)
        )
        vibrate(28)
    }
internal fun Game3DEngine.takeDamage(amount: Float) {
        if (random.nextFloat() < hunterDirector.dodgeChance("weapon_" + weaponIndex)) {
            spawnBurst(player, 0.18f, floatArrayOf(0.35f, 0.9f, 1f))
            return
        }
        hp = max(0f, hp - amount)
        screenShake = 0.5f
        vibrate(20)
        if (hp <= 0f) {
            isDefeated = true
            save()
        }
    }
fun Game3DEngine.setMove(x: Float, y: Float) {
        moveX = x
        moveY = y
    }
@Synchronized fun Game3DEngine.primaryAction() {
        if (scene != SceneMode.HUNT || isDefeated || primaryTimer > 0f) return
        val weapon = WEAPONS[weaponIndex]
        val weaponId = "weapon_" + weaponIndex
        primaryTimer = weapon.cooldown * hunterDirector.cooldownMultiplier(weaponId) / hunterDirector.attackSpeedMultiplier(weaponId)
        val target = nearestEnemy(weapon.mainRange) ?: return

        val damageBonus = hunterDirector.damageMultiplier(weaponId) - 1f
        val damage = OfflineCombatRules.playerDamage(
            weapon.mainDamage,
            level,
            damageBonus,
            hunterDirector.criticalChance(weaponId),
            random.nextFloat()
        )
        if (weapon.archetype == 0 || weapon.archetype == 3) {
            meleeAttack(target, damage, if (weapon.archetype == 3) 2.8f else 2.4f)
        } else {
            fireProjectile(target.pos, damage, 0.9f + weapon.mainRange * 0.03f)
        }
    }
@Synchronized fun Game3DEngine.useSkill(index: Int) {
        if (scene != SceneMode.HUNT || isDefeated || index !in 0..2 || skillTimers[index] > 0f) return
        when (index) {
            0 -> skill1()
            1 -> skill2()
            2 -> skill3()
        }
    }
internal fun Game3DEngine.skill1() {
        skillTimers[0] = (if (WEAPONS[weaponIndex].archetype == 1) 4.0f else 3.0f) * hunterDirector.cooldownMultiplier("weapon_" + weaponIndex)
        if (WEAPONS[weaponIndex].archetype == 1) {
            repeat(3) {
                val t = nearestEnemy(11f)
                if (t != null) fireProjectile(t.pos, 46f * hunterDirector.damageMultiplier("weapon_" + weaponIndex), 1.15f)
            }
        } else {
            dash()
        }
    }
internal fun Game3DEngine.skill2() {
        skillTimers[1] = 6.0f * hunterDirector.cooldownMultiplier("weapon_" + weaponIndex)
        val radius = when (WEAPONS[weaponIndex].archetype) {
            0 -> 3.3f
            1 -> 2.8f
            2 -> 4.3f
            3 -> 3.4f
            else -> 3.7f
        }
        val damage = when (WEAPONS[weaponIndex].archetype) {
            0 -> 64f
            1 -> 58f
            2 -> 72f
            3 -> 86f
            else -> 62f
        }
        if (WEAPONS[weaponIndex].archetype == 1) dash()
        else areaAttack(radius, damage * hunterDirector.damageMultiplier("weapon_" + weaponIndex))
    }
internal fun Game3DEngine.skill3() {
        skillTimers[2] = 12.0f * hunterDirector.cooldownMultiplier("weapon_" + weaponIndex)
        val healFactor = if (WEAPONS[weaponIndex].archetype == 4) 0.48f else 0.36f
        hp = min(maxHp, hp + maxHp * healFactor * hunterDirector.healingMultiplier("weapon_" + weaponIndex))
        spawnBurst(player, 0.65f, floatArrayOf(0.4f, 1f, 0.7f))
        vibrate(34)
    }
internal fun Game3DEngine.dash() {
        val len = sqrt(moveX * moveX + moveY * moveY)
        val dx = if (len > 0.1f) moveX / len else 0f
        val dz = if (len > 0.1f) moveY / len else -1f
        val d = mapRuntime.resolvePlayer(player.x + dx * 4.2f, player.z + dz * 4.2f, 0.55f)
        player.x = d.first
        player.z = d.second
        screenShake = 0.22f
        spawnBurst(player, 0.22f, floatArrayOf(0.45f, 0.78f, 1f))
    }
internal fun Game3DEngine.meleeAttack(target: EnemyEntity, damage: Float, radius: Float) {
        areaAttack(radius, damage)
        if (target.dead) return
        target.hp -= damage * 0.45f
        target.hitFlash = 0.15f
        if (target.hp <= 0f) killEnemy(target)
        screenShake = 0.18f
        vibrate(20)
    }
internal fun Game3DEngine.areaAttack(radius: Float, damage: Float) {
        for (e in enemies) {
            if (e.dead) continue
            val dx = e.pos.x - player.x
            val dz = e.pos.z - player.z
            if (dx * dx + dz * dz <= radius * radius) {
                val finalDamage = if (e.kind == EnemyKind.OVERLOAD_TITAN && bossWeakPointOpen()) damage * 2f else damage
                e.hp -= finalDamage
                e.hitFlash = 0.16f
                if (weaponIndex == 1) e.poison = 5f
                spawnBurst(e.pos, 0.14f, floatArrayOf(0.55f, 0.82f, 1f))
                if (e.hp <= 0f) killEnemy(e)
            }
        }
        spawnBurst(player, radius * 0.18f, floatArrayOf(0.65f, 0.45f, 1f))
        screenShake = 0.25f
        vibrate(26)
    }
internal fun Game3DEngine.fireProjectile(targetPos: V3, damage: Float, speed: Float) {
        val dx = targetPos.x - player.x
        val dz = targetPos.z - player.z
        val distance = max(0.001f, sqrt(dx * dx + dz * dz))
        projectiles += Projectile(
            V3(player.x, 1.15f, player.z),
            V3(dx / distance * speed * 8.0f, 0f, dz / distance * speed * 8.0f),
            damage,
            1.9f,
            true
        )
    }
internal fun Game3DEngine.nearestEnemy(range: Float): EnemyEntity? {
        var best: EnemyEntity? = null
        var best2 = range * range
        for (e in enemies) {
            if (e.dead) continue
            val dx = e.pos.x - player.x
            val dz = e.pos.z - player.z
            val d2 = dx * dx + dz * dz
            if (d2 <= best2) {
                best2 = d2
                best = e
            }
        }
        return best
    }
internal fun Game3DEngine.spawnBurst(origin: V3, size: Float, color: FloatArray) {
        val room = (performanceGovernor.allowedParticles() - particles.size).coerceAtLeast(0)
        val count = (7 + size * 8).toInt().coerceAtMost(32).coerceAtMost(room)
        repeat(count) {
            val a = random.nextFloat() * 6.283f
            val speed = (0.7f + random.nextFloat() * 2.2f) * size
            particles += Particle(
                V3(origin.x, origin.y, origin.z),
                V3(cos(a) * speed, 0.7f + random.nextFloat() * speed, sin(a) * speed),
                0.45f,
                0.45f,
                0.08f + size * 0.08f,
                color
            )
        }
    }
