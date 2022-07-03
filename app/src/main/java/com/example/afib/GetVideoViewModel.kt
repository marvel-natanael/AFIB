package com.example.afib

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class GetVideoViewModel : ViewModel() {

    private var retriever: MediaMetadataRetriever = MediaMetadataRetriever()

    private var bitmap: MutableLiveData<Bitmap> = MutableLiveData()
    val listBitmap = MutableLiveData<ArrayList<Bitmap>>()

    fun setFramesVideo(selectedVideoPath: String) {
        retriever.setDataSource(selectedVideoPath)
        var time =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()

        var seconds = TimeUnit.MILLISECONDS.toSeconds(time)
        viewModelScope.launch {
            val list = ArrayList<Bitmap>()

            for (i in 0 until seconds) {
                launch {
                    var k = i * 1000
                    while(k <= (1000 * (i + 1))) {
                        //    val length = (time / seconds)
                        //    while (time >= length) {
                        bitmap.value = retriever.getFrameAtTime(k)!!

                        list.add(bitmap.value!!)
                        k += 100
                        //         }
                        // time = length.toLong()
                    }
                    listBitmap.value = list
                }.join()
            }
           /* launch {
                val length = (time / 1.2)
                while (time >= length) {
                    bitmap.value = retriever.getFrameAtTime(33)!!

                    list.add(bitmap.value!!)
                    time -= 33
                }
                time = length.toLong()
                listBitmap.value = list
            }.join()*/
        }
    }

    override fun onCleared() {
        super.onCleared()
        retriever.release()
    }
}