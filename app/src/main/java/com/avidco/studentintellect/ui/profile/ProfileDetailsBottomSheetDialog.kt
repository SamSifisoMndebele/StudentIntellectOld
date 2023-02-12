package com.avidco.studentintellect.ui.profile

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import com.avidco.studentintellect.R
import com.avidco.studentintellect.databinding.SheetProfileDetailsBinding
import com.avidco.studentintellect.ui.DataViewModel
import com.avidco.studentintellect.ui.MainActivity
import com.avidco.studentintellect.utils.OpenPicturesContract
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.IOException

class ProfileDetailsBottomSheetDialog(val activity: MainActivity) : BottomSheetDialogFragment() {

    private lateinit var binding: SheetProfileDetailsBinding

    private val user = Firebase.auth.currentUser

    private val selectImageResult = registerForActivityResult(OpenPicturesContract()) { uri: Uri? ->
        uri?.let { setImage(uri) }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        activity.hideKeyboard(binding.root)
    }
    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cancelButton.setOnClickListener {
            it.tempDisable()
            activity.hideKeyboard(binding.root)
            dismiss()
        }

        binding.username.editText?.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                isCancelable = true
                binding.saveButton.visibility = View.INVISIBLE
            } else {
                isCancelable = false
                binding.saveButton.visibility = View.VISIBLE
            }
        }

        binding.saveButton.setOnClickListener {
            it.tempDisable()
            setName(binding.username.editText?.text?.trim().toString())
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
    }

    private fun setName(userName : String){
        binding.progressBar.visibility = View.VISIBLE

        user?.updateProfile(userProfileChangeRequest {
            displayName = userName.trim()
        })?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                context?.hideKeyboard(binding.root)
                dismiss()
                val dataViewModel = ViewModelProvider(activity)[DataViewModel::class.java]
                dataViewModel.setDisplayName(userName.trim())
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setImage(imageUri : Uri?) {
        binding.progressBar.visibility = View.VISIBLE
        try {
            if (imageUri != null){
                val userRef =  Firebase.storage.getReference("users").child("${user?.uid}.pdf")
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
                        user?.updateProfile(userProfileChangeRequest {
                            photoUri = imageUrl })
                            ?.addOnCompleteListener { task ->
                                if (task.exception != null){
                                    Toast.makeText(context, task.exception!!.message, Toast.LENGTH_LONG).show()
                                    binding.progressBar.visibility = View.GONE
                                } else {
                                    dismiss()
                                    val dataViewModel = ViewModelProvider(activity)[DataViewModel::class.java]
                                    dataViewModel.setPhotoUrl(imageUrl.toString())
                                }
                            }
                    }
                    .addOnCompleteListener { task ->
                        if (!task.isSuccessful && task.exception != null) {
                            Toast.makeText(context, task.exception!!.message, Toast.LENGTH_LONG).show()
                            binding.progressBar.visibility = View.GONE
                        }
                    }
            } else {
                binding.userImage.setImageResource(R.drawable.ic_user)
                user?.updateProfile(userProfileChangeRequest {
                    photoUri = null })
                    ?.addOnCompleteListener { task ->
                        if (task.exception != null){
                            Toast.makeText(context, task.exception!!.message, Toast.LENGTH_LONG).show()
                            binding.progressBar.visibility = View.GONE
                        } else {
                            dismiss()
                            val dataViewModel = ViewModelProvider(activity)[DataViewModel::class.java]
                            dataViewModel.setPhotoUrl(null)
                        }
                    }
            }


        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = SheetProfileDetailsBinding.inflate(inflater,container,false)

        val photoUrl = user?.photoUrl
        if (photoUrl != null) {
            binding.removeImageButton.visibility = View.VISIBLE
            Glide.with(binding.userImage)
                .load(photoUrl)
                .placeholder(R.drawable.ic_user)
                .into(binding.userImage)
        }

        binding.username.editText?.setText(user?.displayName)

        return binding.root
    }
}








