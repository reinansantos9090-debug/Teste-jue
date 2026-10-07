package com.example.testejuerpg.data

import android.content.Context
import android.content.SharedPreferences
import com.example.testejuerpg.model.*

class RpgStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("teste_jue_rpg_prefs", Context.MODE_PRIVATE)

    fun hasSavedGame(): Boolean {
        return prefs.getBoolean("has_save", false)
    }

    fun saveHero(hero: Hero, quests: List<Quest>) {
        val editor = prefs.edit()
        editor.putBoolean("has_save", true)
        editor.putString("hero_name", hero.name)
        editor.putString("hero_class", hero.classType.name)
        editor.putInt("hero_level", hero.level)
        editor.putInt("hero_hp", hero.currentHp)
        editor.putInt("hero_max_hp", hero.maxHp)
        editor.putInt("hero_mp", hero.currentMp)
        editor.putInt("hero_max_mp", hero.maxMp)
        editor.putInt("hero_str", hero.strength)
        editor.putInt("hero_agi", hero.agility)
        editor.putInt("hero_int", hero.intellect)
        editor.putInt("hero_vit", hero.vitality)
        editor.putInt("hero_stat_points", hero.availableStatPoints)
        editor.putInt("hero_exp", hero.experience)
        editor.putInt("hero_exp_next", hero.expToNextLevel)
        editor.putInt("hero_gold", hero.gold)
        editor.putInt("hero_slain", hero.monstersSlain)
        editor.putInt("hero_dungeons", hero.dungeonsCleared)
        editor.putInt("hero_bosses", hero.bossesSlain)

        // Equipment IDs
        editor.putString("eq_weapon", hero.equipment.weapon?.id)
        editor.putString("eq_armor", hero.equipment.armor?.id)
        editor.putString("eq_shield", hero.equipment.shield?.id)
        editor.putString("eq_acc", hero.equipment.accessory?.id)

        // Inventory item IDs with quantities (format: id:qty,id:qty)
        val invString = hero.inventory.joinToString(",") { "${it.id}:${it.quantity}" }
        editor.putString("inventory_list", invString)

        // Quests progress (format: questId:currentCount:isClaimed)
        val questString = quests.joinToString(";") { "${it.id}:${it.currentCount}:${it.isClaimed}" }
        editor.putString("quests_state", questString)

        editor.apply()
    }

    fun loadHero(): Pair<Hero, List<Quest>>? {
        if (!hasSavedGame()) return null

        val name = prefs.getString("hero_name", "Herói") ?: "Herói"
        val className = prefs.getString("hero_class", HeroClassType.GUERREIRO.name) ?: HeroClassType.GUERREIRO.name
        val classType = try {
            HeroClassType.valueOf(className)
        } catch (e: Exception) {
            HeroClassType.GUERREIRO
        }

        val level = prefs.getInt("hero_level", 1)
        val currentHp = prefs.getInt("hero_hp", classType.baseHp)
        val maxHp = prefs.getInt("hero_max_hp", classType.baseHp)
        val currentMp = prefs.getInt("hero_mp", classType.baseMp)
        val maxMp = prefs.getInt("hero_max_mp", classType.baseMp)
        val str = prefs.getInt("hero_str", 10)
        val agi = prefs.getInt("hero_agi", 10)
        val intl = prefs.getInt("hero_int", 10)
        val vit = prefs.getInt("hero_vit", 10)
        val statPoints = prefs.getInt("hero_stat_points", 0)
        val exp = prefs.getInt("hero_exp", 0)
        val expNext = prefs.getInt("hero_exp_next", 100)
        val gold = prefs.getInt("hero_gold", 50)
        val slain = prefs.getInt("hero_slain", 0)
        val dungeons = prefs.getInt("hero_dungeons", 0)
        val bosses = prefs.getInt("hero_bosses", 0)

        // Reconstruct items from catalog lookup
        val allCatalogItems = (RpgCatalog.shopItems +
                RpgCatalog.getStartingGear(HeroClassType.GUERREIRO).let { listOfNotNull(it.weapon, it.shield) } +
                RpgCatalog.getStartingGear(HeroClassType.MAGO).let { listOfNotNull(it.weapon) } +
                RpgCatalog.getStartingGear(HeroClassType.LADINO).let { listOfNotNull(it.weapon) } +
                RpgCatalog.getStartingGear(HeroClassType.PALADINO).let { listOfNotNull(it.weapon, it.shield) })
            .associateBy { it.id }

        val weaponId = prefs.getString("eq_weapon", null)
        val armorId = prefs.getString("eq_armor", null)
        val shieldId = prefs.getString("eq_shield", null)
        val accId = prefs.getString("eq_acc", null)

        val equipment = Equipment(
            weapon = weaponId?.let { allCatalogItems[it] },
            armor = armorId?.let { allCatalogItems[it] },
            shield = shieldId?.let { allCatalogItems[it] },
            accessory = accId?.let { allCatalogItems[it] }
        )

        // Reconstruct inventory
        val invString = prefs.getString("inventory_list", "") ?: ""
        val inventory = if (invString.isNotEmpty()) {
            invString.split(",").mapNotNull { part ->
                val pieces = part.split(":")
                val id = pieces.getOrNull(0) ?: return@mapNotNull null
                val qty = pieces.getOrNull(1)?.toIntOrNull() ?: 1
                allCatalogItems[id]?.copy(quantity = qty)
            }
        } else {
            emptyList()
        }

        val skills = RpgCatalog.getSkillsForClass(classType)

        val hero = Hero(
            name = name,
            classType = classType,
            level = level,
            currentHp = currentHp,
            maxHp = maxHp,
            currentMp = currentMp,
            maxMp = maxMp,
            strength = str,
            agility = agi,
            intellect = intl,
            vitality = vit,
            availableStatPoints = statPoints,
            experience = exp,
            expToNextLevel = expNext,
            gold = gold,
            equipment = equipment,
            inventory = inventory,
            skills = skills,
            monstersSlain = slain,
            dungeonsCleared = dungeons,
            bossesSlain = bosses
        )

        // Reconstruct quests
        val questString = prefs.getString("quests_state", "") ?: ""
        val questMap = if (questString.isNotEmpty()) {
            questString.split(";").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size >= 3) {
                    val id = parts[0]
                    val count = parts[1].toIntOrNull() ?: 0
                    val isClaimed = parts[2].toBooleanStrictOrNull() ?: false
                    id to Pair(count, isClaimed)
                } else null
            }.toMap()
        } else {
            emptyMap()
        }

        val quests = RpgCatalog.initialQuests.map { q ->
            val saved = questMap[q.id]
            if (saved != null) {
                val currentCount = saved.first
                val isClaimed = saved.second
                q.copy(
                    currentCount = currentCount,
                    isCompleted = currentCount >= q.requiredCount,
                    isClaimed = isClaimed
                )
            } else {
                q
            }
        }

        return Pair(hero, quests)
    }

    fun clearSave() {
        prefs.edit().clear().apply()
    }
}
