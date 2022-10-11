package com.example.kyrsach

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var connectButton: Button
    private lateinit var getButton: Button
    private lateinit var setButton: Button
    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var dateTime: TextView
    private lateinit var deviceRepository: DeviceRepository
    private var permissionGranted = false
    private var devices: List<Device>? = emptyList()
    private var address = ""

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectButton = findViewById(R.id.connectButton)
        getButton = findViewById(R.id.getTimeButton)
        setButton = findViewById(R.id.setTimeButton)
        datePicker = findViewById(R.id.datePicker)
        timePicker = findViewById(R.id.timePicker)
        dateTime = findViewById(R.id.dateTime)
        deviceRepository =  DeviceRepository(this)
        getButton.isEnabled = false
        setButton.isEnabled = false
        getButton.setOnClickListener {
            deviceRepository.sendData(0xff)
        }
        setButton.setOnClickListener {
            deviceRepository.sendData(0)
            deviceRepository.sendData(timePicker.minute.div(10)*16+timePicker.minute %10)
            deviceRepository.sendData(timePicker.hour.div(10)*16+timePicker.hour %10)
            deviceRepository.sendData(datePicker.dayOfMonth.div(10)*16+datePicker.dayOfMonth %10)
            deviceRepository.sendData((datePicker.month+1).div(10)*16+(datePicker.month+1) %10)
            deviceRepository.sendData((datePicker.year % 100).div(10)*16+(datePicker.year % 100) %10)
            deviceRepository.sendData(datePicker.year.div(100).div(10)*16+datePicker.year.div(100) %10)


            val cal = Calendar.getInstance()
            cal[Calendar.DAY_OF_MONTH] = datePicker.dayOfMonth
            cal[Calendar.MONTH] = datePicker.month
            cal[Calendar.YEAR] = datePicker.year
            val simpledateformat = SimpleDateFormat("EEEE")
            val date = Date(cal.getTimeInMillis())
            var dayOfWeek = 0
            Log.d("day" ,simpledateformat.format(date))
            when( simpledateformat.format(date)){
                "понедельник" -> dayOfWeek = 1
                "вторник" -> dayOfWeek = 2
                "среда" -> dayOfWeek = 3
                "четверг" -> dayOfWeek = 4
                "пятница" -> dayOfWeek = 5
                "суббота" -> dayOfWeek = 6
                "воскресенье" -> dayOfWeek = 7
            }
            deviceRepository.sendData(dayOfWeek)

        }
        connectButton.setOnClickListener{
            GlobalScope.launch { deviceRepository.connect(address) }
        }
        findDevices()
        deviceRepository.startObserve().observe(this){
            var sec = (it.sec!!.toInt().div(16)*10+it.sec!!.toInt() % 16).toString()
            if (sec.length<2) sec = "0"+sec
            var min = (it.min!!.toInt().div(16)*10+it.min!!.toInt() % 16).toString()
            if (min.length<2) min = "0"+min
            var hr = (it.hr!!.toInt().div(16)*10+it.hr!!.toInt() % 16).toString()
            if (hr.length<2) hr = "0"+hr
            var day = (it.day!!.toInt().div(16)*10+it.day!!.toInt() % 16).toString()
            if (day.length<2) day = "0"+day
            var mnth = (it.mnth!!.toInt().div(16)*10+it.mnth!!.toInt() % 16).toString()
            if (mnth.length<2) mnth = "0"+mnth
            var yearfh = (it.yearfh!!.toInt().div(16)*10+it.yearfh!!.toInt() % 16).toString()

            var yearsh = (it.yearsh!!.toInt().div(16)*10+it.yearsh!!.toInt() % 16).toString()
            if (yearsh.length<2) yearsh = "0"+yearsh
            var dayofweek = (it.dayofweek!!.toInt().div(16)*10+it.dayofweek!!.toInt() % 16).toString()

            dateTime.text = day+"."+ mnth+ "."+yearfh+ yearsh+ " "+hr+ ":"+min+ ":"+sec+" "+dayofweek
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

data class TempData(var sec: UByte?, var min: UByte?, var hr: UByte?, var day: UByte?, var mnth: UByte?, var yearfh: UByte?, var yearsh: UByte?, var dayofweek: UByte?)

data class Device (val name: String, val address: String)

data class Drawing(val mode: Int, val position: Int, val color: Int)