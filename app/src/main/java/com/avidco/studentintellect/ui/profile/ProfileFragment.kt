package com.avidco.studentintellect.ui.profile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.avidco.studentintellect.BuildConfig
import com.avidco.studentintellect.R
import com.avidco.studentintellect.auth.AuthActivity
import com.avidco.studentintellect.databinding.FragmentProfileBinding
import com.avidco.studentintellect.databinding.SheetAboutAppBinding
import com.avidco.studentintellect.ui.DataViewModel
import com.avidco.studentintellect.models.UserType
import com.avidco.studentintellect.ui.MainActivity
import com.avidco.studentintellect.utils.LoadingDialog
import com.avidco.studentintellect.utils.OpenPicturesContract
import com.avidco.studentintellect.utils.Utils.isGooglePlayServicesAvailable
import com.avidco.studentintellect.utils.Utils.openPlayStore
import com.avidco.studentintellect.utils.Utils.roundToRand
import com.avidco.studentintellect.utils.Utils.shareApp
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.bumptech.glide.Glide
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
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
                                val sheet = MyModulesBottomSheetDialog(requireActivity() as MainActivity)
                                sheet.show(childFragmentManager, "ModulesSelectBottomSheetDialog")
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
            if (imageUri != null){
                val userRef =  Firebase.storage.getReference("users").child("${currentUser?.uid}.pdf")
                val uploadTask = userRef.putFile(imageUri)
                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    userRef.downloadUrl
                }
                    .addOnSuccessListener { imageUrl ->
                        currentUser?.updateProfile(userProfileChangeRequest {
                            photoUri = imageUrl })
                            ?.addOnSuccessListener {
                                val dataViewModel = ViewModelProvider(requireActivity())[DataViewModel::class.java]
                                dataViewModel.setPhotoUrl(imageUrl.toString())
                            }
                            ?.addOnFailureListener {
                                loadingDialog.isError(it.message.toString()) {  }
                            }
                    }
                    .addOnFailureListener {
                        loadingDialog.isError(it.message.toString()) {  }
                    }

            } else {
                binding.userImage.setImageResource(R.drawable.ic_user)
                currentUser?.updateProfile(userProfileChangeRequest {
                    photoUri = null })
                    ?.addOnSuccessListener {
                        val dataViewModel = ViewModelProvider(requireActivity())[DataViewModel::class.java]
                        dataViewModel.setPhotoUrl(null)
                    }
                    ?.addOnFailureListener {
                        loadingDialog.isError(it.message.toString()) {  }
                    }

            }


        } catch (e: IOException) {
            e.printStackTrace()
            loadingDialog.isError(e.message.toString()) {  }
        }
    }

    override fun onCreateView (
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)


        /*val profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        profileViewModel.text.observe(viewLifecycleOwner,
            Observer { text: String? -> binding.userName.text = text }
        )*/

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.userEmail.text = auth.currentUser?.email

        val dataViewModel = ViewModelProvider(activity as MainActivity)[DataViewModel::class.java]
        dataViewModel.modulesList.observe(viewLifecycleOwner) { list ->
            val modules = StringBuilder()
            var b = false
            list?.sorted()?.forEach {
                if (b) modules.append(", ")
                modules.append(it)
                b = true
            }
            binding.modulesPreview.text = modules.toString().trim().ifEmpty { "Select your modules" }
        }
        dataViewModel.photoUrl.observe(viewLifecycleOwner){
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.ic_user)
                .into(binding.userImage)
        }

        dataViewModel.displayName.observe(viewLifecycleOwner) {
            binding.userName.text = it?:getString(R.string.display_picture_and_name)
        }

        dataViewModel.balance.observe(viewLifecycleOwner) {
            binding.userBalance.text = "R ${it.roundToRand()}"
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
                val sheet = MyModulesBottomSheetDialog(requireActivity() as MainActivity)
                sheet.show(childFragmentManager, "ModulesSelectBottomSheetDialog")
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
                    Firebase.auth.signOut()
                    startActivity(Intent(requireActivity(), AuthActivity::class.java))
                    requireActivity().finishAffinity()
                }
                .show()
        }

        binding.details.setOnClickListener {
            it.tempDisable()
            val sheet = ProfileDetailsBottomSheetDialog(activity as MainActivity)
            sheet.show(childFragmentManager, "ProfileDetailsBottomSheetDialog")
        }

        binding.userEmail.setOnClickListener {
            it.tempDisable()
            val sheet = ProfileDetailsBottomSheetDialog(activity as MainActivity)
            sheet.show(childFragmentManager, "ProfileDetailsBottomSheetDialog")
        }

        binding.userImage.setOnClickListener {
            it.tempDisable()
            val sheet = ProfileDetailsBottomSheetDialog(activity as MainActivity)
            sheet.show(childFragmentManager, "ProfileDetailsBottomSheetDialog")
        }

        binding.feedback.setOnClickListener {
            it.tempDisable()
            val sheet = FeedbackBottomSheetDialog()
            sheet.show(childFragmentManager, "FeedbackBottomSheetDialog")
        }

        binding.appInfo.setOnClickListener {
            it.tempDisable()
            val sheet = AppInfoSheetDialog()
            sheet.show(childFragmentManager, "AppInfoSheetDialog")
        }

        MobileAds.initialize(requireContext()) { loadAd() }
    }

    class AppInfoSheetDialog : BottomSheetDialogFragment() {
        private lateinit var binding: SheetAboutAppBinding
        override fun getTheme(): Int {
            return R.style.CustomBottomSheetDialog
        }
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

