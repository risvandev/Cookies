package com.tinkerhub.livingphone.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.tinkerhub.livingphone.personality.EventType
import com.tinkerhub.livingphone.personality.PersonalityEngine

class PowerStateManager(
    private val context: Context,
    private val engine: PersonalityEngine
) {
    private var lastReportedLevel = -1
    private var isCurrentlyCharging = false

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    isCurrentlyCharging = true
                    engine.triggerEvent(EventType.CHARGER_CONNECTED)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isCurrentlyCharging = false
                    engine.triggerEvent(EventType.CHARGER_DISCONNECTED)
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                    
                    val batteryPct = if (level != -1 && scale != -1) {
                        (level * 100) / scale
                    } else -1

                    val tempCelsius = tempTenths / 10f

                    checkBatteryLevel(batteryPct)
                    checkTemperature(tempCelsius)
                }
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(powerReceiver, filter)
    }

    fun stop() {
        try {
            context.unregisterReceiver(powerReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }

    private fun checkBatteryLevel(level: Int) {
        if (level == -1 || isCurrentlyCharging) return

        // Only trigger once per threshold
        if (level != lastReportedLevel) {
            when (level) {
                20 -> if (lastReportedLevel > 20) engine.triggerEvent(EventType.BATTERY_LEVEL_20)
                10 -> if (lastReportedLevel > 10) engine.triggerEvent(EventType.BATTERY_LEVEL_10)
                5 -> if (lastReportedLevel > 5) engine.triggerEvent(EventType.BATTERY_LEVEL_5)
                1 -> if (lastReportedLevel > 1) engine.triggerEvent(EventType.BATTERY_LEVEL_1)
            }
            lastReportedLevel = level
        }
    }

    private var lastTempWarningTime = 0L
    private fun checkTemperature(tempCelsius: Float) {
        if (tempCelsius > 40.0f) {
            val now = System.currentTimeMillis()
            // Warn at most once every 5 minutes
            if (now - lastTempWarningTime > 5 * 60 * 1000) {
                engine.triggerEvent(EventType.OVERHEATING)
                lastTempWarningTime = now
            }
        }
    }
}
