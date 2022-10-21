package com.example.afib

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
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
import com.example.afib.data.Roi
import com.example.afib.databinding.ActivityGetVideoBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.momentolabs.frameslib.Frames
import com.momentolabs.frameslib.data.model.FrameRetrieveRequest
import com.momentolabs.frameslib.data.model.Status
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc.rectangle
import java.lang.Math.sin
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.cos

class GetVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGetVideoBinding
    private lateinit var retriever: MediaMetadataRetriever

    private val listBitmap = ArrayList<Bitmap>()
    private val listAverage = ArrayList<Float>()
    private val listRoi = ArrayList<Roi>()
    private val listChartAverage = ArrayList<Entry>()
    private val listChartDft = ArrayList<Entry>()
    private val fixListAverage = ArrayList<Float>()

    private var i = 1

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        retriever = MediaMetadataRetriever()

        requestMultiplePermissions()

        OpenCVLoader.initDebug()

        binding.start.setOnClickListener {
            showPictureDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showPictureDialog() {

        val pictureDialog = AlertDialog.Builder(this)
        pictureDialog.setTitle("Select Action")
        val pictureDialogItems =
            arrayOf("Select video from gallery", "Record video from camera")
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

    private fun calculateDFT(length: Int, time: Int) {
        log("Size " + length)
        val Xr = DoubleArray(length)
        val Xi = DoubleArray(length)

        val xn = FloatArray(length)

        for (k in 0 until time) {
            Xr[k] = 0.0
            Xi[k] = 0.0

            for (n in 0 until length) {
                xn[k] = fixListAverage[k]
                Xr[k] = (Xr[k] + xn[n] * cos(2 * 3.141592 * k * n / time))
                Xi[k] = (Xi[k] + xn[n] * sin(2 * 3.141592 * k * n / time))
                log("Xr "+Xr[k])
                log("Xi "+Xi[k])
                val y = Xr[k] + Xi[k]
                log("Result $n " + y)
            }

            val result = Xr[k] + Xi[k]
            listChartDft.add(Entry(k.toFloat(), result.toFloat()))
            log("Result DFT ${Xr[k]} ${Xi[k]}")
        }
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

                val mp = MediaPlayer.create(this, Uri.parse(getPath(contentURI)));
                val duration = mp.duration;

                log("Duration $duration")

                Log.d("path", selectedVideoPath!!)

                retriever.setDataSource(selectedVideoPath);

                val multiFrameRequest = FrameRetrieveRequest.MultipleFrameRequest(
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
                                log("Completed: ${framesResource.frames.size}")
                                framesResource.frames.forEach { data ->
                                    listBitmap.add(data.bitmap!!)
                                }
                                log("Size ${listBitmap.size}")
                                framesResource.frames.forEach { data ->
                                    findRoi(data.bitmap!!)
                                    log("Size i $i")
                                    lineChart(listChartAverage, binding.lineChart)

                                    i++
                                    listRoi.clear()
                                    listAverage.clear()
                                }

                                fixListAverage.forEach {
                                    log("PInit Average $it")
                                }

                                log("Size Fix ${fixListAverage.size}")
                                log("Frame Size " + framesResource.frames.size)
                                val time: Int =
                                    TimeUnit.MILLISECONDS.toSeconds(duration.toLong()).toInt()
                                calculateDFT(framesResource.frames.size, time)

                              //  lineChart(listChartDft, binding.dft)
                            }
                        }
                    }


            }
        }

    private fun lineChart(
        listChart: ArrayList<Entry>,
        chart: com.github.mikephil.charting.charts.LineChart
    ) {
        val lineDataSet = LineDataSet(listChart, "List Chart")
        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        lineDataSet.color = resources.getColor(android.R.color.black)
        lineDataSet.setCircleColor(resources.getColor(android.R.color.holo_red_dark))
        lineDataSet.setDrawValues(false)
        lineDataSet.setDrawFilled(false)
        lineDataSet.disableDashedLine()
        lineDataSet.setColors(resources.getColor(android.R.color.holo_red_dark))

        chart.setTouchEnabled(false)
        chart.axisRight.isEnabled = false
        chart.setPinchZoom(false)
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)

        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawAxisLine(false)

        chart.xAxis.textColor = resources.getColor(android.R.color.black)
        chart.xAxis.textSize = 12f

        chart.axisLeft.textColor = resources.getColor(android.R.color.black)
        chart.axisLeft.textSize = 12f

        //Setup Legend
        val legend = chart.legend
        legend.isEnabled = false

        val frames = ArrayList<String>()
        for (j in 0 until i) {
            frames.add(j.toString())
        }

        val tanggal = AxisDateFormatter(frames.toArray(arrayOfNulls<String>(frames.size)))
        chart.xAxis?.valueFormatter = tanggal


        chart.description.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.spaceMax = 0.5f

        chart.data = LineData(lineDataSet)
        chart.animateY(500, Easing.Linear)
    }

    class AxisDateFormatter(private val mValues: Array<String>) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return if (value >= 0) {
                if (mValues.size > value.toInt()) {
                    mValues[value.toInt()]
                } else ""
            } else {
                ""
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun setRoi(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        green: Scalar,
        sourceMat: Mat,
        bitmap: Bitmap,
    ) {
        val pxy = Point(x, y)
        val pwh = Point(width, height)
        val listTotal = ArrayList<Float>()
        for (i in x.toInt() until width.toInt()) {
            for (j in y.toInt() until height.toInt()) {
                val color = bitmap.getColor(i, j)
                val red = color.red()
                if (red <= 0.5) {
                    //set black
                    bitmap.setPixel(i, j, Color.BLACK)
                } else {
                    /*set red*/
                    listTotal.add(red)
                    bitmap.setPixel(i, j, Color.RED)
                }
            }
        }

        val average =
            listTotal.sum() / ((bitmap.width / 2) * (bitmap.height / 3)) // get average roi
        listAverage.add((0.5f - average).absoluteValue) // add to list average roi
        listRoi.add(
            Roi(
                x,
                y,
                width,
                height,
                (0.5f - average).absoluteValue
            )
        ) //add to list every roi
        log("Average $average")

        rectangle(sourceMat, pxy, pwh, green, 3)
    }

    // PInit = (0.5f - average).absolute

    private fun getPInit() {
        fixListAverage.add(listAverage.minBy { it })
        listChartAverage.add(Entry(i.toFloat(), listAverage.minBy { it }))
        log("PInit ${listAverage.minBy { it }}")
        listAverage.clear()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun setCropRoi(bitmap: Bitmap) {
        // var roi = Roi()
        //listRoi.forEach {
        //  log("PInit ${getPInit()}")
        //  if (it.average == getPInit()) {

        // keyMap.clear()
        //   roi = it
        //listChartAverage.add(Entry(i.toFloat(), it.average!!.toFloat()))
        //listChartAverage.addAll(Entry())
        log("OKE")

        //  i++
        //  }

        getPInit()
        // listAverage.clear()
        //listRoi.clear()

        /* log("Average2 ${roi.average}")

         val width = roi.width!!.toInt()
         val height = roi.height!!.toInt()
         val x = roi.x!!.toInt()
         val y = roi.y!!.toInt()*/

        //   log("Width $width $height $x $y")

        /*  val crop = Bitmap.createBitmap(
              bitmap,
              x,
              y,
              bitmap.width / 2,
              bitmap.height / 3
          )*/
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun findRoi(bitmap: Bitmap) {

        val x = 0.0
        val y = 0.0
        val width = ((bitmap.width) / 2).toDouble()
        val height = (bitmap.height / 3).toDouble()

        val sourceMat = Mat(bitmap.width, bitmap.height, CvType.CV_8UC3)
        sourceMat.clone()
        val green = Scalar(0.0, 255.0, 0.0, 255.0)
        Utils.bitmapToMat(bitmap, sourceMat)

        val roiBitmap =
            Bitmap.createBitmap(sourceMat.cols(), sourceMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(sourceMat, roiBitmap)

        setRoi(x, y, width, height, green, sourceMat, roiBitmap)
        setRoi(
            x + width,
            y,
            width * 2,
            height,
            green,
            sourceMat,
            roiBitmap
        )

        setRoi(
            x,
            y + height,
            width,
            height * 2,
            green,
            sourceMat,
            roiBitmap
        )

        setRoi(
            x + width,
            y + height,
            width * 2,
            height * 2,
            green,
            sourceMat,
            roiBitmap
        )

        setRoi(
            x,
            y + height * 2,
            width,
            height * 3,
            green,
            sourceMat,
            roiBitmap
        )

        setRoi(
            x + width,
            y + height * 2,
            width * 2,
            height * 3,
            green,
            sourceMat,
            roiBitmap
        )

        setCropRoi(roiBitmap)
    }


    private fun log(message: String) {
        Log.d("AFIB", message)
    }

    private fun showToast(message: String) {
        Toast.makeText(
            applicationContext,
            message,
            Toast.LENGTH_SHORT
        ).show()
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
        Dexter.withContext(this@GetVideoActivity)
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
                    p1: PermissionToken?,
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