extends Node
const PATH := "user://aetheria_echoes_save_v7.json"
const BACKUP_PATH := "user://aetheria_echoes_save_v7.bak.json"
const SAVE_INTERVAL := 15.0
var elapsed := 0.0
var dirty := false
var game_state: Node

func setup(state: Node) -> void:
    game_state = state
    if game_state and not game_state.state_changed.is_connected(mark_dirty):
        game_state.state_changed.connect(mark_dirty)

func _process(delta: float) -> void:
    elapsed += delta
    if elapsed >= SAVE_INTERVAL and dirty:
        elapsed = 0.0
        save_now()

func mark_dirty() -> void:
    dirty = true

func save_now() -> bool:
    if game_state == null:
        return false
    var payload: Dictionary = game_state.data.duplicate(true)
    payload["saved_at_unix"] = Time.get_unix_time_from_system()
    if FileAccess.file_exists(PATH):
        var old := FileAccess.open(PATH, FileAccess.READ)
        if old:
            var backup := FileAccess.open(BACKUP_PATH, FileAccess.WRITE)
            if backup:
                backup.store_string(old.get_as_text())
    var file := FileAccess.open(PATH, FileAccess.WRITE)
    if file == null:
        return false
    file.store_string(JSON.stringify(payload))
    file.flush()
    dirty = false
    return true

func load_now() -> bool:
    if game_state == null:
        return false
    var parsed := _read(PATH)
    if parsed.is_empty():
        parsed = _read(BACKUP_PATH)
    if parsed.is_empty():
        return false
    game_state.apply_loaded(parsed)
    dirty = false
    return true

func _read(path: String) -> Dictionary:
    if not FileAccess.file_exists(path):
        return {}
    var file := FileAccess.open(path, FileAccess.READ)
    if file == null:
        return {}
    var parsed = JSON.parse_string(file.get_as_text())
    return parsed if parsed is Dictionary else {}

func force_save_and_flush() -> void:
    save_now()

func _notification(what: int) -> void:
    if what == NOTIFICATION_WM_CLOSE_REQUEST:
        force_save_and_flush()
