package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun BridgeConfigScreen(
  connectionStatus: ConnectionStatus,
  latencyMs: Long,
  bridgeHost: String,
  bridgePort: String,
  activeServerUrl: String,
  serverDescription: String,
  lastLogMessage: String,
  serverPresets: List<ServerPreset>,
  selectedPreset: ServerPreset?,
  shieldConfig: ShieldConfig,
  isRunningOnEmulator: Boolean,
  deviceLocalIp: String?,
  subnetPrefix: String,
  probeStatusMessage: String,
  isProbing: Boolean,
  onSetBridgeHost: (String) -> Unit,
  onSetBridgePort: (String) -> Unit,
  onApplyPreset: (ServerPreset) -> Unit,
  onConnectPreset: (ServerPreset) -> Unit,
  onConnectBridge: () -> Unit,
  onDisconnectBridge: () -> Unit,
  onEnableSimulation: () -> Unit,
  onProbeEndpoint: (String, Int) -> Unit,
  onConnectViaAdbReverse: (Int) -> Unit,
  onConnectViaLanIp: (String, Int) -> Unit,
  onRefreshNetworkInfo: () -> Unit,
  onUpdateRfThresholds: (Float, Float) -> Unit,
  onUpdateFsoThresholds: (Float, Float) -> Unit,
  onToggleAutoShield: (Boolean) -> Unit,
  onTriggerLaserAttack: () -> Unit,
  onTriggerRfJammer: () -> Unit,
  onTriggerLidarSpoof: () -> Unit,
  isBackgroundServiceRunning: Boolean = false,
  bgRfPacketsFetched: Long = 0L,
  bgFsoPacketsFetched: Long = 0L,
  bgLastFetchTimestamp: Long = 0L,
  bgFetchIntervalMs: Long = 1000L,
  bgServiceLog: String = "",
  onStartBackgroundService: (Long) -> Unit = {},
  onStopBackgroundService: () -> Unit = {},
  onUpdateBackgroundInterval: (Long) -> Unit = {},
  onFetchPacketsNow: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var rfAlert by remember(shieldConfig.rfAlertDbm) { mutableStateOf(shieldConfig.rfAlertDbm) }
  var rfHigh by remember(shieldConfig.rfHighDbm) { mutableStateOf(shieldConfig.rfHighDbm) }
  var fsoAlert by remember(shieldConfig.fsoAlertDbm) { mutableStateOf(shieldConfig.fsoAlertDbm) }
  var fsoBlock by remember(shieldConfig.fsoBlockDbm) { mutableStateOf(shieldConfig.fsoBlockDbm) }

  var showTroubleshootingGuide by remember { mutableStateOf(!isRunningOnEmulator) }
  var customLanSuffix by remember { mutableStateOf("100") }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
  ) {

    // 1. Device Environment & Network Status Banner
    item {
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(
            if (!isRunningOnEmulator) listOf(NeonCyan, AmberAlert)
            else listOf(NeonCyan, OpticPurple)
          ),
          width = 1.25.dp
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("device_network_env_card")
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (!isRunningOnEmulator) Icons.Default.PhoneAndroid else Icons.Default.LaptopMac,
                contentDescription = null,
                tint = if (!isRunningOnEmulator) AmberAlert else NeonCyan,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = if (!isRunningOnEmulator) "PHYSICAL ANDROID DEVICE DETECTED" else "ANDROID STUDIO EMULATOR",
                  style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                  )
                )
                Text(
                  text = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                  )
                )
              }
            }

            IconButton(
              onClick = onRefreshNetworkInfo,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Network Info", tint = NeonCyan, modifier = Modifier.size(16.dp))
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Device IP Info Banner
          Surface(
            color = ObsidianBlack,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "PHONE WI-FI IP:",
                  style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                )
                Text(
                  text = deviceLocalIp ?: "Disconnected from Wi-Fi",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = if (deviceLocalIp != null) NeonEmerald else AmberAlert,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp
                  )
                )
              }

              if (!isRunningOnEmulator) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "LOCAL SUBNET:",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                  )
                  Text(
                    text = "${subnetPrefix}X",
                    style = MaterialTheme.typography.labelSmall.copy(
                      color = NeonCyan,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace,
                      fontSize = 10.5.sp
                    )
                  )
                }
              }
            }
          }

          if (!isRunningOnEmulator) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "⚠️ Notice: '10.0.2.2' is an emulator-only address and will not work on a physical phone. Choose one of the 2 physical connection options below:",
              style = MaterialTheme.typography.bodySmall.copy(color = AmberAlert, fontSize = 10.5.sp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Two Quick Connection Buttons for Physical Devices
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Option 1: USB / ADB Reverse
              Button(
                onClick = { onConnectViaAdbReverse(8765) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("quick_usb_adb_connect_btn")
              ) {
                Icon(imageVector = Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("USB (ADB REVERSE)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace))
              }

              // Option 2: Wi-Fi LAN
              Button(
                onClick = { onConnectViaLanIp("${subnetPrefix}$customLanSuffix", 8765) },
                colors = ButtonDefaults.buttonColors(containerColor = OpticPurple, contentColor = TextPrimary),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("quick_wifi_lan_connect_btn")
              ) {
                Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WI-FI LAN HOST", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace))
              }
            }
          }
        }
      }
    }

    // 1B. Background WebSocket Service (Periodic RF & FSO Packet Pipelines)
    item {
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(
            if (isBackgroundServiceRunning) listOf(NeonEmerald, NeonCyan)
            else listOf(CardBorder, CardBorder)
          ),
          width = if (isBackgroundServiceRunning) 1.5.dp else 0.75.dp
        ),
        modifier = Modifier.fillMaxWidth().testTag("bg_websocket_service_card")
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .background(
                    if (isBackgroundServiceRunning) NeonEmerald.copy(alpha = 0.2f) else CardBorder.copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.CloudSync,
                  contentDescription = "Background WebSocket Service",
                  tint = if (isBackgroundServiceRunning) NeonEmerald else TextMuted,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "BACKGROUND WEBSOCKET SERVICE",
                  style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp
                  )
                )
                Text(
                  text = "Periodic RF & FSO Packet Pipeline Sync",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isBackgroundServiceRunning) NeonEmerald else TextMuted,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace
                  )
                )
              }
            }

            Surface(
              color = if (isBackgroundServiceRunning) NeonEmerald.copy(alpha = 0.2f) else CardBorder.copy(alpha = 0.3f),
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = if (isBackgroundServiceRunning) "ACTIVE (BG SERVICE)" else "STANDBY",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = if (isBackgroundServiceRunning) NeonEmerald else TextMuted,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 9.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Telemetry Packet Counters Grid
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              color = ObsidianBlack,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(
                  text = "RF PACKETS",
                  style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                )
                Text(
                  text = "$bgRfPacketsFetched",
                  style = MaterialTheme.typography.titleMedium.copy(
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                  )
                )
              }
            }

            Surface(
              color = ObsidianBlack,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(
                  text = "FSO PACKETS",
                  style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                )
                Text(
                  text = "$bgFsoPacketsFetched",
                  style = MaterialTheme.typography.titleMedium.copy(
                    color = OpticPurple,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                  )
                )
              }
            }

            Surface(
              color = ObsidianBlack,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(
                  text = "FETCH INTERVAL",
                  style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                )
                Text(
                  text = "${bgFetchIntervalMs}ms",
                  style = MaterialTheme.typography.titleMedium.copy(
                    color = AmberAlert,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                  )
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Cadence Selector
          Text(
            text = "PERIODIC PACKET FETCH CADENCE:",
            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            listOf(250L to "250ms", 500L to "500ms", 1000L to "1.0s", 2000L to "2.0s", 5000L to "5.0s").forEach { (ms, label) ->
              val isSelected = bgFetchIntervalMs == ms
              Button(
                onClick = { onUpdateBackgroundInterval(ms) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isSelected) NeonCyan else CardBorder.copy(alpha = 0.5f),
                  contentColor = if (isSelected) ObsidianBlack else TextPrimary
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(30.dp)
              ) {
                Text(label, fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontFamily = FontFamily.Monospace)
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Action Controls (Start/Stop, Fetch Now)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (isBackgroundServiceRunning) {
              Button(
                onClick = onStopBackgroundService,
                colors = ButtonDefaults.buttonColors(containerColor = LaserCrimson.copy(alpha = 0.85f), contentColor = TextPrimary),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f).testTag("stop_bg_service_btn")
              ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("STOP SERVICE", fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
              }
            } else {
              Button(
                onClick = { onStartBackgroundService(bgFetchIntervalMs) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = ObsidianBlack),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f).testTag("start_bg_service_btn")
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("START SERVICE", fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
              }
            }

            OutlinedButton(
              onClick = onFetchPacketsNow,
              shape = RoundedCornerShape(6.dp),
              border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(NeonCyan, NeonCyan))),
              modifier = Modifier.weight(1f).testTag("fetch_packets_now_btn")
            ) {
              Icon(Icons.Default.Sync, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("FETCH NOW", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
          }

          if (bgServiceLog.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              color = ObsidianBlack,
              shape = RoundedCornerShape(4.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "LOG: $bgServiceLog",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = TextMuted,
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
              )
            }
          }
        }
      }
    }

    // 2. Physical Device Setup & Troubleshooting Guide Accordion
    item {
      Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder)), width = 0.5.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showTroubleshootingGuide = !showTroubleshootingGuide },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "PHYSICAL DEVICE CONNECTION GUIDE",
                style = MaterialTheme.typography.titleMedium.copy(
                  color = TextPrimary,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.5.sp
                )
              )
            }

            Icon(
              imageVector = if (showTroubleshootingGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = null,
              tint = TextMuted
            )
          }

          AnimatedVisibility(visible = showTroubleshootingGuide) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
              HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
              Spacer(modifier = Modifier.height(10.dp))

              // Method 1: USB Cable (ADB Reverse)
              Text(
                text = "METHOD 1: USB Cable via ADB Reverse (Recommended & Zero Firewall)",
                style = MaterialTheme.typography.labelMedium.copy(color = NeonEmerald, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "1. Connect phone to PC with USB cable & enable USB Debugging.\n2. Open terminal/PowerShell on PC and run:\n   adb reverse tcp:8765 tcp:8765\n3. In this app, connect to 'ws://127.0.0.1:8765'.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
              )

              // Copy ADB command button
              Spacer(modifier = Modifier.height(6.dp))
              OutlinedButton(
                onClick = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("ADB Command", "adb reverse tcp:8765 tcp:8765; adb reverse tcp:9001 tcp:9001")
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "ADB command copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
              ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = NeonCyan)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy 'adb reverse tcp:8765 tcp:8765'", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = NeonCyan))
              }

              Spacer(modifier = Modifier.height(10.dp))
              HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
              Spacer(modifier = Modifier.height(10.dp))

              // Method 2: Wi-Fi LAN
              Text(
                text = "METHOD 2: Wi-Fi Local Area Network",
                style = MaterialTheme.typography.labelMedium.copy(color = OpticPurple, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "1. Connect phone and PC to the same Wi-Fi router.\n2. Find PC's local IP (run 'ipconfig' on Windows or 'ip a' on Linux/Mac).\n3. Ensure your GNU Radio script binds to 0.0.0.0 (NOT 127.0.0.1).\n4. Allow port 8765 in Windows/Mac Firewall.\n5. Enter ws://<PC_IP>:8765 and click Connect.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
              )

              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                color = LaserCrimson.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Important: If GNU Radio Python script has server host set to '127.0.0.1', external Wi-Fi devices cannot connect. Change it to host='0.0.0.0' or use ADB reverse.",
                  style = MaterialTheme.typography.bodySmall.copy(color = LaserCrimson, fontSize = 9.5.sp),
                  modifier = Modifier.padding(6.dp)
                )
              }
            }
          }
        }
      }
    }

    // 3. GNU Radio & SDR Stream Presets
    item {
      Text(
        text = "STREAM PRESETS",
        style = MaterialTheme.typography.labelMedium.copy(
          color = TextSecondary,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.5.sp
        )
      )
    }

    items(serverPresets.size) { index ->
      val preset = serverPresets[index]
      val isSelected = selectedPreset?.id == preset.id
      val isCurrentConnected = connectionStatus == ConnectionStatus.CONNECTED && activeServerUrl == preset.url

      Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(
            if (isCurrentConnected) listOf(NeonEmerald, NeonEmerald.copy(0.4f))
            else if (isSelected) listOf(NeonCyan, NeonCyan.copy(0.4f))
            else listOf(CardBorder, CardBorder)
          ),
          width = if (isCurrentConnected || isSelected) 1.25.dp else 0.5.dp
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onApplyPreset(preset) }
          .testTag("server_preset_${preset.id}")
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = when (preset.tag) {
                  "USB / ADB" -> Icons.Default.Usb
                  "WI-FI LAN" -> Icons.Default.Wifi
                  "GNU RADIO", "GRC" -> Icons.Default.GraphicEq
                  "RTL-SDR" -> Icons.Default.SettingsInputAntenna
                  "PUBLIC" -> Icons.Default.Lock
                  else -> Icons.Default.Computer
                },
                contentDescription = null,
                tint = if (isCurrentConnected) NeonEmerald else if (preset.tag == "USB / ADB" || preset.tag == "GNU RADIO") NeonCyan else OpticPurple,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = preset.title,
                style = MaterialTheme.typography.titleMedium.copy(
                  color = TextPrimary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              )
            }

            Surface(
              color = if (isCurrentConnected) NeonEmerald.copy(0.18f) else CardSurfaceVariant,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = if (isCurrentConnected) "LIVE" else preset.tag,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = if (isCurrentConnected) NeonEmerald else TextMuted,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = preset.url,
            style = MaterialTheme.typography.labelSmall.copy(
              color = NeonCyan,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.5.sp
            )
          )

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = preset.description,
            style = MaterialTheme.typography.bodySmall.copy(
              color = TextMuted,
              fontSize = 10.sp
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            Button(
              onClick = { onConnectPreset(preset) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isCurrentConnected) EmeraldSubdued else CardSurfaceVariant,
                contentColor = if (isCurrentConnected) NeonEmerald else TextPrimary
              ),
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(30.dp)
            ) {
              Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isCurrentConnected) "ACTIVE" else "CONNECT",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
              )
            }
          }
        }
      }
    }

    // 4. Custom WebSocket Bridge Connection & TCP Diagnostic Probe Card
    item {
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(
            when (connectionStatus) {
              ConnectionStatus.CONNECTED -> listOf(NeonEmerald, EmeraldSubdued)
              ConnectionStatus.CONNECTING -> listOf(AmberAlert, AmberSubdued)
              else -> listOf(CardBorder, CardBorder)
            }
          ),
          width = 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.Router, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "CUSTOM WEBSOCKET CONFIG",
                style = MaterialTheme.typography.titleMedium.copy(
                  color = TextPrimary,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 12.5.sp
                )
              )
            }

            Surface(
              color = when (connectionStatus) {
                ConnectionStatus.CONNECTED -> EmeraldSubdued.copy(0.3f)
                ConnectionStatus.CONNECTING -> AmberSubdued.copy(0.3f)
                ConnectionStatus.SIMULATION -> NeonCyan.copy(0.15f)
                ConnectionStatus.DISCONNECTED -> CardSurfaceVariant
              },
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = connectionStatus.name,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = when (connectionStatus) {
                    ConnectionStatus.CONNECTED -> NeonEmerald
                    ConnectionStatus.CONNECTING -> AmberAlert
                    ConnectionStatus.SIMULATION -> NeonCyan
                    ConnectionStatus.DISCONNECTED -> TextMuted
                  },
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Live Socket Diagnostic Status Banner
          Surface(
            color = ObsidianBlack,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(0.5.dp, CardBorder, RoundedCornerShape(4.dp))
              .padding(8.dp)
          ) {
            Column {
              Text(
                text = "DIAGNOSTIC STATUS:",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TextMuted,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 8.5.sp
                )
              )
              Text(
                text = lastLogMessage,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = if (connectionStatus == ConnectionStatus.CONNECTED) NeonEmerald
                  else if (lastLogMessage.startsWith("Failure") || lastLogMessage.startsWith("Error")) LaserCrimson
                  else TextSecondary,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 9.5.sp
                )
              )
              if (connectionStatus == ConnectionStatus.CONNECTED) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "RTT LATENCY: ${latencyMs}ms | TARGET: $activeServerUrl",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp
                  )
                )
              }
            }
          }

          // Probe Feedback Box if active
          if (probeStatusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              color = if (probeStatusMessage.startsWith("SUCCESS")) EmeraldSubdued.copy(alpha = 0.25f)
              else if (probeStatusMessage.startsWith("TIMEOUT") || probeStatusMessage.startsWith("REFUSED")) AmberSubdued.copy(alpha = 0.25f)
              else CardSurfaceVariant,
              shape = RoundedCornerShape(4.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "TCP PROBE: $probeStatusMessage",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = if (probeStatusMessage.startsWith("SUCCESS")) NeonEmerald else AmberAlert,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 9.5.sp
                ),
                modifier = Modifier.padding(6.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Quick Subnet helper chips for LAN
          if (!isRunningOnEmulator) {
            Text(
              text = "QUICK SUBNET PREFILL:",
              style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              listOf("127.0.0.1", "${subnetPrefix}100", "${subnetPrefix}105", "${subnetPrefix}150").forEach { ipSample ->
                Surface(
                  color = CardSurfaceVariant,
                  shape = RoundedCornerShape(4.dp),
                  modifier = Modifier
                    .clickable { onSetBridgeHost("ws://$ipSample:8765") }
                ) {
                  Text(
                    text = ipSample,
                    style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(8.dp))
          }

          // Host Input
          OutlinedTextField(
            value = bridgeHost,
            onValueChange = onSetBridgeHost,
            label = { Text("Server Host / IP (e.g. ws://127.0.0.1:8765 or ws://${subnetPrefix}105:8765)", fontSize = 10.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = CardBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedContainerColor = ObsidianBlack,
              unfocusedContainerColor = ObsidianBlack
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("bridge_host_input")
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Port Input
          OutlinedTextField(
            value = bridgePort,
            onValueChange = onSetBridgePort,
            label = { Text("WebSocket Port (Default 8765 / 9001)", fontSize = 10.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = CardBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedContainerColor = ObsidianBlack,
              unfocusedContainerColor = ObsidianBlack
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("bridge_port_input")
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Probe Socket Button
          OutlinedButton(
            onClick = {
              val port = bridgePort.toIntOrNull() ?: 8765
              onProbeEndpoint(bridgeHost, port)
            },
            enabled = !isProbing,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(34.dp)
              .testTag("probe_tcp_socket_btn")
          ) {
            if (isProbing) {
              CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("TESTING TCP PORT...", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.5.sp))
            } else {
              Icon(imageVector = Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("TEST TCP PORT REACHABILITY (PROBE)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 9.5.sp))
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Action Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onConnectBridge,
              colors = ButtonDefaults.buttonColors(
                containerColor = if (connectionStatus == ConnectionStatus.CONNECTED) NeonEmerald else NeonCyan,
                contentColor = ObsidianBlack
              ),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("connect_bridge_btn")
            ) {
              Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (connectionStatus == ConnectionStatus.CONNECTED) "RECONNECT" else "CONNECT",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
              )
            }

            if (connectionStatus == ConnectionStatus.CONNECTED || connectionStatus == ConnectionStatus.CONNECTING) {
              Button(
                onClick = onDisconnectBridge,
                colors = ButtonDefaults.buttonColors(
                  containerColor = LaserCrimson,
                  contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("disconnect_bridge_btn")
              ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "DISCONNECT",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                )
              }
            }

            Button(
              onClick = onEnableSimulation,
              colors = ButtonDefaults.buttonColors(
                containerColor = CardSurfaceVariant,
                contentColor = if (connectionStatus == ConnectionStatus.SIMULATION) NeonCyan else TextPrimary
              ),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.testTag("enable_simulation_btn")
            ) {
              Icon(imageVector = Icons.Default.SettingsInputAntenna, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "SIMULATOR",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
              )
            }
          }
        }
      }
    }

    // 5. Threshold & Shield Configuration
    item {
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder)), width = 0.5.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "THRESHOLD ALARM LIMITS",
              style = MaterialTheme.typography.titleMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp
              )
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // RF Alert Slider
          Text(
            text = "RF ALERT THRESHOLD: ${rfAlert.toInt()} dBm",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontFamily = FontFamily.Monospace)
          )
          Slider(
            value = rfAlert,
            onValueChange = { rfAlert = it },
            onValueChangeFinished = { onUpdateRfThresholds(rfAlert, rfHigh) },
            valueRange = -110f..-40f,
            colors = SliderDefaults.colors(thumbColor = AmberAlert, activeTrackColor = AmberAlert),
            modifier = Modifier.testTag("rf_alert_slider")
          )

          // RF Critical High Slider
          Text(
            text = "RF CRITICAL INTRUSION THRESHOLD: ${rfHigh.toInt()} dBm",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontFamily = FontFamily.Monospace)
          )
          Slider(
            value = rfHigh,
            onValueChange = { rfHigh = it },
            onValueChangeFinished = { onUpdateRfThresholds(rfAlert, rfHigh) },
            valueRange = -90f..-20f,
            colors = SliderDefaults.colors(thumbColor = LaserCrimson, activeTrackColor = LaserCrimson),
            modifier = Modifier.testTag("rf_critical_slider")
          )

          HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorder, thickness = 0.5.dp)

          // FSO Alert Slider
          Text(
            text = "FSO OPTICAL ALERT THRESHOLD: ${fsoAlert.toInt()} dBm",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontFamily = FontFamily.Monospace)
          )
          Slider(
            value = fsoAlert,
            onValueChange = { fsoAlert = it },
            onValueChangeFinished = { onUpdateFsoThresholds(fsoAlert, fsoBlock) },
            valueRange = -60f..10f,
            colors = SliderDefaults.colors(thumbColor = OpticPurple, activeTrackColor = OpticPurple),
            modifier = Modifier.testTag("fso_alert_slider")
          )

          // FSO Auto Block Slider
          Text(
            text = "FSO EMERGENCY SHIELD TRIP LIMIT: ${fsoBlock.toInt()} dBm",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontFamily = FontFamily.Monospace)
          )
          Slider(
            value = fsoBlock,
            onValueChange = { fsoBlock = it },
            onValueChangeFinished = { onUpdateFsoThresholds(fsoAlert, fsoBlock) },
            valueRange = -40f..30f,
            colors = SliderDefaults.colors(thumbColor = LaserCrimson, activeTrackColor = LaserCrimson),
            modifier = Modifier.testTag("fso_block_slider")
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Auto-Shield Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Autonomous Shutter Interlock",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Automatically drop mechanical shutters if laser exceeds safety trip limit",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.5.sp)
              )
            }

            Switch(
              checked = shieldConfig.autoBlockFso,
              onCheckedChange = onToggleAutoShield,
              colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonCyanSubdued,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CardSurfaceVariant
              ),
              modifier = Modifier.testTag("auto_shield_switch")
            )
          }
        }
      }
    }

    // 6. Adversary Incursion Injectors
    item {
      Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(LaserCrimson.copy(0.6f), AmberAlert.copy(0.6f))), width = 0.75.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = LaserCrimson, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "TACTICAL INCURSION INJECTORS (TEST)",
              style = MaterialTheme.typography.titleMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp
              )
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Inject artificial hostile electromagnetic intrusions to verify sensor thresholds, trigger alarm state transitions, and test autonomous mitigation circuits.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.5.sp)
          )

          Spacer(modifier = Modifier.height(12.dp))

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = onTriggerLaserAttack,
              colors = ButtonDefaults.buttonColors(containerColor = LaserCrimson, contentColor = TextPrimary),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("inject_laser_attack_btn")
            ) {
              Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("INJECT HIGH-ENERGY PULSED LASER ATTACK (1550nm)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp))
            }

            Button(
              onClick = onTriggerRfJammer,
              colors = ButtonDefaults.buttonColors(containerColor = AmberAlert, contentColor = ObsidianBlack),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("inject_rf_jammer_btn")
            ) {
              Icon(imageVector = Icons.Default.SignalCellularConnectedNoInternet0Bar, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("INJECT BROADBAND RF JAMMER (Wi-Fi 2.4G Ch 6)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp))
            }

            Button(
              onClick = onTriggerLidarSpoof,
              colors = ButtonDefaults.buttonColors(containerColor = OpticPurple, contentColor = TextPrimary),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("inject_lidar_spoof_btn")
            ) {
              Icon(imageVector = Icons.Default.SecurityUpdateWarning, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("INJECT PULSED LIDAR SPOOFING (905nm)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp))
            }
          }
        }
      }
    }
  }
}
