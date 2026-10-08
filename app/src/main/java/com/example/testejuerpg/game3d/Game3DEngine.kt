package com.example.testejuerpg.game3d

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import com.example.testejuerpg.offline.systems.OfflineHunterDirector
import com.example.testejuerpg.offline.systems.OfflineRooftopController
import com.example.testejuerpg.offline.systems.RooftopScreen
import com.example.testejuerpg.offline.OfflineMode
import com.example.testejuerpg.offline.systems.OfflineStoryCampaign
import com.example.testejuerpg.offline.systems.OfflineBiomeCatalog
import com.example.testejuerpg.offline.systems.OfflineVisualCatalog
import com.example.testejuerpg.offline.systems.OfflineWeaponCatalog
import com.example.testejuerpg.offline.systems.OfflineBossCatalog
import com.example.testejuerpg.offline.systems.OfflineMapRuntime
import com.example.testejuerpg.offline.systems.OfflineSquadCatalog
import com.example.testejuerpg.offline.systems.OfflineHistoryArchive
import com.example.testejuerpg.offline.systems.OfflineEmoteCatalog


data class MapObstacleView(val x: Float, val z: Float, val halfX: Float, val halfZ: Float)

class Game3DEngine(private val context: Context) {
    @Volatile var scene: SceneMode = SceneMode.HUB
        private set
    private var inventoryReturnScene: SceneMode = SceneMode.HUB

    @Volatile var playerName: String = "Caçador"
        private set
    @Volatile var level: Int = 1
        private set
    @Volatile var xp: Float = 0f
        private set
    @Volatile var xpToNext: Float = 100f
        private set
    @Volatile var hp: Float = 150f
        private set
    @Volatile var maxHp: Float = 150f
        private set
    @Volatile var gold: Int = 50
        private set
    @Volatile var aetherCores: Int = 0
        private set
    @Volatile var kills: Int = 0
        private set
    @Volatile var weaponIndex: Int = 1
        private set
    @Volatile var objectiveText: String = "Portal pronto • escolha uma caçada"
        private set
    @Volatile var bossActive: Boolean = false
        private set
    @Volatile var bossHp: Float = 900f
        private set
    @Volatile var bossMaxHp: Float = 900f
        private set
    var isDefeated: Boolean = false
        private set

    val player = V3(0f, 0.8f, 5f)
    private val enemies = CopyOnWriteArrayList<EnemyEntity>()
    private val projectiles = CopyOnWriteArrayList<Projectile>()
    private val drops = CopyOnWriteArrayList<Drop>()
    private val particles = CopyOnWriteArrayList<Particle>()
    private val random = Random(8107)
    private val vibration: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val prefs = context.getSharedPreferences("teste_jue_3d", Context.MODE_PRIVATE)
    private val hunterDirector = OfflineHunterDirector(2025)
    private val rooftopController = OfflineRooftopController(hunterDirector)
    private val storyCampaign = OfflineStoryCampaign(context)
    private val renderProfile = MobileRenderProfile.detect(context)
    private val performanceGovernor = MobilePerformanceGovernor(renderProfile)
    private val performanceTelemetry = OfflinePerformanceTelemetry()
    private val audioBus = OfflineAudioBus()
    private val historyArchive = OfflineHistoryArchive()
    private var mapRuntime = OfflineMapRuntime.forBiome("prism_garden")

    private var activityMode = OfflineMode.EXPEDITION
    private var activityTarget = 12
    private var activityCompleted = false

    private var moveX = 0f
    private var moveY = 0f
    private var primaryTimer = 0f
    private val skillTimers = floatArrayOf(0f, 0f, 0f)
    private var waveTimer = 0f
    private var storyWaveTimer = 0f
    private var squadTimer = 0f
    private var emoteTimer = 0f
    private var activeEmote = 0
    private var nextEnemyId = 1
    private var lastTick = SystemClock.elapsedRealtime()
    private var paused = false
    private var huntKills = 0
    private var bossSpawned = false
    private var bossDead = false
    private var bossPatternTime = 0f
    private var storyVictoryTimer = 0f
    var screenShake = 0f
        private set
    var onUiInvalidate: (() -> Unit)? = null

    init {
        load()
        hunterDirector.loadFrom(prefs)
        historyArchive.loadFrom(prefs)
        activeEmote = prefs.getInt("active_emote", 0).coerceIn(0, OfflineEmoteCatalog.all.lastIndex)
    }

    @Synchronized fun resume() {
        paused = false
        lastTick = SystemClock.elapsedRealtime()
    }

    @Synchronized fun pauseAndSave() {
        paused = true
        save()
    }

    @Synchronized fun tick() {
        if (paused) return
        val now = SystemClock.elapsedRealtime()
        var dt = (now - lastTick) / 1000f
        lastTick = now
        dt = dt.coerceIn(0.008f, 0.05f)

        primaryTimer = max(0f, primaryTimer - dt)
        for (i in 0..2) skillTimers[i] = max(0f, skillTimers[i] - dt)

        hunterDirector.tick(dt)
        if (scene == SceneMode.HUNT && !isDefeated) {
            updatePlayer(dt)
            updateEnemies(dt)
            updateProjectiles(dt)
            updateDrops(dt)
            updateParticles(dt)
            updateSquad(dt)
            updateWave(dt)
            if (screenShake > 0f) screenShake = max(0f, screenShake - dt * 2.8f)
            emoteTimer = max(0f, emoteTimer - dt)
            objectiveText = when (activityMode) {
                OfflineMode.EXPEDITION -> when {
                    bossActive -> "Derrote o Titã • Núcleos " + aetherCores + "/12"
                    huntKills < 12 -> "Tarefa: " + (12 - huntKills) + " monstros restantes"
                    aetherCores < 12 -> "Tarefa: colete " + (12 - aetherCores) + " Núcleos"
                    else -> "Portal do Titã surgiu • prepare-se"
                }
                OfflineMode.RIFT_BOSS -> if (bossActive) "RIFT • derrote o chefe antes do tempo" else "RIFT • preparando chefe"
                OfflineMode.RIFT_ARENA -> if (!activityCompleted) "RIFT ARENA • " + huntKills + "/" + activityTarget else "RIFT CONCLUÍDO • retorne ao QG"
                OfflineMode.VERSUS_SIM -> if (!activityCompleted) "VERSUS • score " + rooftopController.versusScore() else "VERSUS VENCIDO • retorne ao QG"
                OfflineMode.EVENT -> if (!activityCompleted) "EVENTO • " + rooftopController.objectiveSummary() else "EVENTO CONCLUÍDO • retorne ao QG"
                OfflineMode.TRAINING -> "TREINO • dano livre"
            OfflineMode.STORY -> storyObjectiveText()
            }
        }
        onUiInvalidate?.invoke()
    }

    private fun updatePlayer(dt: Float) {
        val len = sqrt(moveX * moveX + moveY * moveY)
        val nx = if (len > 0.01f) moveX / max(1f, len) else 0f
        val nz = if (len > 0.01f) moveY / max(1f, len) else 0f
        val desiredX = player.x + nx * 4.5f * dt
        val desiredZ = player.z + nz * 4.5f * dt
        val resolved = mapRuntime.resolvePlayer(desiredX, desiredZ, 0.55f)
        player.x = resolved.first
        player.z = resolved.second
        if (bossActive) {
            val gateRadius = 12f
            val d = sqrt(player.x * player.x + player.z * player.z)
            if (d > gateRadius) {
                player.x *= gateRadius / d
                player.z *= gateRadius / d
            }
        }
    }

