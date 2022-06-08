package com.example.afib.service

import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

class MeasureStore {
    private val measurements = CopyOnWriteArrayList<Measurement<Int>>()
    private var minimum = 2147483647
    private var maximum = -2147483648

    private val rollingAverageSize = 4

    fun add(measurement: Int) {
        val measurementWithDate = Measurement(Date(), measurement)
        measurements.add(measurementWithDate)
        if (measurement < minimum) minimum = measurement
        if (measurement > maximum) maximum = measurement
    }

    fun getStdValues(): CopyOnWriteArrayList<Measurement<Float>> {
        val stdValues = CopyOnWriteArrayList<Measurement<Float>>()
        for (i in measurements.indices) {
            var sum = 0
            for (rollingAverageCounter in 0 until rollingAverageSize) {
                sum += measurements[max(0, i - rollingAverageCounter)].measurement
            }
            val stdValue = Measurement(
                measurements[i].timestamp,
                (sum.toFloat() / rollingAverageSize - minimum) / (maximum - minimum)
            )
            stdValues.add(stdValue)
        }
        return stdValues
    }

    fun getLastStdValues(count: Int): CopyOnWriteArrayList<Measurement<Int>> {
        return if (count < measurements.size) {
            CopyOnWriteArrayList<Measurement<Int>>(
                measurements.subList(
                    measurements.size - 1 - count,
                    measurements.size - 1
                )
            )
        } else {
            measurements
        }
    }

    fun getLastTimestamp(): Date {
        return measurements[measurements.size - 1].timestamp
    }
}