package com.dicoding.testmata

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dicoding.testmata.databinding.ActivityResultBinding
import java.text.NumberFormat
import kotlin.math.round

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setActionBar()


        //Menampilkan hasil gambar, prediksi, dan confidence score.
        val imageUri = Uri.parse(intent.getStringExtra(MainActivity.EXTRA_IMAGE))
        val result = intent.getStringExtra(MainActivity.EXTRA_RESULT)
        val score = intent.getFloatExtra(MainActivity.EXTRA_SCORE, 0F)
        val date = intent.getStringExtra(MainActivity.EXTRA_DATE)

        val displayResult = "$result " + round(score).toInt() + "%"

        binding.resultImage.setImageURI(imageUri)
        binding.resultText.text = displayResult
        binding.tvDate.text = date

        Log.d("resultActivity", result!!)

        if (result.contains("Non Keratitis")) {
            binding.textMessage1.text = getString(R.string.non_keratitis_1)
            binding.textMessage2.text = getString(R.string.non_keratitis_2)
        } else {
            binding.textMessage1.text = getString(R.string.keratitis_1)
            binding.textMessage2.text = getString(R.string.keratitis_2)
        }

    }

    private fun setActionBar(isDarkMode: Boolean = false){
        supportActionBar?.setCustomView(R.layout.app_bar)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val color = if (isDarkMode) {
            R.color.primary_dark // warna untuk dark mode
        } else {
            R.color.primary_light // warna untuk light mode
        }

        supportActionBar?.setBackgroundDrawable(ColorDrawable(getColor(color)))
    }
}