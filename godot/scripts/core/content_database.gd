extends Node
## Authoritative offline content database. Every entry here is consumed by a
## runtime service; there are no disposable catalog rows or numbered filler.

const WEAPONS := {
    "volt_blades":{"name":"Lâminas Voltáicas","damage":24.0,"rate":0.24,"range":3.4,"type":"melee","kit":"cone","abilities":["volt_dash","arc_burst"]},
    "toxic_bow":{"name":"Arco Tóxico","damage":34.0,"rate":0.55,"range":16.0,"type":"ranged","kit":"poison_shot","abilities":["venom_volley","toxic_trap"]},
    "pulse_cannon":{"name":"Canhão Pulsar","damage":46.0,"rate":0.72,"range":18.0,"type":"ranged","kit":"shotgun","abilities":["pulse_beam","overcharge"]},
    "scrap_hammer":{"name":"Martelo Sucateiro","damage":58.0,"rate":0.88,"range":4.0,"type":"melee","kit":"slam","abilities":["quake","magnet_smash"]},
    "aether_lance":{"name":"Lança de Aether","damage":31.0,"rate":0.35,"range":5.8,"type":"melee","kit":"thrust","abilities":["lance_charge","aether_prism"]},
    "orbit_orbs":{"name":"Orbes Orbitais","damage":21.0,"rate":0.18,"range":10.0,"type":"ranged","kit":"orbit","abilities":["orb_swarm","gravity_well"]},
    "frost_repeater":{"name":"Repetidor Glacial","damage":39.0,"rate":0.42,"range":15.0,"type":"ranged","kit":"rapid","abilities":["frost_barrage","frost_zone"]},
    "ember_scythes":{"name":"Foices Ígneas","damage":44.0,"rate":0.36,"range":4.4,"type":"melee","kit":"spin","abilities":["ember_ring","blazing_dash"]},
    "storm_bow":{"name":"Arco de Trovão","damage":36.0,"rate":0.48,"range":17.0,"type":"ranged","kit":"chain","abilities":["thunder_chain","storm_field"]},
    "gravity_matrix":{"name":"Matriz Gravitacional","damage":52.0,"rate":0.66,"range":12.0,"type":"ranged","kit":"field","abilities":["singularity","gravity_lock"]},
    "venom_nova":{"name":"Lâmina Veneno-Nova","damage":41.0,"rate":0.30,"range":4.2,"type":"melee","kit":"toxic_combo","abilities":["venom_burst","corrosive_edge"]},
    "prism_staff":{"name":"Bastão Prisma","damage":33.0,"rate":0.38,"range":16.0,"type":"ranged","kit":"beam","abilities":["prism_nova","prism_barrier"]}
}

