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

private data class V3(var x: Float, var y: Float, var z: Float) {
    fun set(other: V3) { x = other.x; y = other.y; z = other.z }
    fun add(dx: Float, dy: Float, dz: Float) { x += dx; y += dy; z += dz }
}

private enum class SceneMode { HUB, HUNT, INVENTORY, MENU }

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
    PRISM_MOTH("Mariposa Prisma", 74f, 10f, 2.20f, 0.46f),
    SCRAP_DRONE("Drone de Sucata", 108f, 14f, 1.70f, 0.50f),
    PLASMA_EEL("Enguia de Plasma", 132f, 15f, 1.45f, 0.58f),
    VOID_BEETLE("Besouro do Vazio", 158f, 17f, 1.30f, 0.63f),
    AURORA_WRAITH("Espectro Aurora", 118f, 19f, 1.95f, 0.52f),
    MAGNET_HARE("Lebre Magnética", 101f, 12f, 2.40f, 0.48f),
    CRYSTAL_BRUTE("Bruto Cristalino", 285f, 24f, 0.78f, 0.90f),
    MEMORY_ECHO("Eco de Memória", 176f, 21f, 1.05f, 0.66f),
    PORTAL_LEECH("Sanguessuga de Portal", 148f, 18f, 1.55f, 0.56f),
    OVERLOAD_TITAN("Titã de Sobrecarga", 900f, 28f, 0.78f, 1.55f)
}

