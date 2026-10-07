extends Node
signal changed
var state:Node
var database:Node

func setup(game_state:Node,content:Node)->void:
    state=game_state
    database=content

func unlock_next()->String:
    var unlocked:Array=state.data["wardrobe"]["unlocked"]
    if unlocked.size()>=database.STYLES.size():return ""
    var id:=str(database.STYLES[unlocked.size()]["id"])
    unlocked.append(id)
    state.data["wardrobe"]["style"]=id
    changed.emit()
    state.state_changed.emit()
    return id

func select(id:String)->bool:
    if id not in state.data["wardrobe"]["unlocked"]:return false
    state.data["wardrobe"]["style"]=id
    changed.emit()
    state.state_changed.emit()
    return true

func count()->int:
    return state.data["wardrobe"]["unlocked"].size()
