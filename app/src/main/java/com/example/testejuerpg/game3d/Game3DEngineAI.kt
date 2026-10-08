package com.example.testejuerpg.game3d

import android.os.SystemClock
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import com.example.testejuerpg.offline.OfflineMode
import com.example.testejuerpg.offline.systems.OfflineBossCatalog

internal fun Game3DEngine.updateEnemies(dt: Float) {
        for (e in enemies) {
            if (e.dead) continue
            e.hitFlash = max(0f, e.hitFlash - dt)
            if (e.kind == EnemyKind.OVERLOAD_TITAN) {
                val profile = OfflineBossCatalog.forId(e.bossProfileId)
                e.specialTimer = max(0f, e.specialTimer - dt)
                e.summonTimer = max(0f, e.summonTimer - dt)
                val ratio = e.hp / max(1f, bossMaxHp)
                val newPhase = when {
                    ratio <= 0.30f -> 3
                    ratio <= 0.62f -> 2
                    else -> 1
                }
                if (newPhase != e.bossPhase) {
                    e.bossPhase = newPhase
                    e.phase2 = newPhase >= 2
                    screenShake = 0.28f
                    spawnBurst(e.pos, 0.70f, rgbColor(profile.aura))
                    vibrate(60)
                }
                bossPatternTime += dt
                if (e.specialTimer <= 0f) {
                    executeBossPattern(e, profile)
                }
                if (e.summonTimer <= 0f && enemies.count { !it.dead } < min(12, performanceGovernor.allowedEnemies())) {
                    e.summonTimer = max(4.0f, profile.summonInterval * (if (e.bossPhase == 3) 0.58f else if (e.bossPhase == 2) 0.78f else 1f))
                    val summonCount = if (e.bossPhase == 3) 3 else if (e.bossPhase == 2) 2 else 1
                    repeat(summonCount) { spawnEnemy() }
                    spawnBurst(e.pos, 0.35f, rgbColor(profile.aura))
                }
            }
            if (e.poison > 0f) {
                e.poison -= dt
                e.poisonTick -= dt
                if (e.poisonTick <= 0f) {
                    e.poisonTick = 0.6f
                    e.hp -= 8f
                    spawnBurst(e.pos, 0.12f, floatArrayOf(0.45f, 1f, 0.35f))
                }
            }

            val dx = player.x - e.pos.x
            val dz = player.z - e.pos.z
            val dist = sqrt(dx * dx + dz * dz)
            e.attackTimer = max(0f, e.attackTimer - dt)
            e.specialTimer = max(0f, e.specialTimer - dt)

            when (e.kind) {
                EnemyKind.AETHER_SLIME -> {
                    val hop = kotlin.math.sin((SystemClock.elapsedRealtime() * 0.006f) + e.id) * 0.35f
                    if (dist > 2.4f + hop) {
                        moveEnemyToward(e, dx, dz, dist, dt, 0.90f + if (e.elite) 0.14f else 0f)
                    } else if (e.attackTimer <= 0f) {
                        e.attackTimer = 1.25f
                        takeDamage(e.kind.attack * (if (e.elite) 1.15f else 1f))
                        spawnBurst(e.pos, 0.20f, floatArrayOf(0.25f, 0.95f, 0.70f))
                    }
                    if (e.specialTimer <= 0f && dist < 6.5f) {
                        e.specialTimer = 4.8f
                        spawnEnemyAreaTelegraph(e.pos, 2.0f, e.kind.attack * 0.70f)
                    }
                }
                EnemyKind.NEON_STALKER -> {
                    if (e.specialTimer <= 0f && dist < 11f) {
                        e.specialTimer = 3.4f
                        val side = if (e.id % 2 == 0) 1f else -1f
                        val inv = 1f / max(0.001f, dist)
                        e.pos.x = player.x + dx * inv * 3.1f + (-dz * inv) * side * 2.0f
                        e.pos.z = player.z + dz * inv * 3.1f + (dx * inv) * side * 2.0f
                        spawnBurst(e.pos, 0.18f, floatArrayOf(1f, 0.18f, 0.62f))
                    } else if (dist > 1.7f) {
                        moveEnemyToward(e, dx, dz, dist, dt, 1.45f)
                    } else if (e.attackTimer <= 0f) {
                        e.attackTimer = 0.9f
                        takeDamage(e.kind.attack * 1.15f)
                    }
                }
                EnemyKind.SCRAP_GOLEM -> {
                    if (dist > 3.0f) {
                        moveEnemyToward(e, dx, dz, dist, dt, 0.58f)
                    } else if (e.attackTimer <= 0f) {
                        e.attackTimer = 2.1f
                        takeDamage(e.kind.attack)
                        spawnEnemyAreaTelegraph(e.pos, 2.8f, e.kind.attack * 0.55f)
                        screenShake = 0.12f
                    }
                }
                EnemyKind.PRISM_MOTH -> {
                    val orbit = -dz * 0.72f + dx * 0.22f
                    val orbitZ = dx * 0.72f + dz * 0.22f
                    val inv = 1f / max(0.001f, dist)
                    if (dist < 8f) {
                        e.pos.x += orbit * inv * e.kind.speed * dt
                        e.pos.z += orbitZ * inv * e.kind.speed * dt
                    } else {
                        moveEnemyToward(e, dx, dz, dist, dt, 0.82f)
                    }
                    if (e.attackTimer <= 0f && dist < 14f) {
                        e.attackTimer = 2.0f
                        spawnEnemyProjectile(e, 0.82f, 6.2f)
                    }
                }
                EnemyKind.SCRAP_DRONE -> {
                    val strafe = if (e.id % 2 == 0) 1f else -1f
                    if (dist < 7f) {
                        e.pos.x += (-dz) * strafe * 0.55f * dt
                        e.pos.z += dx * strafe * 0.55f * dt
                    } else if (dist > 12f) {
                        moveEnemyToward(e, dx, dz, dist, dt, 0.92f)
                    }
                    if (e.attackTimer <= 0f && dist < 18f) {
                        e.attackTimer = 1.65f
                        spawnEnemyProjectile(e, 0.95f, 7.1f)
                    }
                }
                EnemyKind.PLASMA_EEL -> {
                    val wave = kotlin.math.sin(SystemClock.elapsedRealtime() * 0.008f + e.id) * 1.4f
                    val steerX = dx - dz * 0.28f * wave
                    val steerZ = dz + dx * 0.28f * wave
                    if (dist > 2.1f) {
                        moveEnemyToward(e, steerX, steerZ, max(0.001f, sqrt(steerX * steerX + steerZ * steerZ)), dt, 1.15f)
                    } else if (e.attackTimer <= 0f) {
                        e.attackTimer = 1.2f
                        takeDamage(e.kind.attack)
                    }
                    if (e.specialTimer <= 0f && dist < 10f) {
                        e.specialTimer = 3.6f
                        spawnEnemyProjectile(e, 1.15f, 5.9f)
                    }
                }
                EnemyKind.VOID_BEETLE -> {
                    if (e.specialTimer <= 0f && dist < 12f) {
                        e.specialTimer = 4.5f
                        e.pos.x = player.x - dx * 0.48f
                        e.pos.z = player.z - dz * 0.48f
                        spawnBurst(e.pos, 0.22f, floatArrayOf(0.30f, 0.16f, 0.62f))
                    } else if (dist > 1.9f) {
                        moveEnemyToward(e, dx, dz, dist, dt, 0.98f)
                    } else if (e.attackTimer <= 0f) {
                        e.attackTimer = 1.0f
                        takeDamage(e.kind.attack * 1.25f)
                    }
                }
                EnemyKind.AURORA_WRAITH -> {
                    if (e.specialTimer <= 0f) {
                        e.specialTimer = 4.0f
                        val angle = (SystemClock.elapsedRealtime() * 0.0013f) + e.id
                        e.pos.x = player.x + cos(angle) * 5.2f
                        e.pos.z = player.z + sin(angle) * 5.2f
                        e.hp = min(e.kind.hp.toFloat(), e.hp + e.kind.hp * 0.12f)
                        spawnBurst(e.pos, 0.24f, floatArrayOf(0.50f, 0.92f, 1f))
                    }
                    if (e.attackTimer <= 0f && dist < 13f) {
                        e.attackTimer = 2.4f
                        spawnEnemyProjectile(e, 1.05f, 5.0f)
                    }
                }
                EnemyKind.MAGNET_HARE -> {
                    if (e.specialTimer <= 0f) {
                        e.specialTimer = 2.6f
                        val angle = if (e.id % 2 == 0) 0.75f else -0.75f
                        val nx = dx * cos(angle) - dz * sin(angle)
                        val nz = dx * sin(angle) + dz * cos(angle)
                        val inv = 1f / max(0.001f, dist)
                        e.pos.x = player.x + nx * inv * 4.3f
                        e.pos.z = player.z + nz * inv * 4.3f
                    } else if (dist > 2.2f) {
                        moveEnemyToward(e, dx, dz, dist, dt, 1.65f)
                    } else if (e.attackTimer <= 0f) {
                        e.attackTimer = 0.7f
                        takeDamage(e.kind.attack)
                    }
                }
                EnemyKind.CRYSTAL_BRUTE -> {
                    if (e.specialTimer <= 0f && dist < 10f) {
                        e.specialTimer = 5.2f
                        moveEnemyToward(e, dx, dz, dist, dt, 2.45f)
                        spawnEnemyAreaTelegraph(e.pos, 2.6f, e.kind.attack * 0.9f)
                    } else if (dist > 2.3f) {
                        moveEnemyToward(e, dx, dz, dist, dt, 0.65f)
                    } else if (e.attackTimer <= 0f) {
                        e.attackTimer = 2.0f
                        takeDamage(e.kind.attack * 1.10f)
                    }
                }
                EnemyKind.MEMORY_ECHO -> {
                    if (e.specialTimer <= 0f) {
                        e.specialTimer = 4.2f
                        spawnEnemyProjectile(e, 0.75f, 4.5f)
                        spawnEnemyProjectile(e, 0.75f, 6.0f)
                        spawnBurst(e.pos, 0.20f, floatArrayOf(0.75f, 0.60f, 1f))
                    } else if (dist > 2.4f) {
                        moveEnemyToward(e, dx, dz, dist, dt, 0.86f)
                    } else if (e.attackTimer <= 0f) {
                        e.attackTimer = 1.4f
                        takeDamage(e.kind.attack)
                    }
                }
                EnemyKind.PORTAL_LEECH -> {
                    if (dist > 2.0f) {
                        moveEnemyToward(e, dx, dz, dist, dt, 1.12f)
                    } else if (e.attackTimer <= 0f) {
                        e.attackTimer = 1.3f
                        takeDamage(e.kind.attack)
                        e.hp = min(e.kind.hp.toFloat(), e.hp + e.kind.hp * 0.10f)
                        spawnBurst(e.pos, 0.17f, floatArrayOf(0.60f, 0.22f, 0.90f))
                    }
                    if (e.specialTimer <= 0f && dist < 8f) {
                        e.specialTimer = 4.8f
                        val stolen = min(hp * 0.06f, 10f)
                        hp = max(1f, hp - stolen)
                        e.hp = min(e.kind.hp.toFloat(), e.hp + stolen * 1.4f)
                    }
                }
                EnemyKind.THORN_LING -> {
                    if (dist > 6f) {
                        moveEnemyToward(e, dx, dz, dist, dt, 0.72f)
                    }
                    if (e.attackTimer <= 0f && dist < 16f) {
                        e.attackTimer = 2.25f
                        spawnEnemyProjectile(e, 0.95f, 6.4f)
                        spawnEnemyProjectile(e, 0.95f, 6.0f)
                    }
                }
                EnemyKind.SAND_BOMBER -> {
                    if (dist < 3.2f && e.specialTimer <= 0f) {
                        e.specialTimer = 4f
                        takeDamage(e.kind.attack * 1.35f)
                        spawnBurst(player,0.45f,floatArrayOf(1f,0.42f,0.18f))
                    } else if (dist > 5.5f) {
                        moveEnemyToward(e,dx,dz,dist,dt,0.85f)
                    }
                    if (e.attackTimer <= 0f && dist < 18f) {
                        e.attackTimer = 2.6f
                        spawnEnemyProjectile(e,1.05f,5.4f)
                    }
                }
                EnemyKind.PHASE_MOTH -> {
                    val orbitX = -dz
                    val orbitZ = dx
                    if (e.specialTimer <= 0f && dist < 13f) {
                        e.specialTimer = 3.1f
                        val inv = 1f / max(0.001f, dist)
                        e.pos.x = player.x - dx * inv * 5.5f
                        e.pos.z = player.z - dz * inv * 5.5f
                    } else {
                        val inv = 1f / max(0.001f, sqrt(orbitX*orbitX+orbitZ*orbitZ))
                        e.pos.x += orbitX * inv * e.kind.speed * dt
                        e.pos.z += orbitZ * inv * e.kind.speed * dt
                    }
                    if (dist < 2.2f && e.attackTimer <= 0f) {
                        e.attackTimer=1.0f
                        takeDamage(e.kind.attack * 1.15f)
                    }
                }
                EnemyKind.MOSS_MENDER -> {
                    if (e.specialTimer <= 0f) {
                        e.specialTimer = 4.2f
                        enemies.asSequence()
                            .filter { it != e && !it.dead }
                            .filter { it.pos.distanceSquared(e.pos) < 32f }
                            .take(2)
                            .forEach { ally -> ally.hp = min(ally.kind.hp * 1.45f, ally.hp + ally.kind.hp * 0.16f) }
                        spawnBurst(e.pos, 0.18f, floatArrayOf(0.35f,1f,0.55f))
                    }
                    if (dist > 2.5f) moveEnemyToward(e, dx, dz, dist, dt, 0.84f)
                    else if (e.attackTimer <= 0f) {
                        e.attackTimer = 1.6f
                        takeDamage(e.kind.attack)
                    }
                }
                EnemyKind.CRYSTAL_SENTINEL -> {
                    if (e.specialTimer <= 0f) {
                        e.specialTimer = 2.9f
                        spawnEnemyProjectile(e,1.25f,6.8f)
                        spawnEnemyAreaTelegraph(e.pos,2.4f,e.kind.attack*0.28f)
                    }
                    if (dist > 6.0f) moveEnemyToward(e,dx,dz,dist,dt,0.54f)
                    else if (dist < 3.0f && e.attackTimer <= 0f) {
                        e.attackTimer=1.9f
                        takeDamage(e.kind.attack*0.82f)
                    }
                }
                EnemyKind.RIFT_ASSASSIN -> {
                    if (e.specialTimer <= 0f && dist < 14f) {
                        e.specialTimer=2.8f
                        val inv=1f/max(0.001f,dist)
                        e.pos.x=player.x+dx*inv*2.4f
                        e.pos.z=player.z+dz*inv*2.4f
                        screenShake=0.12f
                        spawnBurst(e.pos,0.20f,floatArrayOf(0.65f,0.35f,1f))
                    } else {
                        moveEnemyToward(e,dx,dz,dist,dt,1.35f)
                    }
                    if (dist < 2.0f && e.attackTimer <= 0f) {
                        e.attackTimer=0.8f
                        takeDamage(e.kind.attack * 1.3f)
                    }
                }
                EnemyKind.MAGNET_TURRET -> {
                    if (e.attackTimer <= 0f && dist < 20f) {
                        e.attackTimer = 2.20f
                        spawnEnemyProjectile(e, 1.25f, 7.0f)
                        spawnBurst(e.pos, 0.12f, floatArrayOf(1f,0.55f,0.30f))
                    }
                }
                EnemyKind.ECHO_SPLITTER -> {
                    if (e.specialTimer <= 0f && dist < 8f) {
                        e.specialTimer = 5.0f
                        spawnEnemyOfKind(EnemyKind.AETHER_SLIME, false)
                        spawnEnemyOfKind(EnemyKind.AETHER_SLIME, false)
                        spawnBurst(e.pos, 0.24f, floatArrayOf(0.65f,0.54f,1f))
                    }
                    if (dist > 1.8f) moveEnemyToward(e,dx,dz,dist,dt,1.18f)
                    else if (e.attackTimer <= 0f) {
                        e.attackTimer=1.25f
                        takeDamage(e.kind.attack*1.08f)
                    }
                }
                EnemyKind.OVERLOAD_TITAN -> {
                    if (dist > 2.7f && e.attackTimer <= 0f) {
                        e.attackTimer = if (e.bossPhase >= 3) 0.95f else 1.4f
                        takeDamage(e.kind.attack * (if (e.bossPhase >= 3) 1.35f else 1.0f))
                    }
                }
            }

            if (e.hp <= 0f) killEnemy(e)
        }
    }
