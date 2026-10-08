extends Node3D
## Aetheria production hunter avatar.
## Original stylized low-poly presentation designed for mobile.
## The Skeleton3D is the animation rig authority; lightweight mesh parts
## keep the visual budget controlled while still supporting expressive poses.

signal animation_changed(state_name: String)
signal weapon_changed(weapon_id: String)

var style_id := "starter_hunter"
var weapon_id := "volt_blades"
var body_color := Color("#ff7a74")
var accent_color := Color("#4cc9f0")
var skin_color := Color("#f4d19b")

var skeleton: Skeleton3D
var model_root: Node3D
var weapon_root: Node3D
var left_arm: Node3D
var right_arm: Node3D
var left_forearm: Node3D
var right_forearm: Node3D
var left_leg: Node3D
var right_leg: Node3D
var torso: Node3D
var head: Node3D
var face: Node3D
var backpack: Node3D
var weapon_model: Node3D

var _bones: Dictionary = {}
var _base_transforms: Dictionary = {}
var _time := 0.0
var _locomotion := Vector2.ZERO
var _animation := "idle"
var _animation_time := 0.0
var _death_tween: Tween
var _weapon_swing := 0.0

func setup(style: Dictionary, initial_weapon: String) -> void:
    style_id = str(style.get("id", "starter_hunter"))
    body_color = style.get("primary", Color("#ff7a74"))
    accent_color = style.get("secondary", Color("#4cc9f0"))
    skin_color = style.get("head", Color("#f4d19b"))
    weapon_id = initial_weapon
    _build()

func _ready() -> void:
    if model_root == null:
        _build()

func _build() -> void:
    for child in get_children():
        child.queue_free()

    skeleton = Skeleton3D.new()
    skeleton.name = "Rig"
    add_child(skeleton)
    _create_rig()

    model_root = Node3D.new()
    model_root.name = "HunterModel"
    add_child(model_root)

    _build_body()
    _build_face()
    _build_gear()
    _build_weapon()
    _capture_base_transforms()

func _create_rig() -> void:
    _bones.clear()
    var root_idx := _add_bone("root", -1, Vector3(0, 0, 0))
    var hips_idx := _add_bone("hips", root_idx, Vector3(0, 0.65, 0))
    var spine_idx := _add_bone("spine", hips_idx, Vector3(0, 0.52, 0))
    var chest_idx := _add_bone("chest", spine_idx, Vector3(0, 0.42, 0))
    var neck_idx := _add_bone("neck", chest_idx, Vector3(0, 0.28, 0))
    _add_bone("head", neck_idx, Vector3(0, 0.22, 0))
    var l_shoulder := _add_bone("upper_arm_l", chest_idx, Vector3(-0.45, 0.16, 0))
    _add_bone("forearm_l", l_shoulder, Vector3(-0.38, -0.04, 0))
    _add_bone("hand_l", _bones["forearm_l"], Vector3(-0.28, -0.02, 0))
    var r_shoulder := _add_bone("upper_arm_r", chest_idx, Vector3(0.45, 0.16, 0))
    _add_bone("forearm_r", r_shoulder, Vector3(0.38, -0.04, 0))
    _add_bone("hand_r", _bones["forearm_r"], Vector3(0.28, -0.02, 0))
    var l_thigh := _add_bone("thigh_l", hips_idx, Vector3(-0.20, -0.38, 0))
    _add_bone("calf_l", l_thigh, Vector3(0, -0.43, 0))
    _add_bone("foot_l", _bones["calf_l"], Vector3(0, -0.25, -0.05))
    var r_thigh := _add_bone("thigh_r", hips_idx, Vector3(0.20, -0.38, 0))
    _add_bone("calf_r", r_thigh, Vector3(0, -0.43, 0))
    _add_bone("foot_r", _bones["calf_r"], Vector3(0, -0.25, -0.05))

func _add_bone(name_value: String, parent: int, rest_position: Vector3) -> int:
    var idx := skeleton.add_bone(name_value)
    if parent >= 0:
        skeleton.set_bone_parent(idx, parent)
    skeleton.set_bone_rest(idx, Transform3D(Basis(), rest_position))
    _bones[name_value] = idx
    return idx

