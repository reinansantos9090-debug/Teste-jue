package com.example.testejuerpg.offline.systems

data class OfflineWeaponDefinition(
    val id: String,
    val name: String,
    val role: String,
    val archetype: Int,
    val damage: Float,
    val range: Float,
    val cooldown: Float,
    val skill1: String,
    val skill2: String,
    val skill3: String,
    val tint: Int,
    val rarity: Int
)

/**
 * Original offline arsenal. The names, silhouettes and mechanics are project-owned.
 * Archetype: 0 melee, 1 ranged, 2 area, 3 heavy, 4 control/support.
 */
object OfflineWeaponCatalog {
    val all: List<OfflineWeaponDefinition> = listOf(
        w("volt_blades","Lâminas Voltáicas","Dano corpo a corpo",0,38f,2.4f,.38f,"Dash","Corte Tempestade","Pulso de Cura",0x7FD7FF,2),
        w("toxic_bow","Arco Tóxico","Dano à distância",1,42f,10.5f,.34f,"Rajada Tripla","Evasão Verde","Antídoto",0x7AFF91,2),
        w("pulsar_cannon","Canhão Pulsar","Dano em área",2,34f,10f,.42f,"Disparo Triplo","Onda de Choque","Nanocura",0xC59AFF,3),
        w("scrap_hammer","Martelo Sucateiro","Dano pesado",3,52f,2.7f,.55f,"Investida","Terremoto","Pulso Reparador",0xFFB76A,3),
        w("prism_spear","Lança Prismática","Precisão corpo a corpo",0,45f,2.9f,.40f,"Impulso Prismático","Estocada Solar","Barreira Prisma",0xB4F2FF,3),
        w("echo_chakrams","Chacras de Eco","Rajada à distância",1,36f,9.5f,.28f,"Eco Triplo","Retorno Cortante","Passo Fantasma",0xE7A6FF,2),
        w("nova_gauntlets","Manoplas Nova","Combate rápido",0,40f,2.6f,.25f,"Pancada Relâmpago","Combo Nova","Regeneração",0xFF8D70,2),
        w("rift_mortar","Morteiro de Fenda","Artilharia em área",2,58f,11f,.68f,"Chuva Orbital","Campo Instável","Pulso Médico",0xB98CFF,4),
        w("arc_whip","Chicote de Arco","Controle próximo",4,33f,4.2f,.36f,"Gancho Elétrico","Laço Temporal","Sobrecarga",0x68E7FF,3),
        w("frost_rail","Trilho Glacial","Precisão perfurante",1,63f,12f,.56f,"Lança Congelante","Frente Fria","Escudo Térmico",0x9FE7FF,4),
        w("solar_lance","Lança Solar","Rajada luminosa",1,49f,10.8f,.43f,"Raio Solar","Explosão Solar","Luz Restauradora",0xFFD76B,3),
        w("grav_hammer","Martelo Graviton","Impacto pesado",3,70f,3.0f,.72f,"Salto Graviton","Campo de Peso","Reparo Pesado",0xA99BFF,4),
        w("drone_staff","Bastão Drone","Suporte tático",4,28f,7.5f,.42f,"Microdrones","Zona Segura","Cura Máxima",0x7CFFA9,3),
        w("plasma_twins","Gêmeas de Plasma","Dano veloz",0,43f,2.7f,.24f,"Corte Duplo","Dança de Plasma","Sifão de Vida",0xFF72BC,3),
        w("vortex_scepter","Cetro Vórtice","Controle à distância",4,39f,9.5f,.46f,"Mini Vórtice","Colapso","Regeneração",0x8FA8FF,3),
        w("meteor_knuckle","Punho Meteoro","Burst pesado",3,78f,2.5f,.80f,"Meteoro","Cratera","Pulso de Cura",0xFF996A,4),
        w("aether_rifle","Rifle Aether","Dano sustentado",1,44f,12f,.32f,"Ráfaga Longa","Marca Aether","Auto-Cura",0x5BE9FF,3),
        w("bloom_blade","Lâmina Botânica","Dano + cura",0,41f,2.5f,.31f,"Pétalas","Raiz Pulsante","Flor Vital",0x80E89E,3),
        w("thunder_orb","Orbe Trovão","Área elétrica",2,47f,9f,.48f,"Três Raios","Tempestade","Campo Regenerativo",0x7EC8FF,4),
        w("mono_drill","Broca Monolítica","Perfuração pesada",3,74f,3.1f,.73f,"Perfuração","Tremor","Reparo",0xF0B66B,4),
        w("starlight_fan","Leque Estelar","Rajada híbrida",1,40f,10f,.30f,"Cinco Estrelas","Arco Celeste","Brilho Vital",0xE2D2FF,3),
        w("phase_blaster","Blaster Fásico","Disparo dimensional",1,51f,12f,.39f,"Salto Fásico","Ruptura","Escudo Fásico",0x91F2FF,4),
        w("ember_halo","Halo de Brasa","Controle de área",2,52f,7.5f,.54f,"Anel Incandescente","Muralha de Fogo","Cura de Cinzas",0xFF8C5A,4),
        w("kinetic_scythe","Foice Cinética","Dano amplo",0,57f,3.5f,.45f,"Giro Cinético","Corte Crescente","Impulso Vital",0xD0A8FF,4)
    )

    private fun w(id: String,name: String,role: String,archetype: Int,damage: Float,range: Float,cooldown: Float,
                  skill1: String,skill2: String,skill3: String,tint: Int,rarity: Int) =
        OfflineWeaponDefinition(id,name,role,archetype,damage,range,cooldown,skill1,skill2,skill3,tint,rarity)
}
