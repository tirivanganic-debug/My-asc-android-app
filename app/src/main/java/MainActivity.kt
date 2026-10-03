package com.ascendant.sentiment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascendant.sentiment.engine.AstroEngine
import com.ascendant.sentiment.model.GeoLocation
import com.ascendant.sentiment.ui.*
import com.ascendant.sentiment.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20)
            }
        } catch (_: Exception) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AscendantSentimentTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BgDark
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onHaptic = { triggerHaptic() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onHaptic: () -> Unit
) {
    var isLive by remember { mutableStateOf(true) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var centerTimeMs by remember { mutableLongStateOf((System.currentTimeMillis() / 300_000L) * 300_000L) }
    var inspectedTimeMs by remember { mutableLongStateOf((System.currentTimeMillis() / 300_000L) * 300_000L) }
    var windowHours by remember { mutableIntStateOf(6) }
    var selectedLocationIdx by remember { mutableIntStateOf(0) }
    var selectedTimezone by remember { mutableStateOf("Europe/London") }
    var useSd by remember { mutableStateOf(true) }   // v3.0: sign dignity switch
    var usePm by remember { mutableStateOf(true) }   // v3.0: planet-to-planet modifier switch

    val locations = AstroEngine.PRESET_LOCATIONS
    val activeLocation = locations[selectedLocationIdx]

    // 1-second live ticker
    LaunchedEffect(isLive) {
        while (true) {
            val cur = System.currentTimeMillis()
            nowMs = cur
            if (isLive) {
                val snapped = (cur / 300_000L) * 300_000L
                centerTimeMs = snapped
                inspectedTimeMs = snapped
            }
            delay(1000)
        }
    }

    // Planetary series calculation (100% offline pure Kotlin)
    val series = remember(centerTimeMs, windowHours, activeLocation, useSd, usePm) {
        AstroEngine.generateSeries(
            centerMs = centerTimeMs,
            windowHours = windowHours,
            lat = activeLocation.lat,
            lon = activeLocation.lon,
            orbLimit = 3.0,
            useSd = useSd,
            usePm = usePm
        )
    }

    // Exact hits in window
    val exactHits = remember(series) {
        AstroEngine.calculateExactHits(series, 3.0)
    }

    // Inspected Bar
    val currentBar = remember(series, inspectedTimeMs) {
        if (series.isEmpty()) null
        else {
            val firstT = series.first().timeMs
            val idx = ((inspectedTimeMs - firstT) / 300_000.0).toInt().coerceIn(0, series.size - 1)
            series[idx]
        }
    }

    val liveSdf = remember(selectedTimezone) {
        SimpleDateFormat("EEE, dd MMM HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(selectedTimezone)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ascendant Sentiment",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Emerald500.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Brent CFD",
                            color = Emerald400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = liveSdf.format(Date(nowMs)) + if (isLive) " · LIVE" else " · PAUSED",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isLive) Emerald400 else Amber500
                )
            }

            Surface(
                color = CardDark,
                shape = RoundedCornerShape(8.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
            ) {
                Text(
                    text = "100% Offline Native",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Live & Location Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onHaptic()
                            isLive = !isLive
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLive) Emerald500 else CardDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isLive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isLive) Slate950 else Slate100,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLive) "Live Active" else "Resume Live",
                            fontSize = 12.sp,
                            color = if (isLive) Slate950 else Slate100,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Location & Window Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        locations.forEachIndexed { idx, loc ->
                            val isSelected = idx == selectedLocationIdx
                            Surface(
                                color = if (isSelected) Emerald500.copy(alpha = 0.2f) else Slate950,
                                shape = RoundedCornerShape(8.dp),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) Emerald400 else BorderDark)
                                ),
                                modifier = Modifier.clickable {
                                    onHaptic()
                                    selectedLocationIdx = idx
                                }
                            ) {
                                Text(
                                    text = loc.name.split(" ").first(),
                                    fontSize = 11.sp,
                                    color = if (isSelected) Emerald400 else Slate400,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // v3.0: model switches, turn both off to see the original formula
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = useSd,
                            onCheckedChange = {
                                onHaptic()
                                useSd = it
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = Emerald500)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sd  sign dignity", fontSize = 11.sp, color = Slate100)
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = usePm,
                            onCheckedChange = {
                                onHaptic()
                                usePm = it
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = Emerald500)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pm  planet aspects", fontSize = 11.sp, color = Slate100)
                    }
                }
            }
        }

        // Sentiment Gauge
        SentimentGaugeCard(
            currentBar = currentBar,
            locationName = activeLocation.name,
            timeZoneId = selectedTimezone,
            orbLimit = 3.0
        )

        // 5-minute Sentiment Chart (v2.0: Active Aspects behind it, step toggles below it)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SENTIMENT INDEX PER 5-MIN BAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    // Back layer: active aspects
                    ActiveAspectsLayer(
                        aspects = currentBar?.aspects ?: emptyList(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp)
                    )
                    // Front layer: semi-transparent bar chart
                    SentimentChart(
                        series = series,
                        inspectedTimeMs = currentBar?.timeMs ?: inspectedTimeMs,
                        onInspectTime = { t ->
                            onHaptic()
                            isLive = false
                            inspectedTimeMs = t
                        },
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Time stepper: moves the white vertical line back / forward
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-60 to "−1h", -5 to "−5m", 5 to "+5m", 60 to "+1h").forEach { (mins, label) ->
                        OutlinedButton(
                            onClick = {
                                onHaptic()
                                isLive = false
                                val newT = inspectedTimeMs + mins * 60_000L
                                centerTimeMs = newT
                                inspectedTimeMs = newT
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate100)
                        }
                    }
                }
            }
        }

        // Exact Hits Card
        ExactHitsCard(
            hits = exactHits,
            timeZoneId = selectedTimezone,
            onJumpToHit = { hitMs ->
                onHaptic()
                isLive = false
                inspectedTimeMs = hitMs
                centerTimeMs = hitMs
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