const ABILITIES := {
    "volt_dash":{"name":"Avanço Voltáico","cooldown":4.0,"energy":20.0,"effect":"dash","power":1.55,"radius":3.2},
    "arc_burst":{"name":"Rajada de Arco","cooldown":7.0,"energy":32.0,"effect":"chain","power":1.25,"radius":6.0,"count":4},
    "venom_volley":{"name":"Salva Venenosa","cooldown":6.0,"energy":30.0,"effect":"multi_projectile","power":1.30,"radius":16.0,"count":3},
    "toxic_trap":{"name":"Armadilha Tóxica","cooldown":10.0,"energy":44.0,"effect":"area","power":2.0,"radius":4.8},
    "pulse_beam":{"name":"Feixe Pulsar","cooldown":8.0,"energy":45.0,"effect":"beam","power":2.45,"radius":18.0},
    "overcharge":{"name":"Sobrecarga","cooldown":18.0,"energy":70.0,"effect":"buff_damage","power":0.45,"radius":0.0,"duration":6.0},
    "quake":{"name":"Tremor de Sucata","cooldown":7.0,"energy":38.0,"effect":"area","power":2.2,"radius":5.6},
    "magnet_smash":{"name":"Esmagador Magnético","cooldown":11.0,"energy":52.0,"effect":"pull_area","power":2.6,"radius":6.3},
    "lance_charge":{"name":"Carga de Aether","cooldown":5.0,"energy":27.0,"effect":"dash","power":2.10,"radius":3.6},
    "aether_prism":{"name":"Prisma de Aether","cooldown":12.0,"energy":54.0,"effect":"beam","power":2.85,"radius":14.0},
    "orb_swarm":{"name":"Enxame Orbital","cooldown":9.0,"energy":50.0,"effect":"multi_projectile","power":1.60,"radius":12.0,"count":5},
    "gravity_well":{"name":"Poço Gravitacional","cooldown":9.0,"energy":45.0,"effect":"pull_area","power":1.85,"radius":6.2},
    "frost_barrage":{"name":"Rajada Glacial","cooldown":7.0,"energy":42.0,"effect":"multi_projectile","power":1.85,"radius":15.0,"count":5},
    "frost_zone":{"name":"Zona de Gelo","cooldown":12.0,"energy":52.0,"effect":"area","power":2.25,"radius":5.2},
    "ember_ring":{"name":"Anel Flamejante","cooldown":8.0,"energy":43.0,"effect":"area","power":2.35,"radius":5.5},
    "blazing_dash":{"name":"Investida Incandescente","cooldown":6.0,"energy":34.0,"effect":"dash","power":2.25,"radius":4.0},
    "thunder_chain":{"name":"Corrente Trovejante","cooldown":8.0,"energy":46.0,"effect":"chain","power":1.75,"radius":8.0,"count":6},
    "storm_field":{"name":"Campo de Tempestade","cooldown":13.0,"energy":58.0,"effect":"area","power":2.50,"radius":6.0},
    "singularity":{"name":"Singularidade","cooldown":12.0,"energy":60.0,"effect":"pull_area","power":2.75,"radius":7.0},
    "gravity_lock":{"name":"Trava Gravitacional","cooldown":14.0,"energy":62.0,"effect":"area","power":2.90,"radius":5.8},
    "venom_burst":{"name":"Explosão Veneno-Nova","cooldown":7.0,"energy":39.0,"effect":"area","power":2.45,"radius":5.4},
    "corrosive_edge":{"name":"Lâmina Corrosiva","cooldown":5.5,"energy":28.0,"effect":"dash","power":2.35,"radius":3.8},
    "prism_nova":{"name":"Nova Prismática","cooldown":11.0,"energy":56.0,"effect":"area","power":2.80,"radius":6.4},
    "prism_barrier":{"name":"Barreira Prisma","cooldown":15.0,"energy":52.0,"effect":"barrier","power":0.50,"radius":0.0,"duration":4.5}
}

