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


class GameRenderer(private val engine: Game3DEngine) : GLSurfaceView.Renderer {
    private var program = 0
    private var positionHandle = 0
    private var normalHandle = 0
    private var mvpHandle = 0
    private var modelHandle = 0
    private var colorHandle = 0
    private var fogColorHandle = 0
    private var lightHandle = 0
    private var fogDensityHandle = 0
    private var rimHandle = 0
    private var viewHandle = 0

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private val temp = FloatArray(16)
    private val camera = FloatArray(3)
    private val target = FloatArray(3)
    private val smoothTarget = FloatArray(3)
    private val smoothCamera = FloatArray(3)
    private var cameraReady = false
    private var lastFrameNs = 0L
    private val meshes = HashMap<String, Mesh>()
    private var width = 1
    private var height = 1
    private var time = 0f

    override fun onSurfaceCreated(
        gl: javax.microedition.khronos.opengles.GL10?,
        config: javax.microedition.khronos.egl.EGLConfig?
    ) {
        GLES20.glClearColor(0.055f, 0.07f, 0.11f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)

        program = ShaderProgram.create()
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        normalHandle = GLES20.glGetAttribLocation(program, "aNormal")
        mvpHandle = GLES20.glGetUniformLocation(program, "uMvp")
        modelHandle = GLES20.glGetUniformLocation(program, "uModel")
        viewHandle = GLES20.glGetUniformLocation(program, "uView")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
        fogColorHandle = GLES20.glGetUniformLocation(program, "uFogColor")
        lightHandle = GLES20.glGetUniformLocation(program, "uLightDir")
        fogDensityHandle = GLES20.glGetUniformLocation(program, "uFogDensity")
        rimHandle = GLES20.glGetUniformLocation(program, "uRimStrength")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(normalHandle)

        meshes["cube"] = Mesh.cube()
        meshes["sphere"] = Mesh.sphere(engine.renderProfile().sphereSegments, engine.renderProfile().sphereRings)
        meshes["cylinder"] = Mesh.cylinder(engine.renderProfile().sphereSegments)
        meshes["torus"] = Mesh.torus(engine.renderProfile().torusSegments, engine.renderProfile().torusTubeSegments)
        meshes["plane"] = Mesh.plane()
    }

    override fun onSurfaceChanged(
        gl: javax.microedition.khronos.opengles.GL10?,
        w: Int,
        h: Int
    ) {
        width = max(1, w)
        height = max(1, h)
        GLES20.glViewport(0, 0, width, height)
        Matrix.perspectiveM(projection, 0, 58f, width.toFloat() / height.toFloat(), 0.1f, 70f)
    }