internal fun Game3DEngine.updateProjectiles(dt: Float) {
        val remove = ArrayList<Projectile>()
        for (p in projectiles) {
            p.pos.add(p.vel.x * dt, p.vel.y * dt, p.vel.z * dt)
            p.life -= dt
            if (p.life <= 0f) {
                remove.add(p)
                continue
            }

            if (p.playerOwned) {
                for (e in enemies) {
                    if (e.dead) continue
                    val dx = p.pos.x - e.pos.x
                    val dz = p.pos.z - e.pos.z
                    val d2 = dx * dx + dz * dz
                    val hitR = e.kind.radius + 0.34f
                    if (d2 <= hitR * hitR) {
                        e.hp -= p.damage
                        e.hitFlash = 0.12f
                        spawnBurst(e.pos, 0.10f, floatArrayOf(0.55f, 0.82f, 1f))
                        remove.add(p)
                        if (e.hp <= 0f) killEnemy(e)
                        break
                    }
                }
            } else {
                val dx = p.pos.x - player.x
                val dz = p.pos.z - player.z
                if (dx * dx + dz * dz <= 0.60f * 0.60f) {
                    takeDamage(p.damage)
                    remove.add(p)
                    spawnBurst(player, 0.16f, floatArrayOf(1f, 0.25f, 0.45f))
                }
            }
        }
        projectiles.removeAll(remove)
    }
