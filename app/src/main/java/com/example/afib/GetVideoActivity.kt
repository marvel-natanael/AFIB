package com.example.afib

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.afib.databinding.ActivityGetVideoBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener


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
                binding.result.setImageBitmap(
                    retriever.frameAtTime
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

                val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
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
                }

            }
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