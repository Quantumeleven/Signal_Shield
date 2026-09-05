package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

  /**
   * Detects if the app is executing within an Android Emulator or on a real physical device.
   */
  fun isRunningOnEmulator(): Boolean {
    return (Build.FINGERPRINT.startsWith("generic")
        || Build.FINGERPRINT.startsWith("unknown")
        || Build.MODEL.contains("google_sdk")
        || Build.MODEL.contains("Emulator")
        || Build.MODEL.contains("Android SDK built for x86")
        || Build.MANUFACTURER.contains("Genymotion")
        || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
        || "google_sdk" == Build.PRODUCT
        || Build.HARDWARE.contains("goldfish")
        || Build.HARDWARE.contains("ranchu"))
  }

  /**
   * Retrieves the physical device's current IPv4 address on the active Wi-Fi / LAN network.
   */
  fun getDeviceLocalIpAddress(context: Context? = null): String? {
    try {
      val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
      for (intf in interfaces) {
        // Look for wifi or ethernet interfaces (wlan0, eth0, etc.)
        if (intf.isUp && !intf.isLoopback) {
          val addrs = Collections.list(intf.inetAddresses)
          for (addr in addrs) {
            if (!addr.isLoopbackAddress && addr is Inet4Address) {
              val ip = addr.hostAddress
              if (ip != null && !ip.startsWith("127.") && !ip.startsWith("10.0.2.")) {
                return ip
              }
            }
          }
        }
      }
    } catch (_: Exception) {
    }

    // Fallback via WifiManager if available
    if (context != null) {
      try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
        if (ipInt != 0) {
          @Suppress("DEPRECATION")
          return Formatter.formatIpAddress(ipInt)
        }
      } catch (_: Exception) {
      }
    }

    return null
  }

  /**
   * Extracts the base subnet prefix from an IP address (e.g. "192.168.1." from "192.168.1.45").
   */
  fun getSubnetPrefix(ip: String?): String {
    if (ip == null || !ip.contains(".")) return "192.168.1."
    val parts = ip.split(".")
    return if (parts.size >= 3) {
      "${parts[0]}.${parts[1]}.${parts[2]}."
    } else {
      "192.168.1."
    }
  }
}
