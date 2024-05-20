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

import java.nio.ByteBuffer
import java.nio.ByteOrder

class SamplePoint
{
    var gasConcentration : Float
    var timestamp: UInt
    var GPSLatitude: UInt
    var GPSLongitude: UInt
    var GPSAltitude: UInt
    var systemStable : UByte
    var fault : UByte
    constructor(bytes: ByteArray) {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        gasConcentration =  buffer.getFloat()
        timestamp = buffer.getInt().toUInt()
        GPSLatitude = buffer.getInt().toUInt()
        GPSLongitude = buffer.getInt().toUInt()
        GPSAltitude = buffer.getInt().toUInt()
        systemStable = buffer.get().toUByte()
        fault = buffer.get().toUByte()
    }
}

class ConfigRegister
{
    var wavelength : Float
    var modulationFrequency: Float
    var demodulationFrequency: Float
    var samplingInterval: UInt
    var demodulationMode: UByte
    var selfCalibration: UByte
    constructor(bytes: ByteArray) {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        wavelength =  buffer.getFloat()
        modulationFrequency = buffer.getFloat()
        demodulationFrequency = buffer.getFloat()
        samplingInterval = buffer.getInt().toUInt()
        demodulationMode = buffer.get().toUByte()
        selfCalibration = buffer.get().toUByte()
    }

    constructor(wavelength: Float, modulationFrequency: Float, demodulationFrequency: Float, samplingInterval: UInt, demodulationMode: UByte, selfCalibration: UByte) {
        this.wavelength=wavelength
        this.modulationFrequency = modulationFrequency
        this.demodulationFrequency = demodulationFrequency
        this.samplingInterval = samplingInterval
         this.demodulationMode = demodulationMode
        this.selfCalibration = selfCalibration
    }
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(18) // TODO: find sizeof() equivalent..
        buffer.order( ByteOrder.LITTLE_ENDIAN);

        buffer.putFloat(wavelength)
        buffer.putFloat(modulationFrequency)
        buffer.putFloat(demodulationFrequency)
        buffer.putInt(samplingInterval.toInt())
        buffer.put(demodulationMode.toByte())
        buffer.put(selfCalibration.toByte())

        return buffer.array()
    }
}