package com.example.testejuerpg.data

import com.example.testejuerpg.model.*

object RpgCatalog {

    // --- SKILLS ---
    val warriorSkills = listOf(
        Skill(
            id = "w_slash",
            namePt = "Golpe Poderoso",
            descriptionPt = "Um golpe devastador com lâmina pesada que causa 150% do dano físico.",
            manaCost = 8,
            powerMultiplier = 1.5f,
            target = SkillTarget.SINGLE_ENEMY,
            iconEmoji = "💥",
            unlockLevel = 1
        ),
        Skill(
            id = "w_shout",
            namePt = "Grito de Guerra",
            descriptionPt = "Restaura 25 de Vida e revigora a moral em combate.",
            manaCost = 12,
            powerMultiplier = 0f,
            target = SkillTarget.SELF,
            healAmount = 25,
            iconEmoji = "🦁",
            unlockLevel = 2
        ),
        Skill(
            id = "w_whirlwind",
            namePt = "Turbilhão Furioso",
            descriptionPt = "Gira violentamente atingindo o oponente com 220% de força.",
            manaCost = 20,
            powerMultiplier = 2.2f,
            target = SkillTarget.SINGLE_ENEMY,
            iconEmoji = "🌪️",
            unlockLevel = 4
        )
    )

    val mageSkills = listOf(
        Skill(
            id = "m_fireball",
            namePt = "Bola de Fogo",
            descriptionPt = "Lança uma esfera ardente que incinera o alvo por 180% do dano arcano.",
            manaCost = 15,
            powerMultiplier = 1.8f,
            target = SkillTarget.SINGLE_ENEMY,
            iconEmoji = "🔥",
            unlockLevel = 1
        ),
        Skill(
            id = "m_frost",
            namePt = "Raio Congelante",
            descriptionPt = "Um feixe de gelo que perfura as defesas com 140% de poder e drena a energia.",
            manaCost = 10,
            powerMultiplier = 1.4f,
            target = SkillTarget.SINGLE_ENEMY,
            iconEmoji = "❄️",
            unlockLevel = 2
        ),
        Skill(
            id = "m_storm",
            namePt = "Tempestade Arcana",
            descriptionPt = "Invoca relâmpagos puros causando 250% de dano destrutivo.",
            manaCost = 35,
            powerMultiplier = 2.5f,
            target = SkillTarget.SINGLE_ENEMY,
            iconEmoji = "⚡",
            unlockLevel = 5
        )
    )

    val rogueSkills = listOf(
        Skill(
            id = "r_backstab",
            namePt = "Apunhalada Traiçoeira",
            descriptionPt = "Atinge um ponto vital pelas sombras causando 175% do dano.",
            manaCost = 10,
            powerMultiplier = 1.75f,
            target = SkillTarget.SINGLE_ENEMY,
            iconEmoji = "🗡️",
            unlockLevel = 1
        ),
        Skill(
            id = "r_poison",
            namePt = "Lâmina Envenenada",
            descriptionPt = "Cobre a lâmina em veneno vítreo causando 130% de dano e sangramento.",
            manaCost = 12,
            powerMultiplier = 1.3f,
            target = SkillTarget.SINGLE_ENEMY,
            iconEmoji = "🧪",
            unlockLevel = 2
        ),
        Skill(
            id = "r_shadow",
            namePt = "Assalto das Sombras",
            descriptionPt = "Sequência letal de cortes ultra-rápidos com 240% de dano crítico.",
            manaCost = 25,
            powerMultiplier = 2.4f,
            target = SkillTarget.SINGLE_ENEMY,
            iconEmoji = "🌑",
            unlockLevel = 4
        )
    )