const MONSTERS := {
    "aether_slime":{"name":"Slime de Aether","hp":55.0,"damage":8.0,"speed":2.4,"xp":25,"tier":1,"behavior":"melee","loot":["aether_core","slime_gel"]},
    "neon_stalker":{"name":"Perseguidor Neon","hp":85.0,"damage":14.0,"speed":3.7,"xp":42,"tier":2,"behavior":"charger","loot":["plasma_fiber","aether_core"]},
    "thornling":{"name":"Broto Espinhado","hp":72.0,"damage":11.0,"speed":2.8,"xp":34,"tier":1,"behavior":"mine_layer","loot":["thorn_fiber","herb"]},
    "briar_charger":{"name":"Investidor de Sarça","hp":118.0,"damage":22.0,"speed":4.8,"xp":68,"tier":2,"behavior":"charger","loot":["thorn_fiber","aether_core"]},
    "moss_mender":{"name":"Musgo Reparador","hp":105.0,"damage":7.0,"speed":2.0,"xp":62,"tier":2,"behavior":"healer","loot":["herb","moss_core"]},
    "gale_moth":{"name":"Mariposa do Vendaval","hp":95.0,"damage":18.0,"speed":4.0,"xp":70,"tier":2,"behavior":"flying","loot":["wing_dust","plasma_fiber"]},
    "crystal_sentinel":{"name":"Sentinela Cristalina","hp":310.0,"damage":32.0,"speed":1.3,"xp":120,"tier":4,"behavior":"shielded","loot":["crystal_shard","aether_core"]},
    "rift_hound":{"name":"Cão da Fenda","hp":130.0,"damage":21.0,"speed":4.4,"xp":78,"tier":3,"behavior":"assassin","loot":["rift_hide","aether_core"]},
    "phase_moth":{"name":"Mariposa Fásica","hp":115.0,"damage":25.0,"speed":4.6,"xp":92,"tier":3,"behavior":"teleporter","loot":["phase_dust","crystal_shard"]},
    "prism_oracle":{"name":"Oráculo Prisma","hp":145.0,"damage":29.0,"speed":2.4,"xp":110,"tier":3,"behavior":"ranged","loot":["prism_fragment","phase_dust"]},
    "frost_spitter":{"name":"Cuspidor Glacial","hp":170.0,"damage":34.0,"speed":1.9,"xp":130,"tier":4,"behavior":"sniper","loot":["frost_core","crystal_shard"]},
    "echo_blinker":{"name":"Eco Saltador","hp":125.0,"damage":27.0,"speed":3.2,"xp":105,"tier":3,"behavior":"splitter","loot":["echo_shard","aether_core"]},
    "dune_bomber":{"name":"Bombardeiro das Dunas","hp":132.0,"damage":38.0,"speed":2.1,"xp":118,"tier":4,"behavior":"bomber","loot":["sunstone","power_cell"]},
    "solar_scarab":{"name":"Escaravelho Solar","hp":155.0,"damage":23.0,"speed":3.6,"xp":95,"tier":3,"behavior":"shielded","loot":["sunstone","herb"]},
    "sand_sniper":{"name":"Atirador do Poente","hp":105.0,"damage":46.0,"speed":1.5,"xp":125,"tier":4,"behavior":"sniper","loot":["sunstone","plasma_fiber"]},
    "mirage_runner":{"name":"Corredor Miragem","hp":142.0,"damage":31.0,"speed":5.2,"xp":126,"tier":4,"behavior":"stealth","loot":["mirage_cloth","aether_core"]},
    "ember_beetle":{"name":"Besouro Braseiro","hp":190.0,"damage":35.0,"speed":2.4,"xp":135,"tier":4,"behavior":"berserker","loot":["ember_scale","power_cell"]},
    "burrower":{"name":"Escavador Solar","hp":210.0,"damage":39.0,"speed":2.6,"xp":150,"tier":5,"behavior":"teleporter","loot":["sunstone","ember_scale"]},
    "scrap_golem":{"name":"Golem de Sucata Cósmica","hp":240.0,"damage":24.0,"speed":1.5,"xp":95,"tier":3,"behavior":"knockback","loot":["scrap_plate","power_cell"]},
    "magnet_turret":{"name":"Torreta Magnética","hp":155.0,"damage":42.0,"speed":0.0,"xp":125,"tier":4,"behavior":"turret","loot":["magnet_core","power_cell"]},
    "coil_guardian":{"name":"Guardião de Bobina","hp":265.0,"damage":34.0,"speed":1.1,"xp":145,"tier":5,"behavior":"guardian","loot":["coil_metal","aether_core"]},
    "scrap_swarm":{"name":"Enxame de Sucata","hp":90.0,"damage":12.0,"speed":5.0,"xp":58,"tier":2,"behavior":"swarm","loot":["scrap_piece","coil_metal"]},
    "crusher":{"name":"Esmagador de Ferro","hp":330.0,"damage":50.0,"speed":1.4,"xp":185,"tier":5,"behavior":"berserker","loot":["scrap_plate","magnet_core"]},
    "repair_drone":{"name":"Drone Reparador","hp":115.0,"damage":10.0,"speed":2.8,"xp":72,"tier":3,"behavior":"healer","loot":["repair_chip","power_cell"]},
    "sky_watcher":{"name":"Vigia Celeste","hp":180.0,"damage":48.0,"speed":2.2,"xp":160,"tier":5,"behavior":"ranged","loot":["sky_fragment","prism_fragment"]},
    "rift_assassin":{"name":"Assassino da Fenda","hp":165.0,"damage":58.0,"speed":5.8,"xp":180,"tier":5,"behavior":"assassin","loot":["rift_hide","phase_dust"]},
    "gravity_wisp":{"name":"Fagulha Gravitacional","hp":140.0,"damage":36.0,"speed":4.5,"xp":135,"tier":4,"behavior":"orbit","loot":["gravity_shard","aether_core"]},
    "void_glider":{"name":"Planador do Vazio","hp":230.0,"damage":44.0,"speed":3.6,"xp":175,"tier":5,"behavior":"flying","loot":["void_membrane","sky_fragment"]},
    "storm_caster":{"name":"Conjurador da Tempestade","hp":205.0,"damage":52.0,"speed":2.0,"xp":190,"tier":5,"behavior":"ranged","loot":["storm_core","prism_fragment"]},
    "chrono_knight":{"name":"Cavaleiro Crono","hp":360.0,"damage":62.0,"speed":2.1,"xp":240,"tier":6,"behavior":"controller","loot":["chrono_gear","gravity_shard"]}
}

