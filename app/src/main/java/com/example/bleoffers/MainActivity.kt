package com.example.bleoffers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_READ_NOT_PERMITTED
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothProfile.STATE_CONNECTED
import android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bleoffers.databinding.ActivityMainBinding
import com.example.bleoffers.utils.PermissionRequester
import com.example.bleoffers.utils.hasRequiredRuntimePermissions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.UUID
import kotlin.system.exitProcess

private const val RUNTIME_PERMISSION_REQUEST_CODE = 2
private const val ESP32S_SERVICE_UUID = "83ebe3dc-c6cb-48e7-96a0-af96b78aa8e3"
private const val OFFERS_CHARACTERISTIC_UUID = "f263c242-b2dc-4b84-b9d0-d2e6ccee3072"
private const val GATT_MAX_MTU_SIZE = 517

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var bleDeviceAdapter: LeDeviceListAdapter
    private var blScanResults = mutableListOf<ScanResult>()
    private lateinit var currentDevice: ScanResult
    private var bluetoothGatt : BluetoothGatt? = null

    private var isBLEnabled = false
    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread { binding.btnScan.text =
                if (value) getString(R.string.btn_stop_scan)
                else getString(R.string.btn_start_scan)
            }
        }

    // Obtiene una instancia del Adaptador Bluetooth físico
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // Objeto scaner que se inicializa solo cuando se requiere
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    // Define las caracteristicas del escaneo
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    // Cacha el intent para habilitar el Bluetooth
    private val enableBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this,"Bluetooth ON", Toast.LENGTH_LONG).show()
            binding.btnEnableDisable.text = getString(R.string.btn_turn_off_bl)
            isBLEnabled = true
        } else {
            isBLEnabled = false
            binding.btnEnableDisable.text = getString(R.string.btn_turn_on_bl)
            showPermissionsSnack()
        }
    }

    // Checa si el Bluetooth está habilitado
    private fun showEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetooth.launch(enableBtIntent)
        }
    }

    // Checa si el Bluetooth está habilitado
    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            showEnableBluetooth()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListeners()
        initListAdapter()
    }

    private fun initListeners() = with(binding) {
        btnScan.setOnClickListener {
            if (isScanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initListAdapter() {
        bleDeviceAdapter = LeDeviceListAdapter(LeDeviceListAdapter.OnClickListener { deviceSelected ->
            currentDevice = deviceSelected
            Toast.makeText(this, deviceSelected.device.name, Toast.LENGTH_SHORT).show()
            if (isScanning) {
                stopBleScan()
            }
            with(deviceSelected.device) {
                Log.w("ScanResultAdapter", "Connecting to $address")
                connectGatt(this@MainActivity, false, gattCallback)
            }
        })

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = bleDeviceAdapter
        }
    }

    // Pregunta si se tienen los permisos necesarios, en caso de tenerlos se inicia el escaneo
    // de dispositivos cercanos filtrando por servicio: ENVIRONMENTAL_SERVICE_UUID
    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        if (!hasRequiredRuntimePermissions()) {
            requestRelevantRuntimePermissions()
        } else {
            blScanResults.clear()
            bleDeviceAdapter.notifyDataSetChanged()

            val filter = listOf<ScanFilter>(
                ScanFilter.Builder().setServiceUuid(
                    ParcelUuid.fromString(ESP32S_SERVICE_UUID)
                ).build()
            )
            bleScanner.startScan(filter, scanSettings, scanCallback)
            isScanning = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
    }

    // La devolución de llamada para cuando encuentre dispositivos BLE cercanos y si es un nuevo
    // dispositivo se agrega al recycler view
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = blScanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // El scan result ya existe en la lista, se actualiza
                blScanResults[indexQuery] = result
                bleDeviceAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    if (!blScanResults.contains(result)){
                        Log.i("ScanCallback", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                        blScanResults.add(result)
                    }
                }
            }
            bleDeviceAdapter.devicesList(blScanResults)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }
    }

    // Callback para cuando se intenta conectar al servidor Gatt de un dispositivo
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == GATT_SUCCESS && newState == STATE_CONNECTED) {
                Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")

                bluetoothGatt = gatt
                runOnUiThread {
                    bluetoothGatt?.discoverServices()
                    bluetoothGatt?.requestMtu(GATT_MAX_MTU_SIZE)
                }
            } else if (newState == STATE_DISCONNECTED) {
                Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                gatt.close()
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
                gatt.printGattTable()

                readCharacteristic()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.w("ATT MTU", "ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}")
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    GATT_SUCCESS -> {
                        Log.i("BluetoothGattCallback", "Read characteristic $uuid:\n${String(value)}")
                        bluetoothGatt?.close()
                    }
                    GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun readCharacteristic() {
        val esp32sServiceUUID = UUID.fromString(ESP32S_SERVICE_UUID)
        val offerUUID = UUID.fromString(OFFERS_CHARACTERISTIC_UUID)
        val offerChar = bluetoothGatt?.getService(esp32sServiceUUID)?.getCharacteristic(offerUUID)
        if (offerChar?.isReadable() == true) {
            bluetoothGatt?.readCharacteristic(offerChar)
        }
    }

    private fun BluetoothGatt.printGattTable() {
        if (services.isEmpty()) {
            Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?")
            return
        }
        services.forEach { service ->
            val characteristicsTable = service.characteristics.joinToString(
                separator = "\n|--",
                prefix = "|--"
            ) { it.uuid.toString() }
            Log.i("printGattTable", "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
            )
        }
    }

    fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isWritable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    // Funcion de extensión para convertir de byteArray a String con formato hexadecimal
    fun ByteArray.toHexString(): String =
        joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }

    private fun Activity.requestRelevantRuntimePermissions() {
        if (hasRequiredRuntimePermissions()) { return }
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                requestLocationPermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                requestBluetoothPermissions()
            }
        }
    }

    private fun requestLocationPermission() {
        runOnUiThread {
            //showAlertDialog(R.string.location_permissions_msg, R.string.location_permissions_msg)
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.location_permissions_title))
                .setMessage(getString(R.string.location_permissions_msg))
                .setPositiveButton(getString(R.string.ok_btn)) { dialog, which ->
                    fineLocation.runWithPermission {
                        startBleScan()
                    }
                }
                .show()
        }
    }

    private fun requestBluetoothPermissions() {
        runOnUiThread {
            //showAlertDialog(R.string.location_permissions_msg, R.string.location_permissions_msg)
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.bluetooth_permissions_title))
                .setMessage(getString(R.string.bluetooth_permissions_msg))
                .setPositiveButton(getString(R.string.ok_btn)) { dialog, which ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ),
                        RUNTIME_PERMISSION_REQUEST_CODE
                    )
                }
                .show()
        }
    }

    private val fineLocation = PermissionRequester(this,
        Manifest.permission.ACCESS_FINE_LOCATION,
        onDenied = { alertPermissions() },
        onShowRationale = { alertPermissions() }
    )

    private fun alertPermissions() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.no_permissions_title))
            .setMessage(getString(R.string.no_permissions_msg))
            .setPositiveButton(getString(R.string.exit_btn)) { dialog, which ->
                // TODO: Desactivar el adaptador Bluetooth
                finish()
                exitProcess(0)
            }
            .show()
    }

    // Tipo callback para verificar si los permisos fueron aceptados, en caso de exito
    // se llama a startBleScan()
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RUNTIME_PERMISSION_REQUEST_CODE -> {
                val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
                    it.second == PackageManager.PERMISSION_DENIED &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(this, it.first)
                }
                val containsDenial = grantResults.any { it == PackageManager.PERMISSION_DENIED }
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                when {
                    containsPermanentDenial -> {
                        // TODO: Handle permanent denial (e.g., show AlertDialog with justification)
                        // Note: The user will need to navigate to App Settings and manually grant
                        // permissions that were permanently denied
                    }
                    containsDenial -> {
                        requestRelevantRuntimePermissions()
                    }
                    allGranted && hasRequiredRuntimePermissions() -> {
                        startBleScan()
                    }
                    else -> {
                        // Unexpected scenario encountered when handling permissions
                        recreate()
                    }
                }
            }
        }
    }

    private fun showPermissionsSnack() {
        Snackbar.make(binding.root,
            getString(R.string.no_permissions_msg),
            Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.ok_btn)) { }
            .show()
        /*MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.no_permissions_title))
            .setMessage(getString(R.string.no_permissions_msg))
            .setPositiveButton(getString(R.string.try_agrain_btn)) { dialog, which ->
                showEnableBluetooth()
            }
            .setNegativeButton(getString(R.string.exit_btn)) { dialog, which ->
                finish()
                exitProcess(0)
            }
            .show()*/
    }


    private fun showAlertDialog (title: Int, message: Int) {

    }

}