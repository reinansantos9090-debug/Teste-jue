extends Node
signal event_changed

var game_state: Node
var database: Node
var active_id := ""
var remaining := 0.0

func setup(state: Node, content: Node) -> void:
    game_state = state
    database = content

func start(id: String) -> bool:
    if database == null:
        return false
    for event in database.EVENTS:
        if event["id"] == id:
            active_id = id
            remaining = float(event["duration"])
            event_changed.emit()
            return true
    return false

func tick(delta: float) -> void:
    if active_id.is_empty():
        return
    remaining = maxf(0.0, remaining - delta)
    if remaining <= 0.0:
        active_id = ""
        event_changed.emit()

func multiplier() -> float:
    for event in database.EVENTS:
        if event["id"] == active_id:
            return float(event.get("multiplier", 1.0))
    return 1.0

func title() -> String:
    for event in database.EVENTS:
        if event["id"] == active_id:
            return str(event["name"])
    return "Nenhum evento ativo"
