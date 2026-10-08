extends Node
## Deterministic local loot resolver. Rarity changes the quantity/value of
## actual drops, and the result is immediately consumed by InventoryService.

var state:Node
var content:Node
var rng:=RandomNumberGenerator.new()

const RARITIES=[
    {"id":"common","name":"Comum","multiplier":1.0,"chance":0.58},
    {"id":"uncommon","name":"Incomum","multiplier":1.25,"chance":0.27},
    {"id":"rare","name":"Raro","multiplier":1.60,"chance":0.11},
    {"id":"epic","name":"Épico","multiplier":2.20,"chance":0.035},
    {"id":"legendary","name":"Lendário","multiplier":3.20,"chance":0.005}
]

func setup(game_state:Node,content_db:Node)->void:
    state=game_state
    content=content_db
    rng.seed=73191

func rarity()->Dictionary:
    var roll:=rng.randf()
    var cursor:=0.0
    for item in RARITIES:
        cursor+=float(item["chance"])
        if roll<=cursor:return item
    return RARITIES[0]

func roll(monster_id:String,map_id:String,level:int,event_multiplier:float=1.0)->Dictionary:
    var monster:Dictionary=content.MONSTERS.get(monster_id,{})
    var picked:Dictionary=rarity()
    var mult:=float(picked["multiplier"])*event_multiplier*(1.0+float(maxi(0,level-1))*0.015)
    var result:Dictionary={"rarity":str(picked["id"]),"rarity_name":str(picked["name"]),"gold":0,"items":{},"monster":monster_id,"map":map_id}
    result["gold"]=maxi(1,roundi((8.0+float(monster.get("tier",1))*6.0)*mult))
    var table:Array=monster.get("loot",[])
    if table.is_empty():table=["aether_core"]
    var drop_count:=2 if str(picked["id"]) in ["epic","legendary"] else 1
    for i in range(drop_count):
        var item_id:=str(table[rng.randi_range(0,table.size()-1)])
        var amount:=1
        if str(picked["id"])=="rare" and rng.randf()<0.35:amount=2
        if str(picked["id"])=="epic":amount=2
        if str(picked["id"])=="legendary":amount=3
        result["items"][item_id]=int(result["items"].get(item_id,0))+amount
    return result

func describe(drop:Dictionary)->String:
    var parts:Array[String]=[]
    for id in drop.get("items",{}).keys():
        parts.append("%s x%d"%[str(id),int(drop["items"][id])])
    return "%s • %s • %d ouro"%[str(drop.get("rarity_name","Comum")),", ".join(parts),int(drop.get("gold",0))]
