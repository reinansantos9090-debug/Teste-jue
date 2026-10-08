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
fun Game3DEngine.equipWeapon(index: Int) {
        if (index in WEAPONS.indices) {
            weaponIndex = index
            save()
        }
    }
fun Game3DEngine.skillLabel(i: Int): String = when (i) {
        0 -> short(WEAPONS[weaponIndex].skill1)
        1 -> short(WEAPONS[weaponIndex].skill2)
        else -> short(WEAPONS[weaponIndex].skill3)
    }
internal fun Game3DEngine.short(value: String) = value.split(' ').first().uppercase(Locale.getDefault())
fun Game3DEngine.openInventory() {
        if (!isDefeated) {
            inventoryReturnScene = scene
            scene = SceneMode.INVENTORY
            save()
        }
    }
fun Game3DEngine.closeInventory() {
        if (scene == SceneMode.INVENTORY) scene = inventoryReturnScene
        save()
    }
fun Game3DEngine.startHunt() = launchExpedition(0)
fun Game3DEngine.storySnapshot() = storyCampaign.snapshot()
fun Game3DEngine.bossWeakPointOpen(): Boolean =
        bossActive && ((bossPatternTime % 5f) < 1.35f || (bossPatternTime % 5f) > 4.35f)
fun Game3DEngine.bossDisplayName(): String {
        if (activityMode == OfflineMode.STORY) {
            return OfflineBossCatalog.forStoryName(storyCampaign.currentChapter().targetId).name
        }
        val boss = enemies.firstOrNull { it.kind == EnemyKind.OVERLOAD_TITAN && !it.dead }
        return boss?.let { OfflineBossCatalog.forId(it.bossProfileId).name } ?: "Titã de Sobrecarga"
    }
fun Game3DEngine.advanceStoryBeat() {
        if (storyCampaign.advanceBeat()) {
            audioBus.play("story")
            invalidateUi()
        }
    }
fun Game3DEngine.launchStory() {
        if (!storyCampaign.startBattle()) return
        activityMode = OfflineMode.STORY
        beginBattle()
    }
internal fun Game3DEngine.storyObjectiveText(): String {
        val s = storyCampaign.snapshot()
        return "HISTÓRIA • " + s.chapterTitle + " • " + s.progress + "/" + s.required
    }
internal fun Game3DEngine.beginStoryBattleSetup() {
        val chapter = storyCampaign.currentChapter()
        activityTarget = chapter.targetCount
        objectiveText = "HISTÓRIA • " + chapter.title + " • " + chapter.synopsis
        when (chapter.objectiveKind) {
            "BOSS" -> spawnBoss()
            "ELITE" -> repeat(4) { spawnStoryEnemy(true) }
            "CORE", "ARENA" -> repeat(4) { spawnStoryEnemy(false) }
            else -> repeat(4) { spawnStoryEnemy(false) }
        }
        storyWaveTimer = 0f
        audioBus.play("story")
    }
internal fun Game3DEngine.spawnStoryEnemy(forceElite: Boolean) {
        val chapter = storyCampaign.currentChapter()
        val kind = when (chapter.targetId) {
            "slime" -> EnemyKind.AETHER_SLIME
            "stalker" -> EnemyKind.NEON_STALKER
            "golem" -> EnemyKind.SCRAP_GOLEM
            "boss" -> EnemyKind.OVERLOAD_TITAN
            else -> listOf(EnemyKind.AETHER_SLIME, EnemyKind.NEON_STALKER, EnemyKind.SCRAP_GOLEM)[random.nextInt(3)]
        }
        if (kind == EnemyKind.OVERLOAD_TITAN) {
            if (!bossSpawned) spawnBoss()
            return
        }
        spawnEnemyOfKind(kind, forceElite || chapter.objectiveKind == "ELITE")
    }
internal fun Game3DEngine.storyTargetId(e: EnemyEntity): String = when (e.kind) {
        EnemyKind.AETHER_SLIME -> "slime"
        EnemyKind.NEON_STALKER -> "stalker"
        EnemyKind.SCRAP_GOLEM -> "golem"
        EnemyKind.OVERLOAD_TITAN -> "boss"
        else -> "any"
    }
