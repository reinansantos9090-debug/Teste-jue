# Aetheria: Echoes — referência de design offline e desempenho mobile

Esta documentação registra a separação entre referência de experiência e implementação própria.

## Estrutura de experiência usada como referência

A documentação pública da Supercell para mo.co descreve um QG no terraço com Portal, Expeditions, Rifts e Versus; também descreve Wardrobe, Upgrade Cores e outros sistemas de progressão. A implementação deste projeto usa essas categorias como referência estrutural, mas os nomes de personagens, mundos, armas, monstros, chefes, falas, código e identidade visual do projeto são próprios.

Fontes oficiais:
- https://support.supercell.com/mo-co/pt/articles/rooftop-hq.html
- https://support.supercell.com/mo-co/pt/articles/upgrade-cores.html
- https://support.supercell.com/mo-co/pt/articles/elite-hunter-path.html
- https://support.supercell.com/mo-co/en/articles/resources-3.html

## Regras de fidelidade do projeto

1. Loop de QG -> Portal -> atividade -> combate -> recompensa -> progresso.
2. Armas possuem kits fixos de habilidades.
3. Upgrade Cores modificam dano, crítico, velocidade, cooldown, cura, vida e esquiva.
4. Expedições, Rifts e Versus são representados offline.
5. O modo história é uma camada autoral persistente e não depende de servidor.
6. Conteúdo visual deve ser original e armazenado localmente.
7. O runtime não deve carregar a massa de catálogo desnecessariamente.

## Desempenho Android

O objetivo de 60 FPS é tratado como meta de engenharia, não como afirmação de que todos os aparelhos atingirão 60 FPS. O pipeline deve medir por cena e distinguir gargalos de CPU e GPU antes de otimizar.

Ferramentas recomendadas pelo Android Developers:
- Perfetto para rastreamento de CPU/GPU e threads.
- Android GPU Inspector para análise de GPU.
- Android Frame Pacing para sincronização do frame.
- `dumpsys meminfo`/Meminfo para uso de memória.
- Simpleperf para hot paths.
- Baseline Profiles/Macrobenchmark para inicialização e caminhos críticos.

Fontes:
- https://developer.android.com/games/optimize/gameperformance
- https://developer.android.com/games/optimize/optimization-tips
- https://developer.android.com/games/tools
- https://developer.android.com/games/optimize/overview

## Política de conteúdo

A meta é reproduzir a sensação de jogo de caça em tempo real, não copiar assets proprietários. Qualquer referência visual externa serve para direção de iluminação, composição, legibilidade de HUD e densidade de efeitos, nunca para duplicação de arte.
