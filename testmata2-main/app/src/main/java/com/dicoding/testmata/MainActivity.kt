package com.dicoding.testmata

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.dicoding.testmata.CameraActivity.Companion.CAMERAX_RESULT
import com.dicoding.testmata.databinding.ActivityMainBinding
import com.dicoding.testmata.ml.KeratitisMetadata
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.InputStream
import java.text.NumberFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private var currentImageUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){ isGranted: Boolean ->
        if (isGranted){
            showMessage("Permission request granted")
        } else {
            showMessage("Permission request denied")
        }

    }

    private fun allPermissionGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setActionBar()
        if(!allPermissionGranted()){
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }

        binding.btnGallery.setOnClickListener { startGallery() }
        binding.btnCamera.setOnClickListener { startCameraX() }

    }

    fun setActionBar(isDarkMode: Boolean = false){
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_about -> {
                // Intent to open a web page
                val openURL = Intent(Intent.ACTION_VIEW)
                openURL.data = Uri.parse("https://www.tesmata.com/tentang-kami/")
                startActivity(openURL)
                true
            }

            R.id.action_privacy -> {
                // Intent to open a web page
                val openURL = Intent(Intent.ACTION_VIEW)
                openURL.data = Uri.parse("https://www.tesmata.com/kebijakan-privasi/")
                startActivity(openURL)
                true
            }

            R.id.action_term -> {
                // Intent to open a web page
                val openURL = Intent(Intent.ACTION_VIEW)
                openURL.data = Uri.parse("https://www.tesmata.com/syarat-ketentuan/")
                startActivity(openURL)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startCameraX() {
        val intent = Intent(this, CameraActivity:: class.java)
        launcherIntentCameraX.launch(intent)
    }

    private val launcherIntentCameraX = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == CAMERAX_RESULT){
            currentImageUri = it.data?.getStringExtra(CameraActivity.EXTRA_CAMERAX_IMAGE)?.toUri()
            showImage()
        }
    }

    private fun startGallery(){
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    )   {uri: Uri? ->
        if(uri != null){
            currentImageUri = uri
            showImage()
        } else {
            Log.d("Photo Picker", "no Media Selected")
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d(TAG, "showImage: $it")
            binding.previewImageView.setImageURI(it)
            binding.cardImage.visibility = View.VISIBLE
            imageClassifierHelper = ImageClassifierHelper(
                context = this,
                classifierListener = object : ImageClassifierHelper.ClassifierListener{
                    override fun onError(error: String) {
                        Log.d(TAG, "message: $error")
                    }

                    override fun onResults(results: List<Classifications>?, infereceTime: Long) {
                        results?.let {
                            if (it.isNotEmpty() && it[0].categories.isNotEmpty()){
                                val result = it[0].categories[0]
                                val diagnoisis = if (result.label == "Class_A_nonkeratitis") "Non Keratitis" else "Keratitis"
                                binding.tvResult.text = diagnoisis
                                binding.tvPercent.text = "${result.score}"
                                binding.cardResult.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            )

            imageClassifierHelper.classifyImage(it)
        }
    }

    private fun imageAnalyze(bitmap: Bitmap) {
        Log.d(TAG, "analisis gais")

        val model = KeratitisMetadata.newInstance(this)

        val image = TensorImage.fromBitmap(bitmap)

        val outputs = model.process(image)
        val probability = outputs.probabilityAsCategoryList

        Log.d(TAG, probability.toString())

        model.close()
    }


    private fun showMessage(message: String){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
        private const val TAG = "MainActivity"
    }
}