internal fun Game3DEngine.checkStoryObjective() {
        if (activityMode != OfflineMode.STORY || activityCompleted || !storyCampaign.objectiveComplete()) return
        val completed = storyCampaign.completeBattle() ?: return
        activityCompleted = true
        gold += completed.rewardGold
        hunterDirector.grantStoryReward(completed.rewardEnergy, completed.rewardGold)
        addXp(260f + completed.number * 12f)
        audioBus.play("level")
        objectiveText = if (completed.number >= 30) {
            "CAMPANHA CONCLUÍDA • AETHERIA FOI SALVA"
        } else {
            "CAPÍTULO " + completed.number + " CONCLUÍDO • +" + completed.rewardGold + " Ouro"
        }
        storyVictoryTimer = 3.8f
        historyArchive.record(
            "chapter_" + completed.number,
            completed.title,
            "story",
            completed.rewardGold,
            System.currentTimeMillis()
        )
        save()
    }
internal fun Game3DEngine.beginBattle() {
        scene = SceneMode.HUNT
        isDefeated = false
        bossActive = false
        bossSpawned = false
        bossDead = false
        bossPatternTime = 0f
        storyVictoryTimer = 0f
        activityCompleted = false
        huntKills = 0
        storyWaveTimer = 0f
        squadTimer = 0f
        mapRuntime = OfflineMapRuntime.forBiome(activeBiome().id)
        enemies.clear()
        projectiles.clear()
        drops.clear()
        particles.clear()
        aetherCores = 0
        player.x = 0f
        player.z = 4f
        hp = maxHp
        waveTimer = 1.5f

        when (activityMode) {
            OfflineMode.EXPEDITION -> {
                activityTarget = 12
                bossMaxHp = 900f
                repeat(3) { spawnEnemy() }
                objectiveText = "Tarefa: derrote 12 monstros"
            }
            OfflineMode.RIFT_BOSS -> {
                activityTarget = 1
                bossMaxHp = 900f
                spawnBoss()
                objectiveText = "RIFT • derrote o chefe"
            }
            OfflineMode.RIFT_ARENA -> {
                val difficulty = hunterDirector.rift()?.difficulty ?: 4
                activityTarget = 8 + difficulty
                repeat(4) { spawnEnemy() }
                objectiveText = "RIFT ARENA • sobreviva às ondas"
            }
            OfflineMode.VERSUS_SIM -> {
                activityTarget = 10
                repeat(4) { spawnEnemy(true) }
                objectiveText = "VERSUS • supere a arena offline"
            }
            OfflineMode.EVENT -> {
                activityTarget = 14
                repeat(3) { spawnEnemy() }
                objectiveText = "EVENTO • atividade especial"
            }
            OfflineMode.TRAINING -> {
                activityTarget = Int.MAX_VALUE
                objectiveText = "TREINO • ataque livre"
            }
            OfflineMode.STORY -> beginStoryBattleSetup()
        }
        save()
    }
fun Game3DEngine.launchExpedition(index: Int) {
        rooftopController.selectExpedition(index)
        rooftopController.enterSelectedExpedition()
        activityMode = OfflineMode.EXPEDITION
        beginBattle()
    }
fun Game3DEngine.launchRift(slot: Int) {
        rooftopController.selectRift(0, slot)
        rooftopController.enterSelectedRift()
        activityMode = hunterDirector.mode()
        beginBattle()
    }
fun Game3DEngine.launchVersus(bot: Int) {
        rooftopController.selectBot(bot)
        rooftopController.enterVersus()
        activityMode = OfflineMode.VERSUS_SIM
        beginBattle()
    }
fun Game3DEngine.returnToHub() {
        rooftopController.backToHome()
        scene = SceneMode.HUB
        isDefeated = false
        bossActive = false
        enemies.clear()
        projectiles.clear()
        drops.clear()
        particles.clear()
        player.x = 0f
        player.z = 5f
        hp = maxHp
        objectiveText = "Portal pronto • escolha uma caçada"
        save()
    }