const BOSSES := {
    "overload_titan":{"name":"Titã de Sobrecarga","hp":1800.0,"phase2_ratio":0.68,"phase3_ratio":0.32,"speed":1.9,"projectile_damage":38.0,"summon_interval":10.0,"summon_id":"aether_slime","patterns":["burst","charge","summon","nova"],"arena":"rift_dome"},
    "thorn_warden":{"name":"Guardião dos Espinhos","hp":2100.0,"phase2_ratio":0.70,"phase3_ratio":0.35,"speed":1.65,"projectile_damage":44.0,"summon_interval":8.0,"summon_id":"thornling","patterns":["roots","burst","summon","charge"],"arena":"emerald_grove"},
    "skybreaker":{"name":"Quebra-Céus Aether","hp":2450.0,"phase2_ratio":0.66,"phase3_ratio":0.30,"speed":2.35,"projectile_damage":52.0,"summon_interval":7.0,"summon_id":"gale_moth","patterns":["beam","rain","summon","charge"],"arena":"sky_ruins"},
    "ember_behemoth":{"name":"Behemoth Incandescente","hp":2700.0,"phase2_ratio":0.72,"phase3_ratio":0.34,"speed":1.8,"projectile_damage":58.0,"summon_interval":9.0,"summon_id":"ember_beetle","patterns":["nova","charge","summon","ring"],"arena":"sunset_crater"},
    "rift_hydra":{"name":"Hidra do Vazio","hp":2900.0,"phase2_ratio":0.70,"phase3_ratio":0.33,"speed":1.7,"projectile_damage":64.0,"summon_interval":8.0,"summon_id":"rift_hound","patterns":["triple","summon","teleport","nova"],"arena":"void_lake"},
    "chrono_colossus":{"name":"Colosso Crono","hp":3200.0,"phase2_ratio":0.74,"phase3_ratio":0.36,"speed":1.4,"projectile_damage":70.0,"summon_interval":9.0,"summon_id":"chrono_knight","patterns":["timewave","charge","summon","ring"],"arena":"chrono_gate"},
    "prism_archon":{"name":"Arconte Prismático","hp":3500.0,"phase2_ratio":0.72,"phase3_ratio":0.38,"speed":1.75,"projectile_damage":76.0,"summon_interval":8.0,"summon_id":"prism_oracle","patterns":["beam","rain","summon","prism"],"arena":"prism_sanctum"},
    "void_harvester":{"name":"Ceifador do Vazio","hp":3900.0,"phase2_ratio":0.68,"phase3_ratio":0.34,"speed":2.05,"projectile_damage":84.0,"summon_interval":7.0,"summon_id":"void_glider","patterns":["harvest","dash","summon","nova"],"arena":"beyond_echo"}
}

const BIOMES := {
    "verdant_frontier":{"name":"Fronteira Esmeralda","ground":Color("#4ea66f"),"accent":Color("#8bd8ff"),"fog":Color("#9fe4c3")},
    "crystal_forest":{"name":"Floresta Cristalina","ground":Color("#547fc4"),"accent":Color("#a3fff4"),"fog":Color("#89aef0")},
    "sunset_desert":{"name":"Deserto do Poente","ground":Color("#d39a57"),"accent":Color("#ffcf7d"),"fog":Color("#f0bd8a")},
    "rust_canyons":{"name":"Cânions de Sucata","ground":Color("#8a5d50"),"accent":Color("#ffc36a"),"fog":Color("#9d7970")},
    "sky_ruins":{"name":"Ruínas do Céu","ground":Color("#6178b1"),"accent":Color("#e0f7ff"),"fog":Color("#b4c9ff")}
}

