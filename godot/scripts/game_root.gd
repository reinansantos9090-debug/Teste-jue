extends Node3D
## Godot runtime foundation for the offline action RPG.

const PLAYER_SPEED := 6.0
const ARENA_SIZE := 42.0
const ENEMY_COUNT := 12

var player: Node3D
var camera: Camera3D
var hud: CanvasLayer
var hp := 100.0
var level := 1
var xp := 0
var attack_cooldown := 0.0
var enemies: Array[Node3D] = []
var day_clock := 0.0
var save_timer := 0.0

func _ready() -> void:
    _build_world()
    _build_player()
    _build_camera()
    _build_hud()
    _spawn_enemies()
    _load_offline_state()

func _process(delta: float) -> void:
    day_clock = fmod(day_clock + delta, 600.0)
    save_timer += delta
    _update_day_night()
    _update_player(delta)
    _update_enemies(delta)
    attack_cooldown = maxf(0.0, attack_cooldown - delta)
    if save_timer >= 5.0:
        save_timer = 0.0
        _save_offline_state()

func _build_world() -> void:
    var environment := WorldEnvironment.new()
    var env := Environment.new()
    env.background_mode = Environment.BG_COLOR
    env.background_color = Color(0.025, 0.04, 0.08)
    env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.ambient_light_color = Color(0.62, 0.68, 0.82)
    env.ambient_light_energy = 0.85
    environment.environment = env
    add_child(environment)

    var sun := DirectionalLight3D.new()
    sun.name = "Sun"
    sun.rotation_degrees = Vector3(-48.0, -28.0, 0.0)
    sun.light_energy = 1.2
    sun.shadow_enabled = true
    add_child(sun)

    var ground := MeshInstance3D.new()
    var mesh := BoxMesh.new()
    mesh.size = Vector3(ARENA_SIZE, 0.4, ARENA_SIZE)
    ground.mesh = mesh
    ground.position.y = -0.2
    ground.material_override = _material(Color(0.12, 0.23, 0.16))
    add_child(ground)

    for p in [
        Vector3(-14, 0, -10), Vector3(11, 0, -12), Vector3(-10, 0, 12),
        Vector3(14, 0, 11), Vector3(0, 0, -17), Vector3(0, 0, 17)
    ]:
        _spawn_prop(p)
    _spawn_portal(Vector3(0, 1.5, -14))

func _build_player() -> void:
    player = _actor_mesh(Color(0.24, 0.68, 1.0), Vector3(0, 1, 4))
    player.name = "Hunter"

func _build_camera() -> void:
    camera = Camera3D.new()
    camera.position = Vector3(0, 15, 18)
    camera.rotation_degrees = Vector3(-32, 0, 0)
    add_child(camera)
    camera.current = true

func _build_hud() -> void:
    hud = CanvasLayer.new()
    add_child(hud)
    var title := Label.new()
    title.text = "TESTE-JUE • OFFLINE HUNTER"
    title.position = Vector2(32, 24)
    title.add_theme_font_size_override("font_size", 26)
    hud.add_child(title)

    var info := Label.new()
    info.text = "WASD mover  •  ESPAÇO atacar  •  Portal: Expeditions / Rifts"
    info.position = Vector2(32, 62)
    info.add_theme_font_size_override("font_size", 17)
    hud.add_child(info)

    var stats := Label.new()
    stats.name = "Stats"
    stats.position = Vector2(32, 96)
    stats.add_theme_font_size_override("font_size", 18)
    hud.add_child(stats)

func _spawn_enemies() -> void:
    for i in ENEMY_COUNT:
        var angle := TAU * float(i) / float(ENEMY_COUNT)
        var radius := 9.0 + float(i % 4) * 2.2
        var enemy := _actor_mesh(
            Color(0.75, 0.28 + float(i % 3) * 0.08, 0.52),
            Vector3(cos(angle) * radius, 0.9, sin(angle) * radius)
        )
        enemy.name = "ChaosMonster_%02d" % i
        enemies.append(enemy)

