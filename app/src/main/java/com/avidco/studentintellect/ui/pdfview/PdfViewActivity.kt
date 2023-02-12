package com.avidco.studentintellect.ui.pdfview

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.avidco.studentintellect.R
import com.avidco.studentintellect.databinding.ActivityPdfViewBinding
import com.avidco.studentintellect.models.MaterialData
import com.avidco.studentintellect.utils.Utils.documentsDir
import com.avidco.studentintellect.utils.Utils.getAdSize
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.avidco.studentintellect.BuildConfig
import com.avidco.studentintellect.auth.AuthActivity
import com.avidco.studentintellect.utils.Utils.imagesDir
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.*


class PdfViewActivity : AppCompatActivity() {

    lateinit var binding: ActivityPdfViewBinding
    private lateinit var materialData: MaterialData
    private lateinit var moduleCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finishAffinity()
            return
        }

        binding = ActivityPdfViewBinding.inflate(layoutInflater)
        materialData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.extras?.getParcelable(MATERIAL_DATA, MaterialData::class.java)!!
        } else {
            intent.extras?.getParcelable(MATERIAL_DATA)!!
        }
        moduleCode = intent.extras?.getString(MODULE_CODE)!!

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = materialData.id + " | " + moduleCode

        binding.viewPager.adapter = SectionsPagerAdapter( materialData , moduleCode,this)
        binding.viewPager.offscreenPageLimit = 2

        if (materialData.withSolutions){
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = if (position == 0) "Questions" else "Solutions"
            }.attach()
            binding.viewPager.isUserInputEnabled = false
        } else {
            binding.tabLayout.visibility = View.GONE
            binding.viewPager.isUserInputEnabled = false
        }

        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        val adView = AdView(this)
        adView.adUnitId = getString(R.string.activity_pdf_view_bannerAdUnitId)
        binding.adViewContainer.addView(adView)
        // Since we're loading the banner based on the adContainerView size, we need
        // to wait until this view is laid out before we can get the width.
        var initialLayoutComplete = false
        binding.adViewContainer.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                initialLayoutComplete = true
                adView.setAdSize(getAdSize(binding.adViewContainer))
                adView.loadAd(adRequest)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    var menuItem : MenuItem? = null
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.pdf_menu, menu)
        this.menuItem = menu.findItem(R.id.item_menu)
        return true
    }

    private lateinit var file : File
    private var i = 0
    private fun searchFile(file :File, directory: File, function : () -> Unit){
        if (file.exists() && file.length() != 0L){
            if (file.canRead()) {
                i=0
                function()
            } else {
                i++
                this.file = if (binding.viewPager.currentItem == 0) File(directory, "${materialData.id}($i).pdf")
                else File(directory, "${materialData.id} Solutions($i).pdf")
                searchFile(this.file, directory) { function() }
            }
        }
        else {
            i=0
            Toast.makeText(this, "Error: Pdf file not found.", Toast.LENGTH_SHORT).show()
        }
    }

    private var showMenu = true
    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_menu -> {
                if (showMenu){
                    showMenuDialog()
                    showMenu = false
                    Handler(Looper.getMainLooper()).postDelayed({
                        showMenu = true
                    }, 1000)
                }
            }
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun showMenuDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.menu_pdf_activity_bottomsheet)
        val crop = dialog.findViewById<LinearLayout>(R.id.crop)
        val export = dialog.findViewById<LinearLayout>(R.id.export)
        val shareDoc = dialog.findViewById<LinearLayout>(R.id.share_doc)
        crop.setOnClickListener {
            it.tempDisable()
            dialog.dismiss()
            startCroppingPdf()
        }
        export.setOnClickListener {
            it.tempDisable()
            dialog.dismiss()
            try {
                val path = "modules/$moduleCode/materials/${materialData.id}" 
                
                file = if (binding.viewPager.currentItem == 0) File(documentsDir(moduleCode), "${materialData.id}.pdf")
                else File(documentsDir(moduleCode), "${materialData.id} Solutions.pdf")
                searchFile(file, documentsDir(moduleCode)) {
                    val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)
                    val pdfOpenIntent = Intent(Intent.ACTION_VIEW)
                    pdfOpenIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    pdfOpenIntent.clipData = ClipData.newRawUri("", uri)
                    pdfOpenIntent.setDataAndType(uri, "application/pdf")
                    pdfOpenIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    startActivity(pdfOpenIntent)
                }
            } catch (activityNotFoundException: ActivityNotFoundException) {
                Toast.makeText(this, "There is no app to export the PDF", Toast.LENGTH_LONG)
                    .show()
            }
        }
        shareDoc.setOnClickListener {
            it.tempDisable()
            dialog.dismiss()
            try {
                val path = "modules/$moduleCode/materials/${materialData.id}"
                file = if (binding.viewPager.currentItem == 0) File(documentsDir(moduleCode), "${materialData.id}.pdf")
                else File(documentsDir(path), "${materialData.id} Solutions.pdf")
                searchFile(file, documentsDir(moduleCode)) {
                    val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.type = "application/pdf"
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
        dialog.window!!.setGravity(Gravity.BOTTOM)
    }

    private fun startCroppingPdf() {
        val view = binding.viewPager
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        //binding.cropImageView.setImageUriAsync(uri)
        binding.cropImageView.setImageBitmap(bitmap)
        binding.cropImageView.guidelines = CropImageView.Guidelines.ON
        binding.cropImageView.scaleType = CropImageView.ScaleType.FIT_CENTER
        binding.cropImageView.isAutoZoomEnabled = true
        binding.cropImageView.isShowProgressBar = true


        binding.viewPager.visibility = View.GONE
        binding.cropViewLayout.visibility = View.VISIBLE
        binding.cancelCroppingButton.setOnClickListener {
            it.tempDisable()
            binding.cropViewLayout.visibility = View.GONE
            binding.viewPager.visibility = View.VISIBLE
            binding.cropImageView.setImageBitmap(null)
            binding.cropImageView.setOnCropImageCompleteListener(null)
            binding.saveCroppedImageButton.setOnClickListener(null)
            binding.cancelCroppingButton.setOnClickListener(null)
        }

        var share = false
        binding.cropImageView.setOnCropImageCompleteListener { _, result ->
            val filename = File(imagesDir(), "/${materialData.id}_${System.currentTimeMillis()}.png")
            try {
                val fos = FileOutputStream(filename)
                result.bitmap.compress(CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()
                Toast.makeText(this,"Saved to DCIM/Student Intellect", Toast.LENGTH_LONG)
                    .apply { setGravity(Gravity.TOP, 0, 0); show() }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (share){
                try {
                    val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", filename)
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.type = "image/png"
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(Intent.createChooser(intent, "Share"))
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
            }

            binding.cropViewLayout.visibility = View.GONE
            binding.viewPager.visibility = View.VISIBLE
            binding.cropImageView.setImageBitmap(null)
            binding.cropImageView.setOnCropImageCompleteListener(null)
            binding.saveCroppedImageButton.setOnClickListener(null)
            binding.cancelCroppingButton.setOnClickListener(null)
        }

        binding.saveCroppedImageButton.setOnClickListener {
            it.tempDisable()
            binding.cropImageView.getCroppedImageAsync()
        }
        binding.shareCroppedImageButton.setOnClickListener {
            it.tempDisable()
            share = true
            binding.cropImageView.getCroppedImageAsync()
        }
    }

    companion object {
        const val MATERIAL_DATA = "arg_material_data"
        const val MODULE_CODE = "arg_module_code"
    }
}