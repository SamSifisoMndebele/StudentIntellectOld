package com.avidco.studentintellect.activities.ui.materials.folders

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.database.FoldersFirestoreDatabase
import com.avidco.studentintellect.activities.ui.database.FoldersFirestoreDatabase.Companion.addOnFailureListener
import com.avidco.studentintellect.activities.ui.database.FoldersFirestoreDatabase.Companion.addOnSuccessListener
import com.avidco.studentintellect.databinding.FragmentEditFolderBinding
import com.avidco.studentintellect.models.Folder
import com.avidco.studentintellect.models.Module
import com.avidco.studentintellect.utils.LoadingDialog
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EditFolderFragment : Fragment() {
    private lateinit var binding: FragmentEditFolderBinding
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var databaseFolders: FoldersFirestoreDatabase
    private lateinit var path: String

    private lateinit var myModuleData: Module
    private var parentFolder: Folder? = null
    private var folder: Folder? = null
    private var pathDisplayText: String? = null
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(MY_MODULE_DATA, myModuleData)
        outState.putParcelable(PARENT_FOLDER_DATA, parentFolder)
        outState.putParcelable(FOLDER_DATA, folder)
        outState.putString(PATH_DISPLAY_TEXT, pathDisplayText)
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()
        if (folder != null) {
            (requireActivity() as MainActivity).supportActionBar!!.title = "Edit Folder \"${folder!!.name}\""
            binding.saveFolder.text = "Rename the folder"
        } else {
            (requireActivity() as MainActivity).supportActionBar!!.title = "Add a Folder"
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditFolderBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(activity)

        val myArgs = savedInstanceState ?: requireArguments()

        myModuleData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            myArgs.getParcelable(MY_MODULE_DATA, Module::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            myArgs.getParcelable(MY_MODULE_DATA)!!
        }
        parentFolder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            myArgs.getParcelable(PARENT_FOLDER_DATA, Folder::class.java)
        } else {
            @Suppress("DEPRECATION")
            myArgs.getParcelable(PARENT_FOLDER_DATA)
        }
        folder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            myArgs.getParcelable(FOLDER_DATA, Folder::class.java)
        } else {
            @Suppress("DEPRECATION")
            myArgs.getParcelable(FOLDER_DATA)
        }
        pathDisplayText = myArgs.getString(PATH_DISPLAY_TEXT, myModuleData.code)
        binding.folderPath.isEnabled = false
        binding.folderPath.editText?.setText(pathDisplayText)
        binding.folderName.editText?.setText(folder?.name)

        path = parentFolder?.path ?: "Modules/${myModuleData.id}"
        val foldersTableName = path.replace(" ", "")
            .replace("Modules", "Folders")
            .replace("/", "_")+"_Table"
        databaseFolders = FoldersFirestoreDatabase(requireContext(), path, foldersTableName)


        binding.saveFolder.setOnClickListener {
            it.tempDisable()
            activity?.hideKeyboard(it)
            uploadMaterial()
        }

        loadingDialog.onCancel {
            (activity as MainActivity?)?.navController?.navigateUp()
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
        if (!validateAllFields()){ return }
        val folderName = binding.folderName.editText?.text.toString().trim()
        if (folder != null) {
            loadingDialog.show("Renaming...")
            val updateFolder = folder!!.apply {
                this.name = folderName
                this.timeUpdated = Timestamp.now()
            }
            databaseFolders.update(updateFolder)
                .addOnSuccessListener {
                    loadingDialog.isDone("Renamed Successfully.") {
                        (activity as MainActivity?)?.navController?.navigateUp()
                    }
                }
                .addOnFailureListener { error ->
                    loadingDialog.isError(error.message) { }
                }
        } else {
            loadingDialog.show("Creating...")
            Firebase.firestore.collection("$path/Folders")
                .whereEqualTo("name", folderName)
                .limit(1).get()
                .addOnSuccessListener {
                    if (it.isEmpty) {
                        databaseFolders.putNew(folderName)
                            .addOnSuccessListener {
                                loadingDialog.isDone("Added successfully.") {
                                    (activity as MainActivity?)?.navController?.navigateUp()
                                }
                            }
                            .addOnFailureListener { error, _ ->
                                loadingDialog.isError(error.message) { }
                            }
                    }
                    else {
                        AlertDialog.Builder(context)
                            .setMessage("Folder \"$folderName\" exists. Do you want to create a folder with the same name?")
                            .setCancelable(false)
                            .setPositiveButton("Create Folder"){ d,_->
                                d.dismiss()
                                databaseFolders.putNew(folderName)
                                    .addOnSuccessListener {
                                        loadingDialog.isDone("Added successfully.") {
                                            (activity as MainActivity?)?.navController?.navigateUp()
                                        }
                                    }
                                    .addOnFailureListener { error, _ ->
                                        loadingDialog.isError(error.message) { }
                                    }
                            }
                            .setNegativeButton("Cancel"){d,_->
                                d.dismiss()
                                loadingDialog.dismiss()
                            }
                            .show()
                    }
                }
                .addOnFailureListener {
                    loadingDialog.isError(it.message) { }
                }
        }



    }

    companion object {
        const val MY_MODULE_DATA = "arg_my_module_data_folder"
        const val FOLDER_DATA = "arg_folder_data_folder"
        const val PARENT_FOLDER_DATA = "arg_parent_folder_data_folder"
        const val PATH_DISPLAY_TEXT = "arg_path_display_text"
    }
}