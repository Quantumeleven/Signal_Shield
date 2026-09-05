package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BioMetricData
import com.example.data.entity.SystemEventEntity
import com.example.data.entity.ThreatLogEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ThreatLogScreen(
  persistedThreats: List<ThreatLogEntity>,
  persistedSystemEvents: List<SystemEventEntity>,
  persistedBioMetrics: List<BioMetricData> = emptyList(),
  searchQuery: String,
  selectedFilter: String,
  activeTab: Int,
  onSearchQueryChanged: (String) -> Unit,
  onFilterChanged: (String) -> Unit,
  onTabChanged: (Int) -> Unit,
  onDeleteThreat: (Long) -> Unit,
  onClearAllThreats: () -> Unit,
  onDeleteSystemEvent: (Long) -> Unit,
  onClearAllSystemEvents: () -> Unit,
  onDeleteBioMetric: (Long) -> Unit = {},
  onClearAllBioMetrics: () -> Unit = {},
  onExportJson: () -> String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showExportDialog by remember { mutableStateOf(false) }
  var showClearConfirmDialog by remember { mutableStateOf(false) }
  var exportReportText by remember { mutableStateOf("") }

  val timeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

  // Filter Threats by search query and category chip
  val filteredThreats = remember(persistedThreats, searchQuery, selectedFilter) {
    persistedThreats.filter { item ->
      val matchesFilter = when (selectedFilter) {
        "CRITICAL" -> item.severity == "CRITICAL"
        "HIGH" -> item.severity == "HIGH" || item.severity == "CRITICAL"
        "RF" -> item.source == "RF"
        "FSO" -> item.source == "FSO"
        else -> true
      }
      val matchesSearch = if (searchQuery.isBlank()) true else {
        item.threatType.contains(searchQuery, ignoreCase = true) ||
            item.band.contains(searchQuery, ignoreCase = true) ||
            item.details.contains(searchQuery, ignoreCase = true) ||
            item.eventId.contains(searchQuery, ignoreCase = true)
      }
      matchesFilter && matchesSearch
    }
  }

  // Filter System Events by search query and category chip
  val filteredSystemEvents = remember(persistedSystemEvents, searchQuery, selectedFilter) {
    persistedSystemEvents.filter { item ->
      val matchesFilter = when (selectedFilter) {
        "CONNECTION" -> item.category == "CONNECTION"
        "SHIELD" -> item.category == "SHIELD"
        "AI" -> item.category == "AI_ANALYSIS"
        "CONFIG" -> item.category == "CONFIG"
        "BIOMED" -> item.category == "BIOMED_ALERT" || item.category == "EMERGENCY_DISPATCH"
        else -> true
      }
      val matchesSearch = if (searchQuery.isBlank()) true else {
        item.title.contains(searchQuery, ignoreCase = true) ||
            item.message.contains(searchQuery, ignoreCase = true) ||
            item.targetEndpoint.contains(searchQuery, ignoreCase = true)
      }
      matchesFilter && matchesSearch
    }
  }

  // Filter BioMetric Telemetry Audit Trail by search query and filter chips
  val filteredBioMetrics = remember(persistedBioMetrics, searchQuery, selectedFilter) {
    persistedBioMetrics.filter { item ->
      val matchesFilter = when (selectedFilter) {
        "EMERGENCY" -> item.isEmergency
        "JAMMED" -> item.status == "JAMMED" || item.packetLossPercent > 50f
        "CRITICAL" -> item.status == "CRITICAL" || item.isEmergency
        "WARNING" -> item.status == "WARNING"
        else -> true
      }
      val matchesSearch = if (searchQuery.isBlank()) true else {
        item.deviceId.contains(searchQuery, ignoreCase = true) ||
            item.patientName.contains(searchQuery, ignoreCase = true) ||
            item.patientRoom.contains(searchQuery, ignoreCase = true) ||
            item.deviceType.contains(searchQuery, ignoreCase = true) ||
            item.auditNotes.contains(searchQuery, ignoreCase = true)
      }
      matchesFilter && matchesSearch
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    // Top Header: Title, Room DB Badge, and Action Buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Storage,
          contentDescription = null,
          tint = NeonCyan,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "MONITORING LOG & AUDIT",
            style = MaterialTheme.typography.titleMedium.copy(
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 13.5.sp
            )
          )
          Text(
            text = "Room SQLite Local Persistence • Schema v2",
            style = MaterialTheme.typography.labelSmall.copy(
              color = NeonEmerald,
              fontSize = 9.5.sp,
              fontFamily = FontFamily.Monospace
            )
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(
          onClick = {
            exportReportText = onExportJson()
            showExportDialog = true
          },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
          border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(listOf(NeonCyan, NeonCyan.copy(0.4f)))
          ),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.testTag("export_log_btn")
        ) {
          Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("EXPORT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontFamily = FontFamily.Monospace))
        }

        Spacer(modifier = Modifier.width(6.dp))

        IconButton(
          onClick = { showClearConfirmDialog = true },
          modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
            .testTag("clear_log_btn")
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Clear History",
            tint = LaserCrimson,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Triple Subtabs: Threat Incidents vs System Audit Events vs BioMetric Telemetry
    TabRow(
      selectedTabIndex = activeTab,
      containerColor = CardSurface,
      contentColor = NeonCyan,
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .border(0.5.dp, CardBorder, RoundedCornerShape(8.dp))
    ) {
      Tab(
        selected = activeTab == 0,
        onClick = { onTabChanged(0) },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "THREATS (${persistedThreats.size})",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            )
          }
        },
        selectedContentColor = NeonCyan,
        unselectedContentColor = TextMuted,
        modifier = Modifier.testTag("tab_threat_logs")
      )

      Tab(
        selected = activeTab == 1,
        onClick = { onTabChanged(1) },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "SYSTEM (${persistedSystemEvents.size})",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            )
          }
        },
        selectedContentColor = NeonCyan,
        unselectedContentColor = TextMuted,
        modifier = Modifier.testTag("tab_system_events")
      )

      Tab(
        selected = activeTab == 2,
        onClick = { onTabChanged(2) },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.MonitorHeart, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "BIOMETRIC (${persistedBioMetrics.size})",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            )
          }
        },
        selectedContentColor = NeonCyan,
        unselectedContentColor = TextMuted,
        modifier = Modifier.testTag("tab_biometric_logs")
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Search Bar Input
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChanged,
      placeholder = {
        Text(
          text = when (activeTab) {
            0 -> "Search by band, threat type, or ID..."
            1 -> "Search system event messages..."
            else -> "Search by patient, device ID, room, or notes..."
          },
          fontSize = 11.sp,
          color = TextMuted
        )
      },
      leadingIcon = {
        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
      },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchQueryChanged("") }) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Search", tint = TextMuted, modifier = Modifier.size(14.dp))
          }
        }
      },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonCyan,
        unfocusedBorderColor = CardBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = CardSurface,
        unfocusedContainerColor = CardSurface
      ),
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("log_search_field")
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Filter Chips Row
    val filterOptions = when (activeTab) {
      0 -> listOf("ALL", "CRITICAL", "HIGH", "RF", "FSO")
      1 -> listOf("ALL", "CONNECTION", "SHIELD", "AI", "BIOMED")
      else -> listOf("ALL", "EMERGENCY", "JAMMED", "CRITICAL", "OVERDOSE")
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      filterOptions.forEach { filterKey ->
        val isSel = selectedFilter == filterKey
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSel) NeonCyan.copy(0.18f) else CardSurface)
            .border(1.dp, if (isSel) NeonCyan else CardBorder, RoundedCornerShape(6.dp))
            .clickable { onFilterChanged(filterKey) }
            .padding(vertical = 5.dp)
            .testTag("filter_chip_$filterKey"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = filterKey,
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (isSel) NeonCyan else TextSecondary,
              fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            )
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Main Log Content based on activeTab
    when (activeTab) {
      0 -> {
        // Threat History Log
        if (filteredThreats.isEmpty()) {
          EmptyLogPlaceholder(
            icon = Icons.Default.VerifiedUser,
            iconTint = NeonEmerald,
            title = "NO THREAT RECORDS FOUND",
            description = if (searchQuery.isNotEmpty()) "No entries match search query '$searchQuery'." else "Room database has no logged threats matching filter."
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
          ) {
            items(filteredThreats.size, key = { index -> filteredThreats[index].id }) { i ->
              val threat = filteredThreats[i]
              PersistedThreatCard(
                threat = threat,
                formattedTime = timeFormatter.format(Date(threat.timestamp)),
                onDelete = { onDeleteThreat(threat.id) }
              )
            }
          }
        }
      }
      1 -> {
        // System Audit Events Log
        if (filteredSystemEvents.isEmpty()) {
          EmptyLogPlaceholder(
            icon = Icons.Default.Info,
            iconTint = NeonCyan,
            title = "NO SYSTEM EVENTS FOUND",
            description = if (searchQuery.isNotEmpty()) "No entries match search query '$searchQuery'." else "Room database has no logged system audit events."
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
          ) {
            items(filteredSystemEvents.size, key = { index -> filteredSystemEvents[index].id }) { i ->
              val evt = filteredSystemEvents[i]
              PersistedSystemEventCard(
                event = evt,
                formattedTime = timeFormatter.format(Date(evt.timestamp)),
                onDelete = { onDeleteSystemEvent(evt.id) }
              )
            }
          }
        }
      }
      else -> {
        // BioMetric Telemetry & Patient Audit Trail Log
        if (filteredBioMetrics.isEmpty()) {
          EmptyLogPlaceholder(
            icon = Icons.Default.MonitorHeart,
            iconTint = NeonEmerald,
            title = "NO BIOMETRIC AUDIT LOGS",
            description = if (searchQuery.isNotEmpty()) "No entries match search query '$searchQuery'." else "Room database has no historical physiological telemetry logs."
          )
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
          ) {
            items(filteredBioMetrics.size, key = { index -> filteredBioMetrics[index].id }) { i ->
              val bio = filteredBioMetrics[i]
              PersistedBioMetricCard(
                bio = bio,
                formattedTime = timeFormatter.format(Date(bio.timestamp)),
                onDelete = { onDeleteBioMetric(bio.id) }
              )
            }
          }
        }
      }
    }
  }

  // Clear Confirmation Dialog
  if (showClearConfirmDialog) {
    val targetLogName = when (activeTab) {
      0 -> "RF/FSO Threat Incursion"
      1 -> "System Event Audit"
      else -> "BioMetric Telemetry Audit"
    }
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = {
        Text("Clear $targetLogName Logs?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
      },
      text = {
        Text(
          "Are you sure you want to permanently purge all persistent $targetLogName records from the Room SQLite database? This action is irreversible.",
          style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
        )
      },
      confirmButton = {
        Button(
          onClick = {
            when (activeTab) {
              0 -> onClearAllThreats()
              1 -> onClearAllSystemEvents()
              else -> onClearAllBioMetrics()
            }
            showClearConfirmDialog = false
            Toast.makeText(context, "$targetLogName logs purged.", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = LaserCrimson)
        ) {
          Text("Purge DB Records", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = CardSurface,
      shape = RoundedCornerShape(12.dp)
    )
  }

  // Export Dialog
  if (showExportDialog) {
    AlertDialog(
      onDismissRequest = { showExportDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Database & Telemetry Audit Export", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
      },
      text = {
        Column {
          Text("Serialized JSON snapshot of current Room SQLite database & active telemetry status:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = ObsidianBlack,
            shape = RoundedCornerShape(6.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder)), width = 0.5.dp),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 240.dp)
          ) {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
              item {
                Text(
                  text = exportReportText,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = NeonEmerald
                  )
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("SignalShield Telemetry Export", exportReportText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Export copied to clipboard!", Toast.LENGTH_SHORT).show()
            showExportDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
          Text("Copy to Clipboard", color = ObsidianBlack, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showExportDialog = false }) {
          Text("Close", color = TextSecondary)
        }
      },
      containerColor = CardSurface,
      shape = RoundedCornerShape(12.dp)
    )
  }
}

@Composable
fun PersistedBioMetricCard(
  bio: BioMetricData,
  formattedTime: String,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }

  val isJammed = bio.status == "JAMMED" || bio.packetLossPercent > 50f
  val statusColor = when {
    bio.isEmergency || bio.status == "CRITICAL" -> LaserCrimson
    isJammed -> AmberAlert
    bio.status == "WARNING" -> NeonCyan
    else -> NeonEmerald
  }

  Card(
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = CardSurface),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(
        if (bio.isEmergency) listOf(LaserCrimson, LaserCrimsonSubdued)
        else if (isJammed) listOf(AmberAlert, AmberSubdued)
        else listOf(CardBorder, CardBorder)
      ),
      width = if (bio.isEmergency || isJammed) 1.dp else 0.5.dp
    ),
    modifier = modifier
      .fillMaxWidth()
      .clickable { expanded = !expanded }
      .testTag("biometric_log_item_${bio.id}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(statusColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = bio.deviceId,
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp
            )
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = statusColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(3.dp)
          ) {
            Text(
              text = if (bio.isEmergency) "EMERGENCY" else bio.status,
              style = MaterialTheme.typography.labelSmall.copy(
                color = statusColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              ),
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }

          if (isJammed) {
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
              color = AmberAlert.copy(alpha = 0.15f),
              shape = RoundedCornerShape(3.dp)
            ) {
              Text(
                text = "RF JAMMED",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = AmberAlert,
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextMuted,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp
            )
          )
          Spacer(modifier = Modifier.width(4.dp))
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(20.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Delete Biometric Log Entry",
              tint = TextMuted,
              modifier = Modifier.size(12.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "${bio.patientName} (${bio.patientRoom})",
            style = MaterialTheme.typography.titleMedium.copy(
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp
            )
          )
          Text(
            text = "Type: ${bio.deviceType.replace("_", " ")}",
            style = MaterialTheme.typography.bodySmall.copy(
              color = TextSecondary,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "${bio.heartRateBpm} BPM",
              style = MaterialTheme.typography.labelSmall.copy(
                color = if (bio.heartRateBpm > 130 || bio.heartRateBpm < 45) LaserCrimson else NeonEmerald,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
              )
            )
            Text(
              text = "SpO2 ${bio.spO2Percent}%",
              style = MaterialTheme.typography.labelSmall.copy(
                color = if (bio.spO2Percent < 90) LaserCrimson else NeonCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp
              )
            )
          }
        }
      }

      AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
          Divider(color = CardBorder, thickness = 0.5.dp)
          Spacer(modifier = Modifier.height(6.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
              text = "Blood Pressure: ${bio.bloodPressureSys}/${bio.bloodPressureDia} mmHg",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            )
            Text(
              text = "Resp: ${bio.respirationRateBpm} /min",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            )
          }

          Spacer(modifier = Modifier.height(3.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
              text = "Battery: ${bio.batteryPercent}%",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            )
            Text(
              text = "RF RSSI: ${"%.1f".format(bio.rssiDbm)} dBm (${bio.packetLossPercent.toInt()}% drop)",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            )
          }

          if (bio.anomalyDescription != null && bio.anomalyDescription.isNotEmpty()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = "Anomaly: ${bio.anomalyDescription}",
              style = MaterialTheme.typography.bodySmall.copy(color = LaserCrimson, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            )
          }

          if (bio.auditNotes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Audit Notes: ${bio.auditNotes}",
              style = MaterialTheme.typography.bodySmall.copy(
                color = if (bio.isEmergency) LaserCrimson else TextPrimary,
                fontSize = 10.5.sp
              )
            )
          }

          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Room DB Audit ID: #${bio.id} • Registered: ${bio.timestamp}",
            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
          )
        }
      }
    }
  }
}

