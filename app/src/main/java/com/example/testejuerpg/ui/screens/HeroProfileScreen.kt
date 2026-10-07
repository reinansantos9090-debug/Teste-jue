package com.example.testejuerpg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testejuerpg.model.Hero
import com.example.testejuerpg.ui.components.ExperienceBar
import com.example.testejuerpg.ui.components.HealthBar
import com.example.testejuerpg.ui.components.ManaBar
import com.example.testejuerpg.ui.components.RpgTopBar
import com.example.testejuerpg.ui.theme.*

@Composable
fun HeroProfileScreen(
    hero: Hero,
    onBack: () -> Unit,
    onAllocateStat: (String) -> Unit
) {
    Scaffold(
        topBar = {
            RpgTopBar(
                hero = hero,
                title = "Ficha do Herói",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DungeonSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(DungeonDark)
                                    .border(2.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(hero.classType.iconEmoji, fontSize = 28.sp)
                            }
                            Column {
                                Text(
                                    text = hero.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight
                                )
                                Text(
                                    text = "Nível ${hero.level}  •  ${hero.classType.titlePt}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        ExperienceBar(
                            current = hero.experience,
                            max = hero.expToNextLevel
                        )
                    }
                }
            }

            // Stat Points Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Atributos Principais",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                    if (hero.availableStatPoints > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RubyPrimary
                        ) {
                            Text(
                                text = "Pontos Livres: ${hero.availableStatPoints}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatRow(
                        name = "Força",
                        description = "Aumenta o ataque físico",
                        value = hero.strength,
                        canAdd = hero.availableStatPoints > 0,
                        onAdd = { onAllocateStat("força") }
                    )
                    StatRow(
                        name = "Destreza",
                        description = "Aumenta chance de crítico e fuga",
                        value = hero.agility,
                        canAdd = hero.availableStatPoints > 0,
                        onAdd = { onAllocateStat("destreza") }
                    )
                    StatRow(
                        name = "Inteligência",
                        description = "Aumenta poder de feitiços e MP",
                        value = hero.intellect,
                        canAdd = hero.availableStatPoints > 0,
                        onAdd = { onAllocateStat("inteligência") }
                    )
                    StatRow(
                        name = "Vitalidade",
                        description = "Aumenta defesa e HP total",
                        value = hero.vitality,
                        canAdd = hero.availableStatPoints > 0,
                        onAdd = { onAllocateStat("vitalidade") }
                    )
                }
            }

            // Combat Stats Overview
            item {
                Text(
                    text = "Estatísticas de Batalha",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DungeonSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CombatStatItem("⚔️ Ataque Total", "${hero.totalAttack}")
                        CombatStatItem("🛡️ Defesa Total", "${hero.totalDefense}")
                        CombatStatItem("💥 Chance de Crítico", "${hero.critChancePercent}%")
                        CombatStatItem("⚡ Velocidade", "${hero.speed}")
                        CombatStatItem("❤️ Vida Máxima", "${hero.totalMaxHp}")
                        CombatStatItem("💙 Mana Máxima", "${hero.totalMaxMp}")
                    }
                }
            }

            // Skills & Magic Library
            item {
                Text(
                    text = "Grimório de Habilidades",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
            }

            items(hero.skills) { skill ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DungeonSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(skill.iconEmoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = skill.namePt,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = skill.descriptionPt,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = "${skill.manaCost} MP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ManaBlue
                        )
                    }
                }
            }

            // Adventure Stats
            item {
                Text(
                    text = "Registros de Valoria",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DungeonSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CombatStatItem("💀 Monstros Derrotados", "${hero.monstersSlain}")
                        CombatStatItem("👹 Chefes Eliminados", "${hero.bossesSlain}")
                        CombatStatItem("🏰 Masmorras Concluídas", "${hero.dungeonsCleared}")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    name: String,
    description: String,
    value: Int,
    canAdd: Boolean,
    onAdd: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DungeonSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(description, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$value",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
                if (canAdd) {
                    Spacer(modifier = Modifier.width(10.dp))
                    FilledIconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("btn_add_stat_$name"),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = GoldPrimary)
                    ) {
                        Text("+", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CombatStatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
