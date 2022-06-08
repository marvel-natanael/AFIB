package com.example.afib

import android.Manifest

object Constants {
    const val TAG = "cameraX"
    const val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss-SSS"
    const val REQUEST_CODE_PERMISSIONS = 123
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    const val MESSAGE_UPDATE_REALTIME = 1
    const val MESSAGE_UPDATE_FINAL = 2
}