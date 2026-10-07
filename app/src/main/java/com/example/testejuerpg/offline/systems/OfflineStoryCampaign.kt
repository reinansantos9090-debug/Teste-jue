package com.example.testejuerpg.offline.systems

import android.content.Context

data class StoryBeat(
    val id: String,
    val speaker: String,
    val text: String,
    val tone: String
)

data class StoryChapter(
    val id: String,
    val number: Int,
    val title: String,
    val location: String,
    val synopsis: String,
    val objectiveKind: String,
    val targetId: String,
    val targetCount: Int,
    val rewardEnergy: Int,
    val rewardGold: Int,
    val beats: List<StoryBeat>
)

data class StorySnapshot(
    val chapterNumber: Int,
    val chapterTitle: String,
    val location: String,
    val synopsis: String,
    val beatNumber: Int,
    val beatTotal: Int,
    val speaker: String,
    val text: String,
    val objective: String,
    val progress: Int,
    val required: Int,
    val readyForBattle: Boolean,
    val completedChapters: Int,
    val finished: Boolean
)

class OfflineStoryCampaign(context: Context) {
    private val prefs = context.getSharedPreferences("aetheria_story_v1", Context.MODE_PRIVATE)
    private val chapters = buildChapters()

    private var chapterIndex = prefs.getInt("chapter_index", 0).coerceIn(0, chapters.lastIndex)
    private var beatIndex = prefs.getInt("beat_index", 0).coerceAtLeast(0)
    private var progress = prefs.getInt("objective_progress", 0).coerceAtLeast(0)
    private var battleStarted = prefs.getBoolean("battle_started", false)
    private var completedChapters = prefs.getInt("completed_chapters", 0).coerceAtLeast(0)

    fun snapshot(): StorySnapshot {
        val chapter = currentChapter()
        val beat = chapter.beats[beatIndex.coerceIn(0, chapter.beats.lastIndex)]
        return StorySnapshot(
            chapter.number, chapter.title, chapter.location, chapter.synopsis,
            beatIndex + 1, chapter.beats.size, beat.speaker, beat.text,
            objectiveLabel(chapter), progress, chapter.targetCount,
            readyForBattle(), completedChapters, finished()
        )
    }

    fun currentChapter(): StoryChapter = chapters[chapterIndex]
    fun chapters(): List<StoryChapter> = chapters
    fun readyForBattle(): Boolean = !finished() && beatIndex >= currentChapter().beats.lastIndex && !battleStarted

    fun startBattle(): Boolean {
        if (!readyForBattle()) return false
        battleStarted = true
        save()
        return true
    }

    fun advanceBeat(): Boolean {
        if (finished() || battleStarted) return false
        if (beatIndex < currentChapter().beats.lastIndex) {
            beatIndex += 1
            save()
            return true
        }
        return false
    }

    fun recordKill(targetId: String, elite: Boolean, boss: Boolean): Boolean {
        if (!battleStarted || finished()) return false
        val chapter = currentChapter()
        val matches = when (chapter.objectiveKind) {
            "BOSS" -> boss
            "ELITE" -> elite
            "KILL" -> chapter.targetId == "any" || chapter.targetId == targetId
            else -> false
        }
        if (matches) progress = (progress + 1).coerceAtMost(chapter.targetCount)
        save()
        return matches
    }

    fun recordCore(): Boolean {
        if (!battleStarted || finished()) return false
        val chapter = currentChapter()
        if (chapter.objectiveKind != "CORE") return false
        progress = (progress + 1).coerceAtMost(chapter.targetCount)
        save()
        return true
    }

    fun recordArenaWave(): Boolean {
        if (!battleStarted || finished()) return false
        val chapter = currentChapter()
        if (chapter.objectiveKind != "ARENA") return false
        progress = (progress + 1).coerceAtMost(chapter.targetCount)
        save()
        return true
    }

