package com.example.afib

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.afib.databinding.ActivityGetVideoBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc.rectangle


class GetVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGetVideoBinding
    private lateinit var retriever: MediaMetadataRetriever
    private lateinit var frameAdapter: FrameImageAdapter

    private val listBitmap = ArrayList<Bitmap>()

    @RequiresApi(Build.VERSION_CODES.Q)
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

    @RequiresApi(Build.VERSION_CODES.Q)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun chooseVideoFromGallery() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )

        galleryIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        activityResult.launch(galleryIntent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun takeVideoFromCamera() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        activityResult.launch(intent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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
                //  getColors(bitmap)
                binding.newImage.setImageBitmap(findRoi(bitmap))
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getColors(bitmap: Bitmap): Bitmap {
        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.height) {
                val color: Color = bitmap.getColor(i, j)
                val red: Float = color.red()
                log(red.toString())

                if (red <= 0.7) {
                    log("Black " + red.toString())
                    //   Imgproc.cvtColor(sourceMat, dst, COLORMAP_AUTUMN)
                    //set color black
                    bitmap.setPixel(i, j, Color.BLACK)
                } else {
                    log("Red " + red.toString())
                    //set color red
                    bitmap.setPixel(i, j, Color.RED)
                }
            }
        }

        return bitmap
    }

    private fun setRoi(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        green: Scalar,
        sourceMat: Mat
    ) {
        val pxy = Point(x, y)
        val pwh = Point(width, height)

        rectangle(sourceMat, pxy, pwh, green, 3)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findRoi(bitmap: Bitmap): Bitmap {
        val x = 0.0
        val y = 0.0
        val width = ((bitmap.width) / 2).toDouble()
        val height = (bitmap.height / 3).toDouble()

        val sourceMat = Mat(bitmap.width, bitmap.height, CvType.CV_8UC3)
        sourceMat.clone()
        val green = Scalar(0.0, 255.0, 0.0, 255.0)
        Utils.bitmapToMat(bitmap, sourceMat)

        setRoi(x, y, width, height, green, sourceMat)
        setRoi(x + width, y, width * 2, height * 2, green, sourceMat)
        setRoi(x, y + height * 2, width, height * 3, green, sourceMat)
        setRoi(x + width, y, width * 2, height, green, sourceMat)
        setRoi(x, y + height, width, height * 2, green, sourceMat)
        setRoi(x + width, y + height * 2, width * 2, height * 3, green, sourceMat)

        /* val pxy1 = Point(x, y)
         val pwh1 = Point(width, height)

         val pxy2 = Point(x + width, y)
         val pwh2 = Point(width * 2, height * 2)

         val pxy3 = Point(x, y + height * 2)
         val pwh3 = Point(width, height *3)

         val pxy4 = Point(x + width, y)
         val pwh4 = Point(width * 2, height)

         val pxy5 = Point(x, y + height)
         val pwh5 = Point(width, height * 2)

         val pxy6 = Point(x + width, y + height * 2)
         val pwh6 = Point(width * 2, height *3)

         rectangle(sourceMat, pxy6, pwh6, green, 3)*/

        val roiBitmap =
            Bitmap.createBitmap(sourceMat.cols(), sourceMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(sourceMat, roiBitmap)

        var pixelCount = 0

        var total = 0f
        var color: Color? = null
        var red: Float? = null

        var listTotal = ArrayList<Float>()

        for (i in 0 until bitmap.width) {
            for (j in 0 until bitmap.height) {
                pixelCount++
                color = roiBitmap.getColor(i, j)
                red = color.red()

                listTotal.add(red)

                // log("Total $total")

                /* log(red.toString())

                 if (red <= 0.7) {
                     log("Black $red")
                     //   Imgproc.cvtColor(sourceMat, dst, COLORMAP_AUTUMN)
                     //set color black
                     roiBitmap.setPixel(i, j, Color.BLACK)
                 } else {
                     log("Red $red")
                     //set color red
                     roiBitmap.setPixel(i, j, Color.RED)

                 }*/

            }
        }

        val avg = listTotal.sum() / (bitmap.width * bitmap.height)

        log("AVG $avg")

        /*  var listRoi = Array(2) { IntArray(3) { 0 } }

          for (i in 1..2) {
              for (j in 1..3) {
                  var count = 1
                  var areaRoi = (bitmap.width / 2) * (bitmap.height / 3)

                  listRoi[i][j] = roiBitmap.setPixel()

                  log("Total $count : $total")
                  count += 1
              }
          }

          for (x in 0 until bitmap.width / 2) {
              for (y in 0 until bitmap.height / 3) {

                  // total
                  val color = roiBitmap.getColor(x, y)
                  val red: Float = color.red()

                  total += red
              }
          }
  */

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