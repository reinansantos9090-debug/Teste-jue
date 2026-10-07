package com.example.testejuerpg.offline.systems

import com.example.testejuerpg.offline.OfflineCoreType
import com.example.testejuerpg.offline.OfflineEventDefinition
import com.example.testejuerpg.offline.OfflineMode
import com.example.testejuerpg.offline.OfflineUpgradeCore
import kotlin.math.max

enum class RooftopScreen {
    HOME, PORTAL, EXPEDITIONS, RIFTS, VERSUS, WARDROBE, CORES, DAILY, EVENTS, PROFILE, CRAFTING, STORY
}

data class RooftopCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val detail: String,
    val accent: Int,
    val enabled: Boolean = true
)

data class OfflineActivityState(
    val mode: OfflineMode,
    val title: String,
    val world: String,
    val objective: String,
    val difficulty: Int,
    val seconds: Int
)

data class OfflineSprint(
    val id: String,
    val weaponId: String,
    val title: String,
    val goal: String,
    val required: Int,
    val rewardEnergy: Int,
    val rewardPerks: Int,
    val progress: Int = 0
) {
    val complete: Boolean get() = progress >= required
}

class OfflineRooftopController(private val director: OfflineHunterDirector) {
    var screen: RooftopScreen = RooftopScreen.HOME
        private set
    var selectedExpedition: Int = 0
        private set
    var selectedRiftSet: Int = 0
        private set
    var selectedRiftSlot: Int = 0
        private set
    var selectedEvent: Int = 0
        private set
    var selectedStyle: Int = 0
        private set
    var selectedBot: Int = 0
        private set

    private var activeEventId: String? = null
    private var expeditionSprintProgress = 0
    private var riftWaveProgress = 0
    private var versusScore = 0

    private val sprints = mutableListOf(
        OfflineSprint("sprint_00", "volt_blades", "Circuito Voltáico", "Derrote 30 monstros.", 30, 280, 2),
        OfflineSprint("sprint_01", "toxic_bow", "Pulso Tóxico", "Derrote 24 monstros.", 24, 300, 2),
        OfflineSprint("sprint_02", "pulsar_cannon", "Impacto Pulsar", "Derrote 40 monstros.", 40, 360, 3),
        OfflineSprint("sprint_03", "scrap_hammer", "Sucata Pesada", "Derrote 18 elites.", 18, 420, 3),
        OfflineSprint("sprint_04", "prism_spear", "Luz de Prisma", "Derrote 32 monstros.", 32, 340, 2),
        OfflineSprint("sprint_05", "echo_chakrams", "Eco Cortante", "Derrote 28 monstros.", 28, 320, 2),
        OfflineSprint("sprint_06", "nova_gauntlets", "Combo Nova", "Derrote 45 monstros.", 45, 390, 3),
        OfflineSprint("sprint_07", "rift_mortar", "Artilharia de Fenda", "Derrote 35 elites.", 35, 450, 3),
        OfflineSprint("sprint_08", "arc_whip", "Arco de Contenção", "Aplique 25 golpes de controle.", 25, 350, 2),
        OfflineSprint("sprint_09", "frost_rail", "Linha Glacial", "Derrote 26 monstros.", 26, 380, 3),
        OfflineSprint("sprint_10", "solar_lance", "Luz Solar", "Derrote 34 monstros.", 34, 400, 3),
        OfflineSprint("sprint_11", "grav_hammer", "Peso Graviton", "Derrote 20 elites.", 20, 460, 3),
        OfflineSprint("sprint_12", "drone_staff", "Esquadrão Drone", "Complete 12 caçadas.", 12, 370, 2),
        OfflineSprint("sprint_13", "plasma_twins", "Dança Plasma", "Derrote 50 monstros.", 50, 430, 3),
        OfflineSprint("sprint_14", "vortex_scepter", "Colapso Vórtice", "Derrote 30 monstros.", 30, 390, 3),
        OfflineSprint("sprint_15", "meteor_knuckle", "Punho Meteoro", "Derrote 24 elites.", 24, 480, 4),
        OfflineSprint("sprint_16", "aether_rifle", "Mira Aether", "Derrote 42 monstros.", 42, 410, 3),
        OfflineSprint("sprint_17", "bloom_blade", "Jardim Cortante", "Colete 28 núcleos.", 28, 420, 3),
        OfflineSprint("sprint_18", "thunder_orb", "Tempestade Orbital", "Derrote 36 monstros.", 36, 430, 4),
        OfflineSprint("sprint_19", "mono_drill", "Perfuração", "Derrote 22 elites.", 22, 500, 4),
        OfflineSprint("sprint_20", "starlight_fan", "Constelação", "Derrote 38 monstros.", 38, 440, 3),
        OfflineSprint("sprint_21", "phase_blaster", "Salto Fásico", "Complete 10 rifts.", 10, 520, 4),
        OfflineSprint("sprint_22", "ember_halo", "Halo Incandescente", "Derrote 44 monstros.", 44, 460, 4),
        OfflineSprint("sprint_23", "kinetic_scythe", "Foice Cinética", "Derrote 28 elites.", 28, 540, 4)
    )

