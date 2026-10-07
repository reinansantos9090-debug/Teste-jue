package com.example.testejuerpg.offline.systems

import com.example.testejuerpg.offline.OfflineCoreType
import com.example.testejuerpg.offline.OfflineDailyGoal
import com.example.testejuerpg.offline.OfflineEventDefinition
import com.example.testejuerpg.offline.OfflineGameConstants
import com.example.testejuerpg.offline.OfflineMode
import com.example.testejuerpg.offline.OfflineQuest
import com.example.testejuerpg.offline.OfflineUpgradeCore
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class OfflineHunterProfile(
    val name: String = "Caçador",
    val careerLevel: Int = 1,
    val seasonLevel: Int = 1,
    val chaosEnergy: Int = 0,
    val eliteMerits: Int = 0,
    val eliteStars: Int = 0,
    val perkTokens: Int = 0,
    val merchCredits: Int = 0,
    val eliteMerchCredits: Int = 0,
    val dailyStreak: Int = 0,
    val kills: Int = 0,
    val bosses: Int = 0,
    val rifts: Int = 0,
    val expeditions: Int = 0
)

data class OfflineExpeditionJob(
    val id: String,
    val title: String,
    val type: String,
    val target: String,
    val required: Int,
    val rewardEnergy: Int,
    val rewardGold: Int,
    val difficulty: Int,
    val progress: Int = 0
) {
    val complete: Boolean
        get() = progress >= required
}

data class OfflineExpedition(
    val id: String,
    val name: String,
    val world: String,
    val difficulty: Int,
    val duration: Int,
    val jobs: List<OfflineExpeditionJob>
)

data class OfflineRiftGoal(
    val id: String,
    val type: String,
    val title: String,
    val target: String,
    val amount: Int
)

data class OfflineRift(
    val id: String,
    val setId: String,
    val name: String,
    val difficulty: Int,
    val duration: Int,
    val goal: OfflineRiftGoal
)

data class OfflineVersusBot(
    val id: String,
    val name: String,
    val difficulty: Int,
    val health: Float,
    val attack: Float,
    val speed: Float,
    val score: Int
)

data class OfflineStyle(
    val slot: Int,
    val outfit: String,
    val headwear: String,
    val hair: String,
    val face: String,
    val ride: String,
    val locked: Boolean
)

data class OfflinePetDefinition(
    val id: String,
    val name: String,
    val damage: Float,
    val healing: Float,
    val speed: Float,
    val unlocked: Boolean
)

data class OfflineCraftDefinition(
    val id: String,
    val name: String,
    val category: String,
    val ingredients: Map<String, Int>,
    val result: String,
    val amount: Int,
    val unlockLevel: Int
)

class OfflineHunterDirector(seed: Int = 2025) {
    private val random = Random(seed)
    private var hunter = OfflineHunterProfile()
    private var mode = OfflineMode.EXPEDITION
    private var activeExpedition: OfflineExpedition? = null
    private var activeRift: OfflineRift? = null
    private var activeRide = "ride_basic"
    private var activeStyle = 0
    private var rideCooldown = 0f
    private var expeditionClock = 0f
    private var riftClock = 0f
    private val cores = mutableListOf<OfflineUpgradeCore>()
    private val equipped = linkedMapOf<String, OfflineUpgradeCore>()
    private val materials = linkedMapOf<String, Int>()
    private val styles = MutableList(OfflineGameConstants.MAX_STYLES) { slot ->
        OfflineStyle(slot, "Traje " + slot, "Cabeça " + slot, "Cabelo " + slot, "Rosto " + slot, "Ride " + slot, slot != 0)
    }
    private val pets = mutableListOf(
        OfflinePetDefinition("spark", "Faísca", 1.05f, 1.05f, 1.08f, true),
        OfflinePetDefinition("scrappy", "Sucata", 1.12f, 1.00f, 1.02f, false),
        OfflinePetDefinition("shade", "Vulto", 1.18f, 1.08f, 1.14f, false),
        OfflinePetDefinition("aetherling", "Aetherling", 1.10f, 1.18f, 1.12f, false)
    )
    private val dailyGoals = mutableListOf<OfflineDailyGoal>()
    private val events = mutableListOf<OfflineEventDefinition>()
    private val recipes = mutableListOf<OfflineCraftDefinition>()
    private val quests = OfflineHunterObjectives.base()
    private val dailyProgressMap = linkedMapOf<String, Int>()

