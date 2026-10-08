extends Node
signal changed

var game_state:Node

func setup(state:Node)->void:
    game_state=state
    if game_state and not game_state.inventory_changed.is_connected(_emit):
        game_state.inventory_changed.connect(_emit)

func _emit()->void:
    changed.emit()

func quantity(id:String)->int:
    if game_state==null:return 0
    return int(game_state.get_inventory().get(id,0))

func has_cost(cost:Dictionary)->bool:
    for item in cost.keys():
        if quantity(str(item))<int(cost[item]):return false
    return true

func craft(recipe:Dictionary)->bool:
    var cost:Dictionary=recipe.get("requires",{})
    if not has_cost(cost):return false
    for item in cost.keys():
        game_state.remove_item(str(item),int(cost[item]))
    for item in recipe.get("result",{}).keys():
        game_state.add_item(str(item),int(recipe["result"][item]))
    return true

func craft_by_id(id:String,database:Node)->bool:
    for recipe in database.RECIPES:
        if str(recipe["id"])==id:return craft(recipe)
    return false

func record_loot(drop:Dictionary)->void:
    if game_state==null:return
    for item_id in drop.get("items",{}).keys():
        game_state.add_item(str(item_id),int(drop["items"][item_id]))
    game_state.data["hunter"]["gold"]=int(game_state.data["hunter"].get("gold",0))+int(drop.get("gold",0))
    var rarity:=str(drop.get("rarity","common"))
    var counts:Dictionary=game_state.data["loot"].get("rarity_counts",{})
    counts[rarity]=int(counts.get(rarity,0))+1
    game_state.data["loot"]["rarity_counts"]=counts
    var recent:Array=game_state.data["loot"].get("last",[])
    recent.push_front({
        "rarity":rarity,
        "rarity_name":str(drop.get("rarity_name","Comum")),
        "gold":int(drop.get("gold",0)),
        "items":drop.get("items",{}),
        "monster":str(drop.get("monster",""))
    })
    while recent.size()>12:recent.pop_back()
    game_state.data["loot"]["last"]=recent
    game_state.data["loot"]["total_gold"]=int(game_state.data["loot"].get("total_gold",0))+int(drop.get("gold",0))
    game_state.inventory_changed.emit()
    game_state.state_changed.emit()

func recent_loot()->Array:
    return game_state.data["loot"].get("last",[])

func summary()->String:
    if game_state==null:return ""
    var parts:Array[String]=[]
    for key in game_state.get_inventory().keys():
        var amount:=quantity(str(key))
        if amount>0:parts.append("%s x%d"%[str(key),amount])
    return ", ".join(parts)
