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
import com.example.testejuerpg.offline.systems.OfflineBossDefinition
import com.example.testejuerpg.offline.systems.OfflineMapRuntime
import com.example.testejuerpg.offline.systems.OfflineSquadCatalog
import com.example.testejuerpg.offline.systems.OfflineHistoryArchive
import com.example.testejuerpg.offline.systems.OfflineEmoteCatalog
import com.example.testejuerpg.offline.EngineSaveData
import com.example.testejuerpg.offline.OfflineCombatRules
import com.example.testejuerpg.offline.OfflineLootRules
import com.example.testejuerpg.offline.OfflineProgressionRules
import com.example.testejuerpg.offline.OfflineSaveCodec
import com.example.testejuerpg.offline.LootRarity


data class MapObstacleView(val x: Float, val z: Float, val halfX: Float, val halfZ: Float)

class Game3DEngine(private val context: Context) {
    @Volatile var scene: SceneMode = SceneMode.HUB
        internal set
    internal var inventoryReturnScene: SceneMode = SceneMode.HUB

    @Volatile var playerName: String = "Caçador"
        internal set
    @Volatile var level: Int = 1
        internal set
    @Volatile var xp: Float = 0f
        internal set
    @Volatile var xpToNext: Float = 100f
        internal set
    @Volatile var hp: Float = 150f
        internal set
    @Volatile var maxHp: Float = 150f
        internal set
    @Volatile var gold: Int = 50
        internal set
    @Volatile var aetherCores: Int = 0
        internal set
    @Volatile var kills: Int = 0
        internal set
    @Volatile var weaponIndex: Int = 1
        internal set
    @Volatile var objectiveText: String = "Portal pronto • escolha uma caçada"
        internal set
    @Volatile var bossActive: Boolean = false
        internal set
    @Volatile var bossHp: Float = 900f
        internal set
    @Volatile var bossMaxHp: Float = 900f
        internal set
    var isDefeated: Boolean = false
        internal set

    val player = V3(0f, 0.8f, 5f)
    internal val enemies = CopyOnWriteArrayList<EnemyEntity>()
    internal val projectiles = CopyOnWriteArrayList<Projectile>()
    internal val drops = CopyOnWriteArrayList<Drop>()
    internal val particles = CopyOnWriteArrayList<Particle>()
    internal val random = Random(8107)
    internal val vibration: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    internal val prefs = context.getSharedPreferences("teste_jue_3d", Context.MODE_PRIVATE)
    internal val hunterDirector = OfflineHunterDirector(2025)
    internal val rooftopController = OfflineRooftopController(hunterDirector)
    internal val storyCampaign = OfflineStoryCampaign(context)
    internal val renderProfile = MobileRenderProfile.detect(context)
    internal val performanceGovernor = MobilePerformanceGovernor(renderProfile)
    internal val performanceTelemetry = OfflinePerformanceTelemetry()
    internal val audioBus = OfflineAudioBus()
    internal val historyArchive = OfflineHistoryArchive()
    internal var mapRuntime = OfflineMapRuntime.forBiome("prism_garden")
    internal var activityMode = OfflineMode.EXPEDITION
    internal var activityTarget = 12
    internal var activityCompleted = false
    internal var moveX = 0f
    internal var moveY = 0f
    internal var primaryTimer = 0f
    internal val skillTimers = floatArrayOf(0f, 0f, 0f)
    internal var waveTimer = 0f
    internal var storyWaveTimer = 0f
    internal var squadTimer = 0f
    internal var emoteTimer = 0f
    internal var activeEmote = 0
    internal var nextEnemyId = 1
    internal var lastTick = SystemClock.elapsedRealtime()
    internal var paused = false
    internal var huntKills = 0
    internal var bossSpawned = false
    internal var bossDead = false
    internal var bossPatternTime = 0f
    internal var storyVictoryTimer = 0f
    var screenShake = 0f
        internal set
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
                    aetherCores += d.amount.coerceAtLeast(1)
                    hunterDirector.recordCore(d.amount.coerceAtLeast(1))
                    if (activityMode == OfflineMode.STORY) {
                        storyCampaign.recordCore()
                        checkStoryObjective()
                    }
                    objectiveText = "LOOT • Núcleos +" + d.amount + " • " + LootRarity.fromId(d.rarity).label
                } else {
                    gold += d.amount.coerceAtLeast(1)
                    objectiveText = "LOOT • Ouro +" + d.amount + " • " + LootRarity.fromId(d.rarity).label
                }
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
    internal val memberOffsets = Array(OfflineSquadCatalog.all.size) { V3(0f, 0.85f, 0f) }

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


    internal val enemyView = ArrayList<EnemyEntity>(24)
    internal val projectileView = ArrayList<Projectile>(24)
    internal val dropView = ArrayList<Drop>(48)
    internal val particleView = ArrayList<Particle>(360)
}
