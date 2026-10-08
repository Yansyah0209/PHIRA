package com.phira.core

import kotlin.math.abs
import kotlin.math.hypot

enum class Grid(val label: String, val anchors: List<Float>) {
    PHI("Phi", listOf(0.381966f, 0.618034f)),
    THIRDS("Thirds", listOf(1f / 3, 2f / 3)),
    CENTER("Center", listOf(0.5f)),
    NONE("Free", emptyList())
}
enum class Mode(val label: String) { PORTRAIT("Portrait"), GROUP("Group"), OBJECT("Object") }
data class Point(val x: Float, val y: Float)
data class Subject(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val center get() = Point((left + right) / 2, (top + bottom) / 2)
    val area get() = (right - left).coerceAtLeast(0f) * (bottom - top).coerceAtLeast(0f)
}
data class Guidance(
    val title: String,
    val detail: String,
    val score: Int? = null,
    val target: Point? = null,
    val ready: Boolean = false
)

/** A geometric alignment indicator, never a learned aesthetic score. Coordinates are preview-normalized. */
class CompositionEngine {
    fun evaluate(subjects: List<Subject>, grid: Grid, mode: Mode, roll: Float? = null): Guidance {
        val valid = subjects.filter { it.area > 0 && listOf(it.left, it.top, it.right, it.bottom).all(Float::isFinite) }
        if (valid.isEmpty()) return Guidance("Find your subject", if (mode == Mode.OBJECT) "Point at a distinct object." else "Keep a face visible in the frame.")
        val subject = if (mode == Mode.GROUP) Subject(valid.minOf { it.left }, valid.minOf { it.top }, valid.maxOf { it.right }, valid.maxOf { it.bottom }) else valid.maxBy { it.area }
        val anchors = if (mode == Mode.GROUP || grid == Grid.CENTER) listOf(0.5f) else grid.anchors
        val target = if (anchors.isEmpty()) subject.center else Point(anchors.minBy { abs(it - subject.center.x) }, if (mode == Mode.OBJECT) anchors.minBy { abs(it - subject.center.y) } else if (grid == Grid.THIRDS) 1f / 3 else if (grid == Grid.CENTER) .5f else .381966f)
        val dx = target.x - subject.center.x
        val dy = target.y - subject.center.y
        val distance = hypot(dx, dy)
        val clipped = subject.left < 0.035f || subject.right > 0.965f || subject.top < 0.035f || subject.bottom > 0.965f
        val score = ((1 - (distance / 0.5f).coerceIn(0f, 1f)) * 100).toInt().coerceAtMost(if (clipped) 60 else 100)
        if (clipped) return Guidance("Give it some room", "Step back to keep the detected subject inside the frame.", score, target)
        if (roll != null && abs(roll) > 3f) return Guidance("Level your camera", "Rotate ${if (roll > 0) "counterclockwise" else "clockwise"} slightly.", score, target)
        if (distance < 0.055f) return Guidance("Nicely composed", "Framing aligned. Capture when the moment feels right.", score, target, true)
        // Pan direction is opposite the desired movement of the subject in the image.
        return if (abs(dx) > abs(dy)) Guidance("Aim a little ${if (dx > 0) "left" else "right"}", "Move the camera gently toward the guide.", score, target)
        else Guidance("Aim a little ${if (dy > 0) "up" else "down"}", "A small tilt will bring the subject into place.", score, target)
    }
}

/** Requires uninterrupted readiness and a release before another automatic shot. */
class CaptureGate(private val holdMs: Long = 1400) {
    private var since: Long? = null
    private var latched = false
    fun update(ready: Boolean, now: Long): Boolean {
        if (!ready) { since = null; latched = false; return false }
        if (latched) return false
        val start = since ?: now.also { since = it }
        if (now - start < holdMs) return false
        latched = true
        return true
    }
    fun reset() { since = null; latched = false }
}
