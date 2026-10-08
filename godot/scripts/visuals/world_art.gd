extends RefCounted
## Procedural production environment art.
## All art is original and generated from low-poly primitives for deterministic
## offline builds. The style favors readable silhouettes and controlled draw calls.

const Factory = preload("res://scripts/visuals/stylized_factory.gd")

static func build_hq(parent: Node3D) -> void:
    _make_rooftop_floor(parent)
    _make_hq_core(parent)
    _make_hq_building(parent, Vector3(-12,0,-7), Vector3(8,4,5), Color("#374762"))
    _make_hq_building(parent, Vector3(12,0,-7), Vector3(8,4,5), Color("#374762"))
    _make_hq_building(parent, Vector3(-13,0,8), Vector3(7,5,4), Color("#303c58"))
    _make_hq_building(parent, Vector3(13,0,8), Vector3(7,5,4), Color("#303c58"))
    _make_crystal_lamps(parent)
    _make_rooftop_plants(parent)
    _make_hq_rails(parent)
    _make_air_units(parent)

static func decorate_biome(parent: Node3D, id: String, biome: Dictionary) -> void:
    var accent: Color = biome.get("accent", Color("#8bd8ff"))
    var ground: Color = biome.get("ground", Color("#4ea66f"))
    match id:
        "verdant_frontier":
            _forest_grid(parent, accent)
            _make_stream(parent, Vector3(0,0,8), 42.0, Color("#5fbce9"))
            _make_ruins(parent, Vector3(-15,0,-15), accent)
            _make_ruins(parent, Vector3(16,0,12), accent.darkened(0.18))
        "crystal_forest":
            _crystal_grid(parent, accent)
            _make_crystal_cave(parent, Vector3(-12,0,-12), accent)
            _make_crystal_cave(parent, Vector3(14,0,13), accent.lightened(0.12))
        "sunset_desert":
            _desert_grid(parent, accent)
            _make_obelisk(parent, Vector3(0,0,-14), accent)
            _make_obelisk(parent, Vector3(16,0,9), accent.darkened(0.15))
            _make_dune(parent, Vector3(-8,0,8), ground.lightened(0.12))
        "rust_canyons":
            _scrap_grid(parent, accent)
            _make_scrap_tower(parent, Vector3(-14,0,-12), accent)
            _make_scrap_tower(parent, Vector3(14,0,12), accent.darkened(0.20))
        "sky_ruins":
            _sky_grid(parent, accent)
            _make_floating_island(parent, Vector3(-12,4,-10), accent)
            _make_floating_island(parent, Vector3(13,5,12), accent.darkened(0.18))
        _:
            _forest_grid(parent, accent)

static func _make_rooftop_floor(parent: Node3D) -> void:
    Factory.box(parent, Vector3(58,0.48,58), Vector3(0,-0.24,0), Color("#2a3448"))
    for x in range(-5,6):
        Factory.box(parent, Vector3(0.06,0.02,52), Vector3(float(x*5),0.015,0), Color("#3a4762"))
    for z in range(-5,6):
        Factory.box(parent, Vector3(52,0.02,0.06), Vector3(0,0.017,float(z*5)), Color("#3a4762"))

static func _make_hq_core(parent: Node3D) -> void:
    var portal := Factory.make_portal(parent, Vector3(0,0,0), Color("#5bdcff"))
    portal.name = "Portal"
    for p in [Vector3(-18,0,-15),Vector3(18,0,-15),Vector3(-18,0,15),Vector3(18,0,15)]:
        var holder := Node3D.new()
        holder.position = p
        parent.add_child(holder)
        Factory.box(holder, Vector3(1.5,2.3,1.5), Vector3(0,1.15,0), Color("#596885"))
        Factory.sphere(holder, 0.58, Vector3(0,2.48,0), Color("#7ceeff"))
        var light := OmniLight3D.new()
        light.light_color = Color("#7ceeff")
        light.light_energy = 1.2
        light.omni_range = 4.5
        light.position = Vector3(0,2.35,0)
        holder.add_child(light)

static func _make_hq_building(parent: Node3D, pos: Vector3, size: Vector3, color: Color) -> void:
    var root := Node3D.new()
    root.position = pos
    parent.add_child(root)
    Factory.box(root, size, Vector3(0,size.y*0.5,0), color)
    Factory.box(root, Vector3(size.x*0.86,0.18,size.z*0.86), Vector3(0,size.y+0.10,0), color.lightened(0.11))
    for x in [-0.30,0.30]:
        Factory.box(root, Vector3(size.x*0.16,1.2,0.08), Vector3(size.x*x, size.y*0.58,-size.z*0.51), Color("#8be8ff"))
        Factory.box(root, Vector3(0.08,1.2,size.z*0.16), Vector3(size.x*0.51, size.y*0.58,size.z*x), Color("#78cde9"))
    for x in [-0.38,0.0,0.38]:
        Factory.box(root, Vector3(0.15,0.55,0.15), Vector3(size.x*x, size.y+0.36,0), Color("#273246"))

