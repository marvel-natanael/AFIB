package com.example.afib

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.afib.databinding.ActivityGetVideoBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.rectangle


class GetVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGetVideoBinding
    private lateinit var retriever: MediaMetadataRetriever
    private lateinit var frameAdapter: FrameImageAdapter

    private val listBitmap = ArrayList<Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        retriever = MediaMetadataRetriever()
        requestMultiplePermissions()

        OpenCVLoader.initDebug();

        binding.start.setOnClickListener {
            showPictureDialog()
        }
    }

    private fun showPictureDialog() {
        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems = arrayOf("Select video from gallery", "Record video from camera")
        pictureDialog.setItems(
            pictureDialogItems
        ) { _, which ->
            when (which) {
                0 -> chooseVideoFromGallery()
                1 -> takeVideoFromCamera()
            }
        }
        pictureDialog.show()
    }

    private fun chooseVideoFromGallery() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )

        galleryIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        activityResult.launch(galleryIntent)
    }

    private fun takeVideoFromCamera() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        activityResult.launch(intent)
    }

    private val activityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_CANCELED) {
                Log.d("what", "cancel")
            }

            if (it.data != null) {

                listBitmap.clear()

                val contentURI = it.data!!.data

                val selectedVideoPath = getPath(contentURI)
                Log.d("path", selectedVideoPath!!)

                retriever.setDataSource(selectedVideoPath);
                val bitmap: Bitmap = retriever.frameAtTime!!
                binding.result.setImageBitmap(
                    bitmap
                )

                /* val multiFrameRequest = FrameRetrieveRequest.MultipleFrameRequest(
                     videoPath = selectedVideoPath,
                     durationPerFrame = 33
                 )

                 Frames
                     .load(multiFrameRequest)
                     .into { framesResource ->
                         when (framesResource.status) {
                             Status.EMPTY_FRAMES -> log(
                                 "emptyframes: ${framesResource.frames.size} ${System.currentTimeMillis()}"
                             )
                             Status.LOADING -> log(
                                 "loading: ${framesResource.frames.size}"
                             )
                             Status.COMPLETED -> {
                                 log("Completed: ${framesResource.frames.size} ${System.currentTimeMillis()}")

                                 log(
                                     framesResource.frames[100].bitmap!!.toDrawable(resources)
                                         .toString()
                                 )
                                 framesResource.frames.forEach { data ->
                                     listBitmap.add(data.bitmap!!)
                                 }

                                 frameAdapter.notifyDataSetChanged()
                                 //binding.result.setImageBitmap(framesResource.frames[100].bitmap)
                             }
                         }
                     }

                 Frames.load(multiFrameRequest).into(
                     videoFramesLayout = binding.layoutFramesLayout,
                     orientation = LinearLayout.HORIZONTAL
                 )*/

                /* val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                 var timeInMillisec = time!!.toLong()

                 while (timeInMillisec >= 33) {
                     log("Time : " + timeInMillisec)
                     listBitmap.add(retriever.getFrameAtTime(33)!!)
                     timeInMillisec -= 33
                 }

                 frameAdapter = FrameImageAdapter(listBitmap)

                 binding.rv.apply {
                     layoutManager = LinearLayoutManager(
                         this@GetVideoActivity,
                         LinearLayoutManager.HORIZONTAL,
                         false
                     )
                     this.adapter = frameAdapter
                 }*/

                binding.newImage.setImageBitmap(findRoi(bitmap))
            }
        }


    private fun findRoi(bitmap: Bitmap): Bitmap {
        val x = 0.0
        val y = 0.0
        val width = ((bitmap.width) / 2).toDouble()
        val height = (bitmap.height / 3).toDouble()

        val sourceMat = Mat(bitmap.width, bitmap.height, CvType.CV_8UC3)
        sourceMat.clone()
        val green = Scalar(0.0, 255.0, 0.0, 255.0)
        Utils.bitmapToMat(bitmap, sourceMat)
        for (i in 1..2) {
            for (j in 1..3) {
                rectangle(
                    sourceMat,
                    Point(x + (width * (i - 1)), y + (height * (j - 1))),
                    Point(width * i, height * j),
                    green,
                    3
                )
            }
        }
        val roiBitmap =
            Bitmap.createBitmap(sourceMat.cols(), sourceMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(sourceMat, roiBitmap)


        return roiBitmap!!
    }

    private fun log(message: String) {
        Log.d("AFIB", message)
    }

    private fun showToast(message: String) {
        Toast.makeText(
            applicationContext,
            message,
            Toast.LENGTH_SHORT
        )
            .show()
    }

    private fun getPath(uri: Uri?): String? {

        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor = contentResolver.query(uri!!, projection, null, null, null)
        return if (cursor != null) {
            val columnIndex = cursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        } else
            null
    }

    private fun requestMultiplePermissions() {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    // check if all permissions are granted
                    if (report.areAllPermissionsGranted()) {
                        showToast("All permissions are granted by user!")
                    }

                    // check for permanent denial of any permission
                    if (report.isAnyPermissionPermanentlyDenied) {
                        // show alert dialog navigating to Settings
                        //openSettingsDialog()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<com.karumi.dexter.listener.PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1!!.continuePermissionRequest()
                }
            }).withErrorListener {
                showToast("Some Error! ")
            }
            .onSameThread()
            .check()
    }

}