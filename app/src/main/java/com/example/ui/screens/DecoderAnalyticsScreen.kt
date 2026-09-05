package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DecodedFrame
import com.example.model.FrameProtocolType
import com.example.model.ModulationClassification
import com.example.model.SigMFMetadata
import com.example.ui.components.ProtocolFrameItem
import com.example.ui.components.RadioMLClassifierCard
import com.example.ui.components.SigMFExportDialog
import com.example.ui.theme.*

@Composable
fun DecoderAnalyticsScreen(
  classification: ModulationClassification,
  candidateList: List<Pair<String, Float>>,
  decodedFrames: List<DecodedFrame>,
  sigmfMetadata: SigMFMetadata,
  sigmfJson: String,
  onClearFrames: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedProtoFilter by remember { mutableStateOf("ALL") }
  var showSigmfDialog by remember { mutableStateOf(false) }

  val filteredFrames = decodedFrames.filter { fr ->
    when (selectedProtoFilter) {
      "WIFI" -> fr.protocol == FrameProtocolType.WIFI_802_11
      "BLE" -> fr.protocol == FrameProtocolType.BLE_ADV
      "LTE" -> fr.protocol == FrameProtocolType.CELLULAR_LTE_SIB
      "LORA" -> fr.protocol == FrameProtocolType.LORA_CSS
      "FSO" -> fr.protocol == FrameProtocolType.OPTICAL_PPM_FSO
      else -> true
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
  ) {
    // 1. RadioML Neural AMR Card
    item {
      RadioMLClassifierCard(
        classification = classification,
        candidateList = candidateList
      )
    }

    // 2. Action Header: SigMF Forensic Export & Frame Clear
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "LIVE PROTOCOL DISSECTOR (${decodedFrames.size})",
            style = MaterialTheme.typography.labelMedium.copy(
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp
            )
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          OutlinedButton(
            onClick = { showSigmfDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
            border = ButtonDefaults.outlinedButtonBorder.copy(
              brush = Brush.horizontalGradient(listOf(NeonCyan, NeonCyan.copy(0.4f)))
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.testTag("sigmf_export_btn")
          ) {
            Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("SigMF v1.0", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontFamily = FontFamily.Monospace))
          }

          Spacer(modifier = Modifier.width(6.dp))

          IconButton(
            onClick = onClearFrames,
            modifier = Modifier
              .size(30.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(CardSurface)
              .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
              .testTag("clear_frames_btn")
          ) {
            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Clear Sniffer", tint = TextMuted, modifier = Modifier.size(16.dp))
          }
        }
      }
    }

    // 3. Protocol Filter Chips
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        listOf("ALL", "WIFI", "BLE", "LTE", "LORA", "FSO").forEach { filter ->
          val isSel = selectedProtoFilter == filter
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSel) NeonCyan.copy(0.18f) else CardSurface)
              .border(
                1.dp,
                if (isSel) NeonCyan else CardBorder,
                RoundedCornerShape(6.dp)
              )
              .clickable { selectedProtoFilter = filter }
              .padding(vertical = 6.dp)
              .testTag("proto_filter_$filter"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = filter,
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
    }

    // 4. Decoded Frames Stream
    items(filteredFrames.size) { index ->
      val frame = filteredFrames[index]
      ProtocolFrameItem(frame = frame)
    }
  }

  // SigMF Export Dialog
  if (showSigmfDialog) {
    SigMFExportDialog(
      metadata = sigmfMetadata,
      sigmfJson = sigmfJson,
      onDismiss = { showSigmfDialog = false }
    )
  }
}
