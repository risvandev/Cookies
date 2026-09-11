package com.tinkerhub.livingphone.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tinkerhub.livingphone.personality.EventType
import com.tinkerhub.livingphone.personality.PersonalityEngine
import com.tinkerhub.livingphone.service.LivingPhoneService

class MainActivity : ComponentActivity() {
    private lateinit var engine: PersonalityEngine
    private var isServiceRunningState = mutableStateOf(false)
    private var isBatteryOptimizedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = PersonalityEngine(this)

        updateStates()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isServiceRunning = isServiceRunningState.value,
                        isBatteryOptimized = isBatteryOptimizedState.value,
                        onToggleService = { toggleLivingService() },
                        onRequestBatteryExemption = { requestBatteryExemption() },
                        onTestAudio = { event -> engine.triggerEvent(event) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStates()
    }

    private fun updateStates() {
        isServiceRunningState.value = LivingPhoneService.isRunning
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        isBatteryOptimizedState.value = powerManager?.isIgnoringBatteryOptimizations(packageName) ?: false
    }

    private fun toggleLivingService() {
        val serviceIntent = Intent(this, LivingPhoneService::class.java)
        if (LivingPhoneService.isRunning) {
            stopService(serviceIntent)
            isServiceRunningState.value = false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            isServiceRunningState.value = true
        }
    }

    private fun requestBatteryExemption() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
}

@Composable
fun MainScreen(
    isServiceRunning: Boolean,
    isBatteryOptimized: Boolean,
    onToggleService: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onTestAudio: (EventType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LivingPhone 🎯",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Sentient Behaviour System",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Phone Soul Status Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isServiceRunning) Color(0xFF4CAF50) else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceRunning) "Soul Status: ACTIVE" else "Soul Status: ASLEEP",
                        fontWeight = FontWeight.SemiBold,
                        color = if (isServiceRunning) Color(0xFF2E7D32) else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isServiceRunning)
                        "Background sensing active (listening to gestures & charger 24/7)"
                    else
                        "Background service is stopped. Tap below to awaken.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onToggleService,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isServiceRunning) "Put Soul to Sleep (Stop Service)" else "Awaken Soul (Start Service)")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Battery Optimization Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Background Longevity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBatteryOptimized)
                        "✅ Unrestricted battery allowed. Android will not kill the soul."
                    else
                        "⚠️ Battery saver might put soul to sleep. Allow unrestricted background battery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!isBatteryOptimized) {
                    Button(
                        onClick = onRequestBatteryExemption,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow Unrestricted Battery")
                    }
                } else {
                    FilledTonalButton(
                        onClick = onRequestBatteryExemption,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configured (Tap to manage settings)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Test Chamber
        Text(
            text = "Voice Test Chamber",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Tap any button to preview the reaction sounds",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { onTestAudio(EventType.CHARGER_CONNECTED) }, modifier = Modifier.weight(1f)) {
                Text("Plug In")
            }
            OutlinedButton(onClick = { onTestAudio(EventType.CHARGER_DISCONNECTED) }, modifier = Modifier.weight(1f)) {
                Text("Unplug")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { onTestAudio(EventType.BATTERY_LEVEL_20) }, modifier = Modifier.weight(1f)) {
                Text("Bat 20%")
            }
            OutlinedButton(onClick = { onTestAudio(EventType.BATTERY_LEVEL_1) }, modifier = Modifier.weight(1f)) {
                Text("Bat 1%")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { onTestAudio(EventType.SHAKE_DETECTED) }, modifier = Modifier.weight(1f)) {
                Text("Shake")
            }
            OutlinedButton(onClick = { onTestAudio(EventType.OVERHEATING) }, modifier = Modifier.weight(1f)) {
                Text("Overheat")
            }
        }
    }
}