    init {
        seedMaterials()
        seedCores()
        seedRecipes()
        refreshDaily(seed)
    }

    fun hunter(): OfflineHunterProfile = hunter
    fun mode(): OfflineMode = mode
    fun dailyGoals(): List<OfflineDailyGoal> = dailyGoals.toList()
    fun events(): List<OfflineEventDefinition> = events.toList()
    fun quests(): List<OfflineQuest> = quests.toList()
    fun styles(): List<OfflineStyle> = styles.toList()
    fun pets(): List<OfflinePetDefinition> = pets.toList()
    fun recipes(): List<OfflineCraftDefinition> = recipes.toList()
    fun cores(): List<OfflineUpgradeCore> = cores.toList()
    fun equippedCores(): Map<String, OfflineUpgradeCore> = equipped.toMap()
    fun materials(): Map<String, Int> = materials.toMap()
    fun expedition(): OfflineExpedition? = activeExpedition
    fun rift(): OfflineRift? = activeRift
    fun expeditionRemaining(): Int = expeditionClock.toInt()
    fun riftRemaining(): Int = riftClock.toInt()

    fun refreshDaily(daySeed: Int) {
        dailyGoals.clear()
        events.clear()
        dailyProgressMap.clear()
        dailyGoals += OfflineDailyGoal("daily_hunt", "Caçada Relâmpago", "Derrote monstros.", 10, 120)
        dailyGoals += OfflineDailyGoal("daily_cores", "Coletor de Núcleos", "Colete núcleos.", 8, 140)
        dailyGoals += OfflineDailyGoal("daily_rift", "Especialista em Rift", "Conclua um Rift.", 1, 220)
        dailyGoals += OfflineDailyGoal("daily_boss", "Caçador de Chefes", "Derrote um chefe.", 1, 260)
        dailyGoals += OfflineDailyGoal("daily_styles", "Arsenal Variado", "Complete caçadas com estilos diferentes.", 3, 160)
        val variant = (daySeed and 7)
        events += OfflineEventDefinition("double_" + variant, "Pulso Duplo", 180, "double_energy", 2f)
        events += OfflineEventDefinition("invasion_" + variant, "Invasão", 240, "portal_invasion", 1.5f)
        events += OfflineEventDefinition("escort_" + variant, "Escolta", 210, "escort", 1.35f)
        events += OfflineEventDefinition("transformation_" + variant, "Transformação", 150, "transformation", 1.8f)
    }

    fun buildExpeditions(): List<OfflineExpedition> {
        val worlds = listOf(
            "Jardins do Prisma",
            "Ruínas de Sucata",
            "Floresta Neon",
            "Pântano de Plasma",
            "Cânion do Vórtice",
            "Domo da Aurora",
            "Vale de Cristal",
            "Planície Magnética"
        )
        return worlds.mapIndexed { index, world ->
            val difficulty = min(10, 1 + hunter.careerLevel / 2 + index / 3)
            val jobs = listOf(
                OfflineExpeditionJob("e" + index + "_kill", "Limpar a área", "kill", "any", 8 + index, 60 + index * 8, 20 + index * 3, difficulty),
                OfflineExpeditionJob("e" + index + "_core", "Recuperar núcleos", "core", "core", 4 + index % 4, 70 + index * 9, 24 + index * 3, difficulty),
                OfflineExpeditionJob("e" + index + "_elite", "Eliminar elites", "elite", "elite", 2 + index % 3, 120 + index * 12, 40 + index * 4, min(10, difficulty + 2)),
                OfflineExpeditionJob("e" + index + "_event", "Responder ao evento", "event", "event", 1, 100 + index * 10, 35 + index * 5, min(10, difficulty + 1))
            )
            OfflineExpedition(
                "expedition_" + index,
                "Expedição " + (index + 1),
                world,
                difficulty,
                max(300, OfflineGameConstants.EXPEDITION_TIME_SECONDS - index * 45),
                jobs
            )
        }
    }