    val paladinSkills = listOf(
        Skill(
            id = "p_smite",
            namePt = "Golpe Sagrado",
            descriptionPt = "Desfere a fúria da luz celestial causando 160% de dano sagrado.",
            manaCost = 12,
            powerMultiplier = 1.6f,
            target = SkillTarget.SINGLE_ENEMY,
            iconEmoji = "✨",
            unlockLevel = 1
        ),
        Skill(
            id = "p_heal",
            namePt = "Cura Radiante",
            descriptionPt = "Canaliza preces celestiais restaurando 40 de Vida.",
            manaCost = 18,
            powerMultiplier = 0f,
            target = SkillTarget.SELF,
            healAmount = 40,
            iconEmoji = "💖",
            unlockLevel = 2
        ),
        Skill(
            id = "p_bastion",
            namePt = "Lança do Julgamento",
            descriptionPt = "Um pilar de fogo sagrado que destrói as sombras com 230% de poder.",
            manaCost = 30,
            powerMultiplier = 2.3f,
            target = SkillTarget.SINGLE_ENEMY,
            iconEmoji = "⚜️",
            unlockLevel = 4
        )
    )

    fun getSkillsForClass(classType: HeroClassType): List<Skill> {
        return when (classType) {
            HeroClassType.GUERREIRO -> warriorSkills
            HeroClassType.MAGO -> mageSkills
            HeroClassType.LADINO -> rogueSkills
            HeroClassType.PALADINO -> paladinSkills
        }
    }

    // --- ITEMS CATALOG ---
    val shopItems = listOf(
        Item(
            id = "pot_hp_small",
            namePt = "Poção de Vida Menor",
            descriptionPt = "Restaura 40 pontos de Vida instantaneamente.",
            type = ItemType.CONSUMABLE,
            rarity = ItemRarity.COMMON,
            healHp = 40,
            valueGold = 15,
            iconEmoji = "🧪"
        ),
        Item(
            id = "pot_hp_large",
            namePt = "Poção de Vida Maior",
            descriptionPt = "Restaura 90 pontos de Vida instantaneamente.",
            type = ItemType.CONSUMABLE,
            rarity = ItemRarity.RARE,
            healHp = 90,
            valueGold = 35,
            iconEmoji = "❤️"
        ),
        Item(
            id = "pot_mp_small",
            namePt = "Elixir de Mana",
            descriptionPt = "Restaura 35 pontos de Mana instantaneamente.",
            type = ItemType.CONSUMABLE,
            rarity = ItemRarity.COMMON,
            healMp = 35,
            valueGold = 18,
            iconEmoji = "💙"
        ),
        Item(
            id = "wep_iron_sword",
            namePt = "Espada de Ferro Nobre",
            descriptionPt = "Forjada por ferreiros de Valoria. +12 Ataque.",
            type = ItemType.WEAPON,
            rarity = ItemRarity.COMMON,
            bonusAttack = 12,
            valueGold = 45,
            iconEmoji = "🗡️"
        ),
        Item(
            id = "wep_steel_axe",
            namePt = "Machado de Batalha",
            descriptionPt = "Pesado e mortal contra monstros couraçados. +18 Ataque.",
            type = ItemType.WEAPON,
            rarity = ItemRarity.RARE,
            bonusAttack = 18,
            valueGold = 85,
            iconEmoji = "🪓"
        ),
        Item(
            id = "wep_crystal_staff",
            namePt = "Cajado de Cristal Arcano",
            descriptionPt = "Canalizador mágico com safira pura. +22 Ataque, +20 Mana.",
            type = ItemType.WEAPON,
            rarity = ItemRarity.EPIC,
            bonusAttack = 22,
            bonusMp = 20,
            valueGold = 140,
            iconEmoji = "🪄"
        ),
        Item(
            id = "arm_leather",
            namePt = "Armadura de Couro Reforçada",
            descriptionPt = "Leve e resistente. +6 Defesa, +15 Vida.",
            type = ItemType.ARMOR,
            rarity = ItemRarity.COMMON,
            bonusDefense = 6,
            bonusHp = 15,
            valueGold = 40,
            iconEmoji = "🥋"
        ),
        Item(
            id = "arm_chainmail",
            namePt = "Cota de Malha Élfica",
            descriptionPt = "Proteção equilibrada contra garras e espadas. +12 Defesa, +30 Vida.",
            type = ItemType.ARMOR,
            rarity = ItemRarity.RARE,
            bonusDefense = 12,
            bonusHp = 30,
            valueGold = 95,
            iconEmoji = "🛡️"
        ),
        Item(
            id = "arm_plate",
            namePt = "Armadura de Placas de Titânio",
            descriptionPt = "Pesadíssima e quase impenetrável. +20 Defesa, +60 Vida.",
            type = ItemType.ARMOR,
            rarity = ItemRarity.EPIC,
            bonusDefense = 20,
            bonusHp = 60,
            valueGold = 180,
            iconEmoji = "🛡️"
        ),
        Item(
            id = "shd_wood",
            namePt = "Escudo de Carvalho",
            descriptionPt = "Simples mas eficiente para bloquear investidas. +5 Defesa.",
            type = ItemType.SHIELD,
            rarity = ItemRarity.COMMON,
            bonusDefense = 5,
            valueGold = 30,
            iconEmoji = "🪵"
        ),
        Item(
            id = "shd_tower",
            namePt = "Escudo Torre de Bronze",
            descriptionPt = "Grande escudo fortificado. +11 Defesa, +20 Vida.",
            type = ItemType.SHIELD,
            rarity = ItemRarity.RARE,
            bonusDefense = 11,
            bonusHp = 20,
            valueGold = 75,
            iconEmoji = "🛡️"
        ),
        Item(
            id = "acc_ring_str",
            namePt = "Anel da Força Titânica",
            descriptionPt = "Brilha com poder antigo. +7 Ataque.",
            type = ItemType.ACCESSORY,
            rarity = ItemRarity.RARE,
            bonusAttack = 7,
            valueGold = 60,
            iconEmoji = "💍"
        ),
        Item(
            id = "acc_amulet_life",
            namePt = "Amuleto do Coração de Rubi",
            descriptionPt = "Aumenta a vitalidade do usuário. +35 Vida, +15 Mana.",
            type = ItemType.ACCESSORY,
            rarity = ItemRarity.EPIC,
            bonusHp = 35,
            bonusMp = 15,
            valueGold = 120,
            iconEmoji = "📿"
        )
    )