static func _make_crystal_lamps(parent: Node3D) -> void:
    for i in range(10):
        var a := TAU * float(i) / 10.0
        var root := Node3D.new()
        root.position = Vector3(cos(a)*20.0,0,sin(a)*20.0)
        parent.add_child(root)
        Factory.cylinder(root,0.08,0.12,1.45,Vector3(0,0.72,0),Color("#2b3549"))
        Factory.make_crystal(root,Vector3(0,1.55,0),Color("#68ecff"))

static func _make_rooftop_plants(parent: Node3D) -> void:
    for i in range(20):
        var a := float(i) * 2.399
        var r := 9.0 + float((i*7)%8)
        var root := Node3D.new()
        root.position = Vector3(cos(a)*r,0,sin(a)*r)
        parent.add_child(root)
        Factory.cylinder(root,0.08,0.13,0.52,Vector3(0,0.26,0),Color("#6e4939"))
        for j in range(3):
            var leaf := Factory.sphere(root,0.25,Vector3(cos(j*2.1)*0.18,0.68+sin(j)*0.04,sin(j*2.1)*0.18),Color("#56bb83"))
            leaf.scale = Vector3(1.15,0.45,0.65)

static func _make_hq_rails(parent: Node3D) -> void:
    for p in [Vector3(-25,0,0),Vector3(25,0,0),Vector3(0,0,-25),Vector3(0,0,25)]:
        var rot := 0.0 if abs(p.x) > 0 else PI*0.5
        for i in range(-10,11):
            var post := Node3D.new()
            post.position = Vector3(p.x + (0 if abs(p.x)>0 else i*2.3),0.0,p.z + (i*2.3 if abs(p.x)>0 else 0))
            parent.add_child(post)
            Factory.box(post,Vector3(0.10,1.05,0.10),Vector3(0,0.52,0),Color("#5c6980"))
        Factory.box(parent,Vector3(52,0.10,0.10) if abs(p.x)<0.1 else Vector3(0.10,0.10,52),Vector3(p.x,1.04,p.z),Color("#74839c"))

static func _make_air_units(parent: Node3D) -> void:
    for i in range(5):
        var root := Node3D.new()
        root.position = Vector3(-16 + i*8, 3.2 + (i%2)*0.4, -18)
        parent.add_child(root)
        Factory.box(root,Vector3(1.1,0.18,1.1),Vector3.ZERO,Color("#273246"))
        Factory.torus(root,0.34,0.06,Vector3(0,0.12,0),Color("#67e8ff"))

static func _forest_grid(parent: Node3D, accent: Color) -> void:
    for x in range(-6,7):
        for z in range(-6,7):
            if (x*7+z*5)%3==0 and abs(x)+abs(z)>1:
                _make_tree(parent,Vector3(x*4.0,0,z*4.0),accent)

static func _crystal_grid(parent: Node3D, accent: Color) -> void:
    for x in range(-6,7):
        for z in range(-6,7):
            if (x*5+z*3)%4==0:
                var p := Vector3(x*4.0,0,z*4.0)
                Factory.make_crystal(parent,p+Vector3(0,1.0,0),accent)
                if (x+z)%2==0:
                    Factory.make_crystal(parent,p+Vector3(0.65,0.55,0.45),accent.lightened(0.18))

static func _desert_grid(parent: Node3D, accent: Color) -> void:
    for x in range(-6,7):
        for z in range(-6,7):
            if (x*2+z*5)%5==0:
                var p := Vector3(x*4.4,0,z*4.4)
                _make_cactus(parent,p,accent)

static func _scrap_grid(parent: Node3D, accent: Color) -> void:
    for x in range(-6,7):
        for z in range(-6,7):
            if (x*4-z*3)%4==0:
                _make_scrap_cluster(parent,Vector3(x*4.2,0,z*4.2),accent)

static func _sky_grid(parent: Node3D, accent: Color) -> void:
    for i in range(18):
        var p := Vector3(((i*17)%43)-21,0,((i*29)%43)-21)
        Factory.box(parent,Vector3(1.4,0.55,1.4),p+Vector3(0,0.05,0),Color("#6e80a8"))
        Factory.make_crystal(parent,p+Vector3(0,0.7,0),accent)

static func _make_tree(parent: Node3D, pos: Vector3, accent: Color) -> void:
    var root := Node3D.new()
    root.position = pos
    parent.add_child(root)
    Factory.cylinder(root,0.24,0.38,1.9,Vector3(0,0.95,0),Color("#5f4539"),10,5)
    var crown := Factory.sphere(root,1.02,Vector3(0,2.05,0),accent.lightened(0.04))
    crown.scale = Vector3(1.0,1.12,0.95)
    Factory.sphere(root,0.62,Vector3(-0.66,1.86,0.10),accent)
    Factory.sphere(root,0.62,Vector3(0.62,1.88,-0.08),accent.darkened(0.06))

