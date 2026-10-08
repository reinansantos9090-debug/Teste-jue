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

class GameGLView(context: Context, private val engine: Game3DEngine) : GLSurfaceView(context) {
    private val renderer = GameRenderer(engine)

    init {
        setEGLContextClientVersion(MobileRenderProfile.detect(context).glesVersion)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        keepScreenOn = true
    }
}

