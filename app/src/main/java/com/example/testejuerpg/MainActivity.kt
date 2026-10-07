package com.example.testejuerpg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testejuerpg.model.GameScreen
import com.example.testejuerpg.ui.screens.*
import com.example.testejuerpg.ui.theme.DungeonDark
import com.example.testejuerpg.ui.theme.TestejueRPGTheme
import com.example.testejuerpg.viewmodel.RpgViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestejueRPGTheme {
                RpgMainApp()
            }
        }
    }
}

@Composable
fun RpgMainApp(viewModel: RpgViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusNotification) {
        val notif = uiState.statusNotification
        if (notif != null) {
            snackbarHostState.showSnackbar(notif, duration = SnackbarDuration.Short)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DungeonDark,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DungeonDark)
        ) {
            when (uiState.currentScreen) {
                GameScreen.TITLE -> {
                    TitleScreen(
                        hasSavedGame = uiState.hasSavedGame,
                        onNewGame = { viewModel.navigateTo(GameScreen.CHARACTER_CREATION) },
                        onContinueGame = { viewModel.continueGame() }
                    )
                }

                GameScreen.CHARACTER_CREATION -> {
                    BackHandler { viewModel.navigateTo(GameScreen.TITLE) }
                    CharacterCreationScreen(
                        onBack = { viewModel.navigateTo(GameScreen.TITLE) },
                        onCreateHero = { name, classType ->
                            viewModel.startNewGame(name, classType)
                        }
                    )
                }

                GameScreen.TOWN_HUB -> {
                    val hero = uiState.hero
                    if (hero != null) {
                        BackHandler { viewModel.navigateTo(GameScreen.TITLE) }
                        TownHubScreen(
                            hero = hero,
                            onNavigateDungeons = { viewModel.navigateTo(GameScreen.DUNGEON_SELECT) },
                            onNavigateInventory = { viewModel.navigateTo(GameScreen.INVENTORY) },
                            onNavigateProfile = { viewModel.navigateTo(GameScreen.HERO_PROFILE) },
                            onNavigateShop = { viewModel.navigateTo(GameScreen.SHOP) },
                            onNavigateBlacksmith = { viewModel.navigateTo(GameScreen.BLACKSMITH) },
                            onNavigateTavern = { viewModel.navigateTo(GameScreen.TAVERN) },
                            onNavigateQuests = { viewModel.navigateTo(GameScreen.QUEST_BOARD) },
                            onExitToTitle = { viewModel.navigateTo(GameScreen.TITLE) }
                        )
                    } else {
                        viewModel.navigateTo(GameScreen.TITLE)
                    }
                }

                GameScreen.DUNGEON_SELECT -> {
                    val hero = uiState.hero
                    if (hero != null) {
                        BackHandler { viewModel.navigateTo(GameScreen.TOWN_HUB) }
                        DungeonSelectScreen(
                            hero = hero,
                            dungeons = uiState.availableDungeons,
                            onBack = { viewModel.navigateTo(GameScreen.TOWN_HUB) },
                            onSelectDungeon = { dungeon -> viewModel.startDungeon(dungeon) }
                        )
                    }
                }

                GameScreen.DUNGEON_EXPLORE -> {
                    val hero = uiState.hero
                    val run = uiState.currentDungeonRun
                    if (hero != null && run != null) {
                        BackHandler { viewModel.fleeDungeon() }
                        DungeonExploreScreen(
                            hero = hero,
                            dungeonRun = run,
                            onOpenChest = { viewModel.openTreasureChest() },
                            onPrayShrine = { viewModel.prayAtShrine() },
                            onDisarmTrap = { force -> viewModel.disarmTrap(force) },
                            onNextRoom = { viewModel.advanceDungeonRoom() },
                            onFleeToTown = { viewModel.fleeDungeon() }
                        )
                    }
                }

                GameScreen.COMBAT -> {
                    val hero = uiState.hero
                    val combat = uiState.combatState
                    if (hero != null && combat != null) {
                        BackHandler {
                            if (!combat.activeMonster.isBoss) {
                                viewModel.heroFleeCombat()
                            }
                        }
                        CombatScreen(
                            hero = hero,
                            combatState = combat,
                            onHeroAttack = { viewModel.heroAttack() },
                            onHeroSkill = { skill -> viewModel.heroSkill(skill) },
                            onHeroDefend = { viewModel.heroDefend() },
                            onHeroUsePotion = { potion -> viewModel.heroUsePotionInCombat(potion) },
                            onHeroFlee = { viewModel.heroFleeCombat() },
                            onFinishVictory = { viewModel.finishCombatVictory() },
                            onReviveDefeat = { viewModel.reviveHeroInTown() }
                        )
                    }
                }

                GameScreen.INVENTORY -> {
                    val hero = uiState.hero
                    if (hero != null) {
                        BackHandler { viewModel.navigateTo(GameScreen.TOWN_HUB) }
                        InventoryScreen(
                            hero = hero,
                            onBack = { viewModel.navigateTo(GameScreen.TOWN_HUB) },
                            onEquipItem = { item -> viewModel.equipItem(item) },
                            onUnequipSlot = { slot -> viewModel.unequipSlot(slot) },
                            onUseItem = { item -> viewModel.useItemOutsideCombat(item) }
                        )
                    }
                }

                GameScreen.HERO_PROFILE -> {
                    val hero = uiState.hero
                    if (hero != null) {
                        BackHandler { viewModel.navigateTo(GameScreen.TOWN_HUB) }
                        HeroProfileScreen(
                            hero = hero,
                            onBack = { viewModel.navigateTo(GameScreen.TOWN_HUB) },
                            onAllocateStat = { stat -> viewModel.allocateStat(stat) }
                        )
                    }
                }

                GameScreen.SHOP -> {
                    val hero = uiState.hero
                    if (hero != null) {
                        BackHandler { viewModel.navigateTo(GameScreen.TOWN_HUB) }
                        ShopScreen(
                            hero = hero,
                            catalog = uiState.shopCatalog,
                            isBlacksmith = false,
                            onBack = { viewModel.navigateTo(GameScreen.TOWN_HUB) },
                            onBuyItem = { item -> viewModel.buyShopItem(item) },
                            onSellItem = { item -> viewModel.sellInventoryItem(item) }
                        )
                    }
                }

                GameScreen.BLACKSMITH -> {
                    val hero = uiState.hero
                    if (hero != null) {
                        BackHandler { viewModel.navigateTo(GameScreen.TOWN_HUB) }
                        ShopScreen(
                            hero = hero,
                            catalog = uiState.shopCatalog,
                            isBlacksmith = true,
                            onBack = { viewModel.navigateTo(GameScreen.TOWN_HUB) },
                            onBuyItem = { item -> viewModel.buyShopItem(item) },
                            onSellItem = { item -> viewModel.sellInventoryItem(item) }
                        )
                    }
                }

                GameScreen.TAVERN -> {
                    val hero = uiState.hero
                    if (hero != null) {
                        BackHandler { viewModel.navigateTo(GameScreen.TOWN_HUB) }
                        TavernScreen(
                            hero = hero,
                            onBack = { viewModel.navigateTo(GameScreen.TOWN_HUB) },
                            onRest = { viewModel.restAtTavern() }
                        )
                    }
                }

                GameScreen.QUEST_BOARD -> {
                    val hero = uiState.hero
                    if (hero != null) {
                        BackHandler { viewModel.navigateTo(GameScreen.TOWN_HUB) }
                        QuestBoardScreen(
                            hero = hero,
                            quests = uiState.quests,
                            onBack = { viewModel.navigateTo(GameScreen.TOWN_HUB) },
                            onClaimReward = { id -> viewModel.claimQuestReward(id) }
                        )
                    }
                }
            }
        }
    }
}
