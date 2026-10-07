package com.example.testejuerpg.offline.systems

data class OfflineWeaponDefinition(
    val id: String, val name: String, val role: String, val power: Int,
    val attackSpeed: Float, val range: Float, val skills: List<String>
)
data class OfflineAbilityDefinition(
    val id: String, val name: String, val category: String, val power: Int,
    val cooldown: Float, val description: String
)
data class OfflineMonsterDefinition(
    val id: String, val name: String, val hp: Int, val attack: Int,
    val speed: Float, val archetype: String, val biome: String
)
data class OfflineBossDefinition(
    val id: String, val name: String, val hp: Int, val phases: Int,
    val biome: String, val signature: String
)
object OfflineCombatCatalog {
    private val weaponSeeds = listOf(
        "Lâminas Voltáicas","Arco Tóxico","Canhão Pulsar","Martelo Sucateiro",
        "Lanças Prismáticas","Foice de Vórtice","Garras de Plasma","Discos Boreais",
        "Bastão de Gravidade","Rifle de Aurora","Escudo Reator","Chakram Neon",
        "Machado Magnético","Lançador de Esporos","Lâmina de Eco","Canhão de Cristal",
        "Arco de Tempestade","Manoplas de Sucata","Lança de Horizonte","Pistola de Prisma",
        "Cajado da Maré","Serra Orbital","Arpão Aether","Fuzil de Fagulha",
        "Marreta de Íon","Foice de Memória","Pistola de Vácuo","Lâmina Lunar",
        "Martelo de Campo","Carabina Azul","Discos de Cinza","Arco de Refração",
        "Lanças de Plasma","Glaive Magnético","Canhão de Névoa","Bumerangue Prismático",
        "Machado de Aurora","Rifle do Vórtice","Punhos de Tempestade","Cortador Solar",
        "Lança-Fios","Canhão de Pulso","Espada de Eco","Arco de Geada",
        "Marreta de Cristal","Pistola de Frequência","Foice do Deserto","Lâmina do Farol",
        "Cajado de Gravidade","Rifle da Origem","Chakram do Horizonte","Martelo do Santuário",
        "Canhão de Ruína","Garras da Aurora","Arco do Vazio","Espada Prismática",
        "Machado do Eco","Lançador Magnético","Foice Neon","Bastão do Prisma",
        "Pistola do Cânion","Lâmina da Cidade Espelhada","Canhão do Farol","Martelo do Portal"
    )
    val weapons: List<OfflineWeaponDefinition> = weaponSeeds.mapIndexed { index, name ->
        val role = listOf("melee","ranged","area","support")[index % 4]
        val skills = listOf("Impulso " + (index + 1), "Explosão " + (index + 2), "Ascensão " + (index + 3))
        OfflineWeaponDefinition("weapon_catalog_" + (index + 1), name, role,
            70 + (index % 17) * 6, 0.22f + (index % 9) * 0.035f,
            2.5f + (index % 8) * 1.1f, skills)
    }
    val abilities: List<OfflineAbilityDefinition> = List(96) { index ->
        val category = listOf("mobility","burst","control","healing","summon","ultimate")[index % 6]
        OfflineAbilityDefinition(
            "ability_catalog_" + (index + 1), "Poder " + (index + 1), category,
            30 + (index % 21) * 7, 2.0f + (index % 10) * 0.75f,
            "Habilidade offline " + category + " com telemetria de impacto e execução determinística."
        )
    }
    val monsters: List<OfflineMonsterDefinition> = List(48) { index ->
        val archetype = listOf("swarm","charger","ranged","tank","assassin","controller")[index % 6]
        val biome = OfflineBiomeCatalog.forIndex(index).id
        OfflineMonsterDefinition(
            "monster_catalog_" + (index + 1),
            archetype.replaceFirstChar { it.uppercase() } + " " + (index + 1),
            80 + (index % 19) * 35, 8 + (index % 14) * 3,
            0.8f + (index % 11) * 0.12f, archetype, biome
        )
    }
    val bosses: List<OfflineBossDefinition> = List(16) { index ->
        OfflineBossDefinition(
            "boss_catalog_" + (index + 1),
            listOf(
                "Sentinela Prismática","Colosso Magnético","Rei Invertido","Titã de Horizonte",
                "Guardião de Aetheria","Matriz Boreal","Arquiteto Neon","Fera do Vórtice",
                "Titã do Farol","Monarca de Cristal","Coração de Plasma","Arauto do Vazio",
                "Executor de Sucata","Guardião Espelhado","Coroa da Aurora","Núcleo Absoluto"
            )[index],
            900 + index * 180, 2 + index % 3, OfflineBiomeCatalog.forIndex(index + 2).id,
            listOf("charge","arena","summon","projectile","teleport","gravity")[index % 6]
        )
    }
    fun weapon(id: String): OfflineWeaponDefinition? = weapons.firstOrNull { it.id == id }
    fun monster(id: String): OfflineMonsterDefinition? = monsters.firstOrNull { it.id == id }
    fun boss(id: String): OfflineBossDefinition? = bosses.firstOrNull { it.id == id }
}
