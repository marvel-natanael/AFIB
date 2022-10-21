package com.example.afib.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.afib.api.ApiConfig
import com.example.afib.api.ResponseData
import com.example.afib.databinding.ActivityGetFrameVideoBinding
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody;
import okhttp3.RequestBody.Companion.create

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class GetFrameVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGetFrameVideoBinding
    private lateinit var retriever: MediaMetadataRetriever

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetFrameVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        retriever = MediaMetadataRetriever()

        binding.start.setOnClickListener {
            showPictureDialog()
        }
    }

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

    private fun takeVideoFromCamera() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        activityResult.launch(intent)
    }

    private fun chooseVideoFromGallery() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )

        galleryIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        activityResult.launch(galleryIntent)
    }

    private fun uploadVideo(file: File) {
        val map: HashMap<String, RequestBody> = HashMap()
        val requestBody: RequestBody = create("*/*".toMediaTypeOrNull(), file)
        map["file\"; filename=\"" + file.name.toString() + "\""] = requestBody
        val getResponse = ApiConfig.api
        val call: Call<ResponseData> = getResponse.uploadVideo(map)

        call.enqueue(object : Callback<ResponseData> {
            override fun onResponse(call: Call<ResponseData>, response: Response<ResponseData>) {
                log(response.body().toString())
            }

            override fun onFailure(call: Call<ResponseData>, t: Throwable) {
                log(t.message.toString())
            }

        })
    }

    private val activityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val contentURI = it.data!!.data

            val selectedVideoPath = getPath(contentURI!!)

            retriever.setDataSource(selectedVideoPath);
            val file = File(selectedVideoPath)
            uploadVideo(file)

        }

    private fun log(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        Log.d("AFIB", message)
    }

    @SuppressLint("Range")
    private fun getPath(uri: Uri): String? {

        val renamed = File(uri.toString(), "New Name")
        // renamed.renameTo(File("newfile"))
        binding.afterRename.text = renamed.name

        if (uri.scheme.equals("content")) {
            val myCursor = contentResolver.query(uri, null, null, null, null);
            try {
                if (myCursor != null && myCursor.moveToFirst()) {

                    binding.nameFile.text =
                        myCursor.getString(myCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))

                }
            } finally {
                myCursor!!.close();
            }
        }
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)

        return if (cursor != null) {
            val columnIndex = cursor
                .getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        } else
            null

    }
}