fun Game3DEngine.openRooftopMenu() {
        if (scene == SceneMode.HUNT && !isDefeated) return
        scene = SceneMode.MENU
        rooftopController.navigate(RooftopScreen.HOME)
        invalidateUi()
    }
fun Game3DEngine.closeRooftopMenu() {
        if (scene == SceneMode.MENU) scene = SceneMode.HUB
        rooftopController.backToHome()
        save()
        invalidateUi()
    }
fun Game3DEngine.rooftopScreen(): RooftopScreen = rooftopController.screen
fun Game3DEngine.portalCards() = rooftopController.portalCards()
fun Game3DEngine.utilityCards() = rooftopController.utilityCards()
fun Game3DEngine.expeditions() = hunterDirector.buildExpeditions()
fun Game3DEngine.rifts() = hunterDirector.buildRiftSet(0)
fun Game3DEngine.bots() = rooftopController.bots()
fun Game3DEngine.styles() = rooftopController.styles()
fun Game3DEngine.cores() = rooftopController.cores()
fun Game3DEngine.dailyGoals() = rooftopController.dailyGoals()
fun Game3DEngine.events() = rooftopController.events()
fun Game3DEngine.recipes() = rooftopController.recipes()
fun Game3DEngine.hunterSummary() = hunterDirector.hunter()
fun Game3DEngine.selectedExpedition() = rooftopController.selectedExpedition
fun Game3DEngine.selectedRiftSlot() = rooftopController.selectedRiftSlot
fun Game3DEngine.selectedEvent() = rooftopController.selectedEvent
fun Game3DEngine.selectedBot() = rooftopController.selectedBot
fun Game3DEngine.activeStyleIndex() = rooftopController.selectedStyle
fun Game3DEngine.coreBonusLabel(core: com.example.testejuerpg.offline.OfflineUpgradeCore) = rooftopController.coreBonusLabel(core)
fun Game3DEngine.selectExpedition(index: Int) { rooftopController.selectExpedition(index) }
fun Game3DEngine.selectRiftSlot(index: Int) { rooftopController.selectRift(0, index) }
fun Game3DEngine.selectEvent(index: Int) { rooftopController.selectEvent(index) }
fun Game3DEngine.selectBot(index: Int) { rooftopController.selectBot(index) }
fun Game3DEngine.selectStyle(index: Int) { if (rooftopController.selectStyle(index)) save() }
fun Game3DEngine.unlockStyle(index: Int) { if (rooftopController.unlockStyle(index)) save() }
fun Game3DEngine.equipVisibleCore(index: Int) {
        val core = rooftopController.cores().getOrNull(index) ?: return
        if (rooftopController.equipCore("weapon_" + weaponIndex, core)) save()
    }
fun Game3DEngine.activateEvent() {
        if (rooftopController.activateEvent()) {
            activityMode = OfflineMode.EVENT
            beginBattle()
        }
    }
fun Game3DEngine.craft(index: Int) {
        val recipe = rooftopController.recipes().getOrNull(index) ?: return
        if (hunterDirector.craft(recipe.id)) save()
    }
fun Game3DEngine.menuTitle(): String = when (rooftopController.screen) {
        RooftopScreen.HOME -> "QG NO TERRAÇO"
        RooftopScreen.PORTAL -> "PORTAL"
        RooftopScreen.EXPEDITIONS -> "EXPEDIÇÕES"
        RooftopScreen.RIFTS -> "RIFTS"
        RooftopScreen.VERSUS -> "VERSUS"
        RooftopScreen.WARDROBE -> "ARMÁRIO"
        RooftopScreen.CORES -> "NÚCLEOS DE MELHORIA"
        RooftopScreen.DAILY -> "OBJETIVOS DIÁRIOS"
        RooftopScreen.EVENTS -> "EVENTOS"
        RooftopScreen.PROFILE -> "PERFIL DO CAÇADOR"
        RooftopScreen.CRAFTING -> "OFICINA"
        RooftopScreen.STORY -> "HISTÓRIA — AETHERIA: ECHOS"
    }
