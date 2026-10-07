extends Node
var game_state: Node
var audio_service: Node
var rng := RandomNumberGenerator.new()

func setup(state: Node, audio: Node) -> void:
    game_state = state
    audio_service = audio
    rng.seed = 9917

func player_damage(weapon: Dictionary, level: int, bonus: float, crit_bonus: float) -> float:
    var base := float(weapon.get("damage",10.0)) + float(level - 1) * 2.5
    var amount := base * (1.0 + bonus)
    if rng.randf() < clampf(0.08 + crit_bonus,0.0,0.65):
        amount *= 1.75
    return amount

func hit(target: Node3D, amount: float, critical := false) -> void:
    if not is_instance_valid(target):
        return
    if target.has_method("take_damage"):
        target.take_damage(amount,critical)
        if game_state:
            game_state.increment_stat("damage_dealt",amount)
        if audio_service:
            audio_service.hit()

func spawn_impact(parent: Node3D, position: Vector3, color := Color("#ffe16a")) -> void:
    var fx := GPUParticles3D.new()
    fx.amount = 8
    fx.lifetime = 0.3
    fx.one_shot = true
    fx.position = position
    var draw := SphereMesh.new()
    draw.radius = 0.05
    draw.height = 0.1
    var material := StandardMaterial3D.new()
    material.albedo_color = color
    draw.material = material
    fx.draw_pass_1 = draw
    parent.add_child(fx)
    fx.emitting = true
