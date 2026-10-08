extends "res://scripts/gameplay/enemy_agent.gd"
## Three-phase boss controller with authored attack patterns per boss.

signal phase_changed(phase:int)
signal summon_requested(monster_id:String,count:int)
signal projectile_requested(origin:Vector3,target:Vector3,damage:float)
signal area_requested(center:Vector3,radius:float,damage:float)

var boss_id:=""
var boss_stats:Dictionary={}
var phase:=1
var special_timer:=2.2
var summon_timer:=8.0
var pattern_index:=0

func configure_boss(id:String,target_node:Node3D,database:Node,scale:=1.0)->void:
    boss_id=id
    boss_stats=database.BOSSES.get(id,database.BOSSES.values()[0])
    target=target_node
    difficulty=maxf(0.6,scale)
    behavior="boss"
    stats={
        "name":boss_stats["name"],
        "hp":float(boss_stats["hp"])*difficulty,
        "damage":float(boss_stats["projectile_damage"])*difficulty,
        "speed":float(boss_stats["speed"]),
        "xp":650+int(boss_stats["hp"])*0.18
    }
    max_hp=float(stats["hp"])
    hp=max_hp
    phase=1
    pattern_index=0
    special_timer=2.0
    summon_timer=float(boss_stats["summon_interval"])
    add_to_group("enemy_agents")

func _process(delta:float)->void:
    if state==State.DEAD or target==null or not is_instance_valid(target):return
    attack_timer=maxf(0.0,attack_timer-delta)
    special_timer=maxf(0.0,special_timer-delta)
    summon_timer=maxf(0.0,summon_timer-delta)
    _update_phase()
    _move_boss(delta)
    if special_timer<=0.0:
        _run_pattern()
    if summon_timer<=0.0:
        summon_timer=float(boss_stats["summon_interval"])*({"1":1.0,"2":0.78,"3":0.58}.get(str(phase),1.0))
        summon_requested.emit(str(boss_stats.get("summon_id","aether_slime")),1 if phase==1 else (2 if phase==2 else 3))

func _update_phase()->void:
    var ratio:=get_hp_ratio()
    var desired:=1
    if ratio<=float(boss_stats.get("phase3_ratio",0.34)):desired=3
    elif ratio<=float(boss_stats.get("phase2_ratio",0.68)):desired=2
    if desired!=phase:
        phase=desired
        phase_changed.emit(phase)

func _move_boss(delta:float)->void:
    var direction:=target.global_position-global_position
    direction.y=0.0
    if direction.length_squared()<0.001:return
    var speed:=float(stats["speed"])*(1.0 if phase==1 else (1.18 if phase==2 else 1.38))
    if direction.length()>3.0:_move_towards(direction,speed,delta)
    elif attack_timer<=0.0 and target.has_method("take_enemy_damage"):
        attack_timer=1.0 if phase<3 else 0.72
        target.take_enemy_damage(float(stats["damage"])*(0.72 if phase==1 else (0.90 if phase==2 else 1.10)))

func _move_towards(direction:Vector3,speed:float,delta:float)->void:
    direction.y=0.0
    if direction.length_squared()<0.001:return
    direction=direction.normalized()
    global_position+=direction*speed*delta
    look_at(global_position+direction,Vector3.UP)

func _run_pattern()->void:
    var patterns:Array=boss_stats.get("patterns",["burst","charge","summon","nova"])
    if patterns.is_empty():patterns=["burst"]
    var pattern:=str(patterns[pattern_index%patterns.size()])
    pattern_index+=1
    match pattern:
        "burst","beam","harvest","triple":
            _projectile_fan(2 if phase==1 else (3 if phase==2 else 5),0.24 if phase==1 else 0.34)
            special_timer=2.5 if phase==1 else (1.8 if phase==2 else 1.35)
        "charge","dash":
            var dir:Vector3=(target.global_position-global_position).normalized()
            global_position+=Vector3(dir.x,0,dir.z)*(4.5 if phase==1 else (6.0 if phase==2 else 7.5))
            area_requested.emit(global_position,2.6 if phase<3 else 3.8,float(stats["damage"])*(0.9+0.25*phase))
            special_timer=3.0 if phase==1 else 2.3
        "summon":
            summon_requested.emit(str(boss_stats.get("summon_id","aether_slime")),2 if phase<3 else 4)
            special_timer=3.7
        "nova","ring","roots","prism","rain","timewave":
            area_requested.emit(target.global_position,4.2+phase*0.7,float(stats["damage"])*(0.65+0.20*phase))
            special_timer=3.0 if phase==1 else 2.1
        "teleport":
            var angle:=Time.get_ticks_msec()*0.001
            global_position=target.global_position+Vector3(cos(angle),0,sin(angle))*6.5
            area_requested.emit(global_position,2.2,float(stats["damage"]))
            special_timer=3.8
        _:
            projectile_requested.emit(global_position+Vector3(0,1.3,0),target.global_position,float(stats["damage"]))
            special_timer=2.5

func _projectile_fan(count:int,spread:float)->void:
    var base:Vector3=(target.global_position-global_position).normalized()
    base.y=0.0
    var start:= -float(count-1)*spread*0.5
    for i in range(count):
        var angle:=start+float(i)*spread
        var dir:=Vector3(
            base.x*cos(angle)-base.z*sin(angle),0.0,
            base.x*sin(angle)+base.z*cos(angle)
        ).normalized()
        projectile_requested.emit(global_position+Vector3(0,1.35,0),global_position+Vector3(dir.x,0,dir.z)*18.0+Vector3(0,1.35,0),float(stats["damage"])*(1.0+0.10*phase))

func get_hp_ratio()->float:
    return clampf(hp/maxf(1.0,max_hp),0.0,1.0)
