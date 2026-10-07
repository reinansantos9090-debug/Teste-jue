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

var state:Node
var save:Node
var content:Node
var perf:Node
var world:Node
var quests:Node
var story:Node
var events:Node
var inventory:Node
var combat:Node
var audio:Node
var hud:CanvasLayer
var player:Node3D
var camera:Camera3D
var enemies:Array[Node3D]=[]
var boss:Node3D
var shots:Array[Node3D]=[]
var mode:="HQ"
var attack_ready:=true
var skills=[true,true,true]
var clock:=0.0

func _ready()->void:
    state=StateScript.new();add_child(state)
    content=ContentScript.new();add_child(content)
    save=SaveScript.new();add_child(save);save.setup(state);save.load_now()
    perf=PerfScript.new();add_child(perf)
    world=WorldScript.new();add_child(world);world.setup(self,content)
    quests=QuestScript.new();add_child(quests);quests.setup(state,content)
    story=StoryScript.new();add_child(story);story.setup(state)
    events=EventScript.new();add_child(events);events.setup(state,content)
    inventory=InventoryScript.new();add_child(inventory);inventory.setup(state)
    audio=AudioScript.new();add_child(audio)
    combat=CombatScript.new();add_child(combat);combat.setup(state,audio)
    player=Node3D.new();player.name="Hunter";add_child(player)
    _set_player()
    camera=Camera3D.new();camera.fov=48.0;add_child(camera);camera.current=true
    hud=HudScript.new();add_child(hud);hud.setup(state)
    hud.attack_pressed.connect(_attack)
    hud.ability_pressed.connect(_ability)
    hud.portal_pressed.connect(_portal)
    hud.inventory_pressed.connect(func():hud.set_mode("INVENTÁRIO • "+inventory.summary()))
    hud.story_pressed.connect(_story)
    hud.wardrobe_pressed.connect(_wardrobe)
    world.build_hq()
    quests.start("story_01")

func _set_player()->void:
    player.position=Vector3(0,0.1,6)
    for c in player.get_children():c.queue_free()
    var s=content.STYLES[0]
    for item in content.STYLES:
        if item["id"]==str(state.data["wardrobe"]["style"]):s=item;break
    Factory.make_hunter(player,s["primary"],s["secondary"],s["head"])

func _portal()->void:
    audio.click()
    if mode=="HQ":
        mode="EXPEDITION";world.build_biome("verdant_frontier");_spawn_wave(12);hud.set_mode("EXPEDIÇÃO • FRONTEIRA ESMERALDA")
    elif mode=="EXPEDITION":
        mode="RIFT";world.build_biome("crystal_forest");_spawn_wave(14);_spawn_boss("overload_titan");hud.set_mode("RIFT • DOMO DA FENDA")
    else:
        mode="HQ";_clear_combat();world.build_hq();hud.set_mode("QG NO TERRAÇO • PORTAL CENTRAL")

func _attack()->void:
    if not attack_ready:return
    attack_ready=false
    var w=content.WEAPONS.get(str(state.data["loadout"]["weapon"]),content.WEAPONS["volt_blades"])
    var target=_nearest(float(w["range"])+1.8)
    if target:
        var amount=combat.player_damage(w,int(state.get_hunter()["level"]),_core("damage"),_core("crit"))
        combat.hit(target,amount)
        combat.spawn_impact(self,target.position+Vector3(0,1,0))
    get_tree().create_timer(float(w["rate"])*(1.0-_core("cooldown"))).timeout.connect(func():attack_ready=true)

func _ability(i:int)->void:
    if not skills[i]:return
    var ids=[str(state.data["loadout"]["ability_1"]),str(state.data["loadout"]["ability_2"]),str(state.data["loadout"]["ability_3"])]
    var a=content.ABILITIES.get(ids[i],content.ABILITIES["shock_dash"])
    var h=state.get_hunter()
    if float(h["energy"])<float(a["energy"]):return
    skills[i]=false;h["energy"]=float(h["energy"])-float(a["energy"])
    if i==0:
        var n=_nearest(12.0)
        if n:player.position=n.position+Vector3(0,0,1.4);combat.hit(n,50.0+float(h["level"])*4.0)
    elif i==1:
        for e in enemies.duplicate():
            if is_instance_valid(e) and e.position.distance_to(player.position)<5.5:combat.hit(e,72.0+float(h["level"])*5.0)
    else:h["hp"]=minf(float(h["max_hp"]),float(h["hp"])+42.0)
    get_tree().create_timer(float(a["cooldown"])*(1.0-_core("cooldown"))).timeout.connect(func():skills[i]=true)

func _spawn_wave(count:int)->void:
    _clear_combat()
    for i in mini(count,perf.max_enemies()):_spawn_enemy(i)

