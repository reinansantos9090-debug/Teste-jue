package com.example.testejuerpg.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineGameplayRulesTest {
    @Test fun damage_scales_and_critical_is_threefold() {
        val normal = OfflineCombatRules.playerDamage(100f, 10, 0.25f, 0f, 0.9f)
        val critical = OfflineCombatRules.playerDamage(100f, 10, 0.25f, 1f, 0f)
        assertTrue(normal > 100f)
        assertEquals(normal * 3f, critical, 0.01f)
    }

    @Test fun xp_can_cross_multiple_thresholds() {
        val result = OfflineProgressionRules.addXp(1, 90f, 100f, 150f, 250f)
        assertTrue(result.level >= 3)
        assertTrue(result.maxHealth > 150f)
    }

    @Test fun rarity_changes_reward_value() {
        val common = OfflineLootRules.roll(0.99f, 0.99f, false, false)
        val legendary = OfflineLootRules.roll(0.01f, 0.01f, true, true)
        assertEquals(LootRarity.COMMON, common.rarity)
        assertTrue(legendary.rarity.id > common.rarity.id)
        assertTrue(legendary.gold > common.gold)
        assertTrue(legendary.cores > common.cores)
    }

    @Test fun save_codec_round_trips_state() {
        val original = EngineSaveData(7,81.5f,245f,240f,197f,1234,48,92,5,4.5f,-2.5f,"Caçador|Aurora")
        assertEquals(original, OfflineSaveCodec.decode(OfflineSaveCodec.encode(original)))
    }
    @Test fun crafting_requires_level_and_materials() {
        val recipe = com.example.testejuerpg.offline.systems.OfflineCraftDefinition(
            id = "gadget_nanodrone",
            name = "Nanodrone",
            category = "gadget",
            ingredients = mapOf("aether_core" to 5, "scrap" to 20),
            result = "nanodrone",
            amount = 1,
            unlockLevel = 3
        )
        assertTrue(!OfflineCraftingRules.canCraft(recipe, 2, mapOf("aether_core" to 99, "scrap" to 99)))
        assertTrue(!OfflineCraftingRules.canCraft(recipe, 3, mapOf("aether_core" to 4, "scrap" to 20)))
        assertTrue(OfflineCraftingRules.canCraft(recipe, 3, mapOf("aether_core" to 5, "scrap" to 20)))
    }

}
