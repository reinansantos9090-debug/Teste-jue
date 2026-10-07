extends Node
signal level_up(level: int)
signal rank_changed(rank: int)
var state: Node

func setup(game_state: Node) -> void:
    state = game_state

func award_xp(amount: int) -> void:
    if state == null:return
    var levels := state.add_xp(amount)
    if levels > 0:
        level_up.emit(int(state.get_hunter()["level"]))
    _update_rank()

func _update_rank() -> void:
    var h := state.get_hunter()
    var rank := int(h["level"]) / 5
    if rank != int(h["rank"]):
        h["rank"] = rank
        rank_changed.emit(rank)
        state.state_changed.emit()