fun Game3DEngine.menuSubtitle(): String = when (rooftopController.screen) {
        RooftopScreen.HOME -> "Hub offline • portal, armário, progresso e oficina"
        RooftopScreen.PORTAL -> "Selecione o destino da próxima caça"
        RooftopScreen.EXPEDITIONS -> "Jobs, mundos e recompensas"
        RooftopScreen.RIFTS -> "Três fendas por conjunto • contra o tempo"
        RooftopScreen.VERSUS -> "Rivais simulados pela IA local"
        RooftopScreen.WARDROBE -> "Doze estilos salvos"
        RooftopScreen.CORES -> "Níveis 1–4 • equipar e fundir"
        RooftopScreen.DAILY -> "Energia e sequência diária"
        RooftopScreen.EVENTS -> "Modificadores especiais"
        RooftopScreen.PROFILE -> "Carreira e temporada"
        RooftopScreen.CRAFTING -> "Armas, gadgets, armaduras e rides"
        RooftopScreen.STORY -> "30 capítulos • diálogo persistente • chefes e decisões de combate"
    }
fun Game3DEngine.openRooftopPage(id: String?) {
        when (id) {
            "expeditions" -> rooftopController.navigate(RooftopScreen.EXPEDITIONS)
            "rifts" -> rooftopController.navigate(RooftopScreen.RIFTS)
            "versus" -> rooftopController.navigate(RooftopScreen.VERSUS)
            "events" -> rooftopController.navigate(RooftopScreen.EVENTS)
            "wardrobe" -> rooftopController.navigate(RooftopScreen.WARDROBE)
            "cores" -> rooftopController.navigate(RooftopScreen.CORES)
            "daily" -> rooftopController.navigate(RooftopScreen.DAILY)
            "crafting" -> rooftopController.navigate(RooftopScreen.CRAFTING)
            "profile" -> rooftopController.navigate(RooftopScreen.PROFILE)
            "story" -> rooftopController.navigate(RooftopScreen.STORY)
            else -> return
        }
        scene = SceneMode.MENU
        invalidateUi()
    }
internal fun Game3DEngine.invalidateUi() = onUiInvalidate?.invoke()
internal fun Game3DEngine.addXp(amount: Float) {
        val result = OfflineProgressionRules.addXp(level, xp, xpToNext, maxHp, amount)
        level = result.level
        xp = result.xp
        xpToNext = result.xpToNext
        maxHp = result.maxHealth
        if (result.levelsGained > 0) {
            hp = maxHp
            repeat(result.levelsGained) {
                vibrate(90)
                spawnBurst(player, 0.7f, floatArrayOf(0.95f, 0.8f, 0.3f))
            }
        }
    }
internal fun Game3DEngine.load() {
        prefs.getString("engine_state", null)?.let { encoded ->
            OfflineSaveCodec.decode(encoded)?.let { s ->
                level=s.level; xp=s.xp; xpToNext=s.xpToNext; maxHp=s.maxHp; hp=s.hp
                gold=s.gold; aetherCores=s.cores; kills=s.kills
                weaponIndex=s.weapon.coerceIn(0,WEAPONS.lastIndex); player.x=s.px; player.z=s.pz; playerName=s.name
                return
            }
        }
        level = prefs.getInt("level", 1)
        xp = prefs.getFloat("xp", 0f)
        xpToNext = prefs.getFloat("xp_next", 100f)
        maxHp = prefs.getFloat("max_hp", 150f)
        hp = prefs.getFloat("hp", maxHp)
        gold = prefs.getInt("gold", 50)
        aetherCores = prefs.getInt("cores", 0)
        kills = prefs.getInt("kills", 0)
        weaponIndex = prefs.getInt("weapon", 1).coerceIn(0, WEAPONS.lastIndex)
        player.x = prefs.getFloat("px", 0f)
        player.z = prefs.getFloat("pz", 5f)
        playerName = prefs.getString("name", "Caçador") ?: "Caçador"
    }
