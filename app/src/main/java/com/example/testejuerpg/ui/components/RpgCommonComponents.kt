package com.example.testejuerpg.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testejuerpg.model.Hero
import com.example.testejuerpg.model.Item
import com.example.testejuerpg.model.ItemRarity
import com.example.testejuerpg.model.ItemType
import com.example.testejuerpg.ui.theme.*

@Composable
fun HealthBar(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier,
    height: Int = 14,
    showLabel: Boolean = true
) {
    val progress = (current.toFloat() / max.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "hp_anim")

    Column(modifier = modifier) {
        if (showLabel) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HP (Vida)",
                    style = MaterialTheme.typography.labelSmall,
                    color = RubyLight,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$current / $max",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2B1114))
                .border(1.dp, RubyDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        Brush.horizontalGradient(
                            listOf(RubyPrimary, Color(0xFFEF4444), RubyLight)
                        )
                    )
            )
        }
    }
}

@Composable
fun ManaBar(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier,
    height: Int = 12,
    showLabel: Boolean = true
) {
    val progress = (current.toFloat() / max.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "mp_anim")

    Column(modifier = modifier) {
        if (showLabel) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MP (Mana)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ManaBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$current / $max",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F1E2E))
                .border(1.dp, ManaDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        Brush.horizontalGradient(
                            listOf(ManaDark, ManaBlue, Color(0xFFBAE6FD))
                        )
                    )
            )
        }
    }
}

@Composable
fun ExperienceBar(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier
) {
    val progress = (current.toFloat() / max.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "exp_anim")

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "EXP (Experiência)",
                style = MaterialTheme.typography.labelSmall,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$current / $max XP",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF261D11))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        Brush.horizontalGradient(
                            listOf(GoldDark, GoldPrimary, GoldLight)
                        )
                    )
            )
        }
    }
}

@Composable
fun RpgTopBar(
    hero: Hero?,
    title: String,
    onBack: (() -> Unit)? = null,
    onOpenInventory: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DungeonSurface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("btn_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = GoldPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                }

                if (hero != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DungeonSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💰", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${hero.gold}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight
                                )
                            }
                        }

                        if (onOpenInventory != null) {
                            FilledTonalIconButton(
                                onClick = onOpenInventory,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_top_inventory")
                            ) {
                                Text("🎒", fontSize = 16.sp)
                            }
                        }

                        if (onOpenProfile != null) {
                            FilledTonalIconButton(
                                onClick = onOpenProfile,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("btn_top_profile")
                            ) {
                                Text(hero.classType.iconEmoji, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            if (hero != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HealthBar(
                        current = hero.currentHp,
                        max = hero.totalMaxHp,
                        modifier = Modifier.weight(1f),
                        height = 10,
                        showLabel = false
                    )
                    ManaBar(
                        current = hero.currentMp,
                        max = hero.totalMaxMp,
                        modifier = Modifier.weight(1f),
                        height = 10,
                        showLabel = false
                    )
                }
            }
        }
    }
}

fun getRarityColor(rarity: ItemRarity): Color {
    return when (rarity) {
        ItemRarity.COMMON -> RarityCommon
        ItemRarity.RARE -> RarityRare
        ItemRarity.EPIC -> RarityEpic
        ItemRarity.LEGENDARY -> RarityLegendary
    }
}

@Composable
fun RpgItemCard(
    item: Item,
    onActionClick: (() -> Unit)? = null,
    actionButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    modifier: Modifier = Modifier
) {
    val rarityColor = getRarityColor(item.rarity)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, rarityColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        color = DungeonSurfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item Icon Box
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(rarityColor.copy(alpha = 0.15f))
                    .border(1.dp, rarityColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.iconEmoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.namePt,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.quantity > 1) {
                        Surface(
                            shape = CircleShape,
                            color = GoldDark
                        ) {
                            Text(
                                text = "x${item.quantity}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = item.descriptionPt,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Stats row
                val stats = mutableListOf<String>()
                if (item.bonusAttack > 0) stats.add("⚔️ +${item.bonusAttack} Atq")
                if (item.bonusDefense > 0) stats.add("🛡️ +${item.bonusDefense} Def")
                if (item.bonusHp > 0) stats.add("❤️ +${item.bonusHp} HP")
                if (item.bonusMp > 0) stats.add("💙 +${item.bonusMp} MP")
                if (item.healHp > 0) stats.add("🧪 Cura +${item.healHp} HP")
                if (item.healMp > 0) stats.add("🧪 Mana +${item.healMp} MP")

                if (stats.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stats.joinToString("  "),
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onActionClick != null && actionButtonText != null) {
                    Button(
                        onClick = onActionClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (item.type == ItemType.CONSUMABLE) HealthGreen else GoldPrimary,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_item_action_${item.id}")
                    ) {
                        Text(actionButtonText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                if (onSecondaryClick != null && secondaryButtonText != null) {
                    OutlinedButton(
                        onClick = onSecondaryClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_item_secondary_${item.id}")
                    ) {
                        Text(secondaryButtonText, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
