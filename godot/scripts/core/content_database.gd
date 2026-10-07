extends Node
## Original content catalog for the offline Aetheria action RPG.
## It follows the PDF's combat/crafting/quest structure and the public mo.co
## categories without copying proprietary names, art, code or assets.

const WEAPONS := {
    "volt_blades": {"name":"Lâminas Voltáicas","damage":24.0,"rate":0.24,"range":3.2,"type":"melee"},
    "toxic_bow": {"name":"Arco Tóxico","damage":34.0,"rate":0.55,"range":15.0,"type":"ranged"},
    "pulse_cannon": {"name":"Canhão Pulsar","damage":46.0,"rate":0.72,"range":18.0,"type":"ranged"},
    "scrap_hammer": {"name":"Martelo Sucateiro","damage":58.0,"rate":0.88,"range":3.6,"type":"melee"},
    "aether_lance": {"name":"Lança de Aether","damage":31.0,"rate":0.35,"range":5.5,"type":"melee"},
    "orbit_orbs": {"name":"Orbes Orbitais","damage":21.0,"rate":0.18,"range":10.0,"type":"ranged"}
}

const ABILITIES := {
    "shock_dash": {"name":"Avanço de Choque","cooldown":4.0,"energy":20.0},
    "aether_burst": {"name":"Explosão Aether","cooldown":7.0,"energy":35.0},
    "nanodrone": {"name":"Nanodrone de Cura","cooldown":12.0,"energy":40.0},
    "gravity_well": {"name":"Poço Gravitacional","cooldown":9.0,"energy":45.0},
    "meteor_shards": {"name":"Fragmentos Meteóricos","cooldown":10.0,"energy":55.0},
    "overdrive": {"name":"Sobrecarga","cooldown":18.0,"energy":70.0}
}

const MONSTERS := {
    "slime": {"name":"Slime de Aether","hp":55.0,"damage":8.0,"speed":2.4,"xp":25,"tier":1},
    "stalker": {"name":"Perseguidor Neon","hp":85.0,"damage":14.0,"speed":3.7,"xp":42,"tier":2},
    "scrap_golem": {"name":"Golem de Sucata Cósmica","hp":240.0,"damage":24.0,"speed":1.5,"xp":95,"tier":3},
    "rift_hound": {"name":"Cão da Fenda","hp":130.0,"damage":21.0,"speed":4.4,"xp":78,"tier":3},
    "chaos_moth": {"name":"Mariposa de Caos","hp":95.0,"damage":18.0,"speed":4.0,"xp":70,"tier":2},
    "crystal_guard": {"name":"Sentinela Cristalina","hp":310.0,"damage":32.0,"speed":1.3,"xp":120,"tier":4},
    "mire_colossus": {"name":"Colosso do Brejo","hp":420.0,"damage":38.0,"speed":1.0,"xp":175,"tier":5}
}

const BOSSES := {
    "overload_titan": {"name":"Titã de Sobrecarga","hp":1200.0,"phase2_ratio":0.5,"speed":2.0,"projectile_damage":34.0,"summon_interval":10.0,"arena":"rift_dome"},
    "thorn_warden": {"name":"Guardião dos Espinhos","hp":1450.0,"phase2_ratio":0.55,"speed":1.65,"projectile_damage":42.0,"summon_interval":8.0,"arena":"emerald_grove"},
    "skybreaker": {"name":"Quebra-Céus Aether","hp":1800.0,"phase2_ratio":0.45,"speed":2.25,"projectile_damage":51.0,"summon_interval":7.0,"arena":"sky_ruins"}
}

const BIOMES := {
    "verdant_frontier": {"name":"Fronteira Esmeralda","ground":Color("#4ea66f"),"accent":Color("#8bd8ff"),"fog":Color("#9fe4c3")},
    "crystal_forest": {"name":"Floresta Cristalina","ground":Color("#547fc4"),"accent":Color("#a3fff4"),"fog":Color("#89aef0")},
    "sunset_desert": {"name":"Deserto do Poente","ground":Color("#d39a57"),"accent":Color("#ffcf7d"),"fog":Color("#f0bd8a")},
    "rust_canyons": {"name":"Cânions de Sucata","ground":Color("#8a5d50"),"accent":Color("#ffc36a"),"fog":Color("#9d7970")},
    "sky_ruins": {"name":"Ruínas do Céu","ground":Color("#6178b1"),"accent":Color("#e0f7ff"),"fog":Color("#b4c9ff")}
}

const QUESTS := [
    {"id":"story_01","title":"Recruta da Aetheria","description":"Conheça o núcleo de caçadores no terraço e abra o Portal.","type":"story","target":1,"xp":80},
    {"id":"hunt_01","title":"Infestação de Gelatina","description":"Elimine Slimes de Aether.","type":"kill","target":6,"monster":"slime","xp":140},
    {"id":"hunt_02","title":"Perigo Veloz","description":"Cace Perseguidores Neon.","type":"kill","target":5,"monster":"stalker","xp":190},
    {"id":"hunt_03","title":"Sucata Viva","description":"Derrote Golems de Sucata Cósmica.","type":"kill","target":3,"monster":"scrap_golem","xp":260},
    {"id":"rift_01","title":"Primeira Fenda","description":"Conclua um objetivo de Rift antes do cronômetro.","type":"rift","target":1,"xp":320},
    {"id":"boss_01","title":"Titã de Sobrecarga","description":"Derrote o chefe do Domo da Fenda.","type":"boss","target":1,"xp":520}
]

const EVENTS := [
    {"id":"research_destroy","name":"Pesquisa & Destruição","duration":900.0,"multiplier":1.5},
    {"id":"double_energy","name":"Energia do Caos em Dobro","duration":600.0,"multiplier":2.0},
    {"id":"transformation","name":"Transformação Aether","duration":480.0,"multiplier":1.0}
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
    "damage_1":{"type":"damage","value":0.08},
    "damage_2":{"type":"damage","value":0.16},
    "cooldown_1":{"type":"cooldown","value":0.07},
    "cooldown_2":{"type":"cooldown","value":0.13},
    "dodge_1":{"type":"dodge","value":0.10},
    "max_health_1":{"type":"max_health","value":0.08},
    "crit_1":{"type":"crit","value":0.10}
}

const RECIPES := [
    {"id":"health_kit","name":"Kit Sintético","requires":{"herb":3,"aether_core":1},"result":{"health_kit":1}},
    {"id":"aether_blade","name":"Lâmina Aether","requires":{"scrap_plate":4,"power_cell":2,"aether_core":3},"result":{"aether_lance":1}},
    {"id":"rift_beacon","name":"Sinalizador de Fenda","requires":{"aether_core":6,"plasma_fiber":3},"result":{"rift_beacon":1}}
]
