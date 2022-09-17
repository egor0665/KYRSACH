package com.example.kyrsach

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

@SuppressLint("MissingPermission")
class DeviceRepository(applicationContext: Context) {
    private val tag = "BluetoothConnection"

    private var counter = 0
    private val tempData = TempData(null, null, null)

    private var btAdapter: BluetoothAdapter? = null
    private var btSocket: BluetoothSocket? = null
    private var outStream: OutputStream? = null
    private var inStream: InputStream? = null
    private var receiveThread: ReceiveThread? = null

    private val info: MutableLiveData<TempData> by lazy {
        MutableLiveData<TempData>()
    }


    fun startObserve(): MutableLiveData<TempData> {
        return info
    }

    init {
        btAdapter =
            (applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    }

    fun findDevices(): List<Device>? {
        val btDevices = mutableListOf<Device>()
        (btAdapter?.bondedDevices)?.forEach {
            Log.d(it.name, it.address)
            btDevices.add(Device(it.name, it.address))
        }
        return btDevices
    }

    fun connect(address: String): Boolean {
        Log.d(tag, "Соединение ")

        try {
            val device = btAdapter?.getRemoteDevice(address)
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

            btSocket = device!!.createRfcommSocketToServiceRecord(uuid)
            Log.d(tag, "Создан")

            btAdapter!!.cancelDiscovery()

            btSocket!!.connect()
            Log.d(tag, "Соединено")

            outStream = btSocket!!.outputStream
            inStream = btSocket!!.inputStream
            receiveThread = ReceiveThread()
            receiveThread?.start()
            sendData(0)
            Log.d(tag, "Поток создан")
            return true

        } catch (e: Exception) {
            try {
                btSocket!!.close()
                Log.d("Socket", "Закрыт")
            } catch (e: Exception) {
                Log.d("Ошибка", "Нет соединения")
                return false
            }
            Log.d("Ошибка", "Что-то пошло не так")
            Log.d("Ошибка", e.toString())
            return false
        }
    }

    fun sendData(data: Int): Boolean {
        val msgBuffer = ByteArray(1)

        msgBuffer[0] = data.toByte()

        return try {
            outStream!!.write(msgBuffer)
            Log.d("Success", "Оправлены: $data")
            true

        } catch (e: Exception) {
            Log.d("Error", "Ошибка отправки")
            false
        }
    }

    private fun receiveData() {
        val msgBuffer = ByteArray(1)
        while (true) {
            try {
                val size = inStream?.read(msgBuffer)
                counter = counter.inc()
                when (counter) {
                    1 -> tempData.id = msgBuffer[0].toInt()
                    2 -> tempData.date = msgBuffer[0].toInt()
                    3 -> {
                        tempData.info = msgBuffer[0].toInt()
                        counter = 0
                        info.postValue(tempData)
                    }
                // 3 байта
                }
                Log.d("Success", "Message: ${msgBuffer[0].toInt()}")
            } catch (i: Exception) {
                break
            }
        }
    }

    inner class ReceiveThread(): Thread() {
        override fun run() {
            receiveData()
        }
    }

}