func _build_body() -> void:
    torso = Node3D.new()
    torso.name = "Torso"
    model_root.add_child(torso)
    _mesh_cylinder(torso, 0.43, 0.52, 0.98, Vector3(0, 0.83, 0), body_color, 14, 8)
    _mesh_box(torso, Vector3(0.98, 0.18, 0.62), Vector3(0, 1.20, -0.02), accent_color, 0.05)
    _mesh_box(torso, Vector3(0.62, 0.12, 0.14), Vector3(0, 0.66, -0.50), Color("#eaf8ff"), 0.04)
    _mesh_cylinder(torso, 0.13, 0.17, 0.62, Vector3(-0.23, 0.19, 0), Color("#252d3b"), 10, 6)
    _mesh_cylinder(torso, 0.13, 0.17, 0.62, Vector3(0.23, 0.19, 0), Color("#252d3b"), 10, 6)

    left_leg = Node3D.new()
    left_leg.name = "LeftLeg"
    model_root.add_child(left_leg)
    _mesh_box(left_leg, Vector3(0.30, 0.78, 0.30), Vector3(-0.22, 0.23, 0), Color("#384254"), 0.06)
    _mesh_box(left_leg, Vector3(0.38, 0.16, 0.58), Vector3(-0.22, -0.18, -0.08), Color("#1f2530"), 0.08)

    right_leg = Node3D.new()
    right_leg.name = "RightLeg"
    model_root.add_child(right_leg)
    _mesh_box(right_leg, Vector3(0.30, 0.78, 0.30), Vector3(0.22, 0.23, 0), Color("#384254"), 0.06)
    _mesh_box(right_leg, Vector3(0.38, 0.16, 0.58), Vector3(0.22, -0.18, -0.08), Color("#1f2530"), 0.08)

    left_arm = Node3D.new()
    left_arm.name = "LeftArm"
    model_root.add_child(left_arm)
    _mesh_capsule(left_arm, 0.17, 0.62, Vector3(-0.54, 1.02, 0), body_color)
    _mesh_box(left_arm, Vector3(0.23, 0.28, 0.32), Vector3(-0.58, 0.54, -0.01), accent_color, 0.04)
    left_forearm = Node3D.new()
    left_forearm.name = "LeftForearm"
    model_root.add_child(left_forearm)
    _mesh_capsule(left_forearm, 0.14, 0.48, Vector3(-0.75, 0.70, 0), body_color)

    right_arm = Node3D.new()
    right_arm.name = "RightArm"
    model_root.add_child(right_arm)
    _mesh_capsule(right_arm, 0.17, 0.62, Vector3(0.54, 1.02, 0), body_color)
    _mesh_box(right_arm, Vector3(0.23, 0.28, 0.32), Vector3(0.58, 0.54, -0.01), accent_color, 0.04)
    right_forearm = Node3D.new()
    right_forearm.name = "RightForearm"
    model_root.add_child(right_forearm)
    _mesh_capsule(right_forearm, 0.14, 0.48, Vector3(0.75, 0.70, 0), body_color)

func _build_face() -> void:
    head = Node3D.new()
    head.name = "Head"
    model_root.add_child(head)
    _mesh_sphere(head, 0.56, Vector3(0, 1.65, 0), skin_color, 14, 9)
    _mesh_box(head, Vector3(0.96, 0.14, 0.64), Vector3(0, 2.04, 0.02), accent_color, 0.08)

    face = Node3D.new()
    face.name = "Face"
    model_root.add_child(face)
    _mesh_sphere(face, 0.07, Vector3(-0.20, 1.67, -0.51), Color("#17202c"), 10, 6)
    _mesh_sphere(face, 0.07, Vector3(0.20, 1.67, -0.51), Color("#17202c"), 10, 6)
    _mesh_sphere(face, 0.022, Vector3(-0.18, 1.69, -0.567), Color.WHITE, 8, 5)
    _mesh_sphere(face, 0.022, Vector3(0.22, 1.69, -0.567), Color.WHITE, 8, 5)
    _mesh_box(face, Vector3(0.24, 0.055, 0.045), Vector3(0, 1.49, -0.545), accent_color, 0.02)