    fun objectiveComplete(): Boolean = !finished() && battleStarted && progress >= currentChapter().targetCount

    fun completeBattle(): StoryChapter? {
        if (!objectiveComplete()) return null
        val completed = currentChapter()
        completedChapters += 1
        battleStarted = false
        if (chapterIndex < chapters.lastIndex) {
            chapterIndex += 1
            beatIndex = 0
            progress = 0
        } else {
            beatIndex = currentChapter().beats.lastIndex
            progress = currentChapter().targetCount
        }
        save()
        return completed
    }

    fun resetProgress() {
        chapterIndex = 0
        beatIndex = 0
        progress = 0
        battleStarted = false
        completedChapters = 0
        save()
    }

    fun finishStory(): Boolean = finished()

    private fun finished(): Boolean =
        chapterIndex == chapters.lastIndex && progress >= chapters.last().targetCount && completedChapters >= chapters.size

    private fun objectiveLabel(chapter: StoryChapter): String = when (chapter.objectiveKind) {
        "BOSS" -> "Derrote o chefe " + chapter.targetId
        "ELITE" -> "Derrote " + chapter.targetCount + " inimigos de elite"
        "CORE" -> "Colete " + chapter.targetCount + " Núcleos de Aether"
        "ARENA" -> "Complete " + chapter.targetCount + " ondas de ameaça"
        else -> "Derrote " + chapter.targetCount + " alvos: " + chapter.targetId
    }

    private fun save() {
        prefs.edit()
            .putInt("chapter_index", chapterIndex)
            .putInt("beat_index", beatIndex)
            .putInt("objective_progress", progress)
            .putBoolean("battle_started", battleStarted)
            .putInt("completed_chapters", completedChapters)
            .apply()
    }

    private fun b(id: String, speaker: String, text: String, tone: String) = StoryBeat(id, speaker, text, tone)

    private fun chapter(
        n: Int, title: String, location: String, synopsis: String, kind: String, target: String, count: Int,
        energy: Int, gold: Int, beats: List<StoryBeat>
    ) = StoryChapter(
        "chapter_" + n.toString().padStart(2, '0'), n, title, location, synopsis, kind, target, count, energy, gold, beats
    )

