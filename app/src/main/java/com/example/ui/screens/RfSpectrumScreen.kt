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
import com.example.model.*
import com.example.ui.components.RFBandCard
import com.example.ui.components.RFSpectrumChart
import com.example.ui.components.WaterfallSpectrogram
import com.example.ui.theme.*

@Composable
fun RfSpectrumScreen(
  rfFrame: RFSpectrumFrame,
  waterfallHistory: List<WaterfallRow>,
  shieldConfig: ShieldConfig,
  selectedGroup: String,
  onSelectGroup: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val groups = listOf("2.4GHz", "4G LTE", "5GHz")
  val currentSpectrum = rfFrame.spectra[selectedGroup] ?: rfFrame.spectra.values.firstOrNull() ?: SpectrumData(emptyList(), emptyList())
  val groupBands = rfFrame.bands.filter {
    when (selectedGroup) {
      "2.4GHz" -> it.group == "2.4GHz"
      "4G LTE" -> it.group == "4G LTE"
      "5GHz" -> it.group == "5GHz"
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
    // 1. Group Selector Tabs
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(CardSurface)
          .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        groups.forEach { grp ->
          val isSelected = grp == selectedGroup
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) NeonCyan.copy(0.2f) else CardSurface)
              .border(
                1.dp,
                if (isSelected) NeonCyan else CardBorder,
                RoundedCornerShape(6.dp)
              )
              .clickable { onSelectGroup(grp) }
              .padding(vertical = 8.dp)
              .testTag("rf_group_tab_$grp"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = grp,
              style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) NeonCyan else TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp
              )
            )
          }
        }
      }
    }

    // 2. FFT Power Spectral Density Graph
    item {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "FFT POWER SPECTRAL DENSITY (PSD)",
            style = MaterialTheme.typography.labelMedium.copy(
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.5.sp
            )
          )
          if (currentSpectrum.hwNote.isNotEmpty()) {
            Text(
              text = currentSpectrum.hwNote,
              style = MaterialTheme.typography.labelSmall.copy(
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        RFSpectrumChart(
          spectrumData = currentSpectrum,
          alertThresholdDbm = shieldConfig.rfAlertDbm,
          highThresholdDbm = shieldConfig.rfHighDbm,
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
        )
      }
    }

    // 3. 2D Waterfall Spectrogram (Time vs Frequency Heatmap)
    item {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.WaterDrop, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "2D WATERFALL SPECTROGRAM",
              style = MaterialTheme.typography.labelMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.5.sp
              )
            )
          }

          Text(
            text = "50 ROWS BUFFERED",
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextMuted,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        WaterfallSpectrogram(
          history = waterfallHistory,
          modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
        )
      }
    }

    // 4. Band Channels List Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "MONITORED RF CHANNELS (${groupBands.size})",
          style = MaterialTheme.typography.labelMedium.copy(
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
          )
        )
      }
    }

    items(groupBands.size) { index ->
      val band = groupBands[index]
      RFBandCard(
        band = band,
        alertThreshold = shieldConfig.rfAlertDbm,
        highThreshold = shieldConfig.rfHighDbm
      )
    }
  }
}
