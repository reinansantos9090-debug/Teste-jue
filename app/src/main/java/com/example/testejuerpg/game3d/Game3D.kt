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

private data class V3(var x: Float, var y: Float, var z: Float) {
    fun set(other: V3) { x = other.x; y = other.y; z = other.z }
    fun add(dx: Float, dy: Float, dz: Float) { x += dx; y += dy; z += dz }
}

private enum class SceneMode { HUB, HUNT, INVENTORY }

private enum class EnemyKind(
    val displayName: String,
    val hp: Float,
    val attack: Float,
    val speed: Float,
    val radius: Float
) {
    AETHER_SLIME("Slime de Aether", 90f, 9f, 1.25f, 0.65f),
    NEON_STALKER("Perseguidor Neon", 125f, 13f, 1.85f, 0.55f),
    SCRAP_GOLEM("Golem de Sucata", 240f, 20f, 0.90f, 0.82f),
    OVERLOAD_TITAN("Titã de Sobrecarga", 900f, 28f, 0.78f, 1.55f)
}

private data class WeaponPreset(
    val name: String,
    val role: String,
    val mainDamage: Float,
    val mainRange: Float,
    val cooldown: Float,
    val skill1: String,
    val skill2: String,
    val skill3: String
)

private val WEAPONS = listOf(
    WeaponPreset("Lâminas Voltáicas", "Dano corpo a corpo", 38f, 2.4f, 0.38f, "Dash", "Corte Tempestade", "Pulso de Cura"),
    WeaponPreset("Arco Tóxico", "Dano à distância", 42f, 10.5f, 0.34f, "Rajada Tripla", "Dash", "Antídoto"),
    WeaponPreset("Canhão Pulsar", "Dano em área", 34f, 10f, 0.42f, "Disparo Triplo", "Onda de Choque", "Nanocura"),
    WeaponPreset("Martelo Sucateiro", "Dano pesado", 52f, 2.7f, 0.55f, "Dash", "Terremoto", "Pulso Reparador")
)

private data class EnemyEntity(
    val id: Int,
    val kind: EnemyKind,
    val pos: V3,
    var hp: Float,
    var attackTimer: Float = 0f,
    var hitFlash: Float = 0f,
    var poison: Float = 0f,
    var poisonTick: Float = 0f,
    var dead: Boolean = false,
    var elite: Boolean = false
)

private data class Projectile(
    val pos: V3,
    val vel: V3,
    val damage: Float,
    var life: Float,
    val playerOwned: Boolean
)

private data class Drop(val pos: V3, val type: Int, var life: Float = 30f)

private data class Particle(
    val pos: V3,
    val vel: V3,
    var life: Float,
    val maxLife: Float,
    val scale: Float,
    val color: FloatArray
)

class Game3DRoot(context: Context) : FrameLayout(context) {
    private val engine = Game3DEngine(context)
    private val surface = GameGLView(context, engine)
    private val hud = GameHUDView(context, engine)

    init {
        isFocusable = true
        addView(surface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(hud, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        engine.onUiInvalidate = { hud.postInvalidate() }
    }

    fun handleBack(): Boolean = when (engine.scene) {
        SceneMode.INVENTORY -> { engine.closeInventory(); true }
        SceneMode.HUNT -> { engine.returnToHub(); true }
        SceneMode.HUB -> false
    }

    fun onHostPause() {
        engine.pauseAndSave()
        surface.onPause()
    }

    fun onHostResume() {
        surface.onResume()
        engine.resume()
    }
}

private class GameGLView(context: Context, private val engine: Game3DEngine) : GLSurfaceView(context) {
    private val renderer = GameRenderer(engine)

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        keepScreenOn = true
    }
}

private class GameHUDView(context: Context, private val engine: Game3DEngine) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panel = RectF()
    private val joystickCenter = V3(0f, 0f, 0f)
    private var joystickPointer = -1
    private var joystickDx = 0f
    private var joystickDy = 0f

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT

        if (engine.scene == SceneMode.INVENTORY) {
            drawInventory(canvas, w, h)
            return
        }

