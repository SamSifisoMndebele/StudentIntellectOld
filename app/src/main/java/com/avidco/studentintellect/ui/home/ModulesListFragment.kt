package com.avidco.studentintellect.ui.home

import android.Manifest
import android.app.Dialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.transition.Slide
import android.transition.TransitionManager
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.avidco.studentintellect.ui.MainActivity
import com.avidco.studentintellect.R
import com.avidco.studentintellect.databinding.FragmentModulesBinding
import com.avidco.studentintellect.databinding.PopupModulesBinding
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.ui.DataViewModel
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.isOnline
import com.avidco.studentintellect.utils.Utils.shareApp
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ModulesListFragment : Fragment() {

    private lateinit var binding: FragmentModulesBinding
    private var adapter : ModulesListAdapter? = null
    private var addAdapter : AddModulesAdapter? = null

    private var modulesBinding: PopupModulesBinding? = null
    private var popupWindow: PopupWindow? = null

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
                            addModulesView()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            // Called when ad fails to show.
                            println( "Ad failed to show fullscreen content.")
                            loadInterstitialAd()
                            addModulesView()
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

    fun addModulesView(init : Boolean = false) {
        val prefs = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
        if (modulesBinding == null){
            modulesBinding = PopupModulesBinding.inflate(layoutInflater)
        }
        //modulesBinding!!.progressBar.showProgressDialog(modulesBinding!!.avi)

        val userModulesSet = prefs?.getStringSet("user_modules", null)
        val allModulesSet = prefs.getStringSet("all_modules_set", null)
        addAdapter = AddModulesAdapter(allModulesSet?.toMutableList()?:mutableListOf(), userModulesSet?.toMutableList())
        modulesBinding!!.addModuleList.adapter = addAdapter

        if (popupWindow == null){
            popupWindow = PopupWindow(modulesBinding!!.root, LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, true)
                .apply {
                    elevation = 10.0f
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val slideIn = Slide()
                        slideIn.slideEdge = Gravity.BOTTOM
                        enterTransition = slideIn

                        val slideOut = Slide()
                        slideOut.slideEdge = Gravity.BOTTOM
                        exitTransition = slideOut
                    }
                    softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    modulesBinding!!.backButton.setOnClickListener { dismiss() }


                    val currentTime = Timestamp.now()
                    if (allModulesSet.isNullOrEmpty()) {
                        FirebaseFirestore
                            .getInstance()
                            .collection("modules")
                            .get()
                            .addOnSuccessListener {
                                val modules = mutableSetOf<String>()
                                it.documents.forEach { snapshot ->
                                    val data = snapshot.toObject(ModuleData::class.java)!!
                                    modules.add(data.code)
                                }
                                prefs.edit().putStringSet("all_modules_set", modules).apply()
                                prefs.edit().putLong("last_read_all_modules_time",currentTime.seconds).apply()

                                addAdapter = AddModulesAdapter(modules.toMutableList(), userModulesSet?.toMutableList())
                                modulesBinding!!.addModuleList.layoutManager = LinearLayoutManager(context)
                                modulesBinding!!.addModuleList.setHasFixedSize(true)
                                modulesBinding!!.addModuleList.adapter = addAdapter
                                //modulesBinding!!.progressBar.hideProgressDialog(modulesBinding!!.avi)
                            }
                            .addOnFailureListener {
                                //modulesBinding!!.progressBar.hideProgressDialog(modulesBinding!!.avi)
                                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                            }
                    }
                    else {
                        val lastReadTime = Timestamp(prefs.getLong("last_read_all_modules_time", 0),0)
                        FirebaseFirestore
                            .getInstance()
                            .collection("modules")
                            .whereGreaterThanOrEqualTo("dateAdded", lastReadTime)
                            .get()
                            .addOnSuccessListener {
                                val modules = mutableSetOf<String>()
                                modules.addAll(allModulesSet)
                                it.documents.forEach { snapshot ->
                                    val data = snapshot.toObject(ModuleData::class.java)!!
                                    modules.add(data.code)
                                }
                                prefs.edit().putStringSet("all_modules_set", modules).apply()
                                prefs.edit().putLong("last_read_all_modules_time",currentTime.seconds).apply()
                                addAdapter = AddModulesAdapter(modules.toMutableList(), userModulesSet?.toMutableList())
                                modulesBinding!!.addModuleList.layoutManager = LinearLayoutManager(context)
                                modulesBinding!!.addModuleList.setHasFixedSize(true)
                                modulesBinding!!.addModuleList.adapter = addAdapter
                               // modulesBinding!!.progressBar.hideProgressDialog(modulesBinding!!.avi)
                            }
                            .addOnFailureListener { e ->
                               // modulesBinding!!.progressBar.hideProgressDialog(modulesBinding!!.avi)
                                context?.let { Toast.makeText(it, e.message, Toast.LENGTH_LONG).show() }
                            }
                    }

                    modulesBinding!!.saveButton.setOnClickListener {
                        if (requireActivity().isOnline()){
                            if (addAdapter != null) {
                               // (it as CircularProgressButton).startAnimation()
                                val modulesList = addAdapter!!.list()
                                Firebase.firestore.collection("users")
                                    .document(Firebase.auth.currentUser!!.uid)
                                    .update("modules", modulesList)
                                    .addOnCompleteListener { task ->
                                        if (task.exception != null) {
                                            Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
                                        }

                                        viewModel.setModules(prefs, modulesList.toMutableSet())
                                        activity?.hideKeyboard(binding.root)
                                        popupWindow!!.dismiss()
                                        //it.revertAnimation()
                                    }
                            } else {
                                it.tempDisable()
                                Toast.makeText(activity, "Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            it.tempDisable()
                            Toast.makeText(activity, "Check your internet connection and try again.", Toast.LENGTH_SHORT).show()
                        }

                    }

                    modulesBinding!!.searchView.queryHint = "Search Module..."
                    modulesBinding!!.searchView.onActionViewExpanded()
                    modulesBinding!!.searchView.clearFocus()
                    modulesBinding!!.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                        override fun onQueryTextSubmit(string: String?): Boolean {
                            modulesBinding!!.searchView.clearFocus()
                            return false
                        }
                        override fun onQueryTextChange(string: String): Boolean {
                            addAdapter?.filter?.filter(string)
                            return false
                        }
                    })
                }
        }
        else if ((allModulesSet?.size?:0) <= 10){
            val currentTime = Timestamp.now()
            FirebaseFirestore
                .getInstance()
                .collection("modules")
                .get()
                .addOnSuccessListener {
                    val modules = mutableSetOf<String>()
                    it.documents.forEach { snapshot ->
                        val data = snapshot.toObject(ModuleData::class.java)!!
                        modules.add(data.code)
                    }
                    prefs.edit().putStringSet("all_modules_set", modules).apply()
                    prefs.edit().putLong("last_read_all_modules_time",currentTime.seconds).apply()

                    addAdapter = AddModulesAdapter(modules.toMutableList(), userModulesSet?.toMutableList())
                    modulesBinding!!.addModuleList.layoutManager = LinearLayoutManager(context)
                    modulesBinding!!.addModuleList.setHasFixedSize(true)
                    modulesBinding!!.addModuleList.adapter = addAdapter
                    //modulesBinding!!.progressBar.hideProgressDialog(modulesBinding!!.avi)
                }
                .addOnFailureListener {
                  //  modulesBinding!!.progressBar.hideProgressDialog(modulesBinding!!.avi)
                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                }
        }
        else {
           // modulesBinding!!.progressBar.hideProgressDialog(modulesBinding!!.avi)
        }

        if (!init){
            TransitionManager.beginDelayedTransition(binding.root)
            popupWindow?.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
        }

    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = FragmentModulesBinding.inflate(inflater, container, false)
        //(activity as MainActivity).supportActionBar?.title = getString(R.string.downloads)
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        homeViewModel.text.observe(viewLifecycleOwner) {

        }


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


        addModulesView(true)
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
                    addModulesView()
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

                    adapter = ModulesListAdapter(activity as MainActivity, query, requestMultiplePermissions, modulesList.toMutableList())
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