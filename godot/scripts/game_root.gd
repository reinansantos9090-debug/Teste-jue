extends Node3D
const StateScript=preload("res://scripts/core/game_state.gd")
const SaveScript=preload("res://scripts/core/save_service.gd")
const ContentScript=preload("res://scripts/core/content_database.gd")
const PerfScript=preload("res://scripts/core/performance_service.gd")
const WorldScript=preload("res://scripts/world/world_service.gd")
const EnemyScript=preload("res://scripts/gameplay/enemy_agent.gd")
const BossScript=preload("res://scripts/gameplay/boss_agent.gd")
const QuestScript=preload("res://scripts/gameplay/quest_service.gd")
const StoryScript=preload("res://scripts/gameplay/story_service.gd")
const EventScript=preload("res://scripts/gameplay/event_service.gd")
const InventoryScript=preload("res://scripts/gameplay/inventory_service.gd")
const CombatScript=preload("res://scripts/gameplay/combat_service.gd")
const AudioScript=preload("res://scripts/core/audio_service.gd")
const HudScript=preload("res://scripts/ui/hud.gd")
const Factory=preload("res://scripts/visuals/stylized_factory.gd")
const HunterAvatar=preload("res://scripts/visuals/hunter_avatar.gd")
const VfxScript=preload("res://scripts/visuals/vfx_service.gd")
const CutsceneScript=preload("res://scripts/gameplay/cutscene_service.gd")
const DailyScript=preload("res://scripts/gameplay/daily_service.gd")
const CoreScript=preload("res://scripts/gameplay/core_service.gd")
const ProgressionScript=preload("res://scripts/gameplay/progression_service.gd")
const WardrobeScript=preload("res://scripts/gameplay/wardrobe_service.gd")
const PortalScript=preload("res://scripts/gameplay/portal_service.gd")
const DirectorScript=preload("res://scripts/gameplay/offline_director.gd")
const OperationsMenuScript=preload("res://scripts/ui/activity_menu.gd")
const MapScript=preload("res://scripts/world/map_service.gd")
const LootScript=preload("res://scripts/gameplay/loot_service.gd")
const SkillTreeScript=preload("res://scripts/gameplay/skill_tree_service.gd")

var state:Node
var save:Node
var content:Node
var perf:Node
var world:Node
var maps:Node
var loot:Node
var skill_tree:Node
var quests:Node
var story:Node
var events:Node
var inventory:Node
var combat:Node
var audio:Node
var hud:CanvasLayer
var vfx:Node
var cutscene:CanvasLayer
var daily:Node
var cores:Node
var progression:Node
var wardrobe:Node
var portal_service:Node
var director:Node
var operations_menu:CanvasLayer
var player:Node3D
var camera:Camera3D
var enemies:Array[Node3D]=[]
var boss:Node3D
var shots:Array[Node3D]=[]
var mode:="HQ"
var attack_ready:=true
var skills=[true,true,true]
var dodge_ready:=true
var invulnerability:=0.0
var dead_timer:=0.0
var damage_buff_time:=0.0
var damage_buff:=0.0
var portal_step:=-1
var activity_remaining:=0.0
var activity_goal:=0
var activity_progress:=0
var clock:=0.0

