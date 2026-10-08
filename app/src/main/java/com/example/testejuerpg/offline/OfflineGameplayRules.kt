package com.example.testejuerpg.offline

import kotlin.math.floor

data class ProgressionResult(
    val level: Int,
    val xp: Float,
    val xpToNext: Float,
    val maxHealth: Float,
    val levelsGained: Int
)

object OfflineCombatRules {
    fun playerDamage(baseDamage: Float, level: Int, damageBonus: Float, criticalChance: Float, criticalRoll: Float): Float {
        val levelMultiplier = 1f + (level.coerceAtLeast(1) - 1) * 0.02f
        val critMultiplier = if (criticalRoll.coerceIn(0f, 1f) < criticalChance.coerceIn(0f, 1f)) 3f else 1f
        return baseDamage.coerceAtLeast(0f) * levelMultiplier * (1f + damageBonus.coerceIn(-0.9f, 10f)) * critMultiplier
    }
}

object OfflineProgressionRules {
    fun addXp(level: Int, xp: Float, xpToNext: Float, maxHealth: Float, amount: Float): ProgressionResult {
        var nextLevel = level.coerceAtLeast(1)
        var currentXp = xp.coerceAtLeast(0f)
        var threshold = xpToNext.coerceAtLeast(1f)
        var health = maxHealth.coerceAtLeast(1f)
        var gained = 0
        currentXp += amount.coerceAtLeast(0f)
        while (currentXp >= threshold) {
            currentXp -= threshold
            nextLevel += 1
            gained += 1
            threshold = floor(threshold * 1.28f + 20f)
            health += 14f
        }
        return ProgressionResult(nextLevel, currentXp, threshold, health, gained)
    }
}

enum class LootRarity(val id: Int, val label: String, val multiplier: Float) {
    COMMON(0, "Comum", 1.0f),
    UNCOMMON(1, "Incomum", 1.25f),
    RARE(2, "Raro", 1.60f),
    EPIC(3, "Épico", 2.20f),
    LEGENDARY(4, "Lendário", 3.20f);

    companion object {
        fun fromId(id: Int): LootRarity = values().firstOrNull { it.id == id } ?: COMMON
    }
}

data class LootResult(val rarity: LootRarity, val gold: Int, val cores: Int)

object OfflineLootRules {
    fun roll(rarityRoll: Float, bonusRoll: Float, elite: Boolean, boss: Boolean): LootResult {
        val base = rarityFrom(rarityRoll, elite, boss)
        val upgradeChance = if (boss) 0.42f else if (elite) 0.24f else 0.08f
        val quality = if (bonusRoll.coerceIn(0f, 1f) < upgradeChance) {
            LootRarity.fromId((base.id + 1).coerceAtMost(LootRarity.LEGENDARY.id))
        } else base
        val gold = ((if (boss) 160 else if (elite) 34 else 8) * quality.multiplier).toInt().coerceAtLeast(1)
        val cores = (if (boss) 8 else if (elite) 2 else 1) + quality.id.coerceAtMost(3)
        return LootResult(quality, gold, cores)
    }

    private fun rarityFrom(roll: Float, elite: Boolean, boss: Boolean): LootRarity {
        val r = roll.coerceIn(0f, 1f)
        if (boss && r < 0.16f) return LootRarity.LEGENDARY
        if ((boss || elite) && r < 0.28f) return LootRarity.EPIC
        if (r < 0.40f) return LootRarity.RARE
        if (r < 0.70f) return LootRarity.UNCOMMON
        return LootRarity.COMMON
    }
}

data class EngineSaveData(
    val level: Int, val xp: Float, val xpToNext: Float, val maxHp: Float, val hp: Float,
    val gold: Int, val cores: Int, val kills: Int, val weapon: Int, val px: Float, val pz: Float, val name: String
)

object OfflineSaveCodec {
    private const val VERSION = 1

    fun encode(data: EngineSaveData): String = listOf(
        VERSION, data.level, data.xp, data.xpToNext, data.maxHp, data.hp,
        data.gold, data.cores, data.kills, data.weapon, data.px, data.pz, escape(data.name)
    ).joinToString("|")

    fun decode(encoded: String): EngineSaveData? {
        val p = encoded.split("|")
        if (p.size != 13 || p[0].toIntOrNull() != VERSION) return null
        return EngineSaveData(
            p[1].toIntOrNull() ?: return null, p[2].toFloatOrNull() ?: return null,
            p[3].toFloatOrNull() ?: return null, p[4].toFloatOrNull() ?: return null,
            p[5].toFloatOrNull() ?: return null, p[6].toIntOrNull() ?: return null,
            p[7].toIntOrNull() ?: return null, p[8].toIntOrNull() ?: return null,
            p[9].toIntOrNull() ?: return null, p[10].toFloatOrNull() ?: return null,
            p[11].toFloatOrNull() ?: return null, unescape(p[12])
        )
    }

    private fun escape(value: String): String = value.replace("%", "%25").replace("|", "%7C").replace("\n", "%0A")
    private fun unescape(value: String): String = value.replace("%0A", "\n").replace("%7C", "|").replace("%25", "%")
}
