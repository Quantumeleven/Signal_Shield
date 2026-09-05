package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.model.DecodedFrame
import com.example.model.FrameProtocolType
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProtocolFrameItem(
  frame: DecodedFrame,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

  val protoColor = when (frame.protocol) {
    FrameProtocolType.WIFI_802_11 -> NeonCyan
    FrameProtocolType.BLE_ADV -> NeonEmerald
    FrameProtocolType.CELLULAR_LTE_SIB -> AmberAlert
    FrameProtocolType.LORA_CSS -> OpticPurple
    FrameProtocolType.OPTICAL_PPM_FSO -> LaserCrimson
  }

  val protoLabel = when (frame.protocol) {
    FrameProtocolType.WIFI_802_11 -> "802.11 AX/N"
    FrameProtocolType.BLE_ADV -> "BLE 5.2 ADV"
    FrameProtocolType.CELLULAR_LTE_SIB -> "LTE SIB1"
    FrameProtocolType.LORA_CSS -> "LoRa CSS"
    FrameProtocolType.OPTICAL_PPM_FSO -> "FSO PPM"
  }

  Card(
    shape = RoundedCornerShape(8.dp),
    colors = CardDefaults.cardColors(containerColor = CardSurface),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(protoColor.copy(alpha = 0.4f), CardBorder)),
      width = 0.75.dp
    ),
    modifier = modifier
      .fillMaxWidth()
      .clickable { expanded = !expanded }
      .testTag("frame_item_${frame.id}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = protoColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(4.dp)
          ) {
            Text(
              text = protoLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                color = protoColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              ),
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          Text(
            text = frame.channelOrFreq,
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextSecondary,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            )
          )
        }

        Text(
          text = timeFormatter.format(Date(frame.timestamp)),
          style = MaterialTheme.typography.labelSmall.copy(
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp
          )
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = frame.summary,
        style = MaterialTheme.typography.bodyMedium.copy(
          color = TextPrimary,
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.5.sp
        )
      )

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "SRC: ${frame.sourceAddress}",
          style = MaterialTheme.typography.labelSmall.copy(
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp
          )
        )

        Row {
          Text(
            text = "${frame.rssiDbm.toInt()} dBm",
            style = MaterialTheme.typography.labelSmall.copy(
              color = NeonCyan,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 10.5.sp
            )
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "SNR ${frame.snrDb.toInt()}dB",
            style = MaterialTheme.typography.labelSmall.copy(
              color = NeonEmerald,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.5.sp
            )
          )
        }
      }

      AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
          Divider(color = CardBorder, thickness = 0.5.dp)
          Spacer(modifier = Modifier.height(6.dp))

          // Key-Value Protocol Fields
          if (frame.extraInfo.isNotEmpty()) {
            frame.extraInfo.forEach { (k, v) ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(k, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace))
                Text(v, style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace))
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
          }

          // Raw Hex Payload Inspection
          Text(
            text = "DECODED HEX PAYLOAD",
            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
          )
          Spacer(modifier = Modifier.height(4.dp))

          Surface(
            color = ObsidianBlack,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(0.5.dp, CardBorder, RoundedCornerShape(4.dp))
              .padding(6.dp)
          ) {
            Text(
              text = frame.payloadHex,
              style = MaterialTheme.typography.bodySmall.copy(
                color = NeonCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                lineHeight = 14.sp
              )
            )
          }
        }
      }
    }
  }
}
