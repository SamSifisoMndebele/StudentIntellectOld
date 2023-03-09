package com.avidco.studentintellect.activities.ui.materials.edit

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.ProfileViewModel
import com.avidco.studentintellect.databinding.FragmentEditFolderBinding
import com.avidco.studentintellect.models.FileData
import com.avidco.studentintellect.models.FolderData
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.utils.LoadingDialog
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.roundToRand
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import java.io.IOException

class EditFolderFragment : Fragment() {

    private lateinit var moduleData: ModuleData
    private var parentFolderData: FolderData? = null
    private var folderData: FolderData? = null

    private val auth = Firebase.auth
    private val database = Firebase.firestore
    private val storage = Firebase.storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        moduleData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(MODULE_DATA, ModuleData::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(MODULE_DATA)!!
        }
        parentFolderData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(PARENT_FOLDER_DATA, FolderData::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(PARENT_FOLDER_DATA)
        }
        folderData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(FOLDER_DATA, FolderData::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(FOLDER_DATA)
        }
    }

    private lateinit var binding: FragmentEditFolderBinding
    private lateinit var loadingDialog: LoadingDialog

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditFolderBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(activity)
        val modulePath = "Modules/${moduleData.code}"
        val folderPath = parentFolderData?.path ?: ""
        val path = modulePath + folderPath

        (requireActivity() as MainActivity).supportActionBar?.title =
            if (folderData != null) "Edit ${folderData!!.name}" else "Add a folder"

        binding.folderPath.isEnabled = false
        binding.folderPath.editText?.setText(moduleData.code+folderPath.replace("/Folders", ""))
        binding.folderName.editText
        binding.saveFolder.setOnClickListener {
            it.tempDisable()
            activity?.hideKeyboard(it)
            uploadMaterial()

        }

        return binding.root
    }

    private fun TextInputLayout.showError() {
        boxBackgroundColor = resources.getColor(R.color.red_transparent)
        Handler(Looper.getMainLooper()).postDelayed({
            boxBackgroundColor = resources.getColor(R.color.transparent)
            Handler(Looper.getMainLooper()).postDelayed({
                boxBackgroundColor = resources.getColor(R.color.red_transparent)
                Handler(Looper.getMainLooper()).postDelayed({
                    boxBackgroundColor = resources.getColor(R.color.transparent)
                }, 1000)
            }, 250)
        }, 250)
    }

    private fun validateAllFields() : Boolean {

        if (binding.folderName.editText?.text?.trim().isNullOrEmpty()) {
            binding.folderName.showError()
            return false
        }

        return true
    }

    private fun uploadMaterial() {

        if (!validateAllFields()){
            return
        }
        loadingDialog.show("Uploading...")

        val folderName = binding.folderName.editText?.text.toString().trim()
        val modulePath2 = "Modules/${moduleData.code}"
        val folderPath = parentFolderData?.path ?: ""
        val path = modulePath2 + folderPath

        database.collection("$path/Folders")
            .whereEqualTo("name", folderName)
            .limit(1)
            .get()
            .addOnSuccessListener {
                val isEmpty = it.isEmpty
                if (isEmpty) {
                    val document = database.document("$path/Folders/$folderName")
                    document.set(FolderData(document.id, document.path))
                        .addOnSuccessListener {
                            (activity as MainActivity?)?.navController?.navigateUp()
                            loadingDialog.isDone("Added successfully.") {  }
                        }
                        .addOnFailureListener { error->
                            loadingDialog.isError(error.message) { }
                        }
                }
                else {
                    loadingDialog.isError("Folder Exist!") { }
                }
            }
            .addOnFailureListener {
                loadingDialog.isError(it.message) { }
            }
    }

    companion object {
        const val MODULE_DATA = "arg_module_data_folder"
        const val FOLDER_DATA = "arg_folder_data_folder"
        const val PARENT_FOLDER_DATA = "arg_parent_folder_data_folder"
    }
}