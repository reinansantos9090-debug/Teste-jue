package com.example.testejuerpg.offline.systems

import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

data class MapObstacle(
    val x: Float,
    val z: Float,
    val halfX: Float,
    val halfZ: Float,
    val solid: Boolean = true
)

data class MapSpawnRegion(
    val minX: Float,
    val maxX: Float,
    val minZ: Float,
    val maxZ: Float
)

data class OfflineMapRuntime(
    val biomeId: String,
    val obstacles: List<MapObstacle>,
    val enemySpawns: List<MapSpawnRegion>,
    val worldRadius: Float
) {
    fun resolvePlayer(x: Float, z: Float, radius: Float): Pair<Float, Float> {
        var px = x.coerceIn(-worldRadius + radius, worldRadius - radius)
        var pz = z.coerceIn(-worldRadius + radius, worldRadius - radius)
        obstacles.filter { it.solid }.forEach { o ->
            val ix = px > o.x - o.halfX - radius && px < o.x + o.halfX + radius
            val iz = pz > o.z - o.halfZ - radius && pz < o.z + o.halfZ + radius
            if (!ix || !iz) return@forEach
            val dx = min(abs(px - (o.x - o.halfX)), abs(px - (o.x + o.halfX)))
            val dz = min(abs(pz - (o.z - o.halfZ)), abs(pz - (o.z + o.halfZ)))
            if (dx < dz) {
                px = if (px < o.x) o.x - o.halfX - radius else o.x + o.halfX + radius
            } else {
                pz = if (pz < o.z) o.z - o.halfZ - radius else o.z + o.halfZ + radius
            }
        }
        return px to pz
    }

    fun canSpawn(x: Float, z: Float, radius: Float): Boolean {
        if (
            x < -worldRadius + radius || x > worldRadius - radius ||
            z < -worldRadius + radius || z > worldRadius - radius
        ) return false
        return enemySpawns.any { region ->
            x in region.minX + radius..region.maxX - radius &&
                z in region.minZ + radius..region.maxZ - radius
        } && obstacles.none {
            it.solid &&
                x > it.x - it.halfX - radius &&
                x < it.x + it.halfX + radius &&
                z > it.z - it.halfZ - radius &&
                z < it.z + it.halfZ + radius
        }
    }

    fun findSpawn(random: Random, radius: Float): Pair<Float, Float>? {
        repeat(enemySpawns.size * 3) {
            val region = enemySpawns[random.nextInt(enemySpawns.size)]
            val x = radius + random.nextFloat() * (region.maxX - region.minX - radius * 2f) + region.minX
            val z = radius + random.nextFloat() * (region.maxZ - region.minZ - radius * 2f) + region.minZ
            if (canSpawn(x, z, radius)) return x to z
        }
        return null
    }

    companion object {
        fun forBiome(biomeId: String): OfflineMapRuntime = when (biomeId) {
            "prism_garden" -> map(
                biomeId,
                listOf(
                    MapObstacle(-5.2f, -2.8f, 1.4f, 2.1f),
                    MapObstacle(4.8f, -3.5f, 1.8f, 1.1f),
                    MapObstacle(-2.0f, 5.0f, 1.2f, 1.7f),
                    MapObstacle(6.1f, 5.4f, 1.0f, 1.4f),
                    MapObstacle(-7.0f, 6.4f, 1.6f, 0.9f),
                    MapObstacle(0.5f, -7.0f, 2.2f, 0.8f)
                ),
                listOf(
                    MapSpawnRegion(-15f, -7f, -14f, 14f),
                    MapSpawnRegion(7f, 15f, -12f, 12f)
                )
            )
            "neon_forest" -> map(
                biomeId,
                listOf(
                    MapObstacle(-7.5f, 0.0f, 1.0f, 4.8f),
                    MapObstacle(7.2f, 0.4f, 1.1f, 4.5f),
                    MapObstacle(0.0f, 6.5f, 4.7f, 1.0f),
                    MapObstacle(0.0f, -6.3f, 3.8f, 1.1f),
                    MapObstacle(-4.2f, 4.2f, 1.1f, 1.2f),
                    MapObstacle(4.6f, 3.7f, 1.2f, 1.0f),
                    MapObstacle(-4.7f, -4.4f, 1.0f, 1.2f)
                ),
                listOf(
                    MapSpawnRegion(-14f, -9f, -12f, 12f),
                    MapSpawnRegion(9f, 14f, -12f, 12f),
                    MapSpawnRegion(-7f, 7f, 9f, 14f)
                )
            )
            "plasma_marsh" -> map(
                biomeId,
                listOf(
                    MapObstacle(-6.0f, -5.0f, 2.4f, 1.0f),
                    MapObstacle(2.0f, -3.2f, 1.0f, 2.2f),
                    MapObstacle(7.0f, -1.0f, 1.2f, 2.8f),
                    MapObstacle(-5.5f, 3.6f, 1.7f, 0.9f),
                    MapObstacle(1.5f, 5.8f, 2.8f, 1.0f),
                    MapObstacle(6.5f, 5.0f, 1.0f, 1.5f)
                ),
                listOf(
                    MapSpawnRegion(-14f, -6f, -14f, -8f),
                    MapSpawnRegion(6f, 14f, -12f, -2f),
                    MapSpawnRegion(-13f, 14f, 8f, 14f)
                )
            )
            "scrap_ruins" -> map(
                biomeId,
                listOf(
                    MapObstacle(-8.0f, -4.8f, 2.0f, 1.0f),
                    MapObstacle(-3.3f, -1.0f, 1.0f, 2.7f),
                    MapObstacle(2.8f, -5.1f, 1.4f, 1.0f),
                    MapObstacle(7.5f, -2.0f, 1.0f, 2.6f),
                    MapObstacle(-6.0f, 4.8f, 2.6f, 0.9f),
                    MapObstacle(0.2f, 5.0f, 1.2f, 1.2f),
                    MapObstacle(5.0f, 6.0f, 2.0f, 1.1f),
                    MapObstacle(0.0f, -8.0f, 3.0f, 0.8f)
                ),
                listOf(
                    MapSpawnRegion(-14f, -8f, -14f, -3f),
                    MapSpawnRegion(8f, 14f, -13f, 4f),
                    MapSpawnRegion(-14f, 14f, 8f, 14f)
                )
            )
            "vortex_canyon" -> map(
                biomeId,
                listOf(
                    MapObstacle(-7.0f, -7.0f, 1.2f, 1.2f),
                    MapObstacle(-3.0f, -2.0f, 1.0f, 4.8f),
                    MapObstacle(3.0f, 2.5f, 1.1f, 5.2f),
                    MapObstacle(7.5f, 7.0f, 1.5f, 1.2f),
                    MapObstacle(-7.0f, 6.5f, 1.8f, 1.0f),
                    MapObstacle(6.5f, -6.2f, 1.6f, 1.0f)
                ),
                listOf(
                    MapSpawnRegion(-14f, -8f, -14f, 14f),
                    MapSpawnRegion(8f, 14f, -14f, 14f)
                )
            )
            "aurora_dome" -> map(
                biomeId,
                listOf(
                    MapObstacle(0.0f, 6.5f, 5.5f, 0.8f),
                    MapObstacle(-6.2f, 0.0f, 0.9f, 4.0f),
                    MapObstacle(6.2f, 0.0f, 0.9f, 4.0f),
                    MapObstacle(-3.7f, -5.2f, 1.5f, 1.0f),
                    MapObstacle(3.8f, -5.2f, 1.5f, 1.0f)
                ),
                listOf(
                    MapSpawnRegion(-14f, -8f, -13f, 13f),
                    MapSpawnRegion(8f, 14f, -13f, 13f),
                    MapSpawnRegion(-6f, 6f, 9f, 14f)
                )
            )
            "crystal_vale" -> map(
                biomeId,
                listOf(
                    MapObstacle(-6.0f, -4.5f, 1.1f, 2.2f),
                    MapObstacle(-1.7f, -6.0f, 1.0f, 1.0f),
                    MapObstacle(3.7f, -4.0f, 2.0f, 1.0f),
                    MapObstacle(6.0f, 1.6f, 1.0f, 2.4f),
                    MapObstacle(-4.8f, 4.4f, 2.0f, 1.0f),
                    MapObstacle(1.0f, 5.5f, 1.1f, 1.8f)
                ),
                listOf(
                    MapSpawnRegion(-14f, -8f, -14f, 14f),
                    MapSpawnRegion(8f, 14f, -13f, 7f),
                    MapSpawnRegion(-13f, 13f, 9f, 14f)
                )
            )
            "magnetic_plain" -> map(
                biomeId,
                listOf(
                    MapObstacle(-6.0f, 0.0f, 2.2f, 0.9f),
                    MapObstacle(0.0f, 5.3f, 1.0f, 2.4f),
                    MapObstacle(6.0f, 0.5f, 2.0f, 0.9f),
                    MapObstacle(0.0f, -5.5f, 1.0f, 2.0f),
                    MapObstacle(-4.0f, -4.2f, 1.2f, 1.2f),
                    MapObstacle(4.2f, 4.0f, 1.2f, 1.2f)
                ),
                listOf(
                    MapSpawnRegion(-14f, -8f, -14f, 14f),
                    MapSpawnRegion(8f, 14f, -14f, 14f),
                    MapSpawnRegion(-6f, 6f, 9f, 14f)
                )
            )
            else -> map(
                "prism_garden",
                listOf(
                    MapObstacle(-4f, -3f, 1.2f, 1.2f),
                    MapObstacle(4f, -2f, 1.2f, 1.2f),
                    MapObstacle(0f, 5f, 1.5f, 1.0f)
                ),
                listOf(
                    MapSpawnRegion(-14f, -8f, -12f, 12f),
                    MapSpawnRegion(8f, 14f, -12f, 12f)
                )
            )
        )

        private fun map(
            id: String,
            obstacles: List<MapObstacle>,
            spawns: List<MapSpawnRegion>
        ): OfflineMapRuntime = OfflineMapRuntime(
            biomeId = id,
            obstacles = obstacles,
            enemySpawns = spawns,
            worldRadius = 18f
        )
    }
}
