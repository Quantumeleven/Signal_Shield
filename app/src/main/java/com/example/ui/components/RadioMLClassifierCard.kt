package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.model.ModulationClassification
import com.example.ui.theme.*

@Composable
fun RadioMLClassifierCard(
  classification: ModulationClassification,
  candidateList: List<Pair<String, Float>>,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = CardSurface),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(NeonCyan.copy(0.4f), OpticPurple.copy(0.4f))),
      width = 1.dp
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("radioml_classifier_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "RadioML AMR NEURAL CLASSIFIER",
            style = MaterialTheme.typography.titleMedium.copy(
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.5.sp
            )
          )
        }

        Surface(
          color = NeonCyan.copy(alpha = 0.15f),
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            text = "arXiv:1602.04105",
            style = MaterialTheme.typography.labelSmall.copy(
              color = NeonCyan,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Top Predicted Modulation & Confidence Progress
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Column {
          Text(
            text = "PREDICTED MODULATION",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
          )
          Text(
            text = classification.modulation,
            style = MaterialTheme.typography.titleLarge.copy(
              color = NeonEmerald,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 18.sp
            )
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "CONFIDENCE",
            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
          )
          Text(
            text = "${(classification.confidence * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium.copy(
              color = if (classification.confidence > 0.85f) NeonEmerald else AmberAlert,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              fontSize = 16.sp
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      LinearProgressIndicator(
        progress = { classification.confidence },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = NeonEmerald,
        trackColor = CardSurfaceVariant
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Neural Net Probability Distribution Bars
      Text(
        text = "MODULATION SOFTMAX PROBABILITIES",
        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
      )

      Spacer(modifier = Modifier.height(6.dp))

      candidateList.take(4).forEach { (mod, prob) ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = mod,
            style = MaterialTheme.typography.bodySmall.copy(
              color = if (mod == classification.modulation) TextPrimary else TextSecondary,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp
            ),
            modifier = Modifier.width(90.dp)
          )

          LinearProgressIndicator(
            progress = { prob },
            modifier = Modifier
              .weight(1f)
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp)),
            color = if (mod == classification.modulation) NeonCyan else TextMuted,
            trackColor = CardSurfaceVariant
          )

          Spacer(modifier = Modifier.width(8.dp))

          Text(
            text = "${(prob * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (mod == classification.modulation) NeonCyan else TextMuted,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            ),
            modifier = Modifier.width(32.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Divider(color = CardBorder, thickness = 0.5.dp)
      Spacer(modifier = Modifier.height(8.dp))

      // Physical Layer Estimations
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("EST. SNR", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace))
          Text("${classification.snrEstDb} dB", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp))
        }

        Column {
          Text("EST. SYMBOL RATE", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace))
          Text("${classification.baudRateEstKhz} kBaud", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp))
        }

        Column {
          Text("CYCLOSTATIONARY \u03B1", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace))
          Text("${classification.cyclostationaryPeakAlpha} MHz", style = MaterialTheme.typography.labelSmall.copy(color = OpticPurple, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp))
        }
      }
    }
  }
}
