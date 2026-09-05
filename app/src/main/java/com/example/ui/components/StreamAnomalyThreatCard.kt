package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.model.InterferenceCategory
import com.example.model.StreamAnomalyReport
import com.example.ui.theme.*

@Composable
fun StreamAnomalyThreatCard(
  report: StreamAnomalyReport,
  connectionStatus: ConnectionStatus,
  activeServerUrl: String,
  latencyMs: Long,
  autoScanEnabled: Boolean,
  onTriggerGeminiScan: () -> Unit,
  onToggleAutoScan: (Boolean) -> Unit,
  onMitigateThreat: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  val threatColor = when (report.threatCategory) {
    InterferenceCategory.HIGH -> LaserCrimson
    InterferenceCategory.MEDIUM -> AmberAlert
    InterferenceCategory.LOW -> NeonEmerald
  }

  val threatSubduedColor = when (report.threatCategory) {
    InterferenceCategory.HIGH -> LaserCrimsonSubdued
    InterferenceCategory.MEDIUM -> AmberAlert.copy(alpha = 0.2f)
    InterferenceCategory.LOW -> EmeraldSubdued
  }

  // Pulsing animation for High threat
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = CardSurface),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(
        if (report.threatCategory == InterferenceCategory.HIGH) {
          listOf(LaserCrimson.copy(alpha = pulseAlpha), LaserCrimson.copy(alpha = 0.4f))
        } else if (report.threatCategory == InterferenceCategory.MEDIUM) {
          listOf(AmberAlert, AmberAlert.copy(alpha = 0.4f))
        } else {
          listOf(NeonEmerald, NeonCyan.copy(alpha = 0.3f))
        }
      ),
      width = if (report.threatCategory == InterferenceCategory.HIGH) 1.5.dp else 1.dp
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("stream_anomaly_threat_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // 1. Header Row: Title, Stream Monitor Status & AI Model Tag
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(
                if (connectionStatus == ConnectionStatus.CONNECTED) NeonEmerald
                else if (connectionStatus == ConnectionStatus.SIMULATION) NeonCyan
                else LaserCrimson
              )
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "WEBSOCKET STREAM ANOMALY MONITOR",
            style = MaterialTheme.typography.titleMedium.copy(
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp,
              letterSpacing = 0.5.sp
            )
          )
        }

        // Gemini AI Badge
        Surface(
          color = OpticPurple.copy(alpha = 0.18f),
          shape = RoundedCornerShape(4.dp),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(OpticPurple, NeonCyan)),
            width = 0.5.dp
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = "Gemini AI",
              tint = NeonCyan,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "GEMINI 3.5 FLASH",
              style = MaterialTheme.typography.labelSmall.copy(
                color = NeonCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2. Main Threat Classification Banner
      Surface(
        color = threatSubduedColor,
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(listOf(threatColor.copy(0.6f), threatColor.copy(0.2f))),
          width = 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "POTENTIAL INTERFERENCE THREAT",
              style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
              )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = report.interferenceType,
              style = MaterialTheme.typography.titleMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Target Band: ${report.affectedBand} • Confidence: ${(report.confidenceScore * 100).toInt()}%",
              style = MaterialTheme.typography.bodySmall.copy(
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
              )
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          // Threat Category Pill (High / Medium / Low)
          Surface(
            color = threatColor,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.testTag("threat_category_badge")
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Text(
                text = report.threatCategoryText.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                  color = ObsidianBlack,
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 15.sp,
                  letterSpacing = 1.sp
                )
              )
              Text(
                text = "THREAT",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = ObsidianBlack.copy(alpha = 0.8f),
                  fontWeight = FontWeight.Bold,
                  fontSize = 8.sp,
                  fontFamily = FontFamily.Monospace
                )
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 3. AI Executive Assessment Summary
      Text(
        text = report.summary,
        style = MaterialTheme.typography.bodyMedium.copy(
          color = TextPrimary,
          fontSize = 11.5.sp,
          lineHeight = 16.sp
        )
      )

      Spacer(modifier = Modifier.height(8.dp))

      // 4. Tactical Mitigation Callout
      Surface(
        color = CardSurfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(0.5.dp, CardBorder, RoundedCornerShape(6.dp))
          .padding(8.dp)
      ) {
        Row(verticalAlignment = Alignment.Top) {
          Icon(
            imageVector = if (report.threatCategory == InterferenceCategory.HIGH) Icons.Default.Warning else Icons.Default.Lightbulb,
            contentDescription = null,
            tint = threatColor,
            modifier = Modifier
              .size(15.dp)
              .padding(top = 1.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = "RECOMMENDED MITIGATION",
              style = MaterialTheme.typography.labelSmall.copy(
                color = threatColor,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
              )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = report.mitigationRecommendation,
              style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 10.5.sp,
                lineHeight = 14.sp
              )
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 5. Live Stream Telemetry Quick Chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        StreamInfoChip(
          label = "SOCKET",
          value = if (activeServerUrl.isNotEmpty()) activeServerUrl.substringAfter("://").take(16) else "10.0.2.2:8765",
          modifier = Modifier.weight(1f)
        )
        StreamInfoChip(
          label = "LATENCY",
          value = "${latencyMs} ms",
          modifier = Modifier.weight(0.7f)
        )
        StreamInfoChip(
          label = "AI MODEL",
          value = "Gemini 3.5",
          modifier = Modifier.weight(0.9f)
        )
      }

      // 6. Expandable Detailed Telemetry Snapshot
      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
        ) {
          Text(
            text = "DEEP FORENSIC ANALYSIS",
            style = MaterialTheme.typography.labelSmall.copy(
              color = NeonCyan,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            )
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = report.detailedAnalysis,
            style = MaterialTheme.typography.bodySmall.copy(
              color = TextSecondary,
              fontSize = 10.5.sp
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Metric key-value table
          if (report.anomalyMetrics.isNotEmpty()) {
            Surface(
              color = ObsidianBlack,
              shape = RoundedCornerShape(4.dp),
              modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, CardBorder, RoundedCornerShape(4.dp))
                .padding(8.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for ((k, v) in report.anomalyMetrics) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = k.replace("_", " ").uppercase(),
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                      )
                    )
                    Text(
                      text = v,
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                      ),
                      maxLines = 1
                    )
                  }
                }
              }

            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 7. Interactive Action Bar: Scan with Gemini, Auto-Scan Switch, Expand & Mitigate
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Expand/Collapse Details
        TextButton(
          onClick = { isExpanded = !isExpanded },
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isExpanded) "COLLAPSE" else "DETAILS",
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextSecondary,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Auto-Scan Toggle
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onToggleAutoScan(!autoScanEnabled) }
          ) {
            Text(
              text = "AUTO",
              style = MaterialTheme.typography.labelSmall.copy(
                color = if (autoScanEnabled) NeonCyan else TextMuted,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Switch(
              checked = autoScanEnabled,
              onCheckedChange = onToggleAutoScan,
              modifier = Modifier
                .scaleSmall()
                .testTag("auto_scan_switch"),
              colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CardSurfaceVariant
              )
            )
          }

          // Scan Now Button
          Button(
            onClick = onTriggerGeminiScan,
            enabled = !report.isAnalyzing,
            colors = ButtonDefaults.buttonColors(
              containerColor = NeonCyan,
              contentColor = ObsidianBlack,
              disabledContainerColor = NeonCyan.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier
              .height(32.dp)
              .testTag("scan_gemini_threat_btn")
          ) {
            if (report.isAnalyzing) {
              CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = ObsidianBlack,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text("ANALYZING...", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
            } else {
              Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("SCAN STREAM", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
            }
          }

          // Mitigate Button (if High Threat)
          if (report.threatCategory == InterferenceCategory.HIGH) {
            Button(
              onClick = onMitigateThreat,
              colors = ButtonDefaults.buttonColors(
                containerColor = LaserCrimson,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              modifier = Modifier
                .height(32.dp)
                .testTag("mitigate_threat_btn")
            ) {
              Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("MITIGATE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StreamInfoChip(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = CardSurfaceVariant,
    shape = RoundedCornerShape(4.dp),
    modifier = modifier.border(0.5.dp, CardBorder, RoundedCornerShape(4.dp))
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          color = TextMuted,
          fontSize = 8.sp,
          fontFamily = FontFamily.Monospace
        )
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelSmall.copy(
          color = TextPrimary,
          fontSize = 9.5.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        ),
        maxLines = 1
      )
    }
  }
}

private fun Modifier.scaleSmall(): Modifier = this.size(width = 38.dp, height = 24.dp)
