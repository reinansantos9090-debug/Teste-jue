extends Node
## Runtime world builder. Map rules are supplied by map_service.gd; visual
## primitives stay lightweight so authored layouts remain viable on mobile.

const Factory = preload("res://scripts/visuals/stylized_factory.gd")

var root: Node3D
var database: Node
var map_db: Node
var world_root: Node3D
var portal: Node3D
var sun: DirectionalLight3D

func setup(parent: Node3D, content: Node, maps: Node = null) -> void:
    root = parent
    database = content
    map_db = maps
    world_root = Node3D.new()
    world_root.name = "WorldRoot"
    root.add_child(world_root)

func build_hq() -> void:
    _reset()
    _make_environment(Color("#08111e"), Color("#d8ebff"))
    Factory.box(world_root,Vector3(54,0.5,54),Vector3(0,-0.25,0),Color("#2b3449"))
    for p in [Vector3(-16,0,-12),Vector3(16,0,-12),Vector3(-16,0,12),Vector3(16,0,12)]:
        var holder := Node3D.new()
        holder.position=p
        world_root.add_child(holder)
        Factory.box(holder,Vector3(1.3,2.1,1.3),Vector3(0,1.05,0),Color("#596885"))
        Factory.sphere(holder,0.65,Vector3(0,2.25,0),Color("#70d9dd"))
    portal=Factory.make_portal(world_root,Vector3(0,0,0),Color("#5bdcff"))
    Factory.box(world_root,Vector3(8,0.4,5),Vector3(-11,0.05,-5),Color("#394762"))
    Factory.box(world_root,Vector3(8,0.4,5),Vector3(11,0.05,-5),Color("#394762"))
    Factory.box(world_root,Vector3(3.5,0.24,3.5),Vector3(0,0.12,8),Color("#32425c"))
    _make_hq_details()

func build_biome(id: String) -> void:
    _reset()
    var b: Dictionary = map_db.get_map(id) if map_db else database.BIOMES.get(id,database.BIOMES["verdant_frontier"])
    _make_environment(b.get("fog",Color("#8fb1a5")),b.get("accent",Color("#d8ebff")))
    Factory.box(world_root,Vector3(58,0.5,58),Vector3(0,-0.25,0),b.get("ground",Color("#4ea66f")))
    if map_db:
        for obstacle in b.get("obstacles",[]):
            _make_obstacle(obstacle,b.get("accent",Color.WHITE))
    _make_biome_decor(id,b)
    portal=Factory.make_portal(world_root,Vector3(0,0,0),b.get("accent",Color("#5bdcff")))
    _make_hazard_markers(id,b.get("accent",Color.WHITE))

func _make_biome_decor(id:String,b:Dictionary)->void:
    var accent:Color=b.get("accent",Color.WHITE)
    var ground:Color=b.get("ground",Color("#4ea66f"))
    match id:
        "verdant_frontier":
            for p in [Vector3(-18,0,-18),Vector3(18,0,-17),Vector3(-19,0,17),Vector3(17,0,18)]:
                _tree(p,accent,ground)
        "crystal_forest":
            for p in [Vector3(-18,0,0),Vector3(18,0,0),Vector3(-16,0,17),Vector3(17,0,-16)]:
                Factory.make_crystal(world_root,p+Vector3(0,0,0),accent)
        "sunset_desert":
            for p in [Vector3(-18,0,-15),Vector3(18,0,-14),Vector3(-17,0,15),Vector3(18,0,16)]:
                _cactus(p,accent)
        "rust_canyons":
            for p in [Vector3(-18,0,-16),Vector3(17,0,-14),Vector3(-17,0,16),Vector3(16,0,17)]:
                _scrap_stack(p,accent)
        "sky_ruins":
            for p in [Vector3(-18,0,-15),Vector3(17,0,-15),Vector3(-17,0,15),Vector3(18,0,15)]:
                _ruin_pillar(p,accent)

func _make_obstacle(obstacle:Dictionary,accent:Color)->void:
    var kind:=str(obstacle.get("shape","rock"))
    var pos:Vector3=obstacle.get("pos",Vector3.ZERO)
    var size:Vector3=obstacle.get("size",Vector3.ONE)
    match kind:
        "tree":
            _tree(pos,accent,Color("#5a4939"),size.y)
        "crystal":
            Factory.box(world_root,Vector3(size.x,0.10,size.z),pos+Vector3(0,0.05,0),Color("#465f86"))
            Factory.make_crystal(world_root,pos+Vector3(0,size.y*0.45,0),accent)
        "cactus":
            _cactus(pos,accent,maxf(1.0,size.y/1.9))
        "scrap":
            _scrap_stack(pos,accent,size)
        "ruin":
            _ruin_pillar(pos,accent,size)
        _:
            Factory.box(world_root,Vector3(size.x,0.35,size.z),pos+Vector3(0,0.17,0),Color("#4b5567"))
            var rock:=Factory.sphere(world_root,maxf(0.45,minf(size.x,size.z)*0.42),pos+Vector3(0,size.y*0.45,0),accent.darkened(0.45))
            rock.scale=Vector3(1.0,size.y/maxf(0.8,minf(size.x,size.z)),1.0)

