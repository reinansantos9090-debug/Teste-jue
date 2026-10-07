extends "res://scripts/gameplay/enemy_agent.gd"
## Reusable multi-phase boss with ranged pressure and timed adds.

signal phase_changed(phase: int)
signal summon_requested(monster_id: String, count: int)
signal projectile_requested(origin: Vector3, target: Vector3, damage: float)

var boss_id := "overload_titan"
var boss_stats: Dictionary = {}
var phase := 1
var special_timer := 4.0
var summon_timer := 10.0

func configure_boss(id: String, target_node: Node3D, database: Node) -> void:
    boss_id = id
    boss_stats = database.BOSSES.get(id, database.BOSSES["overload_titan"])
    target = target_node
    stats = {
        "name": boss_stats["name"],
        "hp": boss_stats["hp"],
        "damage": boss_stats["projectile_damage"],
        "speed": boss_stats["speed"],
        "xp": 650
    }
    hp = float(boss_stats["hp"])
    phase = 1
    special_timer = 3.0
    summon_timer = float(boss_stats["summon_interval"])

func _process(delta: float) -> void:
    if state == State.DEAD or target == null or not is_instance_valid(target):
        return
    attack_timer = maxf(0.0, attack_timer - delta)
    special_timer = maxf(0.0, special_timer - delta)
    summon_timer = maxf(0.0, summon_timer - delta)
    if phase == 1 and get_hp_ratio() <= float(boss_stats["phase2_ratio"]):
        phase = 2
        phase_changed.emit(2)
    _chase(delta)
    if special_timer <= 0.0:
        special_timer = 3.0 if phase == 1 else 2.2
        projectile_requested.emit(global_position + Vector3(0,1.4,0), target.global_position, float(boss_stats["projectile_damage"]) * (1.0 if phase == 1 else 1.25))
    if summon_timer <= 0.0:
        summon_timer = float(boss_stats["summon_interval"]) * (1.0 if phase == 1 else 0.7)
        summon_requested.emit("slime", 2 if phase == 1 else 3)

func _chase(delta: float) -> void:
    var direction := target.global_position - global_position
    direction.y = 0.0
    if direction.length_squared() <= 0.001:
        return
    var speed := float(stats["speed"]) * (1.0 if phase == 1 else 1.22)
    global_position += direction.normalized() * speed * delta
    look_at(global_position + direction.normalized(), Vector3.UP)
