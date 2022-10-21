package com.example.afib

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture.withOutput
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.afib.databinding.ActivityMainBinding
import com.example.afib.ui.VideoActivity
import org.opencv.android.OpenCVLoader
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    //  private lateinit var videoCapture: VideoCapture<Recorder>
    private var currentRecording: Recording? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

//    val cameraProvider = ProcessCameraProvider.getInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (OpenCVLoader.initDebug()) {
            Log.d("myTag", "OpenCV loaded")
        }

        if (allPermissionGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }

        binding.buttonTakePhoto.setOnClickListener {
            //   takePhoto()
            captureVideo()
            // startCamera()
        }
    }

    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        binding.buttonTakePhoto.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
       val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                Constants.FILE_NAME_FORMAT,
                Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".mp4"
        )

        val outputAction  = ImageCapture
            .OutputFileOptions
            .Builder(photoFile)
            .build()

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) ==
                    PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        /*   binding.videoCaptureButton.apply {
                               text = "Stop Capture"
                               isEnabled = true
                           }*/
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
                                .show()
                            Log.d("TAG", msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(
                                "TAG", "Video capture ends with error: " +
                                        "${recordEvent.error}"
                            )
                        }
                        /*   binding.videoCaptureButton.apply {
                               text = "Start Capture"
                               isEnabled = true
                           }*/
                    }
                }
            }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let { mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply {
                mkdirs()
            }
        }

        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                Constants.FILE_NAME_FORMAT,
                Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputAction = ImageCapture
            .OutputFileOptions
            .Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputAction, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val saveUri = Uri.fromFile(photoFile)
                    val msg = "Photo Saved"

                    Toast.makeText(
                        this@MainActivity,
                        "$msg $saveUri",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(
                        Constants.TAG,
                        "onError: ${exception.message}",
                        exception
                    )
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also { mPreview ->
                    mPreview.setSurfaceProvider(
                        binding.cameraView.surfaceProvider
                    )
                }
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                camera.cameraControl.enableTorch(true)
            } catch (e: Exception) {
                Log.d(Constants.TAG, "Camera Failed: ", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setVideo() {

        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )

        val recorder = Recorder.Builder()
            .setExecutor(cameraExecutor).setQualitySelector(qualitySelector)
            .build()

        val videoCapture = VideoCapture.withOutput(recorder)

        try {
            // Bind use cases to camera
            /*  cameraProvider.bindToLifecycle(
                  this, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture)*/
        } catch (exc: Exception) {
            Log.e("TAG", "Use case binding failed", exc)
        }
    }


    /*  private fun startRecording() {
          // create MediaStoreOutputOptions for our recorder: resulting our recording!
          val name = "CameraX-recording-" +
                  SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                      .format(System.currentTimeMillis()) + ".mp4"
          val contentValues = ContentValues().apply {
              put(MediaStore.Video.Media.DISPLAY_NAME, name)
          }
          val mediaStoreOutput = MediaStoreOutputOptions.Builder(
              this.contentResolver,
              MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
              .setContentValues(contentValues)
              .build()

          // configure Recorder and Start recording to the mediaStoreOutput.
          currentRecording = videoCapture.output
              .prepareRecording(this, mediaStoreOutput)
              .apply { if (audioEnabled) withAudioEnabled() }
              .start(mainThreadExecutor, captureListener)

          Log.i("TAG", "Recording started")
      }*/

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permission Not Granted", Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}