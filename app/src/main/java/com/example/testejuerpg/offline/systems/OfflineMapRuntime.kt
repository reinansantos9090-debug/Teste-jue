package com.example.testejuerpg.offline.systems

import kotlin.math.abs
import kotlin.math.min

data class MapObstacle(val x: Float,val z: Float,val halfX: Float,val halfZ: Float,val solid: Boolean = true)
data class MapSpawnRegion(val minX: Float,val maxX: Float,val minZ: Float,val maxZ: Float)

data class OfflineMapRuntime(
    val biomeId: String,
    val obstacles: List<MapObstacle>,
    val enemySpawns: List<MapSpawnRegion>,
    val worldRadius: Float
) {
    fun resolvePlayer(x: Float,z: Float,radius: Float): Pair<Float,Float> {
        var px=x.coerceIn(-worldRadius+radius,worldRadius-radius)
        var pz=z.coerceIn(-worldRadius+radius,worldRadius-radius)
        obstacles.filter { it.solid }.forEach { o ->
            val ix=px>o.x-o.halfX-radius && px<o.x+o.halfX+radius
            val iz=pz>o.z-o.halfZ-radius && pz<o.z+o.halfZ+radius
            if(!ix || !iz) return@forEach
            val dx=min(abs(px-(o.x-o.halfX)),abs(px-(o.x+o.halfX)))
            val dz=min(abs(pz-(o.z-o.halfZ)),abs(pz-(o.z+o.halfZ)))
            if(dx<dz) px=if(px<o.x) o.x-o.halfX-radius else o.x+o.halfX+radius
            else pz=if(pz<o.z) o.z-o.halfZ-radius else o.z+o.halfZ+radius
        }
        return px to pz
    }
    fun canSpawn(x:Float,z:Float,radius:Float):Boolean {
        if(x< -worldRadius+radius||x>worldRadius-radius||z< -worldRadius+radius||z>worldRadius-radius) return false
        return obstacles.none { it.solid && x>it.x-it.halfX-radius&&x<it.x+it.halfX+radius&&z>it.z-it.halfZ-radius&&z<it.z+it.halfZ+radius }
    }
    companion object {
        fun forBiome(biomeId:String):OfflineMapRuntime {
            val seed=biomeId.fold(0){a,c->(a*31+c.code) and 0x7FFFFFFF}
            val o=ArrayList<MapObstacle>()
            for(i in 0 until 10) o += MapObstacle(((seed/(i+3)%280)-140)/10f,((seed/(i+5)%280)-140)/10f,.45f+(i%3)*.22f,.45f+((i+1)%3)*.22f)
            return OfflineMapRuntime(biomeId,o,listOf(MapSpawnRegion(-15f,15f,-15f,15f)),18f)
        }
    }
}