func _ready()->void:
    state=StateScript.new();add_child(state)
    content=ContentScript.new();add_child(content)
    maps=MapScript.new();add_child(maps)
    loot=LootScript.new();add_child(loot);loot.setup(state,content)
    skill_tree=SkillTreeScript.new();add_child(skill_tree);skill_tree.setup(state,content)
    save=SaveScript.new();add_child(save);save.setup(state);save.load_now()
    perf=PerfScript.new();add_child(perf)
    perf.quality_changed.connect(_apply_quality)
    world=WorldScript.new();add_child(world);world.setup(self,content,maps)
    quests=QuestScript.new();add_child(quests);quests.setup(state,content)
    quests.completed.connect(_quest_completed)
    story=StoryScript.new();add_child(story);story.setup(state)
    events=EventScript.new();add_child(events);events.setup(state,content)
    daily=DailyScript.new();add_child(daily);daily.setup(state)
    cores=CoreScript.new();add_child(cores);cores.setup(state,content)
    progression=ProgressionScript.new();add_child(progression);progression.setup(state)
    wardrobe=WardrobeScript.new();add_child(wardrobe);wardrobe.setup(state,content)
    portal_service=PortalScript.new();add_child(portal_service);portal_service.setup(state)
    director=DirectorScript.new();add_child(director);director.setup(state,content)
    vfx=VfxScript.new();add_child(vfx)
    cutscene=CutsceneScript.new();add_child(cutscene)
    cutscene.next_requested.connect(_story)
    inventory=InventoryScript.new();add_child(inventory);inventory.setup(state)
    audio=AudioScript.new();add_child(audio)
    combat=CombatScript.new();add_child(combat);combat.setup(state,audio)

    var hunter_scene:PackedScene=load("res://scenes/player/Hunter.tscn")
    player=hunter_scene.instantiate() if hunter_scene else HunterAvatar.new()
    player.name="Hunter";add_child(player)
    _set_player()

    camera=Camera3D.new()
    camera.fov=48.0
    add_child(camera)
    camera.current=true

    hud=HudScript.new();add_child(hud);hud.setup(state)
    hud.attack_pressed.connect(_attack)
    hud.ability_pressed.connect(_ability)
    hud.portal_pressed.connect(_portal)
    hud.inventory_pressed.connect(func():hud.set_mode("INVENTÁRIO • "+inventory.summary()))
    hud.story_pressed.connect(_story)
    hud.wardrobe_pressed.connect(_wardrobe)
    hud.dodge_pressed.connect(_dodge)
    hud.menu_pressed.connect(_open_operations)
    hud.arsenal_pressed.connect(_cycle_weapon)

    operations_menu=OperationsMenuScript.new()
    add_child(operations_menu)
    operations_menu.selected.connect(_run_operation)

    world.build_hq()
    _apply_quality(perf.current())
    quests.start("story_01")
    hud.set_mode("QG NO TERRAÇO • PORTAL CENTRAL")

func _set_player()->void:
    if not is_instance_valid(player):return
    player.position=Vector3(0,0.1,6)
    var selected=content.STYLES[0]
    for item in content.STYLES:
        if item["id"]==str(state.data["wardrobe"]["style"]):
            selected=item
            break
    if player.has_method("setup"):
        player.setup(selected,str(state.data["loadout"]["weapon"]))
    else:
        for c in player.get_children():c.queue_free()
        Factory.make_hunter(player,selected["primary"],selected["secondary"],selected["head"])

func _portal()->void:
    if cutscene and cutscene.is_active():return
    audio.click()
    match mode:
        "HQ":_enter_expedition()
        "EXPEDITION":_enter_rift()
        "RIFT":_enter_versus()
        "VERSUS":_enter_hq()
        _: _enter_hq()

func _enter_hq()->void:
    mode="HQ"
    _clear_combat()
    portal_service.enter_hq()
    director.enter_hq()
    activity_remaining=0.0
    activity_progress=0
    activity_goal=0
    world.build_hq()
    player.position=Vector3(0,0.1,6)
    hud.set_mode("QG NO TERRAÇO • PORTAL CENTRAL")

func _enter_expedition()->void:
    mode="EXPEDITION"
    portal_step=(portal_step+1)%maps.maps.size()
    var ids:Array=maps.maps.keys()
    ids.sort()
    var map_id:=str(ids[portal_step%ids.size()])
    portal_service.enter_expedition(map_id)
    director.start_expedition(map_id)
    world.build_biome(map_id)
    activity_remaining=maps.duration(map_id)
    activity_goal=maps.activity_goal(map_id)
    activity_progress=0
    player.position=Vector3(0,0.1,6)
    _spawn_wave(mini(16,perf.max_enemies()))
    quests.add_map(map_id)
    hud.set_mode("EXPEDIÇÃO • %s • 0/%d"%[maps.get_map(map_id)["name"],activity_goal])