    fun enterExpedition(index: Int): OfflineExpedition {
        val list = buildExpeditions()
        val source = list[index.coerceIn(0, list.lastIndex)]
        activeExpedition = source.copy(jobs = source.jobs.map { it.copy(progress = 0) })
        activeRift = null
        mode = OfflineMode.EXPEDITION
        expeditionClock = source.duration.toFloat()
        return activeExpedition!!
    }

    fun buildRiftSet(setIndex: Int): List<OfflineRift> {
        val safeSet = setIndex.coerceIn(0, 5)
        return (0..2).map { slot ->
            val difficulty = min(10, 3 + safeSet + slot + hunter.careerLevel / 3)
            val goal = if (slot == 1) {
                OfflineRiftGoal("goal_arena_" + safeSet + "_" + slot, "monster_arena", "Sobreviva às ondas", "wave", 5 + difficulty)
            } else {
                OfflineRiftGoal("goal_boss_" + safeSet + "_" + slot, "boss_fight", "Derrote o chefe", "rift_boss_" + safeSet, 1)
            }
            OfflineRift(
                "rift_" + safeSet + "_" + slot,
                "rift_set_" + safeSet,
                "Rift " + (safeSet + 1) + "-" + (slot + 1),
                difficulty,
                max(120, OfflineGameConstants.RIFT_TIME_SECONDS - difficulty * 12),
                goal
            )
        }
    }

    fun enterRift(setIndex: Int, slot: Int): OfflineRift {
        val list = buildRiftSet(setIndex)
        val chosen = list[slot.coerceIn(0, list.lastIndex)]
        activeRift = chosen
        activeExpedition = null
        mode = if (chosen.goal.type == "monster_arena") OfflineMode.RIFT_ARENA else OfflineMode.RIFT_BOSS
        riftClock = chosen.duration.toFloat()
        return chosen
    }

    fun enterVersus() {
        mode = OfflineMode.VERSUS_SIM
        activeExpedition = null
        activeRift = null
    }

    fun enterTraining() {
        mode = OfflineMode.TRAINING
        activeExpedition = null
        activeRift = null
    }

    fun bots(): List<OfflineVersusBot> {
        val names = listOf("Rook", "Mira", "Volt", "Iris", "Dax", "Nova", "Kiro", "Sable")
        return names.take(4).mapIndexed { index, name ->
            val difficulty = min(10, 2 + hunter.careerLevel / 2 + index)
            OfflineVersusBot(
                "bot_" + index,
                name,
                difficulty,
                110f + difficulty * 18f,
                12f + difficulty * 2.5f,
                2.5f + difficulty * 0.16f,
                100 + difficulty * 35
            )
        }
    }

    fun recordKill(id: String, elite: Boolean = false, boss: Boolean = false) {
        val energy = rewardEnergy(elite, boss)
        hunter = hunter.copy(
            kills = hunter.kills + 1,
            bosses = hunter.bosses + if (boss) 1 else 0,
            chaosEnergy = hunter.chaosEnergy + energy
        )
        dailyProgressMap["daily_hunt"] = (dailyProgressMap["daily_hunt"] ?: 0) + 1
        if (boss) dailyProgressMap["daily_boss"] = (dailyProgressMap["daily_boss"] ?: 0) + 1
        advanceJob("kill", id, 1)
        if (elite) advanceJob("elite", "elite", 1)
        if (boss) advanceJob("boss", id, 1)
        if (random.nextFloat() < if (elite) 0.75f else 0.35f) addCoreFromDrop(id)
    }

    fun recordCore(count: Int = 1) {
        val amount = max(0, count)
        dailyProgressMap["daily_cores"] = min(100, (dailyProgressMap["daily_cores"] ?: 0) + amount)
        repeat(amount) { addCoreFromDrop("pickup") }
        advanceJob("core", "core", amount)
    }

    fun recordEvent(eventId: String) {
        advanceJob("event", eventId, 1)
    }

    fun recordRiftComplete() {
        hunter = hunter.copy(rifts = hunter.rifts + 1, chaosEnergy = hunter.chaosEnergy + 100)
        dailyProgressMap["daily_rift"] = (dailyProgressMap["daily_rift"] ?: 0) + 1
    }

    fun recordExpeditionComplete() {
        hunter = hunter.copy(expeditions = hunter.expeditions + 1, chaosEnergy = hunter.chaosEnergy + 80)
    }

