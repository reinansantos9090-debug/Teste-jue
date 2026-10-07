package com.example.testejuerpg.game3d

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ConfigurationInfo
import android.os.Build

enum class MobileRenderTier { ECO, BALANCED, QUALITY }

data class MobileRenderProfile(
    val tier: MobileRenderTier,
    val maxEnemies: Int,
    val maxParticles: Int,
    val maxFireflies: Int,
    val sphereSegments: Int,
    val sphereRings: Int,
    val torusSegments: Int,
    val torusTubeSegments: Int,
    val fogDensity: Float,
    val glesVersion: Int,
    val targetFrameMs: Float
) {
    companion object {
        fun detect(context: Context): MobileRenderProfile {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryClassMb = activityManager.memoryClass
            val info: ConfigurationInfo? = activityManager.deviceConfigurationInfo
            val gles3 = info?.reqGlEsVersion?.let { it >= 0x30000 } == true
            val ramFactor = when {
                memoryClassMb >= 512 -> MobileRenderTier.QUALITY
                memoryClassMb >= 256 -> MobileRenderTier.BALANCED
                else -> MobileRenderTier.ECO
            }
            val tier = when {
                Build.VERSION.SDK_INT >= 31 && gles3 && ramFactor == MobileRenderTier.QUALITY -> MobileRenderTier.QUALITY
                ramFactor == MobileRenderTier.ECO -> MobileRenderTier.ECO
                else -> MobileRenderTier.BALANCED
            }
            return when (tier) {
                MobileRenderTier.QUALITY -> MobileRenderProfile(tier, 20, 360, 28, 20, 12, 28, 10, 0.010f, if (gles3) 3 else 2, 16.67f)
                MobileRenderTier.BALANCED -> MobileRenderProfile(tier, 16, 240, 20, 16, 10, 24, 8, 0.014f, if (gles3) 3 else 2, 20.0f)
                MobileRenderTier.ECO -> MobileRenderProfile(tier, 10, 120, 12, 12, 8, 16, 6, 0.020f, if (gles3) 3 else 2, 33.33f)
            }
        }
    }
}

class MobilePerformanceGovernor(private val profile: MobileRenderProfile) {
    private var emaFrameMs = profile.targetFrameMs
    private var heavyFrames = 0
    private var coolFrames = 0
    private var particleScale = 1f
    private var enemyScale = 1f
    fun sample(frameMs: Float) {
        val safe = frameMs.coerceIn(1f, 80f)
        emaFrameMs = emaFrameMs * 0.94f + safe * 0.06f
        if (emaFrameMs > profile.targetFrameMs * 1.18f) {
            heavyFrames += 1; coolFrames = 0
        } else if (emaFrameMs < profile.targetFrameMs * 0.82f) {
            coolFrames += 1; heavyFrames = 0
        } else {
            heavyFrames = 0; coolFrames = 0
        }
        if (heavyFrames >= 12) {
            particleScale = (particleScale * 0.88f).coerceAtLeast(0.45f)
            enemyScale = (enemyScale * 0.96f).coerceAtLeast(0.70f)
            heavyFrames = 0
        }
        if (coolFrames >= 60) {
            particleScale = (particleScale * 1.06f).coerceAtMost(1f)
            enemyScale = (enemyScale * 1.025f).coerceAtMost(1f)
            coolFrames = 0
        }
    }
    fun allowedEnemies(): Int = (profile.maxEnemies * enemyScale).toInt().coerceAtLeast(4)
    fun allowedParticles(): Int = (profile.maxParticles * particleScale).toInt().coerceAtLeast(32)
    fun allowedFireflies(): Int = (profile.maxFireflies * particleScale).toInt().coerceAtLeast(4)
    fun currentFrameEmaMs(): Float = emaFrameMs
}
