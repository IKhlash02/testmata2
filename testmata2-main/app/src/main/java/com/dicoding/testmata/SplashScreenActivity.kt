package com.dicoding.testmata

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.testmata.R
import com.dicoding.testmata.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)
        supportActionBar?.hide()

        // Show splash screen for 3 seconds before transitioning to MainActivity
        CoroutineScope(Dispatchers.Main).launch {
            delay(3000L)
            startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
            finish()
        }
    }
}