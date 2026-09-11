package com.tinkerhub.livingphone.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tinkerhub.livingphone.personality.PersonalityEngine
import com.tinkerhub.livingphone.sensors.MotionSensorManager
import com.tinkerhub.livingphone.sensors.PowerStateManager
import com.tinkerhub.livingphone.sensors.ScreenStateTracker

class LivingPhoneService : Service() {

    private lateinit var engine: PersonalityEngine
    private lateinit var powerStateManager: PowerStateManager
    private lateinit var motionSensorManager: MotionSensorManager
    private lateinit var screenStateTracker: ScreenStateTracker

    override fun onCreate() {
        super.onCreate()
        
        engine = PersonalityEngine(this)
        powerStateManager = PowerStateManager(this, engine)
        motionSensorManager = MotionSensorManager(this, engine)
        screenStateTracker = ScreenStateTracker(this, engine)

        powerStateManager.start()
        motionSensorManager.start()
        screenStateTracker.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        powerStateManager.stop()
        motionSensorManager.stop()
        screenStateTracker.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "living_phone_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Living Phone Core",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Living Phone soul active"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Soul is Active")
            .setContentText("Your phone is currently sensing the world.")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
