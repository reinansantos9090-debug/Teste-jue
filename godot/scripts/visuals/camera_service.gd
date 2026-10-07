extends Node
var camera:Camera3D
var target:Node3D
var shake_time:=0.0
var strength:=0.0

func setup(view:Camera3D,follow:Node3D)->void:
    camera=view
    target=follow

func trigger(duration:float=0.12,amount:float=0.10)->void:
    shake_time=maxf(shake_time,duration)
    strength=maxf(strength,amount)

func _process(delta:float)->void:
    if camera==null or target==null:return
    camera.position=target.position+Vector3(0,14,17)
    camera.look_at(target.position+Vector3(0,0.9,0),Vector3.UP)
    if shake_time>0.0:
        shake_time=maxf(0.0,shake_time-delta)
        camera.position+=Vector3(randf_range(-1,1),randf_range(-0.5,0.5),randf_range(-1,1))*strength
