package com.avidco.studentintellect.activities.auth

import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.Slide
import android.transition.TransitionManager
import android.view.*
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.avidco.studentintellect.R
import com.avidco.studentintellect.databinding.FragmentLoginBinding
import com.avidco.studentintellect.databinding.PopupTermsBinding
import com.avidco.studentintellect.models.UserData
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.utils.Utils.isGooglePlayServicesAvailable
import com.avidco.studentintellect.utils.Utils.isOnline
import com.avidco.studentintellect.utils.Constants
import com.avidco.studentintellect.utils.Utils.dpToPx
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.avidco.studentintellect.utils.pdfviewer.util.FitPolicy
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private val auth = Firebase.auth
    private lateinit var terms: PopupTermsBinding
    private lateinit var popupWindow: PopupWindow

    private fun loadPdf() {
        Firebase.storage.getReference("docs/Student Intellect Privacy Policy.pdf")
            .getBytes(1024 * 1024)
            .addOnSuccessListener {
                terms.pdfView.fromBytes(it)
                    .enableSwipe(true)
                    .enableDoubletap(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .onLoad {
                        terms.progressBar.visibility = View.GONE
                    }
                    .onError { error->
                        Toast.makeText(terms.pdfView.context, error.message, Toast.LENGTH_SHORT).show()
                        popupWindow.dismiss()
                    }
                    .onTap { false }
                    .enableAntialiasing(true)
                    .spacing(0)
                    .enableAnnotationRendering(false)
                    .password(null)
                    .scrollHandle(null)
                    .autoSpacing(false)
                    .pageFitPolicy(FitPolicy.WIDTH)
                    .fitEachPage(true)
                    .nightMode(terms.pdfView.context.getString(R.string.night_mode) == "on")
                    .load()
            }
            .addOnFailureListener {
                // Handle any errors
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                popupWindow.dismiss()
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        return binding.root
    }

    private fun openTerms(userData : UserData) {
        terms = PopupTermsBinding.inflate(layoutInflater)
        terms.progressBar.visibility = View.VISIBLE

        popupWindow = PopupWindow(terms.root, LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT, true)
            .apply {
                elevation = 8.0f
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    val slideIn = Slide()
                    slideIn.slideEdge = Gravity.BOTTOM
                    enterTransition = slideIn

                    val slideOut = Slide()
                    slideOut.slideEdge = Gravity.BOTTOM
                    exitTransition = slideOut
                }

                terms.termsClose.setOnClickListener{
                    it.tempDisable()
                    dismiss()
                    Firebase.firestore.collection("users")
                        .document(auth.currentUser!!.uid)
                        .set(hashMapOf("termsAccepted" to false), SetOptions.merge())
                        .addOnCompleteListener { task ->
                            auth.signOut()
                            if (task.exception != null) {
                                Toast.makeText(context, task.exception!!.message, Toast.LENGTH_LONG).show()
                            }
                            Toast.makeText(context, "Try again and accept the terms.", Toast.LENGTH_LONG).show()
                            //binding.loginWithGoogle.revertAnimation()
                            binding.loginButton.isEnabled = true
                           // binding.loginButton.revertAnimation()
                        }
                }
                terms.accept.setOnClickListener {
                    it.tempDisable()
                    dismiss()
                    Firebase.firestore.collection("users")
                        .document(Firebase.auth.currentUser!!.uid)
                        .set(hashMapOf("termsAccepted" to true), SetOptions.merge())
                        .addOnCompleteListener { task->
                            if (task.exception != null) {
                                Toast.makeText(context, task.exception!!.message, Toast.LENGTH_LONG).show()
                            }
                            val checkMark = BitmapFactory.decodeResource(context?.resources, R.drawable.ic_checked)
                           // binding.loginWithGoogle.doneLoadingAnimation(Color.parseColor("#3BB54A"), checkMark)
                            Handler(Looper.getMainLooper()).postDelayed({
                                val prefs = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
                                prefs.edit().putInt("user_type", userData.userType)?.apply()
                                prefs.edit().putStringSet("my_modules", userData.myModules?.split(';')?.toSet()).apply()
                                prefs.edit().putFloat("my_balance", userData.balance).apply()
                                startActivity(Intent(requireActivity(), MainActivity::class.java))
                                requireActivity().finishAffinity()
                            }, 1000)
                        }
                }
                loadPdf()
            }
        TransitionManager.beginDelayedTransition(binding.root)
        popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
    }

    private fun gotoMainActivity(withGoogle : Boolean = false) {
        Firebase.firestore.collection("users").document(auth.currentUser!!.uid).get()
            .addOnFailureListener {
                Toast.makeText(requireActivity(), it.message, Toast.LENGTH_SHORT).show()
                auth.signOut()
                Toast.makeText(context, "Please try again later. 2", Toast.LENGTH_SHORT).show()
               // binding.loginWithGoogle.revertAnimation()
                binding.loginButton.isEnabled = true
               // binding.loginButton.revertAnimation()
            }
            .addOnSuccessListener {
                val userData = it.toObject<UserData>()
                if (userData != null){
                    if (userData.termsAccepted) {
                        val prefs = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
                        prefs.edit().putInt("user_type", userData.userType).apply()
                        prefs.edit().putStringSet("my_modules", userData.myModules?.split(';')?.toSet()).apply()
                        prefs.edit().putFloat("my_balance", userData.balance).apply()
                        val checkMark = BitmapFactory.decodeResource(context?.resources, R.drawable.ic_checked)
                        if (withGoogle){
                           // binding.loginWithGoogle.doneLoadingAnimation(Color.parseColor("#3BB54A"), checkMark)
                        } else {
                            //binding.loginButton.doneLoadingAnimation(Color.parseColor("#3BB54A"), checkMark)
                        }
                        Handler(Looper.getMainLooper()).postDelayed({
                            startActivity(Intent(requireActivity(), MainActivity::class.java))
                            requireActivity().finishAffinity()
                        }, 1000)

                    } else {
                        openTerms(userData)
                    }
                }

            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        validateInput()

        binding.registerButton.setOnClickListener {
            it.tempDisable()
            activity?.hideKeyboard(it)
            findNavController().navigate(R.id.action_LoginFragment_to_RegisterFragment)
        }
        binding.passwordButton.setOnClickListener {
            it.tempDisable()
            activity?.hideKeyboard(it)
            val email = binding.email.editText?.text.toString().trim()
            if (email.isNotEmpty()){
                val arguments = Bundle().apply {
                    putString("login_email", email)
                }
                findNavController().navigate(R.id.action_LoginFragment_to_passwordFragment, arguments)
            } else {
                findNavController().navigate(R.id.action_LoginFragment_to_passwordFragment)
            }
        }

        binding.loginButton.setOnClickListener {
            it.tempDisable()
            activity?.hideKeyboard(it)
            binding.loginWithGoogle.isEnabled = false
           // binding.loginButton.startAnimation()

            if (requireActivity().isOnline()){
                // Configure Google Sign In
                val email = binding.email.editText!!.text.toString().trim()
                val password = binding.password.editText!!.text.toString()

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Sign in success, update UI with the signed-in user's information
                                gotoMainActivity()
                            } else {
                                // If sign in fails, display a message to the user.
                                println( "signInWithEmail:failure: "+ task.exception)
                                Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
                                val warning = BitmapFactory.decodeResource(context?.resources, R.drawable.ic_warning)
                              //  binding.loginButton.doneLoadingAnimation(Color.parseColor("#EE5253"), warning)
                                Handler(Looper.getMainLooper()).postDelayed({
                                   // binding.loginButton.revertAnimation()
                                }, 1000)
                                binding.loginWithGoogle.isEnabled = true
                            }
                        }
                } else if (email.isEmpty()){
                    binding.email.error = "Email Required"
                   // binding.loginButton.revertAnimation()
                    binding.loginWithGoogle.isEnabled = true
                } else {
                   //binding.loginButton.revertAnimation()
                    binding.loginWithGoogle.isEnabled = true
                    binding.password.error = "Password Required"
                }
            } else {
              //  binding.loginButton.revertAnimation()
                binding.loginWithGoogle.isEnabled = true
                Toast.makeText(context, "No interest connection.\nConnect to your WiFi or Mobile data.", Toast.LENGTH_SHORT).show()
            }
        }

        if (requireContext().isGooglePlayServicesAvailable()) {
            binding.loginWithGoogle.setOnClickListener {
                it.tempDisable()
                activity?.hideKeyboard(it)
                binding.loginButton.isEnabled = false
                //binding.loginWithGoogle.startAnimation()

                if (requireActivity().isOnline()) {
                    // Configure Google Sign In
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.web_client_id))
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
                    activityForResult.launch(googleSignInClient.signInIntent)
                } else {
                   // binding.loginWithGoogle.revertAnimation()
                    binding.loginButton.isEnabled = true
                    Toast.makeText(context, "No interest connection.\nConnect to your WiFi or Mobile data.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            binding.loginWithGoogle.visibility = View.GONE
        }
    }

    private val activityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                //.loginWithGoogle.revertAnimation()
                binding.loginButton.isEnabled = true
                Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        } else {
            //binding.loginWithGoogle.revertAnimation()
            binding.loginButton.isEnabled = true
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    gotoMainActivity(true)
                } else {
                    val warning = BitmapFactory.decodeResource(context?.resources, R.drawable.ic_warning)
                    //binding.loginWithGoogle.doneLoadingAnimation(Color.parseColor("#EE5253"), warning)
                    Handler(Looper.getMainLooper()).postDelayed({
                        //binding.loginWithGoogle.revertAnimation()
                        binding.loginButton.isEnabled = true
                    }, 1000)

                    if (task.exception?.message == Constants.CERT_ERROR) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Trust certification not found.")
                            .setMessage("Validate your WiFi connection, or switch to Mobile data and try again.")
                            .create()
                            .show()
                    } else {
                        Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun validateInput() {
        binding.email.editText!!.doOnTextChanged { _,  _,  _,  _ ->
            binding.email.isErrorEnabled = false
        }
        binding.email.editText!!.setOnFocusChangeListener { _, b ->
            if (!b && binding.email.editText!!.text.trim().isNotEmpty() && !binding.email.editText!!.text.trim().toString().matches(
                    Constants.EMAIL_PATTERN.toRegex())) {
                binding.email.error = "Invalid email address."
            }
        }
        binding.password.editText!!.doOnTextChanged { _, _, _, _ ->
            binding.password.isErrorEnabled = false
        }
    }


    override fun onStart() {
        super.onStart()
        //Animate the logo after 1.5 sec
        Handler(Looper.getMainLooper()).postDelayed({
            binding.iconImageView.animate().apply {
                x(16.dpToPx())
                y(64.dpToPx())
                scaleX(1F)
                scaleY(1F)
                duration = 400
                setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {}
                    override fun onAnimationEnd(animation: Animator) {
                        binding.afterAnimationView.visibility = View.VISIBLE
                    }
                    override fun onAnimationCancel(animation: Animator) {}
                    override fun onAnimationRepeat(animation: Animator) {}
                })
            }
        },1500)
    }
}