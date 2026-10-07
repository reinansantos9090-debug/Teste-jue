# Teste-jue 3D — Aetheria: Echoes

**Teste-jue** está sendo convertido de um RPG Android por turnos para uma implementação jogável **3D nativa para Android**, inspirada no loop de caça, portal, armas com kits fixos, expedições, monstros e chefes de *mo.co* em 2025, sem copiar personagens, mapas, código, sons ou assets proprietários.

## O que já entrou no projeto

- Renderer 3D nativo com OpenGL ES 2.0, sem navegador ou Phaser.
- Câmera em terceira pessoa alta e movimentação livre.
- QG/rooftop 3D com portal de expedição.
- Expedição 3D com Slime de Aether, Perseguidor Neon, Golem de Sucata e Titã de Sobrecarga.
- Quatro armas originais com kits fixos: Lâminas Voltáicas, Arco Tóxico, Canhão Pulsar e Martelo Sucateiro.
- Ataque principal, dash, ataques em área, cura, projéteis e perseguição inimiga.
- Núcleos de Aether, ouro, XP, níveis, objetivos e barra de chefe.
- Armário de armas e base de crafting para gadgets.
- Salvamento local/offline de progresso, recursos e posição.
- Controles touch: joystick, ataque, três habilidades e mochila.

## Referência do PDF

As mecânicas do PDF de **Aetheria: Echoes** são a referência de conteúdo: combate, hitbox/hurtbox, quests, crafting, loot, XP/nível, mini-chefe/chefe, efeitos, ambiente e salvamento offline. A apresentação 2D/2.5D do PDF foi substituída deliberadamente por uma fundação 3D para Android.

## Referência de design

A estrutura de *mo.co* de 2025 é usada apenas como referência de design: Portal, Rooftop/HQ, Expedições, tarefas, monstros, armas com kits predefinidos e progressão. Nomes, personagens, mapas, armas, efeitos e código do projeto são próprios.

## Build

O workflow `.github/workflows/android.yml` instala Gradle 9.3.1 e JDK 21, executa `:app:assembleDebug` e publica `app-debug.apk` como artifact.


## Escala desta etapa

Os packs offline compiláveis adicionados nesta conversão somam **100.769 linhas** de código-fonte Kotlin, além do núcleo 3D, sistemas de progressão e código Android. A contagem é baseada na composição dos arquivos versionados desta etapa; ela não é usada como medida de qualidade por si só.
