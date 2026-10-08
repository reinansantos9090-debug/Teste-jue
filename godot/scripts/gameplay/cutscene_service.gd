extends CanvasLayer
## Offline story/cutscene presentation layer.
## No remote content or network dependency.

signal next_requested
signal closed

var panel:Panel
var chapter_label:Label
var speaker_label:Label
var dialogue_label:Label
var continue_button:Button
var close_button:Button
var accent_bar:ColorRect
var fade:ColorRect
var _active:=false

func _ready()->void:
    layer=30
    _build()

func _build()->void:
    fade=ColorRect.new()
    fade.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    fade.color=Color(0.01,0.02,0.05,0.72)
    add_child(fade)

    panel=Panel.new()
    panel.position=Vector2(70,430)
    panel.size=Vector2(1140,240)
    add_child(panel)

    accent_bar=ColorRect.new()
    accent_bar.position=Vector2(0,0)
    accent_bar.size=Vector2(12,240)
    accent_bar.color=Color("#66e8ff")
    panel.add_child(accent_bar)

    chapter_label=Label.new()
    chapter_label.position=Vector2(38,18)
    chapter_label.add_theme_font_size_override("font_size",16)
    panel.add_child(chapter_label)

    speaker_label=Label.new()
    speaker_label.position=Vector2(38,48)
    speaker_label.add_theme_font_size_override("font_size",28)
    panel.add_child(speaker_label)

    dialogue_label=Label.new()
    dialogue_label.position=Vector2(38,88)
    dialogue_label.size=Vector2(1035,90)
    dialogue_label.autowrap_mode=TextServer.AUTOWRAP_WORD_SMART
    dialogue_label.add_theme_font_size_override("font_size",21)
    panel.add_child(dialogue_label)

    continue_button=Button.new()
    continue_button.text="CONTINUAR"
    continue_button.position=Vector2(840,183)
    continue_button.size=Vector2(210,44)
    continue_button.pressed.connect(func():next_requested.emit())
    panel.add_child(continue_button)

    close_button=Button.new()
    close_button.text="FECHAR"
    close_button.position=Vector2(620,183)
    close_button.size=Vector2(150,44)
    close_button.pressed.connect(hide_cutscene)
    panel.add_child(close_button)

    hide_cutscene()

func show_line(chapter_title:String,speaker:String,text_value:String)->void:
    chapter_label.text=chapter_title
    speaker_label.text=speaker
    dialogue_label.text=text_value
    visible=true
    _active=true
    fade.modulate.a=0.0
    panel.modulate.a=0.0
    var tw:=create_tween()
    tw.set_parallel(true)
    tw.tween_property(fade,"modulate:a",1.0,0.16)
    tw.tween_property(panel,"modulate:a",1.0,0.20)

func hide_cutscene()->void:
    visible=false
    _active=false
    closed.emit()

func is_active()->bool:
    return _active
