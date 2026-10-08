extends Node
## Five authored expedition maps. Each biome owns its encounter pool,
## difficulty, obstacle layout and environmental hazard.

var maps: Dictionary = {}

func _ready() -> void:
    maps = {
        "verdant_frontier": {
            "name":"Fronteira Esmeralda",
            "difficulty":1.00,
            "duration":150.0,
            "goal":10,
            "hazard":"raízes móveis",
            "ground":Color("#4ea66f"),
            "accent":Color("#8bd8ff"),
            "fog":Color("#9fe4c3"),
            "enemies":["aether_slime","neon_stalker","thornling","briar_charger","moss_mender","gale_moth"],
            "obstacles":[
                {"shape":"rock","pos":Vector3(-9,0,-7),"size":Vector3(2.0,1.4,1.7)},
                {"shape":"rock","pos":Vector3(9,0,-8),"size":Vector3(2.4,1.8,2.0)},
                {"shape":"tree","pos":Vector3(-13,0,5),"size":Vector3(1.2,2.7,1.2)},
                {"shape":"tree","pos":Vector3(12,0,6),"size":Vector3(1.0,3.0,1.0)},
                {"shape":"rock","pos":Vector3(-3,0,10),"size":Vector3(2.8,1.2,1.6)},
                {"shape":"tree","pos":Vector3(4,0,-12),"size":Vector3(1.1,2.4,1.1)}
            ]
        },
        "crystal_forest": {
            "name":"Floresta Cristalina",
            "difficulty":1.25,
            "duration":165.0,
            "goal":12,
            "hazard":"pulsos cristalinos",
            "ground":Color("#547fc4"),
            "accent":Color("#a3fff4"),
            "fog":Color("#89aef0"),
            "enemies":["crystal_sentinel","rift_hound","phase_moth","prism_oracle","frost_spitter","echo_blinker"],
            "obstacles":[
                {"shape":"crystal","pos":Vector3(-10,0,-9),"size":Vector3(1.4,2.8,1.4)},
                {"shape":"crystal","pos":Vector3(10,0,-7),"size":Vector3(1.2,2.4,1.2)},
                {"shape":"crystal","pos":Vector3(-6,0,8),"size":Vector3(1.6,3.1,1.6)},
                {"shape":"rock","pos":Vector3(8,0,10),"size":Vector3(2.2,1.5,1.8)},
                {"shape":"crystal","pos":Vector3(1,0,-13),"size":Vector3(1.1,2.2,1.1)},
                {"shape":"rock","pos":Vector3(13,0,1),"size":Vector3(2.0,1.7,2.0)}
            ]
        },
        "sunset_desert": {
            "name":"Deserto do Poente",
            "difficulty":1.45,
            "duration":175.0,
            "goal":14,
            "hazard":"ondas de calor",
            "ground":Color("#d39a57"),
            "accent":Color("#ffcf7d"),
            "fog":Color("#f0bd8a"),
            "enemies":["dune_bomber","solar_scarab","sand_sniper","mirage_runner","ember_beetle","burrower"],
            "obstacles":[
                {"shape":"rock","pos":Vector3(-12,0,-8),"size":Vector3(3.0,1.8,2.3)},
                {"shape":"rock","pos":Vector3(11,0,-10),"size":Vector3(2.5,1.5,2.5)},
                {"shape":"cactus","pos":Vector3(-9,0,7),"size":Vector3(0.8,2.2,0.8)},
                {"shape":"cactus","pos":Vector3(8,0,5),"size":Vector3(1.0,2.4,1.0)},
                {"shape":"rock","pos":Vector3(0,0,11),"size":Vector3(3.2,1.4,1.8)},
                {"shape":"cactus","pos":Vector3(4,0,-13),"size":Vector3(0.8,2.0,0.8)}
            ]
        },
        "rust_canyons": {
            "name":"Cânions de Sucata",
            "difficulty":1.70,
            "duration":185.0,
            "goal":16,
            "hazard":"campos magnéticos",
            "ground":Color("#8a5d50"),
            "accent":Color("#ffc36a"),
            "fog":Color("#9d7970"),
            "enemies":["scrap_golem","magnet_turret","coil_guardian","scrap_swarm","crusher","repair_drone"],
            "obstacles":[
                {"shape":"scrap","pos":Vector3(-11,0,-9),"size":Vector3(2.4,2.4,2.0)},
                {"shape":"scrap","pos":Vector3(9,0,-8),"size":Vector3(3.0,1.9,2.4)},
                {"shape":"scrap","pos":Vector3(-8,0,8),"size":Vector3(2.0,3.0,2.0)},
                {"shape":"scrap","pos":Vector3(11,0,7),"size":Vector3(2.8,2.2,2.2)},
                {"shape":"rock","pos":Vector3(0,0,-11),"size":Vector3(3.5,1.6,2.0)},
                {"shape":"scrap","pos":Vector3(1,0,11),"size":Vector3(2.5,2.0,2.6)}
            ]
        },
        "sky_ruins": {
            "name":"Ruínas do Céu",
            "difficulty":2.00,
            "duration":195.0,
            "goal":18,
            "hazard":"rajadas gravitacionais",
            "ground":Color("#6178b1"),
            "accent":Color("#e0f7ff"),
            "fog":Color("#b4c9ff"),
            "enemies":["sky_watcher","rift_assassin","gravity_wisp","void_glider","storm_caster","chrono_knight"],
            "obstacles":[
                {"shape":"ruin","pos":Vector3(-12,0,-9),"size":Vector3(2.2,3.4,2.2)},
                {"shape":"ruin","pos":Vector3(12,0,-9),"size":Vector3(2.5,4.0,2.5)},
                {"shape":"ruin","pos":Vector3(-9,0,8),"size":Vector3(2.8,3.1,2.0)},
                {"shape":"ruin","pos":Vector3(10,0,9),"size":Vector3(2.2,3.7,2.2)},
                {"shape":"ruin","pos":Vector3(0,0,-12),"size":Vector3(2.0,2.8,2.4)},
                {"shape":"ruin","pos":Vector3(0,0,11),"size":Vector3(3.0,2.4,2.0)}
            ]
        }
    }

func get_map(id: String) -> Dictionary:
    return maps.get(id, maps["verdant_frontier"])

func enemy_pool(id: String) -> Array:
    return get_map(id).get("enemies", [])

func difficulty(id: String) -> float:
    return float(get_map(id).get("difficulty", 1.0))

func activity_goal(id: String) -> int:
    return int(get_map(id).get("goal", 10))

func duration(id: String) -> float:
    return float(get_map(id).get("duration", 150.0))

func hazard(id: String) -> String:
    return str(get_map(id).get("hazard", ""))

func blocked(id: String, point: Vector3, radius: float = 0.65) -> bool:
    for obstacle in get_map(id).get("obstacles", []):
        var size: Vector3 = obstacle["size"]
        var pos: Vector3 = obstacle["pos"]
        if absf(point.x - pos.x) <= size.x * 0.5 + radius and absf(point.z - pos.z) <= size.z * 0.5 + radius:
            return true
    return false