    private var drawCalls = 0

    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        val frameStart = System.nanoTime()
        val frameDt = if (lastFrameNs == 0L) 1f / 60f else ((frameStart - lastFrameNs).coerceAtMost(100_000_000L) / 1_000_000_000f)
        lastFrameNs = frameStart
        drawCalls = 0
        engine.tick()
        time += frameDt.coerceIn(0.008f, 0.033f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        val smooth = (1f - Math.exp((-10f * frameDt).toDouble())).toFloat()
        target[0] = engine.player.x
        target[1] = 0.8f
        target[2] = engine.player.z
        if (!cameraReady) {
            smoothTarget[0] = target[0]; smoothTarget[1] = target[1]; smoothTarget[2] = target[2]
            smoothCamera[0] = target[0] + 8.5f; smoothCamera[1] = 10.5f; smoothCamera[2] = target[2] + 8.5f
            cameraReady = true
        } else {
            smoothTarget[0] += (target[0] - smoothTarget[0]) * smooth
            smoothTarget[1] += (target[1] - smoothTarget[1]) * smooth
            smoothTarget[2] += (target[2] - smoothTarget[2]) * smooth
        }
        camera[0] = smoothTarget[0] + 8.5f + sin(time * 0.2f) * engine.screenShake
        camera[1] = 10.5f + engine.screenShake * 0.8f
        camera[2] = smoothTarget[2] + 8.5f + cos(time * 0.2f) * engine.screenShake
        target[0] = smoothTarget[0]
        target[1] = smoothTarget[1]
        target[2] = smoothTarget[2]
        Matrix.setLookAtM(
            view,
            0,
            camera[0],
            camera[1],
            camera[2],
            target[0],
            target[1],
            target[2],
            0f,
            1f,
            0f
        )

        val biome = engine.activeBiome()
        val lightStrength = 0.82f + biome.ambientIntensity * 0.10f
        GLES20.glUniform3f(lightHandle, -0.55f, -1.0f, -0.35f)
        GLES20.glUniform1f(rimHandle, 0.10f + 0.06f * lightStrength)
        val night = 0.5f - 0.5f * cos((time % 600f) / 600f * Math.PI * 2.0).toFloat()
        if (engine.scene == SceneMode.HUB) {
            val r = 0.12f - night * 0.04f
            val g = 0.15f - night * 0.05f
            val b = 0.24f - night * 0.03f
            GLES20.glUniform4f(fogColorHandle, r.coerceAtLeast(0.04f), g.coerceAtLeast(0.05f), b.coerceAtLeast(0.08f), 1f)
        } else {
            val r = 0.06f - night * 0.025f
            val g = 0.09f - night * 0.02f
            val b = 0.13f + night * 0.04f
            GLES20.glUniform4f(fogColorHandle, r.coerceAtLeast(0.02f), g.coerceAtLeast(0.03f), b, 1f)
        }
        GLES20.glUniform1f(fogDensityHandle, engine.renderProfile().fogDensity)
        GLES20.glUniformMatrix4fv(viewHandle, 1, false, view, 0)

        if (engine.scene == SceneMode.HUB || engine.scene == SceneMode.MENU) drawHub() else drawHunt()
        engine.snapshotParticles().forEach {
            drawSphere(it.pos.x, it.pos.y, it.pos.z, it.scale, it.color)
        }
        engine.recordRenderFrame((System.nanoTime() - frameStart) / 1_000_000f, drawCalls)
    }

    private fun drawHub() {
        drawCube(0f, -0.12f, 0f, 42f, 0.2f, 42f, floatArrayOf(0.11f, 0.16f, 0.19f))
        for (i in -3..3) {
            drawCube(i * 5f, 0.05f, 0f, 0.06f, 0.02f, 42f, floatArrayOf(0.16f, 0.24f, 0.29f))
        }
        for (i in -4..4) {
            drawCube(0f, 0.06f, i * 5f, 42f, 0.02f, 0.06f, floatArrayOf(0.16f, 0.24f, 0.29f))
        }
        drawCube(-10f, 1.2f, -4f, 4f, 2.4f, 4f, floatArrayOf(0.28f, 0.30f, 0.38f))
        drawCube(10f, 1.1f, -6f, 5f, 2.2f, 3f, floatArrayOf(0.21f, 0.28f, 0.37f))
        drawCylinder(-10f, 3.0f, -4f, 0.42f, 3.5f, floatArrayOf(0.35f, 0.75f, 1f))
        drawCylinder(10f, 2.7f, -6f, 0.36f, 3.0f, floatArrayOf(0.65f, 0.36f, 1f))
        drawTorus(0f, 2.0f, -3f, 2.3f, floatArrayOf(0.65f, 0.36f, 1f))
        drawTorus(0f, 2.0f, -3f, 1.85f, floatArrayOf(0.32f, 0.9f, 1f))
        drawCube(0f, 0.7f, -3f, 0.9f, 1.4f, 0.9f, floatArrayOf(0.18f, 0.24f, 0.31f))
        drawNpc(-4.5f, 0.85f, 1f, floatArrayOf(0.8f, 0.62f, 0.95f))
        drawNpc(4.3f, 0.85f, 1.8f, floatArrayOf(0.3f, 0.75f, 1f))
        drawPlayer()
    }