internal fun Game3DEngine.save() {
        val snapshot=EngineSaveData(level,xp,xpToNext,maxHp,hp,gold,aetherCores,kills,weaponIndex,player.x,player.z,playerName)
        prefs.edit()
            .putString("engine_state",OfflineSaveCodec.encode(snapshot))
            .putInt("level", level)
            .putFloat("xp", xp)
            .putFloat("xp_next", xpToNext)
            .putFloat("max_hp", maxHp)
            .putFloat("hp", hp)
            .putInt("gold", gold)
            .putInt("cores", aetherCores)
            .putInt("kills", kills)
            .putInt("weapon", weaponIndex)
            .putFloat("px", player.x)
            .putFloat("pz", player.z)
            .putString("name", playerName)
            .apply()
        hunterDirector.saveTo(prefs)
        prefs.edit().putInt("active_emote", activeEmote).apply()
        historyArchive.saveTo(prefs)
    }
internal fun Game3DEngine.vibrate(ms: Long) {
        try {
            if (vibration?.hasVibrator() == true) {
                vibration.vibrate(
                    VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (_: Throwable) {
        }
    }
fun Game3DEngine.activeEmote(): OfflineEmote = OfflineEmoteCatalog.all[activeEmote]
fun Game3DEngine.emoteActive(): Boolean = emoteTimer > 0f
fun Game3DEngine.cycleEmote() {
        activeEmote = (activeEmote + 1) % OfflineEmoteCatalog.all.size
        emoteTimer = 2.2f
        audioBus.play("story")
        save()
        invalidateUi()
    }
fun Game3DEngine.squadMembers() = OfflineSquadCatalog.all
fun Game3DEngine.squadOffsets(): List<V3> = memberOffsets.map { V3(it.x, it.y, it.z) }
fun Game3DEngine.historyEntries() = historyArchive.entries()
fun Game3DEngine.activeStyleVisual(): OfflineVisualCatalog.StyleVisual =
        OfflineVisualCatalog.forStyle(rooftopController.selectedStyle)
fun Game3DEngine.activeBiome(): com.example.testejuerpg.offline.systems.OfflineBiome {
        return OfflineBiomeCatalog.forWorld(
            hunterDirector.expedition()?.world
                ?: hunterDirector.rift()?.setId
                ?: storyCampaign.currentChapter().location
        )
    }
fun Game3DEngine.allowedFireflies(): Int = performanceGovernor.allowedFireflies()
fun Game3DEngine.mapObstacles(): List<MapObstacleView> = mapRuntime.obstacles.map {
        OfflineMapRuntimeObstacle(it.x, it.z, it.halfX, it.halfZ)
    }
fun Game3DEngine.renderProfile(): MobileRenderProfile = renderProfile
fun Game3DEngine.recordRenderFrame(frameMs: Float, drawCalls: Int) {
        performanceGovernor.sample(frameMs)
        performanceTelemetry.record(
            frameMs, drawCalls, snapshotEnemies().size,
            snapshotParticles().size, snapshotProjectiles().size
        )
    }
fun Game3DEngine.performanceSnapshot(): PerformanceSnapshot = performanceTelemetry.snapshot()

    private val enemyView = ArrayList<EnemyEntity>(24)
    private val projectileView = ArrayList<Projectile>(24)
    private val dropView = ArrayList<Drop>(48)
    private val particleView = ArrayList<Particle>(360)
fun Game3DEngine.snapshotEnemies(): List<EnemyEntity> {
        enemyView.clear()
        for (e in enemies) if (!e.dead) enemyView.add(e)
        return enemyView
    }
fun Game3DEngine.snapshotProjectiles(): List<Projectile> {
        projectileView.clear()
        projectileView.addAll(projectiles)
        return projectileView
    }
fun Game3DEngine.snapshotDrops(): List<Drop> {
        dropView.clear()
        dropView.addAll(drops)
        return dropView
    }
fun Game3DEngine.snapshotParticles(): List<Particle> {
        particleView.clear()
        particleView.addAll(particles)
        return particleView
    }
}