const QUESTS := [
    {"id":"story_01","title":"Recruta da Aetheria","description":"Conheça o núcleo de caçadores no terraço e abra o Portal.","type":"story","target":1,"xp":80,"next":"hunt_01"},
    {"id":"hunt_01","title":"Infestação de Gelatina","description":"Elimine Slimes de Aether na Fronteira Esmeralda.","type":"kill","target":6,"monster":"aether_slime","xp":140,"next":"hunt_02"},
    {"id":"hunt_02","title":"Perigo Veloz","description":"Cace Perseguidores Neon antes que alcancem o posto avançado.","type":"kill","target":5,"monster":"neon_stalker","xp":190,"next":"hunt_03"},
    {"id":"hunt_03","title":"Sucata Viva","description":"Derrote Golems de Sucata Cósmica e recupere células.","type":"kill","target":3,"monster":"scrap_golem","xp":260,"next":"rift_01"},
    {"id":"rift_01","title":"Primeira Fenda","description":"Conclua uma incursão de Fenda.","type":"rift","target":1,"xp":320,"next":"boss_01"},
    {"id":"boss_01","title":"Titã de Sobrecarga","description":"Derrote o chefe do Domo da Fenda.","type":"boss","target":1,"xp":520,"next":"chapter_02"},
    {"id":"chapter_02","title":"Ecos Cristalinos","description":"Complete uma expedição na Floresta Cristalina.","type":"map","target":1,"map":"crystal_forest","xp":340,"next":"chapter_03"},
    {"id":"chapter_03","title":"Cinzas do Poente","description":"Derrote 8 criaturas no Deserto do Poente.","type":"map_kill","target":8,"map":"sunset_desert","xp":420,"next":"chapter_04"},
    {"id":"chapter_04","title":"Máquinas Adormecidas","description":"Colete 5 placas em Cânions de Sucata.","type":"collect","target":5,"item":"scrap_plate","xp":460,"next":"chapter_05"},
    {"id":"chapter_05","title":"O Horizonte Partido","description":"Complete uma expedição nas Ruínas do Céu.","type":"map","target":1,"map":"sky_ruins","xp":550,"next":"boss_02"},
    {"id":"boss_02","title":"Arconte do Prisma","description":"Derrote um chefe de alta ameaça.","type":"boss_any","target":1,"xp":700,"next":"chapter_06"},
    {"id":"chapter_06","title":"Arquivo do Caçador","description":"Registre dez descobertas no diário.","type":"journal","target":10,"xp":620,"next":"chapter_07"},
    {"id":"chapter_07","title":"Além do Eco","description":"Derrote 3 chefes em Fendas instáveis.","type":"boss_any","target":3,"xp":900}
]

const EVENTS := [
    {"id":"research_destroy","name":"Pesquisa & Destruição","duration":900.0,"multiplier":1.5},
    {"id":"double_energy","name":"Energia do Caos em Dobro","duration":600.0,"multiplier":2.0},
    {"id":"transformation","name":"Transformação Aether","duration":480.0,"multiplier":1.25},
    {"id":"rift_surge","name":"Surto das Fendas","duration":720.0,"multiplier":1.75}
]

const STYLES := [
    {"id":"starter_hunter","name":"Caçador Neon","primary":Color("#ff7a74"),"secondary":Color("#4cc9f0"),"head":Color("#f4d19b")},
    {"id":"grove_guardian","name":"Guardião Verde","primary":Color("#4bbf8a"),"secondary":Color("#e9d56c"),"head":Color("#f0c89b")},
    {"id":"sky_runner","name":"Corredor Celeste","primary":Color("#6e8cff"),"secondary":Color("#eef7ff"),"head":Color("#d59f80")},
    {"id":"scrap_scout","name":"Explorador de Sucata","primary":Color("#cf804c"),"secondary":Color("#4a4d52"),"head":Color("#b88964")},
    {"id":"rift_mage","name":"Mago da Fenda","primary":Color("#8e6df2"),"secondary":Color("#ffd56a"),"head":Color("#e3b893")},
    {"id":"sunset_nomad","name":"Nômade do Poente","primary":Color("#e66b63"),"secondary":Color("#f6c76a"),"head":Color("#c58a68")},
    {"id":"aether_knight","name":"Cavaleiro Aether","primary":Color("#eef2f7"),"secondary":Color("#65d3ff"),"head":Color("#dcae90")},
    {"id":"night_hunter","name":"Caçador Noturno","primary":Color("#2c3656"),"secondary":Color("#9c8dff"),"head":Color("#c79678")},
    {"id":"forest_tech","name":"Tecnólogo da Floresta","primary":Color("#55b771"),"secondary":Color("#ffef9d"),"head":Color("#d6aa83")},
    {"id":"desert_fox","name":"Raposa do Deserto","primary":Color("#d98a58"),"secondary":Color("#7bd5d2"),"head":Color("#e4b087")},
    {"id":"crystal_runner","name":"Corredor Cristal","primary":Color("#5ad7e8"),"secondary":Color("#e682ff"),"head":Color("#e1a98c")},
    {"id":"stormbreaker","name":"Quebra-Tormentas","primary":Color("#4d5fe8"),"secondary":Color("#8cf2ff"),"head":Color("#c89278")}
]

