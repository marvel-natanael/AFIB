package com.example.afib.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiConfig {
    val api =
        Retrofit.Builder().baseUrl("http://192.168.1.5:8080").addConverterFactory(GsonConverterFactory.create()).build()
            .create(ApiService::class.java)
}