func _tree(pos:Vector3,accent:Color,ground:Color,height:float=2.8)->void:
    var root_node:=Node3D.new()
    root_node.position=pos
    world_root.add_child(root_node)
    Factory.cylinder(root_node,0.24,0.34,height*0.52,Vector3(0,height*0.26,0),Color("#5f4539"))
    var crown:=Factory.sphere(root_node,height*0.30,Vector3(0,height*0.72,0),accent)
    crown.scale=Vector3(1.15,1.0,1.0)
    Factory.sphere(root_node,height*0.18,Vector3(-height*0.25,height*0.68,0.15),accent.lightened(0.08))
    Factory.sphere(root_node,height*0.18,Vector3(height*0.22,height*0.67,-0.12),accent.darkened(0.05))
    Factory.box(root_node,Vector3(0.38,0.16,0.38),Vector3(0,0.05,0),ground)

func _cactus(pos:Vector3,accent:Color,scale_factor:float=1.0)->void:
    var root_node:=Node3D.new()
    root_node.position=pos
    world_root.add_child(root_node)
    Factory.cylinder(root_node,0.20,0.28,1.7*scale_factor,Vector3(0,0.85*scale_factor,0),Color("#6a995b"))
    Factory.cylinder(root_node,0.11,0.15,0.62*scale_factor,Vector3(-0.42*scale_factor,0.72*scale_factor,0),Color("#6a995b")).rotation.z=-0.8
    Factory.cylinder(root_node,0.11,0.15,0.62*scale_factor,Vector3(0.42*scale_factor,0.48*scale_factor,0),Color("#6a995b")).rotation.z=0.8
    Factory.sphere(root_node,0.14*scale_factor,Vector3(0,1.72*scale_factor,0),accent)

func _scrap_stack(pos:Vector3,accent:Color,size:Vector3=Vector3(2.0,2.0,2.0))->void:
    var root_node:=Node3D.new()
    root_node.position=pos
    world_root.add_child(root_node)
    Factory.box(root_node,Vector3(size.x*0.82,size.y*0.52,size.z*0.72),Vector3(0,size.y*0.26,0),Color("#4f5966"))
    Factory.box(root_node,Vector3(size.x*0.46,size.y*0.34,size.z*0.44),Vector3(-size.x*0.20,size.y*0.68,0),accent.darkened(0.18))
    Factory.box(root_node,Vector3(size.x*0.34,size.y*0.24,size.z*0.55),Vector3(size.x*0.21,size.y*0.62,-size.z*0.10),accent)
    Factory.sphere(root_node,0.14,Vector3(0,size.y*0.92,0),accent)

func _ruin_pillar(pos:Vector3,accent:Color,size:Vector3=Vector3(2.0,3.0,2.0))->void:
    var root_node:=Node3D.new()
    root_node.position=pos
    world_root.add_child(root_node)
    Factory.box(root_node,Vector3(size.x*0.68,size.y,size.z*0.68),Vector3(0,size.y*0.5,0),Color("#5d6782"))
    Factory.box(root_node,Vector3(size.x*0.86,0.20,size.z*0.86),Vector3(0,size.y+0.10,0),accent)
    Factory.box(root_node,Vector3(size.x*0.36,0.20,size.z*0.40),Vector3(0,size.y*0.5,-size.z*0.40),accent.lightened(0.12))

func _make_hazard_markers(id:String,accent:Color)->void:
    var marker_color:=accent.darkened(0.20)
    for p in [Vector3(-4,0.025,-4),Vector3(4,0.025,-4),Vector3(-4,0.025,4),Vector3(4,0.025,4)]:
        var ring:=Factory.torus(world_root,0.42,0.06,p,marker_color)
        ring.rotation.x=PI*0.5
    var label:=Label3D.new()
    label.text=str(map_db.hazard(id)) if map_db else ""
    label.position=Vector3(0,0.15,2.9)
    label.modulate=accent
    label.font_size=18
    label.outline_size=4
    world_root.add_child(label)

func _make_hq_details()->void:
    for p in [Vector3(-22,0,-20),Vector3(22,0,-20),Vector3(-22,0,20),Vector3(22,0,20)]:
        var root_node:=Node3D.new()
        root_node.position=p
        world_root.add_child(root_node)
        Factory.cylinder(root_node,0.12,0.16,1.3,Vector3(0,0.65,0),Color("#3f4b61"))
        Factory.make_crystal(root_node,Vector3(0,1.4,0),Color("#7defff"))

func _reset()->void:
    if world_root and is_instance_valid(world_root):
        world_root.free()
    world_root=Node3D.new()
    world_root.name="WorldRoot"
    root.add_child(world_root)
    if sun and is_instance_valid(sun):
        sun.free()
    var old_env:=root.get_node_or_null("World_Environment")
    if old_env:old_env.free()

func _make_environment(background:Color,ambient:Color)->void:
    var node:=WorldEnvironment.new()
    node.name="World_Environment"
    var env:=Environment.new()
    env.background_mode=Environment.BG_COLOR
    env.background_color=background
    env.ambient_light_source=Environment.AMBIENT_SOURCE_COLOR
    env.ambient_light_color=ambient
    env.ambient_light_energy=0.88
    env.tonemap_mode=Environment.TONE_MAPPER_FILMIC
    env.fog_enabled=true
    env.fog_light_color=background.lightened(0.10)
    env.fog_light_energy=0.55
    env.fog_density=0.008
    node.environment=env
    root.add_child(node)
    sun=DirectionalLight3D.new()
    sun.name="Sun"
    sun.rotation_degrees=Vector3(-54,-30,0)
    sun.light_energy=1.2
    sun.shadow_enabled=true
    root.add_child(sun)
