package com.avidco.studentintellect.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import com.avidco.studentintellect.R
import com.avidco.studentintellect.databinding.FragmentPdfBinding
import com.avidco.studentintellect.utils.pdfviewer.scroll.MyScrollHandle
import com.avidco.studentintellect.utils.pdfviewer.util.FitPolicy
import com.avidco.studentintellect.activities.ui.MainActivity
import com.google.android.gms.ads.AdSize
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object Utils {

    fun Context.isGooglePlayServicesAvailable() : Boolean {
        return try {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
        } catch (e : Exception){
            false
        }
    }

    fun Activity.openPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.avidco.studentintellect")
            intent.setPackage("com.android.vending")
            startActivity(intent)
        } catch (e : Exception){
            Toast.makeText(this, "Play Store is not found in your device.", Toast.LENGTH_SHORT).show()
        }
    }

    fun Activity.askPlayStoreRatings() {
        val manager = ReviewManagerFactory.create(this)
        val request: Task<ReviewInfo> = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // We can get the ReviewInfo object
                val reviewInfo: ReviewInfo = task.result
                val flow: Task<Void> = manager.launchReviewFlow(this, reviewInfo)
                flow.addOnCompleteListener {
                    val prefs = getSharedPreferences(RATER_KEY, Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(IS_RATED_KEY, true).apply()
                }
            } else {
                // There was some problem, continue regardless of the result.
                openPlayStore()
            }
        }
    }

    fun Double.roundToRand(): String {
        val number : Int = (this * 100f).roundToInt()
        if (number == 0){ return "0.00" }
        val wholeNum = number/100
        val decNum = number.toString().padStart(2,'0').takeLast(2)

        return "$wholeNum.$decNum"
    }

    fun Int.pxToDp() : Float = (this / Resources.getSystem().displayMetrics.density)
    fun Int.dpToPx() : Float = (this * Resources.getSystem().displayMetrics.density)

    fun View.tempDisable(delayMillis : Long = 1000){
        isEnabled = false
        Handler(Looper.getMainLooper()).postDelayed({
            isEnabled = true
        }, delayMillis)
    }

    fun FragmentPdfBinding.loadPdf(file: File, onError : () -> Unit, onLoad : () -> Unit, page : Int = 0) {
        pdfView.fromFile(file)
            .enableSwipe(true)
            .enableDoubletap(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .enableAnnotationRendering(true)
            .defaultPage(page)
            .onLoad {
               // progressLayout.visibility = View.GONE
                try { onLoad() } catch (_: Exception) {}
            }
            .onError {
                Toast.makeText(pdfView.context, it.message, Toast.LENGTH_SHORT).show()
                if (file.delete())
                    try { onError() } catch (_: Exception) {}
            }
            .onTap { false }
            .enableAntialiasing(true)
            .spacing(0)
            .password(null)
            .scrollHandle(object : MyScrollHandle(pdfView.context){})
            .autoSpacing(false)
            .pageFitPolicy(FitPolicy.WIDTH)
            .fitEachPage(true)
            .nightMode(pdfView.context.getString(R.string.night_mode) == "on")
            .load()
    }

    ///Directories
    fun documentsDir(folderPath : String) : File {
        val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS),"Student Intellect")
        else
            File(Environment.getExternalStorageDirectory(), "Student Intellect/Documents")

        return File(directory.path+"/"+folderPath).apply {
            if (!exists()) mkdirs()
        }
    }
    fun imagesDir() : File {
        return File(
            Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM),"Student Intellect").apply { if (!exists()) mkdirs() }
    }

    fun makeFolderIfNotExists(folderPath : String, folderName: String) {
        File(documentsDir(folderPath), folderName).apply {
            if (!exists()) mkdirs()
        }
    }
    fun fileExists(folderPath : String, fileName: String, i : Int = 0): Boolean {
        val file = if (i == 0) File(documentsDir(folderPath), "$fileName.pdf")
        else File(documentsDir(folderPath), "$fileName($i).pdf")

        return if (file.exists() && file.length() != 0L) {
            if (file.canRead()){
                true
            } else {
                fileExists(folderPath, fileName, i+1)
            }
        } else {
            false
        }
    }

    fun Context.isOnline() : Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {

                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

                /*if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    println("Internet, NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                }
                else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    println("Internet, NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                }
                else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    println("Internet, NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }*/
            }
        } else
        @Suppress("DEPRECATION")
        {
            //println("Internet, connectivityManager.activeNetworkInfo")
            val netInfo = connectivityManager.activeNetworkInfo
            return netInfo != null && netInfo.isConnectedOrConnecting
        }
        return false
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // Determine the screen width (less decorations) to use for the ad width.
    fun Activity.getAdSize(adViewContainer : FrameLayout): AdSize {
        val width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            bounds.width()
        } else @Suppress("DEPRECATION") {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.widthPixels
        }

        var adWidthPixels = adViewContainer.width
        // If the ad hasn't been laid out, default to the full screen width.
        if (adWidthPixels == 0) {
            adWidthPixels = width
        }

        val density = resources.displayMetrics.density
        val adWidth = (adWidthPixels.toFloat() / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    private const val DAYS_UNTIL_PROMPT = 1L//Min number of days
    private const val LAUNCHES_UNTIL_PROMPT = 3//Min number of launches
    private const val RATER_KEY = "rater_key"
    private const val IS_RATED_KEY = "is_rated_key"
    private const val LAUNCH_COUNTER_KEY = "launch_counter_key"
    private const val FIRST_LAUNCH_KEY_TIME = "first_launch_key_time"
    fun Context.appRateCheck(rateApp: () -> Unit, appRated: () -> Unit) {
        val prefs = getSharedPreferences(RATER_KEY, Context.MODE_PRIVATE)
        val launchesCounter = prefs.getLong(LAUNCH_COUNTER_KEY, 0) + 1
        prefs.edit().putLong(LAUNCH_COUNTER_KEY, launchesCounter).apply()

        var firstLaunchTime = prefs.getLong(FIRST_LAUNCH_KEY_TIME, 0)
        if (firstLaunchTime == 0L) {
            firstLaunchTime = System.currentTimeMillis()
            prefs.edit().putLong(FIRST_LAUNCH_KEY_TIME, firstLaunchTime).apply()
        }

        if (launchesCounter >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= (firstLaunchTime + TimeUnit.DAYS.toMillis(DAYS_UNTIL_PROMPT))) {
                appRatedCheck({appRated()}, {rateApp()})
            }
        }
    }
    fun Context.appRatedCheck(appRated: () -> Unit,rateApp: () -> Unit) {
        val prefs = getSharedPreferences(RATER_KEY, Context.MODE_PRIVATE)
        if (prefs.getBoolean(IS_RATED_KEY, false)) {
            appRated()
        } else {
            rateApp()
        }
    }

    fun Activity.shareApp() {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_TEXT, "If you are one of those students who want to make their life easier, this app is created just for you. Download past exam papers, answer guides, lecture notes, and other study materials for Universities.\nStudent Intellect is a simple-to-use study tool created for students.\n\nDownload Student Intellect on\nGoogle Play https://play.google.com/store/apps/details?id=com.avidco.studentintellect or\nApp Gallery https://appgallery.cloud.huawei.com/appDetail?pkgName=com.avidco.studentintellect".trimMargin())
            intent.type = "text/plain"
            startActivity(Intent.createChooser(intent, "Share the App"))
        } catch (e : Exception){
            Toast.makeText(this, "An error occurred.", Toast.LENGTH_SHORT).show()
        }
    }
}