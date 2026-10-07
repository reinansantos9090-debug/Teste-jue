extends Node
var enabled := true
var master_db := -6.0

func click() -> void:
    _tone(620.0,0.05,0.07)

func hit() -> void:
    _tone(145.0,0.08,0.12)

func skill() -> void:
    _tone(440.0,0.10,0.08)
    _tone(720.0,0.12,0.06)

func level_up() -> void:
    _tone(330.0,0.08,0.08)
    _tone(495.0,0.08,0.08)
    _tone(660.0,0.16,0.10)

func _tone(freq:float,duration:float,volume:float)->void:
    if not enabled:return
    var player:=AudioStreamPlayer.new()
    var stream:=AudioStreamGenerator.new()
    stream.mix_rate=22050
    stream.buffer_length=duration+0.04
    player.stream=stream
    player.volume_db=linear_to_db(volume)
    add_child(player)
    player.play()
