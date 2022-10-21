package com.example.afib.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.afib.databinding.ActivityResultHaveAfibactivityBinding

class ResultHaveAFIBActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultHaveAfibactivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultHaveAfibactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}