func _build_gear() -> void:
    backpack = Node3D.new()
    backpack.name = "Backpack"
    model_root.add_child(backpack)
    _mesh_box(backpack, Vector3(0.58, 0.62, 0.25), Vector3(0, 1.05, 0.38), Color("#252d3b"), 0.07)
    _mesh_box(backpack, Vector3(0.38, 0.19, 0.30), Vector3(0, 1.24, 0.53), body_color, 0.04)

    var shoulder_l := Node3D.new()
    shoulder_l.name = "ShoulderL"
    model_root.add_child(shoulder_l)
    _mesh_sphere(shoulder_l, 0.22, Vector3(-0.55, 1.19, 0), accent_color, 10, 6)
    var shoulder_r := Node3D.new()
    shoulder_r.name = "ShoulderR"
    model_root.add_child(shoulder_r)
    _mesh_sphere(shoulder_r, 0.22, Vector3(0.55, 1.19, 0), accent_color, 10, 6)

    var visor := Node3D.new()
    visor.name = "Visor"
    model_root.add_child(visor)
    var visor_color := Color("#8ff7ff") if style_id in ["crystal_runner", "sky_runner", "stormbreaker"] else accent_color
    _mesh_box(visor, Vector3(0.76, 0.12, 0.40), Vector3(0, 1.86, -0.43), visor_color, 0.05)

func _build_weapon() -> void:
    if weapon_root and is_instance_valid(weapon_root):
        weapon_root.queue_free()
    weapon_root = Node3D.new()
    weapon_root.name = "WeaponRoot"
    model_root.add_child(weapon_root)
    _build_weapon_visual(weapon_id)

func _build_weapon_visual(id: String) -> void:
    weapon_model = Node3D.new()
    weapon_model.name = "Weapon"
    weapon_root.add_child(weapon_model)
    match id:
        "volt_blades", "aether_lance":
            _mesh_box(weapon_model, Vector3(0.10, 1.55, 0.12), Vector3(0.0, 0.78, -0.02), accent_color, 0.03)
            _mesh_box(weapon_model, Vector3(0.46, 0.12, 0.15), Vector3(0, 1.47, -0.02), Color("#e8faff"), 0.03)
            _mesh_box(weapon_model, Vector3(0.18, 0.30, 0.16), Vector3(0, 0.02, -0.02), Color("#202936"), 0.03)
        "toxic_bow":
            _mesh_torus(weapon_model, 0.55, 0.10, Vector3(0, 0.65, 0), Color("#6dea94"))
            _mesh_box(weapon_model, Vector3(0.06, 1.36, 0.06), Vector3(0, 0.68, -0.04), Color("#f4d68d"), 0.02)
        "pulse_cannon":
            _mesh_box(weapon_model, Vector3(0.34, 0.92, 0.30), Vector3(0, 0.62, -0.04), Color("#3a4662"), 0.06)
            _mesh_cylinder(weapon_model, 0.17, 0.22, 0.55, Vector3(0, 1.13, -0.08), accent_color, 10, 6)
            _mesh_sphere(weapon_model, 0.12, Vector3(0, 1.38, -0.08), Color("#e8ffff"), 10, 6)
        "scrap_hammer":
            _mesh_box(weapon_model, Vector3(0.17, 1.35, 0.17), Vector3(0, 0.70, 0), Color("#3d4757"), 0.04)
            _mesh_box(weapon_model, Vector3(0.78, 0.38, 0.45), Vector3(0, 1.38, 0), Color("#ce8056"), 0.08)
            _mesh_box(weapon_model, Vector3(0.30, 0.18, 0.54), Vector3(0, 1.38, 0.18), accent_color, 0.05)
        "orbit_orbs":
            for i in range(3):
                var orb := _mesh_sphere(weapon_model, 0.12, Vector3(-0.27 + i * 0.27, 0.82, -0.12), accent_color, 10, 6)
                orb.set_meta("orb_phase", i * TAU / 3.0)
        _:
            _mesh_box(weapon_model, Vector3(0.16, 1.40, 0.16), Vector3(0, 0.70, 0), accent_color, 0.03)
    weapon_changed.emit(weapon_id)

func equip_weapon(id: String) -> void:
    if weapon_id == id and weapon_model:
        return
    weapon_id = id
    if weapon_root and is_instance_valid(weapon_root):
        weapon_root.queue_free()
    weapon_root = Node3D.new()
    weapon_root.name = "WeaponRoot"
    model_root.add_child(weapon_root)
    _build_weapon_visual(id)