func _enter_rift()->void:
    mode="RIFT"
    portal_service.enter_rift("rift_dome")
    director.start_rift("rift_dome")
    world.build_biome("crystal_forest")
    activity_remaining=0.0
    activity_progress=0
    activity_goal=1
    _spawn_wave(perf.max_enemies())
    var ids:Array=content.BOSSES.keys()
    ids.sort()
    var boss_id:=str(ids[portal_step%ids.size()])
    _spawn_boss(boss_id)
    hud.set_mode("RIFT • "+str(content.BOSSES[boss_id]["name"]))

func _enter_versus()->void:
    mode="VERSUS"
    portal_service.enter_versus()
    director.start_versus()
    world.build_biome("sky_ruins")
    activity_remaining=75.0
    activity_goal=8
    activity_progress=0
    _spawn_wave(mini(8,perf.max_enemies()))
    hud.set_mode("VERSUS • SIMULAÇÃO OFFLINE")

func _open_operations()->void:
    if operations_menu and not operations_menu.is_open():
        operations_menu.show_menu()

func _run_operation(action_id:String)->void:
    match action_id:
        "expedition":_enter_expedition()
        "rift":_enter_rift()
        "versus":_enter_versus()
        "daily":hud.set_mode("DIÁRIAS • "+daily.summary())
        "event":
            for item in content.EVENTS:
                var id:=str(item["id"])
                if id!=director.active_event:
                    director.start_event(id)
                    events.start(id)
                    hud.set_mode("EVENTO • "+str(item["name"]))
                    break
        "cores":
            hud.set_mode("NÚCLEOS • "+cores.summary())
        "skills":
            hud.set_mode("ÁRVORE • "+skill_tree.summary())
        "craft":
            var crafted:=false
            for recipe in content.RECIPES:
                if inventory.craft(recipe):
                    crafted=true
                    hud.set_mode("OFICINA • FABRICADO: "+str(recipe["name"]))
                    break
            if not crafted:hud.set_mode("OFICINA • MATERIAIS INSUFICIENTES")
        "history":_story()

func _cycle_weapon()->void:
    var ids:Array=content.WEAPONS.keys()
    if ids.is_empty():return
    ids.sort()
    var current:=str(state.data["loadout"]["weapon"])
    var idx:=ids.find(current)
    idx=(idx+1)%ids.size()
    var next:=str(ids[idx])
    state.data["loadout"]["weapon"]=next
    var weapon:Dictionary=content.WEAPONS[next]
    var abilities:Array=weapon.get("abilities",[])
    for i in range(mini(3,abilities.size())):
        state.data["loadout"]["ability_"+str(i+1)]=str(abilities[i])
    state.state_changed.emit()
    if player.has_method("equip_weapon"):player.equip_weapon(next)
    hud.set_mode("ARSENAL • %s • %.0f DANO"%[str(weapon["name"]),float(weapon["damage"])])

func _attack()->void:
    if not attack_ready or dead_timer>0.0:return
    attack_ready=false
    var weapon:Dictionary=content.WEAPONS.get(str(state.data["loadout"]["weapon"]),content.WEAPONS["volt_blades"])
    var target:=_nearest(float(weapon["range"])+1.8)
    if target:
        if player.has_method("play_attack"):player.play_attack()
        var bonus:=_core("damage")+skill_tree.bonus("damage")+damage_buff
        var crit:=_core("crit")+skill_tree.bonus("crit")
        var amount:=combat.player_damage(weapon,int(state.get_hunter()["level"]),bonus,crit)
        combat.hit(target,amount)
        vfx.hit_burst(self,target.position+Vector3(0,1,0),Color("#ffe16a"))
        vfx.floating_number(self,target.position+Vector3(0,1.5,0),amount,false)
    var speed_bonus:=_core("attack_speed")+skill_tree.bonus("attack_speed")
    var cooldown_bonus:=clampf(_core("cooldown")+skill_tree.bonus("cooldown"),0.0,0.75)
    var wait:=float(weapon["rate"])*(1.0-cooldown_bonus)/(1.0+speed_bonus)
    get_tree().create_timer(maxf(0.08,wait)).timeout.connect(func():attack_ready=true)

