package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
  settings: AppSystemSettings,
  onToggleGpsLocation: (Boolean) -> Unit,
  onToggleEmergencySharing: (Boolean) -> Unit,
  onUpdateFccEnforcement: (Boolean, Boolean, Boolean) -> Unit,
  onToggleHospitalAuth: (String, Boolean) -> Unit,
  onToggleHipaaMasking: (Boolean) -> Unit,
  onAddEmergencyContact: (EmergencyContact) -> Unit,
  onRemoveEmergencyContact: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var activeSubTab by remember { mutableStateOf(0) } // 0 = Location & Emergency, 1 = FCC Regulation, 2 = HIPAA & Hospitals, 3 = Privacy Policy
  var showAddContactDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBlack)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
  ) {
    // 1. Header Banner
    item {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("settings_header_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.horizontalGradient(listOf(NeonEmerald, ElectricCyan))
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
                  .background(NeonEmerald.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Settings,
                  contentDescription = "System Settings",
                  tint = NeonEmerald,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "SETTINGS & REGULATORY GOVERNANCE",
                  style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                  )
                )
                Text(
                  text = "FCC Part 15/95 • HIPAA BAA • Emergency CAD",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                  )
                )
              }
            }

            Surface(
              color = if (settings.fccConfig.enforceNonSimulatedHardware) NeonEmerald.copy(alpha = 0.15f) else AmberAlert.copy(alpha = 0.15f),
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = if (settings.fccConfig.enforceNonSimulatedHardware) "FCC CERTIFIED" else "BENCH TEST",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = if (settings.fccConfig.enforceNonSimulatedHardware) NeonEmerald else AmberAlert,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 9.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
        }
      }
    }

    // 2. Sub-Category Tabs
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        listOf(
          0 to "EMERGENCY & GPS",
          1 to "FCC REGULATION",
          2 to "HIPAA & HOSPITALS",
          3 to "PRIVACY POLICY"
        ).forEach { (index, title) ->
          val isSelected = activeSubTab == index
          Button(
            onClick = { activeSubTab = index },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isSelected) NeonCyan else SurfaceDark,
              contentColor = if (isSelected) ObsidianBlack else TextSecondary
            ),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier
              .weight(1f)
              .height(34.dp)
              .testTag("settings_tab_$index")
          ) {
            Text(
              text = title,
              fontSize = 8.5.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
              maxLines = 1
            )
          }
        }
      }
    }

    // TAB 0: LOCATION & EMERGENCY CONTACT SERVICES
    if (activeSubTab == 0) {
      item {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder))),
          modifier = Modifier.fillMaxWidth().testTag("location_settings_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "GEOGRAPHIC LOCATION TRACKING",
                  style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                )
              }
              Switch(
                checked = settings.locationSettings.gpsLocationEnabled,
                onCheckedChange = { onToggleGpsLocation(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier.testTag("gps_switch")
              )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Live GPS coordinates broadcast to 911 EMS Computer-Aided Dispatch (CAD) to pinpoint jammed or endangered patients carrying biomedical implants.",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 10.sp)
            )

            if (settings.locationSettings.gpsLocationEnabled) {
              Spacer(modifier = Modifier.height(10.dp))
              Surface(
                color = ObsidianBlack,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("CURRENT LATITUDE/LONGITUDE:", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace))
                    Text("ACCURACY: ±${settings.locationSettings.locationAccuracyMeters}m", style = MaterialTheme.typography.labelSmall.copy(color = NeonEmerald, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace))
                  }
                  Text(
                    text = "${settings.locationSettings.lastKnownLatitude}° N, ${settings.locationSettings.lastKnownLongitude.let { kotlin.math.abs(it) }}° W",
                    style = MaterialTheme.typography.bodyMedium.copy(color = NeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "GEOFENCE ZONE: ${settings.locationSettings.hospitalGeofenceZone}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Emergency 911 Dispatch Location Sharing",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                )
                Checkbox(
                  checked = settings.locationSettings.emergencyLocationSharingActive,
                  onCheckedChange = { onToggleEmergencySharing(it) },
                  colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                )
              }
            }
          }
        }
      }

      // Emergency Contacts & First Responders
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "DESIGNATED EMERGENCY SERVICES",
            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
          )
          TextButton(
            onClick = { showAddContactDialog = true },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ADD AGENCY", color = NeonCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
          }
        }
      }

      items(settings.emergencyContacts) { contact ->
        Card(
          shape = RoundedCornerShape(8.dp),
          colors = CardDefaults.cardColors(containerColor = CardSurface),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(if (contact.isPrimary) listOf(LaserCrimson, LaserCrimson.copy(alpha = 0.5f)) else listOf(CardBorder, CardBorder))
          ),
          modifier = Modifier.fillMaxWidth().testTag("emergency_contact_${contact.id}")
        ) {
          Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = contact.serviceName,
                  style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                )
                if (contact.isPrimary) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(color = LaserCrimson.copy(alpha = 0.2f), shape = RoundedCornerShape(3.dp)) {
                    Text(
                      text = "PRIMARY CAD",
                      style = MaterialTheme.typography.labelSmall.copy(color = LaserCrimson, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "${contact.category} • ${contact.phoneNumber}",
                style = MaterialTheme.typography.bodySmall.copy(color = NeonCyan, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
              )
              Text(
                text = "RADIO / DISPATCH: ${contact.frequencyOrRadio}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
              )
              contact.hospitalAffiliation?.let { aff ->
                Text(
                  text = "AFFILIATION: $aff",
                  style = MaterialTheme.typography.bodySmall.copy(color = AmberAlert, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              // Call Button
              IconButton(
                onClick = {
                  val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                  try {
                    context.startActivity(intent)
                  } catch (e: Exception) {
                    Toast.makeText(context, "Dialer unavailable for ${contact.phoneNumber}", Toast.LENGTH_SHORT).show()
                  }
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.Phone, contentDescription = "Call", tint = NeonEmerald, modifier = Modifier.size(16.dp))
              }

              // Copy button
              IconButton(
                onClick = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  clipboard.setPrimaryClip(ClipData.newPlainText("Contact", "${contact.serviceName} - ${contact.phoneNumber}"))
                  Toast.makeText(context, "Copied ${contact.phoneNumber} to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(16.dp))
              }

              if (!contact.isPrimary) {
                IconButton(
                  onClick = { onRemoveEmergencyContact(contact.id) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    }

    // TAB 1: FCC REGULATION & HARDWARE CERTIFICATION
    if (activeSubTab == 1) {
      item {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonEmerald, NeonEmerald.copy(alpha = 0.4f)))),
          modifier = Modifier.fillMaxWidth().testTag("fcc_regulation_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "FCC REGULATORY COMPLIANCE",
                    style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.5.sp)
                  )
                  Text(
                    text = "Title 47 CFR Part 15, Part 90, & Part 95 Subpart I",
                    style = MaterialTheme.typography.bodySmall.copy(color = NeonEmerald, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                  )
                }
              }

              Surface(color = NeonEmerald.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                Text(
                  text = settings.fccConfig.certifiedOperatorCallsign,
                  style = MaterialTheme.typography.labelSmall.copy(color = NeonEmerald, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 8.5.sp),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "This application operates strictly under FCC telecommunications guidelines to prevent harmful radio-frequency interference (RFI) to society, aeronautical nav-aids, and public safety infrastructure.",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 9.5.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Non-simulated hardware switch
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Enforce Real Hardware (Non-Simulated Mode)",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                )
                Text(
                  text = "Disallows artificial signal mocks; binds pipeline strictly to live physical SDR receiver bridges.",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 8.5.sp)
                )
              }
              Switch(
                checked = settings.fccConfig.enforceNonSimulatedHardware,
                onCheckedChange = {
                  onUpdateFccEnforcement(it, settings.fccConfig.strictInterferenceShielding, settings.fccConfig.autoReportRogueTransmitters)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonEmerald, checkedTrackColor = NeonEmerald.copy(alpha = 0.5f))
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Strict interference shielding
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Part 15 Strict Interference Shielding",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                )
                Text(
                  text = "Automatically attenuates RF input if broadband noise violates Part 15 radiated emission limits.",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 8.5.sp)
                )
              }
              Switch(
                checked = settings.fccConfig.strictInterferenceShielding,
                onCheckedChange = {
                  onUpdateFccEnforcement(settings.fccConfig.enforceNonSimulatedHardware, it, settings.fccConfig.autoReportRogueTransmitters)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonEmerald, checkedTrackColor = NeonEmerald.copy(alpha = 0.5f))
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Auto-report rogue transmitters
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "FCC Homeland Bureau Incident Logging",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                )
                Text(
                  text = "Exports timestamped SigMF and IQ snapshots for investigation by FCC field enforcement teams.",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 8.5.sp)
                )
              }
              Switch(
                checked = settings.fccConfig.autoReportRogueTransmitters,
                onCheckedChange = {
                  onUpdateFccEnforcement(settings.fccConfig.enforceNonSimulatedHardware, settings.fccConfig.strictInterferenceShielding, it)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonEmerald, checkedTrackColor = NeonEmerald.copy(alpha = 0.5f))
              )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              color = ObsidianBlack,
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(
                  text = "CERTIFIED RULE PARTS:",
                  style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                )
                settings.fccConfig.fccRuleParts.forEach { part ->
                  Text("• $part", style = MaterialTheme.typography.bodySmall.copy(color = NeonCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace))
                }
              }
            }
          }
        }
      }
    }

    // TAB 2: HIPAA RECORDS & BIOMEDICAL DEVICE AUTHORIZATION
    if (activeSubTab == 2) {
      item {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(OpticPurple, OpticPurple.copy(alpha = 0.4f)))),
          modifier = Modifier.fillMaxWidth().testTag("hipaa_governance_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = OpticPurple, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "HIPAA DATA GOVERNANCE & BAA",
                    style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.5.sp)
                  )
                  Text(
                    text = "Hospital & Biomedical OEM Permitted Viewing",
                    style = MaterialTheme.typography.bodySmall.copy(color = OpticPurple, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                  )
                }
              }

              Surface(
                color = if (settings.hipaaMaskingEnabled) AmberAlert.copy(alpha = 0.2f) else NeonEmerald.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
              ) {
                Text(
                  text = if (settings.hipaaMaskingEnabled) "NAMES MASKED" else "BAA VERIFIED",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = if (settings.hipaaMaskingEnabled) AmberAlert else NeonEmerald,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.5.sp
                  ),
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Per 45 CFR § 164.502 (HIPAA Privacy & Security Rule), patient identities, PHI records, and physiological telemetry may ONLY be shared or alerted if the corresponding Hospital or Biomedical Device Manufacturer explicitly signs a Business Associate Agreement (BAA) and permits third-party viewing.",
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 9.5.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // HIPAA Privacy Masking Switch
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Enforce Anonymized PHI Masking",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                )
                Text(
                  text = "Redacts patient legal names into anonymous hashes unless verified hospital authorization is granted.",
                  style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 8.5.sp)
                )
              }
              Switch(
                checked = settings.hipaaMaskingEnabled,
                onCheckedChange = { onToggleHipaaMasking(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = OpticPurple, checkedTrackColor = OpticPurple.copy(alpha = 0.5f))
              )
            }
          }
        }
      }

      item {
        Text(
          text = "HOSPITAL & BIOMEDICAL OEM ACCESS CONTRACTS",
          style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
        )
      }

      items(settings.hospitalAuthorizations) { hospital ->
        val isPermitted = hospital.thirdPartyViewingPermitted
        Card(
          shape = RoundedCornerShape(8.dp),
          colors = CardDefaults.cardColors(containerColor = CardSurface),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
              if (isPermitted) listOf(NeonEmerald, NeonEmerald.copy(alpha = 0.4f)) else listOf(AmberAlert, AmberAlert.copy(alpha = 0.4f))
            )
          ),
          modifier = Modifier.fillMaxWidth().testTag("hospital_auth_${hospital.hospitalId}")
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = hospital.hospitalName,
                  style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                )
                Text(
                  text = "OEM PARTNER: ${hospital.biomedicalPartner}",
                  style = MaterialTheme.typography.bodySmall.copy(color = NeonCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                )
                Text(
                  text = "BAA STATUS: ${hospital.hipaaBaaStatus}",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isPermitted) NeonEmerald else AmberAlert,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                )
              }

              Switch(
                checked = isPermitted,
                onCheckedChange = { onToggleHospitalAuth(hospital.hospitalId, it) },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonEmerald, checkedTrackColor = NeonEmerald.copy(alpha = 0.5f))
              )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              color = ObsidianBlack,
              shape = RoundedCornerShape(4.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(6.dp)) {
                Text(
                  text = "AUTHORIZED PATIENT TELEMETRY DEPLOYMENTS:",
                  style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                )
                Text(
                  text = hospital.authorizedPatientIds.joinToString(", "),
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isPermitted) TextPrimary else TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                  )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(
                    text = "Third-Party Viewing: ${if (isPermitted) "PERMITTED" else "DENIED"}",
                    style = MaterialTheme.typography.labelSmall.copy(color = if (isPermitted) NeonEmerald else LaserCrimson, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                  )
                  Text(
                    text = "RF Signal Blocking: ${if (hospital.remoteMitigationPermitted) "AUTHORIZED" else "STANDBY"}",
                    style = MaterialTheme.typography.labelSmall.copy(color = if (hospital.remoteMitigationPermitted) NeonCyan else TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                  )
                }
              }
            }
          }
        }
      }
    }

    // TAB 3: STRICT PRIVACY POLICY
    if (activeSubTab == 3) {
      item {
        Card(
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceDark),
          border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder))),
          modifier = Modifier.fillMaxWidth().testTag("privacy_policy_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Policy, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "STRICT PRIVACY POLICY & TERMS",
                style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.5.sp)
              )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = """
1. PURPOSE & NON-SIMULATED SCOPE
Unified Signal Shield is engineered as a non-simulated, physical SDR-grade radio spectrum and optical telemetry shield compliant with Title 47 of the Code of Federal Regulations (FCC) and 45 CFR § 164 (HIPAA). It serves exclusively to mitigate hostile jamming, detect laser sensor blinding, and protect human lives dependent on wireless biomedical apparatus.

2. PATIENT DATA & HIPAA COMPLIANCE
No patient legal names, personal medical histories, or protected health information (PHI) are collected, mined, or sold. Under our Zero-Knowledge Biomedical Protocol, patient identities are NEVER revealed to third parties unless:
(a) The treating Hospital or Medical Center executes a verified HIPAA Business Associate Agreement (BAA);
(b) The Biomedical Device OEM explicitly authorizes remote telemetry and alarm monitoring;
(c) Immediate life-threatening trauma or vital failure triggers an autonomous Computer-Aided Dispatch (CAD) alert to municipal 911/EMS first responders.

3. GEOLOCATION SERVICES
Location tracking (GPS coordinates, latitude, longitude) is collected strictly to establish hospital geofencing, isolate RF interference zones, and deliver emergency medical crews to the exact point of need during active vital crashes. Location data is stored only in local Room SQLite storage and never synchronized to external commercial ad networks.

4. RF SHIELDING & JAMMING MITIGATION
In full compliance with FCC regulations, the application does not emit offensive jammer broadcasts. Shielding actions consist solely of passive receiver attenuation, optical shutter physical closures, adaptive RF thresholding, and automated reporting to public safety authorities to minimize societal threats.

5. DATABASE RETENTION
All captured threat logs, modulation metrics, and biomedical vitals reside within an on-device encrypted Room SQLite database. The operator retains full rights to purge, export, or audit all persistent entries at any time without cloud retention.
              """.trimIndent(),
              style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 9.5.sp, lineHeight = 14.sp)
            )

            Spacer(modifier = Modifier.height(14.dp))
            Button(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Privacy Policy", "Unified Signal Shield HIPAA & FCC Privacy Policy 2026"))
                Toast.makeText(context, "Privacy Policy summary copied to clipboard", Toast.LENGTH_SHORT).show()
              },
              colors = ButtonDefaults.buttonColors(containerColor = CardBorder, contentColor = TextPrimary),
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("COPY PRIVACY POLICY TEXT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }

  // Dialog to add custom emergency contact
  if (showAddContactDialog) {
    var newServiceName by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("Hospital Emergency Dept") }
    var newPhone by remember { mutableStateOf("") }
    var newFreq by remember { mutableStateOf("VHF 155.340 MHz") }

    AlertDialog(
      onDismissRequest = { showAddContactDialog = false },
      title = {
        Text("Add Emergency Contact Agency", style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = newServiceName,
            onValueChange = { newServiceName = it },
            label = { Text("Agency / Hospital Name") },
            placeholder = { Text("e.g., Regional Trauma Emergency") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newPhone,
            onValueChange = { newPhone = it },
            label = { Text("Telephone Number") },
            placeholder = { Text("e.g., (800) 555-0100") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newFreq,
            onValueChange = { newFreq = it },
            label = { Text("Radio Frequency / CAD Protocol") },
            placeholder = { Text("e.g., MED-9 462.950 MHz") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newServiceName.isNotBlank() && newPhone.isNotBlank()) {
              onAddEmergencyContact(
                EmergencyContact(
                  id = "EMERG-${System.currentTimeMillis() % 10000}",
                  serviceName = newServiceName,
                  category = newCategory,
                  phoneNumber = newPhone,
                  frequencyOrRadio = newFreq
                )
              )
              showAddContactDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack)
        ) {
          Text("SAVE CONTACT")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddContactDialog = false }) {
          Text("CANCEL")
        }
      },
      containerColor = SurfaceDark
    )
  }
}
