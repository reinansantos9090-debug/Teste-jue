package com.example.testejuerpg.offline

/**
 * Immutable offline content record used by the native 3D game.
 * All content is compiled into the APK so the playable systems do not need a server.
 */
data class OfflineContentEntry(
    val id: String,
    val category: String,
    val name: String,
    val role: String,
    val power: Int,
    val speed: Int,
    val range: Int,
    val cooldownMs: Int,
    val rarity: Int,
    val description: String
)

enum class OfflineMode {
    EXPEDITION,
    RIFT_BOSS,
    RIFT_ARENA,
    VERSUS_SIM,
    EVENT,
    TRAINING,
    STORY
}

enum class OfflineCoreType {
    DAMAGE,
    CRITICAL_DAMAGE,
    ATTACK_SPEED,
    COOLDOWN,
    HEALING,
    HEALING_RECEIVED,
    MAX_HEALTH,
    DODGE
}

data class OfflineUpgradeCore(
    val id: String,
    val type: OfflineCoreType,
    val tier: Int,
    val charge: Int,
    val bonus: Float
)

data class OfflineQuest(
    val id: String,
    val title: String,
    val description: String,
    val objectiveType: String,
    val targetId: String,
    val targetCount: Int,
    val rewardEnergy: Int,
    val rewardGold: Int
)

data class OfflineDailyGoal(
    val id: String,
    val title: String,
    val description: String,
    val required: Int,
    val rewardEnergy: Int
)

data class OfflineEventDefinition(
    val id: String,
    val name: String,
    val durationSeconds: Int,
    val modifier: String,
    val rewardMultiplier: Float
)

object OfflineGameConstants {
    const val SAVE_SCHEMA = 3
    const val MAX_STYLES = 12
    const val MAX_PARTY_BOTS = 3
    const val MAX_ACTIVE_ENEMIES = 20
    const val EXPEDITION_TIME_SECONDS = 900
    const val RIFT_TIME_SECONDS = 480
    const val BOSS_RESPAWN_DELAY_SECONDS = 8
    const val AUTO_SAVE_SECONDS = 20
}
