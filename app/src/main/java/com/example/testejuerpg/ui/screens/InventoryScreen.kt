package com.example.testejuerpg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testejuerpg.model.Hero
import com.example.testejuerpg.model.Item
import com.example.testejuerpg.model.ItemType
import com.example.testejuerpg.ui.components.RpgItemCard
import com.example.testejuerpg.ui.components.RpgTopBar
import com.example.testejuerpg.ui.theme.*

@Composable
fun InventoryScreen(
    hero: Hero,
    onBack: () -> Unit,
    onEquipItem: (Item) -> Unit,
    onUnequipSlot: (String) -> Unit,
    onUseItem: (Item) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Todos", "Equipamentos", "Consumíveis")

    val filteredItems = remember(selectedTab, hero.inventory) {
        when (selectedTab) {
            1 -> hero.inventory.filter { it.type != ItemType.CONSUMABLE }
            2 -> hero.inventory.filter { it.type == ItemType.CONSUMABLE }
            else -> hero.inventory
        }
    }

    Scaffold(
        topBar = {
            RpgTopBar(
                hero = hero,
                title = "Mochila & Equipamentos",
                onBack = onBack
            )
        },
        containerColor = DungeonDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Equipment Paperdoll Section
            item {
                Text(
                    text = "Equipamento Atual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EquipSlotCard(
                        title = "Arma",
                        item = hero.equipment.weapon,
                        defaultEmoji = "🗡️",
                        onUnequip = { onUnequipSlot("weapon") },
                        modifier = Modifier.weight(1f)
                    )
                    EquipSlotCard(
                        title = "Armadura",
                        item = hero.equipment.armor,
                        defaultEmoji = "🥋",
                        onUnequip = { onUnequipSlot("armor") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EquipSlotCard(
                        title = "Escudo",
                        item = hero.equipment.shield,
                        defaultEmoji = "🛡️",
                        onUnequip = { onUnequipSlot("shield") },
                        modifier = Modifier.weight(1f)
                    )
                    EquipSlotCard(
                        title = "Acessório",
                        item = hero.equipment.accessory,
                        defaultEmoji = "💍",
                        onUnequip = { onUnequipSlot("accessory") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Tabs for filtering items
            item {
                Spacer(modifier = Modifier.height(4.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DungeonSurface,
                    contentColor = GoldPrimary,
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) GoldLight else TextSecondary
                                )
                            }
                        )
                    }
                }
            }

            // Items list
            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum item nesta categoria.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            } else {
                items(filteredItems) { item ->
                    val isConsumable = item.type == ItemType.CONSUMABLE
                    RpgItemCard(
                        item = item,
                        onActionClick = {
                            if (isConsumable) onUseItem(item) else onEquipItem(item)
                        },
                        actionButtonText = if (isConsumable) "Usar" else "Equipar"
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipSlotCard(
    title: String,
    item: Item?,
    defaultEmoji: String,
    onUnequip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DungeonSurfaceBright, RoundedCornerShape(12.dp)),
        color = DungeonSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(item?.iconEmoji ?: defaultEmoji, fontSize = 20.sp)
                    Text(
                        text = item?.namePt ?: "Vazio",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (item != null) FontWeight.Bold else FontWeight.Normal,
                        color = if (item != null) TextPrimary else TextMuted,
                        maxLines = 1
                    )
                }

                if (item != null) {
                    Text(
                        text = "✕",
                        color = RubyLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(onClick = onUnequip)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