    fun getStartingGear(classType: HeroClassType): Equipment {
        return when (classType) {
            HeroClassType.GUERREIRO -> Equipment(
                weapon = Item(
                    id = "init_sword",
                    namePt = "Espada Curta Enferrujada",
                    descriptionPt = "Uma espada antiga com corte básico.",
                    type = ItemType.WEAPON,
                    rarity = ItemRarity.COMMON,
                    bonusAttack = 5,
                    valueGold = 10,
                    iconEmoji = "🗡️"
                ),
                shield = Item(
                    id = "init_buckler",
                    namePt = "Broquel de Madeira",
                    descriptionPt = "Um escudo leve para conter golpes.",
                    type = ItemType.SHIELD,
                    rarity = ItemRarity.COMMON,
                    bonusDefense = 3,
                    valueGold = 8,
                    iconEmoji = "🛡️"
                )
            )
            HeroClassType.MAGO -> Equipment(
                weapon = Item(
                    id = "init_wand",
                    namePt = "Varinha de Aprendiz",
                    descriptionPt = "Varinha esculpida em salgueiro com foco de quartzo.",
                    type = ItemType.WEAPON,
                    rarity = ItemRarity.COMMON,
                    bonusAttack = 6,
                    bonusMp = 15,
                    valueGold = 12,
                    iconEmoji = "🪄"
                )
            )
            HeroClassType.LADINO -> Equipment(
                weapon = Item(
                    id = "init_dagger",
                    namePt = "Adagas Gêmeas Simples",
                    descriptionPt = "Lâminas rápidas ideais para emboscadas.",
                    type = ItemType.WEAPON,
                    rarity = ItemRarity.COMMON,
                    bonusAttack = 6,
                    valueGold = 10,
                    iconEmoji = "🗡️"
                )
            )
            HeroClassType.PALADINO -> Equipment(
                weapon = Item(
                    id = "init_mace",
                    namePt = "Maça de Ferro do Alvorecer",
                    descriptionPt = "Arma contundente abençoada pelo templo.",
                    type = ItemType.WEAPON,
                    rarity = ItemRarity.COMMON,
                    bonusAttack = 5,
                    valueGold = 12,
                    iconEmoji = "🔨"
                ),
                shield = Item(
                    id = "init_shield_pala",
                    namePt = "Escudo do Peregrino",
                    descriptionPt = "Ostenta o símbolo da justiça divina.",
                    type = ItemType.SHIELD,
                    rarity = ItemRarity.COMMON,
                    bonusDefense = 4,
                    valueGold = 10,
                    iconEmoji = "🛡️"
                )
            )
        }
    }

