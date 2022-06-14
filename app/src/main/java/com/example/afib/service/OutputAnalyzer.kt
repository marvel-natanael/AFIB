package com.example.afib.service

import android.os.CountDownTimer
import android.os.Handler
import android.os.Message
import android.view.TextureView
import android.view.View
import android.widget.Button
import com.example.afib.Constant.MESSAGE_CAMERA_NOT_AVAILABLE
import com.example.afib.Constants.MESSAGE_UPDATE_REALTIME
import com.example.afib.R
import com.febrian.flog.Flog
import java.lang.String
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.ceil
import kotlin.math.max

class OutputAnalyzer(
    private val activity: HeartRateActivity,
    private val graphTextureView: TextureView,
    private val cameraTextureView: TextureView,
    private val btnStart: Button,
    private val mainHandler: Handler
) {

    private val chartDrawer: ChartDrawer = ChartDrawer(graphTextureView)

    private var store: MeasureStore? = null

    private val measurementInterval: Long = 45
    private val measurementLength: Long =
        30000 // ensure the number of data points is the power of two

    private val clipLength = 100

    private var detectedValleys = 0
    private var ticksPassed = 0

    private val valleys = CopyOnWriteArrayList<Long>()

    private var timer: CountDownTimer? = null

    private fun detectValley(): Boolean {
        val valleyDetectionWindowSize = 13
        val subList = store!!.getLastStdValues(valleyDetectionWindowSize)
        return if (subList.size < valleyDetectionWindowSize) {
            false
        } else {
            val referenceValue =
                subList[ceil((valleyDetectionWindowSize / 2f).toDouble()).toInt()].measurement
            for (measurement in subList) {
                if (measurement.measurement < referenceValue) return false
            }

            // filter out consecutive measurements due to too high measurement rate
            subList[ceil((valleyDetectionWindowSize / 2f).toDouble())
                .toInt()].measurement != subList[ceil((valleyDetectionWindowSize / 2f).toDouble())
                .toInt() - 1].measurement
        }
    }

    fun measurePulse(textureView: TextureView, cameraService: Camera) {
        store = MeasureStore()
        detectedValleys = 0
        Flog.d("RUN1")
        timer = object : CountDownTimer(measurementLength, measurementInterval) {
            override fun onTick(millisUntilFinished: Long) {
                Flog.d("RUN2")
                // skip the first measurements, which are broken by exposure metering
                 // if (clipLength > ++ticksPassed * measurementInterval) return

                val thread = Thread {
                    val currentBitmap = textureView.bitmap
                    val pixelCount = textureView.width * textureView.height
                    var measurement = 0
                    val pixels = IntArray(pixelCount)
                    currentBitmap!!.getPixels(
                        pixels,
                        0,
                        textureView.width,
                        0,
                        0,
                        textureView.width,
                        textureView.height
                    )

                    for (pixelIndex in 0 until pixelCount) {
                        measurement += pixels[pixelIndex] shr 16 and 0xff
                    }

                    store!!.add(measurement)
                    if (detectValley()) {
                        detectedValleys += 1
                        valleys.add(store!!.getLastTimestamp().time)

                        // update value every second
                        val currentValue = String.format(
                            Locale.getDefault(),
                            activity.resources.getQuantityString(
                                R.plurals.measurement_output_template,
                                detectedValleys
                            ),
                            if (valleys.size == 1) 60f * detectedValleys / max(
                                1f,
                                (measurementLength - millisUntilFinished - clipLength) / 1000f
                            ) else 60f * (detectedValleys - 1) / max(
                                1f,
                                (valleys[valleys.size - 1] - valleys[0]) / 1000f
                            ),
                            detectedValleys,
                            1f * (measurementLength - millisUntilFinished - clipLength) / 1000f
                        )

                        sendMessage(MESSAGE_UPDATE_REALTIME, currentValue)
                    }

                    Flog.d("Chart")

                    // draw chart
                    val chartDrawerThread =
                        Thread {
                            Flog.d("RUN")
                            chartDrawer.draw(
                                store!!.getStdValues()
                            )
                        }
                    chartDrawerThread.start()
                }

                thread.start()
            }

            override fun onFinish() {
                if (valleys.size == 0) {
                    mainHandler.sendMessage(
                        Message.obtain(
                            mainHandler,
                            MESSAGE_CAMERA_NOT_AVAILABLE,
                            "No valleys detected - there may be an issue when accessing the camera."
                        )
                    )
                    return
                }

                // update final
                val currentValue = String.format(
                    Locale.getDefault(),
                    activity.resources.getQuantityString(
                        R.plurals.measurement_output_template,
                        detectedValleys - 1
                    ),
                    60f * (detectedValleys - 1) / max(
                        1f,
                        (valleys[valleys.size - 1] - valleys[0]) / 1000f
                    ),
                    detectedValleys - 1,
                    1f * (valleys[valleys.size - 1] - valleys[0]) / 1000f
                )


                sendMessage(MESSAGE_UPDATE_REALTIME, currentValue)

                cameraTextureView.visibility = View.GONE
                btnStart.visibility = View.VISIBLE
                cameraService.stop()
            }

        }

        timer?.start()
    }

    fun stop() {
        timer?.cancel()
    }

    fun sendMessage(what: Int, message: Any?) {
        val msg = Message()
        msg.what = what
        msg.obj = message
        mainHandler.sendMessage(msg)
    }
}