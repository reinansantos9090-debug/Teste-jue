extends Node
signal changed

var game_state: Node

func setup(state: Node) -> void:
    game_state = state
    if game_state and not game_state.inventory_changed.is_connected(_emit):
        game_state.inventory_changed.connect(_emit)

func _emit() -> void:
    changed.emit()

func quantity(id: String) -> int:
    if game_state == null:
        return 0
    return int(game_state.get_inventory().get(id, 0))

func has_cost(cost: Dictionary) -> bool:
    for item in cost.keys():
        if quantity(str(item)) < int(cost[item]):
            return false
    return true

func craft(recipe: Dictionary) -> bool:
    var cost: Dictionary = recipe.get("requires", {})
    if not has_cost(cost):
        return false
    for item in cost.keys():
        game_state.remove_item(str(item), int(cost[item]))
    for item in recipe.get("result", {}).keys():
        game_state.add_item(str(item), int(recipe["result"][item]))
    return true

func summary() -> String:
    if game_state == null:
        return ""
    var parts: Array[String] = []
    for key in game_state.get_inventory().keys():
        var amount := quantity(str(key))
        if amount > 0:
            parts.append("%s x%d" % [str(key), amount])
    return ", ".join(parts)
