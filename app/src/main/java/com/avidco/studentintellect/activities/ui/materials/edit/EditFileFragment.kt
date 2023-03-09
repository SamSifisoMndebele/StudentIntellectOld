package com.avidco.studentintellect.activities.ui.materials.edit

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.ProfileViewModel
import com.avidco.studentintellect.activities.ui.materials.MaterialsFragment
import com.avidco.studentintellect.activities.ui.upload.UploadFragment222
import com.avidco.studentintellect.databinding.FragmentEditFileBinding
import com.avidco.studentintellect.models.FileData
import com.avidco.studentintellect.models.FolderData
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.utils.Constants
import com.avidco.studentintellect.utils.Extensions
import com.avidco.studentintellect.utils.LoadingDialog
import com.avidco.studentintellect.utils.OpenDocumentContract
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.isOnline
import com.avidco.studentintellect.utils.Utils.roundToRand
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import java.io.IOException

class EditFileFragment : Fragment() {

    companion object {
        const val MODULE_DATA = "arg_module_data_file"
        const val FILE_DATA = "arg_file_data_file"
        const val PARENT_FOLDER_DATA = "arg_parent_folder_data_folder"
    }

    private lateinit var moduleData: ModuleData
    private var parentFolderData: FolderData? = null
    private var fileData: FileData? = null

    private val auth = Firebase.auth
    private val database = Firebase.firestore
    private val storage = Firebase.storage

    private var materialUri : Uri? = null
    private var solutionsUri : Uri? = null

    private var solutionsSize = 0.0
    private var materialSize = 0.0

    @SuppressLint("SetTextI18n")
    private val selectSolutionsResult = registerForActivityResult(OpenDocumentContract()) { uri: Uri? ->
        uri?.let {
            solutionsUri = uri
            binding.selectSolutionsImage.setImageResource(R.drawable.ic_pdf)

            var cursor : Cursor? = null
            try {
                cursor = context?.contentResolver?.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()){
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    solutionsSize = (cursor.getDouble(sizeIndex)/1000000).roundToRand().toDouble()
                    binding.selectSolutionsText.text = "$solutionsSize MB | "+cursor.getString(nameIndex)
                }
            } finally {
                cursor?.close()
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private val selectDocumentResult = registerForActivityResult(OpenDocumentContract()) { uri: Uri? ->
        uri?.let {
            materialUri = uri
            binding.selectMaterialImage.setImageResource(R.drawable.ic_pdf)

            var cursor : Cursor? = null
            try {
                cursor = context?.contentResolver?.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()){
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    materialSize = (cursor.getDouble(sizeIndex)/1000000).roundToRand().toDouble()
                    binding.selectMaterialText.text = "$materialSize MB | "+cursor.getString(nameIndex)
                }
            } finally {
                cursor?.close()
            }
        }
    }


    private var rewardedAd: RewardedAd? = null
    private var rewardItem : RewardItem? = null
    private fun loadAd(showAd : Boolean = false) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(requireActivity(),requireActivity().getString(R.string.upload_material_rewardedAdUnitId), adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                println( adError.toString())
                rewardedAd = null
                /*binding.watchAd.setCardBackgroundColor(resources.getColor(R.color.grey))
                binding.watchAd.setOnClickListener {
                    it.tempDisable()
                    Toast.makeText(context, "Loading ad ...", Toast.LENGTH_SHORT).show()
                    loadAd(true)
                }*/
                if (showAd) {
                    Toast.makeText(context, "Loading ad error: ${adError.message}\nTry again.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onAdLoaded(ad: RewardedAd) {
                println(  "Ad was loaded.")
                rewardedAd = ad
                // binding.watchAd.setCardBackgroundColor(resources.getColor(R.color.primaryColor))

                if (showAd && validateAllFields()) {
                    rewardedAd?.show(requireActivity()) { rewardItem->
                        this@EditFileFragment.rewardItem = rewardItem
                        val amount = if (solutionsUri != null) {
                            (rewardItem.amount.toDouble()/100.0).roundToRand()
                        } else {
                            (rewardItem.amount.toDouble()/200.0).roundToRand()
                        }
                        Toast.makeText(context, "You earned the R$amount reward.", Toast.LENGTH_SHORT).show()
                        rewardedAd = null
                        //binding.watchAd.setCardBackgroundColor(resources.getColor(R.color.grey))
                    }
                    //binding.watchAd.setOnClickListener(null)
                } else {
                    /*binding.watchAd.setOnClickListener {
                        it.tempDisable()
                        if (validateAllFields()) {
                            rewardedAd?.show(requireActivity()) { rewardItem->
                                this@UploadFragment.rewardItem = rewardItem
                                val amount = if (solutionsUri != null) {
                                    (rewardItem.amount.toFloat()/100f).roundToRand()
                                } else {
                                    (rewardItem.amount.toFloat()/200f).roundToRand()
                                }
                                binding.rewardBalance.text = amount
                                Toast.makeText(context, "You earned the R$amount reward.", Toast.LENGTH_SHORT).show()
                                rewardedAd = null
                            }
                        }
                    }*/
                }
            }
        })
    }

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
        fileData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(FILE_DATA, FileData::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(FILE_DATA)
        }

