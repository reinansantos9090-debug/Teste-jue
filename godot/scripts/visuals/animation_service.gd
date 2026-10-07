extends Node

func play_idle(actor: Node3D, time_value: float) -> void:
    if actor == null or not is_instance_valid(actor): return
    var visual := actor.get_node_or_null("Node3D")
    if visual:
        visual.position.y = sin(time_value * 3.0) * 0.035

func play_attack(actor: Node3D) -> void:
    if actor == null or not is_instance_valid(actor): return
    actor.scale = Vector3(1.08,0.92,1.08)
    create_tween().tween_property(actor,"scale",Vector3.ONE,0.14).set_trans(Tween.TRANS_BACK)

func play_hit(actor: Node3D) -> void:
    if actor == null or not is_instance_valid(actor): return
    actor.rotation.z = 0.06
    create_tween().tween_property(actor,"rotation:z",0.0,0.12)
