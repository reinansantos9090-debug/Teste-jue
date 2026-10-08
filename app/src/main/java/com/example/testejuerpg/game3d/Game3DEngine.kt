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


    internal val enemyView = ArrayList<EnemyEntity>(24)
    internal val projectileView = ArrayList<Projectile>(24)
    internal val dropView = ArrayList<Drop>(48)
    internal val particleView = ArrayList<Particle>(360)
}
