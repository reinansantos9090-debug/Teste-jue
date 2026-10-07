extends Node
signal activity_changed(title:String)

var state:Node
var content:Node
var portal_mode:="HQ"
var destination:=""
var active_event:=""
var event_time:=0.0
var sprint_kills:=0
var daily_kills:=0

func setup(game_state:Node,content_db:Node)->void:
    state=game_state
    content=content_db

func enter_hq()->void:
    portal_mode="HQ"
    destination="rooftop"
    activity_changed.emit("QG NO TERRAÇO")

func start_expedition(biome:String)->void:
    portal_mode="EXPEDITION"
    destination=biome
    state.data["world"]["completed_expeditions"]+=1
    activity_changed.emit("EXPEDIÇÃO • "+biome)

func start_rift(arena:String)->void:
    portal_mode="RIFT"
    destination=arena
    activity_changed.emit("RIFT • "+arena)

func start_versus()->void:
    portal_mode="VERSUS"
    destination="training_simulation"
    activity_changed.emit("VERSUS • SIMULAÇÃO OFFLINE")

func start_event(id:String)->bool:
    for event in content.EVENTS:
        if event["id"]==id:
            active_event=id
            event_time=float(event["duration"])
            activity_changed.emit("EVENTO • "+str(event["name"]))
            return true
    return false

func tick(delta:float)->void:
    if event_time>0.0:
        event_time=maxf(0.0,event_time-delta)
        if event_time==0.0:
            active_event=""
            activity_changed.emit("EVENTO ENCERRADO")

func event_multiplier()->float:
    for event in content.EVENTS:
        if event["id"]==active_event:
            return float(event["multiplier"])
    return 1.0

func record_kill()->void:
    sprint_kills+=1
    daily_kills+=1
    state.data["statistics"]["kills"]=int(state.data["statistics"]["kills"])
    if daily_kills>=10:
        state.data["hunter"]["chaos_energy"]+=100

func record_rift()->void:
    state.data["world"]["completed_rifts"]+=1
    state.data["hunter"]["chaos_energy"]+=160

func unlock_style()->String:
    var unlocked:Array=state.data["wardrobe"]["unlocked"]
    if unlocked.size()>=content.STYLES.size():return ""
    var id:=str(content.STYLES[unlocked.size()]["id"])
    unlocked.append(id)
    state.data["wardrobe"]["style"]=id
    state.state_changed.emit()
    return id

func craft(recipe_id:String)->bool:
    for recipe in content.RECIPES:
        if recipe["id"]==recipe_id:
            for item in recipe["requires"]:
                if int(state.data["inventory"].get(item,0))<int(recipe["requires"][item]):
                    return false
            for item in recipe["requires"]:
                state.remove_item(item,int(recipe["requires"][item]))
            for item in recipe["result"]:
                state.add_item(item,int(recipe["result"][item]))
            return true
    return false

func history_title()->String:
    return "CAPÍTULO %d • %s" % [int(state.data["story"]["chapter"]), "AETHERIA"]
