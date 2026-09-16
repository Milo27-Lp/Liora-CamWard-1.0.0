package com.aistudio.lioracamward.detection

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.aistudio.lioracamward.data.model.Finding
import com.aistudio.lioracamward.data.model.ScanModule
import com.aistudio.lioracamward.data.model.Severity
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkDetector(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    fun scanNetwork(onFinding: (Finding) -> Unit) {
        val network = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(network)

        if (caps == null) {
            onFinding(
                Finding(
                    module = ScanModule.NETWORK,
                    severity = Severity.INFO,
                    title = "No Active Network Connection",
                    detail = "Device is offline. Local wireless IP cameras require an active network or WiFi direct pairing to stream footage.",
                    evidence = mapOf("networkState" to "offline")
                )
            )
            return
        }

        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        if (isWifi) {
            val localIp = getLocalIpAddress() ?: "192.168.1.x"
            val wifiInfo = wifiManager?.connectionInfo
            val ssid = wifiInfo?.ssid?.replace("\"", "") ?: "Local WiFi"

            onFinding(
                Finding(
                    module = ScanModule.NETWORK,
                    severity = Severity.INFO,
                    title = "Connected to WiFi Network ($ssid)",
                    detail = "You are on a shared local WiFi subnet ($localIp). In shared lodgings (Airbnbs, hotels), smart home cameras and hidden DVRs share this IP range. Check router client lists if accessible.",
                    evidence = mapOf(
                        "connectionType" to "WiFi",
                        "ssid" to ssid,
                        "localIp" to localIp
                    )
                )
            )
        } else if (isCellular) {
            onFinding(
                Finding(
                    module = ScanModule.NETWORK,
                    severity = Severity.INFO,
                    title = "Connected via Cellular Data",
                    detail = "You are on cellular carrier network. Local IP surveillance devices cannot be reached across cell towers unless they broadcast open BLE beacons or rely on cloud P2P servers.",
                    evidence = mapOf("connectionType" to "Cellular")
                )
            )
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }
}