    // --- DUNGEONS & MONSTERS ---
    val dungeons = listOf(
        Dungeon(
            id = "dungeon_forest",
            namePt = "Floresta dos Murmúrios",
            descriptionPt = "Bosque nebuloso infestado por bandos de goblins saqueadores e lobos sombrios.",
            recommendedLevel = 1,
            totalRooms = 5,
            iconEmoji = "🌲",
            enemyPool = listOf(
                Monster(
                    id = "mon_goblin",
                    namePt = "Batedor Goblin",
                    descriptionPt = "Pequeno mas perigoso com adagas envenenadas.",
                    level = 1,
                    maxHp = 35,
                    currentHp = 35,
                    attack = 10,
                    defense = 3,
                    speed = 9,
                    expReward = 25,
                    goldReward = 14,
                    iconEmoji = "👺"
                ),
                Monster(
                    id = "mon_wolf",
                    namePt = "Lobo Noturno",
                    descriptionPt = "Predador voraz com presas afiadas.",
                    level = 2,
                    maxHp = 48,
                    currentHp = 48,
                    attack = 13,
                    defense = 4,
                    speed = 12,
                    expReward = 35,
                    goldReward = 18,
                    iconEmoji = "🐺"
                ),
                Monster(
                    id = "mon_bandit",
                    namePt = "Salteador da Floresta",
                    descriptionPt = "Malandro armado com machadinha e escudo de pele.",
                    level = 2,
                    maxHp = 55,
                    currentHp = 55,
                    attack = 15,
                    defense = 6,
                    speed = 8,
                    expReward = 42,
                    goldReward = 26,
                    iconEmoji = "🥷"
                )
            ),
            boss = Monster(
                id = "boss_forest_king",
                namePt = "Chefe Ogro Gorg'thok",
                descriptionPt = "Líder brutal das feras florestais armado com tronco espinhoso.",
                level = 3,
                maxHp = 110,
                currentHp = 110,
                attack = 22,
                defense = 8,
                speed = 6,
                expReward = 120,
                goldReward = 85,
                iconEmoji = "👹",
                isBoss = true,
                specialSkillName = "Pancada Esmagadora"
            )
        ),
        Dungeon(
            id = "dungeon_crypt",
            namePt = "Cripta dos Reis Esquecidos",
            descriptionPt = "Catacumbas ancestrais onde mortos-vivos e espectros guardam tesouros sepultados.",
            recommendedLevel = 3,
            totalRooms = 6,
            iconEmoji = "⚰️",
            enemyPool = listOf(
                Monster(
                    id = "mon_skeleton",
                    namePt = "Guerreiro Esqueleto",
                    descriptionPt = "Ossadas animadas com espada enferrujada.",
                    level = 3,
                    maxHp = 65,
                    currentHp = 65,
                    attack = 17,
                    defense = 7,
                    speed = 8,
                    expReward = 45,
                    goldReward = 22,
                    iconEmoji = "💀"
                ),
                Monster(
                    id = "mon_specter",
                    namePt = "Espectro Lamuriante",
                    descriptionPt = "Espírito etéreo que drena a sanidade dos invasores.",
                    level = 4,
                    maxHp = 58,
                    currentHp = 58,
                    attack = 21,
                    defense = 5,
                    speed = 14,
                    expReward = 55,
                    goldReward = 30,
                    iconEmoji = "👻"
                ),
                Monster(
                    id = "mon_ghoul",
                    namePt = "Carniçal Faminto",
                    descriptionPt = "Monstruosidade veloz com garras pútridas.",
                    level = 4,
                    maxHp = 78,
                    currentHp = 78,
                    attack = 23,
                    defense = 8,
                    speed = 10,
                    expReward = 65,
                    goldReward = 36,
                    iconEmoji = "🧟"
                )
            ),
            boss = Monster(
                id = "boss_lich",
                namePt = "Lorde Necromante Malakar",
                descriptionPt = "Mago sombrio que desafiou a morte com rituais macabros.",
                level = 5,
                maxHp = 170,
                currentHp = 170,
                attack = 30,
                defense = 12,
                speed = 11,
                expReward = 240,
                goldReward = 160,
                iconEmoji = "🧙‍♂️",
                isBoss = true,
                specialSkillName = "Onda da Morte"
            )
        ),
        Dungeon(
            id = "dungeon_volcano",
            namePt = "Fosso das Chamas Infernais",
            descriptionPt = "Abismo vulcânico fervente habitado por demônios ígneos e o lendário Dragão Carmesim.",
            recommendedLevel = 5,
            totalRooms = 7,
            iconEmoji = "🌋",
            enemyPool = listOf(
                Monster(
                    id = "mon_imp",
                    namePt = "Diabrete de Fogo",
                    descriptionPt = "Criatura travessa que cospe brasas incandescentes.",
                    level = 5,
                    maxHp = 82,
                    currentHp = 82,
                    attack = 26,
                    defense = 9,
                    speed = 15,
                    expReward = 80,
                    goldReward = 45,
                    iconEmoji = "🔥"
                ),
                Monster(
                    id = "mon_golem",
                    namePt = "Golem de Magma",
                    descriptionPt = "Colosso rochoso esculpido em rocha vulcânica e lava.",
                    level = 6,
                    maxHp = 125,
                    currentHp = 125,
                    attack = 31,
                    defense = 18,
                    speed = 5,
                    expReward = 105,
                    goldReward = 60,
                    iconEmoji = "🗿"
                ),
                Monster(
                    id = "mon_hellhound",
                    namePt = "Cão dos Infernos",
                    descriptionPt = "Fera bicéfala que caça na escuridão incandescente.",
                    level = 6,
                    maxHp = 95,
                    currentHp = 95,
                    attack = 33,
                    defense = 10,
                    speed = 16,
                    expReward = 115,
                    goldReward = 68,
                    iconEmoji = "🐕"
                )
            ),
            boss = Monster(
                id = "boss_dragon",
                namePt = "Dragão Carmesim Ignis",
                descriptionPt = "O terrível soberano do vulcão cuja chama derrete até o mais nobre aço.",
                level = 7,
                maxHp = 260,
                currentHp = 260,
                attack = 42,
                defense = 20,
                speed = 12,
                expReward = 450,
                goldReward = 320,
                iconEmoji = "🐉",
                isBoss = true,
                specialSkillName = "Sopro do Apocalipse"
            )
        )
    )

