extends RefCounted
## Procedural original 3D asset factory.
## Rounded meshes + simple materials keep draw calls and geometry modest for mobile.

static func mat(color: Color, metallic := 0.0, roughness := 0.72, emission := Color.TRANSPARENT) -> StandardMaterial3D:
    var m := StandardMaterial3D.new()
    m.albedo_color = color
    m.metallic = metallic
    m.roughness = roughness
    if emission.a > 0.0:
        m.emission_enabled = true
        m.emission = emission
        m.emission_energy_multiplier = 1.8
    return m

static func box(parent: Node3D, size: Vector3, pos: Vector3, color: Color) -> MeshInstance3D:
    var n := MeshInstance3D.new()
    var mesh := BoxMesh.new()
    mesh.size = size
    n.mesh = mesh
    n.position = pos
    n.material_override = mat(color)
    parent.add_child(n)
    return n

static func sphere(parent: Node3D, radius: float, pos: Vector3, color: Color) -> MeshInstance3D:
    var n := MeshInstance3D.new()
    var mesh := SphereMesh.new()
    mesh.radius = radius
    mesh.height = radius * 2.0
    mesh.radial_segments = 12
    mesh.rings = 7
    n.mesh = mesh
    n.position = pos
    n.material_override = mat(color)
    parent.add_child(n)
    return n

static func cylinder(parent: Node3D, top: float, bottom: float, height: float, pos: Vector3, color: Color) -> MeshInstance3D:
    var n := MeshInstance3D.new()
    var mesh := CylinderMesh.new()
    mesh.top_radius = top
    mesh.bottom_radius = bottom
    mesh.height = height
    mesh.radial_segments = 12
    n.mesh = mesh
    n.position = pos
    n.material_override = mat(color)
    parent.add_child(n)
    return n

static func torus(parent: Node3D, inner: float, outer: float, pos: Vector3, color: Color) -> MeshInstance3D:
    var n := MeshInstance3D.new()
    var mesh := TorusMesh.new()
    mesh.inner_radius = inner
    mesh.outer_radius = outer
    mesh.rings = 20
    mesh.ring_segments = 10
    n.mesh = mesh
    n.position = pos
    n.rotation.x = PI * 0.5
    n.material_override = mat(color, 0.15, 0.35, color)
    parent.add_child(n)
    return n

static func make_hunter(parent: Node3D, body_color: Color, accent: Color, skin: Color) -> Node3D:
    var root := Node3D.new()
    parent.add_child(root)
    cylinder(root, 0.40, 0.48, 0.98, Vector3(0, 0.82, 0), body_color)
    sphere(root, 0.55, Vector3(0, 1.62, 0), skin)
    box(root, Vector3(1.05, 0.18, 0.58), Vector3(0, 2.05, 0), accent)
    sphere(root, 0.065, Vector3(-0.20, 1.66, -0.49), Color("#18212c"))
    sphere(root, 0.065, Vector3(0.20, 1.66, -0.49), Color("#18212c"))
    cylinder(root, 0.13, 0.16, 0.58, Vector3(-0.22,0.22,0), Color("#252b36"))
    cylinder(root, 0.13, 0.16, 0.58, Vector3(0.22,0.22,0), Color("#252b36"))
    return root

static func make_slime(parent: Node3D, color: Color) -> Node3D:
    var root := Node3D.new()
    parent.add_child(root)
    var body := sphere(root, 0.76, Vector3(0,0.72,0), color)
    body.scale = Vector3(1.1,0.72,0.95)
    sphere(root,0.10,Vector3(-0.22,0.82,-0.58),Color.WHITE)
    sphere(root,0.10,Vector3(0.22,0.82,-0.58),Color.WHITE)
    sphere(root,0.04,Vector3(-0.22,0.82,-0.63),Color("#202833"))
    sphere(root,0.04,Vector3(0.22,0.82,-0.63),Color("#202833"))
    return root

static func make_crystal_monster(parent: Node3D, color: Color) -> Node3D:
    var root := Node3D.new()
    parent.add_child(root)
    box(root,Vector3(1.1,1.55,0.95),Vector3(0,0.92,0),color)
    sphere(root,0.20,Vector3(-0.28,1.42,-0.50),Color("#efffff"))
    sphere(root,0.20,Vector3(0.28,1.42,-0.50),Color("#efffff"))
    return root

static func make_golem(parent: Node3D, color: Color, accent := Color("#f8df9b")) -> Node3D:
    var root := Node3D.new()
    parent.add_child(root)
    box(root,Vector3(1.42,1.72,1.16),Vector3(0,1.0,0),color)
    sphere(root,0.18,Vector3(-0.34,1.54,-0.55),accent)
    sphere(root,0.18,Vector3(0.34,1.54,-0.55),accent)
    box(root,Vector3(0.44,0.74,0.52),Vector3(-0.95,0.98,0),color)
    box(root,Vector3(0.44,0.74,0.52),Vector3(0.95,0.98,0),color)
    cylinder(root,0.19,0.24,0.62,Vector3(-0.42,0.18,0),Color("#2a3039"))
    cylinder(root,0.19,0.24,0.62,Vector3(0.42,0.18,0),Color("#2a3039"))
    return root

static func make_portal(parent: Node3D, pos: Vector3, accent: Color) -> Node3D:
    var root := Node3D.new()
    root.position = pos
    parent.add_child(root)
    torus(root,1.9,2.28,Vector3(0,2.25,0),accent)
    torus(root,1.38,1.52,Vector3(0,2.25,0),Color("#d9fbff"))
    cylinder(root,1.25,1.35,0.25,Vector3(0,0.28,0),Color("#273145"))
    var light := OmniLight3D.new()
    light.light_color = accent
    light.light_energy = 2.0
    light.omni_range = 6.0
    light.position = Vector3(0,2.0,0)
    root.add_child(light)
    return root

static func make_crystal(parent: Node3D, pos: Vector3, color: Color) -> Node3D:
    var root := Node3D.new()
    root.name = "Crystal"
    root.position = pos
    parent.add_child(root)
    var mesh_instance := MeshInstance3D.new()
    var mesh := PrismMesh.new()
    mesh.size = Vector3(0.70,1.60,0.70)
    mesh.material = mat(color,0.08,0.25,color)
    mesh_instance.mesh = mesh
    mesh_instance.rotation_degrees = Vector3(0,17,0)
    root.add_child(mesh_instance)
    sphere(root,0.12,Vector3(0,0.86,0),Color("#f4feff"))
    var light := OmniLight3D.new()
    light.light_color = color
    light.light_energy = 0.55
    light.omni_range = 2.4
    light.position = Vector3(0,0.75,0)
    root.add_child(light)
    return root

static func make_weapon_icon(parent: Node3D, kind: String, color: Color) -> Node3D:
    var root := Node3D.new()
    parent.add_child(root)
    if kind == "hammer":
        box(root,Vector3(0.35,1.7,0.35),Vector3(0,0.8,0),Color("#3a4352"))
        box(root,Vector3(1.1,0.55,0.5),Vector3(0,1.65,0),color)
    elif kind == "bow":
        torus(root,0.72,0.10,Vector3(0,0.9,0),color)
    else:
        box(root,Vector3(0.18,1.8,0.18),Vector3(0,0.9,0),color)
        box(root,Vector3(0.45,0.14,0.22),Vector3(0,1.65,0),Color("#dce8f5"))
    return root