func _ability(i:int)->void:
    if i<0 or i>=3 or dead_timer>0.0 or not skills[i]:return
    var weapon:Dictionary=content.WEAPONS.get(str(state.data["loadout"]["weapon"]),content.WEAPONS["volt_blades"])
    var ids:Array=weapon.get("abilities",[])
    if i>=ids.size():return
    var ability:Dictionary=content.ABILITIES.get(str(ids[i]),{})
    if ability.is_empty():return
    var hunter:=state.get_hunter()
    if float(hunter["energy"])<float(ability["energy"]):return

    skills[i]=false
    hunter["energy"]=float(hunter["energy"])-float(ability["energy"])
    daily.add("daily_skill",1)
    if player.has_method("play_skill"):player.play_skill(i)
    audio.skill()
    _execute_ability(
        str(ability.get("effect","area")),
        float(ability.get("power",1.0)),
        float(ability.get("radius",4.0))
    )
    var cooldown:=float(ability["cooldown"])*(1.0-clampf(_core("cooldown")+skill_tree.bonus("cooldown"),0.0,0.75))
    get_tree().create_timer(maxf(0.20,cooldown)).timeout.connect(func():skills[i]=true)

func _execute_ability(effect:String,power:float,radius:float)->void:
    var origin:=player.position
    match effect:
        "dash":
            var target:=_nearest(radius+8.0)
            if target:
                var dir:Vector3=(target.position-player.position).normalized()
                player.position=target.position-dir*1.8
                combat.hit(target,55.0*power)
            vfx.skill_burst(self,player.position,Color("#66eaff"))
        "area","pull_area":
            for e in enemies.duplicate():
                if is_instance_valid(e) and e.position.distance_to(origin)<=radius:
                    combat.hit(e,48.0*power)
                    if effect=="pull_area":
                        e.position=e.position.lerp(origin,0.20)
            if boss and is_instance_valid(boss) and boss.position.distance_to(origin)<=radius:
                combat.hit(boss,65.0*power)
            vfx.skill_burst(self,origin,Color("#b990ff"))
        "multi_projectile","chain","beam":
            var count:=0
            for e in enemies.duplicate():
                if is_instance_valid(e) and e.position.distance_to(origin)<=radius:
                    combat.hit(e,43.0*power)
                    count+=1
                    if count>=8:break
            if boss and is_instance_valid(boss) and boss.position.distance_to(origin)<=radius:
                combat.hit(boss,72.0*power)
            vfx.skill_burst(self,origin,Color("#7defff"))
        "barrier":
            invulnerability=maxf(invulnerability,2.5)
            vfx.skill_burst(self,origin,Color("#d4a7ff"))
        "buff_damage":
            damage_buff=power
            damage_buff_time=6.0
            vfx.skill_burst(self,origin,Color("#ffd65c"))
        _:
            vfx.skill_burst(self,origin,Color.WHITE)

func _spawn_wave(count:int)->void:
    _clear_combat()
    var pool:Array=maps.enemy_pool(str(portal_service.destination))
    if pool.is_empty():pool=content.MONSTERS.keys()
    for i in mini(count,perf.max_enemies()):
        _spawn_enemy(i,str(pool[i%pool.size()]))

func _spawn_enemy(i:int,id:="")->void:
    var pool:Array=maps.enemy_pool(str(portal_service.destination))
    if pool.is_empty():pool=content.MONSTERS.keys()
    var kind:=id if not id.is_empty() else str(pool[i%pool.size()])
    var enemy_scene:PackedScene=load("res://scenes/enemies/Enemy.tscn")
    var e:Node3D=enemy_scene.instantiate() if enemy_scene else EnemyScript.new()
    add_child(e)
    e.position=_spawn_position(i)
    var scale:=maps.difficulty(str(portal_service.destination))
    e.configure(kind,player,content,scale)
    var behavior:=str(content.MONSTERS.get(kind,{}).get("behavior","melee"))
    _attach_enemy_visual(e,behavior,i)
    e.defeated.connect(_enemy_down)
    e.projectile_requested.connect(_shot)
    e.summon_requested.connect(_enemy_summon)
    e.area_requested.connect(_enemy_area)
    enemies.append(e)

