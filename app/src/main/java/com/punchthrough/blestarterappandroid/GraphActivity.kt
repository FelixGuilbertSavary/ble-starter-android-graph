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

import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.punchthrough.blestarterappandroid.ble.ConnectionEventListener
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import com.punchthrough.blestarterappandroid.ble.ConnectionManager.parcelableExtraCompat
import com.punchthrough.blestarterappandroid.databinding.ActivityGraphBinding
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

class GraphActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGraphBinding
    private var lineChart: LineChart? = null
    private var maxXVisibleRange = 50
    private var currentLen = 0
    private var dataXval = 0
    private val dataCharUUID = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")
    private val lineDataSet = LineDataSet(ArrayList<Entry>(), "Concentration")

    private val device: BluetoothDevice by lazy {
        intent.parcelableExtraCompat(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")
    }

    private val characteristics by lazy {
        ConnectionManager.servicesOnDevice(device)?.flatMap { service ->
            service.characteristics ?: listOf()
        } ?: listOf()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGraphBinding.inflate(layoutInflater)
        lineChart = binding.lineChart

        setContentView(binding.root)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = "Graph"
        }

        ConnectionManager.registerListener(connectionEventListener)
        ConnectionManager.requestMtu(device, 60)

        for (characteristic in characteristics) {
            println(characteristic.uuid)
            if(characteristic.uuid.equals(dataCharUUID)) {
                ConnectionManager.enableNotifications(device, characteristic)
                println("Found char!")
            }
        }

        lineDataSet.addEntry(Entry(0F, 0F))
        currentLen = 1
        dataXval = 1

        lineDataSet.setDrawValues(false)
        lineDataSet.setDrawFilled(true)
        lineDataSet.lineWidth = 3f
        lineChart!!.data = LineData(lineDataSet)
        lineChart?.setVisibleXRangeMaximum(maxXVisibleRange.toFloat())

        val buttonClick = findViewById<Button>(R.id.buttonSetting)
        buttonClick.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            println("Starting Setting")
            startActivity(intent)
            println("StartActivity returned")
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onDisconnect = {
                runOnUiThread {
                    AlertDialog.Builder(this@GraphActivity)
                        .setTitle("Disconnected")
                        .setMessage("Disconnected from device.")
                        .setPositiveButton("OK") { _, _ -> onBackPressed() }
                        .show()
                }
            }

            onCharacteristicRead = { _, characteristic, value ->
                //log("Read from ${characteristic.uuid}: ${value.toHexString()}")
            }

            onCharacteristicWrite = { _, characteristic ->
                //log("Wrote to ${characteristic.uuid}")
            }

            onMtuChanged = { _, mtu ->
                //log("MTU updated to $mtu")
            }

            onCharacteristicChanged = { _, characteristic, value ->
                if(characteristic.uuid.equals(dataCharUUID)) {
                    var sample = SamplePoint(value)

                    while(currentLen >= maxXVisibleRange) {
                        lineDataSet.removeFirst();
                        currentLen -= 1
                    }

                    lineDataSet.addEntry(Entry(dataXval.toFloat(), sample.gasConcentration))
                    lineChart?.notifyDataSetChanged()
                    lineChart?.data?.notifyDataChanged()
                    lineChart?.invalidate()
                    dataXval += 1
                    currentLen += 1
                }
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
        ConnectionManager.teardownConnection(device)
        super.onDestroy()
    }
}