const CORES := {
    "damage_1":{"type":"damage","value":0.08,"tier":1},"damage_2":{"type":"damage","value":0.16,"tier":2},
    "damage_3":{"type":"damage","value":0.27,"tier":3},"damage_4":{"type":"damage","value":0.40,"tier":4},
    "cooldown_1":{"type":"cooldown","value":0.07,"tier":1},"cooldown_2":{"type":"cooldown","value":0.13,"tier":2},
    "cooldown_3":{"type":"cooldown","value":0.21,"tier":3},"attack_speed_1":{"type":"attack_speed","value":0.06,"tier":1},
    "attack_speed_2":{"type":"attack_speed","value":0.12,"tier":2},"critical_1":{"type":"crit","value":0.10,"tier":1},
    "critical_2":{"type":"crit","value":0.18,"tier":2},"healing_1":{"type":"healing","value":0.10,"tier":1},
    "max_health_1":{"type":"max_health","value":0.08,"tier":1},"max_health_2":{"type":"max_health","value":0.16,"tier":2},
    "dodge_1":{"type":"dodge","value":0.10,"tier":1},"dodge_2":{"type":"dodge","value":0.17,"tier":2}
}

const ITEMS := {
    "aether_core":{"name":"Núcleo de Aether","value":30},"slime_gel":{"name":"Gel de Slime","value":8},
    "plasma_fiber":{"name":"Fibra de Plasma","value":16},"thorn_fiber":{"name":"Fibra Espinhada","value":14},
    "herb":{"name":"Erva Lumina","value":7},"moss_core":{"name":"Núcleo de Musgo","value":22},
    "wing_dust":{"name":"Pó de Asa","value":18},"crystal_shard":{"name":"Fragmento Cristalino","value":22},
    "phase_dust":{"name":"Pó Fásico","value":30},"prism_fragment":{"name":"Fragmento Prisma","value":34},
    "frost_core":{"name":"Núcleo Glacial","value":36},"echo_shard":{"name":"Estilhaço de Eco","value":29},
    "sunstone":{"name":"Pedra Solar","value":26},"mirage_cloth":{"name":"Tecido Miragem","value":25},
    "ember_scale":{"name":"Escama Incandescente","value":31},"scrap_plate":{"name":"Placa de Sucata","value":19},
    "power_cell":{"name":"Célula de Energia","value":28},"magnet_core":{"name":"Núcleo Magnético","value":38},
    "coil_metal":{"name":"Metal de Bobina","value":24},"scrap_piece":{"name":"Fragmento de Sucata","value":9},
    "repair_chip":{"name":"Chip Reparador","value":20},"sky_fragment":{"name":"Fragmento Celeste","value":41},
    "rift_hide":{"name":"Pele da Fenda","value":37},"gravity_shard":{"name":"Estilhaço Gravitacional","value":43},
    "void_membrane":{"name":"Membrana do Vazio","value":48},"storm_core":{"name":"Núcleo de Tempestade","value":50},
    "chrono_gear":{"name":"Engrenagem Crono","value":58},"oak_branch":{"name":"Galho de Carvalho","value":3},
    "stone":{"name":"Pedra","value":2},"health_kit":{"name":"Kit Sintético","value":40},
    "pulse_grenade":{"name":"Granada Pulsar","value":55},"healing_drone":{"name":"Drone de Cura","value":70},
    "aether_mine":{"name":"Mina de Aether","value":62},"rift_beacon":{"name":"Sinalizador de Fenda","value":90}
}

const RECIPES := [
    {"id":"health_kit","name":"Kit Sintético","requires":{"herb":3,"aether_core":1},"result":{"health_kit":1}},
    {"id":"pulse_grenade","name":"Granada Pulsar","requires":{"power_cell":1,"plasma_fiber":2},"result":{"pulse_grenade":1}},
    {"id":"healing_drone","name":"Drone de Cura","requires":{"moss_core":2,"aether_core":2,"power_cell":1},"result":{"healing_drone":1}},
    {"id":"aether_mine","name":"Mina de Aether","requires":{"scrap_plate":2,"aether_core":2,"prism_fragment":1},"result":{"aether_mine":1}},
    {"id":"rift_beacon","name":"Sinalizador de Fenda","requires":{"aether_core":6,"plasma_fiber":3,"gravity_shard":1},"result":{"rift_beacon":1}}
]
