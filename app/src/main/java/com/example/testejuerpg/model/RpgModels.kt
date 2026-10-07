package com.example.testejuerpg.model

enum class HeroClassType(
    val titlePt: String,
    val titleEn: String,
    val descriptionPt: String,
    val iconEmoji: String,
    val primaryStat: String,
    val baseHp: Int,
    val baseMp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val baseSpeed: Int
) {
    GUERREIRO(
        titlePt = "Guerreiro",
        titleEn = "Warrior",
        descriptionPt = "Mestre do combate corpo a corpo, armaduras pesadas e resistência imbatível.",
        iconEmoji = "⚔️",
        primaryStat = "Força",
        baseHp = 120,
        baseMp = 30,
        baseAttack = 16,
        baseDefense = 14,
        baseSpeed = 8
    ),
    MAGO(
        titlePt = "Mago",
        titleEn = "Mage",
        descriptionPt = "Conjurador de magias arcanas devastadoras, mestre dos elementos e feitiços.",
        iconEmoji = "🔮",
        primaryStat = "Inteligência",
        baseHp = 75,
        baseMp = 100,
        baseAttack = 22,
        baseDefense = 6,
        baseSpeed = 10
    ),
    LADINO(
        titlePt = "Ladino",
        titleEn = "Rogue",
        descriptionPt = "Ágil, letal e furtivo. Especialista em ataques críticos e esquivas rápidas.",
        iconEmoji = "🗡️",
        primaryStat = "Destreza",
        baseHp = 85,
        baseMp = 45,
        baseAttack = 19,
        baseDefense = 8,
        baseSpeed = 16
    ),
    PALADINO(
        titlePt = "Paladino",
        titleEn = "Paladin",
        descriptionPt = "Guerreiro sagrado imbuído de luz divina, capaz de golpear o mal e curar ferimentos.",
        iconEmoji = "🛡️",
        primaryStat = "Fé & Força",
        baseHp = 105,
        baseMp = 60,
        baseAttack = 15,
        baseDefense = 12,
        baseSpeed = 7
    )
}

enum class ItemType {
    WEAPON,
    ARMOR,
    SHIELD,
    ACCESSORY,
    CONSUMABLE
}

enum class ItemRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}

data class Item(
    val id: String,
    val namePt: String,
    val descriptionPt: String,
    val type: ItemType,
    val rarity: ItemRarity,
    val bonusAttack: Int = 0,
    val bonusDefense: Int = 0,
    val bonusHp: Int = 0,
    val bonusMp: Int = 0,
    val healHp: Int = 0,
    val healMp: Int = 0,
    val valueGold: Int = 10,
    val iconEmoji: String = "📦",
    val quantity: Int = 1
)

enum class SkillTarget {
    SINGLE_ENEMY,
    SELF
}

data class Skill(
    val id: String,
    val namePt: String,
    val descriptionPt: String,
    val manaCost: Int,
    val powerMultiplier: Float,
    val target: SkillTarget,
    val healAmount: Int = 0,
    val iconEmoji: String,
    val unlockLevel: Int = 1
)

data class Equipment(
    val weapon: Item? = null,
    val armor: Item? = null,
    val shield: Item? = null,
    val accessory: Item? = null
)

data class Hero(
    val name: String,
    val classType: HeroClassType,
    val level: Int = 1,
    val currentHp: Int,
    val maxHp: Int,
    val currentMp: Int,
    val maxMp: Int,
    val strength: Int = 10,
    val agility: Int = 10,
    val intellect: Int = 10,
    val vitality: Int = 10,
    val availableStatPoints: Int = 0,
    val experience: Int = 0,
    val expToNextLevel: Int = 100,
    val gold: Int = 50,
    val equipment: Equipment = Equipment(),
    val inventory: List<Item> = emptyList(),
    val skills: List<Skill> = emptyList(),
    val monstersSlain: Int = 0,
    val dungeonsCleared: Int = 0,
    val bossesSlain: Int = 0
) {
    val totalAttack: Int
        get() = classType.baseAttack + (strength * 2) +
                (equipment.weapon?.bonusAttack ?: 0) +
                (equipment.accessory?.bonusAttack ?: 0)

    val totalDefense: Int
        get() = classType.baseDefense + (vitality * 1) +
                (equipment.armor?.bonusDefense ?: 0) +
                (equipment.shield?.bonusDefense ?: 0)

    val totalMaxHp: Int
        get() = maxHp + (vitality * 5) +
                (equipment.armor?.bonusHp ?: 0) +
                (equipment.shield?.bonusHp ?: 0)

    val totalMaxMp: Int
        get() = maxMp + (intellect * 4) +
                (equipment.accessory?.bonusMp ?: 0)

    val critChancePercent: Int
        get() = (5 + (agility * 1.5)).toInt().coerceIn(5, 75)

    val speed: Int
        get() = classType.baseSpeed + agility
}

data class Monster(
    val id: String,
    val namePt: String,
    val descriptionPt: String,
    val level: Int,
    val maxHp: Int,
    val currentHp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val expReward: Int,
    val goldReward: Int,
    val iconEmoji: String,
    val isBoss: Boolean = false,
    val specialSkillName: String? = null
)

enum class RoomType {
    EMPTY,
    COMBAT,
    TREASURE,
    SHRINE,
    TRAP,
    BOSS
}

data class Dungeon(
    val id: String,
    val namePt: String,
    val descriptionPt: String,
    val recommendedLevel: Int,
    val totalRooms: Int,
    val iconEmoji: String,
    val enemyPool: List<Monster>,
    val boss: Monster
)

data class DungeonRun(
    val dungeon: Dungeon,
    val currentRoomIndex: Int = 0,
    val currentRoomType: RoomType = RoomType.COMBAT,
    val roomHistory: List<RoomType> = emptyList(),
    val lootCollectedGold: Int = 0,
    val itemsCollected: List<Item> = emptyList(),
    val hasFled: Boolean = false
)

data class Quest(
    val id: String,
    val titlePt: String,
    val descriptionPt: String,
    val targetMonsterId: String,
    val requiredCount: Int,
    val currentCount: Int = 0,
    val rewardGold: Int,
    val rewardExp: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)

enum class CombatTurn {
    HERO,
    ENEMY
}

data class CombatLogEntry(
    val text: String,
    val isHeroAction: Boolean,
    val isCritical: Boolean = false,
    val isHeal: Boolean = false
)

data class CombatState(
    val activeMonster: Monster,
    val turn: CombatTurn = CombatTurn.HERO,
    val isDefending: Boolean = false,
    val monsterIsDefending: Boolean = false,
    val combatLogs: List<CombatLogEntry> = emptyList(),
    val isVictory: Boolean = false,
    val isDefeat: Boolean = false,
    val isFled: Boolean = false,
    val expEarned: Int = 0,
    val goldEarned: Int = 0,
    val droppedItem: Item? = null
)

enum class GameScreen {
    TITLE,
    CHARACTER_CREATION,
    TOWN_HUB,
    DUNGEON_SELECT,
    DUNGEON_EXPLORE,
    COMBAT,
    INVENTORY,
    HERO_PROFILE,
    SHOP,
    BLACKSMITH,
    TAVERN,
    QUEST_BOARD
}
