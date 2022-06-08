package com.example.afib

import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.afib.databinding.ActivityOpenCvtestBinding
import org.opencv.android.*
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.util.*

class OpenCVTestActivity : CameraActivity() {

    private lateinit var binding: ActivityOpenCvtestBinding

    private val TAG = "MainActivity"

    private lateinit var mRgba: Mat
    private lateinit var mGray: Mat

    private lateinit var mOpenCvCameraView: CameraBridgeViewBase

    private var mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    mOpenCvCameraView.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenCvtestBinding.inflate(layoutInflater)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(binding.root)

        mOpenCvCameraView = binding.opencvSurface
        mOpenCvCameraView.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView.setCvCameraViewListener(cvCameraListener)

    }

    override fun getCameraViewList(): MutableList<out CameraBridgeViewBase> {
        return Collections.singletonList(mOpenCvCameraView)
    }

    private var cvCameraListener = object : CameraBridgeViewBase.CvCameraViewListener2 {
        override fun onCameraViewStarted(width: Int, height: Int) {

        }

        override fun onCameraViewStopped() {

        }

        override fun onCameraFrame(inputFrame: CvCameraViewFrame?): Mat? {
            val rgba = inputFrame?.rgba()
            val gray = inputFrame?.gray()

            val corners = MatOfPoint()
            Imgproc.goodFeaturesToTrack(gray, corners, 20, 0.01, 10.0, Mat(), 3, false)
            val cornerArr =     corners.toArray()

            for (i in 0 until corners.rows()){
                Imgproc.circle(rgba, cornerArr[i], 10, Scalar(0.0,255.0,0.0),2)
            }

            return rgba
        }
    }


    override fun onPause() {
        super.onPause()

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView()
        }
    }

}