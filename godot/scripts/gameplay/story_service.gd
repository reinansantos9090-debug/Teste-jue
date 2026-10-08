extends Node
signal line_started(character: String, text: String)
signal chapter_changed(chapter: int)
signal journal_added(text: String)

var game_state: Node

const CHAPTERS := [
    {"title":"Ecos do Portal","lines":[
        ["Luna","Bem-vindo ao terraço. O Portal continua estável o bastante para uma primeira incursão."],
        ["Manny","Sua arma principal define seu ritmo. Escolha algo confortável antes de atravessar."],
        ["Jax","Os monstros estão migrando para as áreas habitadas. Precisamos descobrir o motivo."],
        ["Luna","Derrote as criaturas da fronteira, reúna energia do caos e volte antes que a Fenda cresça."],
        ["Manny","Você vai encontrar núcleos de melhoria para dano, crítico, velocidade, cura, saúde e esquiva."],
        ["Jax","Seu progresso é local. O diário e as recompensas ficam salvos no dispositivo."]
    ]},
    {"title":"Fronteira Esmeralda","lines":[
        ["Luna","A vegetação reage ao fluxo do Portal. Há mais criaturas aqui do que ontem."],
        ["Manny","O primeiro contrato é limpar a infestação e procurar rastros luminosos."],
        ["Jax","Os Perseguidores Neon seguem uma frequência que não reconhecemos."],
        ["Luna","Isso não parece uma invasão comum. Alguém está atraindo o caos."],
        ["Manny","Avance pelo mapa, complete os objetivos e desbloqueie novas rotas."]
    ]},
    {"title":"A Floresta Cristalina","lines":[
        ["Jax","Os cristais estão pulsando em sincronia com o Portal."],
        ["Luna","Há registros de expedições antigas neste lugar. Talvez encontremos uma pista."],
        ["Manny","Fendas menores estão aparecendo perto das ruínas."],
        ["Jax","Uma criatura enorme passou por aqui. O impacto deixou marcas por toda a floresta."],
        ["Luna","É hora de preparar uma build para algo maior."]
    ]},
    {"title":"Ruínas do Céu","lines":[
        ["Manny","A energia desta região altera gravidade, alcance e movimento."],
        ["Jax","Use a esquiva para atravessar ataques e combine habilidades em sequência."],
        ["Luna","Os antigos chamavam esta região de Horizonte Partido."],
        ["Manny","Há três sinais distintos. Um deles parece uma porta para uma arena."],
        ["Jax","Guarde essa descoberta para quando seu equipamento estiver pronto."]
    ]},
    {"title":"Cânions de Sucata","lines":[
        ["Luna","As máquinas abandonadas estão sendo reativadas pelo caos."],
        ["Manny","Colete placas, células de energia e fibras para fabricar equipamentos."],
        ["Jax","Os Golems protegem alguma coisa no centro do cânion."],
        ["Luna","Talvez seja um núcleo antigo, anterior ao sistema atual de Portais."],
        ["Manny","O próximo contrato vai exigir precisão e mobilidade."]
    ]},
    {"title":"O Domo da Fenda","lines":[
        ["Jax","A arena está selada. O núcleo do Domo está carregando."],
        ["Manny","O chefe muda de comportamento quando perde metade da energia vital."],
        ["Luna","Quando a segunda fase começar, mantenha distância e use sua esquiva."],
        ["Jax","Os lacaios surgirão para pressionar você. Não deixe a arena ficar cheia."],
        ["Luna","Esta vitória pode revelar quem iniciou a migração."]
    ]},
    {"title":"Titãs do Horizonte","lines":[
        ["Manny","O Titã não era o único guardião. Existem outros ecos adormecidos."],
        ["Jax","Cada chefe guarda uma parte do mapa que ainda não conseguimos acessar."],
        ["Luna","O Portal está abrindo rotas diferentes conforme você explora."],
        ["Manny","Combine arma, habilidades e núcleos antes de enfrentar um novo titã."],
        ["Jax","A próxima expedição será a mais longa até agora."]
    ]},
    {"title":"A Queda dos Portais","lines":[
        ["Luna","Os Portais começaram a se sobrepor. As fronteiras estão desaparecendo."],
        ["Jax","Rifts instáveis aceleram os monstros e alteram suas rotas."],
        ["Manny","Use as recompensas das expedições para maximizar seus núcleos."],
        ["Luna","Seu diário registra cada contrato, cada chefe e cada descoberta."],
        ["Jax","Agora não estamos mais investigando uma crise. Estamos tentando impedir uma."]
    ]},
    {"title":"Arquivo do Caçador","lines":[
        ["Manny","Seu histórico mostra as batalhas que definiram esta jornada."],
        ["Luna","As primeiras missões pareciam pequenas, mas cada uma revelou uma peça do quebra-cabeça."],
        ["Jax","Você já conhece biomas, armas, fendas e padrões dos chefes."],
        ["Manny","Ainda existem estilos, equipamentos e desafios opcionais no terraço."],
        ["Luna","A campanha principal chega ao seu primeiro encerramento. O mundo continua offline."]
    ]},
    {"title":"Além do Eco","lines":[
        ["Jax","Um último sinal surgiu além do limite do mapa."],
        ["Luna","Não é um Portal comum. A frequência parece responder às escolhas do caçador."],
        ["Manny","Talvez seja a chave para o próximo capítulo."],
        ["Luna","Salve seu progresso, prepare seu equipamento e atravesse quando estiver pronto."],
        ["Jax","Aetheria ainda tem ecos para revelar."]
    ]},
    {"title":"A Cidade Suspensa","lines":[
        ["Luna","As Ruínas do Céu escondem uma cidade que nunca tocou o solo."],
        ["Jax","Os registros mostram caçadores usando os mesmos corredores há muitas gerações."],
        ["Manny","O problema é que os corredores estão se rearranjando."],
        ["Luna","Cada vitória estabiliza uma parte do mapa e libera um novo caminho."],
        ["Jax","Encontramos uma gravação apontando para o núcleo central."],
        ["Manny","Leve suas armas de maior alcance. O céu tem inimigos que não pousam."],
        ["Luna","Quando voltarmos ao terraço, o arquivo deverá registrar esta descoberta."],
        ["Jax","Aetheria está ficando maior a cada resposta."]
    ]},
    {"title":"O Núcleo de Sucata","lines":[
        ["Manny","O núcleo encontrado no cânion ainda está funcionando."],
        ["Jax","Ele converte sucata em energia, mas também atrai criaturas."],
        ["Luna","Precisamos desmontá-lo sem perder a fonte de energia."],
        ["Manny","Gadgets fabricados no terraço podem absorver parte do impacto."],
        ["Jax","Use a oficina para preparar minas, drones e granadas."],
        ["Luna","O núcleo pode se tornar uma ferramenta em vez de uma ameaça."],
        ["Manny","Só precisamos sobreviver ao próximo surto."],
        ["Jax","O surto começou."]
    ]},
    {"title":"O Silêncio do Vazio","lines":[
        ["Luna","A última Fenda não produziu o ruído normal do Portal."],
        ["Jax","Não há eco, não há energia residual e nenhum sinal de retorno."],
        ["Manny","Mesmo assim, algo está atravessando para o nosso lado."],
        ["Luna","O diário marca essa região como Além do Eco."],
        ["Jax","As criaturas daqui não obedecem aos ciclos comuns de ataque."],
        ["Manny","Precisaremos alternar mobilidade, barreira e controle."],
        ["Luna","Não existe reforço chegando. Esta incursão é só nossa."],
        ["Jax","Então termine o que começou."]
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
