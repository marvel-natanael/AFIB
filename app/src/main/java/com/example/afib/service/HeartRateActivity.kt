package com.example.afib.service

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import com.example.afib.Constant.MESSAGE_CAMERA_NOT_AVAILABLE
import com.example.afib.Constant.REQUEST_CODE_CAMERA
import com.example.afib.Constants.MESSAGE_UPDATE_REALTIME
import com.example.afib.R
import com.example.afib.databinding.ActivityHeartRateBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class HeartRateActivity : AppCompatActivity(), OnRequestPermissionsResultCallback {

    private lateinit var binding: ActivityHeartRateBinding

    private var analyzer: OutputAnalyzer? = null

    private var justShared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeartRateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA),
            REQUEST_CODE_CAMERA
        )

        binding.startCamera.setOnClickListener {
            binding.camera.visibility = View.VISIBLE
            binding.startCamera.visibility = View.GONE
            onClickNewMeasurement()
        }
    }

    private val mainHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            if (msg.what == MESSAGE_UPDATE_REALTIME) {
                binding.result.text = msg.obj.toString()
            }

            if (msg.what == MESSAGE_CAMERA_NOT_AVAILABLE) {
                Log.println(Log.WARN, "camera", msg.obj.toString())
                analyzer!!.stop()
            }
        }
    }

    private val cameraService = Camera(this, mainHandler)

    override fun onPause() {
        super.onPause()
        if (analyzer == null) return
        cameraService.stop()
        analyzer!!.stop()
        analyzer = OutputAnalyzer(
            this,
            binding.graphTextureView,
            binding.camera,
            binding.startCamera,
            mainHandler
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Snackbar.make(
                    findViewById(R.id.constraintLayout),
                    getString(R.string.cameraPermissionRequired),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return super.onPrepareOptionsMenu(menu)
    }

    fun onClickNewMeasurement(item: MenuItem?) {
        //onClickNewMeasurement()
    }

    fun onClickNewMeasurement(view: View?) {
        //  onClickNewMeasurement()
    }

    private fun onClickNewMeasurement() {

        analyzer = OutputAnalyzer(
            this,
            binding.graphTextureView,
            binding.camera,
            binding.startCamera,
            mainHandler
        )

        // clear prior results
        val empty = CharArray(0)
        binding.result.setText(empty, 0, 0)

        //   setViewState(VIEW_STATE.MEASUREMENT)
        val previewSurfaceTexture = binding.camera.surfaceTexture
        if (previewSurfaceTexture != null) {
            val previewSurface = Surface(previewSurfaceTexture)
            cameraService.start(previewSurface)
            analyzer!!.measurePulse(binding.camera, cameraService)
        }
    }

    private fun getTextIntent(intentText: String): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(
            Intent.EXTRA_SUBJECT, String.format(
                getString(R.string.output_header_template),
                SimpleDateFormat(
                    getString(R.string.dateFormat),
                    Locale.getDefault()
                ).format(Date())
            )
        )
        intent.putExtra(Intent.EXTRA_TEXT, intentText)
        return intent
    }
}