    private fun updateEnemies(dt: Float) {
        for (e in enemies) {
            if (e.dead) continue
            e.hitFlash = max(0f, e.hitFlash - dt)
            if (e.kind == EnemyKind.OVERLOAD_TITAN) {
                val profile = OfflineBossCatalog.forId(e.bossProfileId)
                e.specialTimer = max(0f, e.specialTimer - dt)
                e.summonTimer = max(0f, e.summonTimer - dt)
                val ratio = e.hp / max(1f, bossMaxHp)
                val newPhase = when {
                    ratio <= 0.30f -> 3
                    ratio <= 0.62f -> 2
                    else -> 1
                }
                if (newPhase != e.bossPhase) {
                    e.bossPhase = newPhase
                    e.phase2 = newPhase >= 2
                    screenShake = 0.28f
                    spawnBurst(e.pos, 0.70f, rgbColor(profile.aura))
                    vibrate(60)
                }
                bossPatternTime += dt
                if (e.specialTimer <= 0f) {
                    executeBossPattern(e, profile)
                }
                if (e.summonTimer <= 0f && enemies.count { !it.dead } < min(12, performanceGovernor.allowedEnemies())) {
                    e.summonTimer = max(4.0f, profile.summonInterval * (if (e.bossPhase == 3) 0.58f else if (e.bossPhase == 2) 0.78f else 1f))
                    val summonCount = if (e.bossPhase == 3) 3 else if (e.bossPhase == 2) 2 else 1
                    repeat(summonCount) { spawnEnemy() }
                    spawnBurst(e.pos, 0.35f, rgbColor(profile.aura))
                }
            }
            if (e.poison > 0f) {
                e.poison -= dt
                e.poisonTick -= dt
                if (e.poisonTick <= 0f) {
                    e.poisonTick = 0.6f
                    e.hp -= 8f
                    spawnBurst(e.pos, 0.12f, floatArrayOf(0.45f, 1f, 0.35f))
                }
            }

            val dx = player.x - e.pos.x
            val dz = player.z - e.pos.z
            val dist = sqrt(dx * dx + dz * dz)
            e.attackTimer = max(0f, e.attackTimer - dt)
            e.specialTimer = max(0f, e.specialTimer - dt)

            when (e.kind) {
                EnemyKind.MAGNET_TURRET -> {
                    if (e.attackTimer <= 0f && dist < 20f) {
                        e.attackTimer = 2.20f
                        spawnEnemyProjectile(e, 1.25f, 7.0f)
                        spawnBurst(e.pos, 0.12f, floatArrayOf(1f,0.55f,0.30f))
                    }
                }
                EnemyKind.MOSS_MENDER -> {
                    if (e.specialTimer <= 0f) {
                        e.specialTimer = 4.2f
                        enemies.asSequence()
                            .filter { it != e && !it.dead }
                            .filter { it.pos.distanceSquared(e.pos) < 32f }
                            .take(2)
                            .forEach { ally -> ally.hp = min(ally.kind.hp * 1.45f, ally.hp + ally.kind.hp * 0.16f) }
                        spawnBurst(e.pos, 0.18f, floatArrayOf(0.35f,1f,0.55f))
                    }
                    moveEnemyToward(e, dx, dz, dist, dt, 1.0f)
                }
                EnemyKind.SAND_BOMBER -> {
                    if (dist < 3.2f && e.specialTimer <= 0f) {
                        e.specialTimer = 4f
                        takeDamage(e.kind.attack * 1.35f)
                        spawnBurst(player,0.45f,floatArrayOf(1f,0.42f,0.18f))
                    } else if (dist > 5.5f) {
                        moveEnemyToward(e,dx,dz,dist,dt,0.85f)
                    }
                    if (e.attackTimer <= 0f && dist < 18f) {
                        e.attackTimer = 2.6f
                        spawnEnemyProjectile(e,1.05f,5.4f)
                    }
                }
                EnemyKind.PHASE_MOTH -> {
                    val orbitX = -dz
                    val orbitZ = dx
                    if (e.specialTimer <= 0f && dist < 13f) {
                        e.specialTimer = 3.1f
                        val inv = 1f / max(0.001f, dist)
                        e.pos.x = player.x - dx * inv * 5.5f
                        e.pos.z = player.z - dz * inv * 5.5f
                    } else {
                        val inv = 1f / max(0.001f, sqrt(orbitX*orbitX+orbitZ*orbitZ))
                        e.pos.x += orbitX * inv * e.kind.speed * dt
                        e.pos.z += orbitZ * inv * e.kind.speed * dt
                    }
                    if (dist < 2.2f && e.attackTimer <= 0f) {
                        e.attackTimer=1.0f
                        takeDamage(e.kind.attack * 1.15f)
                    }
                }
                EnemyKind.RIFT_ASSASSIN -> {
                    if (e.specialTimer <= 0f && dist < 14f) {
                        e.specialTimer=2.8f
                        val inv=1f/max(0.001f,dist)
                        e.pos.x=player.x+dx*inv*2.4f
                        e.pos.z=player.z+dz*inv*2.4f
                        screenShake=0.12f
                        spawnBurst(e.pos,0.20f,floatArrayOf(0.65f,0.35f,1f))
                    } else {
                        moveEnemyToward(e,dx,dz,dist,dt,1.35f)
                    }
                    if (dist < 2.0f && e.attackTimer <= 0f) {
                        e.attackTimer=0.8f
                        takeDamage(e.kind.attack * 1.3f)
                    }
                }
                else -> {
                    if (dist > 1.6f + e.kind.radius) {
                        moveEnemyToward(e,dx,dz,dist,dt,if (e.kind == EnemyKind.CRYSTAL_SENTINEL) 0.62f else 1.0f)
                    } else if (e.attackTimer <= 0f) {
                        e.attackTimer=if (e.kind == EnemyKind.CRYSTAL_SENTINEL) 1.8f else 1.5f
                        val contactDamage=if (e.kind == EnemyKind.CRYSTAL_SENTINEL) e.kind.attack*0.82f else e.kind.attack
                        takeDamage(contactDamage*if(e.elite)1.25f else 1f)
                    }
                }
            }

            if (e.hp <= 0f) killEnemy(e)
        }
    }

    private fun updateProjectiles(dt: Float) {
        val remove = ArrayList<Projectile>()
        for (p in projectiles) {
            p.pos.add(p.vel.x * dt, p.vel.y * dt, p.vel.z * dt)
            p.life -= dt
            if (p.life <= 0f) {
                remove.add(p)
                continue
            }

            if (p.playerOwned) {
                for (e in enemies) {
                    if (e.dead) continue
                    val dx = p.pos.x - e.pos.x
                    val dz = p.pos.z - e.pos.z
                    val d2 = dx * dx + dz * dz
                    val hitR = e.kind.radius + 0.34f
                    if (d2 <= hitR * hitR) {
                        e.hp -= p.damage
                        e.hitFlash = 0.12f
                        spawnBurst(e.pos, 0.10f, floatArrayOf(0.55f, 0.82f, 1f))
                        remove.add(p)
                        if (e.hp <= 0f) killEnemy(e)
                        break
                    }
                }
            } else {
                val dx = p.pos.x - player.x
                val dz = p.pos.z - player.z
                if (dx * dx + dz * dz <= 0.60f * 0.60f) {
                    takeDamage(p.damage)
                    remove.add(p)
                    spawnBurst(player, 0.16f, floatArrayOf(1f, 0.25f, 0.45f))
                }
            }
        }
        projectiles.removeAll(remove)
    }