@Composable
fun PersistedThreatCard(
  threat: ThreatLogEntity,
  formattedTime: String,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }

  val isCrit = threat.severity == "CRITICAL"
  val isHigh = threat.severity == "HIGH"

  val sevColor = when (threat.severity) {
    "CRITICAL" -> LaserCrimson
    "HIGH" -> AmberAlert
    "MEDIUM" -> NeonCyan
    else -> NeonEmerald
  }

  Card(
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = CardSurface),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(
        if (isCrit) listOf(LaserCrimson, LaserCrimsonSubdued)
        else if (isHigh) listOf(AmberAlert, AmberSubdued)
        else listOf(CardBorder, CardBorder)
      ),
      width = if (isCrit || isHigh) 1.dp else 0.5.dp
    ),
    modifier = modifier
      .fillMaxWidth()
      .clickable { expanded = !expanded }
      .testTag("threat_log_item_${threat.id}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(sevColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = threat.eventId,
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextMuted,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            )
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = sevColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(3.dp)
          ) {
            Text(
              text = threat.severity,
              style = MaterialTheme.typography.labelSmall.copy(
                color = sevColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              ),
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = if (threat.source == "FSO") OpticPurple.copy(0.18f) else NeonCyan.copy(0.18f),
            shape = RoundedCornerShape(3.dp)
          ) {
            Text(
              text = threat.source,
              style = MaterialTheme.typography.labelSmall.copy(
                color = if (threat.source == "FSO") OpticPurple else NeonCyan,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              ),
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextMuted,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp
            )
          )
          Spacer(modifier = Modifier.width(4.dp))
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(20.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Delete Threat Log Entry",
              tint = TextMuted,
              modifier = Modifier.size(12.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = threat.threatType,
          style = MaterialTheme.typography.titleMedium.copy(
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        )

        Text(
          text = "${threat.powerDbm} dBm",
          style = MaterialTheme.typography.labelSmall.copy(
            color = sevColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
          )
        )
      }

      Text(
        text = "Band: ${threat.band} • Threshold: ${threat.thresholdDbm} dBm",
        style = MaterialTheme.typography.bodySmall.copy(
          color = TextSecondary,
          fontSize = 10.5.sp,
          fontFamily = FontFamily.Monospace
        )
      )

      AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
          Divider(color = CardBorder, thickness = 0.5.dp)
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = threat.details,
            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 11.sp)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "DEFENSE ACTION: ",
              style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
            )
            Text(
              text = threat.actionTaken,
              style = MaterialTheme.typography.labelSmall.copy(
                color = NeonEmerald,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Room DB Record ID: #${threat.id}",
            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
          )
        }
      }
    }
  }
}

