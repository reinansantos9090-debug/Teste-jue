extends Node
## Pooled visual effects for mobile-safe combat feedback.

var pool:Array[GPUParticles3D]=[]
var active_rings:Array[Node3D]=[]
var max_pool:=24

func burst(parent:Node3D,pos:Vector3,color:=Color("#ffe16a"),amount:=10.0)->void:
    var fx:=_take()
    fx.position=pos
    fx.amount=int(amount)
    fx.modulate=color
    parent.add_child(fx)
    fx.emitting=true
    get_tree().create_timer(0.38).timeout.connect(func():_release(fx))

func hit_burst(parent:Node3D,pos:Vector3,color:=Color("#ffe16a"))->void:
    burst(parent,pos,color,10.0)
    ring(parent,pos,0.42,color,0.18)

func skill_burst(parent:Node3D,pos:Vector3,color:=Color("#66eaff"))->void:
    burst(parent,pos,color,16.0)
    ring(parent,pos,0.85,color,0.24)
    ring(parent,pos,0.48,Color("#f7fbff"),0.17)

func dodge_trail(parent:Node3D,pos:Vector3,dir:Vector3,color:=Color("#67e8ff"))->void:
    var root:=Node3D.new()
    root.position=pos
    parent.add_child(root)
    for i in range(4):
        var s:=SphereMesh.new()
        s.radius=0.08
        s.height=0.16
        s.material=StandardMaterial3D.new()
        s.material.albedo_color=color
        var n:=MeshInstance3D.new()
        n.mesh=s
        n.position=-dir.normalized()*float(i)*0.35+Vector3(0,0.75,0)
        root.add_child(n)
        n.scale=Vector3.ONE*(1.0-float(i)*0.15)
    var tw:=create_tween()
    tw.tween_property(root,"scale",Vector3(0.2,0.2,0.2),0.22)
    tw.tween_callback(root.queue_free)

func boss_phase(parent:Node3D,pos:Vector3,color:=Color("#ff5ad9"))->void:
    burst(parent,pos+Vector3(0,1.0,0),color,28.0)
    ring(parent,pos,1.35,color,0.35)
    ring(parent,pos,2.10,Color("#ffffff"),0.42)

func death(parent:Node3D,pos:Vector3,color:=Color("#9ea7b8"))->void:
    burst(parent,pos+Vector3(0,0.8,0),color,20.0)
    ring(parent,pos,0.65,color,0.28)

func ring(parent:Node3D,pos:Vector3,radius:float,color:Color,duration:float=0.25)->void:
    var n:=MeshInstance3D.new()
    var mesh:=TorusMesh.new()
    mesh.inner_radius=radius*0.72
    mesh.outer_radius=radius
    mesh.rings=18
    mesh.ring_segments=8
    mesh.material=StandardMaterial3D.new()
    mesh.material.albedo_color=color
    mesh.material.emission_enabled=true
    mesh.material.emission=color
    mesh.material.emission_energy_multiplier=1.2
    n.mesh=mesh
    n.position=pos+Vector3(0,0.04,0)
    n.rotation.x=PI*0.5
    n.scale=Vector3(0.35,0.35,0.35)
    parent.add_child(n)
    active_rings.append(n)
    var tw:=create_tween()
    tw.set_parallel(true)
    tw.tween_property(n,"scale",Vector3.ONE,duration)
    tw.tween_property(n,"modulate:a",0.0,duration)
    tw.set_parallel(false)
    tw.tween_callback(func():
        active_rings.erase(n)
        if is_instance_valid(n): n.queue_free()
    )

func floating_number(parent:Node3D,pos:Vector3,value:float,critical:=false)->void:
    var label:=Label3D.new()
    label.text=("+%d" % int(value)) if value<0 else str(int(value))
    label.modulate=Color("#ffe06e") if critical else Color.WHITE
    label.font_size=32 if critical else 24
    label.position=pos+Vector3(0,1.0,0)
    label.outline_size=6
    parent.add_child(label)
    var tw:=create_tween()
    tw.set_parallel(true)
    tw.tween_property(label,"position",label.position+Vector3(0,1.1,0),0.65)
    tw.tween_property(label,"modulate:a",0.0,0.65)
    tw.set_parallel(false)
    tw.tween_callback(label.queue_free)

func _take()->GPUParticles3D:
    if pool.size()>0:
        return pool.pop_back()
    var fx:=GPUParticles3D.new()
    fx.amount=12
    fx.lifetime=0.34
    fx.one_shot=true
    var proc:=ParticleProcessMaterial.new()
    proc.direction=Vector3(0,1,0)
    proc.spread=180.0
    proc.initial_velocity_min=1.2
    proc.initial_velocity_max=4.0
    proc.gravity=Vector3(0,-8,0)
    fx.process_material=proc
    var mesh:=SphereMesh.new()
    mesh.radius=0.055
    mesh.height=0.11
    mesh.radial_segments=6
    mesh.rings=4
    fx.draw_pass_1=mesh
    return fx

func _release(fx:GPUParticles3D)->void:
    if not is_instance_valid(fx):return
    if fx.get_parent():fx.get_parent().remove_child(fx)
    fx.emitting=false
    if pool.size()<max_pool:pool.append(fx)
    else:fx.queue_free()