    private fun updateDrops(dt: Float) {
        for (d in drops) {
            d.life -= dt
            val dx = d.pos.x - player.x
            val dz = d.pos.z - player.z
            if (dx * dx + dz * dz < 1.69f) {
                if (d.type == 0) {
                    aetherCores += 1
                    hunterDirector.recordCore(1)
                    if (activityMode == OfflineMode.STORY) {
                        storyCampaign.recordCore()
                        checkStoryObjective()
                    }
                } else gold += 8
                spawnBurst(
                    d.pos,
                    0.14f,
                    if (d.type == 0) floatArrayOf(0.35f, 0.95f, 1f)
                    else floatArrayOf(1f, 0.82f, 0.25f)
                )
                d.life = -1f
            }
        }
        drops.removeAll { it.life <= 0f }
    }

    private fun updateParticles(dt: Float) {
        for (p in particles) {
            p.life -= dt
            p.pos.add(p.vel.x * dt, p.vel.y * dt, p.vel.z * dt)
            p.vel.y -= 2.8f * dt
        }
        particles.removeAll { it.life <= 0f }
    }

    private fun rgbColor(hex: Int): FloatArray = floatArrayOf(
        ((hex shr 16) and 255) / 255f,
        ((hex shr 8) and 255) / 255f,
        (hex and 255) / 255f
    )

    private fun updateSquad(dt: Float) {
        if (scene != SceneMode.HUNT || isDefeated || activityMode == OfflineMode.TRAINING) return
        squadTimer += dt
        if (squadTimer < 1.05f) return
        squadTimer = 0f
        OfflineSquadCatalog.all.forEachIndexed { index, member ->
            val phase = (SystemClock.elapsedRealtime() % 120000L) / 1000f * (0.9f + index * 0.12f) + index
            val target = nearestEnemy(11f)
            if (target != null) {
                val damage = 13f * member.damageMultiplier * (1f + level * 0.018f)
                target.hp -= damage
                target.hitFlash = 0.10f
                spawnBurst(target.pos, 0.08f, rgbColor(member.tint))
                if (target.hp <= 0f) killEnemy(target)
            }
            if (index == 1) hp = min(maxHp, hp + maxHp * 0.028f * member.healMultiplier)
            val point = mapRuntime.resolvePlayer(
                player.x + cos(phase) * (1.7f + index * 0.4f),
                player.z + sin(phase) * (1.7f + index * 0.4f),
                0.35f
            )
            memberOffsets[index].set(V3(point.first, 0.85f, point.second))
        }
    }

    private val memberOffsets = Array(OfflineSquadCatalog.all.size) { V3(0f, 0.85f, 0f) }

    private fun updateWave(dt: Float) {
        waveTimer += dt

        when (activityMode) {
            OfflineMode.EXPEDITION -> {
                if (!bossActive && !bossSpawned && huntKills >= 12 && aetherCores >= 12) spawnBoss()
                if (!bossActive && waveTimer > 2.8f && enemies.count { !it.dead } < 7 && huntKills < 12) {
                    waveTimer = 0f
                    spawnEnemy()
                    if (huntKills % 4 == 3) spawnEnemy(true)
                }
            }
            OfflineMode.RIFT_BOSS -> {
                if (!bossActive && !bossSpawned) spawnBoss()
            }
            OfflineMode.RIFT_ARENA, OfflineMode.VERSUS_SIM, OfflineMode.EVENT -> {
                if (!activityCompleted && waveTimer > 2.4f && enemies.count { !it.dead } < 7) {
                    waveTimer = 0f
                    spawnEnemy(activityMode == OfflineMode.VERSUS_SIM && huntKills % 3 == 2)
                    if (huntKills % 5 == 4) spawnEnemy(true)
                }
                if (!activityCompleted && huntKills >= activityTarget) {
                    activityCompleted = true
                    if (activityMode == OfflineMode.RIFT_ARENA) hunterDirector.recordRiftComplete()
                    else if (activityMode == OfflineMode.VERSUS_SIM) hunterDirector.recordExpeditionComplete()
                    else hunterDirector.recordRiftComplete()
                    gold += if (activityMode == OfflineMode.EVENT) 180 else 140
                    addXp(220f)
                    objectiveText = when (activityMode) {
                        OfflineMode.RIFT_ARENA -> "RIFT CONCLUÍDO • +140 Ouro"
                        OfflineMode.VERSUS_SIM -> "VERSUS VENCIDO • +140 Ouro"
                        else -> "EVENTO CONCLUÍDO • +180 Ouro"
                    }
                    save()
                }
            }
            OfflineMode.TRAINING -> Unit
            OfflineMode.STORY -> {
                if (storyCampaign.objectiveComplete()) {
                    checkStoryObjective()
                } else {
                    storyWaveTimer += dt
                    if (storyWaveTimer >= 1.75f && enemies.count { !it.dead } < min(6, performanceGovernor.allowedEnemies())) {
                        storyWaveTimer = 0f
                        val chapter = storyCampaign.currentChapter()
                        when (chapter.objectiveKind) {
                            "ELITE" -> spawnStoryEnemy(true)
                            "CORE", "ARENA" -> spawnStoryEnemy(false)
                            "BOSS" -> if (!bossSpawned) spawnBoss()
                            else -> spawnStoryEnemy(false)
                        }
                    }
                }
            }
        }

        if (bossActive) {
            val boss = enemies.firstOrNull { it.kind == EnemyKind.OVERLOAD_TITAN && !it.dead }
            bossHp = boss?.hp ?: 0f
            if (boss == null && bossSpawned && !bossDead) {
                bossDead = true
                bossActive = false
                bossHp = 0f
                if (activityMode == OfflineMode.STORY) {
                    checkStoryObjective()
                    if (!activityCompleted) {
                        objectiveText = "CHEFE DERROTADO • preparando conclusão do capítulo"
                    }
                    spawnBurst(player, 1.2f, floatArrayOf(0.7f, 0.85f, 1f))
                    save()
                } else {
                    val isRift = activityMode == OfflineMode.RIFT_BOSS
                    gold += if (isRift) 360 else 250
                    addXp(if (isRift) 420f else 300f)
                    if (isRift) hunterDirector.recordRiftComplete() else hunterDirector.recordExpeditionComplete()
                    objectiveText = if (isRift) "RIFT CONCLUÍDO • +360 Ouro" else "EXPEDIÇÃO CONCLUÍDA • +250 Ouro"
                    spawnBurst(player, 1.2f, floatArrayOf(0.7f, 0.85f, 1f))
                    save()
                }
            }
            if (boss != null && random.nextFloat() < dt * 0.12f && enemies.count { !it.dead } < 9) {
                spawnEnemy(false)
            }
        }
    }