func _spawn_enemy(i:int,id:="")->void:
    var pool=["slime","slime","stalker","chaos_moth","rift_hound","scrap_golem"]
    var kind=id if not id.is_empty() else pool[i%pool.size()]
    var e:=EnemyScript.new();add_child(e);e.position=player.position+Vector3(cos(i*0.9),0,sin(i*0.9))*float(8+i%4*2)
    e.configure(kind,player,content)
    if kind=="slime":e.attach_visual(Factory.make_slime(e,Color("#43e97c")))
    elif kind=="scrap_golem":e.attach_visual(Factory.make_golem(e,Color("#c97959")))
    else:e.attach_visual(Factory.make_crystal_monster(e,Color("#718bff")))
    e.defeated.connect(_enemy_down);enemies.append(e)

func _spawn_boss(id:String)->void:
    var b:=BossScript.new();add_child(b);b.position=player.position+Vector3(0,0,-10)
    b.configure_boss(id,player,content);b.attach_visual(Factory.make_golem(b,Color("#7d5ce8"),Color("#ffd86b")))
    b.phase_changed.connect(func(p:int):hud.set_mode("CHEFE • FASE %d" % p))
    b.summon_requested.connect(func(k:String,n:int):for j in n:_spawn_enemy(enemies.size()+j,k))
    b.projectile_requested.connect(_shot)
    b.defeated.connect(_boss_down);boss=b

func _shot(o:Vector3,t:Vector3,d:float)->void:
    var p:=Node3D.new();add_child(p);p.position=o;Factory.sphere(p,0.20,Vector3.ZERO,Color("#ff79db"))
    p.set_meta("v",(t-o).normalized()*8.0);p.set_meta("d",d);shots.append(p)

func _enemy_down(e:Node3D,x:int)->void:
    state.increment_stat("kills");state.add_xp(x);state.add_item("aether_core",1)
    quests.add_progress("hunt_01");quests.add_progress("hunt_02");quests.add_progress("hunt_03");e.queue_free()

func _boss_down(b:Node3D,x:int)->void:
    state.increment_stat("bosses");state.add_xp(x);state.data["world"]["completed_rifts"]+=1;quests.add_progress("boss_01")
    b.queue_free();boss=null;hud.set_mode("RIFT CONCLUÍDO • RECOMPENSAS");audio.level_up()

func _nearest(r:float)->Node3D:
    var best:Node3D=null;var d=r
    for e in enemies:
        if is_instance_valid(e):
            var x=e.position.distance_to(player.position)
            if x<d:d=x;best=e
    if boss and is_instance_valid(boss) and boss.position.distance_to(player.position)<d:best=boss
    return best

func take_enemy_damage(d:float)->void:
    var h=state.get_hunter()
    if randf()<_core("dodge"):return
    h["hp"]=maxf(0.0,float(h["hp"])-d)
    if float(h["hp"])<=0.0:
        state.increment_stat("deaths");h["hp"]=h["max_hp"];player.position=Vector3(0,0.1,6)

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
    var line=story.next_line();hud.set_mode("HISTÓRIA • "+str(line["character"])+": "+str(line["text"]))
    if int(state.data["story"]["scene"])>=story.current_chapter()["lines"].size():state.set_story_flag("tutorial_complete",true);quests.start("hunt_01")

func _wardrobe()->void:
    var unlocked:Array=state.data["wardrobe"]["unlocked"]
    var next=content.STYLES[unlocked.size()%content.STYLES.size()]["id"]
    if next not in unlocked:unlocked.append(next);state.data["wardrobe"]["style"]=next;_set_player()
    hud.set_mode("ARMÁRIO • %d/%d ESTILOS" % [unlocked.size(),content.STYLES.size()])

func _core(kind:String)->float:
    var total:=0.0
    for id in state.data["cores"]["equipped"]:
        var c=content.CORES.get(str(id),{})
        if c.get("type","")==kind:total+=float(c.get("value",0.0))
    return total

func _process(delta:float)->void:
    clock=fmod(clock+delta,600.0);state.data["world"]["day_clock"]=clock
    state.increment_stat("play_seconds",delta)
    var h=state.get_hunter();h["energy"]=minf(float(h["max_energy"]),float(h["energy"])+delta*4.5)
    var v=Input.get_vector("move_left","move_right","move_forward","move_back")
    if v.length_squared()>0.001:
        v=v.normalized();player.position+=Vector3(v.x,0,v.y)*6.2*delta
        player.position.x=clampf(player.position.x,-25,25);player.position.z=clampf(player.position.z,-25,25)
    camera.position=player.position+Vector3(0,14,17);camera.look_at(player.position+Vector3(0,1,0),Vector3.UP)
    for i in range(shots.size()-1,-1,-1):
        var p=shots[i]
        if not is_instance_valid(p):shots.remove_at(i);continue
        p.position+=Vector3(p.get_meta("v"))*delta
        if p.position.distance_to(player.position)<0.9:take_enemy_damage(float(p.get_meta("d")));p.queue_free();shots.remove_at(i)
    if world.sun:
        var wave=sin(clock/600.0*TAU)*0.5+0.5
        world.sun.rotation_degrees.x=lerpf(-66,-12,wave);world.sun.light_energy=lerpf(0.35,1.25,wave)

func _notification(what:int)->void:
    if what==NOTIFICATION_WM_CLOSE_REQUEST:save.force_save_and_flush();get_tree().quit()