func _attach_enemy_visual(e:Node3D,behavior:String,index:int)->void:
    var hue:=float(index%8)/8.0
    var base:=Color.from_hsv(hue,0.48,0.92)
    if behavior in ["flying","orbit"]:e.attach_visual(Factory.make_moth(e,base))
    elif behavior in ["guardian","shielded","knockback"]:e.attach_visual(Factory.make_golem(e,base,Color("#e9fcff")))
    elif behavior in ["berserker","crusher"]:e.attach_visual(Factory.make_colossus(e,base,Color("#ffd86b")))
    elif behavior in ["sniper","ranged","turret"]:e.attach_visual(Factory.make_crystal_monster(e,base))
    elif behavior in ["assassin","stealth","teleporter"]:e.attach_visual(Factory.make_hound(e,base))
    else:e.attach_visual(Factory.make_slime(e,base))

func _spawn_position(index:int)->Vector3:
    var angle:=float(index)*2.39996
    var radius:=7.0+float(index%5)*1.6
    return player.position+Vector3(cos(angle)*radius,0,sin(angle)*radius)

func _spawn_boss(id:String)->void:
    var boss_scene:PackedScene=load("res://scenes/bosses/PhaseBoss.tscn")
    var b:Node3D=boss_scene.instantiate() if boss_scene else BossScript.new()
    add_child(b)
    b.position=player.position+Vector3(0,0,-10)
    b.configure_boss(id,player,content,maps.difficulty("sky_ruins"))
    b.attach_visual(Factory.make_colossus(b,Color("#7d5ce8"),Color("#ffd86b")))
    b.phase_changed.connect(func(p:int):
        hud.set_mode("CHEFE • FASE %d"%p)
        if is_instance_valid(b):b.scale=Vector3.ONE*(1.0 if p==1 else (1.12 if p==2 else 1.24))
        vfx.boss_phase(self,b.position,Color("#ff6bd6"))
    )
    b.summon_requested.connect(func(k:String,n:int):
        for j in range(n):_spawn_enemy(enemies.size()+j,k)
    )
    b.projectile_requested.connect(_shot)
    b.area_requested.connect(_boss_area)
    b.defeated.connect(_boss_down)
    boss=b

func _enemy_summon(monster_id:String,count:int)->void:
    for i in range(mini(count,perf.max_enemies()-enemies.size())):
        _spawn_enemy(enemies.size()+i,monster_id)

func _enemy_area(center:Vector3,radius:float,damage:float)->void:
    if player.position.distance_to(center)<=radius:
        take_enemy_damage(damage)
    vfx.skill_burst(self,center,Color("#ff8c70"))

func _boss_area(center:Vector3,radius:float,damage:float)->void:
    if player.position.distance_to(center)<=radius:
        take_enemy_damage(damage)
    vfx.boss_phase(self,center,Color("#ff6bd6"))

func _shot(origin:Vector3,target:Vector3,damage:float)->void:
    var p:=Node3D.new()
    add_child(p)
    p.position=origin
    Factory.sphere(p,0.20,Vector3.ZERO,Color("#ff79db"))
    p.set_meta("v",(target-origin).normalized()*9.0)
    p.set_meta("d",damage)
    shots.append(p)

func _enemy_down(e:Node3D,x:int)->void:
    var monster_id:=str(e.monster_id)
    state.increment_stat("kills")
    progression.award_xp(x)
    var drop:Dictionary=loot.roll(monster_id,str(portal_service.destination),int(state.get_hunter()["level"]),events.multiplier())
    inventory.record_loot(drop)
    daily.add("daily_kills",1)
    director.record_kill()
    quests.add_kill(monster_id,1)
    vfx.death(self,e.position,Color("#a7d7ff"))
    if is_instance_valid(e):enemies.erase(e);e.queue_free()
    if mode=="EXPEDITION":
        activity_progress+=1
        var map_id:=str(portal_service.destination)
        quests.add_collect("scrap_plate",0)
        hud.set_mode("EXPEDIÇÃO • %s • %d/%d"%[maps.get_map(map_id)["name"],activity_progress,activity_goal])
        if activity_progress>=activity_goal:
            activity_remaining=0.0
            state.data["world"]["completed_expeditions"]=int(state.data["world"]["completed_expeditions"])+1
            hud.set_mode("EXPEDIÇÃO CONCLUÍDA • "+str(maps.get_map(map_id)["name"]))
            _clear_combat()

