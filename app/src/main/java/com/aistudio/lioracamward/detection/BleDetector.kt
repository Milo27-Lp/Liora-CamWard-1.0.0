package com.aistudio.lioracamward.detection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.aistudio.lioracamward.data.model.BleDeviceObservation
import com.aistudio.lioracamward.data.model.Finding
import com.aistudio.lioracamward.data.model.ScanModule
import com.aistudio.lioracamward.data.model.Severity
import com.aistudio.lioracamward.ui.components.RadarBlip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.regex.Pattern

class BleDetector(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _devices = MutableStateFlow<List<BleDeviceObservation>>(emptyList())
    val devices: StateFlow<List<BleDeviceObservation>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var onFindingListener: ((Finding) -> Unit)? = null
    private val seenAddresses = mutableSetOf<String>()

    companion object {
        private val WHOLE_WORD_KEYWORDS = listOf(
            "cam", "dvr", "nvr", "cctv", "escam", "icam", "xmeye", "icsee", "yoosee",
            "wyze", "dahua", "reolink", "foscam", "eufy", "annke", "amcrest", "lorex",
            "tiandy", "milesight", "uniview", "kedacom", "hikvision", "zosi", "hanbang",
            "wanscam", "sricam", "vstarcam", "v380", "tuya", "smartlife"
        )

        private val SUBSTRING_KEYWORDS = listOf(
            "camera", "ipcam", "ipcamera", "ip cam", "webcam", "spycam", "spy cam",
            "nannycam", "nanny cam", "babycam", "baby cam", "bodycam", "dashcam",
            "eufycam", "anker cam", "hik-connect", "yi cam", "yicam", "v380pro",
            "p2pcam", "p2p cam", "ctronics", "swann cam", "mini cam", "minicam",
            "clock cam", "pen cam", "pinhole", "surveillance"
        )

        private val WHOLE_WORD_PATTERN = Pattern.compile(
            "\\b(${WHOLE_WORD_KEYWORDS.joinToString("|")})\\b",
            Pattern.CASE_INSENSITIVE
        )
    }

    val isSupported: Boolean
        get() = bluetoothAdapter != null

    val isEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val device = result.device ?: return
            val address = device.address ?: return
            val rawName = device.name ?: result.scanRecord?.deviceName

            val deviceName = if (!rawName.isNullOrBlank()) rawName.trim() else "BLE Peripheral ($address)"
            val rssi = result.rssi

            val signatureMatch = checkSignature(deviceName)
            val isSuspicious = signatureMatch != null

            val observation = BleDeviceObservation(
                name = deviceName,
                address = address,
                rssi = rssi,
                isSuspicious = isSuspicious,
                matchedSignature = signatureMatch
            )

            // Update observed devices list
            val currentList = _devices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == address }
            if (existingIndex >= 0) {
                currentList[existingIndex] = observation
            } else {
                currentList.add(0, observation)
            }
            _devices.value = currentList

            // Report finding once per address if suspicious
            if (isSuspicious && !seenAddresses.contains(address)) {
                seenAddresses.add(address)
                onFindingListener?.invoke(
                    Finding(
                        module = ScanModule.BLUETOOTH,
                        severity = Severity.HIGH,
                        title = "Surveillance BLE Beacon: \"$deviceName\"",
                        detail = "Broadcasting Bluetooth advertisement matches known surveillance / IP camera OEM firmware pattern (\"$signatureMatch\") at RSSI $rssi dBm.",
                        evidence = mapOf(
                            "deviceName" to deviceName,
                            "macAddress" to address,
                            "matchedKeyword" to (signatureMatch ?: "unknown"),
                            "rssi" to "$rssi dBm"
                        )
                    )
                )
            } else if (!seenAddresses.contains(address) && currentList.size <= 5) {
                // Report first few ambient devices for context
                seenAddresses.add(address)
                onFindingListener?.invoke(
                    Finding(
                        module = ScanModule.BLUETOOTH,
                        severity = Severity.INFO,
                        title = "BLE Peripheral Detected: $deviceName",
                        detail = "Local wireless beacon discovered (Signal: $rssi dBm). MAC: $address. Normal ambient device.",
                        evidence = mapOf(
                            "deviceName" to deviceName,
                            "macAddress" to address,
                            "rssi" to "$rssi dBm"
                        )
                    )
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(onFinding: (Finding) -> Unit): Boolean {
        onFindingListener = onFinding
        seenAddresses.clear()
        _devices.value = emptyList()

        if (!isSupported) {
            onFinding(
                Finding(
                    module = ScanModule.BLUETOOTH,
                    severity = Severity.INFO,
                    title = "Bluetooth Adapter Unavailable",
                    detail = "No Bluetooth LE controller detected on this environment. Optical glint, magnetometer, and network scans remain active.",
                    evidence = mapOf("bleSupported" to "false")
                )
            )
            return false
        }

        if (!isEnabled) {
            onFinding(
                Finding(
                    module = ScanModule.BLUETOOTH,
                    severity = Severity.INFO,
                    title = "Bluetooth is Powered Off",
                    detail = "Turn Bluetooth ON in Android settings to enable real-time detection of nearby wireless spy cameras and BLE beacons.",
                    evidence = mapOf("bleEnabled" to "false")
                )
            )
            return false
        }

        if (_isScanning.value) return true

        return try {
            val scanner = bluetoothAdapter?.bluetoothLeScanner
            if (scanner != null) {
                scanner.startScan(scanCallback)
                _isScanning.value = true
                true
            } else {
                false
            }
        } catch (e: SecurityException) {
            _isScanning.value = false
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        _isScanning.value = false
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            // Ignore on teardown
        }
    }

    fun getRadarBlips(): List<RadarBlip> {
        return _devices.value.map { dev ->
            val distance = ((-dev.rssi - 30f) / 65f).coerceIn(0.15f, 0.9f)
            val angle = (kotlin.math.abs(dev.address.hashCode()) % 360).toFloat()
            RadarBlip(
                id = dev.address,
                angleDegrees = angle,
                distanceRatio = distance,
                isAlert = dev.isSuspicious,
                label = dev.name
            )
        }
    }

    private fun checkSignature(name: String): String? {
        val wordMatcher = WHOLE_WORD_PATTERN.matcher(name)
        if (wordMatcher.find()) {
            return wordMatcher.group(1)?.lowercase(Locale.ROOT)
        }

        val lower = name.lowercase(Locale.ROOT)
        for (kw in SUBSTRING_KEYWORDS) {
            if (lower.contains(kw)) {
                return kw
            }
        }
        return null
    }
}
