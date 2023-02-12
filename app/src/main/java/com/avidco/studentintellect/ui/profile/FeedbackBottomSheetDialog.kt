package com.avidco.studentintellect.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.avidco.studentintellect.R
import com.avidco.studentintellect.databinding.SheetFeedbackBinding
import com.avidco.studentintellect.ui.MainActivity
import com.avidco.studentintellect.utils.Utils.appRatedCheck
import com.avidco.studentintellect.utils.Utils.askPlayStoreRatings
import com.avidco.studentintellect.utils.Utils.isGooglePlayServicesAvailable
import com.avidco.studentintellect.utils.Utils.openPlayStore
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FeedbackBottomSheetDialog : BottomSheetDialogFragment() {
    private lateinit var binding: SheetFeedbackBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cancelButton.setOnClickListener {
            it.tempDisable()
            dismiss()
        }
        if (requireContext().isGooglePlayServicesAvailable()) {
            requireActivity().appRatedCheck({
                binding.rateApp.setOnClickListener {
                    it.tempDisable()
                    activity?.openPlayStore()
                }
            },{
                binding.rateApp.setOnClickListener {
                    it.tempDisable()
                    (activity as MainActivity).askPlayStoreRatings()
                }
            })
        } else {
            binding.rateApp.visibility = View.GONE
        }

        binding.feedback.setOnClickListener {
            it.tempDisable()
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLSd_oJoesSeXN1pu1oI0cTOU_n6LSE6wwLG-taGB7JD4X3izpQ/viewform?usp=sf_link")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = SheetFeedbackBinding.inflate(inflater,container,false)
        return binding.root
    }
}








