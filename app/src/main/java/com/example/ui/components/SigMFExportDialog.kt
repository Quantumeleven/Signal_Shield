package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SigMFMetadata
import com.example.ui.theme.*

@Composable
fun SigMFExportDialog(
  metadata: SigMFMetadata,
  sigmfJson: String,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = SurfaceDark,
    modifier = modifier.testTag("sigmf_export_dialog"),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = NeonCyan)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "SigMF v1.0 FORENSIC EXPORT",
          style = MaterialTheme.typography.titleMedium.copy(
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
          )
        )
      }
    },
    text = {
      Column {
        Text(
          text = "Standard Signal Metadata Format (SigMF v1.0) spec-compliant dataset for DARPA/GNU Radio forensic analysis:",
          style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.5.sp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Metadata Spec Highlights
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("DATA TYPE", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace))
            Text(metadata.datatype, style = MaterialTheme.typography.labelSmall.copy(color = NeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.sp))
          }
          Column {
            Text("SAMPLE RATE", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace))
            Text("${metadata.sampleRateHz / 1_000_000} MSps", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.sp))
          }
          Column {
            Text("CENTER FREQ", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace))
            Text("${metadata.centerFreqHz / 1_000_000} MHz", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.sp))
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // JSON Spec viewer
        Surface(
          color = ObsidianBlack,
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
        ) {
          LazyColumn(modifier = Modifier.padding(10.dp)) {
            item {
              Text(
                text = sigmfJson,
                style = MaterialTheme.typography.bodySmall.copy(
                  color = NeonEmerald,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp,
                  lineHeight = 13.5.sp
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
          val clip = ClipData.newPlainText("SigMF Metadata", sigmfJson)
          clipboard.setPrimaryClip(clip)
          Toast.makeText(context, "Copied .sigmf-meta to clipboard", Toast.LENGTH_SHORT).show()
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.testTag("sigmf_copy_btn")
      ) {
        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("COPY .sigmf-meta", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("CLOSE", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
      }
    }
  )
}