static func _make_stream(parent: Node3D, pos: Vector3, length: float, color: Color) -> void:
    var root := Node3D.new()
    root.position = pos
    parent.add_child(root)
    var mesh := BoxMesh.new()
    mesh.size = Vector3(5.0,0.04,length)
    var n := MeshInstance3D.new()
    n.mesh = mesh
    n.material_override = Factory.mat(color,0.05,0.25,color)
    root.add_child(n)
    for i in range(8):
        var ripple := Factory.torus(root,0.16,0.05,Vector3(sin(i)*1.3,0.05,-length*0.42+i*length/7.0),Color("#c0f6ff"))
        ripple.scale = Vector3(1.8,0.6,1.0)

static func _make_ruins(parent: Node3D, pos: Vector3, accent: Color) -> void:
    var root := Node3D.new()
    root.position = pos
    parent.add_child(root)
    Factory.box(root,Vector3(3.4,2.4,0.8),Vector3(0,1.2,0),Color("#5b6870"))
    Factory.box(root,Vector3(0.7,3.4,0.8),Vector3(-1.35,1.7,0),Color("#66757b"))
    Factory.box(root,Vector3(0.7,2.2,0.8),Vector3(1.35,1.1,0),Color("#66757b"))
    Factory.make_crystal(root,Vector3(0,2.8,0.1),accent)

static func _crystal_cave(parent: Node3D, pos: Vector3, accent: Color) -> void:
    var root := Node3D.new()
    root.position = pos
    parent.add_child(root)
    for i in range(9):
        var a := TAU*float(i)/9.0
        Factory.make_crystal(root,Vector3(cos(a)*1.5,0.6+abs(sin(a))*1.1,sin(a)*1.5),accent)

static func _make_obelisk(parent: Node3D, pos: Vector3, accent: Color) -> void:
    var root := Node3D.new()
    root.position = pos
    parent.add_child(root)
    Factory.box(root,Vector3(1.4,4.8,1.4),Vector3(0,2.4,0),Color("#8b6a55"))
    Factory.make_crystal(root,Vector3(0,5.0,0),accent)
    Factory.make_portal(root,Vector3(0,0,0),accent.darkened(0.30)).scale = Vector3(0.55,0.55,0.55)

static func _make_dune(parent: Node3D, pos: Vector3, color: Color) -> void:
    var root := Node3D.new()
    root.position = pos
    parent.add_child(root)
    var m := SphereMesh.new()
    m.radius = 4.5
    m.height = 3.0
    m.material = Factory.mat(color)
    var n := MeshInstance3D.new()
    n.mesh=m
    n.scale=Vector3(1.5,0.42,0.9)
    root.add_child(n)

static func _make_scrap_cluster(parent: Node3D, pos: Vector3, accent: Color) -> void:
    var root := Node3D.new()
    root.position=pos
    parent.add_child(root)
    for i in range(4):
        Factory.box(root,Vector3(0.7+0.2*i,0.5,0.55),Vector3(i*0.42,0.25,sin(i)*0.5),Color("#59616e"))
    Factory.make_crystal(root,Vector3(0.45,0.8,0),accent)

static func _make_scrap_tower(parent: Node3D, pos: Vector3, accent: Color) -> void:
    var root:=Node3D.new()
    root.position=pos
    parent.add_child(root)
    for i in range(5):
        Factory.box(root,Vector3(1.4,0.8,1.0),Vector3(sin(i)*0.3,i*0.78,cos(i)*0.3),Color("#5a4f4a"))
    Factory.make_crystal(root,Vector3(0,4.1,0),accent)

static func _make_floating_island(parent: Node3D, pos: Vector3, accent: Color) -> void:
    var root:=Node3D.new()
    root.position=pos
    parent.add_child(root)
    Factory.box(root,Vector3(4.2,0.8,4.2),Vector3(0,0,0),Color("#5b698f"))
    Factory.cylinder(root,0.25,1.4,2.0,Vector3(0,-1.25,0),Color("#3b4867"))
    Factory.make_crystal(root,Vector3(0,0.7,0),accent)

static func _make_cactus(parent: Node3D, pos: Vector3, accent: Color) -> void:
    var root:=Node3D.new()
    root.position=pos
    parent.add_child(root)
    Factory.cylinder(root,0.20,0.26,1.55,Vector3(0,0.78,0),Color("#6a995b"))
    Factory.cylinder(root,0.11,0.15,0.62,Vector3(-0.42,0.72,0),Color("#6a995b")).rotation.z=-0.8
    Factory.cylinder(root,0.11,0.15,0.62,Vector3(0.42,0.48,0),Color("#6a995b")).rotation.z=0.8
    Factory.sphere(root,0.14,Vector3(0,1.62,0),accent)

static func _make_crystal(parent: Node3D, pos: Vector3, color: Color) -> Node3D:
    return Factory.make_crystal(parent,pos,color)

