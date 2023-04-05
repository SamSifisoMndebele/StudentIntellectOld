package com.avidco.studentintellect.activities.auth

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.avidco.studentintellect.R
import com.avidco.studentintellect.databinding.ActivityAuthBinding
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.utils.Utils.getAdSize
import com.google.android.gms.ads.*
import com.google.android.material.elevation.SurfaceColors
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class AuthActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAuthBinding
    var isOnline = MutableLiveData<Boolean>()


    private fun setupNetworkListener() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                runOnUiThread {
                    isOnline.value = true
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                runOnUiThread {
                    isOnline.value = false
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        // Check if user is signed in (non-null) and update UI accordingly.
        if(Firebase.auth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        //Theme
        val color = SurfaceColors.SURFACE_1.getColor(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = color
        window.navigationBarColor = color
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))

        setupNetworkListener()
        // Show OnBoardingActivity once
        /*if (*//*!getSharedPreferences("on_boarding_pref", MODE_PRIVATE).getBoolean("welcomed", false)*//*!intent.getBooleanExtra("to_auth", false)) {
            startActivity(Intent(this, OnBoardingActivity::class.java))
            finish()
        }*/

        val navController = findNavController(R.id.nav_host_fragment_content_auth)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        adsInit()
    }


    private fun adsInit() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
        binding.adView.adListener = object: AdListener() {
            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }

            override fun onAdFailedToLoad(adError : LoadAdError) {
                // Code to be executed when an ad request fails.
            }

            override fun onAdImpression() {
                // Code to be executed when an impression is recorded
                // for an ad.
            }

            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            override fun onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
            }
        }


        /*val bannerView = AdView(this)
        //val adSize = AdSize(300, 50)
        val adSize = AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(this, 320)
        bannerView.adUnitId = getString(R.string.activity_main_bannerAdUnitId)
        bannerView.setAdSize(adSize)
        bannerView.loadAd(adRequest)
        binding.contentMain.root.addView(bannerView)*/
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        if(Firebase.auth.currentUser != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_auth)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}