extends Node3D
## Lightweight mobile enemy AI: Idle, Chase, Attack, Stunned and Dead.
signal defeated(actor: Node3D, xp: int)

enum State { IDLE, CHASE, ATTACK, STUNNED, DEAD }

var state := State.IDLE
var monster_id := "slime"
var stats: Dictionary = {}
var target: Node3D
var hp := 1.0
var attack_timer := 0.0
var stun_timer := 0.0
var wander_timer := 0.0
var rng := RandomNumberGenerator.new()
var visual: Node3D

func configure(id: String, target_node: Node3D, database: Node) -> void:
    monster_id = id
    stats = database.MONSTERS.get(id, database.MONSTERS["slime"])
    target = target_node
    hp = float(stats["hp"])
    rng.seed = abs(hash(str(id) + str(get_instance_id())))

func attach_visual(node: Node3D) -> void:
    visual = node
    add_child(visual)

func _process(delta: float) -> void:
    if state == State.DEAD or target == null or not is_instance_valid(target):
        return
    attack_timer = maxf(0.0, attack_timer - delta)
    stun_timer = maxf(0.0, stun_timer - delta)
    wander_timer -= delta
    var distance := global_position.distance_to(target.global_position)
    if stun_timer > 0.0:
        state = State.STUNNED
        return
    if distance <= 2.15:
        state = State.ATTACK
        _attack()
    elif distance <= 10.0:
        state = State.CHASE
        _chase(delta)
    else:
        state = State.IDLE
        _wander(delta)

func _chase(delta: float) -> void:
    var direction := target.global_position - global_position
    direction.y = 0.0
    if direction.length_squared() <= 0.001:
        return
    direction = direction.normalized()
    global_position += direction * float(stats.get("speed", 2.0)) * delta
    look_at(global_position + direction, Vector3.UP)

func _wander(delta: float) -> void:
    if wander_timer <= 0.0:
        wander_timer = rng.randf_range(1.0, 2.8)
    var phase := Time.get_ticks_msec() * 0.001
    var direction := Vector3(cos(phase + float(get_instance_id() % 11)), 0, sin(phase + float(get_instance_id() % 7)))
    global_position += direction * 0.12 * delta

func _attack() -> void:
    if attack_timer > 0.0:
        return
    attack_timer = 1.25
    if target.has_method("take_enemy_damage"):
        target.take_enemy_damage(float(stats.get("damage", 5.0)))

func take_damage(amount: float, critical := false) -> void:
    if state == State.DEAD:
        return
    hp -= amount
    stun_timer = 0.16 if critical else 0.08
    if hp <= 0.0:
        hp = 0.0
        state = State.DEAD
        defeated.emit(self, int(stats.get("xp", 10)))

func get_hp_ratio() -> float:
    return clampf(hp / maxf(1.0, float(stats.get("hp", 1.0))), 0.0, 1.0)
