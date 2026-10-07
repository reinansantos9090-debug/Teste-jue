package com.example.testejuerpg.offline.systems

data class OfflineBossDefinition(
    val id: String,
    val name: String,
    val maxHp: Float,
    val baseSpeed: Float,
    val contactDamage: Float,
    val phase2Multiplier: Float,
    val projectileInterval: Float,
    val summonInterval: Float,
    val projectileDamage: Float,
    val body: Int,
    val aura: Int
)

object OfflineBossCatalog {
    val all: List<OfflineBossDefinition> = listOf(
        b("overload_titan","Titã de Sobrecarga",900f,.78f,28f,1.50f,1.60f,10f,22f,0xAA65FF,0xFF4C8F),
        b("prism_sentinel","Sentinela Prismática",980f,.88f,31f,1.45f,1.25f,11f,24f,0x63D8FF,0xD8A7FF),
        b("magnetic_colossus","Colosso Magnético",1180f,.70f,36f,1.55f,1.90f,9f,28f,0xA98C72,0xFF8A5C),
        b("inverted_king","Rei Invertido",1080f,.96f,34f,1.60f,1.05f,8f,26f,0x5B5A8F,0xF05C9A),
        b("horizon_titan","Titã de Horizonte",1320f,.82f,40f,1.50f,.90f,8f,30f,0x5A8BFF,0x72F1FF),
        b("aether_guardian","Guardião de Aetheria",1450f,.84f,44f,1.55f,1.15f,7f,32f,0x6AC39B,0xFFD15C),
        b("void_archon","Arconte do Vazio",1560f,.90f,47f,1.65f,.80f,7f,35f,0x454269,0xA876FF),
        b("storm_behemoth","Behemoth da Tempestade",1710f,.76f,52f,1.70f,.70f,6f,38f,0x4D8FB2,0x60E6FF)
    )
    fun forId(id: String): OfflineBossDefinition = all.firstOrNull { it.id == id } ?: all.first()
    fun forStoryName(name: String): OfflineBossDefinition = when (name) {
        "Sentinela Prismática" -> forId("prism_sentinel")
        "Colosso Magnético" -> forId("magnetic_colossus")
        "Rei Invertido" -> forId("inverted_king")
        "Titã de Horizonte" -> forId("horizon_titan")
        "Guardião de Aetheria" -> forId("aether_guardian")
        else -> forId("overload_titan")
    }
    private fun b(id: String,name: String,hp: Float,speed: Float,damage: Float,phase: Float,projectile: Float,summon: Float,projectileDamage: Float,body: Int,aura: Int) =
        OfflineBossDefinition(id,name,hp,speed,damage,phase,projectile,summon,projectileDamage,body,aura)
}