    private fun drawHunt() {
        val biome = engine.activeBiome()
        drawCube(0f, -0.14f, 0f, 44f, 0.25f, 44f, floatArrayOf(biome.groundR, biome.groundG, biome.groundB))
        val seeds = intArrayOf(2, 5, 8, 11, 15, 19, 23, 29, 31, 37, 41, 43)
        engine.mapObstacles().forEachIndexed { i, o ->
            val body = if (i % 3 == 0) floatArrayOf(0.31f, 0.37f, 0.42f)
            else if (i % 3 == 1) floatArrayOf(0.36f, 0.28f, 0.40f)
            else floatArrayOf(0.29f, 0.41f, 0.37f)
            drawCube(o.x, 0.48f, o.z, o.halfX, 0.48f, o.halfZ, body)
            drawTorus(o.x, 0.96f, o.z, min(o.halfX, o.halfZ) * 0.65f, rgb(0x526B84))
        }
        drawBiomeLandmarks(biome)
        for (i in seeds.indices) {
            val x = ((seeds[i] * 7) % 34 - 17).toFloat()
            val z = ((seeds[i] * 11) % 34 - 17).toFloat()
            val h = 0.5f + (i % 3) * 0.35f
            val color = if (i % 4 == 0) floatArrayOf(0.34f, 0.56f, 0.32f) else floatArrayOf(0.24f, 0.36f, 0.28f)
            drawCube(x, h / 2f, z, 0.8f + (i % 3) * 0.4f, h, 0.7f + (i % 2) * 0.3f, color)
        }
        val nightCycle = 0.5f - 0.5f * cos((time % 600f) / 600f * Math.PI * 2.0).toFloat()
        if (nightCycle > 0.55f) {
            val fireflySeeds = intArrayOf(3, 7, 12, 16, 21, 27, 31, 36, 42, 47, 53, 59, 64, 71, 79, 83)
            for (i in 0 until min(fireflySeeds.size, engine.allowedFireflies())) {
                val x = ((fireflySeeds[i] * 13) % 34 - 17).toFloat()
                val z = ((fireflySeeds[i] * 17) % 34 - 17).toFloat()
                val y = 1.2f + 0.65f * sin(time * 1.9f + i)
                val pulse = 0.10f + 0.05f * (0.5f + 0.5f * sin(time * 3.0f + i))
                drawSphere(x, y, z, pulse, floatArrayOf(0.65f, 1f, 0.32f))
            }
        }
        for (i in -10..10 step 2) {
            drawCube(i.toFloat(), 0.04f, 0f, 0.04f, 0.03f, 44f, floatArrayOf(0.21f, 0.34f, 0.25f))
            drawCube(0f, 0.04f, i.toFloat(), 44f, 0.03f, 0.04f, floatArrayOf(0.21f, 0.34f, 0.25f))
        }
        engine.snapshotDrops().forEach { d ->
            drawSphere(
                d.pos.x,
                0.28f + sin(time * 4f + d.pos.x) * 0.1f,
                d.pos.z,
                0.20f,
                if (d.type == 0) floatArrayOf(0.35f, 0.95f, 1f) else floatArrayOf(1f, 0.82f, 0.22f)
            )
        }
        engine.snapshotProjectiles().forEach { p ->
            drawSphere(
                p.pos.x,
                p.pos.y,
                p.pos.z,
                0.18f,
                if (p.playerOwned) floatArrayOf(0.55f, 0.85f, 1f) else floatArrayOf(1f, 0.25f, 0.45f)
            )
        }
        for (e in engine.snapshotEnemies()) drawEnemy(e)
        drawPlayer()
        drawSquad()
    }

    private fun drawSquad() {
        if (engine.scene != SceneMode.HUNT) return
        engine.squadMembers().forEachIndexed { i, member ->
            val p = engine.squadOffsets().getOrNull(i) ?: return@forEachIndexed
            val tint = rgb(member.tint)
            drawCylinder(p.x, 0.86f, p.z, 0.30f, 0.86f, tint)
            drawSphere(p.x, 1.45f, p.z, 0.25f, rgb(0xF1C7A2))
            drawTorus(p.x, 1.02f, p.z, 0.36f, tint)
        }
    }

