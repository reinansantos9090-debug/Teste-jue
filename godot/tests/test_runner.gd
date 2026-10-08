extends SceneTree
## Headless regression suite for the offline gameplay core.
## Exit code 0 means all assertions passed.

const StateScript=preload("res://scripts/core/game_state.gd")
const CombatScript=preload("res://scripts/gameplay/combat_service.gd")
const LootScript=preload("res://scripts/gameplay/loot_service.gd")
const ContentScript=preload("res://scripts/core/content_database.gd")
const SaveScript=preload("res://scripts/core/save_service.gd")

var failures:Array[String]=[]

func _init()->void:
    _test_xp()
    _test_damage()
    _test_loot()
    _test_save()
    if failures.is_empty():
        print("AETHERIA_TESTS: PASS")
        quit(0)
    else:
        for item in failures:push_error(item)
        print("AETHERIA_TESTS: FAIL %d"%failures.size())
        quit(1)

func _expect(condition:bool,message:String)->void:
    if not condition:failures.append(message)

func _test_xp()->void:
    var state=StateScript.new()
    state.reset()
    var before=int(state.data["hunter"]["level"])
    state.add_xp(120)
    _expect(int(state.data["hunter"]["level"])==before+1,"XP: level did not advance")
    _expect(int(state.data["progression"]["skill_points"])==1,"XP: level did not award skill point")

func _test_damage()->void:
    var state=StateScript.new()
    state.reset()
    var combat=CombatScript.new()
    combat.setup(state,null)
    var damage:=combat.player_damage({"damage":100.0},1,0.0,0.0)
    _expect(damage>=100.0 and damage<=175.0,"Damage: formula outside expected range")
    
func _test_loot()->void:
    var state=StateScript.new()
    state.reset()
    var content=ContentScript.new()
    var loot=LootScript.new()
    loot.setup(state,content)
    var drop:Dictionary=loot.roll("aether_slime","verdant_frontier",1,1.0)
    _expect(not str(drop.get("rarity","")).is_empty(),"Loot: missing rarity")
    _expect(int(drop.get("gold",0))>0,"Loot: missing gold")
    _expect(not drop.get("items",{}).is_empty(),"Loot: missing item")

func _test_save()->void:
    var state=StateScript.new()
    state.reset()
    state.data["hunter"]["gold"]=9876
    var save=SaveScript.new()
    save.setup(state)
    _expect(save.save_now(),"Save: could not write save")
    state.data["hunter"]["gold"]=1
    _expect(save.load_now(),"Save: could not reload save")
    _expect(int(state.data["hunter"]["gold"])==9876,"Save: persisted value mismatch")
