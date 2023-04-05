package com.avidco.studentintellect.activities.auth

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import com.avidco.studentintellect.R
import com.avidco.studentintellect.databinding.FragmentPasswordBinding
import com.avidco.studentintellect.utils.LoadingDialog
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class PasswordFragment : Fragment() {

    private lateinit var binding: FragmentPasswordBinding
    private lateinit var loadingDialog: LoadingDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPasswordBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(activity)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailAddress = arguments?.getString("login_email")
        mainFun(emailAddress)

        binding.email.editText!!.doOnTextChanged { _, _, _, _ ->
            binding.email.isErrorEnabled = false
        }
    }

    private fun sentEmail(email : String){
        Firebase.auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Email sent.")
                    Toast.makeText(context, "Email sent.", Toast.LENGTH_LONG).show()
                    val checkMark = BitmapFactory.decodeResource(context?.resources, R.drawable.ic_checked)
                    //binding.sendButton.doneLoadingAnimation(Color.parseColor("#3BB54A"), checkMark)
                    Handler(Looper.getMainLooper()).postDelayed({
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    }, 1000)
                } else {
                    mainFun()
                    Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
                    val warning = BitmapFactory.decodeResource(context?.resources, R.drawable.ic_warning)
                   // binding.sendButton.doneLoadingAnimation(Color.parseColor("#EE5253"), warning)
                    Handler(Looper.getMainLooper()).postDelayed({
                      //  binding.sendButton.revertAnimation()
                    }, 1000)
                }
            }
    }

    private fun mainFun(email : String? = null){
        if (email != null){
            binding.email.editText?.setText(email)
           // binding.sendButton.startAnimation()
            sentEmail(email)
        }
        else {
            binding.sendButton.setOnClickListener {
                it.tempDisable()
                val emailAddress = binding.email.editText?.text.toString().trim()
                if (emailAddress.isNotEmpty()){
                   // binding.sendButton.startAnimation()
                    sentEmail(emailAddress)
                } else {
                    Toast.makeText(context, "Enter your email address!", Toast.LENGTH_LONG).show()
                    binding.email.error = "Email Required."
                }

            }
        }
    }
}