    fun tick(deltaSeconds: Float) {
        if (deltaSeconds <= 0f) return
        rideCooldown = max(0f, rideCooldown - deltaSeconds)
        expeditionClock = max(0f, expeditionClock - deltaSeconds)
        riftClock = max(0f, riftClock - deltaSeconds)
    }

    fun rideReady(): Boolean = rideCooldown <= 0f

    fun summonRide(): Boolean {
        if (!rideReady()) return false
        activeRide = styles[activeStyle].ride
        rideCooldown = 4f
        return true
    }

    fun currentRide(): String = activeRide

    fun selectStyle(slot: Int): Boolean {
        if (slot !in styles.indices) return false
        if (styles[slot].locked) return false
        activeStyle = slot
        activeRide = styles[slot].ride
        return true
    }

    fun unlockStyle(slot: Int, price: Int): Boolean {
        if (slot !in styles.indices || !styles[slot].locked) return false
        if (hunter.chaosEnergy < price) return false
        hunter = hunter.copy(chaosEnergy = hunter.chaosEnergy - price)
        styles[slot] = styles[slot].copy(locked = false)
        return true
    }

    fun craft(recipeId: String): Boolean {
        val recipe = recipes.firstOrNull { it.id == recipeId } ?: return false
        if (hunter.careerLevel < recipe.unlockLevel) return false
        if (!recipe.ingredients.all { (materials[it.key] ?: 0) >= it.value }) return false
        recipe.ingredients.forEach { (id, value) ->
            materials[id] = max(0, (materials[id] ?: 0) - value)
        }
        materials[recipe.result] = (materials[recipe.result] ?: 0) + recipe.amount
        return true
    }

    fun fuse(firstId: String, secondId: String): OfflineUpgradeCore? {
        val first = cores.firstOrNull { it.id == firstId } ?: return null
        val second = cores.firstOrNull { it.id == secondId } ?: return null
        if (first.id == second.id || first.type != second.type || first.tier != second.tier) return null
        cores.remove(first)
        cores.remove(second)
        val tier = min(4, first.tier + 1)
        val result = OfflineUpgradeCore(
            "fused_" + first.id + "_" + second.id,
            first.type,
            tier,
            first.charge + second.charge,
            min(1.5f, first.bonus + second.bonus * 0.7f)
        )
        cores += result
        return result
    }

    fun equipCore(weaponId: String, coreId: String): Boolean {
        val core = cores.firstOrNull { it.id == coreId } ?: return false
        val old = equipped[weaponId]
        if (old != null && old.tier > core.tier) return false
        if (old != null) cores += old
        cores.remove(core)
        equipped[weaponId] = core
        return true
    }

    fun damageMultiplier(weaponId: String): Float {
        val core = equipped[weaponId] ?: return 1f
        return if (core.type == OfflineCoreType.DAMAGE) 1f + core.bonus else 1f
    }

    fun attackSpeedMultiplier(weaponId: String): Float {
        val core = equipped[weaponId] ?: return 1f
        return if (core.type == OfflineCoreType.ATTACK_SPEED) 1f + core.bonus else 1f
    }

    fun cooldownMultiplier(weaponId: String): Float {
        val core = equipped[weaponId] ?: return 1f
        return if (core.type == OfflineCoreType.COOLDOWN) max(0.55f, 1f - core.bonus) else 1f
    }

    fun healingMultiplier(weaponId: String): Float {
        val core = equipped[weaponId] ?: return 1f
        return if (core.type == OfflineCoreType.HEALING) 1f + core.bonus else 1f
    }

    fun dodgeChance(weaponId: String): Float {
        val core = equipped[weaponId] ?: return 0f
        return if (core.type == OfflineCoreType.DODGE) min(0.6f, core.bonus) else 0f
    }

    fun maxHealthMultiplier(weaponId: String): Float {
        val core = equipped[weaponId] ?: return 1f
        return if (core.type == OfflineCoreType.MAX_HEALTH) 1f + core.bonus else 1f
    }

    private fun rewardEnergy(elite: Boolean, boss: Boolean): Int {
        var value = 4
        if (elite) value += 8
        if (boss) value += 45
        val event = events.firstOrNull { it.modifier == "double_energy" }
        if (event != null && activeExpedition != null) value *= 2
        return value
    }