    private fun drawBiomeLandmarks(biome: com.example.testejuerpg.offline.systems.OfflineBiome) {
        val visual = OfflineVisualCatalog.forBiome(biome.id)
        val accent = rgb(
            (biome.accentR * 255f).toInt().shl(16) or
                (biome.accentG * 255f).toInt().shl(8) or
                (biome.accentB * 255f).toInt()
        )
        val scale = visual.propScale
        when (biome.id) {
            "prism_garden" -> {
                for (i in 0 until 8) {
                    val x = -14f + i * 4.1f * scale
                    drawCylinder(x, 0.65f, -10f + (i % 2) * 3f, 0.18f, 1.3f, accent)
                    drawSphere(x, 1.45f, -10f + (i % 2) * 3f, 0.30f, rgb(0xA1FFD3))
                }
            }
            "neon_forest" -> {
                for (i in 0 until 9) {
                    val x = -15f + (i * 3.7f) * scale
                    val z = -12f + (i % 3) * 5f
                    drawCylinder(x, 1.1f, z, 0.24f, 2.2f, rgb(0x514B78))
                    drawSphere(x, 2.65f, z, 0.72f, rgb(0x33D9A0))
                }
            }
            "plasma_marsh" -> {
                for (i in 0 until 7) {
                    val x = -13f + i * 4.2f * scale
                    val z = -11f + (i % 2) * 5f
                    drawTorus(x, 0.10f, z, 0.8f, accent)
                    drawSphere(x, 0.22f, z, 0.22f, rgb(0x68D8FF))
                }
            }
            "scrap_ruins" -> {
                for (i in 0 until 8) {
                    val x = -15f + i * 4.0f
                    val z = -11f + ((i * 3) % 5)
                    drawCube(x, 1.0f + (i % 3) * 0.3f, z, 0.6f, 1.0f + (i % 3) * 0.3f, 0.6f, rgb(0x6C737F))
                    drawCube(x + 0.65f, 0.5f, z, 0.35f, 0.25f, 0.35f, accent)
                }
            }
            "magnetic_canyon", "vortex_canyon" -> {
                for (i in 0 until 8) {
                    val x = -15f + i * 4.2f
                    val z = -12f + (i % 2) * 6f
                    drawCylinder(x, 1.0f + (i % 3) * 0.6f, z, 0.45f, 2.0f + (i % 3) * 1.2f, accent)
                    drawTorus(x, 2.5f, z, 0.65f, rgb(0xFF8F6A))
                }
            }
            "aurora_dome" -> {
                for (i in 0 until 5) {
                    val z = -13f + i * 6f
                    drawTorus(0f, 1.2f, z, 2.4f + i * 0.25f, accent)
                }
            }
            "crystal_vale" -> {
                for (i in 0 until 10) {
                    val x = -16f + i * 3.5f
                    val z = -11f + (i % 4) * 3.5f
                    drawCube(x, 0.8f + (i % 3) * 0.4f, z, 0.3f, 0.9f, 0.3f, accent)
                    drawSphere(x, 1.65f + (i % 2) * 0.3f, z, 0.20f, rgb(0xDAF4FF))
                }
            }
            "memory_desert" -> {
                for (i in 0 until 7) {
                    val x = -15f + i * 4.8f
                    drawCube(x, 0.9f, -11f, 0.8f, 1.8f, 0.5f, rgb(0xBC9765))
                    drawTorus(x, 1.8f, -11f, 0.7f, accent)
                }
            }
            "origin_chamber", "blue_void" -> {
                for (i in 0 until 7) {
                    val a = i * 0.897f
                    val x = cos(a) * 11f
                    val z = sin(a) * 11f
                    drawCylinder(x, 1.3f, z, 0.16f, 2.6f, accent)
                    drawSphere(x, 2.75f, z, 0.24f, rgb(0x9CCBFF))
                }
            }
        }
    }

