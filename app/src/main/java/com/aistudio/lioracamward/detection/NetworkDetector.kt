package com.aistudio.lioracamward.detection

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.aistudio.lioracamward.data.model.Finding
import com.aistudio.lioracamward.data.model.NetworkDeviceObservation
import com.aistudio.lioracamward.data.model.ScanModule
import com.aistudio.lioracamward.data.model.Severity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

class NetworkDetector(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _devices = MutableStateFlow<List<NetworkDeviceObservation>>(emptyList())
    val devices: StateFlow<List<NetworkDeviceObservation>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var scanJob: Job? = null

    companion object {
        // High-confidence surveillance camera & streaming ports
        val SURVEILLANCE_PORTS = listOf(
            554 to "RTSP Streaming Video",
            8000 to "Hikvision Surveillance Control",
            37777 to "Dahua DVR/NVR Service",
            8899 to "ONVIF Camera WS-Discovery",
            80 to "HTTP Web / Camera Interface",
            8080 to "Alternative HTTP Camera Stream"
        )
    }

    fun startNetworkScan(scope: CoroutineScope, onFinding: (Finding) -> Unit) {
        if (_isScanning.value) return
        _isScanning.value = true
        _devices.value = emptyList()

        scanJob = scope.launch(Dispatchers.IO) {
            val network = connectivityManager?.activeNetwork
            val caps = connectivityManager?.getNetworkCapabilities(network)

            if (caps == null) {
                _isScanning.value = false
                withContext(Dispatchers.Main) {
                    onFinding(
                        Finding(
                            module = ScanModule.NETWORK,
                            severity = Severity.INFO,
                            title = "No Active Network Connection",
                            detail = "Device is offline. Local wireless IP cameras require an active network or WiFi direct pairing to stream footage.",
                            evidence = mapOf("networkState" to "offline")
                        )
                    )
                }
                return@launch
            }

            val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isEthernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

            val localIp = getLocalIpAddress()

            if (isWifi || isEthernet) {
                val wifiInfo = wifiManager?.connectionInfo
                val ssid = if (isWifi) {
                    wifiInfo?.ssid?.replace("\"", "")?.takeIf { it != "<unknown ssid>" } ?: "Local Wi-Fi"
                } else {
                    "Ethernet LAN"
                }

                withContext(Dispatchers.Main) {
                    onFinding(
                        Finding(
                            module = ScanModule.NETWORK,
                            severity = Severity.INFO,
                            title = "LAN Connected: $ssid",
                            detail = "Subnet host audit initiated at $localIp. Scanning active hosts for exposed RTSP (554), ONVIF (8899), and CCTV server ports.",
                            evidence = mapOf(
                                "connectionType" to if (isWifi) "WiFi" else "Ethernet",
                                "ssid" to ssid,
                                "localIp" to (localIp ?: "Unknown")
                            )
                        )
                    )
                }

                // Subnet scan
                if (localIp != null && localIp.contains(".")) {
                    val subnetPrefix = localIp.substringBeforeLast(".") + "."
                    val hostNum = localIp.substringAfterLast(".").toIntOrNull() ?: 1

                    // Target key IP addresses: Gateway, common camera IPs, and neighboring pool
                    val targetIps = mutableSetOf<String>()
                    targetIps.add("${subnetPrefix}1")   // Gateway
                    targetIps.add("${subnetPrefix}2")   // Common second device
                    targetIps.add("${subnetPrefix}100") // Common DHCP start
                    targetIps.add("${subnetPrefix}200") // Common static IP for cameras

                    // Nearby neighbor IP addresses
                    val startRange = (hostNum - 10).coerceAtLeast(1)
                    val endRange = (hostNum + 10).coerceAtMost(254)
                    for (i in startRange..endRange) {
                        targetIps.add("$subnetPrefix$i")
                    }

                    val foundDevices = mutableListOf<NetworkDeviceObservation>()

                    // Scan in chunks of 8 concurrent socket probes to prevent resource starvation
                    val chunks = targetIps.toList().chunked(8)
                    for (chunk in chunks) {
                        val deferreds = chunk.map { ip ->
                            async {
                                probeHost(ip)
                            }
                        }
                        val results = deferreds.awaitAll()
                        for (obs in results) {
                            if (obs != null) {
                                foundDevices.add(obs)
                                _devices.value = foundDevices.toList()

                                if (obs.isSuspicious) {
                                    withContext(Dispatchers.Main) {
                                        onFinding(
                                            Finding(
                                                module = ScanModule.NETWORK,
                                                severity = Severity.HIGH,
                                                title = "IP Camera / Surveillance Port: ${obs.ip}",
                                                detail = "Active surveillance streaming port detected (${obs.openPorts.joinToString()}). Hardware matches: ${obs.serviceHint ?: "IP Surveillance Stream"}.",
                                                evidence = mapOf(
                                                    "ip" to obs.ip,
                                                    "openPorts" to obs.openPorts.joinToString(),
                                                    "service" to (obs.serviceHint ?: "Video Stream")
                                                )
                                            )
                                        )
                                    }
                                } else if (obs.openPorts.isNotEmpty() && obs.ip != localIp) {
                                    withContext(Dispatchers.Main) {
                                        onFinding(
                                            Finding(
                                                module = ScanModule.NETWORK,
                                                severity = Severity.INFO,
                                                title = "Active Host on Subnet: ${obs.ip}",
                                                detail = "Responding device detected on LAN with open service ports (${obs.openPorts.joinToString()}).",
                                                evidence = mapOf(
                                                    "ip" to obs.ip,
                                                    "ports" to obs.openPorts.joinToString()
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        onFinding(
                            Finding(
                                module = ScanModule.NETWORK,
                                severity = Severity.INFO,
                                title = "Subnet Reconnaissance Completed",
                                detail = "Probed ${targetIps.size} addresses on ${subnetPrefix}0/24. Found ${foundDevices.size} active network hosts.",
                                evidence = mapOf(
                                    "hostsProbed" to targetIps.size.toString(),
                                    "activeFound" to foundDevices.size.toString()
                                )
                            )
                        )
                    }
                }
            } else if (isCellular) {
                withContext(Dispatchers.Main) {
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

            _isScanning.value = false
        }
    }

    fun stop() {
        scanJob?.cancel()
        _isScanning.value = false
    }

    private fun probeHost(ip: String): NetworkDeviceObservation? {
        val openPorts = mutableListOf<Int>()
        var isSuspicious = false
        var serviceHint: String? = null

        for ((port, description) in SURVEILLANCE_PORTS) {
            val isOpen = checkPort(ip, port, timeoutMs = 250)
            if (isOpen) {
                openPorts.add(port)
                if (port == 554 || port == 8000 || port == 37777 || port == 8899) {
                    isSuspicious = true
                    serviceHint = description
                } else if (serviceHint == null) {
                    serviceHint = description
                }
            }
        }

        return if (openPorts.isNotEmpty()) {
            NetworkDeviceObservation(
                ip = ip,
                hostname = null,
                openPorts = openPorts,
                isSuspicious = isSuspicious,
                serviceHint = serviceHint
            )
        } else {
            null
        }
    }

    private fun checkPort(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
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
