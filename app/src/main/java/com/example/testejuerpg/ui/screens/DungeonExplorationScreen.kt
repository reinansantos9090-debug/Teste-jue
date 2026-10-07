package com.example.testejuerpg.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.testejuerpg.model.Dungeon
import com.example.testejuerpg.model.DungeonRun
import com.example.testejuerpg.model.Hero
import com.example.testejuerpg.model.RoomType
import com.example.testejuerpg.ui.components.RpgTopBar
import com.example.testejuerpg.ui.theme.*

@Composable
fun DungeonSelectScreen(
    hero: Hero,
    dungeons: List<Dungeon>,
    onBack: () -> Unit,
    onSelectDungeon: (Dungeon) -> Unit
) {
    Scaffold(
        topBar = {
            RpgTopBar(
                hero = hero,
                title = "Portão das Masmorras",
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
            item {
                Text(
                    text = "Escolha seu Destino",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight
                )
                Text(
                    text = "Aventure-se pelas profundezas para obter espólios raros e glória eterna.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            items(dungeons) { dungeon ->
                val isRecommended = hero.level >= dungeon.recommendedLevel

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = if (isRecommended) GoldPrimary.copy(alpha = 0.5f) else RubyPrimary.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectDungeon(dungeon) }
                        .testTag("dungeon_card_${dungeon.id}"),
                    color = DungeonSurfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(dungeon.iconEmoji, fontSize = 32.sp)
                                Column {
                                    Text(
                                        text = dungeon.namePt,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldLight
                                    )
                                    Text(
                                        text = "Salas: ${dungeon.totalRooms}  •  Chefe: ${dungeon.boss.namePt}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isRecommended) HealthGreen.copy(alpha = 0.2f) else RubyDark.copy(alpha = 0.3f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isRecommended) HealthGreen else RubyPrimary
                                )
                            ) {
                                Text(
                                    text = "Nv. ${dungeon.recommendedLevel}+",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRecommended) HealthGreen else RubyLight,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = dungeon.descriptionPt,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { onSelectDungeon(dungeon) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_enter_dungeon_${dungeon.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecommended) GoldPrimary else RubyPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "⚔️ Adentrar ${dungeon.namePt}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DungeonExploreScreen(
    hero: Hero,
    dungeonRun: DungeonRun,
    onOpenChest: () -> Unit,
    onPrayShrine: () -> Unit,
    onDisarmTrap: (force: Boolean) -> Unit,
    onNextRoom: () -> Unit,
    onFleeToTown: () -> Unit
) {
    Scaffold(
        topBar = {
            RpgTopBar(
                hero = hero,
                title = dungeonRun.dungeon.namePt
            )
        },
        containerColor = DungeonDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_dungeon_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.3f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Room progress indicator
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DungeonSurface.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sala ${dungeonRun.currentRoomIndex} de ${dungeonRun.dungeon.totalRooms}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { dungeonRun.currentRoomIndex.toFloat() / dungeonRun.dungeon.totalRooms.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = GoldPrimary,
                            trackColor = DungeonDark
                        )
                    }
                }

                // Room Content Center Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DungeonSurfaceVariant.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (dungeonRun.currentRoomType) {
                            RoomType.TREASURE -> {
                                Text("🎁", fontSize = 56.sp)
                                Text(
                                    text = "Baú de Tesouro Encontrado!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Um antigo baú de carvalho ornado em latão repousa no centro da câmara.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onOpenChest,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_open_chest")
                                ) {
                                    Text("Abrir Baú", fontWeight = FontWeight.Bold)
                                }
                            }
                            RoomType.SHRINE -> {
                                Text("✨", fontSize = 56.sp)
                                Text(
                                    text = "Altar Místico de Valoria",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ManaBlue,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "A estátua de uma divindade ancestral emana bênçãos restauradoras de Vida e Mana.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onPrayShrine,
                                    colors = ButtonDefaults.buttonColors(containerColor = ManaBlue, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_pray_shrine")
                                ) {
                                    Text("Rezar no Altar (Restaurar HP/MP)", fontWeight = FontWeight.Bold)
                                }
                            }
                            RoomType.TRAP -> {
                                Text("⚠️", fontSize = 56.sp)
                                Text(
                                    text = "Armadilha de Espinhos!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = RubyLight,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "O piso está repleto de placas de pressão ligadas a estacas afiadas.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onDisarmTrap(false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("btn_disarm_trap")
                                    ) {
                                        Text("Desarmar (Destreza)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onDisarmTrap(true) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("btn_rush_trap")
                                    ) {
                                        Text("Correr", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            else -> {
                                Text("🚪", fontSize = 56.sp)
                                Text(
                                    text = "Câmara Segura",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "O caminho à frente está desimpedido. Prepare-se para o próximo desafio.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onNextRoom,
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_proceed_room")
                                ) {
                                    Text("Avançar para Próxima Sala ➔", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Bottom Retreat action
                OutlinedButton(
                    onClick = onFleeToTown,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_flee_dungeon")
                ) {
                    Text("🏃 Retornar a Valoria com os Espólios", color = TextSecondary)
                }
            }
        }
    }
}