    private fun drawPlayer() {
        val p = engine.player
        val style = engine.activeStyleVisual()
        val body = rgb(style.body)
        val suit = rgb(style.suit)
        val accent = rgb(style.accent)
        val headwear = rgb(style.headwear)
        val ride = rgb(style.ride)

        drawCylinder(p.x, 1.0f, p.z, 0.48f, 1.25f, suit)
        drawSphere(p.x, 1.85f, p.z, 0.42f, body)
        drawCube(p.x - 0.23f, 0.35f, p.z, 0.15f, 0.55f, 0.2f, rgb(0x18243B))
        drawCube(p.x + 0.23f, 0.35f, p.z, 0.15f, 0.55f, 0.2f, rgb(0x18243B))

        // Style-specific silhouette details keep each owned skin visually distinct
        // without shipping proprietary game assets.
        when (engine.activeStyleIndex()) {
            0 -> drawTorus(p.x, 2.20f, p.z, 0.46f, accent)
            1 -> {
                drawCube(p.x - 0.47f, 1.15f, p.z, 0.12f, 0.34f, 0.32f, headwear)
                drawCube(p.x + 0.47f, 1.15f, p.z, 0.12f, 0.34f, 0.32f, headwear)
            }
            2 -> drawCube(p.x, 2.38f, p.z, 0.15f, 0.52f, 0.15f, accent)
            3 -> {
                drawCube(p.x - 0.58f, 1.15f, p.z, 0.16f, 0.30f, 0.42f, headwear)
                drawCube(p.x + 0.58f, 1.15f, p.z, 0.16f, 0.30f, 0.42f, headwear)
            }
            4, 5 -> drawTorus(p.x, 2.35f, p.z, 0.30f, headwear)
            6, 7 -> drawCube(p.x, 0.72f, p.z - 0.26f, 0.42f, 0.32f, 0.14f, headwear)
            8, 9 -> drawSphere(p.x, 2.33f, p.z, 0.14f, accent)
            10, 11 -> {
                drawTorus(p.x, 1.00f, p.z, 0.78f, accent)
                drawCube(p.x, 2.28f, p.z, 0.64f, 0.08f, 0.18f, headwear)
            }
        }
        // Style-specific headwear and chest emitter.
        drawCube(p.x, 2.20f, p.z, 0.50f, 0.09f, 0.50f, headwear)
        drawSphere(p.x + 0.34f, 1.25f, p.z, 0.12f, accent)
        drawTorus(p.x, 1.34f, p.z, 0.58f, accent)

        val weaponColor = rgb(WEAPONS[engine.weaponIndex].tint)
        drawCube(
            p.x + 0.6f,
            1.0f,
            p.z,
            if (engine.weaponIndex == 3) 0.85f else 0.16f,
            0.14f,
            if (engine.weaponIndex == 3) 0.25f else 0.72f,
            weaponColor
        )

        // Low-cost hover ride silhouette, rendered only while the hub is visible.
        if (engine.scene == SceneMode.HUB) {
            drawCube(p.x, 0.18f, p.z + 0.02f, 0.74f, 0.08f, 0.34f, ride)
            drawSphere(p.x - 0.40f, 0.12f, p.z, 0.11f, accent)
            drawSphere(p.x + 0.40f, 0.12f, p.z, 0.11f, accent)
        }
    }

    private fun rgb(hex: Int): FloatArray = floatArrayOf(
        ((hex shr 16) and 255) / 255f,
        ((hex shr 8) and 255) / 255f,
        (hex and 255) / 255f
    )

    private fun drawNpc(x: Float, y: Float, z: Float, color: FloatArray) {
        drawCylinder(x, y, z, 0.42f, 1.2f, color)
        drawSphere(x, y + 0.8f, z, 0.36f, floatArrayOf(0.95f, 0.78f, 0.64f))
        drawCube(x, 1.65f, z, 0.5f, 0.18f, 0.55f, color)
    }

