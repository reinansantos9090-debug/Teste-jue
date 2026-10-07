extends Node
signal changed
var state:Node
var database:Node

func setup(game_state:Node,content:Node)->void:
    state=game_state
    database=content

func bonus(kind:String)->float:
    var total:=0.0
    for id in state.data["cores"]["equipped"]:
        var c:Dictionary=database.CORES.get(str(id),{})
        if c.get("type","")==kind:total+=float(c.get("value",0.0))
    return total

func equip(id:String)->bool:
    var owned:Array=state.data["cores"]["owned"]
    if id not in owned:return false
    var equipped:Array=state.data["cores"]["equipped"]
    if id not in equipped:
        if equipped.size()>=3:equipped.pop_front()
        equipped.append(id)
    changed.emit();state.state_changed.emit()
    return true

func fuse(first:String,second:String)->String:
    if first==second:return ""
    var owned:Array=state.data["cores"]["owned"]
    if first not in owned or second not in owned:return ""
    var a:Dictionary=database.CORES.get(first,{})
    var b:Dictionary=database.CORES.get(second,{})
    if a.get("type","")!=b.get("type",""):return ""
    owned.erase(first);owned.erase(second)
    var next:=str(a.get("type","core"))+"_2"
    if next not in owned:owned.append(next)
    changed.emit();state.state_changed.emit()
    return next
