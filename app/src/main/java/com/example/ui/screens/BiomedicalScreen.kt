package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun BiomedicalScreen(
  devices: List<BiomedicalDevice>,
  selectedDevice: BiomedicalDevice?,
  onSelectDevice: (BiomedicalDevice) -> Unit,
  onInjectJamming: (String) -> Unit,
  onInjectArrhythmia: (String) -> Unit,
  onInjectInfusionOverdose: (String) -> Unit,
  onInjectVentFailure: (String) -> Unit,
  onResolveDevice: (String) -> Unit,
  onRestoreAll: () -> Unit,
  onDispatchEmergency: (BiomedicalDevice) -> MedicalEmergencyDispatch,
  hipaaMaskingEnabled: Boolean = false,
  onNavigateToSettings: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var activeFilter by remember { mutableStateOf("ALL") }
  var showEmergencyDialog by remember { mutableStateOf<BiomedicalDevice?>(null) }
  var showPcGuideDialog by remember { mutableStateOf(false) }

  val filteredDevices = remember(devices, activeFilter) {
    when (activeFilter) {
      "EMERGENCY" -> devices.filter { it.status.isEmergency || it.status == BiomedicalDeviceStatus.ABNORMAL_VITALS }
      "CARDIOLOGY" -> devices.filter { it.deviceType == BiomedicalDeviceType.PACEMAKER_IMPLANT || it.deviceType == BiomedicalDeviceType.ECG_TELEMETRY_MONITOR }
      "ICU" -> devices.filter { it.deviceType == BiomedicalDeviceType.SMART_INFUSION_PUMP || it.deviceType == BiomedicalDeviceType.ICU_VENTILATOR }
      else -> devices
    }
  }

  val criticalCount = devices.count { it.status.isEmergency }
  val warningCount = devices.count { it.status == BiomedicalDeviceStatus.ABNORMAL_VITALS }
  val nominalCount = devices.count { it.status == BiomedicalDeviceStatus.NOMINAL }

  val targetDevice = selectedDevice ?: devices.firstOrNull()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
  ) {
    // 1. Header Banner & Status Fleet Metric
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(
            listOf(
              if (criticalCount > 0) LaserCrimson else ElectricCyan,
              if (criticalCount > 0) LaserCrimson.copy(alpha = 0.4f) else NeonEmerald.copy(alpha = 0.4f)
            )
          )
        )
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(
                    if (criticalCount > 0) LaserCrimson.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.2f)
                  ),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.MonitorHeart,
                  contentDescription = "Bio-Medical Fleet Icon",
                  tint = if (criticalCount > 0) LaserCrimson else ElectricCyan,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "BIO-MEDICAL DEVICE LIVE FEED",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                  ),
                  color = Color.White
                )
                Text(
                  text = "Hospital Telemetry & Implantable RF Monitor",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary
                )
              }
            }

            IconButton(
              onClick = { showPcGuideDialog = true },
              modifier = Modifier.testTag("pc_bridge_guide_button")
            ) {
              Icon(
                imageVector = Icons.Default.Computer,
                contentDescription = "PC / Desktop Bridge Guide",
                tint = ElectricCyan
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 3-Metric Summary Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FleetStatusChip(
              label = "NOMINAL",
              count = nominalCount,
              color = NeonEmerald,
              modifier = Modifier.weight(1f)
            )
            FleetStatusChip(
              label = "WARNING",
              count = warningCount,
              color = AmberAlert,
              modifier = Modifier.weight(1f)
            )
            FleetStatusChip(
              label = "EMERGENCY",
              count = criticalCount,
              color = LaserCrimson,
              modifier = Modifier.weight(1f)
            )
          }

          // Emergency Quick Dispatch Trigger Banner
          if (criticalCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
              onClick = {
                val emergencyDevice = devices.firstOrNull { it.status.isEmergency } ?: targetDevice
                emergencyDevice?.let { showEmergencyDialog = it }
              },
              colors = ButtonDefaults.buttonColors(containerColor = LaserCrimson),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("dispatch_emergency_911_top_button")
            ) {
              Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "🚨 $criticalCount CRITICAL ISSUE(S) - DISPATCH EMERGENCY (911)",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = Color.White
              )
            }
          }
        }
      }
    }

    // 2. Focused Device Detail & Live Real-Time ECG / Pulse Visualizer
    targetDevice?.let { dev ->
      val displayName = if (hipaaMaskingEnabled && !dev.hospitalAuthorizationGranted) {
        "Patient ID #${dev.id.hashCode().toString().takeLast(6)} (PHI Masked)"
      } else {
        dev.patientName
      }
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
              listOf(
                if (dev.status.isEmergency) LaserCrimson else ElectricCyan.copy(alpha = 0.5f),
                SurfaceDark
              )
            )
          )
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SurfaceLight
                  ) {
                    Text(
                      text = dev.patientRoom,
                      style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                      color = ElectricCyan,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Text(
                  text = "${dev.deviceType.displayName} • ${dev.rfBandFrequency}",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondary
                )
                Text(
                  text = "HOSPITAL BAA: ${dev.hospitalName} • ${if (dev.hospitalAuthorizationGranted) "PERMITTED VIEWING" else "RESTRICTED / THIRD-PARTY VIEWING PENDING"}",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (dev.hospitalAuthorizationGranted) NeonEmerald else AmberAlert,
                    fontWeight = FontWeight.Bold
                  )
                )
              }

              // Status Badge
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (dev.status) {
                  BiomedicalDeviceStatus.NOMINAL -> NeonEmerald.copy(alpha = 0.15f)
                  BiomedicalDeviceStatus.ABNORMAL_VITALS, BiomedicalDeviceStatus.BATTERY_DEPLETED -> AmberAlert.copy(alpha = 0.15f)
                  else -> LaserCrimson.copy(alpha = 0.2f)
                },
                border = CardDefaults.outlinedCardBorder().copy(
                  brush = Brush.horizontalGradient(
                    listOf(
                      when (dev.status) {
                        BiomedicalDeviceStatus.NOMINAL -> NeonEmerald
                        BiomedicalDeviceStatus.ABNORMAL_VITALS, BiomedicalDeviceStatus.BATTERY_DEPLETED -> AmberAlert
                        else -> LaserCrimson
                      },
                      Color.Transparent
                    )
                  )
                )
              ) {
                Text(
                  text = dev.status.displayName,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  ),
                  color = when (dev.status) {
                    BiomedicalDeviceStatus.NOMINAL -> NeonEmerald
                    BiomedicalDeviceStatus.ABNORMAL_VITALS, BiomedicalDeviceStatus.BATTERY_DEPLETED -> AmberAlert
                    else -> LaserCrimson
                  },
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live ECG Waveform Display Canvas
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF070E14))
                .border(1.dp, Color(0xFF162B3D), RoundedCornerShape(8.dp))
                .padding(8.dp)
            ) {
              LiveEcgCanvas(
                samples = dev.ecgSamples,
                isFault = dev.status.isEmergency,
                modifier = Modifier.fillMaxSize()
              )

              // Overlay telemetry HUD indicators
              Row(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text(
                  text = "HR: ${dev.heartRateBpm} BPM",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                  ),
                  color = if (dev.status.isEmergency) LaserCrimson else NeonEmerald
                )
                Text(
                  text = "SpO2: ${dev.spO2Percent}%",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                  ),
                  color = if (dev.spO2Percent < 92) AmberAlert else ElectricCyan
                )
              }

              Text(
                text = "REAL-TIME LEAD II ECG / VITAL TELEMETRY",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace
                ),
                color = Color(0xFF3B5E78),
                modifier = Modifier.align(Alignment.BottomStart)
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vitals Grid
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              VitalItemBox("BLOOD PRESSURE", "${dev.bloodPressureSys}/${dev.bloodPressureDia}", "mmHg", Modifier.weight(1f))
              VitalItemBox("RESPIRATION", "${dev.respirationRateBpm}", "BPM", Modifier.weight(1f))
              VitalItemBox("RF LINK RSSI", "${dev.rssiDbm.toInt()}", "dBm", Modifier.weight(1f))
              VitalItemBox("BATTERY", "${dev.batteryPercent}%", "${dev.packetLossPercent.toInt()}% Loss", Modifier.weight(1f))
            }

            // Anomaly Description if any
            dev.anomalyDescription?.let { desc ->
              Spacer(modifier = Modifier.height(10.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = LaserCrimson.copy(alpha = 0.12f),
                border = CardDefaults.outlinedCardBorder().copy(
                  brush = Brush.horizontalGradient(listOf(LaserCrimson, Color.Transparent))
                ),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Warning, contentDescription = null, tint = LaserCrimson, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall.copy(
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.Medium
                    ),
                    color = LaserCrimson
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons (Emergency Dispatch / Resolve)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { showEmergencyDialog = dev },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (dev.status.isEmergency) LaserCrimson else AmberAlert
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("contact_emergency_button")
              ) {
                Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "CONTACT EMS (911)",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
              }

              if (dev.status != BiomedicalDeviceStatus.NOMINAL) {
                OutlinedButton(
                  onClick = { onResolveDevice(dev.id) },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonEmerald),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("resolve_device_button")
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "RESOLVE ISSUE",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                  )
                }
              }
            }
          }
        }
      }
    }

    // 3. Fault & Anomaly Injection Test Console
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "TESTING & INJECTION SIMULATION (DEVICE FAULTS)",
            style = MaterialTheme.typography.labelMedium.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            ),
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(10.dp))

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            item {
              OutlinedButton(
                onClick = { onInjectJamming("MED-PAC-8041") },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAlert)
              ) {
                Text("Inject MedRadio Jamming", style = MaterialTheme.typography.labelSmall)
              }
            }
            item {
              OutlinedButton(
                onClick = { onInjectArrhythmia("MED-PAC-8041") },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LaserCrimson)
              ) {
                Text("Trigger V-Fib Arrhythmia", style = MaterialTheme.typography.labelSmall)
              }
            }
            item {
              OutlinedButton(
                onClick = { onInjectInfusionOverdose("MED-PUMP-209") },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LaserCrimson)
              ) {
                Text("Trigger Pump Overdose", style = MaterialTheme.typography.labelSmall)
              }
            }
            item {
              OutlinedButton(
                onClick = { onInjectVentFailure("MED-VENT-550") },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LaserCrimson)
              ) {
                Text("Ventilator Apnea", style = MaterialTheme.typography.labelSmall)
              }
            }
            item {
              Button(
                onClick = { onRestoreAll() },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceLight)
              ) {
                Text("Restore All Normal", style = MaterialTheme.typography.labelSmall, color = NeonEmerald)
              }
            }
          }
        }
      }
    }

    // 4. Fleet Filter Tabs
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("ALL", "EMERGENCY", "CARDIOLOGY", "ICU").forEach { filterKey ->
          FilterChip(
            selected = activeFilter == filterKey,
            onClick = { activeFilter = filterKey },
            label = {
              Text(
                text = filterKey,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (activeFilter == filterKey) FontWeight.Bold else FontWeight.Normal
                )
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
              selectedLabelColor = ElectricCyan,
              containerColor = SurfaceDark,
              labelColor = TextSecondary
            )
          )
        }
      }
    }

    // 5. Monitored Devices List
    items(filteredDevices, key = { it.id }) { dev ->
      BiomedicalDeviceRowItem(
        device = dev,
        isSelected = dev.id == selectedDevice?.id,
        onSelect = { onSelectDevice(dev) },
        onQuickEmergency = { showEmergencyDialog = dev },
        hipaaMaskingEnabled = hipaaMaskingEnabled
      )
    }
  }

  // Emergency Dispatch & 911 Direct Contact Dialog
  showEmergencyDialog?.let { dev ->
    val report = remember(dev) { onDispatchEmergency(dev) }
    val formattedCad = remember(report) {
      com.example.engine.BiomedicalTelemetryEngine().formatEmergencyCadPayload(report)
    }

    AlertDialog(
      onDismissRequest = { showEmergencyDialog = null },
      containerColor = SurfaceDark,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Emergency, contentDescription = null, tint = LaserCrimson, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "EMERGENCY SERVICES DISPATCH",
            style = MaterialTheme.typography.titleMedium.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            ),
            color = LaserCrimson
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Initiate instant 911 / Medical Rapid Response dispatch for critical telemetry incident.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0D1822),
            border = CardDefaults.outlinedCardBorder()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "PATIENT: ${report.patientName} (${report.location})",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold
                ),
                color = Color.White
              )
              Text(
                text = "DEVICE: ${report.deviceType.displayName} [${report.deviceId}]",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = ElectricCyan
              )
              Text(
                text = "VITALS: ${report.currentVitalsSummary}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = LaserCrimson
              )
              Text(
                text = "MALFUNCTION: ${report.failureReason}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = AmberAlert
              )
            }
          }

          Text(
            text = "Recommended Action:\n${report.recommendedMedicalAction}",
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            // Launch standard phone dialer with 911 / emergency dispatch
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
              data = Uri.parse("tel:911")
            }
            try {
              context.startActivity(dialIntent)
            } catch (e: Exception) {
              Toast.makeText(context, "Initiating 911 Call: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            showEmergencyDialog = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = LaserCrimson),
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier.testTag("dial_911_confirm_button")
        ) {
          Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("CALL 911 NOW", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Medical CAD Dispatch", formattedCad)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "CAD Emergency Report Copied to Clipboard!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(6.dp)
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Copy CAD")
          }

          TextButton(onClick = { showEmergencyDialog = null }) {
            Text("Close", color = TextSecondary)
          }
        }
      }
    )
  }

  // PC & Python Bridge Guide Dialog
  if (showPcGuideDialog) {
    AlertDialog(
      onDismissRequest = { showPcGuideDialog = false },
      containerColor = SurfaceDark,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Computer, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "PC / DESKTOP SETUP & PYTHON BRIDGE",
            style = MaterialTheme.typography.titleMedium.copy(
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            ),
            color = Color.White
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "How to run fully operational on your PC / Desktop:",
            style = MaterialTheme.typography.titleSmall,
            color = ElectricCyan
          )
          Text(
            text = "1. Interactive Web Streaming (No install needed):\nThis app is live in your PC browser right now via the interactive AI Studio emulator window.\n\n2. Desktop Windows / macOS / Linux Deployment:\nRun via Android Subsystem (WSA), ChromeOS, or BlueStacks/Waydroid.\n\n3. PC Python Telemetry & SDR Bridge (gnuradio_biomed_bridge.py):\nRun the bridge script on your PC to feed live SDR and hospital telemetry over WebSocket (port 8765).",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF080F16),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "$ pip install websockets\n$ python gnuradio_biomed_bridge.py",
              style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = NeonEmerald
              ),
              modifier = Modifier.padding(10.dp)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val scriptSnippet = "pip install websockets && python gnuradio_biomed_bridge.py"
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("PC Bridge Command", scriptSnippet)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "PC Launch Command Copied!", Toast.LENGTH_SHORT).show()
            showPcGuideDialog = false
          },
          shape = RoundedCornerShape(6.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
        ) {
          Text("Copy PC Command", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showPcGuideDialog = false }) {
          Text("Close", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun FleetStatusChip(
  label: String,
  count: Int,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = color.copy(alpha = 0.12f),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), Color.Transparent))
    )
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        ),
        color = color
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace
        ),
        color = TextSecondary
      )
    }
  }
}

