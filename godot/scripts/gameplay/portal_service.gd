extends Node
signal mode_changed(mode:String)
var state:Node
var mode:="HQ"
var destination:=""
var expedition_count:=0
var rift_count:=0

func setup(game_state:Node)->void:
    state=game_state

func enter_expedition(biome:String)->void:
    mode="EXPEDITION"
    destination=biome
    expedition_count+=1
    mode_changed.emit(mode)

func enter_rift(arena:String)->void:
    mode="RIFT"
    destination=arena
    rift_count+=1
    mode_changed.emit(mode)

func enter_hq()->void:
    mode="HQ"
    destination="rooftop"
    mode_changed.emit(mode)

func enter_versus()->void:
    mode="VERSUS"
    destination="training_simulation"
    mode_changed.emit(mode)
