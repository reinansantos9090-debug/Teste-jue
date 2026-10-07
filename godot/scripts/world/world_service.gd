extends Node
const Factory = preload("res://scripts/visuals/stylized_factory.gd")
var root: Node3D
var database: Node
var portal: Node3D
var sun: DirectionalLight3D

func setup(parent: Node3D, content: Node) -> void:
    root = parent
    database = content

func build_hq() -> void:
    _clear_world()
    _make_environment(Color("#08111e"))
    Factory.box(root, Vector3(54,0.5,54), Vector3(0,-0.25,0), Color("#2b3449"))
    for p in [Vector3(-16,0,-12),Vector3(16,0,-12),Vector3(-16,0,12),Vector3(16,0,12)]:
        var holder := Node3D.new()
        holder.position = p
        root.add_child(holder)
        Factory.box(holder,Vector3(1.3,2.1,1.3),Vector3(0,1.05,0),Color("#596885"))
        Factory.sphere(holder,0.65,Vector3(0,2.25,0),Color("#70d9dd"))
    portal = Factory.make_portal(root, Vector3(0,0,0), Color("#5bdcff"))
    Factory.box(root,Vector3(8,0.4,5),Vector3(-11,0.05,-5),Color("#394762"))
    Factory.box(root,Vector3(8,0.4,5),Vector3(11,0.05,-5),Color("#394762"))

func build_biome(id: String) -> void:
    _clear_world()
    var b: Dictionary = database.BIOMES.get(id, database.BIOMES["verdant_frontier"])
    _make_environment(b["fog"])
    Factory.box(root,Vector3(58,0.5,58),Vector3(0,-0.25,0),b["ground"])
    for x in range(-6,7):
        for z in range(-6,7):
            if (x*3+z) % 4 == 0:
                var prop := Node3D.new()
                prop.position = Vector3(float(x*4),0,float(z*4))
                root.add_child(prop)
                Factory.cylinder(prop,0.30,0.46,2.1,Vector3(0,1.05,0),Color("#634938"))
                Factory.sphere(prop,0.78,Vector3(0,2.18,0),b["accent"])
    portal = Factory.make_portal(root,Vector3(0,0,0),b["accent"])

func _clear_world() -> void:
    for child in root.get_children():
        if child.name.begins_with("World_") or child.name == "Sun":
            child.queue_free()

func _make_environment(background: Color) -> void:
    var env_node := WorldEnvironment.new()
    env_node.name = "World_Environment"
    var env := Environment.new()
    env.background_mode = Environment.BG_COLOR
    env.background_color = background
    env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    env.ambient_light_color = Color("#d8ebff")
    env.ambient_light_energy = 0.85
    env.tonemap_mode = Environment.TONE_MAPPER_FILMIC
    env_node.environment = env
    root.add_child(env_node)
    sun = DirectionalLight3D.new()
    sun.name = "Sun"
    sun.rotation_degrees = Vector3(-54,-30,0)
    sun.light_energy = 1.2
    sun.shadow_enabled = true
    root.add_child(sun)
