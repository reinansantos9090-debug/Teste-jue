extends Node
## Procedural offline SFX. AudioStreamGenerator playback is filled with samples,
## so effects are audible without shipping or downloading audio files.

var enabled:=true
var master_db:=-6.0

func click()->void:
    _tone(620.0,0.05,0.07)

func hit()->void:
    _tone(145.0,0.08,0.12)

func skill()->void:
    _tone(440.0,0.10,0.08)
    _tone(720.0,0.12,0.06)

func level_up()->void:
    _tone(330.0,0.08,0.08)
    _tone(495.0,0.08,0.08)
    _tone(660.0,0.16,0.10)

func boss()->void:
    _tone(92.0,0.22,0.14)
    _tone(184.0,0.18,0.10)

func pickup()->void:
    _tone(880.0,0.05,0.06)
    _tone(1320.0,0.07,0.05)

func _tone(freq:float,duration:float,volume:float)->void:
    if not enabled:return
    var player:=AudioStreamPlayer.new()
    var stream:=AudioStreamGenerator.new()
    stream.mix_rate=22050
    stream.buffer_length=maxf(0.08,duration+0.03)
    player.stream=stream
    player.volume_db=master_db
    add_child(player)
    player.play()
    var playback:=player.get_stream_playback() as AudioStreamGeneratorPlayback
    if playback==null:
        player.queue_free()
        return
    var frames:=mini(4096,int(stream.mix_rate*duration))
    for i in range(frames):
        var t:=float(i)/float(stream.mix_rate)
        var envelope:=pow(maxf(0.0,1.0-float(i)/float(frames)),1.8)
        var sample:=sin(TAU*freq*t)*volume*envelope
        playback.push_frame(Vector2(sample,sample))
    get_tree().create_timer(duration+0.12).timeout.connect(func():
        if is_instance_valid(player):player.queue_free()
    )