func set_locomotion(value: Vector2) -> void:
    _locomotion = value.limit_length(1.0)

func play_idle() -> void:
    _set_animation("idle")

func play_run() -> void:
    _set_animation("run")

func play_attack() -> void:
    _set_animation("attack")
    _weapon_swing = 0.0

func play_skill(skill_index: int) -> void:
    _set_animation("skill_%d" % skill_index)

func play_dodge() -> void:
    _set_animation("dodge")

func play_hurt() -> void:
    _set_animation("hurt")

func play_death() -> void:
    _set_animation("death")
    if _death_tween and _death_tween.is_valid():
        _death_tween.kill()
    _death_tween = create_tween()
    _death_tween.set_parallel(true)
    _death_tween.tween_property(model_root, "rotation:z", -1.15, 0.32)
    _death_tween.tween_property(model_root, "position:y", 0.06, 0.32)
    _death_tween.tween_property(model_root, "scale", Vector3(0.90,0.90,0.90), 0.32)

func respawn() -> void:
    if model_root == null:
        return
    model_root.rotation = Vector3.ZERO
    model_root.position = Vector3.ZERO
    model_root.scale = Vector3.ONE
    _set_animation("idle")

func _set_animation(value: String) -> void:
    if _animation == value:
        return
    _animation = value
    _animation_time = 0.0
    animation_changed.emit(value)

func _process(delta: float) -> void:
    if model_root == null:
        return
    _time += delta
    _animation_time += delta
    _apply_animation(delta)

func _apply_animation(delta: float) -> void:
    var speed := _locomotion.length()
    var walk_wave := sin(_time * 11.0) * minf(1.0, speed)
    var breathe := sin(_time * 2.5) * 0.025

    model_root.position.y = breathe
    torso.rotation.x = 0.02 * sin(_time * 2.0)
    head.rotation.y = -_locomotion.x * 0.08

    if _animation == "idle":
        left_arm.rotation.x = 0.06 + sin(_time * 2.1) * 0.015
        right_arm.rotation.x = -0.06 - sin(_time * 2.1) * 0.015
        left_leg.rotation.x = 0.0
        right_leg.rotation.x = 0.0
        weapon_root.rotation = Vector3(0, 0, 0)
    elif _animation == "run":
        left_leg.rotation.x = walk_wave * 0.45
        right_leg.rotation.x = -walk_wave * 0.45
        left_arm.rotation.x = -walk_wave * 0.28
        right_arm.rotation.x = walk_wave * 0.28
        torso.position.y = abs(walk_wave) * 0.035
    elif _animation == "attack":
        var t := clampf(_animation_time / 0.34, 0.0, 1.0)
        var swing := sin(t * PI)
        right_arm.rotation.z = -0.25 - swing * 0.85
        right_forearm.rotation.z = -0.20 - swing * 1.10
        left_arm.rotation.z = 0.18 + swing * 0.20
        weapon_root.rotation.y = lerpf(-0.35, 1.35, clampf(t * 1.2,0.0,1.0))
        model_root.position.z = -swing * 0.16
        if _animation_time >= 0.36:
            _set_animation("idle" if speed <= 0.1 else "run")
    elif _animation.begins_with("skill_"):
        var pulse := (sin(_animation_time * 15.0) * 0.5 + 0.5)
        right_arm.rotation.x = -0.35 - pulse * 0.55
        left_arm.rotation.x = -0.25 - pulse * 0.45
        torso.rotation.y = sin(_animation_time * 8.0) * 0.08
        weapon_root.rotation.z = sin(_animation_time * 12.0) * 0.32
        if _animation_time > 0.60:
            _set_animation("idle" if speed <= 0.1 else "run")
    elif _animation == "dodge":
        var d := clampf(_animation_time / 0.28, 0.0, 1.0)
        model_root.rotation.z = lerpf(0.0, -0.35, sin(d * PI))
        model_root.scale = Vector3(1.0 + 0.10 * sin(d * PI), 1.0 - 0.18 * sin(d * PI), 1.0 + 0.10 * sin(d * PI))
        if _animation_time > 0.30:
            model_root.rotation.z = 0.0
            model_root.scale = Vector3.ONE
            _set_animation("idle" if speed <= 0.1 else "run")
    elif _animation == "hurt":
        var h := clampf(_animation_time / 0.16, 0.0, 1.0)
        model_root.position.x = sin(h * PI) * 0.16
        if _animation_time > 0.18:
            _set_animation("idle" if speed <= 0.1 else "run")
    elif _animation == "death":
        if _animation_time > 0.34:
            model_root.position.y = 0.06

    if weapon_id == "orbit_orbs" and weapon_model:
        for child in weapon_model.get_children():
            if child is MeshInstance3D:
                var phase := float(child.get_meta("orb_phase", 0.0))
                var a := _time * 3.5 + phase
                child.position = Vector3(cos(a) * 0.42, 0.82 + sin(a * 1.4) * 0.14, -0.12 + sin(a) * 0.22)

