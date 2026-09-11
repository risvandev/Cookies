package com.tinkerhub.livingphone.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.tinkerhub.livingphone.personality.EventType
import com.tinkerhub.livingphone.personality.PersonalityEngine
import kotlin.math.sqrt

class MotionSensorManager(
    context: Context,
    private val engine: PersonalityEngine
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastShakeTime = 0L
    private var shakeCount = 0
    private var lastShakeTimestamp = 0L

    // Requires deliberate, firm acceleration (similar to a firm double-chop gesture for torch)
    // 1.0G = resting on table. 3.4G+ = firm, intentional chop/shake gesture.
    private val SHAKE_THRESHOLD_G = 3.4f
    private val REQUIRED_SHAKE_CYCLES = 2 // Needs at least 2 distinct chops/shakes within the window
    private val GESTURE_TIME_WINDOW_MS = 700L // 700ms window to complete the gesture

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate total G-force
            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            // Only register strong deliberate motion spikes
            if (gForce > SHAKE_THRESHOLD_G) {
                val now = System.currentTimeMillis()

                // If the previous motion peak was too long ago, reset the gesture count
                if (now - lastShakeTimestamp > GESTURE_TIME_WINDOW_MS) {
                    shakeCount = 0
                }

                // Debounce between consecutive spikes in the same gesture (min 120ms apart)
                if (now - lastShakeTimestamp > 120) {
                    shakeCount++
                    lastShakeTimestamp = now

                    // When the full gesture cycle is completed (e.g. 2 chops)
                    if (shakeCount >= REQUIRED_SHAKE_CYCLES) {
                        if (now - lastShakeTime > 2500) { // 2.5s cooldown between audio triggers
                            engine.triggerEvent(EventType.SHAKE_DETECTED)
                            lastShakeTime = now
                        }
                        shakeCount = 0
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
}