@Composable
private fun VitalItemBox(
  title: String,
  value: String,
  unit: String,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = SurfaceLight
  ) {
    Column(
      modifier = Modifier.padding(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace),
        color = TextSecondary
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
        color = Color.White
      )
      Text(
        text = unit,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = ElectricCyan
      )
    }
  }
}

@Composable
private fun LiveEcgCanvas(
  samples: List<Float>,
  isFault: Boolean,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val width = size.width
    val height = size.height
    val midY = height / 2f

    // Background grid lines
    val gridColor = Color(0xFF132838)
    val stepX = width / 12f
    for (i in 0..12) {
      drawLine(
        color = gridColor,
        start = Offset(i * stepX, 0f),
        end = Offset(i * stepX, height),
        strokeWidth = 1f
      )
    }
    val stepY = height / 4f
    for (j in 0..4) {
      drawLine(
        color = gridColor,
        start = Offset(0f, j * stepY),
        end = Offset(width, j * stepY),
        strokeWidth = 1f
      )
    }

    if (samples.isEmpty()) return@Canvas

    val path = Path()
    val dx = width / (samples.size - 1).coerceAtLeast(1)

    samples.forEachIndexed { index, sample ->
      val x = index * dx
      val y = midY - (sample * (height * 0.4f))
      if (index == 0) {
        path.moveTo(x, y)
      } else {
        path.lineTo(x, y)
      }
    }

    val waveColor = if (isFault) LaserCrimson else NeonEmerald
    drawPath(
      path = path,
      color = waveColor,
      style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )
  }
}