    fun navigate(target: RooftopScreen) { screen = target }
    fun backToHome() { screen = RooftopScreen.HOME }

    fun portalCards(): List<RooftopCard> = listOf(
        RooftopCard("expeditions", "EXPEDIÇÕES", "Mundos paralelos", "Tarefas rotativas, eventos e chefes.", 0xFF5B8CFF.toInt()),
        RooftopCard("rifts", "RIFTS", "Objetivo contra o tempo", "Conjuntos de 3 fendas com arena ou chefe.", 0xFFB36BFF.toInt()),
        RooftopCard("versus", "VERSUS", "Simulação offline", "Duelo contra caçadores controlados pela IA.", 0xFFFF6B88.toInt()),
        RooftopCard("events", "EVENTOS", "Modificadores", "Atividades especiais com recompensas maiores.", 0xFFFFC857.toInt())
    )

    fun utilityCards(): List<RooftopCard> = listOf(
        RooftopCard("wardrobe", "ARMÁRIO", "12 estilos", "Traje, cabelo, rosto e ride.", 0xFF5DE2E7.toInt()),
        RooftopCard("cores", "NÚCLEOS", "4 níveis", "Equipar, trocar e fundir núcleos.", 0xFF74D86D.toInt()),
        RooftopCard("daily", "OBJETIVOS", "Sequência", "Objetivos diários e bônus de sequência.", 0xFFFFAA66.toInt()),
        RooftopCard("crafting", "OFICINA", "Criação", "Armas, gadgets, armaduras e rides.", 0xFFD5A0FF.toInt()),
        RooftopCard("profile", "PERFIL", "Carreira", "Nível, energia, abates e progresso.", 0xFF95A6FF.toInt()),
        RooftopCard("story", "HISTÓRIA", "Campanha offline", "30 capítulos com diálogos, objetivos e chefes.", 0xFFFF7BC8.toInt())
    )

    fun selectExpedition(index: Int) {
        selectedExpedition = index.coerceIn(0, max(0, director.buildExpeditions().lastIndex))
    }

    fun selectedExpeditionData() = director.buildExpeditions().getOrNull(selectedExpedition)

    fun selectRift(set: Int, slot: Int) {
        selectedRiftSet = set.coerceIn(0, 5)
        selectedRiftSlot = slot.coerceIn(0, 2)
    }

    fun selectedRiftData() = director.buildRiftSet(selectedRiftSet).getOrNull(selectedRiftSlot)

    fun selectEvent(index: Int) {
        selectedEvent = index.coerceIn(0, max(0, director.events().lastIndex))
    }

    fun selectedEventData(): OfflineEventDefinition? = director.events().getOrNull(selectedEvent)

    fun activateEvent(): Boolean {
        val chosen = selectedEventData() ?: return false
        activeEventId = chosen.id
        director.recordEvent(chosen.id)
        screen = RooftopScreen.PORTAL
        return true
    }

    fun activeEvent(): OfflineEventDefinition? =
        activeEventId?.let { id -> director.events().firstOrNull { it.id == id } }

    fun selectStyle(index: Int): Boolean {
        val changed = director.selectStyle(index)
        if (changed) selectedStyle = index
        return changed
    }

    fun unlockStyle(index: Int): Boolean = director.unlockStyle(index, 80 + index * 30)

    fun styles() = director.styles()
    fun pets() = director.pets()
    fun cores() = director.cores()
    fun equippedCores() = director.equippedCores()
    fun materials() = director.materials()
    fun recipes() = director.recipes()
    fun quests() = director.quests()
    fun dailyGoals() = director.dailyGoals()
    fun events() = director.events()
    fun sprints() = sprints.toList()
    fun selectedStyle() = director.styles().getOrNull(selectedStyle)

    fun selectBot(index: Int) {
        selectedBot = index.coerceIn(0, max(0, director.bots().lastIndex))
    }

    fun bots() = director.bots()

