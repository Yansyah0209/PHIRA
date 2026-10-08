package com.phira.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.*

/** Portrait roll from gravity; no horizon claim and no reading when the lens points straight up/down. */
class LevelSensor(context: Context, private val onRoll: (Float?) -> Unit) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = manager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private var smooth: Float? = null
    fun start() { if (sensor == null) onRoll(null) else manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI) }
    fun stop() { manager.unregisterListener(this); smooth = null; onRoll(null) }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]; val y = event.values[1]
        if (hypot(x, y) < 2f || y < 0) { smooth = null; onRoll(null); return }
        val raw = Math.toDegrees(atan2(x.toDouble(), y.toDouble())).toFloat()
        smooth = smooth?.let { it * .8f + raw * .2f } ?: raw
        onRoll(smooth)
    }
}
