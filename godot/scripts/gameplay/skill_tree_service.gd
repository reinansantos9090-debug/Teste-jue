extends Node
## Small meaningful skill tree used by progression. Nodes alter runtime combat
## multipliers through the same bonus() API consumed by GameRoot.

var state:Node
var content:Node

const NODES=[
    {"id":"power_core","name":"Núcleo de Potência","level":2,"requires":[],"kind":"damage","value":0.08},
    {"id":"swift_hands","name":"Mãos Rápidas","level":3,"requires":["power_core"],"kind":"attack_speed","value":0.07},
    {"id":"critical_eye","name":"Olho Crítico","level":4,"requires":["power_core"],"kind":"crit","value":0.08},
    {"id":"aether_skin","name":"Pele de Aether","level":5,"requires":["power_core"],"kind":"max_health","value":0.10},
    {"id":"field_medic","name":"Médico de Campo","level":6,"requires":["aether_skin"],"kind":"healing","value":0.12},
    {"id":"phase_step","name":"Passo Fásico","level":7,"requires":["swift_hands"],"kind":"dodge","value":0.06},
    {"id":"coolant","name":"Circuito Frio","level":8,"requires":["swift_hands"],"kind":"cooldown","value":0.08},
    {"id":"hunter_instinct","name":"Instinto de Caça","level":10,"requires":["critical_eye"],"kind":"damage","value":0.12},
    {"id":"rift_mastery","name":"Maestria das Fendas","level":12,"requires":["phase_step","coolant"],"kind":"damage","value":0.16},
    {"id":"echo_guard","name":"Guarda do Eco","level":15,"requires":["hunter_instinct","field_medic"],"kind":"max_health","value":0.15}
]

func setup(game_state:Node,content_db:Node)->void:
    state=game_state
    content=content_db
    state.data["progression"].get_or_add("skill_points",0)
    state.data["progression"].get_or_add("skills",[])

func node(id:String)->Dictionary:
    for item in NODES:
        if item["id"]==id:return item
    return {}

func can_unlock(id:String)->bool:
    var item:=node(id)
    if item.is_empty():return false
    var h:Dictionary=state.data["hunter"]
    var skills:Array=state.data["progression"]["skills"]
    if id in skills:return false
    if int(h["level"])<int(item["level"]):return false
    if int(state.data["progression"]["skill_points"])<1:return false
    for req in item["requires"]:
        if req not in skills:return false
    return true

func unlock(id:String)->bool:
    if not can_unlock(id):return false
    state.data["progression"]["skill_points"]=int(state.data["progression"]["skill_points"])-1
    state.data["progression"]["skills"].append(id)
    state.state_changed.emit()
    return true

func unlock_next_available()->String:
    for item in NODES:
        if can_unlock(str(item["id"])):
            unlock(str(item["id"]))
            return str(item["name"])
    return "nenhum novo nó disponível"

func bonus(kind:String)->float:
    var total:=0.0
    for id in state.data["progression"]["skills"]:
        var item:=node(str(id))
        if item.get("kind","")==kind:total+=float(item.get("value",0.0))
    return total

func summary()->String:
    return "%d/%d nós • %d pontos"%[
        state.data["progression"]["skills"].size(),NODES.size(),
        int(state.data["progression"]["skill_points"])
    ]
