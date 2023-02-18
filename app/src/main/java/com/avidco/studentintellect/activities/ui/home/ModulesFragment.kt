package com.avidco.studentintellect.activities.ui.home

import android.Manifest
import android.app.Dialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.R
import com.avidco.studentintellect.databinding.FragmentModulesBinding
import com.avidco.studentintellect.activities.ui.DataViewModel
import com.avidco.studentintellect.utils.Utils.shareApp
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ModulesFragment : Fragment() {

    private lateinit var binding: FragmentModulesBinding
    private var adapter : ModulesAdapter? = null

    private val database = Firebase.firestore
    private var interstitialAd : InterstitialAd? = null

    internal fun loadInterstitialAd() {
        InterstitialAd.load(requireActivity(), getString(R.string.activity_modulesSelectList_interstitialAdUnitId),
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
                            (requireActivity() as MainActivity).navController.navigate(R.id.addModulesFragment)
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            // Called when ad fails to show.
                            println( "Ad failed to show fullscreen content.")
                            loadInterstitialAd()
                            (requireActivity() as MainActivity).navController.navigate(R.id.addModulesFragment)
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

    private val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        var granted = true
        permissions.entries.forEach {
            println("permission : "+ "${it.key} = ${it.value}")
            granted = granted && it.value
        }
        println("permissions granted : $granted")

        if(!granted){
            askPermissionsAgain()
        } else {
            adapter?.gotoMaterialsListActivity()
        }
    }

    private fun askPermissionsAgain() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                requireActivity().shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                        requireActivity().shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    // In an educational UI, explain to the user why your app requires this
                    // permission for a specific feature to behave as expected. In this UI,
                    // include a "cancel" or "no thanks" button that allows the user to
                    // continue using your app without granting the permission.

                    AlertDialog.Builder(requireActivity())
                        .setTitle("Storage Permission needed")
                        .setMessage("This permission is needed to save and read the study materials in your storage.\nPlease allow access to files.")
                        .setPositiveButton("Grant Permission") { _, _ ->
                            requestMultiplePermissions.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        }
                        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                        .create()
                        .show()
                }
                PermissionChecker.checkCallingOrSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED ||
                        PermissionChecker.checkCallingOrSelfPermission(requireContext(),
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED ->
                {
                    AlertDialog.Builder(requireActivity())
                        .setTitle("Storage Permission needed")
                        .setMessage("This permission is needed to save and read the study materials in your storage.\nPlease allow access to files.")
                        .setPositiveButton("Grant Permission") { _, _ ->
                            AlertDialog.Builder(requireActivity())
                                .setMessage("Click:\n-Permissions\n-Files and Media\n-Allow access to media only.")
                                .setPositiveButton("OK") { _, _ ->
                                    try {
                                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity?.packageName}")))
                                    } catch (e: Exception) {
                                        AlertDialog.Builder(requireActivity())
                                            .setMessage("An error occurred. Please go to App Info and follow the given instructions.")
                                            .create()
                                            .show()
                                    }
                                }
                                .create()
                                .show()
                        }
                        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                        .create()
                        .show()
                }
            }
        }
        else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            when {
                PermissionChecker.checkCallingOrSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED ||
                        PermissionChecker.checkCallingOrSelfPermission(requireContext(),
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED ->
                {
                    AlertDialog.Builder(requireActivity())
                        .setTitle("Storage Permission needed")
                        .setMessage("This permission is needed to save and read the study materials in your storage.\nPlease allow access to files.")
                        .setPositiveButton("Grant Permission") { _, _ ->
                            AlertDialog.Builder(requireActivity())
                                .setMessage("Click:\n-Permissions\n-Files and Media\n-Allow access to media only.")
                                .setPositiveButton("OK") { _, _ ->
                                    try {
                                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity?.packageName}")))
                                    } catch (e: Exception) {
                                        AlertDialog.Builder(requireActivity())
                                            .setMessage("An error occurred. Please go to App Info and follow the given instructions.")
                                            .create()
                                            .show()
                                    }
                                }
                                .create()
                                .show()
                        }
                        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                        .create()
                        .show()
                }
            }
        }
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = FragmentModulesBinding.inflate(inflater, container, false)


        observeViewData()

        MobileAds.initialize(requireContext()) {
            loadInterstitialAd()
        }

        /*binding.menuButton.setOnClickListener {
            it.tempDisable()
            binding.addButton.tempDisable()

            showMenuDialog()
        }*/

        /*binding.addButton.setOnClickListener {
            it.tempDisable()
            binding.menuButton.tempDisable()

            if (interstitialAd == null){
                addModulesView()
            } else {
                interstitialAd?.show(requireActivity())
            }
        }*/

        return binding.root
    }

    private lateinit var viewModel : DataViewModel
    private fun observeViewData() {
        viewModel = ViewModelProvider(activity as MainActivity)[DataViewModel::class.java]
        viewModel.modulesList.observe(viewLifecycleOwner) { modulesList ->
            if (modulesList.isNullOrEmpty()) {
                binding.welcomeLayout.visibility = View.VISIBLE
                binding.moduleListLayout.visibility = View.GONE
                binding.selectModules.setOnClickListener {
                    it.tempDisable()
                    (requireActivity() as MainActivity).navController.navigate(R.id.addModulesFragment)
                }
                //binding.progressBar.visibility = View.GONE
            }
            else {
                database.disableNetwork().addOnCompleteListener {
                    // Do offline things
                    binding.welcomeLayout.visibility = View.GONE
                    binding.moduleListLayout.visibility = View.VISIBLE
                    val query : Query = database.collection("modules")
                        .whereIn("code", modulesList.sorted().take(10).toMutableList())

                    adapter = ModulesAdapter(activity as MainActivity, query, requestMultiplePermissions, modulesList.toMutableList())
                    binding.moduleList.layoutManager = LinearLayoutManager(context)
                    binding.moduleList.setHasFixedSize(true)
                    binding.moduleList.adapter = adapter
                    adapter!!.startListening()
                   // binding.progressBar.visibility = View.GONE
                    database.enableNetwork()
                }
            }
        }
    }

    private fun showMenuDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.menu_layout_bottomsheet)
        val feedback = dialog.findViewById<LinearLayout>(R.id.feedback)
        val shareApp = dialog.findViewById<LinearLayout>(R.id.share_app)
        /*feedback.setOnClickListener {
            it.tempDisable()
            dialog.dismiss()
            val sheet = FeedbackBottomSheetDialog()
            sheet.show(childFragmentManager, "FeedbackBottomSheetDialog")
        }*/
        shareApp.setOnClickListener {
            it.tempDisable()
            dialog.dismiss()
            activity?.shareApp()
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
}