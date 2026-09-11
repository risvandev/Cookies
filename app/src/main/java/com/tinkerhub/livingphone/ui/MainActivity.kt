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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinkerhub.livingphone.alarm.AlarmState
import com.tinkerhub.livingphone.alarm.LivingAlarmManager
import com.tinkerhub.livingphone.personality.EventType
import com.tinkerhub.livingphone.personality.PersonalityEngine
import com.tinkerhub.livingphone.service.LivingPhoneService

class MainActivity : ComponentActivity() {
    private lateinit var engine: PersonalityEngine
    private lateinit var alarmManager: LivingAlarmManager

    private var isServiceRunningState = mutableStateOf(false)
    private var isBatteryOptimizedState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = PersonalityEngine(this)
        alarmManager = LivingAlarmManager(engine)

        updateStates()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val alarmState by alarmManager.alarmState.collectAsState()

                    MainAppContainer(
                        isServiceRunning = isServiceRunningState.value,
                        isBatteryOptimized = isBatteryOptimizedState.value,
                        alarmState = alarmState,
                        onToggleService = { toggleLivingService() },
                        onRequestBatteryExemption = { requestBatteryExemption() },
                        onManageBatterySettings = { openBatterySettings() },
                        onStartAlarm = { seconds -> alarmManager.startAlarm(seconds) },
                        onStopAlarm = { alarmManager.stopAlarm() },
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
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openBatterySettings()
        }
    }

    private fun openBatterySettings() {
        android.widget.Toast.makeText(this, "Opening App Battery Settings...", android.widget.Toast.LENGTH_SHORT).show()
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e2: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }
}

@Composable
fun MainAppContainer(
    isServiceRunning: Boolean,
    isBatteryOptimized: Boolean,
    alarmState: AlarmState,
    onToggleService: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onManageBatterySettings: () -> Unit,
    onStartAlarm: (Int) -> Unit,
    onStopAlarm: () -> Unit,
    onTestAudio: (EventType) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Phone Soul 👻", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Anxious Alarm ⏰", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            SoulDashboardScreen(
                isServiceRunning = isServiceRunning,
                isBatteryOptimized = isBatteryOptimized,
                onToggleService = onToggleService,
                onRequestBatteryExemption = onRequestBatteryExemption,
                onManageBatterySettings = onManageBatterySettings,
                onTestAudio = onTestAudio
            )
        } else {
            AlarmScreen(
                alarmState = alarmState,
                onStartAlarm = onStartAlarm,
                onStopAlarm = onStopAlarm
            )
        }
    }
}

@Composable
fun SoulDashboardScreen(
    isServiceRunning: Boolean,
    isBatteryOptimized: Boolean,
    onToggleService: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onManageBatterySettings: () -> Unit,
    onTestAudio: (EventType) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
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

        Spacer(modifier = Modifier.height(20.dp))

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
                        "Background sensing active (listening to gestures, late-night & charger 24/7)"
                    else
                        "Background service is stopped. Tap below to awaken.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
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
                        onClick = onManageBatterySettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configured (Tap to open App Info)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { onTestAudio(EventType.LATE_NIGHT) }, modifier = Modifier.weight(1f)) {
                Text("🌙 Late Night")
            }
            OutlinedButton(onClick = { onTestAudio(EventType.SCREEN_ON_IDLE_PICKUP) }, modifier = Modifier.weight(1f)) {
                Text("📱 Pickup")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { onTestAudio(EventType.ALARM_STARTED) }, modifier = Modifier.weight(1f)) {
                Text("⏰ Alarm Start")
            }
            OutlinedButton(onClick = { onTestAudio(EventType.ALARM_TWO_THIRDS) }, modifier = Modifier.weight(1f)) {
                Text("⚠️ Alarm 2/3")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { onTestAudio(EventType.ALARM_FINAL_5_SEC) }, modifier = Modifier.weight(1f)) {
                Text("🚨 Alarm 5s")
            }
            OutlinedButton(onClick = { onTestAudio(EventType.ALARM_DONE) }, modifier = Modifier.weight(1f)) {
                Text("🔔 Alarm Ring")
            }
        }
    }
}

@Composable
fun AlarmScreen(
    alarmState: AlarmState,
    onStartAlarm: (Int) -> Unit,
    onStopAlarm: () -> Unit
) {
    var selectedSeconds by remember { mutableStateOf(15) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Anxious Alarm ⏰",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "The alarm that nags and panics with you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Countdown & Stage Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val minutes = alarmState.remainingSeconds / 60
                val seconds = alarmState.remainingSeconds % 60
                val timeString = if (alarmState.isRunning) {
                    String.format("%02d:%02d", minutes, seconds)
                } else {
                    String.format("%02d:%02d", selectedSeconds / 60, selectedSeconds % 60)
                }

                Text(
                    text = timeString,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (alarmState.isRunning && alarmState.remainingSeconds <= 5)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )

                if (alarmState.isRunning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { alarmState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = alarmState.currentStage,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (alarmState.isRunning) {
                    Button(
                        onClick = onStopAlarm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel / Shut Alarm Up")
                    }
                } else {
                    Button(
                        onClick = { onStartAlarm(selectedSeconds) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Anxious Alarm")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!alarmState.isRunning) {
            Text(
                text = "Choose Alarm Duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Presets
            val presets = listOf(
                Pair("⚡ 15s (Instant Test)", 15),
                Pair("1 Minute", 60),
                Pair("5 Minutes", 300),
                Pair("15 Minutes", 900),
                Pair("25 Minutes", 1500)
            )

            presets.forEach { (label, secs) ->
                OutlinedButton(
                    onClick = { selectedSeconds = secs },
                    colors = if (selectedSeconds == secs)
                        ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else
                        ButtonDefaults.outlinedButtonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Info Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📢 How the Anxious Alarm behaves:",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Start: Mocks you for setting a timer (voice_alarm_started)\n" +
                           "• 2/3 Elapsed: Nags that most of your time is already gone (voice_alarm_twothirds)\n" +
                           "• Final 5 Secs: Total panic countdown (voice_alarm_final)\n" +
                           "• Time's Up: Final alarm bell/dialogue (voice_alarm_done)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
