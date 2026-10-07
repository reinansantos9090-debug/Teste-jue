package com.example.testejuerpg.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testejuerpg.R
import com.example.testejuerpg.model.*
import com.example.testejuerpg.ui.components.HealthBar
import com.example.testejuerpg.ui.components.ManaBar
import com.example.testejuerpg.ui.components.getRarityColor
import com.example.testejuerpg.ui.theme.*

@Composable
fun CombatScreen(
    hero: Hero,
    combatState: CombatState,
    onHeroAttack: () -> Unit,
    onHeroSkill: (Skill) -> Unit,
    onHeroDefend: () -> Unit,
    onHeroUsePotion: (Item) -> Unit,
    onHeroFlee: () -> Unit,
    onFinishVictory: () -> Unit,
    onReviveDefeat: () -> Unit
) {
    var showSkillsSheet by remember { mutableStateOf(false) }
    var showItemsSheet by remember { mutableStateOf(false) }

    val logListState = rememberLazyListState()

    // Auto-scroll combat log to bottom
    LaunchedEffect(combatState.combatLogs.size) {
        if (combatState.combatLogs.isNotEmpty()) {
            logListState.animateScrollToItem(combatState.combatLogs.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DungeonDark)
    ) {
        // Battle Arena Background
        Image(
            painter = painterResource(id = R.drawable.img_dungeon_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.25f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP: Monster Display & Status
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DungeonSurface.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (combatState.activeMonster.isBoss) RubyPrimary else GoldPrimary.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(combatState.activeMonster.iconEmoji, fontSize = 36.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = combatState.activeMonster.namePt,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (combatState.activeMonster.isBoss) RubyLight else TextPrimary
                                    )
                                    if (combatState.activeMonster.isBoss) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = RubyDark
                                        ) {
                                            Text(
                                                "CHEFE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Nível ${combatState.activeMonster.level}  •  Defesa ${combatState.activeMonster.defense}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (combatState.monsterIsDefending) {
                            Surface(
                                shape = CircleShape,
                                color = GoldDark
                            ) {
                                Text("🛡️", modifier = Modifier.padding(6.dp), fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    HealthBar(
                        current = combatState.activeMonster.currentHp,
                        max = combatState.activeMonster.maxHp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // MIDDLE: Combat Action Log
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DungeonSurfaceVariant.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DungeonSurfaceBright),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
            ) {
                LazyColumn(
                    state = logListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(combatState.combatLogs) { logEntry ->
                        val textColor = when {
                            logEntry.isCritical -> GoldLight
                            logEntry.isHeal -> HealthGreen
                            logEntry.isHeroAction -> ManaBlue
                            else -> RubyLight
                        }
                        Text(
                            text = logEntry.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor,
                            fontWeight = if (logEntry.isCritical) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // LOWER: Hero Status
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DungeonSurface.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(hero.classType.iconEmoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${hero.name} (Nv. ${hero.level})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight
                                )
                                Text(
                                    text = "Atq: ${hero.totalAttack}  •  Def: ${hero.totalDefense}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (combatState.isDefending) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GoldDark
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🛡️", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Defendendo", style = MaterialTheme.typography.labelSmall, color = GoldLight)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HealthBar(
                        current = hero.currentHp,
                        max = hero.totalMaxHp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ManaBar(
                        current = hero.currentMp,
                        max = hero.totalMaxMp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // BOTTOM: Combat Action Controls
            val canAct = combatState.turn == CombatTurn.HERO && !combatState.isVictory && !combatState.isDefeat

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onHeroAttack,
                        enabled = canAct,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RubyPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_combat_attack")
                    ) {
                        Text("⚔️ Atacar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showSkillsSheet = true },
                        enabled = canAct,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ManaBlue,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_combat_skills")
                    ) {
                        Text("✨ Magias", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onHeroDefend,
                        enabled = canAct,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DungeonSurfaceBright,
                            contentColor = GoldLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_combat_defend")
                    ) {
                        Text("🛡️ Defender", style = MaterialTheme.typography.labelLarge)
                    }

                    FilledTonalButton(
                        onClick = { showItemsSheet = true },
                        enabled = canAct,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DungeonSurfaceBright,
                            contentColor = HealthGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_combat_items")
                    ) {
                        Text("🧪 Itens", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = onHeroFlee,
                        enabled = canAct && !combatState.activeMonster.isBoss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_combat_flee")
                    ) {
                        Text("🏃 Fugir", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    }
                }
            }
        }

        // Skills Dialog
        if (showSkillsSheet) {
            AlertDialog(
                onDismissRequest = { showSkillsSheet = false },
                title = {
                    Text("Habilidades & Feitiços", style = MaterialTheme.typography.titleMedium, color = GoldLight)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        hero.skills.forEach { skill ->
                            val canCast = hero.currentMp >= skill.manaCost
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, if (canCast) ManaBlue.copy(alpha = 0.5f) else DungeonSurfaceBright, RoundedCornerShape(10.dp))
                                    .clickable(enabled = canCast) {
                                        showSkillsSheet = false
                                        onHeroSkill(skill)
                                    }
                                    .testTag("btn_cast_skill_${skill.id}"),
                                color = if (canCast) DungeonSurfaceVariant else DungeonSurface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(skill.iconEmoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                skill.namePt,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (canCast) TextPrimary else TextMuted
                                            )
                                            Text(
                                                skill.descriptionPt,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                    Text(
                                        "${skill.manaCost} MP",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canCast) ManaBlue else RubyLight
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSkillsSheet = false }) {
                        Text("Fechar", color = TextSecondary)
                    }
                },
                containerColor = DungeonSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Potions / Consumables Dialog
        if (showItemsSheet) {
            val consumables = hero.inventory.filter { it.type == ItemType.CONSUMABLE }
            AlertDialog(
                onDismissRequest = { showItemsSheet = false },
                title = {
                    Text("Bolsa de Poções", style = MaterialTheme.typography.titleMedium, color = HealthGreen)
                },
                text = {
                    if (consumables.isEmpty()) {
                        Text(
                            "Você não possui poções utilizáveis na mochila.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            consumables.forEach { item ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            showItemsSheet = false
                                            onHeroUsePotion(item)
                                        }
                                        .testTag("btn_combat_use_${item.id}"),
                                    color = DungeonSurfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(item.iconEmoji, fontSize = 24.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    item.namePt,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    item.descriptionPt,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            "x${item.quantity}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showItemsSheet = false }) {
                        Text("Fechar", color = TextSecondary)
                    }
                },
                containerColor = DungeonSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // VICTORY DIALOG
        if (combatState.isVictory) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        "🏆 Vitória Gloriosa!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldLight,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "${combatState.activeMonster.namePt} foi derrotado com bravura!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DungeonSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Experiência Ganha:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text("+${combatState.expEarned} XP", color = GoldLight, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ouro Encontrado:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text("+${combatState.goldEarned} 💰", color = GoldLight, fontWeight = FontWeight.Bold)
                                }
                                if (combatState.droppedItem != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Espólio Raro:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "${combatState.droppedItem.iconEmoji} ${combatState.droppedItem.namePt}",
                                            color = getRarityColor(combatState.droppedItem.rarity),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onFinishVictory,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_victory_continue")
                    ) {
                        Text("Coletar Espólios e Continuar", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = DungeonSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // DEFEAT DIALOG
        if (combatState.isDefeat) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        "💀 Derrota...",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RubyLight
                    )
                },
                text = {
                    Text(
                        "Seus pontos de vida chegaram a zero. Um grupo de aldeões de Valoria conseguiu resgatar você antes que o pior acontecesse.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onReviveDefeat,
                        colors = ButtonDefaults.buttonColors(containerColor = RubyPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_defeat_revive")
                    ) {
                        Text("Retornar à Taverna", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = DungeonSurface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
