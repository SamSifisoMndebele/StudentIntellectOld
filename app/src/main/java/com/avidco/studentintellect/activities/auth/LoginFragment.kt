package com.avidco.studentintellect.activities.auth

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import com.avidco.studentintellect.models.User
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.database.MyModulesLocalDatabase
import com.avidco.studentintellect.utils.Utils.isGooglePlayServicesAvailable
import com.avidco.studentintellect.utils.Constants
import com.avidco.studentintellect.utils.LoadingDialog
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.avidco.studentintellect.utils.pdfviewer.util.FitPolicy
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.security.cert.CertPathValidatorException

class LoginFragment : Fragment() {
    private lateinit var binding: FragmentLoginBinding
    private val auth = Firebase.auth
    private lateinit var terms: PopupTermsBinding
    private lateinit var popupWindow: PopupWindow
    private lateinit var loadingDialog: LoadingDialog

    private fun openTerms(userData : User?) {
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
                    auth.signOut()
                    Toast.makeText(context, "Try again and accept the terms.", Toast.LENGTH_LONG).show()
                }
                terms.accept.setOnClickListener {
                    it.tempDisable()
                    dismiss()
                    loadingDialog.show("Login ...")
                    Firebase.firestore.collection("Users")
                        .document(Firebase.auth.currentUser!!.uid)
                        .set(hashMapOf("isTermsAccepted" to true), SetOptions.merge())
                        .addOnCompleteListener { task ->
                            if (task.exception != null){
                                Toast.makeText(context, task.exception!!.message, Toast.LENGTH_SHORT).show()
                            }
                            gotoMainActivity(Firebase.auth.currentUser!!)
                        }
                }

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
        TransitionManager.beginDelayedTransition(binding.root)
        popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
    }

    private fun gotoMainActivity(currentUser : FirebaseUser) {
        Firebase.firestore.collection("Users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { userSnapshot ->
                val user = userSnapshot.toObject<User>()
                if (user != null){
                    user.myModulesList?.let {
                        MyModulesLocalDatabase(context).addModulesFromFirebase(it)
                    }
                    loadingDialog.isDone {
                        startActivity(Intent(activity, MainActivity::class.java).apply {
                            putExtra(MainActivity.USER_ARG, user)
                        })
                        activity?.finishAffinity()
                    }
                } else {
                    loadingDialog.isError("User information not found!") {
                        Firebase.auth.signOut()
                    }
                }
            }
            .addOnFailureListener {
                loadingDialog.isError(it.message) {
                    Firebase.auth.signOut()
                }
            }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(activity)
        return binding.root
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
            (activity as AuthActivity?)?.let { authActivity ->
                if (authActivity.isOnline.value == true) {

                    val email = binding.email.editText!!.text.toString().trim()
                    val password = binding.password.editText!!.text.toString()

                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        loadingDialog.show("Login ...")
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener { authR ->
                                if (authR.user != null){
                                    gotoMainActivity(authR.user!!)
                                } else {
                                    loadingDialog.isError("Invalid User") { }
                                }
                            }
                            .addOnFailureListener { exception->
                                // If sign in fails, display a message to the user.
                                loadingDialog.isError(exception.message) { }
                            }
                    } else if (email.isEmpty()){
                        binding.email.error = "Email Required"
                    } else {
                        binding.password.error = "Password Required"
                    }
                } else {
                    Toast.makeText(context, "No interest connection.\nConnect to your WiFi or Mobile data.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (requireContext().isGooglePlayServicesAvailable()) {
            binding.loginWithGoogle.setOnClickListener {
                it.tempDisable()
                activity?.hideKeyboard(it)
                (activity as AuthActivity?)?.let { authActivity ->
                    if (authActivity.isOnline.value == true) {
                        // Configure Google Sign In
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.web_client_id))
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
                        activityForResult.launch(googleSignInClient.signInIntent)
                    } else {
                        binding.loginButton.isEnabled = true
                        Toast.makeText(context, "No interest connection.\nConnect to your WiFi or Mobile data.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            binding.loginWithGoogle.visibility = View.GONE
        }
    }

    private val activityForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            loadingDialog.show("Login ...")
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                loadingDialog.isError(task.exception?.message){}
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnFailureListener { exception ->
                if (exception.message == CertPathValidatorException().message) {
                    loadingDialog.dismiss()
                    AlertDialog.Builder(requireContext())
                        .setTitle("Trust certification not found.")
                        .setMessage("Validate your WiFi connection, or switch to Mobile data and, try again.")
                        .show()
                } else {
                    loadingDialog.isError(exception.message) {

                    }
                }
            }
            .addOnSuccessListener {
                gotoMainActivity(it.user!!)
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
}