func _boss_down(b:Node3D,x:int)->void:
    state.increment_stat("bosses")
    progression.award_xp(roundi(float(x)*events.multiplier()))
    quests.add_boss(str(b.boss_id))
    quests.add_rift()
    daily.add("daily_rift",1)
    director.record_rift()
    var drop:Dictionary=loot.roll("aether_slime",str(portal_service.destination),int(state.get_hunter()["level"]),events.multiplier()*2.0)
    inventory.record_loot(drop)
    vfx.boss_phase(self,b.position,Color("#ff6bd6"))
    if is_instance_valid(b):b.queue_free()
    boss=null
    activity_progress=1
    activity_remaining=0.0
    hud.clear_boss()
    hud.set_mode("RIFT CONCLUÍDO • RECOMPENSAS • "+loot.describe(drop))
    audio.level_up()

func _quest_completed(id:String)->void:
    var q:Dictionary=quests.get_quest(id)
    if not q.is_empty():
        hud.set_mode("OBJETIVO CONCLUÍDO • "+str(q.get("title",id)))
    if id.begins_with("chapter_") or id.begins_with("boss_"):
        _story()

func _nearest(radius:float)->Node3D:
    var best:Node3D=null
    var best_distance:=radius
    for e in enemies:
        if is_instance_valid(e):
            var distance:=e.position.distance_to(player.position)
            if distance<best_distance:
                best_distance=distance
                best=e
    if boss and is_instance_valid(boss) and boss.position.distance_to(player.position)<best_distance:
        best=boss
    return best

func _dodge()->void:
    if not dodge_ready or dead_timer>0.0:return
    dodge_ready=false
    invulnerability=0.48
    if player.has_method("play_dodge"):player.play_dodge()
    var direction:Vector2=hud.move_vector if hud and hud.move_vector.length_squared()>0.01 else Input.get_vector("move_left","move_right","move_forward","move_back")
    if direction.length_squared()<0.01:direction=Vector2(0,-1)
    direction=direction.normalized()
    var move:=Vector3(direction.x,0,direction.y)
    var next:=player.position+move*3.2
    var map_id:=str(portal_service.destination)
    if mode=="EXPEDITION" and maps.blocked(map_id,next,0.6):
        next=player.position
    player.position=next
    vfx.dodge_trail(self,player.position,move)
    get_tree().create_timer(1.0).timeout.connect(func():dodge_ready=true)

func take_enemy_damage(d:float)->void:
    if invulnerability>0.0 or dead_timer>0.0:return
    var h:=state.get_hunter()
    var dodge:=clampf(_core("dodge")+skill_tree.bonus("dodge"),0.0,0.70)
    if randf()<dodge:return
    h["hp"]=maxf(0.0,float(h["hp"])-d)
    if player.has_method("play_hurt"):player.play_hurt()
    vfx.hit_burst(self,player.position+Vector3(0,0.8,0),Color("#ff7185"))
    if float(h["hp"])<=0.0:
        state.increment_stat("deaths")
        dead_timer=1.0
        if player.has_method("play_death"):player.play_death()
        get_tree().create_timer(dead_timer).timeout.connect(_respawn_player)

func _respawn_player()->void:
    if not is_instance_valid(player):return
    var h:=state.get_hunter()
    h["hp"]=h["max_hp"]
    player.position=Vector3(0,0.1,6)
    if player.has_method("respawn"):player.respawn()
    dead_timer=0.0

func _clear_combat()->void:
    for e in enemies:
        if is_instance_valid(e):e.queue_free()
    enemies.clear()
    if boss and is_instance_valid(boss):boss.queue_free()
    boss=null
    for p in shots:
        if is_instance_valid(p):p.queue_free()
    shots.clear()

func _story()->void:
    var line:=story.next_line()
    if cutscene and is_instance_valid(cutscene):
        cutscene.show_line(story.chapter_title(),str(line["character"]),str(line["text"]))
    hud.set_mode("HISTÓRIA • "+str(line["character"]))

