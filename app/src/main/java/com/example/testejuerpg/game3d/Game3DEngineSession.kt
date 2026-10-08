package com.example.testejuerpg.game3d

import java.util.Locale
import android.os.VibrationEffect
import com.example.testejuerpg.offline.EngineSaveData
import com.example.testejuerpg.offline.OfflineSaveCodec
import com.example.testejuerpg.offline.OfflineEmote
import com.example.testejuerpg.offline.OfflineEmoteCatalog
import com.example.testejuerpg.offline.OfflineProgressionRules
import com.example.testejuerpg.offline.OfflineMode
import com.example.testejuerpg.offline.LootRarity
import com.example.testejuerpg.offline.systems.OfflineBiomeCatalog
import com.example.testejuerpg.offline.systems.OfflineVisualCatalog
import com.example.testejuerpg.offline.systems.OfflineSquadCatalog
import com.example.testejuerpg.offline.systems.RooftopScreen

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

