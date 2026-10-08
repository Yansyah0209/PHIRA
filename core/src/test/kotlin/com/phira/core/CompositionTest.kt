package com.phira.core

import kotlin.test.*

class CompositionTest {
    private val engine = CompositionEngine()
    private fun face(x: Float, y: Float) = Subject(x - .07f, y - .08f, x + .07f, y + .08f)
    @Test fun noDetectionHasNoInventedScore() { assertNull(engine.evaluate(emptyList(), Grid.PHI, Mode.PORTRAIT).score) }
    @Test fun alignmentAndCameraPanDirection() {
        assertTrue(engine.evaluate(listOf(face(.618f, .382f)), Grid.PHI, Mode.PORTRAIT).ready)
        assertEquals("Aim a little left", engine.evaluate(listOf(face(.52f, .382f)), Grid.PHI, Mode.PORTRAIT).title)
        assertEquals("Aim a little up", engine.evaluate(listOf(face(.618f, .25f)), Grid.PHI, Mode.PORTRAIT).title)
    }
    @Test fun clippingAndTiltBlockCapture() {
        assertFalse(engine.evaluate(listOf(Subject(-.1f, .2f, .4f, .6f)), Grid.NONE, Mode.OBJECT).ready)
        assertFalse(engine.evaluate(listOf(face(.618f, .382f)), Grid.PHI, Mode.PORTRAIT, 7f).ready)
    }
    @Test fun portraitRespectsSelectedGridAndLevelDirection() {
        assertTrue(engine.evaluate(listOf(face(.5f, .5f)), Grid.CENTER, Mode.PORTRAIT).ready)
        assertTrue(engine.evaluate(listOf(face(1f / 3, 1f / 3)), Grid.THIRDS, Mode.PORTRAIT).ready)
        assertEquals("Rotate counterclockwise slightly.", engine.evaluate(listOf(face(.618f, .382f)), Grid.PHI, Mode.PORTRAIT, 10f).detail)
    }
    @Test fun groupUsesEntireGroup() {
        assertTrue(engine.evaluate(listOf(face(.3f, .382f), face(.7f, .382f)), Grid.PHI, Mode.GROUP).ready)
    }
    @Test fun malformedDetectionIsIgnored() { assertNull(engine.evaluate(listOf(face(Float.NaN, .3f)), Grid.PHI, Mode.PORTRAIT).score) }
    @Test fun autoCaptureHoldsAndLatches() {
        val gate = CaptureGate(1000)
        assertFalse(gate.update(true, 0))
        assertFalse(gate.update(true, 900))
        assertTrue(gate.update(true, 1000))
        assertFalse(gate.update(true, 5000))
        assertFalse(gate.update(false, 6000))
        assertFalse(gate.update(true, 6001))
        assertTrue(gate.update(true, 7001))
    }
    @Test fun interruptedReadinessRestartsHold() {
        val gate = CaptureGate(1000)
        gate.update(true, 0); gate.update(false, 900)
        assertFalse(gate.update(true, 1100))
        assertFalse(gate.update(true, 1500))
    }
}
