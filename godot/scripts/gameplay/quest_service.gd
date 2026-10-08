extends Node
signal updated
signal completed(quest_id:String)

var game_state:Node
var database:Node

func setup(state:Node,content:Node)->void:
    game_state=state
    database=content

func start(id:String)->void:
    if get_quest(id).is_empty():return
    var active:Array=game_state.data["quests"]["active"]
    if id not in active and id not in game_state.data["quests"]["completed"]:
        active.append(id)
        game_state.data["quests"]["progress"][id]=0
        game_state.quest_changed.emit()
        updated.emit()

func add_progress(id:String,amount:=1)->void:
    if id not in game_state.data["quests"]["active"]:return
    var value:=int(game_state.data["quests"]["progress"].get(id,0))+amount
    game_state.data["quests"]["progress"][id]=value
    var quest:=get_quest(id)
    if value>=int(quest.get("target",1)):complete(id)
    else:
        game_state.quest_changed.emit()
        updated.emit()

func add_kill(monster_id:String,amount:=1)->void:
    for id in game_state.data["quests"]["active"].duplicate():
        var q:=get_quest(str(id))
        if q.get("type","")=="kill" and str(q.get("monster",""))==monster_id:add_progress(str(id),amount)
        elif q.get("type","")=="boss" and monster_id.begins_with("boss_"):add_progress(str(id),amount)
        elif q.get("type","")=="boss_any" and monster_id.begins_with("boss_"):add_progress(str(id),amount)
        elif q.get("type","")=="map_kill":add_progress(str(id),amount)
        
func add_collect(item_id:String,amount:=1)->void:
    for id in game_state.data["quests"]["active"].duplicate():
        var q:=get_quest(str(id))
        if q.get("type","")=="collect" and str(q.get("item",""))==item_id:add_progress(str(id),amount)

func add_map(map_id:String)->void:
    for id in game_state.data["quests"]["active"].duplicate():
        var q:=get_quest(str(id))
        if q.get("type","")=="map" and str(q.get("map",""))==map_id:add_progress(str(id))
        
func add_boss(boss_id:String)->void:
    for id in game_state.data["quests"]["active"].duplicate():
        var q:=get_quest(str(id))
        if q.get("type","")=="boss_any":add_progress(str(id))
        elif q.get("type","")=="boss" and str(q.get("boss",""))==boss_id:add_progress(str(id))

func add_rift()->void:
    for id in game_state.data["quests"]["active"].duplicate():
        var q:=get_quest(str(id))
        if q.get("type","")=="rift":add_progress(str(id))

func complete(id:String)->void:
    var active:Array=game_state.data["quests"]["active"]
    var completed_ids:Array=game_state.data["quests"]["completed"]
    active.erase(id)
    if id in completed_ids:return
    completed_ids.append(id)
    var quest:=get_quest(id)
    game_state.add_xp(int(quest.get("xp",0)))
    var rewards:Dictionary=quest.get("rewards",{})
    for item in rewards.keys():game_state.add_item(str(item),int(rewards[item]))
    var next_id:=str(quest.get("next",""))
    if not next_id.is_empty():start(next_id)
    self.completed.emit(id)
    game_state.quest_changed.emit()
    updated.emit()

func get_quest(id:String)->Dictionary:
    if database==null:return {}
    for quest in database.QUESTS:
        if str(quest["id"])==id:return quest
    return {}

func active_summary()->String:
    var active:Array=game_state.data["quests"]["active"]
    if active.is_empty():return "Nenhum contrato ativo"
    var id:=str(active[0])
    var quest:=get_quest(id)
    var value:=int(game_state.data["quests"]["progress"].get(id,0))
    return "%s  %d/%d"%[quest.get("title",id),value,int(quest.get("target",1))]