internal fun Game3DEngine.updateDrops(dt: Float) {
        for (d in drops) {
            d.life -= dt
            val dx = d.pos.x - player.x
            val dz = d.pos.z - player.z
            if (dx * dx + dz * dz < 1.69f) {
                if (d.type == 0) {
                    aetherCores += d.amount.coerceAtLeast(1)
                    hunterDirector.recordCore(d.amount.coerceAtLeast(1))
                    if (activityMode == OfflineMode.STORY) {
                        storyCampaign.recordCore()
                        checkStoryObjective()
                    }
                    objectiveText = "LOOT • Núcleos +" + d.amount + " • " + LootRarity.fromId(d.rarity).label
                } else {
                    gold += d.amount.coerceAtLeast(1)
                    objectiveText = "LOOT • Ouro +" + d.amount + " • " + LootRarity.fromId(d.rarity).label
                }
                spawnBurst(
                    d.pos,
                    0.14f,
                    if (d.type == 0) floatArrayOf(0.35f, 0.95f, 1f)
                    else floatArrayOf(1f, 0.82f, 0.25f)
                )
                d.life = -1f
            }
        }
        drops.removeAll { it.life <= 0f }
    }
internal fun Game3DEngine.updateParticles(dt: Float) {
        for (p in particles) {
            p.life -= dt
            p.pos.add(p.vel.x * dt, p.vel.y * dt, p.vel.z * dt)
            p.vel.y -= 2.8f * dt
        }
        particles.removeAll { it.life <= 0f }
    }
