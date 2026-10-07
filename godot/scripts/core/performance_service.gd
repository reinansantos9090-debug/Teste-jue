extends Node
signal quality_changed(profile: Dictionary)

const LEVELS := [
    {"name":"LOW","render_scale":0.72,"shadows":false,"vfx":0.55,"enemies":10},
    {"name":"MEDIUM","render_scale":0.82,"shadows":true,"vfx":0.75,"enemies":14},
    {"name":"HIGH","render_scale":0.92,"shadows":true,"vfx":0.90,"enemies":20}
]

var level := 1
var samples: Array[float] = []
var timer := 0.0

func _process(delta: float) -> void:
    timer += delta
    if timer < 1.0 or delta <= 0.0:
        return
    timer = 0.0
    samples.append(1.0 / delta)
    if samples.size() > 8:
        samples.pop_front()
    _adapt()

func current() -> Dictionary:
    return LEVELS[level]

func set_level(value: int) -> void:
    level = clampi(value, 0, LEVELS.size() - 1)
    quality_changed.emit(current())

func _adapt() -> void:
    if samples.size() < 5:
        return
    var average := 0.0
    for fps in samples:
        average += fps
    average /= float(samples.size())
    if average < 46.0 and level > 0:
        set_level(level - 1)
        samples.clear()
    elif average > 59.0 and level < LEVELS.size() - 1:
        set_level(level + 1)
        samples.clear()

func max_enemies() -> int:
    return int(current()["enemies"])

func vfx_multiplier() -> float:
    return float(current()["vfx"])

func shadows_enabled() -> bool:
    return bool(current()["shadows"])