    // --- INITIAL QUESTS ---
    val initialQuests = listOf(
        Quest(
            id = "quest_1",
            titlePt = "Limpeza do Bosque",
            descriptionPt = "Derrote 3 Batedores Goblins na Floresta dos Murmúrios para proteger as caravanas mercantes.",
            targetMonsterId = "mon_goblin",
            requiredCount = 3,
            rewardGold = 50,
            rewardExp = 60
        ),
        Quest(
            id = "quest_2",
            titlePt = "A Caçada dos Lobos",
            descriptionPt = "Elimine 2 Lobos Noturnos que aterrorizam os arredores da vila.",
            targetMonsterId = "mon_wolf",
            requiredCount = 2,
            rewardGold = 70,
            rewardExp = 80
        ),
        Quest(
            id = "quest_3",
            titlePt = "Descanso Eterno",
            descriptionPt = "Purifique 3 Guerreiros Esqueletos na Cripta dos Reis Esquecidos.",
            targetMonsterId = "mon_skeleton",
            requiredCount = 3,
            rewardGold = 120,
            rewardExp = 150
        ),
        Quest(
            id = "quest_4",
            titlePt = "Ameaça Ogro",
            descriptionPt = "Derrote o temível Chefe Ogro Gorg'thok na Floresta dos Murmúrios.",
            targetMonsterId = "boss_forest_king",
            requiredCount = 1,
            rewardGold = 150,
            rewardExp = 200
        )
    )
}