        drawTopHud(canvas, w)
        drawControls(canvas, w, h)
        if (engine.scene == SceneMode.HUB) drawHubPrompt(canvas, w, h)
        if (engine.isDefeated) drawDefeat(canvas, w, h)
        if (engine.bossActive) drawBossBar(canvas, w)
    }

    private fun drawTopHud(c: Canvas, w: Float) {
        paint.color = 0xCC101726.toInt()
        panel.set(14f, 14f, w - 14f, 122f)
        c.drawRoundRect(panel, 18f, 18f, paint)

        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 20f
        c.drawText(engine.playerName, 30f, 40f, paint)
        paint.textSize = 13f
        paint.color = 0xFFB8C4D8.toInt()
        c.drawText("Nível " + engine.level + " • " + WEAPONS[engine.weaponIndex].name, 30f, 60f, paint)
        c.drawText("Kills " + engine.kills + " • Núcleos " + engine.aetherCores + " • Ouro " + engine.gold, 30f, 79f, paint)

        drawBar(c, 30f, 88f, min(250f, w * 0.58f), 12f, engine.hp / engine.maxHp, 0xFFE9546B.toInt())
        drawBar(c, 30f, 103f, min(250f, w * 0.58f), 7f, engine.xp / engine.xpToNext, 0xFF6DD5FF.toInt())
        paint.color = 0xFFEAF3FF.toInt()
        paint.textSize = 10f
        c.drawText(engine.hp.toInt().toString() + " / " + engine.maxHp.toInt(), 36f, 99f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = 0xFFDDE7F5.toInt()
        paint.textSize = 12f
        c.drawText(if (engine.scene == SceneMode.HUB) "QG • ROOFTOP" else "EXPEDIÇÃO • CAMPOS DO CAOS", w - 28f, 38f, paint)
        paint.color = 0xFFB8C4D8.toInt()
        paint.textSize = 11f
        c.drawText(engine.objectiveText, w - 28f, 57f, paint)
        c.drawText("☰", w - 30f, 91f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawControls(c: Canvas, w: Float, h: Float) {
        joystickCenter.x = 94f
        joystickCenter.y = h - 112f
        paint.color = 0x383B4960.toInt()
        c.drawCircle(joystickCenter.x, joystickCenter.y, 62f, paint)
        paint.color = 0x664E5A73.toInt()
        c.drawCircle(joystickCenter.x, joystickCenter.y, 38f, paint)
        paint.color = 0xAAE6EEF9.toInt()
        c.drawCircle(
            joystickCenter.x + joystickDx * 34f,
            joystickCenter.y + joystickDy * 34f,
            23f,
            paint
        )

        val attackX = w - 82f
        val attackY = h - 114f
        paint.color = 0xCC8A56FF.toInt()
        c.drawCircle(attackX, attackY, 44f, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f
        c.drawText(if (engine.weaponIndex == 3) "SMASH" else "ATK", attackX, attackY + 4f, paint)

        val sx = arrayOf(w - 178f, w - 224f, w - 270f)
        for (i in 0..2) {
            paint.color = 0xCC23324A.toInt()
            c.drawCircle(sx[i], h - 58f, 29f, paint)
            paint.color = 0xFFB9D8FF.toInt()
            paint.textSize = 9f
            c.drawText((i + 1).toString(), sx[i], h - 60f, paint)
            paint.textSize = 7f
            c.drawText(engine.skillLabel(i), sx[i], h - 48f, paint)
        }

        paint.textAlign = Paint.Align.LEFT
        paint.color = 0xCC151D2E.toInt()
        panel.set(w - 98f, 128f, w - 14f, 168f)
        c.drawRoundRect(panel, 12f, 12f, paint)
        paint.color = 0xFFEAF3FF.toInt()
        paint.textSize = 11f
        paint.textAlign = Paint.Align.CENTER
        c.drawText("MOCHILA", w - 56f, 152f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawHubPrompt(c: Canvas, w: Float, h: Float) {
        paint.color = 0xEE101826.toInt()
        panel.set(28f, h * 0.23f, w - 28f, h * 0.45f)
        c.drawRoundRect(panel, 22f, 22f, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 26f
        c.drawText("CAÇADA 3D", w / 2f, h * 0.30f, paint)
        paint.color = 0xFFBFD4EE.toInt()
        paint.textSize = 14f
        c.drawText("Atravesse o portal e enfrente o caos em tempo real.", w / 2f, h * 0.34f, paint)
        paint.color = 0xFF7A5CFF.toInt()
        panel.set(w * 0.22f, h * 0.37f, w * 0.78f, h * 0.43f)
        c.drawRoundRect(panel, 18f, 18f, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 17f
        c.drawText("ENTRAR NA EXPEDIÇÃO", w / 2f, h * 0.407f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawInventory(c: Canvas, w: Float, h: Float) {
        paint.color = 0xF20C1220.toInt()
        c.drawRect(0f, 0f, w, h, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 25f
        c.drawText("ARMÁRIO / EQUIPAMENTO", 24f, 48f, paint)
        paint.color = 0xFF9FB5CF.toInt()
        paint.textSize = 12f
        c.drawText("Armas têm kits fixos de habilidades. Escolha e domine a que preferir.", 24f, 70f, paint)

        for (i in WEAPONS.indices) {
            val top = 92f + i * 106f
            paint.color = if (i == engine.weaponIndex) 0xFF3A2E61.toInt() else 0xFF172033.toInt()
            panel.set(18f, top, w - 18f, top + 88f)
            c.drawRoundRect(panel, 18f, 18f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.textSize = 17f
            c.drawText(WEAPONS[i].name, 34f, top + 29f, paint)
            paint.color = 0xFFB4C5DA.toInt()
            paint.textSize = 11f
            c.drawText(WEAPONS[i].role, 34f, top + 47f, paint)
            paint.color = 0xFF8CA8C8.toInt()
            paint.textSize = 10f
            c.drawText(
                "1 " + WEAPONS[i].skill1 + " • 2 " + WEAPONS[i].skill2 + " • 3 " + WEAPONS[i].skill3,
                34f,
                top + 65f,
                paint
            )
            paint.color = 0xFF765EFF.toInt()
            panel.set(w - 116f, top + 18f, w - 32f, top + 64f)
            c.drawRoundRect(panel, 14f, 14f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 11f
            c.drawText(if (i == engine.weaponIndex) "EQUIPADA" else "EQUIPAR", w - 74f, top + 46f, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        paint.color = 0xFF1E2A3F.toInt()
        panel.set(18f, h - 112f, w - 18f, h - 60f)
        c.drawRoundRect(panel, 14f, 14f, paint)
        paint.color = 0xFFDAE7F6.toInt()
        paint.textSize = 12f
        c.drawText(
            "CRAFTING • Nanodrone de Cura: 5 Núcleos + 20 Sucata • Anel de Choque: 8 Núcleos",
            30f,
            h - 80f,
            paint
        )
        paint.color = 0xFFB1C1D5.toInt()
        paint.textSize = 11f
        c.drawText("Toque em uma arma para equipar • Voltar retorna ao mundo 3D", 30f, h - 44f, paint)
    }

    private fun drawDefeat(c: Canvas, w: Float, h: Float) {
        paint.color = 0xDD0D111A.toInt()
        c.drawRect(0f, h * 0.32f, w, h * 0.68f, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 30f
        c.drawText("CAÇADOR DERROTADO", w / 2f, h * 0.44f, paint)
        paint.color = 0xFFB6C9E1.toInt()
        paint.textSize = 13f
        c.drawText("Retorne ao QG para se recuperar e tentar outra vez.", w / 2f, h * 0.48f, paint)
        paint.color = 0xFF765EFF.toInt()
        panel.set(w * 0.25f, h * 0.53f, w * 0.75f, h * 0.60f)
        c.drawRoundRect(panel, 18f, 18f, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 16f
        c.drawText("VOLTAR AO QG", w / 2f, h * 0.575f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawBossBar(c: Canvas, w: Float) {
        val bw = min(w - 50f, 470f)
        val left = (w - bw) / 2f
        paint.color = 0xD91A1425.toInt()
        panel.set(left, 132f, left + bw, 174f)
        c.drawRoundRect(panel, 14f, 14f, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f
        c.drawText("TITÃ DE SOBRECARGA", w / 2f, 147f, paint)
        drawBar(c, left + 12f, 153f, bw - 24f, 12f, engine.bossHp / engine.bossMaxHp, 0xFFFF4C77.toInt())
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawBar(c: Canvas, x: Float, y: Float, width: Float, height: Float, fraction: Float, color: Int) {
        paint.color = 0x66334152.toInt()
        c.drawRoundRect(RectF(x, y, x + width, y + height), height, height, paint)
        paint.color = color
        c.drawRoundRect(RectF(x, y, x + width * fraction.coerceIn(0f, 1f), y + height), height, height, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (engine.scene == SceneMode.INVENTORY) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val y = event.y
                if (y > 92f && y < 92f + 4 * 106f) {
                    val idx = floor((y - 92f) / 106f).toInt().coerceIn(0, 3)
                    engine.equipWeapon(idx)
                    invalidate()
                }
                if (y > height - 70f) {
                    engine.closeInventory()
                    invalidate()
                }
            }
            return true
        }

        val x = event.x
        val y = event.y
        val w = width.toFloat()
        val h = height.toFloat()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val px = event.getX(idx)
                val py = event.getY(idx)
                if (px < 180f && py > h - 210f && joystickPointer == -1) {
                    joystickPointer = event.getPointerId(idx)
                    updateJoystick(px, py, h)
                } else if (py > h - 180f && px > w - 140f) {
                    engine.primaryAction()
                } else if (py > h - 110f && px > w - 315f && px < w - 150f) {
                    val skill = when {
                        px > w - 205f -> 0
                        px > w - 250f -> 1
                        else -> 2
                    }
                    engine.useSkill(skill)
                } else if (px > w - 112f && py in 124f..176f) {
                    engine.openInventory()
                } else if (engine.scene == SceneMode.HUB && py in (h * 0.36f)..(h * 0.45f)) {
                    engine.startHunt()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (joystickPointer != -1) {
                    val pointerIndex = (0 until event.pointerCount).firstOrNull {
                        event.getPointerId(it) == joystickPointer
                    }
                    if (pointerIndex != null) {
                        updateJoystick(event.getX(pointerIndex), event.getY(pointerIndex), h)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.actionMasked == MotionEvent.ACTION_CANCEL ||
                    event.getPointerId(event.actionIndex) == joystickPointer
                ) {
                    joystickPointer = -1
                    joystickDx = 0f
                    joystickDy = 0f
                    engine.setMove(0f, 0f)
                }
            }
        }
        if (engine.isDefeated && event.actionMasked == MotionEvent.ACTION_UP) engine.returnToHub()
        invalidate()
        return true
    }

    private fun updateJoystick(x: Float, y: Float, h: Float) {
        var dx = (x - joystickCenter.x) / 62f
        var dy = (y - joystickCenter.y) / 62f
        val len = sqrt(dx * dx + dy * dy)
        if (len > 1f) {
            dx /= len
            dy /= len
        }
        joystickDx = dx
        joystickDy = dy
        engine.setMove(dx, dy)
    }
}

private class Game3DEngine(private val context: Context) {
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

    private var moveX = 0f
    private var moveY = 0f
    private var primaryTimer = 0f
    private val skillTimers = floatArrayOf(0f, 0f, 0f)
    private var waveTimer = 0f
    private var nextEnemyId = 1
    private var lastTick = SystemClock.elapsedRealtime()
    private var paused = false
    private var huntKills = 0
    private var bossSpawned = false
    private var bossDead = false
    var screenShake = 0f
        private set
    var onUiInvalidate: (() -> Unit)? = null

    init {
        load()
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
            updateWave(dt)
            if (screenShake > 0f) screenShake = max(0f, screenShake - dt * 2.8f)
            objectiveText = when {
                bossActive -> "Derrote o Titã • Núcleos " + aetherCores + "/12"
                huntKills < 12 -> "Tarefa: " + (12 - huntKills) + " monstros restantes"
                aetherCores < 12 -> "Tarefa: colete " + (12 - aetherCores) + " Núcleos"
                else -> "Portal do Titã surgiu • prepare-se"
            }
        }
        onUiInvalidate?.invoke()
    }

    private fun updatePlayer(dt: Float) {
        val len = sqrt(moveX * moveX + moveY * moveY)
        val nx = if (len > 0.01f) moveX / max(1f, len) else 0f
        val nz = if (len > 0.01f) moveY / max(1f, len) else 0f
        player.x = (player.x + nx * 4.5f * dt).coerceIn(-18f, 18f)
        player.z = (player.z + nz * 4.5f * dt).coerceIn(-18f, 18f)
    }

    private fun updateEnemies(dt: Float) {
        for (e in enemies) {
            if (e.dead) continue
            e.hitFlash = max(0f, e.hitFlash - dt)
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

            if (dist > 1.6f + e.kind.radius) {
                val inv = 1f / max(0.001f, dist)
                e.pos.x += dx * inv * e.kind.speed * dt
                e.pos.z += dz * inv * e.kind.speed * dt
            } else if (e.attackTimer <= 0f) {
                e.attackTimer = if (e.kind == EnemyKind.OVERLOAD_TITAN) 1.2f else 1.5f
                takeDamage(e.kind.attack * if (e.elite) 1.25f else 1f)
                if (e.kind == EnemyKind.OVERLOAD_TITAN) {
                    spawnBurst(player, 0.22f, floatArrayOf(1f, 0.28f, 0.5f))
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

    private fun updateWave(dt: Float) {
        waveTimer += dt
        if (!bossActive && !bossSpawned && huntKills >= 12 && aetherCores >= 12) spawnBoss()

        if (!bossActive && waveTimer > 2.8f && enemies.count { !it.dead } < 7 && huntKills < 12) {
            waveTimer = 0f
            spawnEnemy()
            if (huntKills % 4 == 3) spawnEnemy(true)
        }

        if (bossActive) {
            val boss = enemies.firstOrNull { it.kind == EnemyKind.OVERLOAD_TITAN && !it.dead }
            bossHp = boss?.hp ?: 0f
            if (boss == null && bossSpawned && !bossDead) {
                bossDead = true
                bossActive = false
                bossHp = 0f
                gold += 250
                addXp(300f)
                hunterDirector.recordExpeditionComplete()
                objectiveText = "EXPEDIÇÃO CONCLUÍDA • +250 Ouro"
                spawnBurst(player, 1.2f, floatArrayOf(0.7f, 0.85f, 1f))
            }
            if (boss != null && random.nextFloat() < dt * 0.12f && enemies.count { !it.dead } < 9) {
                spawnEnemy(false)
            }
        }
    }

    private fun spawnEnemy(elite: Boolean = false) {
        val choices = when {
            huntKills >= 8 -> listOf(EnemyKind.AETHER_SLIME, EnemyKind.NEON_STALKER, EnemyKind.SCRAP_GOLEM)
            else -> listOf(EnemyKind.AETHER_SLIME, EnemyKind.NEON_STALKER)
        }
        val kind = choices[random.nextInt(choices.size)]
        val angle = random.nextFloat() * 6.283f
        val distance = 8f + random.nextFloat() * 8f
        val pos = V3(cos(angle) * distance, if (kind == EnemyKind.SCRAP_GOLEM) 1.0f else 0.65f, sin(angle) * distance)
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
        bossHp = EnemyKind.OVERLOAD_TITAN.hp
        enemies += EnemyEntity(
            nextEnemyId++,
            EnemyKind.OVERLOAD_TITAN,
            V3(0f, 1.55f, -9f),
            EnemyKind.OVERLOAD_TITAN.hp
        )
        spawnBurst(V3(0f, 1f, -9f), 1f, floatArrayOf(0.85f, 0.25f, 1f))
        vibrate(90)
    }

    private fun killEnemy(e: EnemyEntity) {
        if (e.dead) return
        e.dead = true
        kills += 1
        huntKills += 1
        addXp(if (e.kind == EnemyKind.OVERLOAD_TITAN) 300f else if (e.elite) 28f else 18f)
        hunterDirector.recordKill(e.kind.name, e.elite, e.kind == EnemyKind.OVERLOAD_TITAN, if (e.kind == EnemyKind.OVERLOAD_TITAN) 3 else 1)
        gold += if (e.kind == EnemyKind.OVERLOAD_TITAN) 250 else if (e.elite) 20 else 8

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
        primaryTimer = WEAPONS[weaponIndex].cooldown

        val weapon = WEAPONS[weaponIndex]
        val target = nearestEnemy(weapon.mainRange) ?: return

        if (weaponIndex == 0 || weaponIndex == 3) {
            meleeAttack(target, weapon.mainDamage, if (weaponIndex == 3) 2.8f else 2.4f)
        } else {
            fireProjectile(target.pos, weapon.mainDamage, 0.9f + weapon.mainRange * 0.03f)
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
        skillTimers[0] = if (weaponIndex == 1) 4.0f else 3.0f
        if (weaponIndex == 1) {
            repeat(3) {
                val t = nearestEnemy(11f)
                if (t != null) fireProjectile(t.pos, 46f, 1.15f)
            }
        } else {
            dash()
        }
    }

    private fun skill2() {
        skillTimers[1] = 6.0f
        val radius = when (weaponIndex) {
            0 -> 3.3f
            2 -> 3.8f
            else -> 3.7f
        }
        val damage = when (weaponIndex) {
            0 -> 64f
            2 -> 54f
            3 -> 82f
            else -> 58f
        }
        if (weaponIndex == 1) dash() else areaAttack(radius, damage)
    }

    private fun skill3() {
        skillTimers[2] = 12.0f
        hp = min(maxHp, hp + maxHp * 0.36f)
        spawnBurst(player, 0.65f, floatArrayOf(0.4f, 1f, 0.7f))
        vibrate(34)
    }

    private fun dash() {
        val len = sqrt(moveX * moveX + moveY * moveY)
        val dx = if (len > 0.1f) moveX / len else 0f
        val dz = if (len > 0.1f) moveY / len else -1f
        player.x = (player.x + dx * 4.2f).coerceIn(-18f, 18f)
        player.z = (player.z + dz * 4.2f).coerceIn(-18f, 18f)
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
                e.hp -= damage
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
        repeat((7 + size * 8).toInt().coerceAtMost(32)) {
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

    fun startHunt() {
        if (scene == SceneMode.INVENTORY) return
        hunterDirector.enterExpedition(0)
        scene = SceneMode.HUNT
        isDefeated = false
        bossActive = false
        bossSpawned = false
        bossDead = false
        bossHp = bossMaxHp
        huntKills = 0
        enemies.clear()
        projectiles.clear()
        drops.clear()
        particles.clear()
        aetherCores = 0
        player.x = 0f
        player.z = 4f
        hp = maxHp
        waveTimer = 3f
        repeat(3) { spawnEnemy() }
        objectiveText = "Tarefa: derrote 12 monstros"
        save()
    }

    fun returnToHub() {
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
        weaponIndex = prefs.getInt("weapon", 1).coerceIn(0, 3)
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

    fun snapshotEnemies(): List<EnemyEntity> = enemies.filterNot { it.dead }
    fun snapshotProjectiles(): List<Projectile> = projectiles.toList()
    fun snapshotDrops(): List<Drop> = drops.toList()
    fun snapshotParticles(): List<Particle> = particles.toList()
}

private class GameRenderer(private val engine: Game3DEngine) : GLSurfaceView.Renderer {
    private var program = 0
    private var positionHandle = 0
    private var normalHandle = 0
    private var mvpHandle = 0
    private var modelHandle = 0
    private var colorHandle = 0
    private var fogColorHandle = 0
    private var lightHandle = 0
    private var fogDensityHandle = 0
    private var viewHandle = 0

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private val temp = FloatArray(16)
    private val camera = FloatArray(3)
    private val target = FloatArray(3)
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

        meshes["cube"] = Mesh.cube()
        meshes["sphere"] = Mesh.sphere(16, 10)
        meshes["cylinder"] = Mesh.cylinder(16)
        meshes["torus"] = Mesh.torus(24, 8)
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

    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        engine.tick()
        time += 0.016f
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)

        target[0] = engine.player.x
        target[1] = 0.8f
        target[2] = engine.player.z
        camera[0] = engine.player.x + 8.5f + sin(time * 0.2f) * engine.screenShake
        camera[1] = 10.5f + engine.screenShake * 0.8f
        camera[2] = engine.player.z + 8.5f + cos(time * 0.2f) * engine.screenShake
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

        GLES20.glUniform3f(lightHandle, -0.55f, -1.0f, -0.35f)
        if (engine.scene == SceneMode.HUB) {
            GLES20.glUniform4f(fogColorHandle, 0.12f, 0.15f, 0.24f, 1f)
        } else {
            GLES20.glUniform4f(fogColorHandle, 0.06f, 0.09f, 0.13f, 1f)
        }
        GLES20.glUniform1f(fogDensityHandle, 0.015f)
        GLES20.glUniformMatrix4fv(viewHandle, 1, false, view, 0)

        if (engine.scene == SceneMode.HUB) drawHub() else drawHunt()
        engine.snapshotParticles().forEach {
            drawSphere(it.pos.x, it.pos.y, it.pos.z, it.scale, it.color)
        }
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
        drawCube(0f, -0.14f, 0f, 44f, 0.25f, 44f, floatArrayOf(0.17f, 0.28f, 0.20f))
        val seeds = intArrayOf(2, 5, 8, 11, 15, 19, 23, 29, 31, 37, 41, 43)
        for (i in seeds.indices) {
            val x = ((seeds[i] * 7) % 34 - 17).toFloat()
            val z = ((seeds[i] * 11) % 34 - 17).toFloat()
            val h = 0.5f + (i % 3) * 0.35f
            val color = if (i % 4 == 0) floatArrayOf(0.34f, 0.56f, 0.32f) else floatArrayOf(0.24f, 0.36f, 0.28f)
            drawCube(x, h / 2f, z, 0.8f + (i % 3) * 0.4f, h, 0.7f + (i % 2) * 0.3f, color)
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
    }

    private fun drawPlayer() {
        val p = engine.player
        drawCylinder(p.x, 1.0f, p.z, 0.48f, 1.25f, floatArrayOf(0.32f, 0.54f, 0.86f))
        drawSphere(p.x, 1.85f, p.z, 0.42f, floatArrayOf(0.94f, 0.80f, 0.68f))
        drawCube(p.x - 0.23f, 0.35f, p.z, 0.15f, 0.55f, 0.2f, floatArrayOf(0.12f, 0.18f, 0.28f))
        drawCube(p.x + 0.23f, 0.35f, p.z, 0.15f, 0.55f, 0.2f, floatArrayOf(0.12f, 0.18f, 0.28f))
        val weaponColor = when (engine.weaponIndex) {
            0 -> floatArrayOf(0.50f, 0.80f, 1f)
            1 -> floatArrayOf(0.35f, 0.95f, 0.45f)
            2 -> floatArrayOf(0.75f, 0.40f, 1f)
            else -> floatArrayOf(0.90f, 0.55f, 0.22f)
        }
        drawCube(
            p.x + 0.6f,
            1.0f,
            p.z,
            if (engine.weaponIndex == 3) 0.85f else 0.16f,
            0.14f,
            if (engine.weaponIndex == 3) 0.25f else 0.72f,
            weaponColor
        )
    }

    private fun drawNpc(x: Float, y: Float, z: Float, color: FloatArray) {
        drawCylinder(x, y, z, 0.42f, 1.2f, color)
        drawSphere(x, y + 0.8f, z, 0.36f, floatArrayOf(0.95f, 0.78f, 0.64f))
        drawCube(x, 1.65f, z, 0.5f, 0.18f, 0.55f, color)
    }

    private fun drawEnemy(e: EnemyEntity) {
        val c = when (e.kind) {
            EnemyKind.AETHER_SLIME -> floatArrayOf(0.28f, 0.88f, 0.72f)
            EnemyKind.NEON_STALKER -> floatArrayOf(0.92f, 0.32f, 0.62f)
            EnemyKind.SCRAP_GOLEM -> floatArrayOf(0.52f, 0.58f, 0.66f)
            EnemyKind.OVERLOAD_TITAN -> floatArrayOf(0.67f, 0.24f, 1f)
        }

        if (e.kind == EnemyKind.AETHER_SLIME) {
            drawSphere(e.pos.x, 0.58f, e.pos.z, if (e.elite) 0.9f else 0.68f, if (e.hitFlash > 0f) floatArrayOf(1f, 1f, 1f) else c)
        } else if (e.kind == EnemyKind.NEON_STALKER) {
            drawCylinder(e.pos.x, 0.9f, e.pos.z, 0.44f, 1.3f, if (e.hitFlash > 0f) floatArrayOf(1f, 1f, 1f) else c)
            drawSphere(e.pos.x, 1.72f, e.pos.z, 0.40f, floatArrayOf(0.45f, 0.16f, 0.22f))
            drawCube(e.pos.x, 2.15f, e.pos.z, 0.08f, 0.45f, 0.55f, c)
        } else {
            val s = if (e.kind == EnemyKind.OVERLOAD_TITAN) 1.6f else 0.9f
            drawCube(e.pos.x, s, e.pos.z, s, s * 1.65f, s, if (e.hitFlash > 0f) floatArrayOf(1f, 1f, 1f) else c)
            drawSphere(e.pos.x, s * 2.0f, e.pos.z, s * 0.55f, c)
            if (e.kind == EnemyKind.OVERLOAD_TITAN) {
                drawTorus(e.pos.x, s * 2.1f, e.pos.z, s * 0.8f, floatArrayOf(1f, 0.32f, 0.72f))
            }
        }

        if (e.kind == EnemyKind.OVERLOAD_TITAN || e.elite) {
            drawTorus(e.pos.x, 0.12f, e.pos.z, e.kind.radius + 0.35f, floatArrayOf(1f, 0.32f, 0.72f))
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

private object ShaderProgram {
    fun create(): Int {
        val vertex = """
            uniform mat4 uMvp;
            uniform mat4 uModel;
            uniform mat4 uView;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying float vLight;
            varying float vDepth;
            uniform vec3 uLightDir;
            void main() {
                vec4 worldPos = uModel * vec4(aPosition, 1.0);
                vec3 n = normalize(mat3(uModel) * aNormal);
                vLight = max(0.28, dot(n, normalize(-uLightDir)) * 0.72 + 0.28);
                vDepth = -(uView * worldPos).z;
                gl_Position = uMvp * vec4(aPosition, 1.0);
            }
        """.trimIndent()

        val fragment = """
            precision mediump float;
            varying float vLight;
            varying float vDepth;
            uniform vec4 uColor;
            uniform vec4 uFogColor;
            uniform float uFogDensity;
            void main() {
                vec3 lit = uColor.rgb * vLight;
                float fog = clamp(1.0 - exp(-vDepth * uFogDensity), 0.0, 1.0);
                gl_FragColor = vec4(mix(lit, uFogColor.rgb, fog), uColor.a);
            }
        """.trimIndent()

        val v = compile(GLES20.GL_VERTEX_SHADER, vertex)
        val f = compile(GLES20.GL_FRAGMENT_SHADER, fragment)
        return GLES20.glCreateProgram().also { p ->
            GLES20.glAttachShader(p, v)
            GLES20.glAttachShader(p, f)
            GLES20.glLinkProgram(p)
            val ok = IntArray(1)
            GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
            if (ok[0] == 0) throw IllegalStateException(GLES20.glGetProgramInfoLog(p))
        }
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) throw IllegalStateException(GLES20.glGetShaderInfoLog(shader))
        return shader
    }
}

private class Mesh(private val vertices: FloatArray, private val normals: FloatArray) {
    private val vb: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply { put(vertices).position(0) }

    private val nb: FloatBuffer = ByteBuffer.allocateDirect(normals.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply { put(normals).position(0) }

    private val count = vertices.size / 3

    fun draw(
        pos: Int,
        normal: Int,
        mvpHandle: Int,
        modelHandle: Int,
        colorHandle: Int,
        mvp: FloatArray,
        model: FloatArray,
        color: FloatArray
    ) {
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(modelHandle, 1, false, model, 0)
        GLES20.glUniform4f(colorHandle, color[0], color[1], color[2], 1f)

        vb.position(0)
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, vb)

        nb.position(0)
        GLES20.glEnableVertexAttribArray(normal)
        GLES20.glVertexAttribPointer(normal, 3, GLES20.GL_FLOAT, false, 0, nb)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
        GLES20.glDisableVertexAttribArray(pos)
        GLES20.glDisableVertexAttribArray(normal)
    }

    companion object {
        fun plane(): Mesh {
            val v = floatArrayOf(
                -0.5f, 0f, -0.5f, 0.5f, 0f, -0.5f, 0.5f, 0f, 0.5f,
                -0.5f, 0f, -0.5f, 0.5f, 0f, 0.5f, -0.5f, 0f, 0.5f
            )
            val n = FloatArray(v.size) { i -> if (i % 3 == 1) 1f else 0f }
            return Mesh(v, n)
        }

        fun cube(): Mesh {
            val normals = arrayOf(
                floatArrayOf(0f, 0f, 1f),
                floatArrayOf(0f, 0f, -1f),
                floatArrayOf(1f, 0f, 0f),
                floatArrayOf(-1f, 0f, 0f),
                floatArrayOf(0f, 1f, 0f),
                floatArrayOf(0f, -1f, 0f)
            )
            val faceVerts = arrayOf(
                floatArrayOf(-1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f, 1f, -1f, -1f, 1f, 1f, 1f, 1f, -1f, 1f, 1f),
                floatArrayOf(1f, -1f, -1f, -1f, -1f, -1f, -1f, 1f, -1f, 1f, -1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f),
                floatArrayOf(1f, -1f, 1f, 1f, -1f, -1f, 1f, 1f, -1f, 1f, -1f, 1f, 1f, 1f, -1f, 1f, 1f, 1f),
                floatArrayOf(-1f, -1f, -1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, -1f, -1f, -1f, 1f, 1f, -1f, 1f, -1f),
                floatArrayOf(-1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, -1f, -1f, 1f, 1f, 1f, 1f, -1f, -1f, 1f, -1f),
                floatArrayOf(-1f, -1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f, -1f, -1f, -1f, 1f, -1f, 1f, -1f, -1f, 1f)
            )

            val v = FloatArray(6 * 18)
            val n = FloatArray(6 * 18)
            var offset = 0
            for (i in 0..5) {
                faceVerts[i].copyInto(v, offset)
                for (j in 0 until 18 step 3) {
                    n[offset + j] = normals[i][0]
                    n[offset + j + 1] = normals[i][1]
                    n[offset + j + 2] = normals[i][2]
                }
                offset += 18
            }
            return Mesh(v, n)
        }

        fun sphere(segs: Int, rings: Int): Mesh {
            val v = ArrayList<Float>()
            val n = ArrayList<Float>()
            for (r in 0 until rings) {
                val t0 = Math.PI * r / rings - Math.PI / 2.0
                val t1 = Math.PI * (r + 1) / rings - Math.PI / 2.0
                for (s in 0 until segs) {
                    val p0 = Math.PI * 2 * s / segs
                    val p1 = Math.PI * 2 * (s + 1) / segs
                    val pts = arrayOf(
                        floatArrayOf(cos(t0).toFloat() * cos(p0).toFloat(), sin(t0).toFloat(), cos(t0).toFloat() * sin(p0).toFloat()),
                        floatArrayOf(cos(t0).toFloat() * cos(p1).toFloat(), sin(t0).toFloat(), cos(t0).toFloat() * sin(p1).toFloat()),
                        floatArrayOf(cos(t1).toFloat() * cos(p1).toFloat(), sin(t1).toFloat(), cos(t1).toFloat() * sin(p1).toFloat()),
                        floatArrayOf(cos(t1).toFloat() * cos(p0).toFloat(), sin(t1).toFloat(), cos(t1).toFloat() * sin(p0).toFloat())
                    )
                    val order = intArrayOf(0, 1, 2, 0, 2, 3)
                    for (idx in order) {
                        val q = pts[idx]
                        v += q[0]; v += q[1]; v += q[2]
                        n += q[0]; n += q[1]; n += q[2]
                    }
                }
            }
            return Mesh(v.toFloatArray(), n.toFloatArray())
        }

        fun cylinder(segs: Int): Mesh {
            val v = ArrayList<Float>()
            val n = ArrayList<Float>()
            val y0 = -0.5f
            val y1 = 0.5f

            for (s in 0 until segs) {
                val a0 = Math.PI * 2 * s / segs
                val a1 = Math.PI * 2 * (s + 1) / segs
                val p = arrayOf(
                    floatArrayOf(cos(a0).toFloat(), y0, sin(a0).toFloat()),
                    floatArrayOf(cos(a1).toFloat(), y0, sin(a1).toFloat()),
                    floatArrayOf(cos(a1).toFloat(), y1, sin(a1).toFloat()),
                    floatArrayOf(cos(a0).toFloat(), y1, sin(a0).toFloat())
                )

                val ord = intArrayOf(0, 1, 2, 0, 2, 3)
                for (idx in ord) {
                    val q = p[idx]
                    v += q[0]; v += q[1]; v += q[2]
                    n += q[0]; n += 0f; n += q[2]
                }

                v += 0f; v += y1; v += 0f
                n += 0f; n += 1f; n += 0f
                v += p[2][0]; v += p[2][1]; v += p[2][2]
                n += 0f; n += 1f; n += 0f
                v += p[3][0]; v += p[3][1]; v += p[3][2]
                n += 0f; n += 1f; n += 0f

                v += 0f; v += y0; v += 0f
                n += 0f; n += -1f; n += 0f
                v += p[1][0]; v += p[1][1]; v += p[1][2]
                n += 0f; n += -1f; n += 0f
                v += p[0][0]; v += p[0][1]; v += p[0][2]
                n += 0f; n += -1f; n += 0f
            }
            return Mesh(v.toFloatArray(), n.toFloatArray())
        }

        fun torus(segs: Int, tubeSegs: Int): Mesh {
            val v = ArrayList<Float>()
            val n = ArrayList<Float>()
            val major = 0.78
            val minor = 0.14

            fun point(a: Double, b: Double): FloatArray = floatArrayOf(
                ((major + minor * cos(b)) * cos(a)).toFloat(),
                (minor * sin(b)).toFloat(),
                ((major + minor * cos(b)) * sin(a)).toFloat()
            )

            fun normal(a: Double, b: Double): FloatArray = floatArrayOf(
                (cos(b) * cos(a)).toFloat(),
                sin(b).toFloat(),
                (cos(b) * sin(a)).toFloat()
            )

            for (i in 0 until segs) {
                val a0 = 2 * Math.PI * i / segs
                val a1 = 2 * Math.PI * (i + 1) / segs
                for (j in 0 until tubeSegs) {
                    val b0 = 2 * Math.PI * j / tubeSegs
                    val b1 = 2 * Math.PI * (j + 1) / tubeSegs
                    val pts = listOf(point(a0, b0), point(a1, b0), point(a1, b1), point(a0, b1))
                    val ns = listOf(normal(a0, b0), normal(a1, b0), normal(a1, b1), normal(a0, b1))
                    val ord = intArrayOf(0, 1, 2, 0, 2, 3)
                    for (idx in ord) {
                        v += pts[idx][0]; v += pts[idx][1]; v += pts[idx][2]
                        n += ns[idx][0]; n += ns[idx][1]; n += ns[idx][2]
                    }
                }
            }
            return Mesh(v.toFloatArray(), n.toFloatArray())
        }
    }
}