private data class WeaponPreset(
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

private data class EnemyEntity(
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
    var dead: Boolean = false,
    var elite: Boolean = false,
    var bossProfileId: String = "overload_titan"
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
        SceneMode.MENU -> { engine.closeRooftopMenu(); true }
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
        setEGLContextClientVersion(MobileRenderProfile.detect(context).glesVersion)
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

        if (engine.scene == SceneMode.MENU) {
            drawRooftopMenu(canvas, w, h)
            return
        }

        if (engine.scene == SceneMode.INVENTORY) {
            drawInventory(canvas, w, h)
            return
        }

        drawTopHud(canvas, w)
        drawControls(canvas, w, h)
        if (!engine.isDefeated && engine.hp / engine.maxHp < 0.20f) drawLowHpVignette(canvas, w, h)
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
        val cols = 4
        val gap = 10f
        val cardW = (w - 36f - gap * (cols - 1)) / cols
        for (i in WEAPONS.indices) {
            val col = i % cols
            val row = i / cols
            val left = 18f + col * (cardW + gap)
            val top = 92f + row * 83f
            paint.color = if (i == engine.weaponIndex) 0xFF3A2E61.toInt() else 0xFF172033.toInt()
            panel.set(left, top, left + cardW, top + 70f)
            c.drawRoundRect(panel, 13f, 13f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.textSize = 11f
            c.drawText(WEAPONS[i].name.take(18), left + 10f, top + 19f, paint)
            paint.color = 0xFFB4C5DA.toInt()
            paint.textSize = 8f
            c.drawText(WEAPONS[i].role.take(20), left + 10f, top + 35f, paint)
            paint.color = 0xFF86A4C8.toInt()
            paint.textSize = 7f
            c.drawText("1 " + WEAPONS[i].skill1.take(8) + " • 2 " + WEAPONS[i].skill2.take(8), left + 10f, top + 50f, paint)
            paint.color = WEAPONS[i].tint
            c.drawCircle(left + cardW - 17f, top + 17f, 6f, paint)
            paint.color = if (i == engine.weaponIndex) 0xFF8CF4BE.toInt() else 0xFF765EFF.toInt()
            paint.textSize = 7f
            c.drawText(if (i == engine.weaponIndex) "ATIVA" else "EQUIPAR", left + 10f, top + 63f, paint)
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

    private fun drawRooftopMenu(c: Canvas, w: Float, h: Float) {
        paint.color = 0xF20B1220.toInt()
        c.drawRect(0f, 0f, w, h, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 25f
        c.drawText(engine.menuTitle(), 24f, 42f, paint)
        paint.color = 0xFF9FB5CF.toInt()
        paint.textSize = 11f
        c.drawText(engine.menuSubtitle(), 24f, 62f, paint)

        paint.color = 0xFF1B2639.toInt()
        panel.set(w - 98f, 18f, w - 18f, 54f)
        c.drawRoundRect(panel, 12f, 12f, paint)
        paint.color = 0xFFEAF3FF.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 11f
        c.drawText("VOLTAR", w - 58f, 40f, paint)
        paint.textAlign = Paint.Align.LEFT

        when (engine.rooftopScreen()) {
            RooftopScreen.HOME -> {
                drawMenuSection(c, 82f, "PORTAL", engine.portalCards(), w)
                drawMenuSection(c, 292f, "QG", engine.utilityCards(), w)
            }
            RooftopScreen.PORTAL -> drawMenuSection(c, 82f, "DESTINOS", engine.portalCards(), w)
            RooftopScreen.EXPEDITIONS -> drawExpeditions(c, w, h)
            RooftopScreen.RIFTS -> drawRifts(c, w)
            RooftopScreen.VERSUS -> drawVersus(c, w)
            RooftopScreen.WARDROBE -> drawWardrobe(c, w)
            RooftopScreen.CORES -> drawCores(c, w, h)
            RooftopScreen.DAILY -> drawDaily(c, w, h)
            RooftopScreen.EVENTS -> drawEvents(c, w)
            RooftopScreen.PROFILE -> drawProfile(c, w)
            RooftopScreen.CRAFTING -> drawCrafting(c, w)
            RooftopScreen.STORY -> drawStory(c, w, h)
        }
    }

    private fun drawMenuSection(c: Canvas, top: Float, label: String, cards: List<com.example.testejuerpg.offline.systems.RooftopCard>, w: Float) {
        paint.color = 0xFF6B7E98.toInt()
        paint.textSize = 10f
        c.drawText(label, 24f, top, paint)
        cards.forEachIndexed { i, card ->
            val col = i % 2
            val row = i / 2
            val left = 18f + col * ((w - 54f) / 2f)
            val cardW = (w - 66f) / 2f
            val y = top + 12f + row * 93f
            paint.color = 0xFF1A2538.toInt()
            panel.set(left, y, left + cardW, y + 78f)
            c.drawRoundRect(panel, 16f, 16f, paint)
            paint.color = card.accent
            panel.set(left, y, left + 5f, y + 78f)
            c.drawRoundRect(panel, 3f, 3f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.textSize = 13f
            c.drawText(card.title, left + 15f, y + 24f, paint)
            paint.color = 0xFFAEC0D6.toInt()
            paint.textSize = 9f
            c.drawText(card.subtitle, left + 15f, y + 42f, paint)
            paint.color = 0xFF8399B4.toInt()
            paint.textSize = 8f
            c.drawText(card.detail.take(34), left + 15f, y + 59f, paint)
        }
    }

    private fun drawExpeditions(c: Canvas, w: Float, h: Float) {
        engine.expeditions().forEachIndexed { i, e ->
            val y = 84f + i * 78f
            paint.color = if (i == engine.selectedExpedition()) 0xFF2A3450.toInt() else 0xFF172033.toInt()
            panel.set(18f, y, w - 18f, y + 66f)
            c.drawRoundRect(panel, 15f, 15f, paint)
            paint.color = 0xFF78B9FF.toInt()
            paint.textSize = 12f
            c.drawText(e.name + " • " + e.world, 32f, y + 21f, paint)
            paint.color = 0xFFB0C3DA.toInt()
            paint.textSize = 8f
            c.drawText("Dificuldade " + e.difficulty + " • " + e.duration + "s", 32f, y + 37f, paint)
            c.drawText(e.jobs.take(2).joinToString(" • ") { it.title + " " + it.required }, 32f, y + 52f, paint)
            paint.color = 0xFF765EFF.toInt()
            panel.set(w - 118f, y + 14f, w - 30f, y + 52f)
            c.drawRoundRect(panel, 12f, 12f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 9f
            c.drawText("ENTRAR", w - 74f, y + 37f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
        paint.color = 0xFF8FA5BF.toInt()
        paint.textSize = 9f
        c.drawText("Selecione e entre. Tudo continua disponível sem conexão.", 20f, h - 26f, paint)
    }

    private fun drawRifts(c: Canvas, w: Float) {
        engine.rifts().forEachIndexed { i, r ->
            val y = 86f + i * 92f
            paint.color = if (i == engine.selectedRiftSlot()) 0xFF38284D.toInt() else 0xFF192235.toInt()
            panel.set(18f, y, w - 18f, y + 76f)
            c.drawRoundRect(panel, 15f, 15f, paint)
            paint.color = if (r.goal.type == "monster_arena") 0xFF68E0B1.toInt() else 0xFFB38CFF.toInt()
            paint.textSize = 13f
            c.drawText(r.name, 32f, y + 22f, paint)
            paint.color = 0xFFB0C3DA.toInt()
            paint.textSize = 9f
            c.drawText("Dificuldade " + r.difficulty + " • " + r.duration + "s", 32f, y + 40f, paint)
            c.drawText(r.goal.title + " • " + r.goal.amount, 32f, y + 57f, paint)
            paint.color = 0xFF8E6EFF.toInt()
            panel.set(w - 120f, y + 17f, w - 28f, y + 58f)
            c.drawRoundRect(panel, 12f, 12f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 9f
            c.drawText("ABRIR", w - 74f, y + 42f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawVersus(c: Canvas, w: Float) {
        engine.bots().forEachIndexed { i, bot ->
            val y = 84f + i * 84f
            paint.color = if (i == engine.selectedBot()) 0xFF422A3B.toInt() else 0xFF192235.toInt()
            panel.set(18f, y, w - 18f, y + 68f)
            c.drawRoundRect(panel, 15f, 15f, paint)
            paint.color = 0xFFFF7590.toInt()
            paint.textSize = 13f
            c.drawText(bot.name, 32f, y + 22f, paint)
            paint.color = 0xFFB7C7DB.toInt()
            paint.textSize = 9f
            c.drawText("Rival IA • Poder " + bot.difficulty + " • Score " + bot.score, 32f, y + 40f, paint)
            c.drawText("Vida " + bot.health.toInt() + " • Dano " + bot.attack.toInt(), 32f, y + 56f, paint)
            paint.color = 0xFFFF5474.toInt()
            panel.set(w - 120f, y + 16f, w - 28f, y + 53f)
            c.drawRoundRect(panel, 12f, 12f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 9f
            c.drawText("DESAFIAR", w - 74f, y + 38f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawWardrobe(c: Canvas, w: Float) {
        engine.styles().forEachIndexed { i, style ->
            val col = i % 2
            val row = i / 2
            val left = 18f + col * ((w - 54f) / 2f)
            val cardW = (w - 66f) / 2f
            val y = 82f + row * 72f
            paint.color = if (i == engine.activeStyleIndex()) 0xFF23444A.toInt() else 0xFF172033.toInt()
            panel.set(left, y, left + cardW, y + 58f)
            c.drawRoundRect(panel, 13f, 13f, paint)
            paint.color = if (style.locked) 0xFF6D7890.toInt() else 0xFF66E7E9.toInt()
            paint.textSize = 10f
            paint.color = OfflineVisualCatalog.forStyle(i).accent
            c.drawCircle(left + cardW - 18f, y + 18f, 7f, paint)
            paint.color = if (style.locked) 0xFF6D7890.toInt() else 0xFFFFFFFF.toInt()
            paint.textSize = 10f
            c.drawText("STYLE " + (i + 1), left + 12f, y + 18f, paint)
            paint.color = 0xFFB7C7DB.toInt()
            paint.textSize = 8f
            c.drawText(style.outfit + " • " + style.ride, left + 12f, y + 34f, paint)
            c.drawText(if (style.locked) "BLOQUEADO" else if (i == engine.activeStyleIndex()) "ATIVO" else "EQUIPAR", left + 12f, y + 49f, paint)
        }
    }

    private fun drawCores(c: Canvas, w: Float, h: Float) {
        engine.cores().take(10).forEachIndexed { i, core ->
            val y = 80f + i * 46f
            paint.color = 0xFF172033.toInt()
            panel.set(18f, y, w - 18f, y + 37f)
            c.drawRoundRect(panel, 10f, 10f, paint)
            paint.color = 0xFF7FE6A0.toInt()
            paint.textSize = 9f
            c.drawText(core.type.name, 30f, y + 15f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.textSize = 8f
            c.drawText("T" + core.tier + " • Carga " + core.charge, 30f, y + 29f, paint)
            paint.color = 0xFFAEC1D8.toInt()
            c.drawText(engine.coreBonusLabel(core), w - 168f, y + 22f, paint)
        }
        paint.color = 0xFF8FA5BF.toInt()
        paint.textSize = 9f
        c.drawText("Toque em um núcleo para equipá-lo na arma atual.", 20f, h - 24f, paint)
    }

    private fun drawDaily(c: Canvas, w: Float, h: Float) {
        engine.dailyGoals().forEachIndexed { i, goal ->
            val y = 84f + i * 61f
            paint.color = 0xFF172033.toInt()
            panel.set(18f, y, w - 18f, y + 49f)
            c.drawRoundRect(panel, 12f, 12f, paint)
            paint.color = 0xFFFFB36B.toInt()
            paint.textSize = 10f
            c.drawText(goal.title, 30f, y + 19f, paint)
            paint.color = 0xFFB4C6DB.toInt()
            paint.textSize = 8f
            c.drawText(goal.description + " • " + goal.rewardEnergy + " Energia", 30f, y + 36f, paint)
        }
        val summary = engine.hunterSummary()
        paint.color = 0xFF9DE6BF.toInt()
        paint.textSize = 10f
        c.drawText("Sequência: " + summary.dailyStreak + " dias", 20f, h - 24f, paint)
    }

    private fun drawEvents(c: Canvas, w: Float) {
        engine.events().forEachIndexed { i, event ->
            val y = 86f + i * 70f
            paint.color = if (i == engine.selectedEvent()) 0xFF4A3B22.toInt() else 0xFF172033.toInt()
            panel.set(18f, y, w - 18f, y + 56f)
            c.drawRoundRect(panel, 13f, 13f, paint)
            paint.color = 0xFFFFC857.toInt()
            paint.textSize = 11f
            c.drawText(event.name, 30f, y + 20f, paint)
            paint.color = 0xFFB6C8DE.toInt()
            paint.textSize = 8f
            c.drawText(event.modifier + " • " + event.durationSeconds + "s • x" + event.rewardMultiplier, 30f, y + 37f, paint)
            paint.color = 0xFFFFA64D.toInt()
            panel.set(w - 120f, y + 14f, w - 28f, y + 46f)
            c.drawRoundRect(panel, 10f, 10f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 8f
            c.drawText("ATIVAR", w - 74f, y + 34f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawProfile(c: Canvas, w: Float) {
        val s = engine.hunterSummary()
        listOf(
            "Nível de carreira" to s.careerLevel,
            "Nível da temporada" to s.seasonLevel,
            "Energia do caos" to s.chaosEnergy,
            "Abates" to s.kills,
            "Chefes" to s.bosses,
            "Rifts" to s.rifts,
            "Expedições" to s.expeditions,
            "Méritos de elite" to s.eliteMerits
        ).forEachIndexed { i, row ->
            val y = 86f + i * 40f
            paint.color = 0xFF172033.toInt()
            panel.set(22f, y, w - 22f, y + 31f)
            c.drawRoundRect(panel, 9f, 9f, paint)
            paint.color = 0xFFB8C9DF.toInt()
            paint.textSize = 9f
            c.drawText(row.first, 36f, y + 20f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.textAlign = Paint.Align.RIGHT
            c.drawText(row.second.toString(), w - 36f, y + 20f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawCrafting(c: Canvas, w: Float) {
        engine.recipes().forEachIndexed { i, recipe ->
            val y = 84f + i * 64f
            paint.color = 0xFF172033.toInt()
            panel.set(18f, y, w - 18f, y + 52f)
            c.drawRoundRect(panel, 12f, 12f, paint)
            paint.color = 0xFFD49DFF.toInt()
            paint.textSize = 10f
            c.drawText(recipe.name, 30f, y + 18f, paint)
            paint.color = 0xFFB7C8DD.toInt()
            paint.textSize = 8f
            c.drawText("Nível " + recipe.unlockLevel + " • " + recipe.ingredients.entries.joinToString(" + ") { it.key + " " + it.value }, 30f, y + 34f, paint)
            c.drawText("Resultado: " + recipe.result, 30f, y + 47f, paint)
        }
    }

    private fun drawStory(c: Canvas, w: Float, h: Float) {
        val s = engine.storySnapshot()
        paint.color = 0xFF172033.toInt()
        panel.set(18f, 82f, w - 18f, h - 154f)
        c.drawRoundRect(panel, 18f, 18f, paint)
        paint.color = 0xFFFF7BC8.toInt()
        paint.textSize = 10f
        c.drawText("CAPÍTULO " + s.chapterNumber + " • " + s.location, 32f, 106f, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 21f
        c.drawText(s.chapterTitle, 32f, 136f, paint)
        paint.color = 0xFFB8C9DE.toInt()
        paint.textSize = 10f
        c.drawText(s.synopsis, 32f, 158f, paint)
        paint.color = 0xFF8CA1BC.toInt()
        paint.textSize = 9f
        c.drawText("CENA " + s.beatNumber + "/" + s.beatTotal, 32f, 184f, paint)
        paint.color = 0xFFEAF2FC.toInt()
        paint.textSize = 15f
        c.drawText(s.speaker, 32f, 214f, paint)
        paint.color = 0xFFCBD8E8.toInt()
        paint.textSize = 13f
        var line = ""
        var lineY = 242f
        s.text.split(" ").forEach { word ->
            val candidate = if (line.isEmpty()) word else line + " " + word
            if (paint.measureText(candidate) > w - 70f) {
                c.drawText(line, 32f, lineY, paint)
                line = word
                lineY += 24f
            } else line = candidate
        }
        if (line.isNotEmpty()) c.drawText(line, 32f, lineY, paint)
        paint.color = 0xFF7890AC.toInt()
        paint.textSize = 10f
        c.drawText("Objetivo: " + s.objective + " • " + s.progress + "/" + s.required, 32f, h - 184f, paint)
        paint.color = 0xFF6B4C9A.toInt()
        panel.set(22f, h - 138f, w - 22f, h - 76f)
        c.drawRoundRect(panel, 17f, 17f, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 14f
        c.drawText(if (s.readyForBattle) "INICIAR MISSÃO DO CAPÍTULO" else "AVANÇAR HISTÓRIA", w / 2f, h - 100f, paint)
        paint.textAlign = Paint.Align.LEFT
        paint.color = 0xFF9AAECC.toInt()
        paint.textSize = 9f
        c.drawText("Campanha persistente • " + s.completedChapters + "/30 capítulos concluídos", 22f, h - 44f, paint)
    }

    private fun drawLowHpVignette(c: Canvas, w: Float, h: Float) {
        paint.color = 0x223C0A18
        c.drawRect(0f, 0f, w, 28f, paint)
        c.drawRect(0f, h - 28f, w, h, paint)
        c.drawRect(0f, 0f, 24f, h, paint)
        c.drawRect(w - 24f, 0f, w, h, paint)
        paint.color = 0x183C0A18
        c.drawRect(24f, 28f, w - 24f, 52f, paint)
        c.drawRect(24f, h - 52f, w - 24f, h - 28f, paint)
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
        c.drawText(engine.bossDisplayName().uppercase(Locale.getDefault()), w / 2f, 147f, paint)
        drawBar(c, left + 12f, 153f, bw - 24f, 12f, engine.bossHp / engine.bossMaxHp, 0xFFFF4C77.toInt())
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawBar(c: Canvas, x: Float, y: Float, width: Float, height: Float, fraction: Float, color: Int) {
        paint.color = 0x66334152.toInt()
        c.drawRoundRect(RectF(x, y, x + width, y + height), height, height, paint)
        paint.color = color
        c.drawRoundRect(RectF(x, y, x + width * fraction.coerceIn(0f, 1f), y + height), height, height, paint)
    }

    private fun handleMenuTap(x: Float, y: Float, w: Float, h: Float) {
        if (y < 70f || x > w - 120f) {
            engine.closeRooftopMenu()
            return
        }
        when (engine.rooftopScreen()) {
            RooftopScreen.HOME -> {
                if (y in 88f..280f) {
                    val col = if (x < w / 2f) 0 else 1
                    val row = ((y - 94f) / 93f).toInt().coerceIn(0, 1)
                    engine.openRooftopPage(engine.portalCards().getOrNull(row * 2 + col)?.id)
                } else if (y in 292f..h) {
                    val col = if (x < w / 2f) 0 else 1
                    val row = ((y - 304f) / 93f).toInt().coerceAtLeast(0)
                    engine.openRooftopPage(engine.utilityCards().getOrNull(row * 2 + col)?.id)
                }
            }
            RooftopScreen.PORTAL -> {
                val col = if (x < w / 2f) 0 else 1
                val row = ((y - 94f) / 93f).toInt().coerceIn(0, 1)
                engine.openRooftopPage(engine.portalCards().getOrNull(row * 2 + col)?.id)
            }
            RooftopScreen.EXPEDITIONS -> {
                val index = ((y - 84f) / 78f).toInt()
                if (index in engine.expeditions().indices) {
                    if (x > w - 145f) engine.launchExpedition(index) else engine.selectExpedition(index)
                }
            }
            RooftopScreen.RIFTS -> {
                val index = ((y - 86f) / 92f).toInt()
                if (index in 0..2) {
                    if (x > w - 145f) engine.launchRift(index) else engine.selectRiftSlot(index)
                }
            }
            RooftopScreen.VERSUS -> {
                val index = ((y - 84f) / 84f).toInt()
                if (index in engine.bots().indices) {
                    if (x > w - 145f) engine.launchVersus(index) else engine.selectBot(index)
                }
            }
            RooftopScreen.WARDROBE -> {
                val col = if (x < w / 2f) 0 else 1
                val row = ((y - 82f) / 72f).toInt()
                val index = row * 2 + col
                if (index in engine.styles().indices) {
                    if (engine.styles()[index].locked) engine.unlockStyle(index) else engine.selectStyle(index)
                }
            }
            RooftopScreen.CORES -> {
                val index = ((y - 80f) / 46f).toInt()
                if (index in 0..9) engine.equipVisibleCore(index)
            }
            RooftopScreen.EVENTS -> {
                val index = ((y - 86f) / 70f).toInt()
                if (index in engine.events().indices) {
                    engine.selectEvent(index)
                    if (x > w - 145f) engine.activateEvent()
                }
            }
            RooftopScreen.CRAFTING -> {
                val index = ((y - 84f) / 64f).toInt()
                if (index in engine.recipes().indices) engine.craft(index)
            }
            RooftopScreen.DAILY, RooftopScreen.PROFILE -> Unit
            RooftopScreen.STORY -> {
                if (y > h - 150f) {
                    if (engine.storySnapshot().readyForBattle) engine.launchStory() else engine.advanceStoryBeat()
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (engine.scene == SceneMode.MENU) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                handleMenuTap(event.x, event.y, width.toFloat(), height.toFloat())
                invalidate()
            }
            return true
        }

        if (engine.scene == SceneMode.INVENTORY) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                val y = event.y
                val cols = 4
                val gap = 10f
                val cardW = (width.toFloat() - 36f - gap * (cols - 1)) / cols
                if (y > 92f && y < 92f + 6 * 83f && event.x >= 18f) {
                    val col = ((event.x - 18f) / (cardW + gap)).toInt().coerceIn(0, cols - 1)
                    val row = ((y - 92f) / 83f).toInt().coerceIn(0, 5)
                    val idx = row * cols + col
                    if (idx in WEAPONS.indices) engine.equipWeapon(idx)
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
                if (px > w - 92f && py < 118f) {
                    engine.openRooftopMenu()
                } else if (px < 180f && py > h - 210f && joystickPointer == -1) {
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
                    engine.openRooftopMenu()
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
    private val rooftopController = OfflineRooftopController(hunterDirector)
    private val storyCampaign = OfflineStoryCampaign(context)
    private val renderProfile = MobileRenderProfile.detect(context)
    private val performanceGovernor = MobilePerformanceGovernor(renderProfile)
    private val performanceTelemetry = OfflinePerformanceTelemetry()
    private val audioBus = OfflineAudioBus()

    private var activityMode = OfflineMode.EXPEDITION
    private var activityTarget = 12
    private var activityCompleted = false

    private var moveX = 0f
    private var moveY = 0f
    private var primaryTimer = 0f
    private val skillTimers = floatArrayOf(0f, 0f, 0f)
    private var waveTimer = 0f
    private var storyWaveTimer = 0f
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
        hunterDirector.loadFrom(prefs)
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
        player.x = (player.x + nx * 4.5f * dt).coerceIn(-18f, 18f)
        player.z = (player.z + nz * 4.5f * dt).coerceIn(-18f, 18f)
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
                e.phase2 = e.hp <= bossMaxHp * 0.50f
                e.specialTimer = max(0f, e.specialTimer - dt)
                e.summonTimer = max(0f, e.summonTimer - dt)
                if (e.phase2 && e.specialTimer <= 0f) {
                    e.specialTimer = profile.projectileInterval
                    repeat(if (profile.id == "storm_behemoth") 3 else 1) { spawnBossProjectile(e) }
                    spawnBurst(e.pos, 0.25f, rgbColor(profile.aura))
                }
                if (e.phase2 && e.summonTimer <= 0f && enemies.count { !it.dead } < min(9, performanceGovernor.allowedEnemies())) {
                    e.summonTimer = profile.summonInterval
                    repeat(if (profile.id == "aether_guardian") 3 else 2) { spawnEnemy() }
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

            if (dist > 1.6f + e.kind.radius) {
                val inv = 1f / max(0.001f, dist)
                val speedMultiplier = if (e.kind == EnemyKind.OVERLOAD_TITAN && e.phase2) {
                    OfflineBossCatalog.forId(e.bossProfileId).phase2Multiplier
                } else 1f
                e.pos.x += dx * inv * e.kind.speed * speedMultiplier * dt
                e.pos.z += dz * inv * e.kind.speed * speedMultiplier * dt
            } else if (e.attackTimer <= 0f) {
                val bossProfile = if (e.kind == EnemyKind.OVERLOAD_TITAN) OfflineBossCatalog.forId(e.bossProfileId) else null
                e.attackTimer = bossProfile?.projectileInterval?.coerceAtLeast(0.8f) ?: 1.5f
                val contactDamage = bossProfile?.contactDamage ?: e.kind.attack
                takeDamage(contactDamage * if (e.elite) 1.25f else 1f)
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
            huntKills < 4 -> listOf(EnemyKind.AETHER_SLIME, EnemyKind.NEON_STALKER, EnemyKind.PRISM_MOTH)
            huntKills < 10 -> listOf(
                EnemyKind.AETHER_SLIME, EnemyKind.NEON_STALKER, EnemyKind.SCRAP_GOLEM,
                EnemyKind.SCRAP_DRONE, EnemyKind.PLASMA_EEL, EnemyKind.MAGNET_HARE
            )
            else -> listOf(
                EnemyKind.NEON_STALKER, EnemyKind.SCRAP_GOLEM, EnemyKind.VOID_BEETLE,
                EnemyKind.AURORA_WRAITH, EnemyKind.CRYSTAL_BRUTE, EnemyKind.MEMORY_ECHO,
                EnemyKind.PORTAL_LEECH
            )
        }
        spawnEnemyOfKind(choices[random.nextInt(choices.size)], elite)
    }

    private fun spawnEnemyOfKind(kind: EnemyKind, elite: Boolean = false) {
        if (enemies.count { !it.dead } >= performanceGovernor.allowedEnemies()) return
        val angle = random.nextFloat() * 6.283f
        val distance = 8f + random.nextFloat() * 8f
        val pos = V3(
            cos(angle) * distance,
            if (kind == EnemyKind.SCRAP_GOLEM || kind == EnemyKind.OVERLOAD_TITAN) 1.0f else 0.65f,
            sin(angle) * distance
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
        objectiveText = "CAPÍTULO " + completed.number + " CONCLUÍDO • +" + completed.rewardGold + " Ouro"
        save()
    }

    private fun beginBattle() {
        scene = SceneMode.HUNT
        isDefeated = false
        bossActive = false
        bossSpawned = false
        bossDead = false
        activityCompleted = false
        huntKills = 0
        storyWaveTimer = 0f
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
    private var rimHandle = 0
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
        drawCalls = 0
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
                drawTorus(e.pos.x, s * 2.1f, e.pos.z, s * 0.8f, rgb(bossProfile?.aura ?: 0xFF4C8F))
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
            uniform float uRimStrength;
            void main() {
                vec3 lit = uColor.rgb * vLight;
                float fog = clamp(1.0 - exp(-vDepth * uFogDensity), 0.0, 1.0);
                float rim = pow(1.0 - max(vLight - 0.22, 0.0), 1.8) * uRimStrength;
                lit += uColor.rgb * rim;
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
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 0, vb)

        nb.position(0)
        GLES20.glVertexAttribPointer(normal, 3, GLES20.GL_FLOAT, false, 0, nb)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
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
