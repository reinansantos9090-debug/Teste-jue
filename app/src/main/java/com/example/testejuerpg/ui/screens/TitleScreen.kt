package com.example.testejuerpg.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.testejuerpg.ui.theme.*

@Composable
fun TitleScreen(
    hasSavedGame: Boolean,
    onNewGame: () -> Unit,
    onContinueGame: () -> Unit
) {
    var showLoreDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DungeonDark)
    ) {
        // Background Image with Dark Vignette Gradient
        Image(
            painter = painterResource(id = R.drawable.img_dungeon_bg),
            contentDescription = "Cenário de Masmorra",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.35f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DungeonDark.copy(alpha = 0.7f),
                            Color.Transparent,
                            DungeonDark.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Main Title & Emblem
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(DungeonSurfaceVariant)
                        .border(2.dp, GoldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_icon),
                        contentDescription = "Emblema do Jogo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "TESTE-JUE RPG",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldLight,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "⚔️ As Lendas de Valoria ⚔️",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DungeonSurface.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Aventura Clássica em Turnos",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hasSavedGame) {
                    Button(
                        onClick = onContinueGame,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("btn_continue_game"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            text = "▶  Continuar Jornada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onNewGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_new_game"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (hasSavedGame) DungeonSurfaceVariant else GoldPrimary,
                        contentColor = if (hasSavedGame) GoldLight else Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = if (hasSavedGame) androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)) else null
                ) {
                    Text(
                        text = if (hasSavedGame) "✨ Novo Herói" else "⚔️ Iniciar Nova Aventura",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { showLoreDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_about_game"),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4B4663))
                ) {
                    Text(
                        text = "📜 História & Regras",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }

            Text(
                text = "v1.0.0 • Teste-jue RPG Engine",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }

    if (showLoreDialog) {
        AlertDialog(
            onDismissRequest = { showLoreDialog = false },
            title = {
                Text(
                    text = "Crônicas de Valoria",
                    style = MaterialTheme.typography.titleLarge,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "O reino de Valoria está sob cerco. Criaturas sombrias emergiram das florestas ancestrais e catacumbas esquecidas lideradas pelo Lorde Malakar e o Dragão Ignis.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Batalhas em Turnos: Escolha seus ataques, magias, postura defensiva ou poções com sabedoria.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "• Exploração de Masmorras: Encontre baús de tesouros, altares mágicos e enfrente chefes colossais.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "• Evolução e Equipamentos: Ganhe EXP para subir de nível, distribua pontos de atributos e compre armas e armaduras no ferreiro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLoreDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DungeonSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
