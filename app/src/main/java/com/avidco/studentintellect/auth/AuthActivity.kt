package com.avidco.studentintellect.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.avidco.studentintellect.R
import com.avidco.studentintellect.databinding.ActivityAuthBinding
import com.avidco.studentintellect.ui.MainActivity
import com.avidco.studentintellect.utils.Utils.getAdSize
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class AuthActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAuthBinding

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

        // Show OnBoardingActivity once
        /*if (*//*!getSharedPreferences("on_boarding_pref", MODE_PRIVATE).getBoolean("welcomed", false)*//*!intent.getBooleanExtra("to_auth", false)) {
            startActivity(Intent(this, OnBoardingActivity::class.java))
            finish()
        }*/

        val navController = findNavController(R.id.nav_host_fragment_content_auth)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        val adView = AdView(this)
        adView.adUnitId = getString(R.string.activity_auth_bannerAdUnitId)
        binding.adViewContainer.addView(adView)
        // Since we're loading the banner based on the adContainerView size, we need
        // to wait until this view is laid out before we can get the width.
        var initialLayoutComplete = false
        binding.adViewContainer.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                initialLayoutComplete = true
                adView.setAdSize(getAdSize(binding.adViewContainer))
                Handler(Looper.getMainLooper()).postDelayed({
                    adView.loadAd(adRequest)
                }, 1000)
            }
        }
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