        (requireActivity() as MainActivity).supportActionBar?.title =
            if (fileData != null) "Edit ${fileData!!.name}" else "Add a pdf file"
    }

    private lateinit var binding: FragmentEditFileBinding

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditFileBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(activity)
        loadAd()

        val modulePath = "Modules/${moduleData.code}"
        val folderPath = parentFolderData?.path ?: ""
        val path = modulePath + folderPath

        (requireActivity() as MainActivity).supportActionBar?.title =
            if (fileData != null) "Edit ${fileData!!.name}" else "Add a pdf file"

        binding.folderPath.isEnabled = false
        binding.folderPath.editText?.setText(moduleData.code+folderPath.replace("/Folders", ""))


        binding.uploadMaterial.setOnClickListener {
            it.tempDisable()
            activity?.hideKeyboard(it)
            uploadMaterial()
        }

        binding.selectMaterial.setOnClickListener {
            it.tempDisable()
            activity?.hideKeyboard(it)
            selectFile(false)
        }
        binding.selectSolutions.setOnClickListener {
            it.tempDisable();
            activity?.hideKeyboard(it)
            selectFile(true)
        }

        val materialNameAdapter = ArrayAdapter.createFromResource(requireContext(),R.array.materials_array,
            android.R.layout.simple_spinner_dropdown_item)
        binding.materialNameAutoComplete.setAdapter(materialNameAdapter)


        /*
        binding.materialYearSpinner.adapter = yearAdapter
        binding.materialYearSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?,
                    position: Int, id: Long
                ) {
                    materialYear = parent.getItemAtPosition(position).toString()
                }
                override fun onNothingSelected(parent : AdapterView<*>) {
                    materialYear = parent.getItemAtPosition(0).toString()
                }
            }*/

        return binding.root
    }

    private fun selectFile(isSolutions : Boolean) {
        if (isSolutions) {
            if (solutionsUri != null){
                val choice = arrayOf<CharSequence>("Change File","Remove File")
                AlertDialog.Builder(context).apply {
                    setItems(choice) { _, item ->
                        when {
                            choice[item] == "Change File" -> {
                                selectSolutionsResult.launch(arrayOf("application/pdf"))
                            }
                            choice[item] == "Remove File" -> {
                                binding.selectSolutionsImage.setImageResource(R.drawable.ic_pdf_upload)
                                binding.selectSolutionsText.text = getString(R.string.select_the_study_material_solutions_pdf_file)
                                solutionsUri = null
                            }
                        }
                    }
                    show()
                }
            } else {
                selectSolutionsResult.launch(arrayOf("application/pdf"))
            }
        }
        else {
            if (materialUri != null){
                val choice = arrayOf<CharSequence>("Change File","Remove File")
                AlertDialog.Builder(context).apply {
                    setItems(choice) { _, item ->
                        when {
                            choice[item] == "Change File" -> {
                                selectDocumentResult.launch(arrayOf("application/pdf"))
                            }
                            choice[item] == "Remove File" -> {
                                binding.selectMaterialImage.setImageResource(R.drawable.ic_pdf_upload)
                                binding.selectMaterialText.text = getString(R.string.select_a_study_material_pdf_file)
                                materialUri = null
                            }
                        }
                    }
                    show()
                }
            } else {
                selectDocumentResult.launch(arrayOf("application/pdf"))
            }
        }
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

        if (materialUri == null) {
            binding.selectMaterialInput.showError()
            return false
        }

        if (binding.materialNameAutoComplete.text?.trim().isNullOrEmpty()) {
            binding.materialNameInput.showError()
            return false
        }

        return true
    }

    private lateinit var loadingDialog : LoadingDialog

    private fun uploadMaterial() {

        if (!validateAllFields()){
            return
        }
        loadingDialog.show("Uploading...")

        val materialName = binding.materialNameAutoComplete.text.toString().trim()
        val modulePath2 = "Modules/${moduleData.code}"
        val folderPath = parentFolderData?.path ?: ""
        val path = modulePath2 + folderPath

        database.collection("$path/Files")
            .whereEqualTo("name", materialName)
            .limit(1)
            .get()
            .addOnSuccessListener {
                val isEmpty = it.isEmpty
                if (isEmpty) {
                    val document = database.document("$path/Files/$materialName")
                    val storageRef = storage.getReference(path.replace("/Folders", ""))
                    try {
                        val materialRef = storageRef.child("${document.id}.pdf")
                        val metadata = storageMetadata {
                            setCustomMetadata("Uploader Name", auth.currentUser?.displayName)
                        }
                        val uploadTask = materialRef.putFile(materialUri!!, metadata)
                        uploadTask.continueWithTask { task->
                            if (!task.isSuccessful) {
                                task.exception?.let { exception ->
                                    throw exception
                                }
                            }
                            materialRef.downloadUrl
                        }
                            .addOnSuccessListener { materialUri ->
                                if (solutionsUri != null){
                                    val solutionsRef = storageRef.child("${document.id} Solutions.pdf")
                                    val solutionsUploadTask = solutionsRef.putFile(solutionsUri!!, metadata)
                                    solutionsUploadTask.continueWithTask { task ->
                                        if (!task.isSuccessful) {
                                            task.exception?.let { err ->
                                                throw err
                                            }
                                        }
                                        solutionsRef.downloadUrl
                                    }
                                        .addOnSuccessListener { solutionsUri ->
                                            document.set(FileData(document.id,document.id, materialUri.toString(), materialSize, solutionsUri.toString(), solutionsSize))
                                                .addOnSuccessListener {
                                                    (activity as MainActivity?)?.navController?.navigateUp()

                                                    if (rewardItem != null) {
                                                        val amount = rewardItem!!.amount/100.0
                                                        loadingDialog.isDone("Uploaded successfully.\nYou earned R${amount.roundToRand()} reward.") {  }

                                                        database.collection("users")
                                                            .document(auth.currentUser!!.uid)
                                                            .update("balance", FieldValue.increment(amount.toDouble()))

                                                        val prefs = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
                                                        val dataViewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
                                                        dataViewModel.setBalance(amount)
                                                        rewardItem = null
                                                    } else {
                                                        loadingDialog.isDone("Uploaded successfully.") {  }
                                                    }
                                                    loadAd()
                                                }
                                                .addOnFailureListener { err ->
                                                    loadingDialog.isError(err.message) { }
                                                }
                                        }
                                        .addOnFailureListener { err ->
                                            loadingDialog.isError(err.message) { }
                                        }

                                    solutionsUploadTask.addOnProgressListener { uploadTaskSnap->
                                        val progress = (100.0 * uploadTaskSnap.bytesTransferred) / uploadTaskSnap.totalByteCount
                                        // binding.progressLayout.showProgress("Uploading Solutions…\n${progress.toInt()}%")
                                    }
                                }
                                else {
                                    document.set(FileData(document.id,document.id, materialUri.toString(), materialSize))
                                        .addOnSuccessListener {
                                            (activity as MainActivity?)?.navController?.navigateUp()

                                            if (rewardItem != null){
                                                val amount = rewardItem!!.amount/200.0

                                                database.collection("users")
                                                    .document(auth.currentUser!!.uid)
                                                    .update("balance", FieldValue.increment(amount.toDouble()))

                                                loadingDialog.isDone("Uploaded successfully.\nYou earned R${amount.roundToRand()} reward.") {  }

                                                val prefs = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
                                                val dataViewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
                                                dataViewModel.setBalance(amount)
                                                rewardItem = null
                                            } else {
                                                loadingDialog.isDone("Uploaded successfully.") {  }
                                            }
                                            loadAd()
                                        }
                                        .addOnFailureListener { error->
                                            loadingDialog.isError(error.message) { }
                                        }
                                }
                            }
                            .addOnFailureListener { error ->
                                loadingDialog.isError(error.message) { }
                            }

                        uploadTask.addOnProgressListener { uploadTaskSnap->
                            val progress = (100.0 * uploadTaskSnap.bytesTransferred) / uploadTaskSnap.totalByteCount
                            // binding.progressLayout.showProgress("Uploading Material…\n${progress.toInt()}%")
                        }

                    } catch (e: IOException) {
                        e.printStackTrace()
                        loadingDialog.isError(e.message) { }
                    }
                }
                else {
                    loadingDialog.isError("Material Exist!") { }
                }
            }
            .addOnFailureListener {
                loadingDialog.isError(it.message) { }
            }
    }
}