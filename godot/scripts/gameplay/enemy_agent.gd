extends Node3D
## Behavior-driven mobile enemy agent. Each behavior changes movement, attacks,
## support actions or death effects while sharing a small deterministic update loop.

signal defeated(actor:Node3D,xp:int)
signal projectile_requested(origin:Vector3,target:Vector3,damage:float)
signal summon_requested(monster_id:String,count:int)
signal area_requested(center:Vector3,radius:float,damage:float)

enum State { IDLE, CHASE, ATTACK, STUNNED, DEAD }

var state:=State.IDLE
var monster_id:=""
var stats:Dictionary={}
var behavior:=""
var target:Node3D
var hp:=1.0
var max_hp:=1.0
var attack_timer:=0.0
var special_timer:=2.0
var stun_timer:=0.0
var wander_timer:=0.0
var difficulty:=1.0
var visual:Node3D
var rng:=RandomNumberGenerator.new()
var base_speed:=2.0

func configure(id:String,target_node:Node3D,database:Node,scale:=1.0)->void:
    monster_id=id
    stats=database.MONSTERS.get(id,database.MONSTERS["aether_slime"])
    target=target_node
    difficulty=maxf(0.5,scale)
    behavior=str(stats.get("behavior","melee"))
    add_to_group("enemy_agents")
    max_hp=float(stats.get("hp",50.0))*difficulty
    hp=max_hp
    base_speed=float(stats.get("speed",2.0))
    rng.seed=abs(hash("%s:%s"%(id,get_instance_id())))
    attack_timer=rng.randf_range(0.1,0.7)
    special_timer=rng.randf_range(1.0,3.0)

func attach_visual(node:Node3D)->void:
    visual=node
    add_child(visual)

func _process(delta:float)->void:
    if state==State.DEAD or target==null or not is_instance_valid(target):return
    attack_timer=maxf(0.0,attack_timer-delta)
    special_timer=maxf(0.0,special_timer-delta)
    stun_timer=maxf(0.0,stun_timer-delta)
    wander_timer-=delta
    if stun_timer>0.0:
        state=State.STUNNED
        return
    var distance:=global_position.distance_to(target.global_position)
    match behavior:
        "ranged","sniper","turret":
            _ranged_behavior(delta,distance)
        "flying","orbit","teleporter","stealth":
            _mobile_behavior(delta,distance)
        "healer","guardian":
            _support_behavior(delta,distance)
        "charger","berserker","assassin","swarm","knockback":
            _aggressive_behavior(delta,distance)
        "mine_layer","bomber","controller":
            _special_behavior(delta,distance)
        "splitter":
            _split_behavior(delta,distance)
        _:
            _melee_behavior(delta,distance)

func _speed()->float:
    var value:=base_speed*difficulty
    if behavior=="berserker" and get_hp_ratio()<0.40:value*=1.65
    if behavior=="swarm":value*=1.35
    if behavior=="charger" and special_timer<=0.0:value*=2.45
    if behavior=="assassin":value*=1.35
    return value

func _move_towards(direction:Vector3,speed:float,delta:float)->void:
    direction.y=0.0
    if direction.length_squared()<0.001:return
    direction=direction.normalized()
    global_position+=direction*speed*delta
    look_at(global_position+direction,Vector3.UP)

func _melee_behavior(delta:float,distance:float)->void:
    if distance<=2.0:
        state=State.ATTACK
        _melee_attack(1.0)
    elif distance<=12.0:
        state=State.CHASE
        _move_towards(target.global_position-global_position,_speed(),delta)
    else:
        _wander(delta)

func _aggressive_behavior(delta:float,distance:float)->void:
    if behavior=="charger" and special_timer<=0.0:
        special_timer=3.0+rng.randf_range(0.0,1.0)
    if distance<=2.2:
        state=State.ATTACK
        _melee_attack(1.0 if behavior!="knockback" else 1.25)
    elif distance<=15.0:
        state=State.CHASE
        _move_towards(target.global_position-global_position,_speed(),delta)
    else:_wander(delta)

func _ranged_behavior(delta:float,distance:float)->void:
    if behavior=="turret":
        state=State.ATTACK
        if special_timer<=0.0:
            special_timer=2.2
            projectile_requested.emit(global_position+Vector3(0,1.0,0),target.global_position,float(stats["damage"])*difficulty)
        return
    if distance<5.0:
        _move_towards(global_position-target.global_position,_speed()*0.75,delta)
    elif distance>10.0 and distance<20.0:
        _move_towards(target.global_position-global_position,_speed()*0.70,delta)
    else:
        state=State.ATTACK
    if special_timer<=0.0 and distance<22.0:
        special_timer=2.0 if behavior=="sniper" else 1.3
        var damage:=float(stats["damage"])*difficulty*(1.35 if behavior=="sniper" else 1.0)
        projectile_requested.emit(global_position+Vector3(0,1.1,0),target.global_position,damage)