    fun fuseCores(first: OfflineUpgradeCore, second: OfflineUpgradeCore): OfflineUpgradeCore? =
        director.fuse(first.id, second.id)

    fun equipCore(weaponId: String, core: OfflineUpgradeCore): Boolean =
        director.equipCore(weaponId, core.id)

    fun coreBonusLabel(core: OfflineUpgradeCore): String = when (core.type) {
        OfflineCoreType.DAMAGE -> "Dano +"+(core.bonus * 100).toInt()+"%"
        OfflineCoreType.CRITICAL_DAMAGE -> "Crítico +"+(core.bonus * 100).toInt()+"%"
        OfflineCoreType.ATTACK_SPEED -> "Vel. ataque +"+(core.bonus * 100).toInt()+"%"
        OfflineCoreType.COOLDOWN -> "Cooldown -"+(core.bonus * 100).toInt()+"%"
        OfflineCoreType.HEALING -> "Cura +"+(core.bonus * 100).toInt()+"%"
        OfflineCoreType.HEALING_RECEIVED -> "Cura recebida +"+(core.bonus * 100).toInt()+"%"
        OfflineCoreType.MAX_HEALTH -> "Vida máxima +"+(core.bonus * 100).toInt()+"%"
        OfflineCoreType.DODGE -> "Esquiva "+(core.bonus * 100).toInt()+"%"
    }

    fun buildActivityState(): OfflineActivityState? {
        director.expedition()?.let { e ->
            return OfflineActivityState(
                OfflineMode.EXPEDITION,
                e.name,
                e.world,
                e.jobs.joinToString(" • ") { job -> job.title+" "+job.progress+"/"+job.required },
                e.difficulty,
                director.expeditionRemaining()
            )
        }
        director.rift()?.let { r ->
            return OfflineActivityState(
                director.mode(),
                r.name,
                r.setId,
                r.goal.title+": "+r.goal.amount,
                r.difficulty,
                director.riftRemaining()
            )
        }
        if (director.mode() == OfflineMode.VERSUS_SIM) {
            val bot = director.bots().getOrNull(selectedBot)
            return OfflineActivityState(
                OfflineMode.VERSUS_SIM,
                "Versus: "+(bot?.name ?: "Rival"),
                "Arena Offline",
                "Pontuação "+versusScore,
                bot?.difficulty ?: 1,
                300
            )
        }
        return null
    }

    fun enterSelectedExpedition() {
        director.enterExpedition(selectedExpedition)
        expeditionSprintProgress = 0
        screen = RooftopScreen.HOME
    }

    fun enterSelectedRift() {
        director.enterRift(selectedRiftSet, selectedRiftSlot)
        riftWaveProgress = 0
        screen = RooftopScreen.HOME
    }

    fun enterVersus() {
        director.enterVersus()
        versusScore = 0
        screen = RooftopScreen.HOME
    }

    fun enterTraining() {
        director.enterTraining()
        screen = RooftopScreen.HOME
    }

    fun recordBattleKill(weaponIndex: Int, elite: Boolean, boss: Boolean) {
        val current = weaponIndex.coerceIn(0, sprints.lastIndex)
        val sprint = sprints[current]
        if (!sprint.complete && (current != 3 || elite)) {
            sprints[current] = sprint.copy(progress = sprint.progress + 1)
        }
        when (director.mode()) {
            OfflineMode.RIFT_ARENA -> riftWaveProgress += 1
            OfflineMode.VERSUS_SIM -> versusScore += 100 + if (elite) 35 else 0
            else -> expeditionSprintProgress += 1
        }
        director.recordKill(
            when {
                boss -> "boss"
                elite -> "elite"
                else -> "monster"
            },
            elite,
            boss
        )
    }

    fun riftWaveProgress(): Int = riftWaveProgress
    fun versusScore(): Int = versusScore
    fun expeditionSprintProgress(): Int = expeditionSprintProgress

    fun objectiveSummary(): String = when (director.mode()) {
        OfflineMode.EXPEDITION -> "Trabalhos de caçador • Energia do caos"
        OfflineMode.RIFT_BOSS -> "Rift instável • chefe antes do cronômetro"
        OfflineMode.RIFT_ARENA -> "Rift arena • sobreviva às ondas"
        OfflineMode.VERSUS_SIM -> "Versus offline • supere a pontuação do rival"
        OfflineMode.EVENT -> activeEvent()?.name ?: "Evento ativo"
        OfflineMode.TRAINING -> "Treino • sem pressão"
        OfflineMode.STORY -> "HISTÓRIA • campanha offline persistente"
    }
}
