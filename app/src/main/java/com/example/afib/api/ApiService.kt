package com.example.afib.api

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PartMap

interface ApiService {
    @Multipart
    @POST("/")
    fun uploadVideo(
        @PartMap map: HashMap<String, @JvmSuppressWildcards RequestBody>
    ): Call<ResponseData>
}