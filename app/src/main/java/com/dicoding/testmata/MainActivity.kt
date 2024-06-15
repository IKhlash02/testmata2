package com.dicoding.testmata

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager

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
import com.dicoding.testmata.helper.DateHelper
import com.dicoding.testmata.helper.ImageClassifierHelper
import com.yalantis.ucrop.UCrop
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File
import kotlin.math.round

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
    ) { it ->
        if (it.resultCode == CAMERAX_RESULT){
            val resultImage  = it.data?.getStringExtra(CameraActivity.EXTRA_CAMERAX_IMAGE)?.toUri()
            resultImage?.let {
                startCrop(it)
            }

//           currentImageUri = it.data?.getStringExtra(CameraActivity.EXTRA_CAMERAX_IMAGE)?.toUri()
//            showImage()
        }
    }

    private fun startGallery(){
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    )   {uri: Uri? ->
        if(uri != null){
          startCrop(uri)
        } else {
            Log.d("Photo Picker", "no Media Selected")
        }
    }

    private fun showImage() {
        currentImageUri?.let {uri ->
            Log.d(TAG, "showImage: $uri")
            binding.previewImageView.setImageURI(uri)
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
                                val date = DateHelper.getCurrentDate()
                                val result = it[0].categories[0]
                                val diagnoisis = if (result.label == "Class_A_nonkeratitis") "Non Keratitis" else "Keratitis"
                                binding.tvResult.text = diagnoisis
                                binding.tvPercent.text = "${round(result.score).toInt()}%"
                                binding.cardResult.visibility = View.VISIBLE

                                moveToResult(diagnoisis, result.score, uri , date)

                            }
                        }
                    }
                }
            )

            imageClassifierHelper.classifyImage(uri)
        }
    }

    private fun moveToResult(result: String, score:Float,  imageUri: Uri, date: String) {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra(EXTRA_IMAGE, imageUri.toString())
        intent.putExtra(EXTRA_RESULT, result)
        intent.putExtra(EXTRA_SCORE, score)
        intent.putExtra(EXTRA_DATE, date)
        startActivity(intent)
    }

    private fun showMessage(message: String){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "IMG_${System.currentTimeMillis()}"))
        val option = UCrop.Options()
        option.setCompressionQuality(80)

        UCrop.of(sourceUri, destinationUri)
            .withOptions(option)
            .start(this)

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK && data != null) {
            val resultUri = UCrop.getOutput(data)
            currentImageUri = resultUri
            showImage()
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            cropError?.printStackTrace()
        }
    }

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
        private const val TAG = "MainActivity"
        const val EXTRA_IMAGE = "extra_image"
        const val EXTRA_RESULT = "extra_result"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_SCORE = "extra_score"
    }
}