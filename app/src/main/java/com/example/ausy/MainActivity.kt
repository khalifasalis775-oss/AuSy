package com.example.ausy

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val discoveredDevices = mutableStateListOf<Pair<String, Int>>()
    private var syncTriggeredDevice = mutableStateOf<String?>(null)
    private var isScanning = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.all { it }) {
                startBleScan()
            }
        }

        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        setContent {
            AuSyDashboard(
                devices = discoveredDevices,
                activeSyncDevice = syncTriggeredDevice.value,
                isScanning = isScanning.value,
                onToggleScan = { active ->
                    if (active) startBleScan() else stopBleScan()
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        discoveredDevices.clear()
        isScanning.value = true

        scanner.startScan(object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val deviceName = result.device.name ?: result.device.address ?: "Unknown Device"
                val rssi = result.rssi

                val existingIndex = discoveredDevices.indexOfFirst { it.first == deviceName }
                if (existingIndex >= 0) {
                    discoveredDevices[existingIndex] = Pair(deviceName, rssi)
                } else {
                    discoveredDevices.add(Pair(deviceName, rssi))
                }

                if (rssi > -60) {
                    syncTriggeredDevice.value = deviceName
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        scanner?.stopScan(object : ScanCallback() {})
        isScanning.value = false
    }
}

@Composable
fun AuSyDashboard(
    devices: List<Pair<String, Int>>,
    activeSyncDevice: String?,
    isScanning: Boolean,
    onToggleScan: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "AuSy",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3DD5F3)
            )
            Text(
                text = "Proximity Auto-Sync Engine",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            activeSyncDevice?.let { device ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00C853)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚡ AuSy Auto-Sync Activated!",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Connected to $device (Within 3 feet)",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isScanning) "Radar Active" else "Radar Paused",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isScanning) "Searching for nearby devices..." else "Tap switch to enable",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isScanning,
                        onCheckedChange = { onToggleScan(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Discovered Devices (${devices.size})",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn {
                items(devices) { (name, rssi) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = name, color = Color.White, fontWeight = FontWeight.Medium)
                            Text(
                                text = "$rssi dBm",
                                color = if (rssi > -60) Color(0xFF00C853) else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