    private fun moveEnemyToward(e: EnemyEntity, dx: Float, dz: Float, dist: Float, dt: Float, speedMultiplier: Float) {
        val inv = 1f / max(0.001f, dist)
        e.pos.x += dx * inv * e.kind.speed * speedMultiplier * dt
        e.pos.z += dz * inv * e.kind.speed * speedMultiplier * dt
    }

    private fun spawnEnemyProjectile(enemy: EnemyEntity, damageMultiplier: Float, speed: Float) {
        val dx = player.x - enemy.pos.x
        val dz = player.z - enemy.pos.z
        val distance = max(0.001f, sqrt(dx * dx + dz * dz))
        projectiles += Projectile(
            V3(enemy.pos.x, enemy.pos.y + 0.35f, enemy.pos.z),
            V3(dx / distance * speed, 0f, dz / distance * speed),
            enemy.kind.attack * damageMultiplier,
            3.0f,
            false
        )
    }


    private fun executeBossPattern(boss: EnemyEntity, profile: OfflineBossDefinition) {
        val phase = boss.bossPhase
        val id = profile.id
        val pattern = when (id) {
            "overload_titan" -> listOf("fan","charge","nova","fan","summon")
            "prism_sentinel" -> listOf("cross","fan","nova","cross","teleport")
            "magnetic_colossus" -> listOf("pull","charge","ring","pull","fan")
            "inverted_king" -> listOf("teleport","fan","cross","nova","charge")
            "horizon_titan" -> listOf("beam","rain","charge","fan","beam")
            "aether_guardian" -> listOf("charge","ring","summon","fan","nova")
            "void_archon" -> listOf("teleport","fan","void","summon","nova")
            "storm_behemoth" -> listOf("rain","fan","ring","summon","charge")
            else -> listOf("fan","charge","nova")
        }[(floor(bossPatternTime / 2.1f).toInt()) % 5]
        val damage = profile.projectileDamage * (if (phase == 3) 1.45f else if (phase == 2) 1.18f else 1f)
        when (pattern) {
            "fan" -> {
                val count = if (phase == 3) 5 else if (phase == 2) 3 else 2
                repeat(count) { index ->
                    val angle = (index - (count - 1) / 2f) * 0.24f
                    spawnBossProjectileSpread(boss, damage, angle)
                }
                spawnBurst(boss.pos, 0.26f, rgbColor(profile.aura))
            }
            "cross" -> {
                repeat(4) { index -> spawnBossProjectileSpread(boss, damage * 0.86f, index * (Math.PI.toFloat() / 2f)) }
            }
            "beam" -> {
                val target = nearestEnemy(0f)
                spawnEnemyAreaTelegraph(boss.pos, 3.8f + phase * 0.8f, damage * 0.75f)
                takeDamage(0f)
            }
            "rain" -> {
                repeat(if (phase == 3) 8 else 5) { index ->
                    val angle = (index * 1.37f + bossPatternTime) % 6.283f
                    val r = 3f + (index % 3) * 1.7f
                    val cx = player.x + cos(angle) * r
                    val cz = player.z + sin(angle) * r
                    spawnEnemyAreaTelegraph(V3(cx,0.3f,cz),1.5f + phase * 0.25f,damage * 0.42f)
                }
            }
            "ring" -> {
                spawnEnemyAreaTelegraph(boss.pos,3.5f + phase * 0.8f,damage * 0.65f)
                if (phase >= 2) spawnEnemyAreaTelegraph(boss.pos,6.2f + phase * 0.8f,damage * 0.35f)
            }
            "nova" -> spawnEnemyAreaTelegraph(player,4.2f + phase * 0.8f,damage * 0.78f)
            "charge" -> {
                val dx=player.x-boss.pos.x
                val dz=player.z-boss.pos.z
                val d=max(0.001f,sqrt(dx*dx+dz*dz))
                val distance=if(phase==3)7.0f else 5.0f
                boss.pos.x += dx/d*distance
                boss.pos.z += dz/d*distance
                spawnEnemyAreaTelegraph(boss.pos,2.3f + phase*0.35f,damage*0.72f)
                screenShake=0.18f
            }
            "teleport" -> {
                val angle=bossPatternTime*0.9f
                boss.pos.x=player.x+cos(angle)*6.4f
                boss.pos.z=player.z+sin(angle)*6.4f
                spawnEnemyAreaTelegraph(boss.pos,2.5f,damage*0.65f)
            }
            "pull" -> {
                val dx=player.x-boss.pos.x
                val dz=player.z-boss.pos.z
                val d=max(0.001f,sqrt(dx*dx+dz*dz))
                if(d>3f){ player.x -= dx/d*min(2.2f*dtSafe(),d-2f); player.z -= dz/d*min(2.2f*dtSafe(),d-2f) }
                spawnEnemyAreaTelegraph(boss.pos,5.4f,damage*0.35f)
            }
            "void" -> {
                spawnEnemyAreaTelegraph(player,5.2f,damage*0.60f)
                repeat(2){spawnBossProjectileSpread(boss,damage*0.90f,it*0.55f-0.28f)}
            }
            "summon" -> {
                repeat(if(phase==3)3 else 2){spawnEnemy()}
                spawnBurst(boss.pos,0.42f,rgbColor(profile.aura))
            }
        }
        boss.specialTimer=max(1.15f,profile.projectileInterval/(if(phase==3)1.65f else if(phase==2)1.25f else 1f))
    }

    private fun spawnBossProjectileSpread(boss: EnemyEntity, damage: Float, angle: Float) {
        val dx=player.x-boss.pos.x
        val dz=player.z-boss.pos.z
        val distance=max(0.001f,sqrt(dx*dx+dz*dz))
        val bx=dx/distance
        val bz=dz/distance
        val x=bx*cos(angle)-bz*sin(angle)
        val z=bx*sin(angle)+bz*cos(angle)
        projectiles += Projectile(
            V3(boss.pos.x,boss.pos.y,boss.pos.z),
            V3(x*7.2f,0f,z*7.2f),damage,3.1f,false
        )
    }

    private fun spawnEnemyAreaTelegraph(center: V3, radius: Float, damage: Float) {
        if (player.x-center.x <= radius && player.x-center.x >= -radius && player.z-center.z <= radius && player.z-center.z >= -radius) {
            takeDamage(damage)
        }
        spawnBurst(center, radius*0.12f, floatArrayOf(0.95f,0.34f,0.70f))
    }

    private fun dtSafe(): Float = (SystemClock.elapsedRealtime() - lastTick).coerceIn(1L,33L) / 1000f

    private fun spawnBossProjectile(boss: EnemyEntity) {
        val dx = player.x - boss.pos.x
        val dz = player.z - boss.pos.z
        val distance = max(0.001f, sqrt(dx * dx + dz * dz))
        projectiles += Projectile(
            V3(boss.pos.x, boss.pos.y, boss.pos.z),
            V3(dx / distance * 7.0f, 0f, dz / distance * 7.0f),
            OfflineBossCatalog.forId(boss.bossProfileId).projectileDamage,
            2.8f,
            false
        )
    }

