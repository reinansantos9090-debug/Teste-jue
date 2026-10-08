package com.example.testejuerpg.game3d

data class V3(var x: Float, var y: Float, var z: Float) {
    fun set(other: V3) { x = other.x; y = other.y; z = other.z }
    fun add(dx: Float, dy: Float, dz: Float) { x += dx; y += dy; z += dz }
}

enum class SceneMode { HUB, HUNT, INVENTORY, MENU }

enum class EnemyKind(
    val displayName: String,
    val hp: Float,
    val attack: Float,
    val speed: Float,
    val radius: Float
) {
    AETHER_SLIME("Slime de Aether", 90f, 9f, 1.25f, 0.65f),
    NEON_STALKER("Perseguidor Neon", 125f, 13f, 1.85f, 0.55f),
    SCRAP_GOLEM("Golem de Sucata", 240f, 20f, 0.90f, 0.82f),
    PRISM_MOTH("Mariposa Prisma", 74f, 10f, 2.20f, 0.46f),
    SCRAP_DRONE("Drone de Sucata", 108f, 14f, 1.70f, 0.50f),
    PLASMA_EEL("Enguia de Plasma", 132f, 15f, 1.45f, 0.58f),
    VOID_BEETLE("Besouro do Vazio", 158f, 17f, 1.30f, 0.63f),
    AURORA_WRAITH("Espectro Aurora", 118f, 19f, 1.95f, 0.52f),
    MAGNET_HARE("Lebre Magnética", 101f, 12f, 2.40f, 0.48f),
    CRYSTAL_BRUTE("Bruto Cristalino", 285f, 24f, 0.78f, 0.90f),
    MEMORY_ECHO("Eco de Memória", 176f, 21f, 1.05f, 0.66f),
    PORTAL_LEECH("Sanguessuga de Portal", 148f, 18f, 1.55f, 0.56f),
    THORN_LING("Broto Espinhado", 72f, 11f, 1.40f, 0.52f),
    SAND_BOMBER("Bombardeiro das Dunas", 132f, 38f, 1.10f, 0.62f),
    PHASE_MOTH("Mariposa Fásica", 114f, 25f, 2.55f, 0.48f),
    MOSS_MENDER("Musgo Reparador", 105f, 8f, 1.25f, 0.58f),
    CRYSTAL_SENTINEL("Sentinela Cristalina", 310f, 32f, 0.72f, 0.92f),
    RIFT_ASSASSIN("Assassino da Fenda", 165f, 58f, 3.25f, 0.54f),
    MAGNET_TURRET("Torreta Magnética", 155f, 42f, 0.0f, 0.72f),
    ECHO_SPLITTER("Eco Saltador", 125f, 27f, 1.85f, 0.58f),
    OVERLOAD_TITAN("Titã de Sobrecarga", 900f, 28f, 0.78f, 1.55f)
}

data class WeaponPreset(
    val id: String,
    val name: String,
    val role: String,
    val archetype: Int,
    val mainDamage: Float,
    val mainRange: Float,
    val cooldown: Float,
    val skill1: String,
    val skill2: String,
    val skill3: String,
    val tint: Int
)

private val WEAPONS: List<WeaponPreset> = OfflineWeaponCatalog.all.map {
    WeaponPreset(it.id, it.name, it.role, it.archetype, it.damage, it.range, it.cooldown, it.skill1, it.skill2, it.skill3, it.tint)
}

data class EnemyEntity(
    val id: Int,
    val kind: EnemyKind,
    val pos: V3,
    var hp: Float,
    var attackTimer: Float = 0f,
    var hitFlash: Float = 0f,
    var poison: Float = 0f,
    var poisonTick: Float = 0f,
    var specialTimer: Float = 0f,
    var summonTimer: Float = 10f,
    var phase2: Boolean = false,
    var bossPhase: Int = 1,
    var dead: Boolean = false,
    var elite: Boolean = false,
    var bossProfileId: String = "overload_titan"
)

data class Projectile(
    val pos: V3,
    val vel: V3,
    val damage: Float,
    var life: Float,
    val playerOwned: Boolean
)

data class Drop(val pos: V3, val type: Int, val amount: Int = 1, val rarity: Int = 0, var life: Float = 30f)

data class Particle(
    val pos: V3,
    val vel: V3,
    var life: Float,
    val maxLife: Float,
    val scale: Float,
    val color: FloatArray
)