    private fun advanceJob(type: String, target: String, amount: Int) {
        val current = activeExpedition ?: return
        val jobs = current.jobs.map { job ->
            if (job.type == type && !job.complete && (job.target == "any" || job.target == target)) {
                job.copy(progress = min(job.required, job.progress + amount))
            } else {
                job
            }
        }
        activeExpedition = current.copy(jobs = jobs)
    }

    private fun addCoreFromDrop(source: String) {
        val types = OfflineCoreType.entries
        val type = types[(source.hashCode() + cores.size) and (types.size - 1)]
        val tier = min(4, 1 + random.nextInt(4))
        cores += OfflineUpgradeCore(
            "drop_" + source + "_" + cores.size,
            type,
            tier,
            1 + random.nextInt(3),
            0.03f * tier + random.nextFloat() * 0.02f
        )
    }

    private fun seedMaterials() {
        listOf(
            "slime_gel" to 24,
            "aether_core" to 14,
            "plasma_fiber" to 16,
            "scrap_plate" to 18,
            "power_cell" to 10,
            "prism_shard" to 8,
            "neon_thread" to 12,
            "healing_fluid" to 10,
            "circuit_board" to 8,
            "rift_ore" to 6
        ).forEach { materials[it.first] = it.second }
    }

    private fun seedCores() {
        OfflineCoreType.entries.forEachIndexed { index, type ->
            cores += OfflineUpgradeCore(
                "starter_" + index,
                type,
                1 + index % 2,
                1,
                0.04f + index * 0.005f
            )
        }
    }

    private fun seedRecipes() {
        recipes += OfflineCraftDefinition("craft_dagger", "Adagas de Frequência", "weapon", mapOf("slime_gel" to 4, "aether_core" to 2), "weapon_dagger", 1, 1)
        recipes += OfflineCraftDefinition("craft_cannon", "Canhão Pulsar", "weapon", mapOf("scrap_plate" to 3, "power_cell" to 2, "plasma_fiber" to 2), "weapon_cannon", 1, 3)
        recipes += OfflineCraftDefinition("craft_drone", "Nanodrone de Cura", "gadget", mapOf("healing_fluid" to 2, "circuit_board" to 2, "aether_core" to 1), "gadget_heal", 1, 2)
        recipes += OfflineCraftDefinition("craft_ring", "Anel de Choque", "gadget", mapOf("power_cell" to 2, "prism_shard" to 3), "gadget_shock", 1, 4)
        recipes += OfflineCraftDefinition("craft_armor", "Armadura Sintética", "armor", mapOf("neon_thread" to 3, "slime_gel" to 3), "armor_hunter", 1, 2)
        recipes += OfflineCraftDefinition("craft_ride", "Ride Magnético", "ride", mapOf("scrap_plate" to 8, "power_cell" to 4, "rift_ore" to 2), "ride_magnetic", 1, 5)
    }
}

object OfflineHunterObjectives {
    fun base(): List<OfflineQuest> = listOf(
        OfflineQuest("q_intro", "Fale com o Guardião", "Aprenda o QG e o portal.", "talk", "guardian", 1, 80, 20),
        OfflineQuest("q_slimes", "Infestação de Gelatina", "Derrote Slimes de Aether.", "kill", "slime", 4, 120, 35),
        OfflineQuest("q_stalkers", "Perigo Veloz", "Cace Perseguidores Neon.", "kill", "stalker", 3, 160, 45),
        OfflineQuest("q_scrap", "Recuperação de Sucata", "Colete peças de sucata.", "collect", "scrap", 8, 150, 50),
        OfflineQuest("q_golem", "Alvo Colossal", "Derrote o Golem de Sucata.", "kill_boss", "golem", 1, 260, 90),
        OfflineQuest("q_guardian", "O Guardião de Aetheria", "Derrote o chefe da arena.", "kill_boss", "guardian", 1, 500, 180),
        OfflineQuest("q_rift", "Ruptura Instável", "Conclua um Rift antes da instabilidade.", "rift", "rift", 1, 320, 120),
        OfflineQuest("q_elite", "Contrato de Elite", "Derrote inimigos de elite.", "kill_elite", "elite", 5, 420, 140)
    )
}