@Composable
fun PersistedSystemEventCard(
  event: SystemEventEntity,
  formattedTime: String,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val catColor = when (event.category) {
    "CONNECTION" -> NeonCyan
    "SHIELD" -> LaserCrimson
    "AI_ANALYSIS" -> OpticPurple
    "SCAN" -> NeonEmerald
    "BIOMED_ALERT" -> AmberAlert
    "EMERGENCY_DISPATCH" -> LaserCrimson
    else -> AmberAlert
  }

  Card(
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = CardSurface),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder)), width = 0.5.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("system_event_item_${event.id}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = catColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(3.dp)
          ) {
            Text(
              text = event.category,
              style = MaterialTheme.typography.labelSmall.copy(
                color = catColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              ),
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = event.title,
            style = MaterialTheme.typography.titleMedium.copy(
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp
            )
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextMuted,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp
            )
          )
          Spacer(modifier = Modifier.width(4.dp))
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(20.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Delete Event",
              tint = TextMuted,
              modifier = Modifier.size(12.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = event.message,
        style = MaterialTheme.typography.bodySmall.copy(
          color = TextSecondary,
          fontSize = 11.sp
        )
      )

      if (event.targetEndpoint.isNotEmpty()) {
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "Endpoint: ${event.targetEndpoint}",
          style = MaterialTheme.typography.labelSmall.copy(
            color = NeonCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp
          )
        )
      }
    }
  }
}

@Composable
private fun EmptyLogPlaceholder(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: Color,
  title: String,
  description: String
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(bottom = 80.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(48.dp)
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
          color = iconTint,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
        modifier = Modifier.padding(horizontal = 24.dp)
      )
    }
  }
}
