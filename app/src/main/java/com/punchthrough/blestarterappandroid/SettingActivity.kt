/*
 * Copyright 2024 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.punchthrough.blestarterappandroid.ble.ConnectionEventListener
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import com.punchthrough.blestarterappandroid.ble.ConnectionManager.parcelableExtraCompat
import com.punchthrough.blestarterappandroid.databinding.ActivitySettingsBinding
import com.punchthrough.blestarterappandroid.ble.toHexString
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private val settingCharUUID = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")
    private lateinit var settingChar : BluetoothGattCharacteristic
    private lateinit var samplingField : EditText

    private val device: BluetoothDevice by lazy {
        intent.parcelableExtraCompat(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from graph activity!")
    }

    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)

        setContentView(binding.root)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = "Settings"
        }

        ConnectionManager.registerListener(connectionEventListener)

        for (characteristic in characteristics) {
            println(characteristic.uuid)
            if(characteristic.uuid.equals(settingCharUUID)) {
                println("Found char Setting!")
                settingChar = characteristic
            }
        }

        ConnectionManager.readCharacteristic(device, settingChar)
        samplingField = findViewById<EditText>(R.id.samplingInterval)

        val btnPull = findViewById<Button>(R.id.btnSettingPull)
        btnPull.setOnClickListener{
            ConnectionManager.readCharacteristic(device, settingChar)
        }

        val btnPush = findViewById<Button>(R.id.btnSettingPush)
        btnPush.setOnClickListener{
            val parsedInt = samplingField.getText().toString().toIntOrNull()
            if(parsedInt != null) {
                println("The parsed int is $parsedInt")
                var conf = ConfigRegister(0f, 0f, 0f, parsedInt.toUInt(), 0u, 0u)
                ConnectionManager.writeCharacteristic(device, settingChar, conf.toByteArray())
            }
        }
    }

    private fun updateDisplayInterval(interval: UInt) {
        runOnUiThread {
            binding.samplingInterval.setText(interval.toString())
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {

            }

            onCharacteristicRead = { _, characteristic, value ->
                println("Read from ${characteristic.uuid}: ${value.toHexString()}")
                if(characteristic.uuid.equals(settingCharUUID)) {
                    var conf = ConfigRegister(value)
                    updateDisplayInterval(conf.samplingInterval)
                }
            }

            onCharacteristicWrite = { _, characteristic ->
                println("Wrote to ${characteristic.uuid}")
            }

            onMtuChanged = { _, mtu ->
                //log("MTU updated to $mtu")
            }

            onCharacteristicChanged = { _, characteristic, value ->

            }

            onNotificationsEnabled = { _, characteristic ->
                //log("Enabled notifications on ${characteristic.uuid}")
                //notifyingCharacteristics.add(characteristic.uuid)
            }

            onNotificationsDisabled = { _, characteristic ->
                //log("Disabled notifications on ${characteristic.uuid}")
                //notifyingCharacteristics.remove(characteristic.uuid)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onDestroy() {
        ConnectionManager.unregisterListener(connectionEventListener)
        super.onDestroy()
    }
}