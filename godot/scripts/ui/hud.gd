extends CanvasLayer
signal attack_pressed
signal ability_pressed(index: int)
signal portal_pressed
signal inventory_pressed
signal story_pressed
signal wardrobe_pressed

var state: Node
var title_label: Label
var stats_label: Label
var quest_label: Label
var move_vector := Vector2.ZERO

func setup(game_state: Node) -> void:
    state = game_state
    _build()
    state.state_changed.connect(_refresh)
    state.quest_changed.connect(_refresh)
    _refresh()

func _build() -> void:
    var root := Control.new()
    root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    add_child(root)
    title_label = Label.new()
    title_label.position = Vector2(22,18)
    title_label.add_theme_font_size_override("font_size",24)
    root.add_child(title_label)
    stats_label = Label.new()
    stats_label.position = Vector2(22,58)
    root.add_child(stats_label)
    quest_label = Label.new()
    quest_label.position = Vector2(22,84)
    root.add_child(quest_label)
    _btn(root,"PORTAL",Vector2(760,24),Vector2(170,58)).pressed.connect(func(): portal_pressed.emit())
    _btn(root,"HISTÓRIA",Vector2(945,24),Vector2(170,58)).pressed.connect(func(): story_pressed.emit())
    _btn(root,"ARMÁRIO",Vector2(575,24),Vector2(170,58)).pressed.connect(func(): wardrobe_pressed.emit())
    _btn(root,"INVENTÁRIO",Vector2(1000,105),Vector2(215,62)).pressed.connect(func(): inventory_pressed.emit())
    _btn(root,"ATAQUE",Vector2(1000,545),Vector2(220,120)).pressed.connect(func(): attack_pressed.emit())
    _btn(root,"H1",Vector2(795,575),Vector2(78,78)).pressed.connect(func(): ability_pressed.emit(0))
    _btn(root,"H2",Vector2(885,535),Vector2(78,78)).pressed.connect(func(): ability_pressed.emit(1))
    _btn(root,"H3",Vector2(885,625),Vector2(78,78)).pressed.connect(func(): ability_pressed.emit(2))
    _move_button(root,"▲",Vector2(42,545),Vector2(84,68),Vector2(0,-1))
    _move_button(root,"◀",Vector2(0,612),Vector2(84,68),Vector2(-1,0))
    _move_button(root,"●",Vector2(84,612),Vector2(84,68),Vector2(0,1))
    _move_button(root,"▶",Vector2(168,612),Vector2(84,68),Vector2(1,0))

func _move_button(parent: Control, caption: String, pos: Vector2, size: Vector2, direction: Vector2) -> void:
    var b := _btn(parent,caption,pos,size)
    b.button_down.connect(func(): move_vector=direction)
    b.button_up.connect(func(): move_vector=Vector2.ZERO)
    b.mouse_entered.connect(func(): move_vector=direction if b.button_pressed else move_vector)

func _btn(parent: Control, text_value: String, pos: Vector2, size: Vector2) -> Button:
    var b := Button.new()
    b.text = text_value
    b.position = pos
    b.size = size
    b.add_theme_font_size_override("font_size",18)
    parent.add_child(b)
    return b

func set_mode(value: String) -> void:
    if title_label:
        title_label.text = value

func _refresh() -> void:
    if state == null:
        return
    var h: Dictionary = state.data["hunter"]
    title_label.text = "AETHERIA • CAÇADOR OFFLINE"
    stats_label.text = "HP %d/%d  |  LV %d  |  XP %d/%d  |  EN %d/%d" % [int(h["hp"]),int(h["max_hp"]),int(h["level"]),int(h["xp"]),int(h["xp_to_next"]),int(h["energy"]),int(h["max_energy"])]
    var active: Array = state.data["quests"]["active"]
    quest_label.text = "Contrato: nenhum" if active.is_empty() else "Contrato: "+str(active[0])