func _update_player(delta: float) -> void:
    if player == null:
        return
    var input := Input.get_vector("move_left", "move_right", "move_forward", "move_back")
    var direction := Vector3(input.x, 0, input.y)
    if direction.length_squared() > 0.01:
        direction = direction.normalized()
        player.position += direction * PLAYER_SPEED * delta
        player.position.x = clampf(player.position.x, -ARENA_SIZE * 0.45, ARENA_SIZE * 0.45)
        player.position.z = clampf(player.position.z, -ARENA_SIZE * 0.45, ARENA_SIZE * 0.45)
        player.look_at(player.position + direction, Vector3.UP)

    if Input.is_action_pressed("attack") and attack_cooldown <= 0.0:
        _attack()
        attack_cooldown = 0.32

    camera.position = player.position + Vector3(0, 15, 18)
    camera.look_at(player.position + Vector3(0, 0.8, 0), Vector3.UP)
    var stats := hud.get_node_or_null("Stats") as Label
    if stats:
        stats.text = "HP %d/100   LV %d   XP %d   Monsters %d" % [int(hp), level, xp, enemies.size()]

func _update_enemies(delta: float) -> void:
    if player == null:
        return
    for enemy in enemies:
        if not is_instance_valid(enemy):
            continue
        var distance := enemy.position.distance_to(player.position)
        if distance > 2.4 and distance < 20.0:
            var direction := player.position - enemy.position
            direction.y = 0
            if direction.length_squared() > 0.01:
                enemy.position += direction.normalized() * delta * 1.25
        elif distance <= 2.4:
            hp = maxf(0.0, hp - delta * 4.0)
            if hp <= 0.0:
                hp = 100.0
                player.position = Vector3(0, 1, 4)

func _attack() -> void:
    for enemy in enemies.duplicate():
        if is_instance_valid(enemy) and enemy.position.distance_to(player.position) < 3.8:
            enemy.queue_free()
            enemies.erase(enemy)
            xp += 25
            if xp >= level * 100:
                xp -= level * 100
                level += 1

func _spawn_portal(pos: Vector3) -> void:
    var portal := MeshInstance3D.new()
    var torus := TorusMesh.new()
    torus.inner_radius = 1.6
    torus.outer_radius = 2.0
    portal.mesh = torus
    portal.position = pos
    portal.rotation_degrees.x = 90
    portal.material_override = _material(Color(0.2, 0.65, 1.0))
    add_child(portal)

func _spawn_prop(pos: Vector3) -> void:
    var prop := MeshInstance3D.new()
    var mesh := CylinderMesh.new()
    mesh.top_radius = 0.7
    mesh.bottom_radius = 1.0
    mesh.height = 2.0
    prop.mesh = mesh
    prop.position = pos + Vector3(0, 1, 0)
    prop.material_override = _material(Color(0.28, 0.34, 0.4))
    add_child(prop)

func _actor_mesh(color: Color, pos: Vector3) -> MeshInstance3D:
    var actor := MeshInstance3D.new()
    var mesh := CapsuleMesh.new()
    mesh.radius = 0.55
    mesh.height = 1.8
    actor.mesh = mesh
    actor.position = pos
    actor.material_override = _material(color)
    add_child(actor)
    return actor

func _material(color: Color) -> StandardMaterial3D:
    var material := StandardMaterial3D.new()
    material.albedo_color = color
    material.roughness = 0.72
    return material

func _update_day_night() -> void:
    var sun := get_node_or_null("Sun") as DirectionalLight3D
    if sun:
        var phase := day_clock / 600.0
        var cycle := sin(phase * TAU) * 0.5 + 0.5
        sun.rotation_degrees.x = lerpf(-35.0, -8.0, cycle)
        sun.light_energy = lerpf(0.35, 1.25, cycle)

func _save_offline_state() -> void:
    var state := {"hp": hp, "level": level, "xp": xp}
    var file := FileAccess.open("user://offline_save.json", FileAccess.WRITE)
    if file:
        file.store_string(JSON.stringify(state))

func _load_offline_state() -> void:
    if not FileAccess.file_exists("user://offline_save.json"):
        return
    var file := FileAccess.open("user://offline_save.json", FileAccess.READ)
    if file == null:
        return
    var parsed = JSON.parse_string(file.get_as_text())
    if parsed is Dictionary:
        hp = float(parsed.get("hp", 100.0))
        level = int(parsed.get("level", 1))
        xp = int(parsed.get("xp", 0))
