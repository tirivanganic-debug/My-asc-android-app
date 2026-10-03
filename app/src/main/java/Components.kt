package com.ascendant.sentiment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascendant.sentiment.engine.AstroEngine
import com.ascendant.sentiment.model.*
import com.ascendant.sentiment.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun SentimentGaugeCard(
    currentBar: SentimentBar?,
    locationName: String,
    timeZoneId: String,
    orbLimit: Double
) {
    if (currentBar == null) return

    val score = currentBar.sentimentScore
    val isBull = score > 0.15
    val isBear = score < -0.15
    val statusText = if (isBull) "BULLISH" else if (isBear) "BEARISH" else "NEUTRAL"
    val statusColor = if (isBull) Emerald400 else if (isBear) Rose400 else Slate400
    val statusBg = if (isBull) Emerald500.copy(alpha = 0.15f) else if (isBear) Rose500.copy(alpha = 0.15f) else Slate800

    val sdf = SimpleDateFormat("EEE, dd MMM HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone(timeZoneId)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SENTIMENT INDEX S",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(statusColor.copy(alpha = 0.5f)))
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (score > 0) "+%.3f".format(score) else "%.3f".format(score),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = statusColor
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = sdf.format(Date(currentBar.timeMs)),
                        fontSize = 12.sp,
                        color = Slate100,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "ASC " + AstroEngine.formatDMS(currentBar.ascDegree),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Emerald400,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = locationName,
                        fontSize = 10.sp,
                        color = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visual Meter Bar
            val clampedScore = score.coerceIn(-1.0, 1.0)
            val fraction = ((clampedScore + 1.0) / 2.0).toFloat()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Slate950)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(if (isBull) Emerald500 else if (isBear) Rose500 else Slate400)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("−1.0 Bearish", fontSize = 9.sp, color = Slate400)
                Text("0.0 Neutral", fontSize = 9.sp, color = Slate400)
                Text("+1.0 Bullish", fontSize = 9.sp, color = Slate400)
            }
        }
    }
}

/**
 * v2.0: Active Ascendant Aspects, drawn as the BACK layer of the sentiment chart.
 * The chart sits on top of this with a semi-transparent background.
 */
@Composable
fun ActiveAspectsLayer(aspects: List<AspectInfo>, modifier: Modifier = Modifier) {
    val sorted = aspects.sortedByDescending { abs(it.contribution) }

    Column(
        modifier = modifier
            .background(BgDark)
            .padding(start = 46.dp, end = 16.dp, top = 14.dp, bottom = 28.dp)
    ) {
        Text(
            text = "ACTIVE ASCENDANT ASPECTS (${aspects.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Slate100
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (sorted.isEmpty()) {
            Text(
                text = "No aspects within orb limit at this moment",
                fontSize = 11.sp,
                color = Slate400,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            sorted.forEach { aspect ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = aspect.glyph,
                            fontSize = 15.sp,
                            color = Amber500,
                            modifier = Modifier.width(22.dp)
                        )
                        Text(
                            text = "${aspect.planetName} ${aspect.aspectAngle}°",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate100
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(orb %.2f°)".format(aspect.orbDiff),
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }

                    val contrib = aspect.contribution
                    Text(
                        text = if (contrib > 0) "+%.2f".format(contrib) else "%.2f".format(contrib),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (contrib > 0) Emerald400 else Rose400
                    )
                }
            }
        }
    }
}

@Composable
fun ExactHitsCard(
    hits: List<HitEvent>,
    timeZoneId: String,
    onJumpToHit: (Long) -> Unit
) {
    val sdf = SimpleDateFormat("HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone(timeZoneId)
    }
    val now = System.currentTimeMillis()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "EXACT ASCENDANT HITS IN WINDOW (${hits.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Slate100
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (hits.isEmpty()) {
                Text(
                    text = "None in window",
                    fontSize = 11.sp,
                    color = Slate400,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                hits.forEach { hit ->
                    val isPast = hit.timeMs < now
                    val diffMin = ((hit.timeMs - now) / 60000).toInt()
                    val relStr = if (abs(diffMin) <= 3) "NOW" else if (diffMin > 0) "in ${diffMin}m" else "${abs(diffMin)}m ago"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onJumpToHit(hit.timeMs) }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sdf.format(Date(hit.timeMs)),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPast) Slate400 else Slate100
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${hit.glyph} ${hit.planetName} ${hit.aspectAngle}°",
                                fontSize = 12.sp,
                                color = if (isPast) Slate400 else Slate100
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (hit.polarity > 0) "▲ +1" else "▼ −1",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hit.polarity > 0) Emerald400 else Rose400
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = relStr,
                                fontSize = 10.sp,
                                color = if (relStr == "NOW") Amber500 else Slate400
                            )
                        }
                    }
                    Divider(color = BorderDark.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}
