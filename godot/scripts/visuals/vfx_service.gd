extends Node
var pool:Array[GPUParticles3D]=[]

func burst(parent:Node3D,pos:Vector3)->void:
    var fx:=_take()
    fx.position=pos
    parent.add_child(fx)
    fx.emitting=true
    get_tree().create_timer(0.36).timeout.connect(func():_release(fx))

func _take()->GPUParticles3D:
    if pool.size()>0:return pool.pop_back()
    var fx:=GPUParticles3D.new()
    fx.amount=10
    fx.lifetime=0.32
    fx.one_shot=true
    var proc:=ParticleProcessMaterial.new()
    proc.direction=Vector3.UP
    proc.initial_velocity_min=1.0
    proc.initial_velocity_max=3.5
    proc.gravity=Vector3(0,-7,0)
    fx.process_material=proc
    var mesh:=SphereMesh.new()
    mesh.radius=0.055
    mesh.height=0.11
    fx.draw_pass_1=mesh
    return fx

func _release(fx:GPUParticles3D)->void:
    if not is_instance_valid(fx):return
    if fx.get_parent():fx.get_parent().remove_child(fx)
    fx.emitting=false
    if pool.size()<16:pool.append(fx)
    else:fx.queue_free()
