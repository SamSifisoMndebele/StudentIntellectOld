package com.avidco.studentintellect.activities.ui.profile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.avidco.studentintellect.BuildConfig
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.auth.AuthActivity
import com.avidco.studentintellect.databinding.FragmentProfileBinding
import com.avidco.studentintellect.databinding.SheetAboutAppBinding
import com.avidco.studentintellect.models.UserType
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.database.UserDB
import com.avidco.studentintellect.activities.ui.modules.MyModulesDatabaseHelper
import com.avidco.studentintellect.utils.LoadingDialog
import com.avidco.studentintellect.utils.OpenPicturesContract
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.isGooglePlayServicesAvailable
import com.avidco.studentintellect.utils.Utils.openPlayStore
import com.avidco.studentintellect.utils.Utils.toRand
import com.avidco.studentintellect.utils.Utils.shareApp
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.bumptech.glide.Glide
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.IOException

class ProfileFragment : Fragment() {

    lateinit var binding: FragmentProfileBinding
    private val auth = Firebase.auth
    private val currentUser = Firebase.auth.currentUser
    private var interstitialAd: InterstitialAd? = null

    private fun loadAd() {
        InterstitialAd.load(requireContext(),
            getString(R.string.activity_modulesSelectList_interstitialAdUnitId),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    println("Ad was loaded.")
                    interstitialAd = ad
                    interstitialAd?.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdClicked() {
                                // Called when a click is recorded for an ad.
                                println("Ad was clicked.")
                            }

                            override fun onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                // Set the ad reference to null so you don't show the ad a second time.
                                println("Ad dismissed fullscreen content.")
                                (requireActivity() as MainActivity).navController.navigate(R.id.action_modules_fragment)
                                loadAd()
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                // Called when ad fails to show.
                                println("Ad failed to show fullscreen content. $adError")
                                interstitialAd = null
                            }

                            override fun onAdImpression() {
                                // Called when an impression is recorded for an ad.
                                println("Ad recorded an impression.")
                            }

                            override fun onAdShowedFullScreenContent() {
                                // Called when ad is shown.
                                println("Ad showed fullscreen content.")
                            }
                        }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }
            })
    }


    private val selectImageResult = registerForActivityResult(OpenPicturesContract()) { uri: Uri? ->
        uri?.let { setImage(uri) }
    }
    private fun setImage(imageUri : Uri?) {
        val loadingDialog = LoadingDialog(requireActivity())
        loadingDialog.show("Saving Picture...")
        try {
            if (imageUri != null) {
                val userImagesRef =  Firebase.storage.getReference("Users")
                    .child("Images/${currentUser?.uid}.png")
                val uploadTask = userImagesRef.putFile(imageUri)
                loadingDialog.onCancel {
                    uploadTask.cancel()
                }
                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            loadingDialog.isError(it.message) {  }
                            throw it
                        }
                    }
                    userImagesRef.downloadUrl
                }
                    .addOnSuccessListener { imageUrl ->
                        userDB.setImageUrl(imageUrl.toString())
                        loadingDialog.isDone {  }
                    }
                    .addOnFailureListener {
                        loadingDialog.isError(it.message) {  }
                    }

            } else {
                binding.userImage.setImageResource(R.drawable.ic_user)
                userDB.setImageUrl(null)
            }


        } catch (e: IOException) {
            loadingDialog.isError(e.message) {  }
            e.printStackTrace()
        }
    }

    private lateinit var userDB : UserDB

    override fun onCreateView (
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        userDB = UserDB(context, null)
        binding.userEmail.text = userDB.email.value

        /*profileViewModel.modulesList.observe(viewLifecycleOwner) { list ->
            val modules = StringBuilder()
            var b = false
            list?.forEach {
                if (b) modules.append(", ")
                modules.append(it.code +" - "+it.name)
                b = true
            }
            binding.modulesPreview.setText(modules.toString().trim().ifEmpty { "Select your modules" })
        }*/
        userDB.imageUrl.observe(viewLifecycleOwner) {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.ic_user)
                .into(binding.userImage)
        }

        userDB.name.observe(viewLifecycleOwner) {
            binding.username.setText(it?:getString(R.string.display_picture_and_name))
        }

        userDB.balance.observe(viewLifecycleOwner) {
            @SuppressLint("SetTextI18n")
            binding.userBalance.text = it.toRand()
        }

        /*binding.username.editText?.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.saveButton.visibility = View.INVISIBLE
            } else {
                binding.saveButton.visibility = View.VISIBLE
            }
        }*/

        binding.saveButton.setOnClickListener {
            it.tempDisable()
            context?.hideKeyboard(binding.root)
            userDB.setName(binding.username.text?.trim().toString())
        }


        val prefs = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
        when (prefs.getInt("user_type", -1)) {
            UserType.STUDENT -> {

            }
            UserType.TUTOR -> {

            }
            UserType.ADMIN -> {

            }
        }

        binding.editImageButton.setOnClickListener {
            it.tempDisable()
            selectImageResult.launch(arrayOf("image/*"))
        }
        binding.removeImageButton.setOnClickListener {
            it.tempDisable()
            binding.userImage.setImageResource(R.drawable.ic_user)
            setImage(null)
        }




        binding.modules.setOnClickListener {
            it.tempDisable()
            if (interstitialAd == null) {
                (requireActivity() as MainActivity).navController.navigate(R.id.action_modules_fragment)
            } else {
                interstitialAd!!.show(requireActivity())
            }
        }
        binding.modulesPreview.setOnClickListener {
            it.tempDisable()
            if (interstitialAd == null) {
                (requireActivity() as MainActivity).navController.navigate(R.id.action_modules_fragment)
            } else {
                interstitialAd!!.show(requireActivity())
            }
        }

        binding.logout.setOnClickListener {
            it.tempDisable()
            MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(R.string.logout_question))
                .setMessage(getString(R.string.confirm_logout_text))
                .setNeutralButton(getString(R.string.cancel)){d,_->
                    d.dismiss()
                }
                .setPositiveButton(getString(R.string.logout)){d,_->
                    d.dismiss()
                    MyModulesDatabaseHelper(requireContext()).doOnSignOut {
                        startActivity(Intent(requireActivity(), AuthActivity::class.java))
                        requireActivity().finishAffinity()
                    }
                }
                .show()
        }

        binding.feedback.setOnClickListener {
            it.tempDisable()
            (activity as MainActivity).showFeedbackDialog()
        }

        binding.appInfo.setOnClickListener {
            it.tempDisable()
            val sheet = AppInfoSheetDialog()
            sheet.show(childFragmentManager, "AppInfoSheetDialog")
        }

        MobileAds.initialize(requireContext()) { loadAd() }
        return binding.root
    }

    class AppInfoSheetDialog : BottomSheetDialogFragment() {
        private lateinit var binding: SheetAboutAppBinding
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            super.onCreateView(inflater, container, savedInstanceState)
            binding = SheetAboutAppBinding.inflate(inflater,container,false)

            binding.appVersion.text = BuildConfig.VERSION_NAME
            binding.shareApp.setOnClickListener {
                it.tempDisable()
                dismiss()
                activity?.shareApp()
            }
            if (requireContext().isGooglePlayServicesAvailable()) {
                binding.openPlayStore.setOnClickListener {
                    it.tempDisable()
                    dismiss()
                    activity?.openPlayStore()
                }
            } else {
                binding.openPlayStore.visibility = View.GONE
            }

            return binding.root
        }
    }
}