internal fun Game3DEngine.rgbColor(hex: Int): FloatArray = floatArrayOf(
        ((hex shr 16) and 255) / 255f,
        ((hex shr 8) and 255) / 255f,
        (hex and 255) / 255f
    )
internal fun Game3DEngine.updateSquad(dt: Float) {
        if (scene != SceneMode.HUNT || isDefeated || activityMode == OfflineMode.TRAINING) return
        squadTimer += dt
        if (squadTimer < 1.05f) return
        squadTimer = 0f
        OfflineSquadCatalog.all.forEachIndexed { index, member ->
            val phase = (SystemClock.elapsedRealtime() % 120000L) / 1000f * (0.9f + index * 0.12f) + index
            val target = nearestEnemy(11f)
            if (target != null) {
                val damage = 13f * member.damageMultiplier * (1f + level * 0.018f)
                target.hp -= damage
                target.hitFlash = 0.10f
                spawnBurst(target.pos, 0.08f, rgbColor(member.tint))
                if (target.hp <= 0f) killEnemy(target)
            }
            if (index == 1) hp = min(maxHp, hp + maxHp * 0.028f * member.healMultiplier)
            val point = mapRuntime.resolvePlayer(
                player.x + cos(phase) * (1.7f + index * 0.4f),
                player.z + sin(phase) * (1.7f + index * 0.4f),
                0.35f
            )
            memberOffsets[index].set(V3(point.first, 0.85f, point.second))
        }
    }
    internal val memberOffsets = Array(OfflineSquadCatalog.all.size) { V3(0f, 0.85f, 0f) }
