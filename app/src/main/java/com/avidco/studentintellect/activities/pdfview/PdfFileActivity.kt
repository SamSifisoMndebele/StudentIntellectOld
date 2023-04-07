package com.avidco.studentintellect.activities.pdfview

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.LayoutDirection
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.avidco.studentintellect.BuildConfig
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.auth.AuthActivity
import com.avidco.studentintellect.databinding.ActivityPdfFileBinding
import com.avidco.studentintellect.models.Folder
import com.avidco.studentintellect.models.Module
import com.avidco.studentintellect.models.PdfFile
import com.avidco.studentintellect.utils.Utils
import com.avidco.studentintellect.utils.Utils.getAdSize
import com.avidco.studentintellect.utils.Utils.isGooglePlayServicesAvailable
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.File

class PdfFileActivity : AppCompatActivity() {

    companion object {
        const val ARG_PDF_FILE = "arg_pdf_file"
        const val ARG_PARENT_FOLDER = "arg_parent_folder"
    }

    private lateinit var binding: ActivityPdfFileBinding
    private lateinit var pdfFile: PdfFile
    private lateinit var parentFolder: Folder

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_PDF_FILE, pdfFile)
        outState.putParcelable(ARG_PARENT_FOLDER, parentFolder)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        if (Firebase.auth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finishAffinity()
            return
        }
        binding = ActivityPdfFileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //Theme
        val color = SurfaceColors.SURFACE_1.getColor(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = color
        window.navigationBarColor = color
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))

        val extras = savedInstanceState ?: intent.extras
        pdfFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras?.getParcelable(ARG_PDF_FILE, PdfFile::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            extras?.getParcelable(ARG_PDF_FILE)!!
        }
        parentFolder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(ARG_PARENT_FOLDER, Folder::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(ARG_PARENT_FOLDER)!!
        }

        val sectionsPagerAdapter = SectionsPagerAdapter(pdfFile, parentFolder, this)
        binding.viewPager.adapter = sectionsPagerAdapter
        binding.viewPager.offscreenPageLimit = 2

        if (pdfFile.solutionsUrl.isNullOrEmpty()) {
            supportActionBar?.title = pdfFile.name
            binding.tabLayout.visibility = View.GONE
            binding.viewPager.isUserInputEnabled = false
        } else {
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                if (position == 0) {
                    tab.setIcon(R.drawable.ic_open_book)
                } else {
                    tab.text = "Solutions"
                }
            }.attach()
            supportActionBar?.title = pdfFile.name
            binding.tabLayout.visibility = View.VISIBLE
            binding.tabLayout.setBackgroundColor(color)
            binding.viewPager.isUserInputEnabled = true
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

    private lateinit var file : File
    private var i = 0
    private fun searchFile(file : File, directory: File, function : () -> Unit){
        if (file.exists() && file.length() != 0L){
            if (file.canRead()) {
                i=0
                function()
            } else {
                i++
                this.file = if (binding.viewPager.currentItem == 0) File(directory, "${pdfFile.name}($i).pdf")
                else File(directory, "${pdfFile.name} Solutions($i).pdf")
                searchFile(this.file, directory) { function() }
            }
        }
        else {
            i=0
            Toast.makeText(this, "Error: Pdf file not found.", Toast.LENGTH_SHORT).show()
        }
    }


    var menu : Menu? = null
    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.menu_pdf, menu)
        if (menu is MenuBuilder) menu.setOptionalIconsVisible(true)
        menu.setGroupVisible(0, false)
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.item_export -> {
                /*try {
                    val path = "modules/${Folder.code}/materials/${pdfFile.name}"

                    file = if (binding.viewPager.currentItem == 0) File(
                        Utils.documentsDir(
                            module.code
                        ), "${pdfFile.name}.pdf")
                    else File(Utils.documentsDir(module.code), "${pdfFile.name} Solutions.pdf")
                    searchFile(file, Utils.documentsDir(module.code)) {
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
                }*/
            }
            R.id.item_share -> {
                /*try {
                    val path = "modules/${module.code}/materials/${pdfFile.name}"
                    file = if (binding.viewPager.currentItem == 0) File(
                        Utils.documentsDir(
                            module.code
                        ), "${pdfFile.name}.pdf")
                    else File(Utils.documentsDir(path), "${pdfFile.name} Solutions.pdf")
                    searchFile(file, Utils.documentsDir(module.code)) {
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
                }*/
            }
            R.id.item_feedback -> {

            }
        }
        return super.onOptionsItemSelected(menuItem)
    }


    /*private fun startCroppingPdf() {
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
            val filename = File(imagesDir(), "/${pdfFile.name}_${System.currentTimeMillis()}.png")
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
    }*/
}