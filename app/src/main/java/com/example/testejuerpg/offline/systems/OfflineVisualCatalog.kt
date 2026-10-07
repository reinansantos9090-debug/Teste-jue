package com.example.testejuerpg.offline.systems

/**
 * Original visual direction for Aetheria: stylized, readable, colorful and offline.
 * The catalog contains only project-owned palette/layout hints; it does not embed
 * proprietary mo.co artwork or assets.
 */
data class StyleVisual(
    val id: String,
    val name: String,
    val body: Int,
    val suit: Int,
    val accent: Int,
    val headwear: Int,
    val ride: Int
)

data class BiomeVisual(
    val id: String,
    val landmarkCount: Int,
    val propScale: Float,
    val landmarkStyle: Int,
    val fogStrength: Float
)

object OfflineVisualCatalog {
    val styles: List<StyleVisual> = listOf(
        StyleVisual("style_00","Aurora Scout",0xF4C8A2.toInt(),0x2E6FD1.toInt(),0x73E4FF.toInt(),0xDDF7FF.toInt(),0x6B4CFF.toInt()),
        StyleVisual("style_01","Prism Runner",0xF0B995.toInt(),0x2DAB7F.toInt(),0xB5FFE4.toInt(),0xE4FFF8.toInt(),0x36D6C0.toInt()),
        StyleVisual("style_02","Neon Drifter",0xE9AE94.toInt(),0x4E3DBA.toInt(),0x63F1FF.toInt(),0xD8D8FF.toInt(),0x7D5CFF.toInt()),
        StyleVisual("style_03","Scrap Ranger",0xDFAF89.toInt(),0x555E6B.toInt(),0xFFB76A.toInt(),0xE3E7EC.toInt(),0xE17B3B.toInt()),
        StyleVisual("style_04","Plasma Diver",0xF2C49C.toInt(),0x207A91.toInt(),0x58E4FF.toInt(),0xCBF7FF.toInt(),0x1BB4D8.toInt()),
        StyleVisual("style_05","Void Scholar",0xD9A88E.toInt(),0x3C3A56.toInt(),0xC58BFF.toInt(),0xE9DCFF.toInt(),0x8759E8.toInt()),
        StyleVisual("style_06","Magnet Knight",0xE6B993.toInt(),0x634B36.toInt(),0xFF8C69.toInt(),0xFFE2B5.toInt(),0xD35D42.toInt()),
        StyleVisual("style_07","Crystal Gardener",0xF4CAA8.toInt(),0x3F8D70.toInt(),0xA5F3E7.toInt(),0xE8FFF5.toInt(),0x56D8B7.toInt()),
        StyleVisual("style_08","Thunder Courier",0xF0BB9E.toInt(),0x285278.toInt(),0xFFDA5A.toInt(),0xFFF4A4.toInt(),0xF1A92C.toInt()),
        StyleVisual("style_09","Memory Wanderer",0xE1AC92.toInt(),0x7E5A46.toInt(),0xFFD28A.toInt(),0xFFECC5.toInt(),0xC5914F.toInt()),
        StyleVisual("style_10","Origin Diver",0xDEB193.toInt(),0x31354F.toInt(),0xA98BFF.toInt(),0xDCD7FF.toInt(),0x6550C8.toInt()),
        StyleVisual("style_11","Horizon Hero",0xF3C39F.toInt(),0x315A4D.toInt(),0x75FFB8.toInt(),0xE0FFEF.toInt(),0x40C989.toInt())
    )

    val biomes: List<BiomeVisual> = listOf(
        BiomeVisual("prism_garden",8,1.0f,0,0.90f),
        BiomeVisual("neon_forest",10,1.15f,1,0.96f),
        BiomeVisual("plasma_marsh",8,0.90f,2,1.02f),
        BiomeVisual("scrap_ruins",12,1.05f,3,1.10f),
        BiomeVisual("magnetic_canyon",9,1.25f,4,1.06f),
        BiomeVisual("aurora_dome",7,1.15f,5,0.92f),
        BiomeVisual("crystal_vale",12,0.90f,6,0.88f),
        BiomeVisual("magnetic_plain",8,1.20f,4,0.94f),
        BiomeVisual("vortex_canyon",10,1.22f,4,1.12f),
        BiomeVisual("memory_desert",8,1.30f,7,1.06f),
        BiomeVisual("origin_chamber",9,0.80f,8,0.86f),
        BiomeVisual("blue_void",9,0.80f,8,0.80f)
    )

    fun forStyle(index: Int): StyleVisual = styles[index.mod(styles.size)]
    fun forBiome(id: String): BiomeVisual =
        biomes.firstOrNull { it.id == id } ?: biomes.first()
}