internal fun Game3DEngine.updateWave(dt: Float) {
        waveTimer += dt

        when (activityMode) {
            OfflineMode.EXPEDITION -> {
                if (!bossActive && !bossSpawned && huntKills >= 12 && aetherCores >= 12) spawnBoss()
                if (!bossActive && waveTimer > 2.8f && enemies.count { !it.dead } < 7 && huntKills < 12) {
                    waveTimer = 0f
                    spawnEnemy()
                    if (huntKills % 4 == 3) spawnEnemy(true)
                }
            }
            OfflineMode.RIFT_BOSS -> {
                if (!bossActive && !bossSpawned) spawnBoss()
            }
            OfflineMode.RIFT_ARENA, OfflineMode.VERSUS_SIM, OfflineMode.EVENT -> {
                if (!activityCompleted && waveTimer > 2.4f && enemies.count { !it.dead } < 7) {
                    waveTimer = 0f
                    spawnEnemy(activityMode == OfflineMode.VERSUS_SIM && huntKills % 3 == 2)
                    if (huntKills % 5 == 4) spawnEnemy(true)
                }
                if (!activityCompleted && huntKills >= activityTarget) {
                    activityCompleted = true
                    if (activityMode == OfflineMode.RIFT_ARENA) hunterDirector.recordRiftComplete()
                    else if (activityMode == OfflineMode.VERSUS_SIM) hunterDirector.recordExpeditionComplete()
                    else hunterDirector.recordRiftComplete()
                    gold += if (activityMode == OfflineMode.EVENT) 180 else 140
                    addXp(220f)
                    objectiveText = when (activityMode) {
                        OfflineMode.RIFT_ARENA -> "RIFT CONCLUÍDO • +140 Ouro"
                        OfflineMode.VERSUS_SIM -> "VERSUS VENCIDO • +140 Ouro"
                        else -> "EVENTO CONCLUÍDO • +180 Ouro"
                    }
                    save()
                }
            }
            OfflineMode.TRAINING -> Unit
            OfflineMode.STORY -> {
                if (storyCampaign.objectiveComplete()) {
                    checkStoryObjective()
                } else {
                    storyWaveTimer += dt
                    if (storyWaveTimer >= 1.75f && enemies.count { !it.dead } < min(6, performanceGovernor.allowedEnemies())) {
                        storyWaveTimer = 0f
                        val chapter = storyCampaign.currentChapter()
                        when (chapter.objectiveKind) {
                            "ELITE" -> spawnStoryEnemy(true)
                            "CORE", "ARENA" -> spawnStoryEnemy(false)
                            "BOSS" -> if (!bossSpawned) spawnBoss()
                            else -> spawnStoryEnemy(false)
                        }
                    }
                }
            }
        }

        if (bossActive) {
            val boss = enemies.firstOrNull { it.kind == EnemyKind.OVERLOAD_TITAN && !it.dead }
            bossHp = boss?.hp ?: 0f
            if (boss == null && bossSpawned && !bossDead) {
                bossDead = true
                bossActive = false
                bossHp = 0f
                if (activityMode == OfflineMode.STORY) {
                    checkStoryObjective()
                    if (!activityCompleted) {
                        objectiveText = "CHEFE DERROTADO • preparando conclusão do capítulo"
                    }
                    spawnBurst(player, 1.2f, floatArrayOf(0.7f, 0.85f, 1f))
                    save()
                } else {
                    val isRift = activityMode == OfflineMode.RIFT_BOSS
                    gold += if (isRift) 360 else 250
                    addXp(if (isRift) 420f else 300f)
                    if (isRift) hunterDirector.recordRiftComplete() else hunterDirector.recordExpeditionComplete()
                    objectiveText = if (isRift) "RIFT CONCLUÍDO • +360 Ouro" else "EXPEDIÇÃO CONCLUÍDA • +250 Ouro"
                    spawnBurst(player, 1.2f, floatArrayOf(0.7f, 0.85f, 1f))
                    save()
                }
            }
            if (boss != null && random.nextFloat() < dt * 0.12f && enemies.count { !it.dead } < 9) {
                spawnEnemy(false)
            }
        }
    }