func _wardrobe()->void:
    if content.STYLES.is_empty():return
    var unlocked:Array=state.data["wardrobe"]["unlocked"]
    var next_index:=unlocked.size()%content.STYLES.size()
    var next_id:=str(content.STYLES[next_index]["id"])
    if next_id not in unlocked:
        unlocked.append(next_id)
    wardrobe.select(next_id)
    _set_player()
    hud.set_mode("ARMÁRIO • %d/%d ESTILOS"%[unlocked.size(),content.STYLES.size()])

func _core(kind:String)->float:
    return cores.bonus(kind)+float(skill_tree.bonus(kind))

func _process(delta:float)->void:
    if cutscene and cutscene.is_active():return
    if activity_remaining>0.0:
        activity_remaining=maxf(0.0,activity_remaining-delta)
        if activity_remaining<=0.0:
            if mode=="EXPEDITION":
                hud.set_mode("EXPEDIÇÃO ENCERRADA • %d/%d"%[activity_progress,activity_goal])
            elif mode=="VERSUS":
                hud.set_mode("VERSUS ENCERRADO • %d/%d"%[activity_progress,activity_goal])
    clock=fmod(clock+delta,600.0)
    state.data["world"]["day_clock"]=clock
    invulnerability=maxf(0.0,invulnerability-delta)
    damage_buff_time=maxf(0.0,damage_buff_time-delta)
    if damage_buff_time<=0.0:damage_buff=0.0
    events.tick(delta)
    director.tick(delta)
    state.data["statistics"]["play_seconds"]=float(state.data["statistics"].get("play_seconds",0.0))+delta

    var h:=state.get_hunter()
    h["energy"]=minf(float(h["max_energy"]),float(h["energy"])+delta*4.5)

    var v:Vector2=hud.move_vector if hud and hud.move_vector.length_squared()>0.01 else Input.get_vector("move_left","move_right","move_forward","move_back")
    if v.length_squared()>0.001 and dead_timer<=0.0:
        v=v.normalized()
        var step:=Vector3(v.x,0,v.y)*6.2*delta
        var next:=player.position+step
        if mode=="EXPEDITION" and maps.blocked(str(portal_service.destination),next,0.62):
            next=player.position
        player.position=next
        player.position.x=clampf(player.position.x,-25,25)
        player.position.z=clampf(player.position.z,-25,25)
    if player.has_method("set_locomotion"):player.set_locomotion(v)
    if dead_timer<=0.0 and player.has_method("play_run") and v.length_squared()>0.001:
        player.play_run()
    elif dead_timer<=0.0 and player.has_method("play_idle") and v.length_squared()<=0.001:
        player.play_idle()

    var camera_target:=player.position+Vector3(0,14,17)
    camera.position=camera.position.lerp(camera_target,1.0-exp(-10.0*delta))
    camera.look_at(player.position+Vector3(0,1,0),Vector3.UP)

    if boss and is_instance_valid(boss):
        hud.set_boss(str(boss.boss_stats.get("name","CHEFE")),boss.get_hp_ratio(),int(boss.phase))
    elif hud:
        hud.clear_boss()

    for i in range(shots.size()-1,-1,-1):
        var p:=shots[i]
        if not is_instance_valid(p):
            shots.remove_at(i)
            continue
        p.position+=Vector3(p.get_meta("v"))*delta
        if p.position.distance_to(player.position)<0.9:
            take_enemy_damage(float(p.get_meta("d")))
            p.queue_free()
            shots.remove_at(i)

    if world.sun:
        var wave:=sin(clock/600.0*TAU)*0.5+0.5
        world.sun.rotation_degrees.x=lerpf(-66,-12,wave)
        world.sun.light_energy=lerpf(0.35,1.25,wave)

func _notification(what:int)->void:
    if what==NOTIFICATION_WM_CLOSE_REQUEST:
        save.force_save_and_flush()
        get_tree().quit()

func _apply_quality(profile:Dictionary)->void:
    if perf and get_viewport():
        perf.apply(get_viewport(),world)
    if vfx and "max_pool" in vfx:
        vfx.max_pool=12 if str(profile.get("name",""))=="LOW" else (18 if str(profile.get("name",""))=="MEDIUM" else 24)