func _capture_base_transforms() -> void:
    _base_transforms.clear()
    for node in [torso,left_arm,right_arm,left_forearm,right_forearm,left_leg,right_leg,head]:
        if node:
            _base_transforms[node.name] = node.transform

func _mesh_box(parent: Node3D, size: Vector3, pos: Vector3, color: Color, bevel: float) -> MeshInstance3D:
    var n := MeshInstance3D.new()
    var mesh := BoxMesh.new()
    mesh.size = size
    mesh.material = _material(color)
    n.mesh = mesh
    n.position = pos
    parent.add_child(n)
    return n

func _mesh_sphere(parent: Node3D, radius: float, pos: Vector3, color: Color, radial := 12, rings := 8) -> MeshInstance3D:
    var n := MeshInstance3D.new()
    var mesh := SphereMesh.new()
    mesh.radius = radius
    mesh.height = radius * 2.0
    mesh.radial_segments = radial
    mesh.rings = rings
    mesh.material = _material(color)
    n.mesh = mesh
    n.position = pos
    parent.add_child(n)
    return n

func _mesh_cylinder(parent: Node3D, top: float, bottom: float, height: float, pos: Vector3, color: Color, radial := 12, rings := 6) -> MeshInstance3D:
    var n := MeshInstance3D.new()
    var mesh := CylinderMesh.new()
    mesh.top_radius = top
    mesh.bottom_radius = bottom
    mesh.height = height
    mesh.radial_segments = radial
    mesh.material = _material(color)
    n.mesh = mesh
    n.position = pos
    parent.add_child(n)
    return n

func _mesh_capsule(parent: Node3D, radius: float, height: float, pos: Vector3, color: Color) -> MeshInstance3D:
    var n := MeshInstance3D.new()
    var mesh := CapsuleMesh.new()
    mesh.radius = radius
    mesh.height = height
    mesh.radial_segments = 10
    mesh.rings = 4
    mesh.material = _material(color)
    n.mesh = mesh
    n.position = pos
    parent.add_child(n)
    return n

func _mesh_torus(parent: Node3D, inner: float, outer: float, pos: Vector3, color: Color) -> MeshInstance3D:
    var n := MeshInstance3D.new()
    var mesh := TorusMesh.new()
    mesh.inner_radius = inner
    mesh.outer_radius = outer
    mesh.rings = 18
    mesh.ring_segments = 9
    mesh.material = _material(color, true)
    n.mesh = mesh
    n.position = pos
    n.rotation.x = PI * 0.5
    parent.add_child(n)
    return n

func _material(color: Color, emissive := false) -> Material:
    var shader: Shader = load("res://shaders/aether_toon.gdshader")
    if shader:
        var sm := ShaderMaterial.new()
        sm.shader = shader
        sm.set_shader_parameter("base_color", color)
        sm.set_shader_parameter("rim_color", color.lightened(0.25))
        sm.set_shader_parameter("rim_strength", 0.22)
        sm.set_shader_parameter("emission_color", color if emissive else Color.TRANSPARENT)
        sm.set_shader_parameter("emission_strength", 1.4 if emissive else 0.0)
        return sm
    var m := StandardMaterial3D.new()
    m.albedo_color = color
    m.roughness = 0.68
    if emissive:
        m.emission_enabled = true
        m.emission = color
        m.emission_energy_multiplier = 1.4
    return m