func _mobile_behavior(delta:float,distance:float)->void:
    if behavior=="teleporter" and special_timer<=0.0 and distance<13.0:
        special_timer=3.0
        var angle:=rng.randf_range(0.0,TAU)
        global_position=target.global_position+Vector3(cos(angle),0,sin(angle))*rng.randf_range(4.0,7.0)
    if behavior=="flying":
        global_position.y=1.6+sin(Time.get_ticks_msec()*0.004+float(get_instance_id()%13))*0.35
    if behavior=="orbit":
        var offset:=target.global_position-global_position
        if offset.length()>0.1:
            var tangent:=Vector3(-offset.z,0,offset.x).normalized()
            _move_towards(tangent,_speed(),delta)
    elif distance<=2.3:
        _melee_attack(1.15)
    else:
        _move_towards(target.global_position-global_position,_speed(),delta)
    if behavior=="stealth" and visual:
        visual.modulate.a=0.38 if distance>6.0 else 1.0

func _support_behavior(delta:float,distance:float)->void:
    if distance>4.5:_move_towards(target.global_position-global_position,_speed(),delta)
    if special_timer<=0.0:
        special_timer=4.0
        var healed:=false
        for node in get_tree().get_nodes_in_group("enemy_agents"):
            if node!=self and is_instance_valid(node) and node.global_position.distance_to(global_position)<6.0 and node.has_method("heal_percent"):
                node.heal_percent(0.12);healed=true
        if healed:vfx_pulse()
    if distance<2.2:_melee_attack(0.65)

func _special_behavior(delta:float,distance:float)->void:
    if behavior=="bomber" and distance<2.7:
        area_requested.emit(global_position,3.4,float(stats["damage"])*1.45)
        _melee_attack(0.7)
        return
    if distance<2.0:_melee_attack(1.0)
    elif distance<14.0:_move_towards(target.global_position-global_position,_speed(),delta)
    if special_timer<=0.0:
        special_timer=3.0
        if behavior=="mine_layer":
            area_requested.emit(target.global_position,2.1,float(stats["damage"])*0.75)
        elif behavior=="controller":
            area_requested.emit(target.global_position,4.4,float(stats["damage"])*0.55)

func _split_behavior(delta:float,distance:float)->void:
    if distance<2.0:_melee_attack(0.9)
    else:_move_towards(target.global_position-global_position,_speed(),delta)

func _melee_attack(multiplier:float)->void:
    if attack_timer>0.0:return
    attack_timer=1.20/maxf(0.65,difficulty)
    if target.has_method("take_enemy_damage"):
        target.take_enemy_damage(float(stats.get("damage",5.0))*difficulty*multiplier)
    if behavior=="knockback":
        area_requested.emit(target.global_position,2.5,float(stats.get("damage",5.0))*0.5)

func _wander(delta:float)->void:
    if wander_timer<=0.0:wander_timer=rng.randf_range(1.0,2.5)
    var phase:=Time.get_ticks_msec()*0.001+float(get_instance_id()%17)
    global_position+=Vector3(cos(phase),0,sin(phase))*0.10*delta

func take_damage(amount:float,critical:=false)->void:
    if state==State.DEAD:return
    var effective:=amount
    if behavior=="shielded" or behavior=="guardian":
        effective*=0.58 if special_timer>0.0 else 0.82
    if behavior=="stealth" and visual and visual.modulate.a<0.7:effective*=0.82
    hp-=effective
    stun_timer=0.20 if critical else 0.08
    if hp<=0.0:
        hp=0.0
        state=State.DEAD
        if behavior=="splitter":summon_requested.emit("aether_slime",2)
        if behavior=="bomber":area_requested.emit(global_position,4.6,float(stats.get("damage",10.0))*1.65)
        defeated.emit(self,int(round(float(stats.get("xp",10))*difficulty)))

func heal_percent(percent:float)->void:
    if state==State.DEAD:return
    hp=minf(max_hp,hp+max_hp*clampf(percent,0.0,0.5))

func get_hp_ratio()->float:
    return clampf(hp/maxf(1.0,max_hp),0.0,1.0)

func vfx_pulse()->void:
    if visual:
        visual.scale=Vector3.ONE*1.08
        create_tween().tween_property(visual,"scale",Vector3.ONE,0.20)
