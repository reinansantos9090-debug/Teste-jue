package com.example.testejuerpg.game3d

data class PerformanceSnapshot(
    val frameMs: Float,
    val averageFrameMs: Float,
    val fps: Float,
    val drawCalls: Int,
    val enemies: Int,
    val particles: Int,
    val projectiles: Int
)

class OfflinePerformanceTelemetry {
    private var emaMs = 16.67f
    private var lastFrameMs = 16.67f
    private var lastDrawCalls = 0
    private var lastEnemies = 0
    private var lastParticles = 0
    private var lastProjectiles = 0
    private var sampleCounter = 0

    fun record(frameMs: Float, drawCalls: Int, enemies: Int, particles: Int, projectiles: Int) {
        val safe = frameMs.coerceIn(1f, 100f)
        lastFrameMs = safe
        emaMs = emaMs * 0.92f + safe * 0.08f
        lastDrawCalls = drawCalls
        lastEnemies = enemies
        lastParticles = particles
        lastProjectiles = projectiles
        sampleCounter += 1
    }

    fun snapshot(): PerformanceSnapshot = PerformanceSnapshot(
        lastFrameMs, emaMs, 1000f / emaMs.coerceAtLeast(1f),
        lastDrawCalls, lastEnemies, lastParticles, lastProjectiles
    )

    fun samples(): Int = sampleCounter
}
