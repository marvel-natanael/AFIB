package com.example.afib.ui

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.afib.databinding.ActivityScanBinding

class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding

    var i = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.scan.setOnClickListener  {
            binding.bgCircle.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
            val handler = Handler()
            handler.postDelayed(object : Runnable {
                override fun run() {
                    // set the limitations for the numeric
                    // text under the progress bar
                    if (i <= 100) {
                        binding.progressText.text = "Memindai\n$i %"
                        binding.progressBar.progress = i
                        i++
                        handler.postDelayed(this, 200)
                    } else {
                        handler.removeCallbacks(this)
                    }
                }
            }, 200)
        }
    }
}