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
import com.example.testejuerpg.data.RpgCatalog
import com.example.testejuerpg.model.HeroClassType
import com.example.testejuerpg.ui.components.RpgTopBar
import com.example.testejuerpg.ui.theme.*

@Composable
fun CharacterCreationScreen(
    onBack: () -> Unit,
    onCreateHero: (name: String, classType: HeroClassType) -> Unit
) {
    var heroName by remember { mutableStateOf("Valerius") }
    var selectedClass by remember { mutableStateOf(HeroClassType.GUERREIRO) }

    Scaffold(
        topBar = {
            RpgTopBar(
                hero = null,
                title = "Criação de Herói",
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name Input
                item {
                    Text(
                        text = "Nome do Personagem",
                        style = MaterialTheme.typography.titleSmall,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = heroName,
                        onValueChange = { if (it.length <= 18) heroName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_hero_name"),
                        placeholder = { Text("Digite o nome...", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = DungeonSurfaceBright,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DungeonSurfaceVariant,
                            unfocusedContainerColor = DungeonSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Choose Class
                item {
                    Text(
                        text = "Escolha sua Classe",
                        style = MaterialTheme.typography.titleSmall,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(HeroClassType.entries.toTypedArray()) { classType ->
                    val isSelected = selectedClass == classType
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) GoldPrimary else DungeonSurfaceBright,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedClass = classType }
                            .testTag("class_card_${classType.name}"),
                        color = if (isSelected) DungeonSurfaceVariant else DungeonSurface,
                        tonalElevation = if (isSelected) 6.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(classType.iconEmoji, fontSize = 28.sp)
                                    Column {
                                        Text(
                                            text = classType.titlePt,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) GoldLight else TextPrimary
                                        )
                                        Text(
                                            text = "Foco: ${classType.primaryStat}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedClass = classType },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = GoldPrimary,
                                        unselectedColor = TextMuted
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = classType.descriptionPt,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Base Stats Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatChip(label = "HP", value = "${classType.baseHp}", color = RubyLight)
                                StatChip(label = "MP", value = "${classType.baseMp}", color = ManaBlue)
                                StatChip(label = "Atq", value = "${classType.baseAttack}", color = GoldLight)
                                StatChip(label = "Def", value = "${classType.baseDefense}", color = Color(0xFFA78BFA))
                                StatChip(label = "Vel", value = "${classType.baseSpeed}", color = HealthGreen)
                            }

                            // Skills preview
                            Spacer(modifier = Modifier.height(10.dp))
                            val skills = RpgCatalog.getSkillsForClass(classType)
                            Text(
                                text = "Habilidades: " + skills.joinToString(", ") { "${it.iconEmoji} ${it.namePt}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onCreateHero(heroName, selectedClass) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_confirm_hero"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "⚔️ Começar com ${selectedClass.titlePt}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DungeonDark.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
