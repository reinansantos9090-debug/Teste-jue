# Aetheria: Echoes — progresso

## Estado atual

- Hub/QG 3D e batalha em tempo real já existem.
- Godot 4 está versionado e validado/exportado pelo CI.
- O conteúdo offline já cobre mapas, inimigos, chefes, armas, habilidades, história, wardrobe, núcleos, eventos e atividades.
- Esta etapa remove fontes geradas e o RPG 2D legado.

## Em correção

- Remover todos os GeneratedPack de offline/generated.
- Remover o código 2D por turnos sem referências no app 3D.
- Corrigir a divisão do engine Kotlin para manter cada arquivo abaixo de 800 linhas.
- Corrigir parsing/tipagem Godot reportados pelo CI.
- Cobrir dano, XP, loot, crafting e save com testes.
- Fortalecer o CI para detectar código legado/gerado.

## Critério de linhas

O limite é um teto de 500.000 linhas Kotlin. Não serão adicionadas linhas apenas para aumentar contagem.

## Validação

A última execução do CI (37712909647) exportou o APK Godot, mas falhou nos testes Kotlin e registrou erros de parsing Godot. O projeto ainda não está certificado como concluído.