    private fun buildChapters(): List<StoryChapter> = listOf(
        chapter(1, "O Sinal no Céu", "QG do Terraço", "Um pulso desconhecido abre uma passagem sobre a cidade.", "KILL", "slime", 6, 120, 80, listOf(
            b("1a","Ari","O céu acabou de piscar. Isso não é clima, é um chamado.","calm"),
            b("1b","Mila","As criaturas estão saindo da fenda como se conhecessem o caminho.","alert"),
            b("1c","Ari","Primeiro vamos proteger a cidade. As respostas podem esperar.","urgent"),
            b("1d","Sistema","Primeira missão de campo: estabeleça uma zona segura.","mission")
        )),
        chapter(2, "Maré de Gel", "Planície do Prisma", "A primeira colônia de criaturas de Aether se espalha pela planície.", "KILL", "slime", 10, 140, 95, listOf(
            b("2a","Mila","O gel não é natural. Ele repete o mesmo padrão de movimento.","scan"),
            b("2b","Ari","Então alguma coisa está guiando o enxame.","thinking"),
            b("2c","Mila","Descubra o foco. Eu mantenho a evacuação.","command"),
            b("2d","Sistema","Elimine a colônia e procure sinais de controle.","mission")
        )),
        chapter(3, "Frequência Fantasma", "Floresta Neon", "Um eco sonoro atrai caçadores para uma região proibida.", "CORE", "core", 5, 160, 105, listOf(
            b("3a","Lio","Escute. O som não vem das árvores. Vem debaixo delas.","whisper"),
            b("3b","Mila","Os Núcleos estão vibrando juntos. Há uma frequência comum.","analysis"),
            b("3c","Ari","Colete amostras. Não destrua a fonte até entendermos o padrão.","care"),
            b("3d","Sistema","Recupere Núcleos de Aether na floresta.","mission")
        )),
        chapter(4, "O Caçador que Sumiu", "Ruínas de Sucata", "Uma cápsula de retorno pertence a uma patrulha desaparecida.", "ELITE", "elite", 5, 180, 120, listOf(
            b("4a","Mila","Cinco sinais biométricos desapareceram ao mesmo tempo.","grave"),
            b("4b","Ari","Se eles ainda estiverem vivos, o rastro termina aqui.","resolve"),
            b("4c","Lio","As máquinas estão protegendo alguma coisa. Não pare quando avançarem.","warning"),
            b("4d","Sistema","Elimine os guardiões de elite das ruínas.","mission")
        )),
        chapter(5, "Peso da Sucata", "Cânion do Vórtice", "Um antigo reator transformou ferro em criaturas colossais.", "KILL", "golem", 7, 200, 135, listOf(
            b("5a","Ari","O metal está crescendo. Nunca vi uma fornalha criar vida.","surprise"),
            b("5b","Mila","Não é vida. É resposta ao ambiente.","science"),
            b("5c","Ari","Então vamos desligá-lo antes que encontre outra coisa para copiar.","resolve"),
            b("5d","Sistema","Destrua os Golems de Sucata e estabilize o cânion.","mission")
        )),
        chapter(6, "Porta de Cinzas", "Núcleo Magmático", "Uma segunda passagem conecta Aetheria a um mundo vulcânico instável.", "KILL", "stalker", 10, 220, 150, listOf(
            b("6a","Lio","O calor está abrindo rachaduras no ar.","fear"),
            b("6b","Mila","O Portal não está apenas conectando lugares. Está aprendendo rotas.","analysis"),
            b("6c","Ari","Então cada expedição pode estar ensinando alguma coisa a ele.","realization"),
            b("6d","Sistema","Caçe os Perseguidores Neon antes que alcancem a passagem.","mission")
        )),
        chapter(7, "A Cidade Debaixo da Cidade", "Subnível Aether", "Túneis esquecidos revelam uma civilização anterior aos Portais.", "CORE", "core", 8, 240, 170, listOf(
            b("7a","Mila","Estas paredes são mais antigas que a própria cidade.","wonder"),
            b("7b","Ari","E alguém escreveu instruções nelas em uma linguagem de energia.","wonder"),
            b("7c","Lio","Colecione os Núcleos. Talvez eles sejam a tradução.","focus"),
            b("7d","Sistema","Recupere Núcleos nas câmaras subterrâneas.","mission")
        )),
        chapter(8, "Rastro de Horizonte", "Domo da Aurora", "Um inimigo veloz deixa cortes luminosos no espaço.", "ELITE", "elite", 7, 260, 190, listOf(
            b("8a","Ari","Ele não está correndo. Está saltando entre quadros do mundo.","analysis"),
            b("8b","Mila","Não tente alcançá-lo. Faça com que ele escolha onde aparecer.","tactics"),
            b("8c","Ari","Vamos transformar o terreno em armadilha.","resolve"),
            b("8d","Sistema","Derrote elites no Domo da Aurora.","mission")
        )),
        chapter(9, "Primeiro Guardião", "Santuário Prismático", "Um antigo protetor desperta para testar os novos caçadores.", "BOSS", "Sentinela Prismática", 1, 320, 250, listOf(
            b("9a","Sentinela Prismática","Quem carrega luz sem compreender seu custo não merece atravessar.","ancient"),
            b("9b","Ari","Não pedimos passagem. Queremos impedir que o mundo desabe.","defiant"),
            b("9c","Sentinela Prismática","Então prove que sabe proteger aquilo que não pode possuir.","trial"),
            b("9d","Sistema","Derrote a Sentinela Prismática.","boss")
        )),
        chapter(10, "O Mapa Partido", "Observatório do Vazio", "Fragmentos do mapa mostram seis novas rotas impossíveis.", "CORE", "core", 10, 280, 210, listOf(
            b("10a","Mila","Seis rotas terminam no mesmo vazio.","analysis"),
            b("10b","Lio","Como se alguém estivesse desenhando uma cidade do outro lado.","mystery"),
            b("10c","Ari","Hoje, informação vale mais que ouro.","command"),
            b("10d","Sistema","Colete Núcleos para estabilizar o mapa.","mission")
        )),
        chapter(11, "Contrato de Ferro", "Cemitério Mecânico", "A ameaça agora possui máquinas de patrulha próprias.", "KILL", "golem", 12, 300, 230, listOf(
            b("11a","Ari","Eles mudaram o comportamento. Agora trabalham em formação.","alert"),
            b("11b","Mila","Isso significa memória compartilhada.","analysis"),
            b("11c","Ari","Então apagaremos a formação uma peça de cada vez.","resolve"),
            b("11d","Sistema","Quebre a patrulha e abra caminho para o centro.","mission")
        )),
        chapter(12, "Vozes do Vazio", "Fenda Silenciosa", "As comunicações captam mensagens de caçadores que nunca existiram.", "ELITE", "elite", 9, 320, 245, listOf(
            b("12a","Lio","Eu ouvi meu próprio nome. Mas a gravação é de amanhã.","fear"),
            b("12b","Mila","O Portal pode estar devolvendo ecos de futuros possíveis.","theory"),
            b("12c","Ari","Nós escolhemos o nosso.","resolve"),
            b("12d","Sistema","Elimine as anomalias de elite da Fenda Silenciosa.","mission")
        )),
        chapter(13, "As Três Luas", "Vale de Cristal", "Três fontes de energia sincronizam a região inteira.", "CORE", "core", 12, 340, 260, listOf(
            b("13a","Mila","As luas são satélites artificiais.","reveal"),
            b("13b","Ari","Quem construiu isso queria observar as fendas de longe.","wonder"),
            b("13c","Lio","Ou manter alguma coisa presa.","dark"),
            b("13d","Sistema","Recupere energia suficiente para alimentar o observatório.","mission")
        )),
        chapter(14, "A Fortaleza Ambulante", "Planície Magnética", "Uma fortaleza viva atravessa o mapa puxando tudo para seu núcleo.", "BOSS", "Colosso Magnético", 1, 380, 300, listOf(
            b("14a","Ari","Ele puxa o terreno junto com os próprios passos.","urgent"),
            b("14b","Mila","O núcleo fica exposto quando ele gira.","tactics"),
            b("14c","Ari","Uma abertura é melhor que dez golpes errados.","calm"),
            b("14d","Sistema","Derrote o Colosso Magnético.","boss")
        )),
        chapter(15, "Arquivo Zero", "Biblioteca Aether", "O primeiro arquivo revela a origem do fenômeno.", "CORE", "core", 14, 400, 315, listOf(
            b("15a","Lio","Tudo começou como uma ferramenta para explorar mundos distantes.","history"),
            b("15b","Mila","Depois transformaram a ferramenta em prisão.","history"),
            b("15c","Ari","E alguém está tentando abrir as portas de novo.","resolve"),
            b("15d","Sistema","Colete os Núcleos do Arquivo Zero.","mission")
        )),
        chapter(16, "Rebelião das Sombras", "Cidade Espelhada", "Reflexos hostis aprendem os padrões dos caçadores.", "ELITE", "elite", 10, 420, 330, listOf(
            b("16a","Ari","Eles desviaram do mesmo ataque duas vezes.","concern"),
            b("16b","Mila","Estão observando nossas escolhas.","analysis"),
            b("16c","Ari","Então paremos de repetir escolhas.","strategy"),
            b("16d","Sistema","Derrote elites adaptativas da Cidade Espelhada.","mission")
        )),
        chapter(17, "Coração de Tempestade", "Mar de Plasma", "Uma tempestade concentra milhares de partículas de energia em um único núcleo.", "ARENA", "wave", 10, 440, 350, listOf(
            b("17a","Mila","A tempestade está ficando menor e mais densa.","analysis"),
            b("17b","Ari","Quando fechar, qualquer coisa dentro vira combustível.","danger"),
            b("17c","Lio","Então não deixe os monstros tocarem no núcleo.","command"),
            b("17d","Sistema","Sobreviva a dez ondas no coração da tempestade.","mission")
        )),
        chapter(18, "O Rei sem Trono", "Palácio Invertido", "Um soberano artificial controla criaturas por sinais de luz.", "BOSS", "Rei Invertido", 1, 470, 380, listOf(
            b("18a","Rei Invertido","Eu fui criado para comandar o vazio. Vocês trouxeram caos.","ancient"),
            b("18b","Ari","Caos é só o nome que damos ao que ainda não conseguimos organizar.","defiant"),
            b("18c","Rei Invertido","Então organizem isto.","challenge"),
            b("18d","Sistema","Derrote o Rei Invertido em duas fases.","boss")
        )),
        chapter(19, "Rachadura Azul", "Geleira Elétrica", "Uma nova passagem começa a congelar memórias.", "KILL", "stalker", 16, 490, 400, listOf(
            b("19a","Lio","Esqueci por que vim aqui por alguns segundos.","fear"),
            b("19b","Mila","Não deixe a névoa tocar no capacete.","warning"),
            b("19c","Ari","Não vamos lutar só contra monstros desta vez.","grave"),
            b("19d","Sistema","Elimine perseguidores antes que a memória seja apagada.","mission")
        )),
        chapter(20, "A Estação das Mil Portas", "Terminal Dimensional", "Cada porta mostra uma versão diferente de Aetheria.", "CORE", "core", 16, 510, 420, listOf(
            b("20a","Ari","Uma porta mostra nossa cidade. Outra mostra cinzas.","wonder"),
            b("20b","Mila","As duas podem ser verdadeiras.","gravity"),
            b("20c","Lio","Qual delas é a original?","question"),
            b("20d","Sistema","Recupere os Núcleos que estabilizam o terminal.","mission")
        )),
        chapter(21, "Queda do Farol", "Farol do Vácuo", "A torre que mantinha o Portal está perdendo energia.", "ELITE", "elite", 12, 530, 440, listOf(
            b("21a","Mila","Quando o farol apagar, todas as rotas podem colidir.","urgent"),
            b("21b","Ari","Então cada segundo vale uma vida.","resolve"),
            b("21c","Lio","Eu fico na base. Vocês vão ao topo.","command"),
            b("21d","Sistema","Elimine os guardiões do farol.","mission")
        )),
        chapter(22, "As Cinzas que Lembram", "Deserto da Memória", "Fragmentos do passado ganham forma física.", "ARENA", "wave", 12, 550, 470, listOf(
            b("22a","Ari","Essas criaturas têm memórias de pessoas reais.","sad"),
            b("22b","Mila","Não as trate como monstros. Trate como ecos.","care"),
            b("22c","Lio","Vamos limpar a área sem apagar o que restou.","gentle"),
            b("22d","Sistema","Complete doze ondas de ecos instáveis.","mission")
        )),
        chapter(23, "O Último Mapa", "Câmara de Origem", "O mapa final aponta para o local onde o sistema de Portais nasceu.", "CORE", "core", 18, 580, 500, listOf(
            b("23a","Mila","O endereço final não é outro mundo. É este.","reveal"),
            b("23b","Ari","Nós estivemos dentro do mapa o tempo todo.","realization"),
            b("23c","Lio","Quem está do outro lado conhece cada passo nosso.","resolve"),
            b("23d","Sistema","Reúna os Núcleos necessários para abrir a Câmara de Origem.","mission")
        )),
        chapter(24, "A Máquina de Horizonte", "Câmara de Origem", "A máquina que criou as primeiras fendas desperta.", "BOSS", "Titã de Horizonte", 1, 620, 540, listOf(
            b("24a","Titã de Horizonte","Todas as suas expedições foram dados para minha calibração.","reveal"),
            b("24b","Ari","Então você sabe que não vamos parar.","defiant"),
            b("24c","Titã de Horizonte","Parar não é necessário. Apenas escolha o que salvar.","gravity"),
            b("24d","Sistema","Derrote o Titã de Horizonte e alcance o núcleo.","boss")
        )),
        chapter(25, "Depois do Trovão", "QG do Terraço", "O QG sobrevive, mas todas as rotas estão instáveis.", "KILL", "slime", 8, 640, 560, listOf(
            b("25a","Mila","A cidade está segura. O Portal, não.","quiet"),
            b("25b","Ari","Nossa história não termina no chefe.","smile"),
            b("25c","Lio","Podemos reconstruir a rede sem repetir os erros.","hope"),
            b("25d","Sistema","Faça uma última patrulha e assegure o QG.","mission")
        )),
        chapter(26, "Nova Rota", "Jardins de Prisma", "O primeiro mundo restaurado começa a florescer.", "CORE", "core", 20, 660, 580, listOf(
            b("26a","Ari","Olhe. O chão voltou a respirar.","wonder"),
            b("26b","Mila","Aetheria está respondendo à ausência do conflito.","hope"),
            b("26c","Lio","Talvez os Portais também possam servir para cuidar.","vision"),
            b("26d","Sistema","Recupere Núcleos e prepare a nova rede.","mission")
        )),
        chapter(27, "Os Caçadores do Amanhã", "Cidade-Refúgio", "Uma geração nova chega ao QG para aprender a atravessar as fendas.", "ELITE", "elite", 14, 680, 600, listOf(
            b("27a","Ari","Não ensine só a lutar.","wisdom"),
            b("27b","Mila","Ensine a voltar para casa.","wisdom"),
            b("27c","Lio","E a diferença entre uma missão e uma aventura.","smile"),
            b("27d","Sistema","Proteja os aprendizes durante o teste final.","mission")
        )),
        chapter(28, "O Eco Final", "Vazio Azul", "Um último eco tenta reabrir uma rota perdida.", "ARENA", "wave", 14, 700, 620, listOf(
            b("28a","Mila","É o mesmo sinal do começo.","recognition"),
            b("28b","Ari","Só que agora sabemos responder.","confidence"),
            b("28c","Lio","Equipe pronta.","brave"),
            b("28d","Sistema","Sobreviva ao eco final e feche a rota perdida.","mission")
        )),
        chapter(29, "Aetheria Livre", "Cidade Alta", "As últimas criaturas deixam de aparecer, mas o céu permanece aberto.", "BOSS", "Guardião de Aetheria", 1, 760, 700, listOf(
            b("29a","Guardião de Aetheria","Vocês chegaram ao fim da primeira trilha.","ancient"),
            b("29b","Ari","Primeira?","surprise"),
            b("29c","Guardião de Aetheria","Todo mundo precisa de uma primeira história antes da segunda.","smile"),
            b("29d","Sistema","Derrote o Guardião de Aetheria e conclua a campanha.","boss")
        )),
        chapter(30, "Horizonte Aberto", "QG do Terraço", "O Portal é reconstruído como uma ferramenta de exploração responsável.", "KILL", "any", 18, 800, 760, listOf(
            b("30a","Ari","Quantos mundos existem além deste?","wonder"),
            b("30b","Mila","Mais do que podemos contar hoje.","hope"),
            b("30c","Lio","Então vamos contar amanhã.","smile"),
            b("30d","Sistema","Última patrulha do arco principal: mantenha o horizonte seguro.","finale")
        ))
    )
}