    private fun drawEnemy(e: EnemyEntity) {
        val bossProfile = if (e.kind == EnemyKind.OVERLOAD_TITAN) OfflineBossCatalog.forId(e.bossProfileId) else null
        val c = when (e.kind) {
            EnemyKind.AETHER_SLIME -> floatArrayOf(0.28f, 0.88f, 0.72f)
            EnemyKind.NEON_STALKER -> floatArrayOf(0.92f, 0.32f, 0.62f)
            EnemyKind.SCRAP_GOLEM -> floatArrayOf(0.52f, 0.58f, 0.66f)
            EnemyKind.OVERLOAD_TITAN -> rgb(bossProfile?.body ?: 0xAA65FF)
            else -> {
                val palette = intArrayOf(0x76E1FF, 0xFF8CD9, 0xA0FF7D, 0xFFB86B, 0xC6A0FF)
                rgb(palette[e.kind.ordinal % palette.size])
            }
        }

        drawCylinder(e.pos.x, 0.07f, e.pos.z, e.kind.radius * 0.9f, 0.025f, e.kind.radius * 0.7f, floatArrayOf(0.07f, 0.09f, 0.12f))
        if (e.kind == EnemyKind.AETHER_SLIME) {
            drawSphere(e.pos.x, 0.58f, e.pos.z, if (e.elite) 0.9f else 0.68f, if (e.hitFlash > 0f) floatArrayOf(1f, 1f, 1f) else c)
        } else if (e.kind == EnemyKind.THORN_LING) {
            drawSphere(e.pos.x,0.58f,e.pos.z,0.62f,c)
            for(i in 0..2) {
                val a=i*2.094f
                drawCube(e.pos.x+cos(a)*0.48f,0.72f,e.pos.z+sin(a)*0.48f,0.12f,0.48f,0.12f,c)
            }
        } else if (e.kind == EnemyKind.SAND_BOMBER) {
            drawCube(e.pos.x,0.78f,e.pos.z,0.78f,1.12f,0.78f,c)
            drawSphere(e.pos.x,1.55f,e.pos.z,0.34f,floatArrayOf(1f,0.68f,0.25f))
            drawTorus(e.pos.x,1.58f,e.pos.z,0.52f,c)
        } else if (e.kind == EnemyKind.PHASE_MOTH) {
            drawSphere(e.pos.x,0.94f,e.pos.z,0.36f,c)
            drawSphere(e.pos.x-0.42f,1.08f,e.pos.z,0.48f,c)
            drawSphere(e.pos.x+0.42f,1.08f,e.pos.z,0.48f,c)
            drawTorus(e.pos.x,1.05f,e.pos.z,0.62f,rgb(0x9C7BFF))
        } else if (e.kind == EnemyKind.MOSS_MENDER) {
            drawCylinder(e.pos.x,0.62f,e.pos.z,0.42f,1.00f,c)
            drawSphere(e.pos.x,1.33f,e.pos.z,0.42f,c)
            drawTorus(e.pos.x,1.45f,e.pos.z,0.52f,rgb(0x72FF9A))
        } else if (e.kind == EnemyKind.CRYSTAL_SENTINEL) {
            drawCube(e.pos.x,1.05f,e.pos.z,1.20f,1.85f,1.00f,if(e.hitFlash>0f) floatArrayOf(1f,1f,1f) else c)
            drawTorus(e.pos.x,1.16f,e.pos.z,1.02f,rgb(0xB9FAFF))
            drawSphere(e.pos.x,2.10f,e.pos.z,0.34f,rgb(0xEFFFFF))
        } else if (e.kind == EnemyKind.RIFT_ASSASSIN) {
            drawCylinder(e.pos.x,0.70f,e.pos.z,0.38f,1.10f,c)
            drawCube(e.pos.x,1.53f,e.pos.z,0.72f,0.12f,0.72f,rgb(0xD8B8FF))
            drawTorus(e.pos.x,1.0f,e.pos.z,0.54f,c)
        } else if (e.kind == EnemyKind.MAGNET_TURRET) {
            drawCylinder(e.pos.x,0.55f,e.pos.z,0.58f,0.88f,c)
            drawCube(e.pos.x,1.12f,e.pos.z,0.24f,0.76f,0.24f,rgb(0xF7B45F))
            drawTorus(e.pos.x,1.15f,e.pos.z,0.68f,rgb(0xFF7A4F))
        } else if (e.kind == EnemyKind.ECHO_SPLITTER) {
            drawSphere(e.pos.x,0.72f,e.pos.z,0.70f,c)
            drawSphere(e.pos.x-0.24f,0.86f,e.pos.z-0.55f,0.10f,floatArrayOf(1f,1f,1f))
            drawSphere(e.pos.x+0.24f,0.86f,e.pos.z-0.55f,0.10f,floatArrayOf(1f,1f,1f))
        } else if (e.kind == EnemyKind.NEON_STALKER) {
            drawCylinder(e.pos.x, 0.9f, e.pos.z, 0.44f, 1.3f, if (e.hitFlash > 0f) floatArrayOf(1f, 1f, 1f) else c)
            drawSphere(e.pos.x, 1.72f, e.pos.z, 0.40f, floatArrayOf(0.45f, 0.16f, 0.22f))
            drawCube(e.pos.x, 2.15f, e.pos.z, 0.08f, 0.45f, 0.55f, c)
        } else {
            val s = if (e.kind == EnemyKind.OVERLOAD_TITAN) 1.6f else 0.9f
            val bodyColor = if (e.hitFlash > 0f) floatArrayOf(1f, 1f, 1f) else if (e.phase2) floatArrayOf(1f, 0.20f, 0.45f) else c
            drawCube(e.pos.x, s, e.pos.z, s, s * 1.65f, s, bodyColor)
            drawSphere(e.pos.x, s * 2.0f, e.pos.z, s * 0.55f, c)
            if (e.kind == EnemyKind.OVERLOAD_TITAN) {
                val phaseColor = when (e.bossPhase) {
                    3 -> rgb(0xFF4C72)
                    2 -> rgb(0xFFB04C)
                    else -> rgb(bossProfile?.aura ?: 0xFF4C8F)
                }
                drawTorus(e.pos.x, s * 2.1f, e.pos.z, s * 0.8f, phaseColor)
                if (engine.bossWeakPointOpen()) {
                    drawSphere(e.pos.x, s * 2.65f, e.pos.z, 0.22f, rgb(0xFFF07A))
                    drawTorus(e.pos.x, s * 2.65f, e.pos.z, 0.42f, rgb(0xFFF07A))
                }
            }
        }

        if (e.kind == EnemyKind.OVERLOAD_TITAN || e.elite) {
            drawTorus(e.pos.x, 0.12f, e.pos.z, e.kind.radius + 0.35f, rgb(bossProfile?.aura ?: 0xFF4C8F))
        }
    }

    private fun drawCube(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, color: FloatArray) =
        drawMesh("cube", x, y, z, sx, sy, sz, color)

    private fun drawSphere(x: Float, y: Float, z: Float, s: Float, color: FloatArray) =
        drawMesh("sphere", x, y, z, s, s, s, color)

    private fun drawCylinder(x: Float, y: Float, z: Float, r: Float, h: Float, color: FloatArray) =
        drawMesh("cylinder", x, y, z, r, h, r, color)

    private fun drawTorus(x: Float, y: Float, z: Float, r: Float, color: FloatArray) =
        drawMesh("torus", x, y, z, r, r, r, color)

    private fun drawMesh(
        name: String,
        x: Float,
        y: Float,
        z: Float,
        sx: Float,
        sy: Float,
        sz: Float,
        color: FloatArray
    ) {
        val mesh = meshes[name] ?: return
        drawCalls += 1
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        Matrix.scaleM(model, 0, sx, sy, sz)
        Matrix.multiplyMM(temp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, temp, 0)
        mesh.draw(
            positionHandle,
            normalHandle,
            mvpHandle,
            modelHandle,
            colorHandle,
            mvp,
            model,
            color
        )
    }
}

