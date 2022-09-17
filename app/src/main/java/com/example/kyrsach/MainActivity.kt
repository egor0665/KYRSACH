package com.example.kyrsach

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.AlphabeticIndex
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var connectButton: View
    private lateinit var getButton: View
    private lateinit var setButton: View
    private lateinit var deviceRepository: DeviceRepository
    private var permissionGranted = false
    private var devices: List<Device>? = emptyList()
    private var address = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectButton = findViewById<Button>(R.id.connectButton)
        getButton = findViewById<Button>(R.id.getTimeButton)
        setButton = findViewById<Button>(R.id.setTimeButton)
        deviceRepository =  DeviceRepository(this)
        getButton.isEnabled = false
        setButton.isEnabled = false
        getButton.setOnClickListener {

        }
        setButton.setOnClickListener {
            deviceRepository.sendData(1224) //16 число
        }
        connectButton.setOnClickListener{
            connect(address)
        }
        findDevices()
        deviceRepository.startObserve().observe(this){

        }
        getLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        if (!permissionGranted)
            getLocationPermission()
    }
    private fun getLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            checkSelfPermission()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkSelfPermission() {
        val accessFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH
        )
        if (accessFineLocation != PackageManager.PERMISSION_GRANTED)
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH
                )
            )
    }
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissionGranted = true
            permissions.entries.forEach {
                if (!it.value) permissionGranted = false
            }
            if (!permissionGranted) {
                startDeniedPermissionAlert()
            }
        }
    private fun startDeniedPermissionAlert() {
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this).apply {
            setTitle("Permission needed")
            setMessage("Please, Allow all the time location permission for app to work")
            setPositiveButton("Open Setting") { _, _ ->
                val uri: Uri = Uri.fromParts("package", packageName, null)
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = uri
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                }
                startActivity(intent)
            }
            setNegativeButton("Quit") { _, _ ->
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
            }
        }

        val dialog: AlertDialog = alertDialogBuilder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()

        val mPositiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        mPositiveButton.setOnClickListener {
            dialog.cancel()
            val uri: Uri = Uri.fromParts("package", packageName, null)
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = uri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            }
            startActivity(intent)
        }
    }

    private fun connect(address: String): Boolean {
        return deviceRepository.connect(address)
    }

    private fun findDevices() {
        devices = deviceRepository.findDevices()
        if (!devices.isNullOrEmpty())
            address = devices!![0].address
        getButton.isEnabled = true
        setButton.isEnabled = true
        findViewById<TextView>(R.id.dateTime).text = address
    }

}

data class Data(val id: Int, val info: String?, val date: String?)

data class TempData(var id: Int?, var date: Int?, var info: Int?)

data class Device (val name: String, val address: String)

data class Drawing(val mode: Int, val position: Int, val color: Int)