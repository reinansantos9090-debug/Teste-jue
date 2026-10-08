package com.example.testejuerpg.offline

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
