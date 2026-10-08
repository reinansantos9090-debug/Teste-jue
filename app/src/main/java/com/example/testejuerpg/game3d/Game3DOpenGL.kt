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


object ShaderProgram {
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

class Mesh(private val vertices: FloatArray, private val normals: FloatArray) {
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

