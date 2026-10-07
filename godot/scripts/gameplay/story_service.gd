extends Node
signal line_started(character: String, text: String)
signal chapter_changed(chapter: int)
signal journal_added(text: String)

var game_state: Node

const CHAPTERS := [
    {"title":"Ecos do Portal","lines":[
        ["Luna","Bem-vindo, caçador. O Portal está instável, mas ainda podemos usá-lo."],
        ["Manny","Pegue uma arma, equipe um núcleo e volte para o terraço quando precisar."],
        ["Jax","Se encontrar uma Fenda, respeite o cronômetro. O caos não espera."],
        ["Luna","Sua primeira missão é limpar a fronteira e descobrir por que as criaturas estão migrando."]
    ]},
    {"title":"A Floresta Cristalina","lines":[
        ["Manny","Os cristais estão pulsando. Isso normalmente significa que alguma coisa acordou."],
        ["Jax","Há sinais de um predador maior escondido além da mata."],
        ["Luna","Cada monstro derrotado deixa uma pista. Continue seguindo os ecos."]
    ]},
    {"title":"O Domo da Fenda","lines":[
        ["Jax","O núcleo do Domo está carregando energia demais."],
        ["Manny","O chefe altera a arena quando muda de fase. Não fique parado."],
        ["Luna","Esta batalha pode decidir o destino da Fronteira Esmeralda."]
    ]},
    {"title":"Horizonte Partido","lines":[
        ["Luna","Os portais estão conectando regiões que nunca deveriam se tocar."],
        ["Jax","Precisamos de um caçador capaz de cruzar todos os biomas."],
        ["Manny","E de equipamento preparado para qualquer mutação do caos."]
    ]}
]

func setup(state: Node) -> void:
    game_state = state

func current_chapter() -> Dictionary:
    var index := clampi(int(game_state.data["story"]["chapter"]) - 1, 0, CHAPTERS.size() - 1)
    return CHAPTERS[index]

func next_line() -> Dictionary:
    var story: Dictionary = game_state.data["story"]
    var chapter: Dictionary = current_chapter()
    var lines: Array = chapter["lines"]
    var index := int(story["scene"])
    if index >= lines.size():
        if int(story["chapter"]) < CHAPTERS.size():
            story["chapter"] = int(story["chapter"]) + 1
            story["scene"] = 0
            chapter_changed.emit(int(story["chapter"]))
            chapter = current_chapter()
            lines = chapter["lines"]
            index = 0
        else:
            return {"character":"Sistema","text":"Você concluiu a campanha disponível."}
    var pair: Array = lines[index]
    story["scene"] = index + 1
    var character := str(pair[0])
    var text := str(pair[1])
    story["journal"].append("%s: %s" % [character, text])
    line_started.emit(character, text)
    journal_added.emit(text)
    return {"character":character,"text":text}

func chapter_title() -> String:
    return str(current_chapter()["title"])
