package com.avidco.studentintellect.activities.ui

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.*
import android.os.*
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.cardview.widget.CardView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.auth.AuthActivity
import com.avidco.studentintellect.activities.ui.database.UserDatabase
import com.avidco.studentintellect.activities.ui.database.MyModulesLocalDatabase
import com.avidco.studentintellect.databinding.ActivityMainBinding
import com.avidco.studentintellect.models.User
import com.avidco.studentintellect.utils.Utils.appRateCheck
import com.avidco.studentintellect.utils.Utils.appRatedCheck
import com.avidco.studentintellect.utils.Utils.askPlayStoreRatings
import com.avidco.studentintellect.utils.Utils.dpToPx
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.isGooglePlayServicesAvailable
import com.avidco.studentintellect.utils.Utils.openPlayStore
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.bumptech.glide.Glide
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textview.MaterialTextView
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() , OnSharedPreferenceChangeListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var navController : NavController
    private lateinit var navView: NavigationView
    private lateinit var binding: ActivityMainBinding
    var userDB: UserDatabase? = null
    companion object{
        private const val UPDATE_REQUEST_CODE = 12
        const val USER_ARG = "user_arg"
    }
    private var appUpdateManager : AppUpdateManager? = null
    private var exit = false
    private lateinit var currentUser : FirebaseUser

    private lateinit var adRequest : AdRequest
    private var interstitialAd : InterstitialAd? = null
    private fun loadInterstitialAd() {
        InterstitialAd.load(this, getString(R.string.activity_modulesSelectList_interstitialAdUnitId), adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    // The interstitialAd reference will be null until
                    // an ad is loaded.
                    interstitialAd = ad
                    //nextLevelButton.setEnabled(true)

                    interstitialAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            // Called when fullscreen content is dismissed.
                            interstitialAd = null
                            loadInterstitialAd()
                            navController.navigate(R.id.action_modules_fragment)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            // Called when fullscreen content failed to show.
                            interstitialAd = null
                            loadInterstitialAd()
                            navController.navigate(R.id.action_modules_fragment)
                        }

                        override fun onAdShowedFullScreenContent() {
                            // Called when fullscreen content is shown.
                        }
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error
                    interstitialAd = null
                    //nextLevelButton.setEnabled(true)
                }
            })
    }
    private fun showInterstitial() {
        // Show the ad if it"s ready. Otherwise toast and reload the ad.
        if (interstitialAd != null) {
            interstitialAd!!.show(this)
        } else {
            Toast.makeText(this, "Ad did not load", Toast.LENGTH_SHORT).show()
            //goToNextLevel()
        }
    }

    private fun setupNetworkListener() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                runOnUiThread {
                    userDB?.setIsOnline(true)
                    binding.noInternet.visibility = View.GONE
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                runOnUiThread {
                    userDB?.setIsOnline(false)
                    binding.noInternet.visibility = View.VISIBLE
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        if (Firebase.auth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        currentUser = Firebase.auth.currentUser!!
        binding = ActivityMainBinding.inflate(layoutInflater)
        val user = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.extras?.getParcelable(USER_ARG, User::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.extras?.getParcelable(USER_ARG)
        }
        userDB = UserDatabase(this, user)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupNetworkListener()

        //Theme
        val color = SurfaceColors.SURFACE_1.getColor(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = color
        window.navigationBarColor = color
        supportActionBar?.setBackgroundDrawable(ColorDrawable(color))


        if (isGooglePlayServicesAvailable()) {
            /**Check updates*/
            appUpdateManager = AppUpdateManagerFactory.create(this)
            checkUpdateAvailability()

            /**Check app rate*/
            appRateCheck ({
                askPlayStoreRatings()
            },{})
        } else {
            //Hide google services
        }




        val drawerLayout: DrawerLayout = binding.drawerLayout
        navView = binding.navView
        navView.itemIconTintList = null
        navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_my_materials
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
            navController.navigate(R.id.action_profile_fragment)
            drawerLayout.close()
        }

        navViewMenuItemClickListeners( drawerLayout)

        /**Init ads*/
        adsInit()




        /*val timetable = navView.menu.findItem(R.id.action_timetable).actionView as TextView
        timetable.gravity = Gravity.CENTER_VERTICAL;
        //timetable.setTypeface(null,Typeface.BOLD);
        timetable.setTextColor(ResourcesCompat.getColor(resources, R.color.primaryVariantColor, theme));
        timetable.text = "Coming Soon";*/
        val timetable = navView.menu.findItem(R.id.action_timetable).actionView as ImageView
        timetable.adjustViewBounds = true
        timetable.maxHeight = 28.dpToPx().toInt()
        timetable.setImageResource(R.drawable.ic_coming_soon)

        val youtube = navView.menu.findItem(R.id.nav_youtube).actionView as ImageView
        youtube.adjustViewBounds = true
        youtube.maxHeight = 28.dpToPx().toInt()
        youtube.setImageResource(R.drawable.ic_coming_soon)

        val map = navView.menu.findItem(R.id.action_map).actionView as ImageView
        map.adjustViewBounds = true
        map.maxHeight = 28.dpToPx().toInt()
        map.setImageResource(R.drawable.ic_coming_soon)
    }

    private fun navViewMenuItemClickListeners(drawerLayout: DrawerLayout) {
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
                    MyModulesLocalDatabase(this).doOnSignOut {
                        startActivity(Intent(this, AuthActivity::class.java))
                        finishAffinity()
                    }
                }
                .show()
            true
        }

        navView.menu.findItem(R.id.action_timetable).setOnMenuItemClickListener {
            Toast.makeText(this, "This feature is under construction.", Toast.LENGTH_SHORT).show()
            false
        }
        navView.menu.findItem(R.id.nav_youtube).setOnMenuItemClickListener {
            Toast.makeText(this, "This feature is under construction.", Toast.LENGTH_SHORT).show()
            false
        }
        navView.menu.findItem(R.id.action_map).setOnMenuItemClickListener {
            Toast.makeText(this, "This feature is under construction.", Toast.LENGTH_SHORT).show()
            false
        }

        //Blackboard
        navView.menu.findItem(R.id.action_blackboard).setOnMenuItemClickListener {
            drawerLayout.close()
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val openBlackboardApp = sharedPreferences.getBoolean("open_blackboard_app", false)
            if (openBlackboardApp){
                try {
                    val blackboardIntent = Intent().apply {
                        setClassName("com.blackboard.android.bbstudent", "com.blackboard.android.bbstudent.splash.SplashActivity")
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(blackboardIntent)
                }
                catch (e: ActivityNotFoundException) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Download Blackboard Learn?")
                        .setNeutralButton("Cancel"){d,_->
                            d.dismiss()
                        }
                        .setNegativeButton("Open the website"){d,_->
                            d.dismiss()
                            sharedPreferences.edit().putBoolean("open_blackboard_app", false).apply()

                            val url = "https://tmlearn.ul.ac.za/"
                            val builder = CustomTabsIntent.Builder()
                            builder.setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder()
                                .setToolbarColor(SurfaceColors.SURFACE_1.getColor(this))
                                .build())
                            builder.setCloseButtonIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_back_arrow))
                            builder.setToolbarCornerRadiusDp(15)
                            builder.setUrlBarHidingEnabled(true)
                            val customTabsIntent = builder.build()

                            customTabsIntent.launchUrl(this, Uri.parse(url))
                        }
                        .setPositiveButton("Download"){d,_->
                            d.dismiss()
                            startActivity(Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("market://details?id=com.blackboard.android.bbstudent")
                            })
                        }
                        .show()
                }
            }
            else {
                val url = "https://tmlearn.ul.ac.za/"
                val builder = CustomTabsIntent.Builder()
                builder.setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(SurfaceColors.SURFACE_1.getColor(this))
                    .build())
                builder.setCloseButtonIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_back_arrow))
                builder.setToolbarCornerRadiusDp(15)
                builder.setUrlBarHidingEnabled(true)
                val customTabsIntent = builder.build()

                customTabsIntent.launchUrl(this, Uri.parse(url))
            }

            false
        }

        //Intellect Calculator
        navView.menu.findItem(R.id.action_calculator).setOnMenuItemClickListener {
            drawerLayout.close()
            try {
                val calculatorIntent = Intent().apply {
                    setClassName("com.avidco.intellectcalculator", "com.avidco.intellectcalculator.MainActivity")
                    putExtra("displayHomeAsUpEnabled", true)
                }
                startActivity(calculatorIntent)
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=com.avidco.intellectcalculator")
                })
            }
            false
        }

    }

    private fun adsInit(){
        MobileAds.initialize(this) {}
        adRequest = AdRequest.Builder().build()
        loadInterstitialAd()
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

    fun showFeedbackDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.menu_layout_feedback)
        val playStore = dialog.findViewById<MaterialTextView>(R.id.rate_app_on_play_store)
        val googleForm = dialog.findViewById<MaterialTextView>(R.id.fill_google_form)

        if (isGooglePlayServicesAvailable()) {
            appRatedCheck({
                playStore.setOnClickListener {
                    it.tempDisable()
                    dialog.dismiss()
                    openPlayStore()
                }
            },{
                playStore.setOnClickListener {
                    it.tempDisable()
                    dialog.dismiss()
                    askPlayStoreRatings()
                }
            })
        } else {
            playStore.visibility = View.GONE
        }

        googleForm.setOnClickListener {
            it.tempDisable()
            dialog.dismiss()

            val url = "https://docs.google.com/forms/d/e/1FAIpQLSd_oJoesSeXN1pu1oI0cTOU_n6LSE6wwLG-taGB7JD4X3izpQ/viewform?usp=sf_link"
            val builder = CustomTabsIntent.Builder()
            builder.setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder()
                .setToolbarColor(SurfaceColors.SURFACE_1.getColor(this))
                .build())
            builder.setCloseButtonIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_back_arrow))
            builder.setUrlBarHidingEnabled(true)
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(this, Uri.parse(url))
        }

        dialog.show()
        dialog.window!!.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when(key){
            "under_construction_features" -> {
                val construction = sharedPreferences.getBoolean(key, false)
                navView.menu.findItem(R.id.action_timetable).isVisible = construction
                navView.menu.findItem(R.id.nav_youtube).isVisible = construction
                navView.menu.findItem(R.id.action_map).isVisible = construction
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        val construction = sharedPreferences.getBoolean("under_construction_features", false)
        navView.menu.findItem(R.id.action_timetable).isVisible = construction
        navView.menu.findItem(R.id.nav_youtube).isVisible = construction
        navView.menu.findItem(R.id.action_map).isVisible = construction
    }

    override fun onStop() {
        super.onStop()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }
}