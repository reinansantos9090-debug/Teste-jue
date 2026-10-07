extends Node
signal updated

var game_state: Node
var database: Node

func setup(state: Node, content: Node) -> void:
    game_state = state
    database = content

func start(id: String) -> void:
    var active: Array = game_state.data["quests"]["active"]
    if id not in active and id not in game_state.data["quests"]["completed"]:
        active.append(id)
        game_state.data["quests"]["progress"][id] = 0
        game_state.quest_changed.emit()
        updated.emit()

func add_progress(id: String, amount := 1) -> void:
    if id not in game_state.data["quests"]["active"]:
        return
    var value := int(game_state.data["quests"]["progress"].get(id, 0)) + amount
    game_state.data["quests"]["progress"][id] = value
    var quest := get_quest(id)
    if value >= int(quest.get("target", 1)):
        complete(id)
    else:
        game_state.quest_changed.emit()
        updated.emit()

func complete(id: String) -> void:
    var active: Array = game_state.data["quests"]["active"]
    var completed: Array = game_state.data["quests"]["completed"]
    active.erase(id)
    if id not in completed:
        completed.append(id)
    var quest := get_quest(id)
    game_state.add_xp(int(quest.get("xp", 0)))
    game_state.quest_changed.emit()
    updated.emit()

func get_quest(id: String) -> Dictionary:
    if database == null:
        return {}
    for quest in database.QUESTS:
        if quest["id"] == id:
            return quest
    return {}

func active_summary() -> String:
    var active: Array = game_state.data["quests"]["active"]
    if active.is_empty():
        return "Nenhum contrato ativo"
    var id := str(active[0])
    var quest := get_quest(id)
    var value := int(game_state.data["quests"]["progress"].get(id, 0))
    return "%s  %d/%d" % [quest.get("title", id), value, int(quest.get("target", 1))]