    private fun spawnEnemy(elite: Boolean = false) {
        val choices = when {
            huntKills < 4 -> listOf(
                EnemyKind.AETHER_SLIME, EnemyKind.NEON_STALKER, EnemyKind.PRISM_MOTH,
                EnemyKind.THORN_LING
            )
            huntKills < 10 -> listOf(
                EnemyKind.AETHER_SLIME, EnemyKind.NEON_STALKER, EnemyKind.SCRAP_GOLEM,
                EnemyKind.SCRAP_DRONE, EnemyKind.PLASMA_EEL, EnemyKind.MAGNET_HARE,
                EnemyKind.THORN_LING, EnemyKind.MOSS_MENDER, EnemyKind.SAND_BOMBER,
                EnemyKind.PHASE_MOTH
            )
            else -> EnemyKind.entries.filter { it != EnemyKind.OVERLOAD_TITAN }
        spawnEnemyOfKind(choices[random.nextInt(choices.size)], elite)
    }

    private fun spawnEnemyOfKind(kind: EnemyKind, elite: Boolean = false) {
        if (enemies.count { !it.dead } >= performanceGovernor.allowedEnemies()) return
        var sx = 0f
        var sz = 0f
        var placed = false
        repeat(8) {
            val angle = random.nextFloat() * 6.283f
            val distance = 8f + random.nextFloat() * 8f
            val tx = cos(angle) * distance
            val tz = sin(angle) * distance
            if (mapRuntime.canSpawn(tx, tz, kind.radius)) {
                sx = tx
                sz = tz
                placed = true
                return@repeat
            }
        }
        if (!placed) return
        val pos = V3(
            sx,
            if (kind == EnemyKind.SCRAP_GOLEM || kind == EnemyKind.OVERLOAD_TITAN) 1.0f else 0.65f,
            sz
        )
        enemies += EnemyEntity(
            nextEnemyId++,
            kind,
            pos,
            kind.hp * if (elite) 1.35f else 1f,
            elite = elite
        )
    }

    private fun spawnBoss() {
        bossSpawned = true
        bossActive = true
        val difficulty = hunterDirector.rift()?.difficulty ?: 1
        val profile = if (activityMode == OfflineMode.STORY) {
            OfflineBossCatalog.forStoryName(storyCampaign.currentChapter().targetId)
        } else {
            OfflineBossCatalog.all[((difficulty - 1).coerceIn(0, OfflineBossCatalog.all.lastIndex))]
        }
        bossMaxHp = profile.maxHp + if (activityMode == OfflineMode.RIFT_BOSS) difficulty * 120f else 0f
        bossHp = bossMaxHp
        enemies += EnemyEntity(
            nextEnemyId++,
            EnemyKind.OVERLOAD_TITAN,
            V3(0f, 1.55f, -9f),
            bossMaxHp,
            bossProfileId = profile.id
        )
        spawnBurst(V3(0f, 1f, -9f), 1f, rgbColor(profile.aura))
        vibrate(90)
    }

    private fun killEnemy(e: EnemyEntity) {
        if (e.dead) return
        e.dead = true
        kills += 1
        huntKills += 1
        addXp(if (e.kind == EnemyKind.OVERLOAD_TITAN) 300f else if (e.elite) 28f else 18f)
        rooftopController.recordBattleKill(weaponIndex, e.elite, e.kind == EnemyKind.OVERLOAD_TITAN)
        if (activityMode == OfflineMode.STORY) {
            if (storyCampaign.currentChapter().objectiveKind == "ARENA") {
                storyCampaign.recordArenaWave()
            } else {
                storyCampaign.recordKill(storyTargetId(e), e.elite, e.kind == EnemyKind.OVERLOAD_TITAN)
            }
            checkStoryObjective()
        }
        gold += if (e.kind == EnemyKind.OVERLOAD_TITAN) 250 else if (e.elite) 20 else 8

        if (e.kind == EnemyKind.ECHO_SPLITTER) {
            repeat(2) { spawnEnemyOfKind(EnemyKind.AETHER_SLIME, false) }
        }
        if (e.kind != EnemyKind.OVERLOAD_TITAN) {
            drops += Drop(V3(e.pos.x, 0.3f, e.pos.z), 0)
            if (random.nextFloat() < 0.25f) {
                drops += Drop(V3(e.pos.x + 0.3f, 0.3f, e.pos.z), 1)
            }
        }

        spawnBurst(
            e.pos,
            if (e.kind == EnemyKind.OVERLOAD_TITAN) 1.0f else 0.32f,
            if (e.kind == EnemyKind.OVERLOAD_TITAN) floatArrayOf(0.8f, 0.24f, 1f)
            else floatArrayOf(0.55f, 0.75f, 1f)
        )
        vibrate(28)
    }

    private fun takeDamage(amount: Float) {
        if (random.nextFloat() < hunterDirector.dodgeChance("weapon_" + weaponIndex)) {
            spawnBurst(player, 0.18f, floatArrayOf(0.35f, 0.9f, 1f))
            return
        }
        hp = max(0f, hp - amount)
        screenShake = 0.5f
        vibrate(20)
        if (hp <= 0f) {
            isDefeated = true
            save()
        }
    }

    fun setMove(x: Float, y: Float) {
        moveX = x
        moveY = y
    }

    @Synchronized fun primaryAction() {
        if (scene != SceneMode.HUNT || isDefeated || primaryTimer > 0f) return
        val weapon = WEAPONS[weaponIndex]
        val weaponId = "weapon_" + weaponIndex
        primaryTimer = weapon.cooldown * hunterDirector.cooldownMultiplier(weaponId) / hunterDirector.attackSpeedMultiplier(weaponId)
        val target = nearestEnemy(weapon.mainRange) ?: return

        if (weapon.archetype == 0 || weapon.archetype == 3) {
            meleeAttack(target, weapon.mainDamage * hunterDirector.damageMultiplier(weaponId), if (weapon.archetype == 3) 2.8f else 2.4f)
        } else {
            fireProjectile(target.pos, weapon.mainDamage * hunterDirector.damageMultiplier(weaponId), 0.9f + weapon.mainRange * 0.03f)
        }
    }

    @Synchronized fun useSkill(index: Int) {
        if (scene != SceneMode.HUNT || isDefeated || index !in 0..2 || skillTimers[index] > 0f) return
        when (index) {
            0 -> skill1()
            1 -> skill2()
            2 -> skill3()
        }
    }

    private fun skill1() {
        skillTimers[0] = (if (WEAPONS[weaponIndex].archetype == 1) 4.0f else 3.0f) * hunterDirector.cooldownMultiplier("weapon_" + weaponIndex)
        if (WEAPONS[weaponIndex].archetype == 1) {
            repeat(3) {
                val t = nearestEnemy(11f)
                if (t != null) fireProjectile(t.pos, 46f * hunterDirector.damageMultiplier("weapon_" + weaponIndex), 1.15f)
            }
        } else {
            dash()
        }
    }

    private fun skill2() {
        skillTimers[1] = 6.0f * hunterDirector.cooldownMultiplier("weapon_" + weaponIndex)
        val radius = when (WEAPONS[weaponIndex].archetype) {
            0 -> 3.3f
            1 -> 2.8f
            2 -> 4.3f
            3 -> 3.4f
            else -> 3.7f
        }
        val damage = when (WEAPONS[weaponIndex].archetype) {
            0 -> 64f
            1 -> 58f
            2 -> 72f
            3 -> 86f
            else -> 62f
        }
        if (WEAPONS[weaponIndex].archetype == 1) dash()
        else areaAttack(radius, damage * hunterDirector.damageMultiplier("weapon_" + weaponIndex))
    }

    private fun skill3() {
        skillTimers[2] = 12.0f * hunterDirector.cooldownMultiplier("weapon_" + weaponIndex)
        val healFactor = if (WEAPONS[weaponIndex].archetype == 4) 0.48f else 0.36f
        hp = min(maxHp, hp + maxHp * healFactor * hunterDirector.healingMultiplier("weapon_" + weaponIndex))
        spawnBurst(player, 0.65f, floatArrayOf(0.4f, 1f, 0.7f))
        vibrate(34)
    }

    private fun dash() {
        val len = sqrt(moveX * moveX + moveY * moveY)
        val dx = if (len > 0.1f) moveX / len else 0f
        val dz = if (len > 0.1f) moveY / len else -1f
        val d = mapRuntime.resolvePlayer(player.x + dx * 4.2f, player.z + dz * 4.2f, 0.55f)
        player.x = d.first
        player.z = d.second
        screenShake = 0.22f
        spawnBurst(player, 0.22f, floatArrayOf(0.45f, 0.78f, 1f))
    }

    private fun meleeAttack(target: EnemyEntity, damage: Float, radius: Float) {
        areaAttack(radius, damage)
        if (target.dead) return
        target.hp -= damage * 0.45f
        target.hitFlash = 0.15f
        if (target.hp <= 0f) killEnemy(target)
        screenShake = 0.18f
        vibrate(20)
    }

    private fun areaAttack(radius: Float, damage: Float) {
        for (e in enemies) {
            if (e.dead) continue
            val dx = e.pos.x - player.x
            val dz = e.pos.z - player.z
            if (dx * dx + dz * dz <= radius * radius) {
                val finalDamage = if (e.kind == EnemyKind.OVERLOAD_TITAN && bossWeakPointOpen()) damage * 2f else damage
                e.hp -= finalDamage
                e.hitFlash = 0.16f
                if (weaponIndex == 1) e.poison = 5f
                spawnBurst(e.pos, 0.14f, floatArrayOf(0.55f, 0.82f, 1f))
                if (e.hp <= 0f) killEnemy(e)
            }
        }
        spawnBurst(player, radius * 0.18f, floatArrayOf(0.65f, 0.45f, 1f))
        screenShake = 0.25f
        vibrate(26)
    }

    private fun fireProjectile(targetPos: V3, damage: Float, speed: Float) {
        val dx = targetPos.x - player.x
        val dz = targetPos.z - player.z
        val distance = max(0.001f, sqrt(dx * dx + dz * dz))
        projectiles += Projectile(
            V3(player.x, 1.15f, player.z),
            V3(dx / distance * speed * 8.0f, 0f, dz / distance * speed * 8.0f),
            damage,
            1.9f,
            true
        )
    }

    private fun nearestEnemy(range: Float): EnemyEntity? {
        var best: EnemyEntity? = null
        var best2 = range * range
        for (e in enemies) {
            if (e.dead) continue
            val dx = e.pos.x - player.x
            val dz = e.pos.z - player.z
            val d2 = dx * dx + dz * dz
            if (d2 <= best2) {
                best2 = d2
                best = e
            }
        }
        return best
    }

    private fun spawnBurst(origin: V3, size: Float, color: FloatArray) {
        val room = (performanceGovernor.allowedParticles() - particles.size).coerceAtLeast(0)
        val count = (7 + size * 8).toInt().coerceAtMost(32).coerceAtMost(room)
        repeat(count) {
            val a = random.nextFloat() * 6.283f
            val speed = (0.7f + random.nextFloat() * 2.2f) * size
            particles += Particle(
                V3(origin.x, origin.y, origin.z),
                V3(cos(a) * speed, 0.7f + random.nextFloat() * speed, sin(a) * speed),
                0.45f,
                0.45f,
                0.08f + size * 0.08f,
                color
            )
        }
    }

    fun equipWeapon(index: Int) {
        if (index in WEAPONS.indices) {
            weaponIndex = index
            save()
        }
    }

    fun skillLabel(i: Int): String = when (i) {
        0 -> short(WEAPONS[weaponIndex].skill1)
        1 -> short(WEAPONS[weaponIndex].skill2)
        else -> short(WEAPONS[weaponIndex].skill3)
    }

    private fun short(value: String) = value.split(' ').first().uppercase(Locale.getDefault())

    fun openInventory() {
        if (!isDefeated) {
            inventoryReturnScene = scene
            scene = SceneMode.INVENTORY
            save()
        }
    }

    fun closeInventory() {
        if (scene == SceneMode.INVENTORY) scene = inventoryReturnScene
        save()
    }

    fun startHunt() = launchExpedition(0)

    fun storySnapshot() = storyCampaign.snapshot()

    fun bossWeakPointOpen(): Boolean =
        bossActive && ((bossPatternTime % 5f) < 1.35f || (bossPatternTime % 5f) > 4.35f)

    fun bossDisplayName(): String {
        if (activityMode == OfflineMode.STORY) {
            return OfflineBossCatalog.forStoryName(storyCampaign.currentChapter().targetId).name
        }
        val boss = enemies.firstOrNull { it.kind == EnemyKind.OVERLOAD_TITAN && !it.dead }
        return boss?.let { OfflineBossCatalog.forId(it.bossProfileId).name } ?: "Titã de Sobrecarga"
    }


    fun advanceStoryBeat() {
        if (storyCampaign.advanceBeat()) {
            audioBus.play("story")
            invalidateUi()
        }
    }

    fun launchStory() {
        if (!storyCampaign.startBattle()) return
        activityMode = OfflineMode.STORY
        beginBattle()
    }

    private fun storyObjectiveText(): String {
        val s = storyCampaign.snapshot()
        return "HISTÓRIA • " + s.chapterTitle + " • " + s.progress + "/" + s.required
    }

    private fun beginStoryBattleSetup() {
        val chapter = storyCampaign.currentChapter()
        activityTarget = chapter.targetCount
        objectiveText = "HISTÓRIA • " + chapter.title + " • " + chapter.synopsis
        when (chapter.objectiveKind) {
            "BOSS" -> spawnBoss()
            "ELITE" -> repeat(4) { spawnStoryEnemy(true) }
            "CORE", "ARENA" -> repeat(4) { spawnStoryEnemy(false) }
            else -> repeat(4) { spawnStoryEnemy(false) }
        }
        storyWaveTimer = 0f
        audioBus.play("story")
    }

    private fun spawnStoryEnemy(forceElite: Boolean) {
        val chapter = storyCampaign.currentChapter()
        val kind = when (chapter.targetId) {
            "slime" -> EnemyKind.AETHER_SLIME
            "stalker" -> EnemyKind.NEON_STALKER
            "golem" -> EnemyKind.SCRAP_GOLEM
            "boss" -> EnemyKind.OVERLOAD_TITAN
            else -> listOf(EnemyKind.AETHER_SLIME, EnemyKind.NEON_STALKER, EnemyKind.SCRAP_GOLEM)[random.nextInt(3)]
        }
        if (kind == EnemyKind.OVERLOAD_TITAN) {
            if (!bossSpawned) spawnBoss()
            return
        }
        spawnEnemyOfKind(kind, forceElite || chapter.objectiveKind == "ELITE")
    }

    private fun storyTargetId(e: EnemyEntity): String = when (e.kind) {
        EnemyKind.AETHER_SLIME -> "slime"
        EnemyKind.NEON_STALKER -> "stalker"
        EnemyKind.SCRAP_GOLEM -> "golem"
        EnemyKind.OVERLOAD_TITAN -> "boss"
        else -> "any"
    }

    private fun checkStoryObjective() {
        if (activityMode != OfflineMode.STORY || activityCompleted || !storyCampaign.objectiveComplete()) return
        val completed = storyCampaign.completeBattle() ?: return
        activityCompleted = true
        gold += completed.rewardGold
        hunterDirector.grantStoryReward(completed.rewardEnergy, completed.rewardGold)
        addXp(260f + completed.number * 12f)
        audioBus.play("level")
        objectiveText = if (completed.number >= 30) {
            "CAMPANHA CONCLUÍDA • AETHERIA FOI SALVA"
        } else {
            "CAPÍTULO " + completed.number + " CONCLUÍDO • +" + completed.rewardGold + " Ouro"
        }
        storyVictoryTimer = 3.8f
        historyArchive.record(
            "chapter_" + completed.number,
            completed.title,
            "story",
            completed.rewardGold,
            System.currentTimeMillis()
        )
        save()
    }

    private fun beginBattle() {
        scene = SceneMode.HUNT
        isDefeated = false
        bossActive = false
        bossSpawned = false
        bossDead = false
        bossPatternTime = 0f
        storyVictoryTimer = 0f
        activityCompleted = false
        huntKills = 0
        storyWaveTimer = 0f
        squadTimer = 0f
        mapRuntime = OfflineMapRuntime.forBiome(activeBiome().id)
        enemies.clear()
        projectiles.clear()
        drops.clear()
        particles.clear()
        aetherCores = 0
        player.x = 0f
        player.z = 4f
        hp = maxHp
        waveTimer = 1.5f

        when (activityMode) {
            OfflineMode.EXPEDITION -> {
                activityTarget = 12
                bossMaxHp = 900f
                repeat(3) { spawnEnemy() }
                objectiveText = "Tarefa: derrote 12 monstros"
            }
            OfflineMode.RIFT_BOSS -> {
                activityTarget = 1
                bossMaxHp = 900f
                spawnBoss()
                objectiveText = "RIFT • derrote o chefe"
            }
            OfflineMode.RIFT_ARENA -> {
                val difficulty = hunterDirector.rift()?.difficulty ?: 4
                activityTarget = 8 + difficulty
                repeat(4) { spawnEnemy() }
                objectiveText = "RIFT ARENA • sobreviva às ondas"
            }
            OfflineMode.VERSUS_SIM -> {
                activityTarget = 10
                repeat(4) { spawnEnemy(true) }
                objectiveText = "VERSUS • supere a arena offline"
            }
            OfflineMode.EVENT -> {
                activityTarget = 14
                repeat(3) { spawnEnemy() }
                objectiveText = "EVENTO • atividade especial"
            }
            OfflineMode.TRAINING -> {
                activityTarget = Int.MAX_VALUE
                objectiveText = "TREINO • ataque livre"
            }
            OfflineMode.STORY -> beginStoryBattleSetup()
        }
        save()
    }

    fun launchExpedition(index: Int) {
        rooftopController.selectExpedition(index)
        rooftopController.enterSelectedExpedition()
        activityMode = OfflineMode.EXPEDITION
        beginBattle()
    }

    fun launchRift(slot: Int) {
        rooftopController.selectRift(0, slot)
        rooftopController.enterSelectedRift()
        activityMode = hunterDirector.mode()
        beginBattle()
    }

    fun launchVersus(bot: Int) {
        rooftopController.selectBot(bot)
        rooftopController.enterVersus()
        activityMode = OfflineMode.VERSUS_SIM
        beginBattle()
    }

    fun returnToHub() {
        rooftopController.backToHome()
        scene = SceneMode.HUB
        isDefeated = false
        bossActive = false
        enemies.clear()
        projectiles.clear()
        drops.clear()
        particles.clear()
        player.x = 0f
        player.z = 5f
        hp = maxHp
        objectiveText = "Portal pronto • escolha uma caçada"
        save()
    }

    fun openRooftopMenu() {
        if (scene == SceneMode.HUNT && !isDefeated) return
        scene = SceneMode.MENU
        rooftopController.navigate(RooftopScreen.HOME)
        invalidateUi()
    }

    fun closeRooftopMenu() {
        if (scene == SceneMode.MENU) scene = SceneMode.HUB
        rooftopController.backToHome()
        save()
        invalidateUi()
    }

    fun rooftopScreen(): RooftopScreen = rooftopController.screen
    fun portalCards() = rooftopController.portalCards()
    fun utilityCards() = rooftopController.utilityCards()
    fun expeditions() = hunterDirector.buildExpeditions()
    fun rifts() = hunterDirector.buildRiftSet(0)
    fun bots() = rooftopController.bots()
    fun styles() = rooftopController.styles()
    fun cores() = rooftopController.cores()
    fun dailyGoals() = rooftopController.dailyGoals()
    fun events() = rooftopController.events()
    fun recipes() = rooftopController.recipes()
    fun hunterSummary() = hunterDirector.hunter()
    fun selectedExpedition() = rooftopController.selectedExpedition
    fun selectedRiftSlot() = rooftopController.selectedRiftSlot
    fun selectedEvent() = rooftopController.selectedEvent
    fun selectedBot() = rooftopController.selectedBot
    fun activeStyleIndex() = rooftopController.selectedStyle
    fun coreBonusLabel(core: com.example.testejuerpg.offline.OfflineUpgradeCore) = rooftopController.coreBonusLabel(core)

    fun selectExpedition(index: Int) { rooftopController.selectExpedition(index) }
    fun selectRiftSlot(index: Int) { rooftopController.selectRift(0, index) }
    fun selectEvent(index: Int) { rooftopController.selectEvent(index) }
    fun selectBot(index: Int) { rooftopController.selectBot(index) }
    fun selectStyle(index: Int) { if (rooftopController.selectStyle(index)) save() }
    fun unlockStyle(index: Int) { if (rooftopController.unlockStyle(index)) save() }
    fun equipVisibleCore(index: Int) {
        val core = rooftopController.cores().getOrNull(index) ?: return
        if (rooftopController.equipCore("weapon_" + weaponIndex, core)) save()
    }
    fun activateEvent() {
        if (rooftopController.activateEvent()) {
            activityMode = OfflineMode.EVENT
            beginBattle()
        }
    }
    fun craft(index: Int) {
        val recipe = rooftopController.recipes().getOrNull(index) ?: return
        if (hunterDirector.craft(recipe.id)) save()
    }

    fun menuTitle(): String = when (rooftopController.screen) {
        RooftopScreen.HOME -> "QG NO TERRAÇO"
        RooftopScreen.PORTAL -> "PORTAL"
        RooftopScreen.EXPEDITIONS -> "EXPEDIÇÕES"
        RooftopScreen.RIFTS -> "RIFTS"
        RooftopScreen.VERSUS -> "VERSUS"
        RooftopScreen.WARDROBE -> "ARMÁRIO"
        RooftopScreen.CORES -> "NÚCLEOS DE MELHORIA"
        RooftopScreen.DAILY -> "OBJETIVOS DIÁRIOS"
        RooftopScreen.EVENTS -> "EVENTOS"
        RooftopScreen.PROFILE -> "PERFIL DO CAÇADOR"
        RooftopScreen.CRAFTING -> "OFICINA"
        RooftopScreen.STORY -> "HISTÓRIA — AETHERIA: ECHOS"
    }

    fun menuSubtitle(): String = when (rooftopController.screen) {
        RooftopScreen.HOME -> "Hub offline • portal, armário, progresso e oficina"
        RooftopScreen.PORTAL -> "Selecione o destino da próxima caça"
        RooftopScreen.EXPEDITIONS -> "Jobs, mundos e recompensas"
        RooftopScreen.RIFTS -> "Três fendas por conjunto • contra o tempo"
        RooftopScreen.VERSUS -> "Rivais simulados pela IA local"
        RooftopScreen.WARDROBE -> "Doze estilos salvos"
        RooftopScreen.CORES -> "Níveis 1–4 • equipar e fundir"
        RooftopScreen.DAILY -> "Energia e sequência diária"
        RooftopScreen.EVENTS -> "Modificadores especiais"
        RooftopScreen.PROFILE -> "Carreira e temporada"
        RooftopScreen.CRAFTING -> "Armas, gadgets, armaduras e rides"
        RooftopScreen.STORY -> "30 capítulos • diálogo persistente • chefes e decisões de combate"
    }

    fun openRooftopPage(id: String?) {
        when (id) {
            "expeditions" -> rooftopController.navigate(RooftopScreen.EXPEDITIONS)
            "rifts" -> rooftopController.navigate(RooftopScreen.RIFTS)
            "versus" -> rooftopController.navigate(RooftopScreen.VERSUS)
            "events" -> rooftopController.navigate(RooftopScreen.EVENTS)
            "wardrobe" -> rooftopController.navigate(RooftopScreen.WARDROBE)
            "cores" -> rooftopController.navigate(RooftopScreen.CORES)
            "daily" -> rooftopController.navigate(RooftopScreen.DAILY)
            "crafting" -> rooftopController.navigate(RooftopScreen.CRAFTING)
            "profile" -> rooftopController.navigate(RooftopScreen.PROFILE)
            "story" -> rooftopController.navigate(RooftopScreen.STORY)
            else -> return
        }
        scene = SceneMode.MENU
        invalidateUi()
    }

    private fun invalidateUi() = onUiInvalidate?.invoke()

    private fun addXp(amount: Float) {
        xp += amount
        while (xp >= xpToNext) {
            xp -= xpToNext
            level += 1
            xpToNext = floor(xpToNext * 1.28f + 20f)
            maxHp += 14f
            hp = maxHp
            vibrate(90)
            spawnBurst(player, 0.7f, floatArrayOf(0.95f, 0.8f, 0.3f))
        }
    }

    private fun load() {
        level = prefs.getInt("level", 1)
        xp = prefs.getFloat("xp", 0f)
        xpToNext = prefs.getFloat("xp_next", 100f)
        maxHp = prefs.getFloat("max_hp", 150f)
        hp = prefs.getFloat("hp", maxHp)
        gold = prefs.getInt("gold", 50)
        aetherCores = prefs.getInt("cores", 0)
        kills = prefs.getInt("kills", 0)
        weaponIndex = prefs.getInt("weapon", 1).coerceIn(0, WEAPONS.lastIndex)
        player.x = prefs.getFloat("px", 0f)
        player.z = prefs.getFloat("pz", 5f)
        playerName = prefs.getString("name", "Caçador") ?: "Caçador"
    }

    private fun save() {
        prefs.edit()
            .putInt("level", level)
            .putFloat("xp", xp)
            .putFloat("xp_next", xpToNext)
            .putFloat("max_hp", maxHp)
            .putFloat("hp", hp)
            .putInt("gold", gold)
            .putInt("cores", aetherCores)
            .putInt("kills", kills)
            .putInt("weapon", weaponIndex)
            .putFloat("px", player.x)
            .putFloat("pz", player.z)
            .putString("name", playerName)
            .apply()
        hunterDirector.saveTo(prefs)
        prefs.edit().putInt("active_emote", activeEmote).apply()
        historyArchive.saveTo(prefs)
    }

    private fun vibrate(ms: Long) {
        try {
            if (vibration?.hasVibrator() == true) {
                vibration.vibrate(
                    VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (_: Throwable) {
        }
    }

    fun activeEmote(): OfflineEmote = OfflineEmoteCatalog.all[activeEmote]
    fun emoteActive(): Boolean = emoteTimer > 0f
    fun cycleEmote() {
        activeEmote = (activeEmote + 1) % OfflineEmoteCatalog.all.size
        emoteTimer = 2.2f
        audioBus.play("story")
        save()
        invalidateUi()
    }

    fun squadMembers() = OfflineSquadCatalog.all
    fun squadOffsets(): List<V3> = memberOffsets.map { V3(it.x, it.y, it.z) }
    fun historyEntries() = historyArchive.entries()

    fun activeStyleVisual(): OfflineVisualCatalog.StyleVisual =
        OfflineVisualCatalog.forStyle(rooftopController.selectedStyle)

    fun activeBiome(): com.example.testejuerpg.offline.systems.OfflineBiome {
        return OfflineBiomeCatalog.forWorld(
            hunterDirector.expedition()?.world
                ?: hunterDirector.rift()?.setId
                ?: storyCampaign.currentChapter().location
        )
    }

    fun allowedFireflies(): Int = performanceGovernor.allowedFireflies()
    fun mapObstacles(): List<MapObstacleView> = mapRuntime.obstacles.map {
        OfflineMapRuntimeObstacle(it.x, it.z, it.halfX, it.halfZ)
    }
    fun renderProfile(): MobileRenderProfile = renderProfile

    fun recordRenderFrame(frameMs: Float, drawCalls: Int) {
        performanceGovernor.sample(frameMs)
        performanceTelemetry.record(
            frameMs, drawCalls, snapshotEnemies().size,
            snapshotParticles().size, snapshotProjectiles().size
        )
    }

    fun performanceSnapshot(): PerformanceSnapshot = performanceTelemetry.snapshot()

    private val enemyView = ArrayList<EnemyEntity>(24)
    private val projectileView = ArrayList<Projectile>(24)
    private val dropView = ArrayList<Drop>(48)
    private val particleView = ArrayList<Particle>(360)

    fun snapshotEnemies(): List<EnemyEntity> {
        enemyView.clear()
        for (e in enemies) if (!e.dead) enemyView.add(e)
        return enemyView
    }

    fun snapshotProjectiles(): List<Projectile> {
        projectileView.clear()
        projectileView.addAll(projectiles)
        return projectileView
    }

    fun snapshotDrops(): List<Drop> {
        dropView.clear()
        dropView.addAll(drops)
        return dropView
    }

    fun snapshotParticles(): List<Particle> {
        particleView.clear()
        particleView.addAll(particles)
        return particleView
    }
}

