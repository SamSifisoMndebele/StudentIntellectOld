package com.avidco.studentintellect.activities.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.MenuItemCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.auth.AuthActivity
import com.avidco.studentintellect.activities.ui.profile.FeedbackBottomSheetDialog
import com.avidco.studentintellect.databinding.ActivityMainBinding
import com.avidco.studentintellect.utils.Utils.appRateCheck
import com.avidco.studentintellect.utils.Utils.askPlayStoreRatings
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.isGooglePlayServicesAvailable
import com.avidco.studentintellect.utils.Utils.shareApp
import com.bumptech.glide.Glide
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var navController : NavController
    private lateinit var binding: ActivityMainBinding
    companion object{
        private const val UPDATE_REQUEST_CODE = 12
    }
    private var appUpdateManager : AppUpdateManager? = null
    private var exit = false
    private lateinit var currentUser : FirebaseUser


    private var interstitialAd : InterstitialAd? = null
    internal fun loadInterstitialAd() {
        InterstitialAd.load(this, getString(R.string.activity_modulesSelectList_interstitialAdUnitId),
            AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    println("Ad was loaded.")
                    interstitialAd = ad

                    interstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                        override fun onAdClicked() {
                            // Called when a click is recorded for an ad.
                            println( "Ad was clicked.")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            // Called when ad is dismissed.
                            // Set the ad reference to null so you don't show the ad a second time.
                            println( "Ad dismissed fullscreen content.")
                            //startActivity(Intent(activity, ModulesSelectActivity::class.java))
                            loadInterstitialAd()
                            navController.navigate(R.id.addModulesFragment)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            // Called when ad fails to show.
                            println( "Ad failed to show fullscreen content.")
                            loadInterstitialAd()
                            navController.navigate(R.id.addModulesFragment)
                        }

                        override fun onAdImpression() {
                            // Called when an impression is recorded for an ad.
                            println( "Ad recorded an impression.")
                        }

                        override fun onAdShowedFullScreenContent() {
                            // Called when ad is shown.
                            println( "Ad showed fullscreen content.")
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }
            })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        currentUser = Firebase.auth.currentUser!!

        val prefs = getSharedPreferences("pref", Context.MODE_PRIVATE)
        val modulesViewModel = ViewModelProvider(this)[DataViewModel::class.java]
        modulesViewModel.init(prefs)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.contentMain.toolbar)
        //Theme
        val color = SurfaceColors.SURFACE_1.getColor(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = color
        window.navigationBarColor = color
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))

        /**Check updates*/
        if (isGooglePlayServicesAvailable()) {
            appUpdateManager = AppUpdateManagerFactory.create(this)
            checkUpdateAvailability()
        }
        /**Check app rate*/
        if (isGooglePlayServicesAvailable()) {
            appRateCheck ({
                askPlayStoreRatings()
            },{})
        } else {
            //Hide google services
        }




        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navView.itemIconTintList = null
        navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_materials, R.id.nav_materials_upload
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        /**Back method*/
        onBackPressedMethod()

        //Profile Header
        val headerView = navView.getHeaderView(0)
        Glide.with(this)
            .load(currentUser.photoUrl)
            .circleCrop()
            .into(headerView.findViewById(R.id.user_image))
        headerView.findViewById<TextView>(R.id.user_name).text = currentUser.displayName
        headerView.findViewById<TextView>(R.id.user_email).text = currentUser.email
        headerView.findViewById<CardView>(R.id.profile_layout).setOnClickListener {
            navController.navigate(R.id.profileFragment)
            drawerLayout.close()
        }

        navView.menu.findItem(R.id.action_logout).setOnMenuItemClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.logout_question))
                .setMessage(getString(R.string.confirm_logout_text))
                .setNeutralButton(getString(R.string.cancel)){d,_->
                    d.dismiss()
                }
                .setPositiveButton(getString(R.string.logout)){d,_->
                    d.dismiss()
                    drawerLayout.close()
                    Firebase.auth.signOut()
                    startActivity(Intent(this, AuthActivity::class.java))
                    finishAffinity()
                }
                .show()
            true
        }
        /*val timetable = navView.menu.findItem(R.id.action_timetable).actionView as TextView
        timetable.gravity = Gravity.CENTER_VERTICAL;
        //timetable.setTypeface(null,Typeface.BOLD);
        timetable.setTextColor(ResourcesCompat.getColor(resources, R.color.primaryVariantColor, theme));
        timetable.text = "Coming Soon";*/
        val timetable = navView.menu.findItem(R.id.action_timetable).actionView as ImageView
        timetable.setImageResource(R.drawable.ic_coming_soon)

        /**Init ads*/
        adsInit()
    }

    private fun adsInit(){
        MobileAds.initialize(this) {
            loadInterstitialAd()
            val adRequest = AdRequest.Builder().build()
            binding.contentMain.adView.loadAd(adRequest)
            binding.contentMain.adView.adListener = object: AdListener() {
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
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        if (menu is MenuBuilder) menu.setOptionalIconsVisible(true)
        if (!isGooglePlayServicesAvailable()) {
            menu.findItem(R.id.item_rate).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.item_modules -> {
                if (interstitialAd == null) {
                    navController.navigate(R.id.addModulesFragment)
                } else {
                    interstitialAd!!.show(this)
                }
            }
            R.id.item_share -> {
                shareApp()
            }
            R.id.item_rate -> {
                askPlayStoreRatings()
            }
            R.id.item_feedback -> {
                val sheet = FeedbackBottomSheetDialog()
                sheet.show(supportFragmentManager, "FeedbackBottomSheetDialog")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        hideKeyboard(binding.root)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun onBackPressedMethod() {
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                if (!navController.navigateUp()){
                    if (exit) {
                        finish()
                    } else {
                        exit = true
                        Toast.makeText(this, "Tab again to exit", Toast.LENGTH_SHORT).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            exit = false
                        }, 5000)
                    }
                }
            }
        } else {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    val navController = findNavController(R.id.nav_host_fragment_content_main)
                    if (!navController.navigateUp()){
                        if (exit) {
                            finish()
                        } else {
                            exit = true
                            Toast.makeText(this@MainActivity, "Tab again to exit", Toast.LENGTH_SHORT).show()
                            Handler(Looper.getMainLooper()).postDelayed({
                                exit = false
                            }, 5000)
                        }
                    }
                }
            })
        }
    }

    private fun checkUpdateAvailability() {
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager?.appUpdateInfo

        // Checks whether the platform allows the specified type of update,
        // and current version staleness.
        appUpdateInfoTask?.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                // Request an immediate update.
                appUpdateManager?.startUpdateFlowForResult(appUpdateInfo,
                    AppUpdateType.IMMEDIATE, this, UPDATE_REQUEST_CODE
                )
            }
        }
    }

    // Checks that the update is not stalled during 'onResume()'.
    override fun onResume() {
        super.onResume()
        appUpdateManager?.appUpdateInfo
            ?.addOnSuccessListener { appUpdateInfo ->
                // notify the user to complete the update.
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager?.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this,
                        UPDATE_REQUEST_CODE
                    )
                }
            }
    }
}