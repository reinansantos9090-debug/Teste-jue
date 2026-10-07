package com.example.testejuerpg.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testejuerpg.R
import com.example.testejuerpg.model.Hero
import com.example.testejuerpg.model.Item
import com.example.testejuerpg.model.ItemType
import com.example.testejuerpg.model.Quest
import com.example.testejuerpg.ui.components.RpgItemCard
import com.example.testejuerpg.ui.components.RpgTopBar
import com.example.testejuerpg.ui.theme.*

@Composable
fun ShopScreen(
    hero: Hero,
    catalog: List<Item>,
    isBlacksmith: Boolean,
    onBack: () -> Unit,
    onBuyItem: (Item) -> Unit,
    onSellItem: (Item) -> Unit
) {
    var isSellTab by remember { mutableStateOf(false) }

    val filteredCatalog = remember(isBlacksmith, catalog) {
        if (isBlacksmith) {
            catalog.filter { it.type != ItemType.CONSUMABLE }
        } else {
            catalog.filter { it.type == ItemType.CONSUMABLE }
        }
    }

    Scaffold(
        topBar = {
            RpgTopBar(
                hero = hero,
                title = if (isBlacksmith) "Ferreiro Armeiro" else "Loja de Alquimia",
                onBack = onBack
            )
        },
        containerColor = DungeonDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Tab Buy vs Sell
            TabRow(
                selectedTabIndex = if (isSellTab) 1 else 0,
                containerColor = DungeonSurface,
                contentColor = GoldPrimary
            ) {
                Tab(
                    selected = !isSellTab,
                    onClick = { isSellTab = false },
                    text = { Text("Comprar Itens", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = isSellTab,
                    onClick = { isSellTab = true },
                    text = { Text("Vender Pertences", fontWeight = FontWeight.Bold) }
                )
            }

            if (!isSellTab) {
                // Buy List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCatalog) { item ->
                        val canAfford = hero.gold >= item.valueGold
                        RpgItemCard(
                            item = item,
                            onActionClick = { onBuyItem(item) },
                            actionButtonText = "${item.valueGold} 💰 Comprar",
                            modifier = Modifier.testTag("shop_item_${item.id}")
                        )
                    }
                }
            } else {
                // Sell List
                if (hero.inventory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sua mochila está vazia.", color = TextMuted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(hero.inventory) { item ->
                            val sellPrice = (item.valueGold * 0.5).toInt().coerceAtLeast(1)
                            RpgItemCard(
                                item = item,
                                onActionClick = { onSellItem(item) },
                                actionButtonText = "Vender por $sellPrice 💰",
                                modifier = Modifier.testTag("sell_item_${item.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TavernScreen(
    hero: Hero,
    onBack: () -> Unit,
    onRest: () -> Unit
) {
    Scaffold(
        topBar = {
            RpgTopBar(
                hero = hero,
                title = "Taverna do Javali Saltitante",
                onBack = onBack
            )
        },
        containerColor = DungeonDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🍺", fontSize = 64.sp)
                Text(
                    text = "O Descanso do Viajante",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "A lareira queima lenha perfumada. O taberneiro Barnabé oferece um quarto confortável e um banquete quente para curar todas as suas feridas e recarregar suas energias arcanas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DungeonSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Condições Atuais:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                        Text("❤️ Vida Atual: ${hero.currentHp} / ${hero.totalMaxHp}", color = TextPrimary)
                        Text("💙 Mana Atual: ${hero.currentMp} / ${hero.totalMaxMp}", color = TextPrimary)
                        Text("💰 Custo do Pernoite: 10 Moedas de Ouro", color = GoldLight, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = onRest,
                enabled = hero.gold >= 10,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_tavern_rest")
            ) {
                Text(
                    text = "🛌 Descansar na Taverna (10 Ouro)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuestBoardScreen(
    hero: Hero,
    quests: List<Quest>,
    onBack: () -> Unit,
    onClaimReward: (String) -> Unit
) {
    Scaffold(
        topBar = {
            RpgTopBar(
                hero = hero,
                title = "Quadro de Missões",
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
            item {
                Text(
                    text = "Recompensas e Bounties de Valoria",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
                Text(
                    text = "Derrote monstros nas masmorras para cumprir os editais e resgatar ouro e prestígio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            items(quests) { quest ->
                val isDone = quest.isCompleted
                val isClaimed = quest.isClaimed

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 1.dp,
                            color = when {
                                isClaimed -> DungeonSurfaceBright
                                isDone -> GoldPrimary
                                else -> Color(0xFF4B4663)
                            },
                            shape = RoundedCornerShape(14.dp)
                        ),
                    color = DungeonSurfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = quest.titlePt,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDone && !isClaimed) GoldLight else TextPrimary
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isClaimed -> DungeonDark
                                    isDone -> HealthGreen.copy(alpha = 0.2f)
                                    else -> DungeonDark
                                }
                            ) {
                                Text(
                                    text = when {
                                        isClaimed -> "Concluída"
                                        isDone -> "Pronta!"
                                        else -> "${quest.currentCount} / ${quest.requiredCount}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isClaimed -> TextMuted
                                        isDone -> HealthGreen
                                        else -> TextSecondary
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = quest.descriptionPt,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        // Reward Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recompensa: +${quest.rewardGold} 💰  •  +${quest.rewardExp} XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldLight,
                                fontWeight = FontWeight.Bold
                            )

                            if (isDone && !isClaimed) {
                                Button(
                                    onClick = { onClaimReward(quest.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GoldPrimary,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_claim_quest_${quest.id}")
                                ) {
                                    Text("Coletar!", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
