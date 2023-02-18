package com.avidco.studentintellect.activities.auth

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.doOnTextChanged
import com.avidco.studentintellect.R
import com.avidco.studentintellect.databinding.FragmentRegisterBinding
import com.avidco.studentintellect.databinding.PopupTermsBinding
import com.avidco.studentintellect.utils.Constants
import com.avidco.studentintellect.utils.OpenPicturesContract
import com.avidco.studentintellect.models.UserType
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.avidco.studentintellect.utils.pdfviewer.util.FitPolicy
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.IOException
import java.lang.Exception

class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var terms: PopupTermsBinding
    private lateinit var popupWindow : PopupWindow
    private var imageUri : Uri? = null
    
    private val auth = Firebase.auth
    private val storage =  Firebase.storage

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root

    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.termsOfService.setOnClickListener{ b ->
            b.tempDisable()
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

                    terms.termsClose.setOnClickListener{ b ->
                        b.tempDisable()
                        dismiss()
                    }
                    terms.accept.setOnClickListener {
                        it.tempDisable()
                        dismiss()
                        binding.acceptTermsOfService.isChecked = true
                    }
                    loadPdf()
                }
            TransitionManager.beginDelayedTransition(binding.root)
            popupWindow.showAtLocation(binding.root, Gravity.CENTER, 0, 0)

        }

        binding.selectImage.setOnClickListener {
            it.tempDisable()
            selectImage()
        }

        validateInput()

        binding.registerButton.setOnClickListener {
            it.tempDisable()
            val username = binding.username.editText!!.text.toString().trim()
            val email = binding.email.editText!!.text.toString().trim()
            val password = binding.password.editText!!.text.toString()
            val passwordConfirm = binding.passwordConfirm.editText!!.text.toString()

            if (username.isEmpty()){
                binding.username.error = "Enter your name."
                return@setOnClickListener
            }
            if (email.isEmpty()){
                binding.email.error = "Enter your email address."
                return@setOnClickListener
            }
            if (password.isEmpty()){
                binding.password.error = "Create a password."
                return@setOnClickListener
            }
            if (passwordConfirm.isEmpty()){
                binding.passwordConfirm.boxStrokeErrorColor = ColorStateList.valueOf(Color.parseColor("#B00020"))
                binding.passwordConfirm.setErrorTextColor(ColorStateList.valueOf(Color.parseColor("#B00020")))
                binding.passwordConfirm.setErrorIconTintList(ColorStateList.valueOf(Color.parseColor("#B00020")))
                binding.passwordConfirm.error = "Confirm your password."
                return@setOnClickListener
            }
            if (password != passwordConfirm){
                binding.passwordConfirm.boxStrokeErrorColor = ColorStateList.valueOf(Color.parseColor("#F6BE67"))
                binding.passwordConfirm.setErrorTextColor(ColorStateList.valueOf(Color.parseColor("#F6BE67")))
                binding.passwordConfirm.setErrorIconTintList(ColorStateList.valueOf(Color.parseColor("#F6BE67")))
                binding.passwordConfirm.error = "Passwords does not match."
                return@setOnClickListener
            }
            if (!binding.acceptTermsOfService.isChecked){
                Toast.makeText(context, "Read and accept the terms and conditions.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

           // binding.registerButton.startAnimation()
            binding.responseRegister.visibility = View.VISIBLE
            binding.responseRegister.text = getString(R.string.creating_account)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { createUserTask ->
                    if (createUserTask.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Firebase.firestore.collection("users")
                            .document(Firebase.auth.currentUser!!.uid)
                            .set(hashMapOf("userType" to if (binding.toggleUserType.isChecked)
                                UserType.TUTOR else UserType.STUDENT,
                                "termsAccepted" to true), SetOptions.merge())
                            .addOnCompleteListener { task->
                                if (task.exception != null) {
                                    showException(task.exception!!)
                                }
                                if (imageUri != null) {
                                    updateImage(username)
                                }
                                else {
                                    val profileUpdates = userProfileChangeRequest {
                                        displayName = username
                                    }
                                    updateProfile(profileUpdates)
                                }
                            }
                    }
                    else {
                        // If sign in fails, display a message to the user.
                        showException(createUserTask.exception!!)
                    }
                }
        }
    }

    private fun updateImage(username: String) {
        try {
            binding.responseRegister.text = getString(R.string.iploading_image)
            val userRef =  storage.getReference("users").child("${auth.currentUser!!.uid}.png")
            val uploadTask = userRef.putFile(imageUri!!)
            uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    userRef.downloadUrl
                }
                .addOnSuccessListener { imageUrl ->
                    val profileUpdates = userProfileChangeRequest {
                        displayName = username
                        photoUri = imageUrl
                    }
                    updateProfile(profileUpdates)
                }
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful && task.exception != null) {
                        showException(task.exception!!)
                    }
                    val profileUpdates = userProfileChangeRequest {
                        displayName = username
                    }
                    updateProfile(profileUpdates)
                }
            uploadTask.addOnProgressListener {
                val progress = (100.0 * it.bytesTransferred) / it.totalByteCount
                binding.responseRegister.text = "Uploading image... ${progress.toInt()}%"
            }

        } catch (e: IOException) {
            e.printStackTrace()
            showException(e)
            val profileUpdates = userProfileChangeRequest {
                displayName = username
            }
            updateProfile(profileUpdates)
        }
    }

    private fun updateProfile(profileUpdates: UserProfileChangeRequest) {
        binding.responseRegister.text = getString(R.string.updating_profile)
        auth.currentUser!!.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.exception != null){
                    showException(task.exception!!)
                } else {
                    val checkMark = BitmapFactory.decodeResource(context?.resources, R.drawable.ic_checked)
                   // binding.registerButton.doneLoadingAnimation(Color.parseColor("#3BB54A"), checkMark)
                }
                val prefs = requireContext().getSharedPreferences("pref", Context.MODE_PRIVATE)
                prefs.edit().putInt("userType", if (binding.toggleUserType.isChecked)
                    UserType.TUTOR else UserType.STUDENT).apply()

                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(activity, MainActivity::class.java))
                    activity?.finish()
                }, 1000)
            }
    }

    private fun validateInput() {
        binding.username.editText!!.doOnTextChanged { _,  _,  _,  _ ->
            binding.username.isErrorEnabled = false
        }
        binding.email.editText!!.doOnTextChanged { _,  _,  _,  _ ->
            binding.email.isErrorEnabled = false
        }
        binding.email.editText!!.setOnFocusChangeListener { _, b ->
            if (!b && binding.email.editText!!.text.trim().isNotEmpty() && !binding.email.editText!!.text.trim().toString().matches(
                    Constants.EMAIL_PATTERN.toRegex())) {
                binding.email.error = "Invalid email address."
            }
        }
        binding.password.editText!!.doOnTextChanged { password,  _,  _,  _ ->
            binding.password.isErrorEnabled = false
            if (password.toString() == binding.passwordConfirm.editText!!.text.toString()){
                binding.passwordConfirm.isErrorEnabled = false
            }
        }
        binding.passwordConfirm.editText!!.doOnTextChanged { passwordConfirm,  _,  _,  _ ->
            binding.passwordConfirm.isErrorEnabled = false
            if (passwordConfirm.toString().isNotEmpty() && binding.password.editText!!.text.toString() != passwordConfirm.toString()){
                binding.passwordConfirm.boxStrokeErrorColor = ColorStateList.valueOf(Color.parseColor("#F6BE67"))
                binding.passwordConfirm.setErrorTextColor(ColorStateList.valueOf(Color.parseColor("#F6BE67")))
                binding.passwordConfirm.setErrorIconTintList(ColorStateList.valueOf(Color.parseColor("#F6BE67")))
                binding.passwordConfirm.error = "Passwords does not match."
            } else {
                binding.passwordConfirm.isErrorEnabled = false
                binding.passwordConfirm.boxStrokeErrorColor = ColorStateList.valueOf(Color.parseColor("#B00020"))
                binding.passwordConfirm.setErrorTextColor(ColorStateList.valueOf(Color.parseColor("#B00020")))
                binding.passwordConfirm.setErrorIconTintList(ColorStateList.valueOf(Color.parseColor("#B00020")))
            }
        }
    }

    private fun showException(exception: Exception){
        Toast.makeText(context, exception.message, Toast.LENGTH_LONG).show()
        val warning = BitmapFactory.decodeResource(context?.resources, R.drawable.ic_warning)
      //  binding.registerButton.doneLoadingAnimation(Color.parseColor("#EE5253"), warning)
        Handler(Looper.getMainLooper()).postDelayed({
            //binding.registerButton.revertAnimation()
        }, 1000)
    }

    private val selectImageResult = registerForActivityResult(OpenPicturesContract()) { uri: Uri? ->
        uri?.let {
            imageUri = uri
            binding.userImage.setImageURI(imageUri)
            binding.userImageText.text = getString(R.string.select_your_photo)

            var cursor : Cursor? = null
            try {
                cursor = requireContext().applicationContext.contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()){
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    binding.userImageText.text = cursor.getString(nameIndex)
                }
            } finally {
                cursor?.close()
            }
        }
    }
    private fun selectImage() {
        if (imageUri != null) {
            val choice = arrayOf<CharSequence>("Change Image","Remove Image")
            AlertDialog.Builder(context).apply {
                setItems(choice) { _, item ->
                    when {
                        choice[item] == "Change Image" -> {
                            selectImageResult.launch(arrayOf("image/*"))
                        }
                        choice[item] == "Remove Image" -> {
                            binding.userImage.setImageResource(R.drawable.ic_user)
                            binding.userImageText.text = getString(R.string.select_your_photo)
                            imageUri = null
                        }
                    }
                }
                show()
            }
        } else {
            selectImageResult.launch(arrayOf("image/*"))
        }
    }
}