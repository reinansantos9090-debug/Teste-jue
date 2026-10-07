package com.example.testejuerpg.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testejuerpg.R
import com.example.testejuerpg.model.Hero
import com.example.testejuerpg.ui.components.ExperienceBar
import com.example.testejuerpg.ui.components.HealthBar
import com.example.testejuerpg.ui.components.ManaBar
import com.example.testejuerpg.ui.components.RpgTopBar
import com.example.testejuerpg.ui.theme.*

private data class TownAction(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val accentColor: Color,
    val isPrimary: Boolean = false
)

@Composable
fun TownHubScreen(
    hero: Hero,
    onNavigateDungeons: () -> Unit,
    onNavigateInventory: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateShop: () -> Unit,
    onNavigateBlacksmith: () -> Unit,
    onNavigateTavern: () -> Unit,
    onNavigateQuests: () -> Unit,
    onExitToTitle: () -> Unit
) {
    val townActions = listOf(
        TownAction(
            id = "dungeons",
            title = "Portão da Masmorra",
            subtitle = "Adentrar cavernas e ruínas",
            iconEmoji = "🏰",
            accentColor = RubyPrimary,
            isPrimary = true
        ),
        TownAction(
            id = "tavern",
            title = "Taverna do Javali",
            subtitle = "Descanse e recupere Vida e Mana",
            iconEmoji = "🍺",
            accentColor = GoldPrimary
        ),
        TownAction(
            id = "quests",
            title = "Quadro de Missões",
            subtitle = "Recompensas e caçadas",
            iconEmoji = "📜",
            accentColor = Color(0xFF60A5FA)
        ),
        TownAction(
            id = "blacksmith",
            title = "Ferreiro Armeiro",
            subtitle = "Armas, escudos e armaduras",
            iconEmoji = "🗡️",
            accentColor = Color(0xFFF97316)
        ),
        TownAction(
            id = "alchemy",
            title = "Loja de Alquimia",
            subtitle = "Poções de vida e elixires",
            iconEmoji = "🧪",
            accentColor = HealthGreen
        ),
        TownAction(
            id = "inventory",
            title = "Mochila do Herói",
            subtitle = "Equipar itens e conferir espólios",
            iconEmoji = "🎒",
            accentColor = Color(0xFFA78BFA)
        ),
        TownAction(
            id = "profile",
            title = "Atributos & Habilidades",
            subtitle = "Distribuir pontos e magias",
            iconEmoji = "👤",
            accentColor = ManaBlue
        ),
        TownAction(
            id = "exit",
            title = "Menu Principal",
            subtitle = "Salvar e voltar ao início",
            iconEmoji = "🚪",
            accentColor = TextSecondary
        )
    )

    Scaffold(
        topBar = {
            RpgTopBar(
                hero = hero,
                title = "Vila de Valoria",
                onOpenInventory = onNavigateInventory,
                onOpenProfile = onNavigateProfile
            )
        },
        containerColor = DungeonDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scenic Town Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_town_bg),
                    contentDescription = "Vila de Valoria",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    DungeonDark.copy(alpha = 0.95f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Vila de Valoria",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldLight
                    )
                    Text(
                        text = "Refúgio dos corajosos aventureiros",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Hero Quick Status Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                color = DungeonSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(hero.classType.iconEmoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${hero.name} (Nv. ${hero.level} ${hero.classType.titlePt})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldLight
                            )
                        }
                        if (hero.availableStatPoints > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = RubyDark
                            ) {
                                Text(
                                    text = "+${hero.availableStatPoints} Pontos!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    ExperienceBar(
                        current = hero.experience,
                        max = hero.expToNextLevel
                    )
                }
            }

            // Town Hub Grid Actions
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(townActions) { action ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (action.isPrimary) 2.dp else 1.dp,
                                color = if (action.isPrimary) RubyPrimary else action.accentColor.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                when (action.id) {
                                    "dungeons" -> onNavigateDungeons()
                                    "tavern" -> onNavigateTavern()
                                    "quests" -> onNavigateQuests()
                                    "blacksmith" -> onNavigateBlacksmith()
                                    "alchemy" -> onNavigateShop()
                                    "inventory" -> onNavigateInventory()
                                    "profile" -> onNavigateProfile()
                                    "exit" -> onExitToTitle()
                                }
                            }
                            .testTag("town_action_${action.id}"),
                        color = if (action.isPrimary) Color(0xFF261217) else DungeonSurfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(action.iconEmoji, fontSize = 28.sp)
                                if (action.isPrimary) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = RubyPrimary
                                    ) {
                                        Text(
                                            "Batalha",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = action.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (action.isPrimary) RubyLight else TextPrimary
                            )

                            Text(
                                text = action.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
