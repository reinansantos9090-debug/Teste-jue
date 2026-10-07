extends Node
signal goal_completed(id:String)
var state:Node
var goals=[
    {"id":"daily_kills","name":"Caçada Diária","target":10,"reward":80},
    {"id":"daily_rift","name":"Explorador de Fendas","target":1,"reward":120},
    {"id":"daily_skill","name":"Domínio de Habilidades","target":8,"reward":100}
]

func setup(game_state:Node)->void:
    state=game_state
    for g in goals:
        state.data["quests"]["progress"].get_or_add(g["id"],0)

func add(id:String,amount:=1)->void:
    var value:=int(state.data["quests"]["progress"].get(id,0))+amount
    state.data["quests"]["progress"][id]=value
    for g in goals:
        if g["id"]==id and value==int(g["target"]):
            state.add_xp(int(g["reward"]));goal_completed.emit(id);state.state_changed.emit()

func summary()->String:
    var g=goals[0]
    return "%s %d/%d"%[g["name"],int(state.data["quests"]["progress"].get(g["id"],0)),int(g["target"])]