internal fun Game3DEngine.moveEnemyToward(e: EnemyEntity, dx: Float, dz: Float, dist: Float, dt: Float, speedMultiplier: Float) {
        val inv = 1f / max(0.001f, dist)
        e.pos.x += dx * inv * e.kind.speed * speedMultiplier * dt
        e.pos.z += dz * inv * e.kind.speed * speedMultiplier * dt
    }
internal fun Game3DEngine.spawnEnemyProjectile(enemy: EnemyEntity, damageMultiplier: Float, speed: Float) {
        val dx = player.x - enemy.pos.x
        val dz = player.z - enemy.pos.z
        val distance = max(0.001f, sqrt(dx * dx + dz * dz))
        projectiles += Projectile(
            V3(enemy.pos.x, enemy.pos.y + 0.35f, enemy.pos.z),
            V3(dx / distance * speed, 0f, dz / distance * speed),
            enemy.kind.attack * damageMultiplier,
            3.0f,
            false
        )
    }
internal fun Game3DEngine.executeBossPattern(boss: EnemyEntity, profile: OfflineBossDefinition) {
        val phase = boss.bossPhase
        val id = profile.id
        val pattern = when (id) {
            "overload_titan" -> listOf("fan","charge","nova","fan","summon")
            "prism_sentinel" -> listOf("cross","fan","nova","cross","teleport")
            "magnetic_colossus" -> listOf("pull","charge","ring","pull","fan")
            "inverted_king" -> listOf("teleport","fan","cross","nova","charge")
            "horizon_titan" -> listOf("beam","rain","charge","fan","beam")
            "aether_guardian" -> listOf("charge","ring","summon","fan","nova")
            "void_archon" -> listOf("teleport","fan","void","summon","nova")
            "storm_behemoth" -> listOf("rain","fan","ring","summon","charge")
            else -> listOf("fan","charge","nova")
        }[(floor(bossPatternTime / 2.1f).toInt()) % 5]
        val damage = profile.projectileDamage * (if (phase == 3) 1.45f else if (phase == 2) 1.18f else 1f)
        when (pattern) {
            "fan" -> {
                val count = if (phase == 3) 5 else if (phase == 2) 3 else 2
                repeat(count) { index ->
                    val angle = (index - (count - 1) / 2f) * 0.24f
                    spawnBossProjectileSpread(boss, damage, angle)
                }
                spawnBurst(boss.pos, 0.26f, rgbColor(profile.aura))
            }
            "cross" -> {
                repeat(4) { index -> spawnBossProjectileSpread(boss, damage * 0.86f, index * (Math.PI.toFloat() / 2f)) }
            }
            "beam" -> {
                spawnEnemyAreaTelegraph(
                    boss.pos,
                    3.8f + phase * 0.8f,
                    damage * 0.75f
                )
            }
            "rain" -> {
                repeat(if (phase == 3) 8 else 5) { index ->
                    val angle = (index * 1.37f + bossPatternTime) % 6.283f
                    val r = 3f + (index % 3) * 1.7f
                    val cx = player.x + cos(angle) * r
                    val cz = player.z + sin(angle) * r
                    spawnEnemyAreaTelegraph(V3(cx,0.3f,cz),1.5f + phase * 0.25f,damage * 0.42f)
                }
            }
            "ring" -> {
                spawnEnemyAreaTelegraph(boss.pos,3.5f + phase * 0.8f,damage * 0.65f)
                if (phase >= 2) spawnEnemyAreaTelegraph(boss.pos,6.2f + phase * 0.8f,damage * 0.35f)
            }
            "nova" -> spawnEnemyAreaTelegraph(player,4.2f + phase * 0.8f,damage * 0.78f)
            "charge" -> {
                val dx=player.x-boss.pos.x
                val dz=player.z-boss.pos.z
                val d=max(0.001f,sqrt(dx*dx+dz*dz))
                val distance=if(phase==3)7.0f else 5.0f
                boss.pos.x += dx/d*distance
                boss.pos.z += dz/d*distance
                spawnEnemyAreaTelegraph(boss.pos,2.3f + phase*0.35f,damage*0.72f)
                screenShake=0.18f
            }
            "teleport" -> {
                val angle=bossPatternTime*0.9f
                boss.pos.x=player.x+cos(angle)*6.4f
                boss.pos.z=player.z+sin(angle)*6.4f
                spawnEnemyAreaTelegraph(boss.pos,2.5f,damage*0.65f)
            }
            "pull" -> {
                val dx=player.x-boss.pos.x
                val dz=player.z-boss.pos.z
                val d=max(0.001f,sqrt(dx*dx+dz*dz))
                if(d>3f){ player.x -= dx/d*min(2.2f*dtSafe(),d-2f); player.z -= dz/d*min(2.2f*dtSafe(),d-2f) }
                spawnEnemyAreaTelegraph(boss.pos,5.4f,damage*0.35f)
            }
            "void" -> {
                spawnEnemyAreaTelegraph(player,5.2f,damage*0.60f)
                repeat(2){spawnBossProjectileSpread(boss,damage*0.90f,it*0.55f-0.28f)}
            }
            "summon" -> {
                repeat(if(phase==3)3 else 2){spawnEnemy()}
                spawnBurst(boss.pos,0.42f,rgbColor(profile.aura))
            }
        }
        boss.specialTimer=max(1.15f,profile.projectileInterval/(if(phase==3)1.65f else if(phase==2)1.25f else 1f))
    }
internal fun Game3DEngine.spawnBossProjectileSpread(boss: EnemyEntity, damage: Float, angle: Float) {
        val dx=player.x-boss.pos.x
        val dz=player.z-boss.pos.z
        val distance=max(0.001f,sqrt(dx*dx+dz*dz))
        val bx=dx/distance
        val bz=dz/distance
        val x=bx*cos(angle)-bz*sin(angle)
        val z=bx*sin(angle)+bz*cos(angle)
        projectiles += Projectile(
            V3(boss.pos.x,boss.pos.y,boss.pos.z),
            V3(x*7.2f,0f,z*7.2f),damage,3.1f,false
        )
    }


    internal val enemyView = ArrayList<EnemyEntity>(24)
    internal val projectileView = ArrayList<Projectile>(24)