@Composable
private fun BiomedicalDeviceRowItem(
  device: BiomedicalDevice,
  isSelected: Boolean,
  onSelect: () -> Unit,
  onQuickEmergency: () -> Unit,
  hipaaMaskingEnabled: Boolean = false
) {
  val displayName = if (hipaaMaskingEnabled && !device.hospitalAuthorizationGranted) {
    "Patient #${device.id.hashCode().toString().takeLast(6)} (PHI MASKED)"
  } else {
    device.patientName
  }
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onSelect() }
      .testTag("biomed_device_item_${device.id}"),
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) SurfaceLight else SurfaceDark
    ),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(
        listOf(
          when (device.status) {
            BiomedicalDeviceStatus.NOMINAL -> if (isSelected) ElectricCyan else Color.Transparent
            BiomedicalDeviceStatus.ABNORMAL_VITALS, BiomedicalDeviceStatus.BATTERY_DEPLETED -> AmberAlert
            else -> LaserCrimson
          },
          Color.Transparent
        )
      )
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
              when (device.status) {
                BiomedicalDeviceStatus.NOMINAL -> NeonEmerald.copy(alpha = 0.15f)
                BiomedicalDeviceStatus.ABNORMAL_VITALS, BiomedicalDeviceStatus.BATTERY_DEPLETED -> AmberAlert.copy(alpha = 0.15f)
                else -> LaserCrimson.copy(alpha = 0.2f)
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when (device.deviceType) {
              BiomedicalDeviceType.PACEMAKER_IMPLANT -> Icons.Default.Favorite
              BiomedicalDeviceType.SMART_INFUSION_PUMP -> Icons.Default.Vaccines
              BiomedicalDeviceType.ICU_VENTILATOR -> Icons.Default.Air
              BiomedicalDeviceType.PULSE_OXIMETER_SPO2 -> Icons.Default.Sensors
              BiomedicalDeviceType.ECG_TELEMETRY_MONITOR -> Icons.Default.MonitorHeart
              BiomedicalDeviceType.GLUCOSE_MONITOR_CGM -> Icons.Default.Bloodtype
              BiomedicalDeviceType.NEURAL_STIMULATOR -> Icons.Default.Psychology
            },
            contentDescription = null,
            tint = when (device.status) {
              BiomedicalDeviceStatus.NOMINAL -> NeonEmerald
              BiomedicalDeviceStatus.ABNORMAL_VITALS, BiomedicalDeviceStatus.BATTERY_DEPLETED -> AmberAlert
              else -> LaserCrimson
            },
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = displayName,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "(${device.patientRoom})",
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
              color = TextSecondary
            )
          }
          Text(
            text = "${device.deviceType.displayName} • ${device.id}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = TextSecondary
          )
          Text(
            text = "${device.hospitalName} • ${if (device.hospitalAuthorizationGranted) "BAA Authorized" else "Third-Party Viewing Revoked"}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              fontFamily = FontFamily.Monospace,
              color = if (device.hospitalAuthorizationGranted) NeonEmerald else AmberAlert
            )
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "HR: ${device.heartRateBpm} BPM",
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              ),
              color = if (device.status.isEmergency) LaserCrimson else ElectricCyan
            )
            Text(
              text = "SpO2: ${device.spO2Percent}%",
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
              color = if (device.spO2Percent < 92) AmberAlert else NeonEmerald
            )
            Text(
              text = "Batt: ${device.batteryPercent}%",
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
              color = if (device.batteryPercent < 20) AmberAlert else TextSecondary
            )
          }
        }
      }

      // Action button
      if (device.status.isEmergency) {
        IconButton(
          onClick = { onQuickEmergency() },
          modifier = Modifier.testTag("quick_emergency_button_${device.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Emergency,
            contentDescription = "Quick Emergency Dispatch",
            tint = LaserCrimson
          )
        }
      } else {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          tint = TextSecondary,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
