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


class GameHUDView(context: Context, private val engine: Game3DEngine) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panel = RectF()
    private val joystickCenter = V3(0f, 0f, 0f)
    private var joystickPointer = -1
    private var joystickDx = 0f
    private var joystickDy = 0f

    init {
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
        paint.color = 0xFF1F2D42.toInt()
        panel.set(w - 130f, 164f, w - 18f, 204f)
        c.drawRoundRect(panel, 12f, 12f, paint)
        paint.color = 0xFFBBD0E8.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 9f
        c.drawText("GESTO • " + engine.activeEmote().name, w - 74f, 189f, paint)
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
        paint.color = 0xFF8EA4BE.toInt()
        paint.textSize = 8f
        c.drawText(
            "24 armas • 12 estilos • 30 capítulos • " + engine.historyEntries().size + " registros históricos • esquadrão IA offline",
            22f, 414f, paint
        )
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
        paint.color = if (engine.bossWeakPointOpen()) 0xFFFFE477.toInt() else 0xFF9AAECC.toInt()
        paint.textSize = 8f
        c.drawText(if (engine.bossWeakPointOpen()) "PONTO FRACO ABERTO • DANO x2" else "PONTO FRACO FECHADO", left + 12f, 181f, paint)
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
                } else if (px > w - 130f && py in 164f..210f && engine.scene == SceneMode.HUB) {
                    engine.cycleEmote()
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

