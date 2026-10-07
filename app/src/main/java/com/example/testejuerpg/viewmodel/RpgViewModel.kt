package com.example.testejuerpg.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import com.example.testejuerpg.data.RpgCatalog
import com.example.testejuerpg.data.RpgStorage
import com.example.testejuerpg.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class RpgUiState(
    val currentScreen: GameScreen = GameScreen.TITLE,
    val hero: Hero? = null,
    val hasSavedGame: Boolean = false,
    val availableDungeons: List<Dungeon> = RpgCatalog.dungeons,
    val currentDungeonRun: DungeonRun? = null,
    val combatState: CombatState? = null,
    val quests: List<Quest> = RpgCatalog.initialQuests,
    val shopCatalog: List<Item> = RpgCatalog.shopItems,
    val statusNotification: String? = null
)

class RpgViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = RpgStorage(application)

    private val _uiState = MutableStateFlow(
        RpgUiState(hasSavedGame = storage.hasSavedGame())
    )
    val uiState: StateFlow<RpgUiState> = _uiState.asStateFlow()

    init {
        checkSavedGame()
    }

    private fun checkSavedGame() {
        val hasSave = storage.hasSavedGame()
        _uiState.value = _uiState.value.copy(hasSavedGame = hasSave)
    }

    fun vibrate(durationMs: Long = 50, amplitude: Int = 100) {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {
            // Ignored if device lacks vibrator
        }
    }

    fun startNewGame(name: String, selectedClass: HeroClassType) {
        val heroName = name.ifBlank { "Aventureiro" }
        val startingGear = RpgCatalog.getStartingGear(selectedClass)
        val startingSkills = RpgCatalog.getSkillsForClass(selectedClass)

        val starterPotions = listOf(
            RpgCatalog.shopItems.first { it.id == "pot_hp_small" }.copy(quantity = 2),
            RpgCatalog.shopItems.first { it.id == "pot_mp_small" }.copy(quantity = 2)
        )

        val newHero = Hero(
            name = heroName,
            classType = selectedClass,
            level = 1,
            currentHp = selectedClass.baseHp,
            maxHp = selectedClass.baseHp,
            currentMp = selectedClass.baseMp,
            maxMp = selectedClass.baseMp,
            strength = if (selectedClass == HeroClassType.GUERREIRO) 14 else 10,
            agility = if (selectedClass == HeroClassType.LADINO) 15 else 10,
            intellect = if (selectedClass == HeroClassType.MAGO) 16 else 10,
            vitality = if (selectedClass == HeroClassType.GUERREIRO || selectedClass == HeroClassType.PALADINO) 13 else 10,
            availableStatPoints = 0,
            experience = 0,
            expToNextLevel = 100,
            gold = 50,
            equipment = startingGear,
            inventory = starterPotions,
            skills = startingSkills
        )

        _uiState.value = _uiState.value.copy(
            hero = newHero,
            currentScreen = GameScreen.TOWN_HUB,
            quests = RpgCatalog.initialQuests,
            statusNotification = "Bem-vindo a Valoria, ${newHero.name}!"
        )
        saveGame()
    }

    fun continueGame() {
        val saved = storage.loadHero()
        if (saved != null) {
            _uiState.value = _uiState.value.copy(
                hero = saved.first,
                quests = saved.second,
                currentScreen = GameScreen.TOWN_HUB,
                statusNotification = "Progresso carregado com sucesso!"
            )
        }
    }

    fun navigateTo(screen: GameScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun startDungeon(dungeon: Dungeon) {
        val hero = _uiState.value.hero ?: return
        val run = DungeonRun(
            dungeon = dungeon,
            currentRoomIndex = 1,
            currentRoomType = RoomType.COMBAT,
            roomHistory = listOf(RoomType.COMBAT),
            lootCollectedGold = 0,
            itemsCollected = emptyList()
        )

        val firstMonster = dungeon.enemyPool.random()
        val combat = CombatState(
            activeMonster = firstMonster,
            combatLogs = listOf(
                CombatLogEntry("Você entrou na ${dungeon.namePt}!", false),
                CombatLogEntry("Um ${firstMonster.namePt} bloqueia o seu caminho!", false)
            )
        )

        _uiState.value = _uiState.value.copy(
            currentDungeonRun = run,
            combatState = combat,
            currentScreen = GameScreen.COMBAT
        )
    }

    fun advanceDungeonRoom() {
        val currentRun = _uiState.value.currentDungeonRun ?: return
        val nextIndex = currentRun.currentRoomIndex + 1

        if (nextIndex > currentRun.dungeon.totalRooms) {
            // Dungeon Cleared!
            val hero = _uiState.value.hero ?: return
            val updatedHero = hero.copy(
                dungeonsCleared = hero.dungeonsCleared + 1,
                gold = hero.gold + currentRun.lootCollectedGold
            )
            _uiState.value = _uiState.value.copy(
                hero = updatedHero,
                currentDungeonRun = null,
                currentScreen = GameScreen.TOWN_HUB,
                statusNotification = "🎉 Masmorra ${currentRun.dungeon.namePt} Concluída com Sucesso!"
            )
            saveGame()
            return
        }

        // Room generator
        val roomType = when {
            nextIndex == currentRun.dungeon.totalRooms -> RoomType.BOSS
            Random.nextFloat() < 0.20f -> RoomType.TREASURE
            Random.nextFloat() < 0.15f -> RoomType.SHRINE
            Random.nextFloat() < 0.15f -> RoomType.TRAP
            else -> RoomType.COMBAT
        }

        val updatedRun = currentRun.copy(
            currentRoomIndex = nextIndex,
            currentRoomType = roomType,
            roomHistory = currentRun.roomHistory + roomType
        )

        when (roomType) {
            RoomType.COMBAT -> {
                val monster = currentRun.dungeon.enemyPool.random()
                val combat = CombatState(
                    activeMonster = monster,
                    combatLogs = listOf(
                        CombatLogEntry("Sala $nextIndex: Você encontrou um ${monster.namePt}!", false)
                    )
                )
                _uiState.value = _uiState.value.copy(
                    currentDungeonRun = updatedRun,
                    combatState = combat,
                    currentScreen = GameScreen.COMBAT
                )
            }
            RoomType.BOSS -> {
                val boss = currentRun.dungeon.boss
                val combat = CombatState(
                    activeMonster = boss,
                    combatLogs = listOf(
                        CombatLogEntry("⚔️ CÂMARA DO CHEFE! ${boss.namePt} desperta!", false, isCritical = true)
                    )
                )
                _uiState.value = _uiState.value.copy(
                    currentDungeonRun = updatedRun,
                    combatState = combat,
                    currentScreen = GameScreen.COMBAT
                )
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    currentDungeonRun = updatedRun,
                    currentScreen = GameScreen.DUNGEON_EXPLORE
                )
            }
        }
    }

    fun openTreasureChest() {
        val currentRun = _uiState.value.currentDungeonRun ?: return
        val hero = _uiState.value.hero ?: return

        val goldFound = Random.nextInt(25, 65)
        val possibleLoot = RpgCatalog.shopItems.filter { it.type != ItemType.CONSUMABLE || Random.nextBoolean() }
        val randomItem = possibleLoot.random().copy(quantity = 1)

        val updatedInventory = addToInventory(hero.inventory, randomItem)
        val updatedHero = hero.copy(
            gold = hero.gold + goldFound,
            inventory = updatedInventory
        )

        val updatedRun = currentRun.copy(
            lootCollectedGold = currentRun.lootCollectedGold + goldFound,
            itemsCollected = currentRun.itemsCollected + randomItem,
            currentRoomType = RoomType.EMPTY
        )

        vibrate(80, 150)
        _uiState.value = _uiState.value.copy(
            hero = updatedHero,
            currentDungeonRun = updatedRun,
            statusNotification = "🎁 Baú Aberto: Encontrou +$goldFound Ouro e ${randomItem.namePt}!"
        )
        saveGame()
    }

    fun prayAtShrine() {
        val currentRun = _uiState.value.currentDungeonRun ?: return
        val hero = _uiState.value.hero ?: return

        // Heal full HP and MP
        val updatedHero = hero.copy(
            currentHp = hero.totalMaxHp,
            currentMp = hero.totalMaxMp
        )

        val updatedRun = currentRun.copy(currentRoomType = RoomType.EMPTY)

        vibrate(60, 100)
        _uiState.value = _uiState.value.copy(
            hero = updatedHero,
            currentDungeonRun = updatedRun,
            statusNotification = "✨ O Altar Sagrado restaurou toda a sua Vida e Mana!"
        )
    }

    fun disarmTrap(forceThrough: Boolean = false) {
        val currentRun = _uiState.value.currentDungeonRun ?: return
        val hero = _uiState.value.hero ?: return

        val success = if (forceThrough) false else Random.nextInt(100) < (hero.agility * 4).coerceIn(30, 85)

        val message: String
        val updatedHero: Hero
        if (success) {
            message = "🎯 Você desarmou a armadilha de espinhos com maestria e ganhou +20 EXP!"
            updatedHero = addExp(hero, 20)
        } else {
            val trapDamage = Random.nextInt(15, 30)
            val newHp = (hero.currentHp - trapDamage).coerceAtLeast(1)
            message = "⚠️ A armadilha disparou! Você sofreu $trapDamage de dano de espinhos!"
            vibrate(120, 200)
            updatedHero = hero.copy(currentHp = newHp)
        }

        val updatedRun = currentRun.copy(currentRoomType = RoomType.EMPTY)
        _uiState.value = _uiState.value.copy(
            hero = updatedHero,
            currentDungeonRun = updatedRun,
            statusNotification = message
        )
    }

    fun fleeDungeon() {
        val run = _uiState.value.currentDungeonRun ?: return
        val hero = _uiState.value.hero ?: return

        val updatedHero = hero.copy(gold = hero.gold + run.lootCollectedGold)

        _uiState.value = _uiState.value.copy(
            hero = updatedHero,
            currentDungeonRun = null,
            combatState = null,
            currentScreen = GameScreen.TOWN_HUB,
            statusNotification = "Você recuou em segurança para Valoria com o saque obtido."
        )
        saveGame()
    }

    // --- COMBAT LOGIC ---

    fun heroAttack() {
        val state = _uiState.value.combatState ?: return
        val hero = _uiState.value.hero ?: return
        if (state.turn != CombatTurn.HERO || state.isVictory || state.isDefeat) return

        val isCrit = Random.nextInt(100) < hero.critChancePercent
        val rawDamage = hero.totalAttack + Random.nextInt(-2, 4)
        val defenseReduction = (state.activeMonster.defense * (if (state.monsterIsDefending) 1.5f else 1.0f)).toInt()
        val calculatedDamage = ((rawDamage - defenseReduction) * (if (isCrit) 1.75f else 1.0f)).toInt().coerceAtLeast(3)

        val newMonsterHp = (state.activeMonster.currentHp - calculatedDamage).coerceAtLeast(0)
        val updatedMonster = state.activeMonster.copy(currentHp = newMonsterHp)

        vibrate(if (isCrit) 90 else 40, if (isCrit) 220 else 120)

        val critText = if (isCrit) " CRÍTICO!" else ""
        val log = CombatLogEntry(
            text = "⚔️ ${hero.name} atacou ${state.activeMonster.namePt} causando $calculatedDamage de dano!$critText",
            isHeroAction = true,
            isCritical = isCrit
        )

        val updatedLogs = state.combatLogs + log

        if (newMonsterHp <= 0) {
            handleVictory(state.copy(activeMonster = updatedMonster, combatLogs = updatedLogs))
        } else {
            val nextState = state.copy(
                activeMonster = updatedMonster,
                combatLogs = updatedLogs,
                turn = CombatTurn.ENEMY,
                isDefending = false
            )
            _uiState.value = _uiState.value.copy(combatState = nextState)
            executeEnemyTurn()
        }
    }

    fun heroSkill(skill: Skill) {
        val state = _uiState.value.combatState ?: return
        val hero = _uiState.value.hero ?: return
        if (state.turn != CombatTurn.HERO || state.isVictory || state.isDefeat) return

        if (hero.currentMp < skill.manaCost) {
            _uiState.value = _uiState.value.copy(
                statusNotification = "Mana insuficiente para conjurar ${skill.namePt}!"
            )
            return
        }

        val updatedHeroMp = hero.currentMp - skill.manaCost
        var currentHero = hero.copy(currentMp = updatedHeroMp)

        val newLogs = state.combatLogs.toMutableList()

        if (skill.target == SkillTarget.SELF) {
            val healAmount = (skill.healAmount + (hero.intellect * 1.5f)).toInt()
            val newHp = (currentHero.currentHp + healAmount).coerceAtMost(currentHero.totalMaxHp)
            currentHero = currentHero.copy(currentHp = newHp)
            newLogs.add(
                CombatLogEntry(
                    text = "${skill.iconEmoji} ${hero.name} usou ${skill.namePt} e recuperou $healAmount de Vida!",
                    isHeroAction = true,
                    isHeal = true
                )
            )
            vibrate(50, 100)
            val nextState = state.copy(
                combatLogs = newLogs,
                turn = CombatTurn.ENEMY,
                isDefending = false
            )
            _uiState.value = _uiState.value.copy(hero = currentHero, combatState = nextState)
            executeEnemyTurn()
        } else {
            val rawDamage = (hero.totalAttack * skill.powerMultiplier) + Random.nextInt(0, 5)
            val defenseReduction = state.activeMonster.defense
            val calculatedDamage = (rawDamage - defenseReduction).toInt().coerceAtLeast(6)

            val newMonsterHp = (state.activeMonster.currentHp - calculatedDamage).coerceAtLeast(0)
            val updatedMonster = state.activeMonster.copy(currentHp = newMonsterHp)

            newLogs.add(
                CombatLogEntry(
                    text = "${skill.iconEmoji} ${hero.name} desferiu ${skill.namePt} em ${state.activeMonster.namePt} causando $calculatedDamage de dano!",
                    isHeroAction = true,
                    isCritical = true
                )
            )

            vibrate(100, 200)

            if (newMonsterHp <= 0) {
                _uiState.value = _uiState.value.copy(hero = currentHero)
                handleVictory(state.copy(activeMonster = updatedMonster, combatLogs = newLogs))
            } else {
                val nextState = state.copy(
                    activeMonster = updatedMonster,
                    combatLogs = newLogs,
                    turn = CombatTurn.ENEMY,
                    isDefending = false
                )
                _uiState.value = _uiState.value.copy(hero = currentHero, combatState = nextState)
                executeEnemyTurn()
            }
        }
    }

    fun heroDefend() {
        val state = _uiState.value.combatState ?: return
        val hero = _uiState.value.hero ?: return
        if (state.turn != CombatTurn.HERO || state.isVictory || state.isDefeat) return

        val manaRestored = 5
        val updatedHero = hero.copy(currentMp = (hero.currentMp + manaRestored).coerceAtMost(hero.totalMaxMp))

        val log = CombatLogEntry(
            text = "🛡️ ${hero.name} assume postura defensiva (+50% defesa no próximo golpe e recupera 5 de Mana)!",
            isHeroAction = true
        )

        vibrate(30, 80)
        val nextState = state.copy(
            isDefending = true,
            combatLogs = state.combatLogs + log,
            turn = CombatTurn.ENEMY
        )
        _uiState.value = _uiState.value.copy(hero = updatedHero, combatState = nextState)
        executeEnemyTurn()
    }

    fun heroUsePotionInCombat(item: Item) {
        val state = _uiState.value.combatState ?: return
        val hero = _uiState.value.hero ?: return
        if (state.turn != CombatTurn.HERO || state.isVictory || state.isDefeat) return

        var updatedHero = hero
        var logText = ""

        if (item.healHp > 0) {
            val newHp = (hero.currentHp + item.healHp).coerceAtMost(hero.totalMaxHp)
            updatedHero = updatedHero.copy(currentHp = newHp)
            logText = "🧪 Usou ${item.namePt} e restaurou ${item.healHp} de Vida!"
        } else if (item.healMp > 0) {
            val newMp = (hero.currentMp + item.healMp).coerceAtMost(hero.totalMaxMp)
            updatedHero = updatedHero.copy(currentMp = newMp)
            logText = "🧪 Usou ${item.namePt} e recuperou ${item.healMp} de Mana!"
        }

        val updatedInventory = removeFromInventory(hero.inventory, item.id)
        updatedHero = updatedHero.copy(inventory = updatedInventory)

        vibrate(40, 100)
        val log = CombatLogEntry(logText, isHeroAction = true, isHeal = true)
        val nextState = state.copy(
            combatLogs = state.combatLogs + log,
            turn = CombatTurn.ENEMY,
            isDefending = false
        )
        _uiState.value = _uiState.value.copy(hero = updatedHero, combatState = nextState)
        executeEnemyTurn()
    }

    fun heroFleeCombat() {
        val state = _uiState.value.combatState ?: return
        val hero = _uiState.value.hero ?: return
        if (state.turn != CombatTurn.HERO || state.isVictory || state.isDefeat) return

        if (state.activeMonster.isBoss) {
            _uiState.value = _uiState.value.copy(
                statusNotification = "Não é possível fugir de uma batalha contra um Chefe!"
            )
            return
        }

        val fleeSuccess = Random.nextInt(100) < (50 + (hero.speed - state.activeMonster.speed) * 3).coerceIn(30, 85)

        if (fleeSuccess) {
            vibrate(40, 100)
            val log = CombatLogEntry("🏃 ${hero.name} escapou com sucesso da batalha!", isHeroAction = true)
            val nextState = state.copy(
                isFled = true,
                combatLogs = state.combatLogs + log
            )
            _uiState.value = _uiState.value.copy(
                combatState = nextState,
                statusNotification = "Fugiu do combate!"
            )
        } else {
            vibrate(80, 150)
            val log = CombatLogEntry("❌ Falha na fuga! O monstro bloqueou sua retirada!", isHeroAction = true)
            val nextState = state.copy(
                combatLogs = state.combatLogs + log,
                turn = CombatTurn.ENEMY
            )
            _uiState.value = _uiState.value.copy(combatState = nextState)
            executeEnemyTurn()
        }
    }

    private fun executeEnemyTurn() {
        val state = _uiState.value.combatState ?: return
        val hero = _uiState.value.hero ?: return
        val monster = state.activeMonster

        val isSkill = monster.specialSkillName != null && Random.nextFloat() < 0.35f
        val monsterAtk = if (isSkill) (monster.attack * 1.35f).toInt() else monster.attack + Random.nextInt(-2, 3)

        val heroDefense = (hero.totalDefense * (if (state.isDefending) 1.6f else 1.0f)).toInt()
        val damage = (monsterAtk - heroDefense).coerceAtLeast(3)

        val newHeroHp = (hero.currentHp - damage).coerceAtLeast(0)
        val updatedHero = hero.copy(currentHp = newHeroHp)

        val logText = if (isSkill) {
            "⚡ ${monster.namePt} usou ${monster.specialSkillName} causando $damage de dano!"
        } else {
            "💥 ${monster.namePt} desferiu um ataque causando $damage de dano!"
        }

        vibrate(60, 160)

        val log = CombatLogEntry(text = logText, isHeroAction = false)
        val updatedLogs = state.combatLogs + log

        if (newHeroHp <= 0) {
            val defeatState = state.copy(
                isDefeat = true,
                combatLogs = updatedLogs + CombatLogEntry("💀 ${hero.name} foi derrotado em combate...", false)
            )
            _uiState.value = _uiState.value.copy(hero = updatedHero, combatState = defeatState)
        } else {
            val nextState = state.copy(
                combatLogs = updatedLogs,
                turn = CombatTurn.HERO,
                isDefending = false
            )
            _uiState.value = _uiState.value.copy(hero = updatedHero, combatState = nextState)
        }
    }

    private fun handleVictory(state: CombatState) {
        val hero = _uiState.value.hero ?: return
        val monster = state.activeMonster

        val expGained = monster.expReward
        val goldGained = monster.goldReward

        // Possible loot drop (35% chance)
        val drop = if (Random.nextFloat() < 0.35f) {
            RpgCatalog.shopItems.random().copy(quantity = 1)
        } else null

        val victoryLog = CombatLogEntry(
            text = "🏆 Vitória! ${monster.namePt} foi derrotado! (+$expGained EXP, +$goldGained Ouro)",
            isHeroAction = true,
            isCritical = true
        )
        val dropLog = drop?.let {
            CombatLogEntry("📦 Recompensa de Espólio: Encontrou ${it.namePt}!", isHeroAction = true)
        }

        val allLogs = state.combatLogs + victoryLog + listOfNotNull(dropLog)

        vibrate(120, 240)

        val victoryState = state.copy(
            isVictory = true,
            expEarned = expGained,
            goldEarned = goldGained,
            droppedItem = drop,
            combatLogs = allLogs
        )
        _uiState.value = _uiState.value.copy(combatState = victoryState)

        // Update Quest Progress
        updateQuestProgress(monster.id)
    }

    private fun updateQuestProgress(monsterId: String) {
        val currentQuests = _uiState.value.quests
        val updatedQuests = currentQuests.map { q ->
            if (q.targetMonsterId == monsterId && !q.isCompleted) {
                val newCount = q.currentCount + 1
                q.copy(
                    currentCount = newCount,
                    isCompleted = newCount >= q.requiredCount
                )
            } else q
        }
        _uiState.value = _uiState.value.copy(quests = updatedQuests)
    }

    fun finishCombatVictory() {
        val state = _uiState.value.combatState ?: return
        val hero = _uiState.value.hero ?: return
        val currentRun = _uiState.value.currentDungeonRun

        var updatedHero = hero.copy(
            gold = hero.gold + state.goldEarned,
            monstersSlain = hero.monstersSlain + 1,
            bossesSlain = if (state.activeMonster.isBoss) hero.bossesSlain + 1 else hero.bossesSlain
        )

        state.droppedItem?.let {
            updatedHero = updatedHero.copy(inventory = addToInventory(updatedHero.inventory, it))
        }

        updatedHero = addExp(updatedHero, state.expEarned)

        if (currentRun != null) {
            val updatedRun = currentRun.copy(
                lootCollectedGold = currentRun.lootCollectedGold + state.goldEarned,
                itemsCollected = if (state.droppedItem != null) currentRun.itemsCollected + state.droppedItem else currentRun.itemsCollected
            )
            _uiState.value = _uiState.value.copy(
                hero = updatedHero,
                currentDungeonRun = updatedRun,
                combatState = null
            )
            advanceDungeonRoom()
        } else {
            _uiState.value = _uiState.value.copy(
                hero = updatedHero,
                combatState = null,
                currentScreen = GameScreen.TOWN_HUB
            )
        }
        saveGame()
    }

    fun reviveHeroInTown() {
        val hero = _uiState.value.hero ?: return
        val revivedHero = hero.copy(
            currentHp = hero.totalMaxHp / 2,
            currentMp = hero.totalMaxMp / 2,
            gold = (hero.gold * 0.85).toInt() // small gold penalty on defeat
        )
        _uiState.value = _uiState.value.copy(
            hero = revivedHero,
            currentDungeonRun = null,
            combatState = null,
            currentScreen = GameScreen.TOWN_HUB,
            statusNotification = "Você foi resgatado e levado à Taverna de Valoria."
        )
        saveGame()
    }

    // --- LEVEL UP & STATS ---

    private fun addExp(hero: Hero, amount: Int): Hero {
        var currentExp = hero.experience + amount
        var currentLevel = hero.level
        var currentExpNext = hero.expToNextLevel
        var availablePoints = hero.availableStatPoints
        var hpGain = 0
        var mpGain = 0

        while (currentExp >= currentExpNext) {
            currentExp -= currentExpNext
            currentLevel += 1
            currentExpNext = (currentExpNext * 1.5).toInt()
            availablePoints += 3
            hpGain += 15
            mpGain += 10
            vibrate(150, 255)
        }

        return hero.copy(
            experience = currentExp,
            level = currentLevel,
            expToNextLevel = currentExpNext,
            availableStatPoints = availablePoints,
            maxHp = hero.maxHp + hpGain,
            currentHp = (hero.currentHp + hpGain).coerceAtMost(hero.maxHp + hpGain),
            maxMp = hero.maxMp + mpGain,
            currentMp = (hero.currentMp + mpGain).coerceAtMost(hero.maxMp + mpGain)
        )
    }

    fun allocateStat(stat: String) {
        val hero = _uiState.value.hero ?: return
        if (hero.availableStatPoints <= 0) return

        val updatedHero = when (stat.lowercase()) {
            "força", "strength" -> hero.copy(
                strength = hero.strength + 1,
                availableStatPoints = hero.availableStatPoints - 1
            )
            "destreza", "agility" -> hero.copy(
                agility = hero.agility + 1,
                availableStatPoints = hero.availableStatPoints - 1
            )
            "inteligência", "intellect" -> hero.copy(
                intellect = hero.intellect + 1,
                availableStatPoints = hero.availableStatPoints - 1
            )
            "vitalidade", "vitality" -> hero.copy(
                vitality = hero.vitality + 1,
                availableStatPoints = hero.availableStatPoints - 1,
                currentHp = hero.currentHp + 5
            )
            else -> hero
        }

        vibrate(30, 80)
        _uiState.value = _uiState.value.copy(hero = updatedHero)
        saveGame()
    }

    // --- INVENTORY & EQUIPMENT ---

    fun equipItem(item: Item) {
        val hero = _uiState.value.hero ?: return

        var eq = hero.equipment
        val inv = hero.inventory.toMutableList()

        // Remove item from inventory
        val existingIndex = inv.indexOfFirst { it.id == item.id }
        if (existingIndex >= 0) {
            val existing = inv[existingIndex]
            if (existing.quantity > 1) {
                inv[existingIndex] = existing.copy(quantity = existing.quantity - 1)
            } else {
                inv.removeAt(existingIndex)
            }
        }

        // Put previous equipped item back into inventory if present
        when (item.type) {
            ItemType.WEAPON -> {
                eq.weapon?.let { inv.add(it) }
                eq = eq.copy(weapon = item)
            }
            ItemType.ARMOR -> {
                eq.armor?.let { inv.add(it) }
                eq = eq.copy(armor = item)
            }
            ItemType.SHIELD -> {
                eq.shield?.let { inv.add(it) }
                eq = eq.copy(shield = item)
            }
            ItemType.ACCESSORY -> {
                eq.accessory?.let { inv.add(it) }
                eq = eq.copy(accessory = item)
            }
            ItemType.CONSUMABLE -> return
        }

        vibrate(40, 100)
        val updatedHero = hero.copy(equipment = eq, inventory = inv)
        _uiState.value = _uiState.value.copy(
            hero = updatedHero,
            statusNotification = "Equipou ${item.namePt} com sucesso!"
        )
        saveGame()
    }

    fun unequipSlot(slot: String) {
        val hero = _uiState.value.hero ?: return
        var eq = hero.equipment
        var unequippedItem: Item? = null

        when (slot.lowercase()) {
            "weapon" -> {
                unequippedItem = eq.weapon
                eq = eq.copy(weapon = null)
            }
            "armor" -> {
                unequippedItem = eq.armor
                eq = eq.copy(armor = null)
            }
            "shield" -> {
                unequippedItem = eq.shield
                eq = eq.copy(shield = null)
            }
            "accessory" -> {
                unequippedItem = eq.accessory
                eq = eq.copy(accessory = null)
            }
        }

        if (unequippedItem != null) {
            val updatedInventory = addToInventory(hero.inventory, unequippedItem)
            val updatedHero = hero.copy(equipment = eq, inventory = updatedInventory)
            _uiState.value = _uiState.value.copy(
                hero = updatedHero,
                statusNotification = "Desequipou ${unequippedItem.namePt}."
            )
            saveGame()
        }
    }

    fun useItemOutsideCombat(item: Item) {
        val hero = _uiState.value.hero ?: return
        if (item.type != ItemType.CONSUMABLE) return

        var newHp = hero.currentHp
        var newMp = hero.currentMp

        if (item.healHp > 0) {
            newHp = (newHp + item.healHp).coerceAtMost(hero.totalMaxHp)
        }
        if (item.healMp > 0) {
            newMp = (newMp + item.healMp).coerceAtMost(hero.totalMaxMp)
        }

        val updatedInventory = removeFromInventory(hero.inventory, item.id)
        val updatedHero = hero.copy(
            currentHp = newHp,
            currentMp = newMp,
            inventory = updatedInventory
        )

        vibrate(40, 90)
        _uiState.value = _uiState.value.copy(
            hero = updatedHero,
            statusNotification = "Usou ${item.namePt}."
        )
        saveGame()
    }

    // --- TOWN SERVICES ---

    fun restAtTavern() {
        val hero = _uiState.value.hero ?: return
        val cost = 10
        if (hero.gold < cost) {
            _uiState.value = _uiState.value.copy(statusNotification = "Ouro insuficiente para descansar na Taverna (Custa $cost Ouro)!")
            return
        }

        val updatedHero = hero.copy(
            gold = hero.gold - cost,
            currentHp = hero.totalMaxHp,
            currentMp = hero.totalMaxMp
        )

        vibrate(60, 120)
        _uiState.value = _uiState.value.copy(
            hero = updatedHero,
            statusNotification = "🍺 Você descansou confortavelmente e recuperou toda a Vida e Mana!"
        )
        saveGame()
    }

    fun buyShopItem(item: Item) {
        val hero = _uiState.value.hero ?: return
        if (hero.gold < item.valueGold) {
            _uiState.value = _uiState.value.copy(statusNotification = "Ouro insuficiente para comprar ${item.namePt}!")
            return
        }

        val updatedInventory = addToInventory(hero.inventory, item.copy(quantity = 1))
        val updatedHero = hero.copy(
            gold = hero.gold - item.valueGold,
            inventory = updatedInventory
        )

        vibrate(50, 110)
        _uiState.value = _uiState.value.copy(
            hero = updatedHero,
            statusNotification = "Comprou ${item.namePt} por ${item.valueGold} Ouro!"
        )
        saveGame()
    }

    fun sellInventoryItem(item: Item) {
        val hero = _uiState.value.hero ?: return
        val sellPrice = (item.valueGold * 0.5).toInt().coerceAtLeast(1)

        val updatedInventory = removeFromInventory(hero.inventory, item.id)
        val updatedHero = hero.copy(
            gold = hero.gold + sellPrice,
            inventory = updatedInventory
        )

        vibrate(30, 80)
        _uiState.value = _uiState.value.copy(
            hero = updatedHero,
            statusNotification = "Vendeu ${item.namePt} por $sellPrice Ouro!"
        )
        saveGame()
    }

    fun claimQuestReward(questId: String) {
        val hero = _uiState.value.hero ?: return
        val quest = _uiState.value.quests.find { it.id == questId } ?: return
        if (!quest.isCompleted || quest.isClaimed) return

        var updatedHero = hero.copy(gold = hero.gold + quest.rewardGold)
        updatedHero = addExp(updatedHero, quest.rewardExp)

        val updatedQuests = _uiState.value.quests.map {
            if (it.id == questId) it.copy(isClaimed = true) else it
        }

        vibrate(100, 200)
        _uiState.value = _uiState.value.copy(
            hero = updatedHero,
            quests = updatedQuests,
            statusNotification = "🏆 Recompensa Coletada: +${quest.rewardGold} Ouro e +${quest.rewardExp} EXP!"
        )
        saveGame()
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(statusNotification = null)
    }

    private fun saveGame() {
        val hero = _uiState.value.hero ?: return
        storage.saveHero(hero, _uiState.value.quests)
        _uiState.value = _uiState.value.copy(hasSavedGame = true)
    }

    // --- UTILITIES ---

    private fun addToInventory(list: List<Item>, newItem: Item): List<Item> {
        val mutable = list.toMutableList()
        val index = mutable.indexOfFirst { it.id == newItem.id && it.type == ItemType.CONSUMABLE }
        if (index >= 0) {
            val existing = mutable[index]
            mutable[index] = existing.copy(quantity = existing.quantity + newItem.quantity)
        } else {
            mutable.add(newItem)
        }
        return mutable
    }

    private fun removeFromInventory(list: List<Item>, itemId: String): List<Item> {
        val mutable = list.toMutableList()
        val index = mutable.indexOfFirst { it.id == itemId }
        if (index >= 0) {
            val item = mutable[index]
            if (item.quantity > 1) {
                mutable[index] = item.copy(quantity = item.quantity - 1)
            } else {
                mutable.removeAt(index)
            }
        }
        return mutable
    }
}
