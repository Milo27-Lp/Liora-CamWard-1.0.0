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

    private var isScanning = false
    private var onFindingListener: ((Finding) -> Unit)? = null
    private val seenAddresses = mutableSetOf<String>()

    companion object {
        private val WHOLE_WORD_KEYWORDS = listOf(
            "cam", "dvr", "nvr", "cctv", "escam", "icam", "xmeye", "icsee", "yoosee",
            "wyze", "dahua", "reolink", "foscam", "eufy", "annke", "amcrest", "lorex",
            "tiandy", "milesight", "uniview", "kedacom", "hikvision", "zosi", "hanbang",
            "wanscam", "sricam", "vstarcam", "v380"
        )

        private val SUBSTRING_KEYWORDS = listOf(
            "camera", "ipcam", "ipcamera", "ip cam", "webcam", "spycam", "spy cam",
            "nannycam", "nanny cam", "babycam", "baby cam", "bodycam", "dashcam",
            "eufycam", "anker cam", "hik-connect", "yi cam", "yicam", "v380pro",
            "p2pcam", "p2p cam", "ctronics", "swann cam", "mini cam", "minicam",
            "clock cam", "pen cam", "pinhole"
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
                        severity = Severity.SUSPICIOUS,
                        title = "Suspicious BLE Device: \"$deviceName\"",
                        detail = "Broadcasting Bluetooth beacon matches known surveillance / IP camera OEM firmware pattern (\"$signatureMatch\") at RSSI $rssi dBm.",
                        evidence = mapOf(
                            "deviceName" to deviceName,
                            "macAddress" to address,
                            "matchedKeyword" to (signatureMatch ?: "unknown"),
                            "rssi" to "$rssi dBm"
                        )
                    )
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            // Scan failed or permission denied
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(onFinding: (Finding) -> Unit): Boolean {
        if (!isSupported || !isEnabled || isScanning) return false
        onFindingListener = onFinding
        seenAddresses.clear()
        _devices.value = emptyList()

        return try {
            val scanner = bluetoothAdapter?.bluetoothLeScanner
            if (scanner != null) {
                scanner.startScan(scanCallback)
                isScanning = true
                true
            } else {
                false
            }
        } catch (e: SecurityException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        isScanning = false
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            // Ignore on teardown
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
