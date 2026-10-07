extends Node
signal state_changed
signal inventory_changed
signal quest_changed
signal story_changed

const SCHEMA := 7
var data: Dictionary = {}

func _ready() -> void:
    reset()

func reset() -> void:
    data = {
        "schema": SCHEMA,
        "hunter": {"name":"Aether Hunter","level":1,"xp":0,"xp_to_next":120,"hp":120,"max_hp":120,"energy":100.0,"max_energy":100.0,"gold":0,"chaos_energy":0,"rank":0},
        "loadout": {"weapon":"volt_blades","ability_1":"shock_dash","ability_2":"aether_burst","ability_3":"nanodrone","core_slots":["damage","cooldown","dodge"]},
        "inventory": {"aether_core":8,"slime_gel":12,"plasma_fiber":4,"scrap_plate":5,"power_cell":3,"oak_branch":8,"stone":6,"herb":10},
        "quests": {"active":["story_01"],"progress":{},"completed":[]},
        "story": {"chapter":1,"scene":0,"flags":{"tutorial_complete":false,"rooftop_unlocked":true},"journal":[]},
        "world": {"map_id":"verdant_frontier","day_clock":0.0,"completed_expeditions":0,"completed_rifts":0},
        "wardrobe": {"style":"starter_hunter","unlocked":["starter_hunter"],"rides":[],"emotes":["wave"]},
        "cores": {"owned":["damage_1","cooldown_1","dodge_1"],"equipped":["damage_1","cooldown_1","dodge_1"]},
        "events": {"research_destroy":0,"transformation_unlocked":false},
        "statistics": {"kills":0,"bosses":0,"deaths":0,"damage_dealt":0.0,"distance":0.0,"play_seconds":0.0}
    }
    state_changed.emit()

func apply_loaded(loaded: Dictionary) -> void:
    reset()
    _merge_recursive(data, loaded)
    state_changed.emit()
    inventory_changed.emit()
    quest_changed.emit()
    story_changed.emit()

func _merge_recursive(target: Dictionary, source: Dictionary) -> void:
    for key in source.keys():
        if target.get(key) is Dictionary and source[key] is Dictionary:
            _merge_recursive(target[key], source[key])
        else:
            target[key] = source[key]

func get_hunter() -> Dictionary:
    return data["hunter"]

func get_inventory() -> Dictionary:
    return data["inventory"]

func add_item(id: String, amount: int) -> void:
    data["inventory"][id] = int(data["inventory"].get(id, 0)) + amount
    inventory_changed.emit()
    state_changed.emit()

func remove_item(id: String, amount: int) -> bool:
    var current := int(data["inventory"].get(id, 0))
    if current < amount:
        return false
    data["inventory"][id] = current - amount
    inventory_changed.emit()
    state_changed.emit()
    return true

func add_xp(amount: int) -> int:
    var gained := 0
    var hunter := data["hunter"]
    hunter["xp"] = int(hunter["xp"]) + amount
    while int(hunter["xp"]) >= int(hunter["xp_to_next"]):
        hunter["xp"] = int(hunter["xp"]) - int(hunter["xp_to_next"])
        hunter["level"] = int(hunter["level"]) + 1
        hunter["xp_to_next"] = 120 + (int(hunter["level"]) - 1) * 35
        hunter["max_hp"] = int(hunter["max_hp"]) + 12
        hunter["hp"] = hunter["max_hp"]
        hunter["max_energy"] = float(hunter["max_energy"]) + 4.0
        gained += 1
    state_changed.emit()
    return gained

func set_story_flag(id: String, value: bool) -> void:
    data["story"]["flags"][id] = value
    story_changed.emit()
    state_changed.emit()

func increment_stat(id: String, amount: float = 1.0) -> void:
    data["statistics"][id] = float(data["statistics"].get(id, 0.0)) + amount
    state_changed.emit()
