package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChargingSession
import com.example.ui.theme.GeometricAlert
import com.example.ui.theme.GeometricBg
import com.example.ui.theme.GeometricBorder
import com.example.ui.theme.GeometricGreen
import com.example.ui.theme.GeometricOnPrimaryContainer
import com.example.ui.theme.GeometricPrimary
import com.example.ui.theme.GeometricPrimaryContainer
import com.example.ui.theme.GeometricSecondary
import com.example.ui.theme.GeometricSurface
import com.example.ui.theme.GeometricTextDark
import com.example.ui.theme.GeometricTextGray
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val batteryState by viewModel.batteryState.collectAsStateWithLifecycle()
    val limit by viewModel.autocutLimit.collectAsStateWithLifecycle()
    val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsStateWithLifecycle()
    val isTtsEnabled by viewModel.isTtsEnabled.collectAsStateWithLifecycle()
    val isWebhookEnabled by viewModel.isWebhookEnabled.collectAsStateWithLifecycle()
    val webhookUrl by viewModel.webhookUrl.collectAsStateWithLifecycle()
    val webhookMethod by viewModel.webhookMethod.collectAsStateWithLifecycle()
    val webhookBody by viewModel.webhookBody.collectAsStateWithLifecycle()
    val isSimulationActive by viewModel.isSimulationActive.collectAsStateWithLifecycle()
    val autocutTriggered by viewModel.autocutTriggered.collectAsStateWithLifecycle()
    val webhookLogs by viewModel.webhookLogs.collectAsStateWithLifecycle()
    val histories by viewModel.chargingHistory.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Monitor, 1 = Config/Webhook, 2 = Riwayat

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(GeometricPrimaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚡",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                text = "VoltGuard Eco",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = GeometricTextDark
                            )
                            Text(
                                text = "Geometric Smart Control",
                                style = MaterialTheme.typography.bodySmall,
                                color = GeometricTextGray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GeometricBg,
                    titleContentColor = GeometricTextDark
                ),
                actions = {
                    // Geometric status indicator beacon
                    val isPlugged = batteryState.isCharging
                    val colorBrush = if (autocutTriggered) {
                        Brush.radialGradient(listOf(GeometricAlert, Color.Transparent))
                    } else if (isPlugged) {
                        Brush.radialGradient(listOf(GeometricGreen, Color.Transparent))
                    } else {
                        Brush.radialGradient(listOf(GeometricTextGray, Color.Transparent))
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(24.dp)
                            .drawBehind {
                                drawCircle(brush = colorBrush, radius = size.minDimension / 2f)
                            }
                    )
                }
            )
        },
        containerColor = GeometricBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Segmented Tab Selector strictly respecting Geometric Balance specifications
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, GeometricBorder, RoundedCornerShape(24.dp))
                    .background(GeometricSurface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("MONITOR", "WEBHOOK", "RIWAYAT").forEachIndexed { index, label ->
                    val isSelected = activeTab == index
                    val animBg by animateColorAsState(
                        targetValue = if (isSelected) GeometricPrimary else Color.Transparent,
                        label = "tabBg"
                    )
                    val animText by animateColorAsState(
                        targetValue = if (isSelected) Color.White else GeometricTextGray,
                        label = "tabText"
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(animBg)
                            .clickable { activeTab = index }
                            .testTag("tab_button_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = animText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    (slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut())
                },
                label = "tabContent"
            ) { targetTab ->
                when (targetTab) {
                    0 -> DashboardTab(
                        batteryState = batteryState,
                        limit = limit,
                        autocutTriggered = autocutTriggered,
                        isSimulationActive = isSimulationActive,
                        viewModel = viewModel
                    )
                    1 -> ConfigTab(
                        limit = limit,
                        isAlarmEnabled = isAlarmEnabled,
                        isTtsEnabled = isTtsEnabled,
                        isWebhookEnabled = isWebhookEnabled,
                        webhookUrl = webhookUrl,
                        webhookMethod = webhookMethod,
                        webhookBody = webhookBody,
                        webhookLogs = webhookLogs,
                        viewModel = viewModel
                    )
                    2 -> HistoryTab(
                        histories = histories,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardTab(
    batteryState: BatteryState,
    limit: Int,
    autocutTriggered: Boolean,
    isSimulationActive: Boolean,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High-Fidelity Circular Battery Visualizer on pure white background
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(32.dp), clip = false),
            colors = CardDefaults.cardColors(containerColor = GeometricSurface),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, GeometricBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularBatteryTelemetry(
                    level = batteryState.level,
                    isCharging = batteryState.isCharging,
                    targetLimit = limit,
                    autocutTriggered = autocutTriggered
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Inline Telemetry status Row inside visualizer card
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SUHU",
                            color = GeometricTextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${batteryState.temperature}°C",
                            color = if (batteryState.temperature > 40.0) GeometricAlert else GeometricTextDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(modifier = Modifier.size(1.dp, 24.dp).background(GeometricBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "VOLT",
                            color = GeometricTextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", batteryState.voltage / 1000.0)}V",
                            color = GeometricTextDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(modifier = Modifier.size(1.dp, 24.dp).background(GeometricBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ESTIMASI",
                            color = GeometricTextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (batteryState.isCharging) "${String.format(Locale.US, "%.1f", batteryState.estimatedWattage)}W" else "0W",
                            color = if (batteryState.isCharging) GeometricGreen else GeometricTextGray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Tactile Protection Auto-cut Indicator (Primary banner in lavender container)
        StatusAlertBanner(batteryState, limit, autocutTriggered)

        // Telemetry grid parameters card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(28.dp), clip = false),
            colors = CardDefaults.cardColors(containerColor = GeometricSurface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, GeometricBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔋 DETAIL KELAYAKAN DAYA",
                    color = GeometricPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    TelemetryItem(
                        modifier = Modifier.weight(1f),
                        label = "Metode Pengisian",
                        value = batteryState.pluggedType,
                        icon = "🔌",
                        indicatorColor = if (batteryState.isCharging) GeometricGreen else GeometricTextDark
                    )
                    TelemetryItem(
                        modifier = Modifier.weight(1f),
                        label = "Kesehatan Baterai",
                        value = batteryState.health,
                        icon = "🛡️",
                        indicatorColor = if (batteryState.health == "Sehat" || batteryState.health == "Baik") GeometricGreen else GeometricAlert
                    )
                }
            }
        }

        // AMBANG BATAS THRESHOLD SLIDER SETTING
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(28.dp), clip = false),
            colors = CardDefaults.cardColors(containerColor = GeometricSurface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, GeometricBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AMBANG BATAS PEMUTUSAN",
                            color = GeometricTextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Kesehatan sel optimal di limit 80%-85%",
                            color = GeometricTextGray,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(GeometricPrimary, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$limit%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Slider(
                    value = limit.toFloat(),
                    onValueChange = { viewModel.setAutocutLimit(it.toInt()) },
                    valueRange = 50f..100f,
                    steps = 50,
                    colors = SliderDefaults.colors(
                        thumbColor = GeometricPrimary,
                        activeTrackColor = GeometricPrimary,
                        inactiveTrackColor = GeometricBorder
                    ),
                    modifier = Modifier.testTag("limit_slider")
                )
            }
        }

        // SIMULATOR CONTROL row
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(28.dp), clip = false),
            colors = CardDefaults.cardColors(containerColor = GeometricSurface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (isSimulationActive) GeometricPrimary else GeometricBorder
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "🧪 SIMULATION DRY RUN",
                            color = GeometricPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Uji coba respon alarm, TTS, dan Smart Plug dengan simulasi pengisian cepat otomatis.",
                            color = GeometricTextGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = isSimulationActive,
                        onCheckedChange = { viewModel.setSimulationActive(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GeometricPrimary
                        ),
                        modifier = Modifier.testTag("simulator_switch")
                    )
                }

                if (isSimulationActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GeometricPrimaryContainer, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Pengisian simulasi berjalan: ${batteryState.level}% / limit target $limit%",
                            fontSize = 12.sp,
                            color = GeometricOnPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigTab(
    limit: Int,
    isAlarmEnabled: Boolean,
    isTtsEnabled: Boolean,
    isWebhookEnabled: Boolean,
    webhookUrl: String,
    webhookMethod: String,
    webhookBody: String,
    webhookLogs: String,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ALARM SOUND SETTINGS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(28.dp), clip = false),
            colors = CardDefaults.cardColors(containerColor = GeometricSurface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, GeometricBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🔊 NOTIFIKASI ALARM SUARA",
                    color = GeometricPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "Bunyikan Alarm",
                            color = GeometricTextDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Mainkan nada notifikasi ketika baterai HP penuh sesuai limit",
                            color = GeometricTextGray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isAlarmEnabled,
                        onCheckedChange = { viewModel.toggleAlarmSetting(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = GeometricPrimary),
                        modifier = Modifier.testTag("alarm_switch")
                    )
                }

                Divider(color = GeometricBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "Robot Pengucap TTS",
                            color = GeometricTextDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Bicarakan instruksi cabut charger menggunakan bahasa Indonesia cerdas",
                            color = GeometricTextGray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isTtsEnabled,
                        onCheckedChange = { viewModel.toggleTtsSetting(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = GeometricPrimary),
                        modifier = Modifier.testTag("tts_switch")
                    )
                }
            }
        }

        // IoT SMART PLUG CONFIGURATION (PRECISE PHYSICAL AUTOCUT SOLUTION)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(28.dp), clip = false),
            colors = CardDefaults.cardColors(containerColor = GeometricSurface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, GeometricBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text(
                            text = "🔌 STOPKONTAK PINTAR PHYSICAL STOP",
                            color = GeometricPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kirim sinyal web API untuk mematikan stopkontak IoT (Smart Plug) secara fisik",
                            color = GeometricTextGray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isWebhookEnabled,
                        onCheckedChange = { viewModel.toggleWebhookSetting(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = GeometricPrimary),
                        modifier = Modifier.testTag("webhook_switch")
                    )
                }

                AnimatedVisibility(visible = isWebhookEnabled) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = webhookUrl,
                            onValueChange = { viewModel.updateWebhookUrl(it) },
                            label = { Text("Webhook URL target physical stopper") },
                            placeholder = { Text("e.g. http://192.168.1.100/cm?cmnd=Power%20off") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("webhook_url_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GeometricPrimary,
                                unfocusedBorderColor = GeometricBorder,
                                focusedLabelColor = GeometricPrimary
                            ),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Metode HTTP:",
                                fontSize = 12.sp,
                                color = GeometricTextDark,
                                modifier = Modifier.weight(0.4f)
                            )
                            listOf("GET", "POST").forEach { method ->
                                Row(
                                    modifier = Modifier
                                        .weight(0.3f)
                                        .clickable { viewModel.updateWebhookMethod(method) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = webhookMethod == method,
                                        onClick = { viewModel.updateWebhookMethod(method) },
                                        colors = RadioButtonDefaults.colors(selectedColor = GeometricPrimary)
                                    )
                                    Text(text = method, fontSize = 12.sp, color = GeometricTextDark)
                                }
                            }
                        }

                        if (webhookMethod == "POST") {
                            OutlinedTextField(
                                value = webhookBody,
                                onValueChange = { viewModel.updateWebhookBody(it) },
                                label = { Text("Body JSON data (POST)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("webhook_body_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GeometricPrimary,
                                    unfocusedBorderColor = GeometricBorder
                                ),
                                maxLines = 3
                            )
                        }

                        Button(
                            onClick = { viewModel.triggerTestWebhook() },
                            colors = ButtonDefaults.buttonColors(containerColor = GeometricPrimaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("webhook_test_btn")
                        ) {
                            Text(
                                text = "Uji Sinyal Web 🔌",
                                color = GeometricOnPrimaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (webhookLogs.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GeometricBg, RoundedCornerShape(12.dp))
                            .border(1.dp, GeometricBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "STATUS WEB INTERACTION LOGGER:",
                            color = GeometricPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = webhookLogs,
                            color = GeometricTextDark,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTab(
    histories: List<ChargingSession>,
    viewModel: MainViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SAVED METRICS ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .shadow(1.dp, RoundedCornerShape(20.dp), clip = false),
                colors = CardDefaults.cardColors(containerColor = GeometricSurface),
                border = BorderStroke(1.dp, GeometricBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Total Sesi Cerdas", color = GeometricTextGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${histories.size} Pengisian",
                        color = GeometricPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val totalSavedWh = histories.sumOf { it.energySavedWh }
            Card(
                modifier = Modifier
                    .weight(1.2f)
                    .shadow(1.dp, RoundedCornerShape(20.dp), clip = false),
                colors = CardDefaults.cardColors(containerColor = GeometricSurface),
                border = BorderStroke(1.dp, GeometricBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Mencegah Bocor (Overcharge)", color = GeometricTextGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f Wh Saved", totalSavedWh),
                        color = GeometricGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📜 RIWAYAT DATA PEMELIHARAAN BATERAI",
                color = GeometricTextDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            if (histories.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = GeometricAlert),
                    modifier = Modifier.testTag("clear_history_btn")
                ) {
                    Text("Hapus Riwayat", fontSize = 12.sp)
                }
            }
        }

        if (histories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(GeometricSurface, RoundedCornerShape(28.dp))
                    .border(1.dp, GeometricBorder, RoundedCornerShape(28.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "🧪", fontSize = 44.sp)
                    Text(
                        text = "Belum Ada Riwayat Sesi Pengaman",
                        color = GeometricTextDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Colok kabel cas anda atau tekan swicth di bagian bawah menu Utama simulator untuk melihat kalkulasi energi yang terselamatkan secara real-time.",
                        color = GeometricTextGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(histories) { session ->
                    HistoryItemRow(session)
                }
            }
        }
    }
}

@Composable
fun HistoryItemRow(session: ChargingSession) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(session.startTime))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(0.5.dp, RoundedCornerShape(16.dp), clip = false),
        colors = CardDefaults.cardColors(containerColor = GeometricSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GeometricBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Sesi ${dateString}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GeometricTextDark
                    )
                    if (session.isAutocutTriggered) {
                        Box(
                            modifier = Modifier
                                .background(GeometricGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("AUTOCUT", color = GeometricGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Mulai: ${session.startBatteryLevel}% ➡️ Selesai: ${session.endBatteryLevel}%",
                        color = GeometricTextGray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Puncak: ${session.peakTemperature}°C",
                        color = if (session.peakTemperature > 38.0) GeometricAlert else GeometricTextGray,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${session.endBatteryLevel - session.startBatteryLevel}%",
                    color = GeometricPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (session.energySavedWh > 0) {
                    Text(
                        text = "Sirkuit Hemat ${String.format(Locale.US, "%.2f Wh", session.energySavedWh)}",
                        color = GeometricGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// GORGEOUS RADIAL TELEMETRY GAUGE - EMBEDDING GEOMETRIC OUTLINE BACKGROUND
@Composable
fun CircularBatteryTelemetry(
    level: Int,
    isCharging: Boolean,
    targetLimit: Int,
    autocutTriggered: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = level / 100f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "progress"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val arcColor = if (autocutTriggered) GeometricAlert else GeometricPrimary

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val diameter = size.minDimension - strokeWidth - 10.dp.toPx()
            val radii = diameter / 2f
            val topLeftOffset = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )

            // GEOMETRIC ENVIRONMENT BACKGROUND DECORATIONS
            // Abstract thin lines perfectly matching the Geometric Balance specifications
            drawCircle(
                color = GeometricPrimary.copy(alpha = 0.05f),
                radius = radii + 15.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = GeometricPrimary.copy(alpha = 0.02f),
                radius = radii + 30.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )

            // 1. Draw Inactive Empty Tracker Arc line
            drawArc(
                color = GeometricBorder,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = topLeftOffset,
                size = Size(diameter, diameter)
            )

            // 2. Draw Active Solid Primary Level Ring
            drawArc(
                color = arcColor,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth + 1.dp.toPx(), cap = StrokeCap.Round),
                topLeft = topLeftOffset,
                size = Size(diameter, diameter)
            )

            // 3. Draw limit indicator notch tick
            val thresholdRatio = targetLimit / 100f
            val angleDeg = 135f + (270f * thresholdRatio)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            
            val tickRadius = radii + strokeWidth / 2f
            val startX = center.x + (tickRadius - 12.dp.toPx()) * cos(angleRad).toFloat()
            val startY = center.y + (tickRadius - 12.dp.toPx()) * sin(angleRad).toFloat()
            val endX = center.x + (tickRadius + 6.dp.toPx()) * cos(angleRad).toFloat()
            val endY = center.y + (tickRadius + 6.dp.toPx()) * sin(angleRad).toFloat()

            drawLine(
                color = GeometricPrimary,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.5f.dp.toPx()
            )
        }

        // Inner absolute numeric battery context
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val textState = if (autocutTriggered) "CUT-OFF" else if (isCharging) "CHARGING" else "BATTERY"
            Text(
                text = textState,
                color = if (autocutTriggered) GeometricAlert else GeometricPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$level",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light, // Light elegant font for elegant geometric feeling
                    color = GeometricTextDark,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = GeometricTextGray
                )
            }

            Text(
                text = if (isCharging) "Limit: $targetLimit%" else "Siaga",
                fontSize = 11.sp,
                color = GeometricTextGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatusAlertBanner(
    state: BatteryState,
    limit: Int,
    autocutTriggered: Boolean
) {
    val containerBg = if (autocutTriggered) {
        GeometricAlert.copy(alpha = 0.1f)
    } else {
        GeometricPrimaryContainer // #EADDFF elegant light purple container background
    }

    val borderOutlineColor = if (autocutTriggered) {
        GeometricAlert
    } else {
        Color.Transparent
    }

    val onContainerTxt = if (autocutTriggered) {
        GeometricAlert
    } else {
        GeometricOnPrimaryContainer // #21005D deep royal purple text
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(containerBg)
            .border(1.dp, borderOutlineColor, RoundedCornerShape(28.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (autocutTriggered) "🚫" else "🛡️",
                fontSize = 20.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            val triggerTitle = if (autocutTriggered) "Perlindungan Autocut Aktif!" else "Sirkuit Pengaman Aktif"
            val triggerDesc = if (autocutTriggered) {
                "Sinyal pemutus otomatis telah diledakkan pada batas aman $limit%! Cabut charger fisik Anda atau andalkan Smart Plug partner."
            } else if (state.isCharging) {
                "Mengisi daya cerdas menuju limit optimal $limit%. Voltase & suhu baterai dimonitor terus-menerus."
            } else {
                "EcoCharge siap siaga. Hubungkan charger ke perangkat untuk pemantauan perlindungan siklus baterai sehat."
            }

            Text(
                text = triggerTitle,
                fontWeight = FontWeight.Bold,
                color = onContainerTxt,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = triggerDesc,
                color = onContainerTxt.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun TelemetryItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: String,
    indicatorColor: Color
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(GeometricBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 16.sp)
        }

        Column {
            Text(
                text = label,
                color = GeometricTextGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                color = indicatorColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
