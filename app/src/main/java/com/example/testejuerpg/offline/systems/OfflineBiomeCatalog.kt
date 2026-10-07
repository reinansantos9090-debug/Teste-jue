package com.example.testejuerpg.offline.systems

data class OfflineBiome(
    val id: String,
    val name: String,
    val seed: Int,
    val groundR: Float, val groundG: Float, val groundB: Float,
    val accentR: Float, val accentG: Float, val accentB: Float,
    val fogR: Float, val fogG: Float, val fogB: Float,
    val obstacleDensity: Float,
    val ambientIntensity: Float
)

object OfflineBiomeCatalog {
    val all: List<OfflineBiome> = listOf(
        OfflineBiome("prism_garden","Jardins do Prisma",101,0.13f,0.34f,0.23f,0.32f,0.88f,0.66f,0.04f,0.09f,0.11f,0.36f,1.00f),
        OfflineBiome("neon_forest","Floresta Neon",203,0.08f,0.24f,0.20f,0.24f,0.92f,0.50f,0.03f,0.07f,0.12f,0.50f,0.96f),
        OfflineBiome("plasma_marsh","Pântano de Plasma",307,0.12f,0.25f,0.32f,0.32f,0.67f,1.00f,0.04f,0.06f,0.14f,0.43f,0.92f),
        OfflineBiome("scrap_ruins","Ruínas de Sucata",401,0.19f,0.20f,0.23f,0.62f,0.68f,0.78f,0.07f,0.08f,0.12f,0.58f,0.88f),
        OfflineBiome("magnetic_canyon","Cânion Magnético",503,0.25f,0.19f,0.18f,1.00f,0.48f,0.25f,0.11f,0.05f,0.07f,0.46f,0.94f),
        OfflineBiome("aurora_dome","Domo da Aurora",601,0.15f,0.22f,0.38f,0.55f,0.86f,1.00f,0.04f,0.05f,0.16f,0.32f,1.05f),
        OfflineBiome("crystal_vale","Vale de Cristal",701,0.18f,0.28f,0.34f,0.53f,0.75f,1.00f,0.05f,0.08f,0.13f,0.34f,1.02f),
        OfflineBiome("magnetic_plain","Planície Magnética",809,0.20f,0.24f,0.28f,0.80f,0.58f,1.00f,0.06f,0.08f,0.13f,0.27f,1.00f),
        OfflineBiome("vortex_canyon","Cânion do Vórtice",907,0.22f,0.16f,0.28f,0.76f,0.38f,1.00f,0.08f,0.04f,0.12f,0.48f,0.90f),
        OfflineBiome("memory_desert","Deserto da Memória",1009,0.37f,0.29f,0.18f,1.00f,0.73f,0.34f,0.12f,0.09f,0.05f,0.22f,0.91f),
        OfflineBiome("origin_chamber","Câmara de Origem",1103,0.12f,0.12f,0.18f,0.72f,0.42f,1.00f,0.03f,0.03f,0.08f,0.18f,1.10f),
        OfflineBiome("blue_void","Vazio Azul",1201,0.04f,0.08f,0.20f,0.22f,0.60f,1.00f,0.02f,0.04f,0.14f,0.20f,0.98f)
    )
    fun forIndex(index: Int): OfflineBiome = all[index.mod(all.size)]
    fun forWorld(name: String): OfflineBiome {
        val hash = name.fold(17) { acc, c -> acc * 31 + c.code }
        return forIndex(kotlin.math.abs(hash))
    }
}
