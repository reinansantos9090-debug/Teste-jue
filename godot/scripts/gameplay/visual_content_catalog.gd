extends Node
## Runtime-scalable content catalog.
## Uses deterministic generation to provide many original loadout variants
## without loading a giant scene graph at startup.

const WEAPON_ARCHETYPES := [
    ["volt","Lâminas Voltáicas",24.0,0.24,3.2,"melee"],
    ["pulse","Canhão Pulsar",46.0,0.72,18.0,"ranged"],
    ["toxic","Arco Tóxico",34.0,0.55,15.0,"ranged"],
    ["hammer","Martelo Sucateiro",58.0,0.88,3.6,"melee"],
    ["lance","Lança de Aether",31.0,0.35,5.5,"melee"],
    ["orb","Orbes Orbitais",21.0,0.18,10.0,"ranged"],
    ["frost","Lançador Glacial",39.0,0.42,13.0,"ranged"],
    ["ember","Foices Ígneas",44.0,0.36,4.0,"melee"],
    ["storm","Arco de Trovão",36.0,0.48,16.0,"ranged"],
    ["gravity","Matriz Gravitacional",52.0,0.66,11.0,"ranged"],
    ["venom","Lâmina Veneno-Nova",41.0,0.30,4.2,"melee"],
    ["prism","Prisma Radiante",33.0,0.38,14.0,"ranged"]
]

const PREFIXES := [
    "Aether","Neon","Solar","Rift","Verdant","Crystal","Nova","Echo","Storm","Twilight",
    "Quantum","Titan","Arc","Pulse","Prism","Sky","Ember","Frost","Vortex","Lumen"
]

const SUFFIXES := [
    "Prime","MK-II","Horizon","Catalyst","Runner","Breaker","Sentry","Bloom","Edge","Core"
]

const ABILITY_FAMILIES := [
    ["dash","Impulso Relâmpago",4.0,20.0],
    ["burst","Pulso Aether",7.0,34.0],
    ["drone","Nanodrone",12.0,40.0],
    ["gravity","Poço Gravitacional",9.0,45.0],
    ["meteor","Chuva Meteórica",10.0,55.0],
    ["overdrive","Sobrecarga",18.0,70.0],
    ["barrier","Barreira Prisma",14.0,48.0],
    ["blink","Salto de Fenda",8.0,32.0],
    ["chain","Corrente Elétrica",11.0,50.0],
    ["nova","Nova de Energia",15.0,62.0]
]

const ELEMENTS := ["Aether","Neon","Fogo","Gelo","Trovão","Veneno","Gravidade","Cristal"]

var weapons: Dictionary = {}
var abilities: Dictionary = {}

func _ready() -> void:
    _build_weapons()
    _build_abilities()

func _build_weapons() -> void:
    weapons.clear()
    for archetype in WEAPON_ARCHETYPES:
        var code := str(archetype[0])
        weapons[code] = {
            "name": str(archetype[1]),
            "damage": float(archetype[2]),
            "rate": float(archetype[3]),
            "range": float(archetype[4]),
            "type": str(archetype[5]),
            "element": "Aether",
            "variant": 0
        }
    var index := 1
    for prefix in PREFIXES:
        for suffix in SUFFIXES:
            var base = WEAPON_ARCHETYPES[(index-1) % WEAPON_ARCHETYPES.size()]
            var archetype_code := str(base[0])
            var id := "weapon_%03d" % index
            var multiplier := 0.90 + float(index % 13) * 0.025
            weapons[id] = {
                "name": "%s %s %s" % [prefix, str(base[1]), suffix],
                "damage": snappedf(float(base[2]) * multiplier,0.1),
                "rate": maxf(0.12,float(base[3]) * (1.03 - float(index%7)*0.018)),
                "range": float(base[4]) + float(index%4)*0.55,
                "type": str(base[5]),
                "element": ELEMENTS[index % ELEMENTS.size()],
                "variant": index
            }
            index += 1

func _build_abilities() -> void:
    abilities.clear()
    for family in ABILITY_FAMILIES:
        var code := str(family[0])
        abilities[code] = {
            "name":str(family[1]),
            "cooldown":float(family[2]),
            "energy":float(family[3]),
            "power":1.0,
            "element":"Aether"
        }
    var index := 1
    for element in ELEMENTS:
        for family in ABILITY_FAMILIES:
            var id := "ability_%03d" % index
            abilities[id] = {
                "name":"%s %s" % [element,str(family[1])],
                "cooldown":maxf(3.0,float(family[2]) - float(index%5)*0.15),
                "energy":float(family[3]) + float(index%4)*2.0,
                "power":1.0 + float(index%11)*0.045,
                "element":element
            }
            index += 1

func weapon(id:String) -> Dictionary:
    return weapons.get(id,weapons["volt"])

func ability(id:String) -> Dictionary:
    return abilities.get(id,abilities["dash"])

func weapon_count() -> int:
    return weapons.size()

func ability_count() -> int:
    return abilities.size()
