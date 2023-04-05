package com.avidco.studentintellect.activities.ui.materials.files

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import android.widget.ArrayAdapter
import android.widget.Toast
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.database.FirestoreFiles
import com.avidco.studentintellect.activities.ui.database.FirestoreFiles.Companion.addOnFailureListener
import com.avidco.studentintellect.activities.ui.database.FirestoreFiles.Companion.addOnSuccessListener
import com.avidco.studentintellect.activities.ui.materials.folders.EditFolderFragment
import com.avidco.studentintellect.databinding.FragmentEditFileBinding
import com.avidco.studentintellect.models.PdfFile
import com.avidco.studentintellect.models.Folder
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.utils.LoadingDialog
import com.avidco.studentintellect.utils.OpenDocumentContract
import com.avidco.studentintellect.utils.Utils.fixDecimalsTo
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.avidco.studentintellect.utils.Utils.toRand
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata

class EditFileFragment : Fragment() {

    private val auth = Firebase.auth
    private val database = Firebase.firestore
    private val storage = Firebase.storage

    companion object {
        const val MY_MODULE_DATA = "arg_my_module_data_file"
        const val FILE_DATA = "arg_file_data_file"
        const val PARENT_FOLDER_DATA = "arg_parent_folder_data_folder"
        const val PATH_DISPLAY_TEXT = "arg_path_display_text"
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
                            (rewardItem.amount.toDouble()/100.0)
                        } else {
                            (rewardItem.amount.toDouble()/200.0)
                        }
                        Toast.makeText(context, "You earned ${amount.toRand()} reward.", Toast.LENGTH_SHORT).show()
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




    @SuppressLint("SetTextI18n")
    private fun setSolutionsFields(uri: Uri){
        var cursor : Cursor? = null
        try {
            cursor = context?.contentResolver?.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()){
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                solutionsSize = (cursor.getDouble(sizeIndex)/1000000).fixDecimalsTo(2).toDouble()

                binding.selectSolutionsSize.text = "$solutionsSize MB"
                binding.selectSolutionsText.text = cursor.getString(nameIndex)
                binding.selectSolutionsImage.setImageResource(R.drawable.ic_pdf)
            }
        } finally {
            cursor?.close()
        }
    }
    private val selectSolutionsResult = registerForActivityResult(OpenDocumentContract()) { uri: Uri? ->
        uri?.let {
            solutionsUri = uri
            setSolutionsFields(uri)
        }
    }
    @SuppressLint("SetTextI18n")
    private fun setMaterialFields(uri: Uri){
        var cursor : Cursor? = null
        try {
            cursor = context?.contentResolver?.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()){
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                materialSize = (cursor.getDouble(sizeIndex)/1000000).fixDecimalsTo(2).toDouble()
                binding.selectMaterialSize.text = "$materialSize MB"
                binding.selectMaterialText.text = cursor.getString(nameIndex)
                binding.selectMaterialImage.setImageResource(R.drawable.ic_pdf)
                if (binding.materialNameInput.editText?.text.isNullOrEmpty()){
                    binding.materialNameInput.editText?.setText(cursor.getString(nameIndex).split('.')[0])
                }
            }
        } finally {
            cursor?.close()
        }
    }
    private val selectMaterialResult = registerForActivityResult(OpenDocumentContract()) { uri: Uri? ->
        uri?.let {
            materialUri = uri
            setMaterialFields(uri)
        }
    }



    private lateinit var binding: FragmentEditFileBinding
    private lateinit var loadingDialog : LoadingDialog
    private lateinit var databaseFiles: FirestoreFiles
    private lateinit var path: String

    private lateinit var myModuleData: ModuleData
    private var parentFolder: Folder? = null
    private var pdfFile: PdfFile? = null
    private var pathDisplayText: String? = null
    private var materialUri : Uri? = null
    private var solutionsUri : Uri? = null
    private var solutionsSize = 0.0
    private var materialSize = 0.0

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(MY_MODULE_DATA, myModuleData)
        outState.putParcelable(PARENT_FOLDER_DATA, parentFolder)
        outState.putParcelable(FILE_DATA, pdfFile)
        outState.putString(PATH_DISPLAY_TEXT, pathDisplayText)
        outState.putParcelable("materialUri", materialUri)
        outState.putParcelable("solutionsUri", solutionsUri)
        outState.putDouble("solutionsSize", solutionsSize)
        outState.putDouble("materialSize", materialSize)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val myArgs = savedInstanceState ?: requireArguments()
        myModuleData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            myArgs.getParcelable(MY_MODULE_DATA, ModuleData::class.java)!!
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
        pdfFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            myArgs.getParcelable(FILE_DATA, PdfFile::class.java)
        } else {
            @Suppress("DEPRECATION")
            myArgs.getParcelable(FILE_DATA)
        }
        pathDisplayText = myArgs.getString(EditFolderFragment.PATH_DISPLAY_TEXT, myModuleData.code)
        if (savedInstanceState != null){
            materialUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelable("materialUri", Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelable("materialUri")
            }
            solutionsUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelable("solutionsUri", Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelable("solutionsUri")
            }
            materialSize = savedInstanceState.getDouble("materialSize")
            solutionsSize = savedInstanceState.getDouble("solutionsSize")
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditFileBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(activity)
        loadAd()

        binding.folderPath.isEnabled = false
        binding.folderPath.editText?.setText(pathDisplayText)
        binding.materialNameInput.editText?.setText(pdfFile?.name)
        if (materialUri != null) setMaterialFields(materialUri!!)
        else if (pdfFile?.materialUrl != null){
            binding.selectMaterialSize.text = "${pdfFile?.materialSize} MB"
            binding.selectMaterialText.text = pdfFile?.materialUrl!!.split("?alt")[0]
            binding.selectMaterialImage.setImageResource(R.drawable.ic_pdf)
        }
        if (solutionsUri != null) setSolutionsFields(solutionsUri!!)
        else if (!pdfFile?.solutionsUrl.isNullOrEmpty()) {
            binding.selectSolutionsSize.text = "${pdfFile?.solutionsSize} MB"
            binding.selectSolutionsText.text = pdfFile?.solutionsUrl!!.split("?alt")[0]
            binding.selectSolutionsImage.setImageResource(R.drawable.ic_pdf)
        }

        path = parentFolder?.path ?: "Modules/${myModuleData.id}"
        val pdfFilesTableName = path.replace(" ", "")
            .replace("Modules", "PdfFiles")
            .replace("/", "_")+"_Table"
        databaseFiles = FirestoreFiles(requireContext(), path, pdfFilesTableName)


        binding.uploadMaterial.setOnClickListener {
            it.tempDisable()
            activity?.hideKeyboard(it)
            uploadPdfFile()
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

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()
        if (pdfFile != null) {
            (requireActivity() as MainActivity).supportActionBar!!.title = "Edit File \"${pdfFile!!.name}\""
            binding.uploadMaterial.text = "Save"
        } else {
            (requireActivity() as MainActivity).supportActionBar!!.title = "Add a PDF File"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun selectFile(isSolutions : Boolean) {
        if (isSolutions) {
            if (pdfFile == null) {
                if (solutionsUri == null) {
                    selectSolutionsResult.launch(arrayOf("application/pdf"))
                } else {
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
                                    binding.selectSolutionsSize.text = ""
                                    solutionsUri = null
                                    solutionsSize = 0.0
                                }
                            }
                        }
                        show()
                    }
                }
            } else {
                if (solutionsUri == null) {
                    if (pdfFile?.solutionsUrl.isNullOrEmpty()){
                        selectSolutionsResult.launch(arrayOf("application/pdf"))
                    } else {
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
                                        binding.selectSolutionsSize.text = ""
                                        solutionsUri = null
                                        solutionsSize = 0.0
                                        pdfFile?.solutionsUrl = null
                                        pdfFile?.solutionsSize = 0.0
                                    }
                                }
                            }
                            show()
                        }
                    }
                }
                else {
                    if (pdfFile?.solutionsUrl.isNullOrEmpty()){
                        val choice = arrayOf<CharSequence>("Change File","Remove File")
                        AlertDialog.Builder(context)
                            .setItems(choice) { _, item ->
                                when {
                                    choice[item] == "Change File" -> {
                                        selectSolutionsResult.launch(arrayOf("application/pdf"))
                                    }
                                    choice[item] == "Remove File" -> {
                                        binding.selectSolutionsImage.setImageResource(R.drawable.ic_pdf_upload)
                                        binding.selectSolutionsText.text = getString(R.string.select_the_study_material_solutions_pdf_file)
                                        binding.selectSolutionsSize.text = ""
                                        solutionsUri = null
                                        solutionsSize = 0.0
                                    }
                                }
                            }
                            .show()
                    } else {
                        val choice = arrayOf<CharSequence>("Change File","Restore Original File","Remove File")
                        AlertDialog.Builder(context)
                            .setItems(choice) { _, item ->
                                when {
                                    choice[item] == "Change File" -> {
                                        selectSolutionsResult.launch(arrayOf("application/pdf"))
                                    }
                                    choice[item] == "Restore Original File" -> {
                                        binding.selectSolutionsSize.text = "${pdfFile?.solutionsSize} MB"
                                        binding.selectSolutionsText.text = pdfFile?.solutionsUrl!!.split("?alt")[0]
                                        binding.selectSolutionsImage.setImageResource(R.drawable.ic_pdf)
                                        solutionsUri = null
                                        solutionsSize = 0.0
                                    }
                                    choice[item] == "Remove File" -> {
                                        binding.selectSolutionsImage.setImageResource(R.drawable.ic_pdf_upload)
                                        binding.selectSolutionsText.text = getString(R.string.select_the_study_material_solutions_pdf_file)
                                        binding.selectSolutionsSize.text = ""
                                        solutionsUri = null
                                        solutionsSize = 0.0
                                        pdfFile?.solutionsUrl = null
                                        pdfFile?.solutionsSize = 0.0
                                    }
                                }
                            }
                            .show()
                    }
                }
            }
        }
        else {
            if (pdfFile == null) {
                if (materialUri == null) {
                    selectMaterialResult.launch(arrayOf("application/pdf"))
                } else {
                    val choice = arrayOf<CharSequence>("Change File","Remove File")
                    AlertDialog.Builder(context).apply {
                        setItems(choice) { _, item ->
                            when {
                                choice[item] == "Change File" -> {
                                    selectMaterialResult.launch(arrayOf("application/pdf"))
                                }
                                choice[item] == "Remove File" -> {
                                    binding.selectMaterialImage.setImageResource(R.drawable.ic_pdf_upload)
                                    binding.selectMaterialText.text = getString(R.string.select_a_study_material_pdf_file)
                                    binding.selectMaterialSize.text = ""
                                    materialUri = null
                                    materialSize = 0.0
                                }
                            }
                        }
                        show()
                    }
                }
            } else {
                if (materialUri == null) {
                    AlertDialog.Builder(context)
                        .setItems(arrayOf("Change File")) { _, _ ->
                            selectMaterialResult.launch(arrayOf("application/pdf"))
                        }
                        .show()
                } else {
                    val choice = arrayOf("Change File","Restore Original File")
                    AlertDialog.Builder(context)
                        .setItems(choice) { _, item ->
                            when {
                                choice[item] == "Change File" -> {
                                    selectMaterialResult.launch(arrayOf("application/pdf"))
                                }
                                choice[item] == "Restore Original File" -> {
                                    binding.selectMaterialSize.text = "${pdfFile?.materialSize} MB"
                                    binding.selectMaterialText.text = pdfFile?.materialUrl!!.split("?alt")[0]
                                    binding.selectMaterialImage.setImageResource(R.drawable.ic_pdf)
                                    materialUri = null
                                    materialSize = 0.0
                                }
                            }
                        }
                        .show()
                }
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
        if (materialUri == null && pdfFile == null){
            binding.selectMaterialInput.showError()
            return false
        }

        if (binding.materialNameAutoComplete.text?.trim().isNullOrEmpty()) {
            binding.materialNameInput.showError()
            return false
        }

        return true
    }

    private fun uploadPdfFile(pdfFilesName : String) {
        val document = Firebase.firestore.collection("$path/PdfFiles").document()
        val storageRef = storage.getReference(path.replace("/Folders", ""))

        val materialRef = storageRef.child("${document.id}.pdf")
        val metadata = storageMetadata {
            setCustomMetadata("Uploader UID", auth.currentUser?.uid)
            setCustomMetadata("Uploader Name", auth.currentUser?.displayName)
            setCustomMetadata("Uploader Email", auth.currentUser?.email)
        }
        val uploadTask = materialRef.putFile(materialUri!!, metadata)
        uploadTask
            .continueWithTask { task->
                if (!task.isSuccessful) {
                    task.exception?.let { exception ->
                        throw exception
                    }
                }
                materialRef.downloadUrl
            }
            .addOnSuccessListener { materialUrl ->
                if (solutionsUri == null) {
                    databaseFiles.put(PdfFile(document.id, pdfFilesName, materialUrl.toString(),materialSize))
                        .addOnSuccessListener {
                            loadingDialog.isDone {
                                (activity as MainActivity?)?.navController?.navigateUp()
                            }
                            /*if (rewardItem != null){
                                            val amount = rewardItem!!.amount/200.0

                                            database.collection("users")
                                                .document(auth.currentUser!!.uid)
                                                .update("balance", FieldValue.increment(amount))

                                            loadingDialog.isDone("Uploaded successfully.\nYou earned ${amount.toRand()} reward.") {  }

                                            val prefs = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
                                            val dataViewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
                                            dataViewModel.setBalance(amount)
                                            rewardItem = null
                                        } else {
                                            loadingDialog.isDone("Uploaded successfully.") {  }
                                        }
                                        loadAd()*/
                        }
                        .addOnFailureListener { error, _->
                            loadingDialog.isError(error.message) { }
                        }
                }
                else {
                    val solutionsRef = storageRef.child("${document.id}_sltns.pdf")
                    val solutionsUploadTask = solutionsRef.putFile(solutionsUri!!, metadata)
                    solutionsUploadTask
                        .continueWithTask { task ->
                            if (!task.isSuccessful) {
                                task.exception?.let { err ->
                                    throw err
                                }
                            }
                            solutionsRef.downloadUrl
                        }
                        .addOnSuccessListener { solutionsUrl ->
                            databaseFiles
                                .put(PdfFile(document.id, pdfFilesName, materialUrl.toString(),
                                    materialSize, solutionsUrl.toString(), solutionsSize))
                                .addOnSuccessListener {
                                    loadingDialog.isDone {
                                        (activity as MainActivity?)?.navController?.navigateUp()
                                    }
                                }
                                .addOnFailureListener { error, _ ->
                                    loadingDialog.isError(error.message) { }
                                }
                        }
                    solutionsUploadTask.addOnProgressListener { uploadTaskSnap->
                        val progress = (100.0 * uploadTaskSnap.bytesTransferred) / uploadTaskSnap.totalByteCount
                        loadingDialog.setText("Uploading Solutions…\n${progress.toInt()}%")
                    }
                }
            }
            .addOnFailureListener { error ->
                loadingDialog.isError(error.message) { }
            }
        uploadTask.addOnProgressListener { uploadTaskSnap->
            val progress = (100.0 * uploadTaskSnap.bytesTransferred) / uploadTaskSnap.totalByteCount
            loadingDialog.setText("Uploading…\n${progress.toInt()}%")
        }
    }
    private fun uploadPdfFile(pdfFilesName : String, pdfFile: PdfFile) {
        if (materialUri == null) {
            loadingDialog.show("Saving...")
            if (solutionsUri == null){
                databaseFiles
                    .put(pdfFile.apply { this.name = pdfFilesName; this.timeUpdated = Timestamp.now() })
                    .addOnSuccessListener {
                        loadingDialog.isDone {
                            (activity as MainActivity?)?.navController?.navigateUp()
                        }
                    }
                    .addOnFailureListener { error, _ ->
                        loadingDialog.isError(error.message) { }
                    }
            }
            else {
                val storageRef = storage.getReference(path.replace("/Folders", ""))
                val metadata = storageMetadata {
                    setCustomMetadata("Uploader UID", auth.currentUser?.uid)
                    setCustomMetadata("Uploader Name", auth.currentUser?.displayName)
                    setCustomMetadata("Uploader Email", auth.currentUser?.email)
                }
                val solutionsRef = storageRef.child("${pdfFile.id}_sltns.pdf")
                val solutionsUploadTask = solutionsRef.putFile(solutionsUri!!, metadata)
                solutionsUploadTask
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { err ->
                                throw err
                            }
                        }
                        solutionsRef.downloadUrl
                    }
                    .addOnSuccessListener { solutionsUrl ->
                        databaseFiles
                            .put(pdfFile.apply {
                                this.name = pdfFilesName; this.timeUpdated = Timestamp.now()
                                this.solutionsUrl = solutionsUrl.toString()
                                this.solutionsSize = this@EditFileFragment.solutionsSize
                            })
                            .addOnSuccessListener {
                                loadingDialog.isDone {
                                    (activity as MainActivity?)?.navController?.navigateUp()
                                }
                            }
                            .addOnFailureListener { error, _ ->
                                loadingDialog.isError(error.message) { }
                            }
                    }
                solutionsUploadTask.addOnProgressListener { uploadTaskSnap->
                    val progress = (100.0 * uploadTaskSnap.bytesTransferred) / uploadTaskSnap.totalByteCount
                    loadingDialog.setText("Uploading Solutions…\n${progress.toInt()}%")
                }
            }
        }
        else {
            loadingDialog.show("Saving...")
            val storageRef = storage.getReference(path.replace("/Folders", ""))
            val materialRef = storageRef.child("${pdfFile.id}.pdf")
            val metadata = storageMetadata {
                setCustomMetadata("Uploader UID", auth.currentUser?.uid)
                setCustomMetadata("Uploader Name", auth.currentUser?.displayName)
                setCustomMetadata("Uploader Email", auth.currentUser?.email)
            }
            val uploadTask = materialRef.putFile(materialUri!!, metadata)
            uploadTask
                .continueWithTask { task->
                    if (!task.isSuccessful) {
                        task.exception?.let { exception ->
                            throw exception
                        }
                    }
                    materialRef.downloadUrl
                }
                .addOnSuccessListener { materialUrl ->
                    if (solutionsUri == null) {
                        databaseFiles
                            .put(pdfFile.apply {
                                this.name = pdfFilesName; this.timeUpdated = Timestamp.now()
                                this.materialUrl = materialUrl.toString()
                                this.materialSize = this@EditFileFragment.materialSize
                            })
                            .addOnSuccessListener {
                                loadingDialog.isDone {
                                    (activity as MainActivity?)?.navController?.navigateUp()
                                }
                            }
                            .addOnFailureListener { error, _->
                                loadingDialog.isError(error.message) { }
                            }
                    }
                    else {
                        val solutionsRef = storageRef.child("${pdfFile.id}_sltns.pdf")
                        val solutionsUploadTask = solutionsRef.putFile(solutionsUri!!, metadata)
                        solutionsUploadTask
                            .continueWithTask { task ->
                                if (!task.isSuccessful) {
                                    task.exception?.let { err ->
                                        throw err
                                    }
                                }
                                solutionsRef.downloadUrl
                            }
                            .addOnSuccessListener { solutionsUrl ->
                                databaseFiles
                                    .put(pdfFile.apply {
                                        this.name = pdfFilesName; this.timeUpdated = Timestamp.now()
                                        this.materialUrl = materialUrl.toString()
                                        this.materialSize = this@EditFileFragment.materialSize
                                        this.solutionsUrl = solutionsUrl.toString()
                                        this.solutionsSize = this@EditFileFragment.solutionsSize
                                    })
                                    .addOnSuccessListener {
                                        loadingDialog.isDone {
                                            (activity as MainActivity?)?.navController?.navigateUp()
                                        }
                                    }
                                    .addOnFailureListener { error, _ ->
                                        loadingDialog.isError(error.message) { }
                                    }
                            }
                        solutionsUploadTask.addOnProgressListener { uploadTaskSnap->
                            val progress = (100.0 * uploadTaskSnap.bytesTransferred) / uploadTaskSnap.totalByteCount
                            loadingDialog.setText("Uploading Solutions…\n${progress.toInt()}%")
                        }
                    }
                }
                .addOnFailureListener { error ->
                    loadingDialog.isError(error.message) { }
                }
            uploadTask.addOnProgressListener { uploadTaskSnap->
                val progress = (100.0 * uploadTaskSnap.bytesTransferred) / uploadTaskSnap.totalByteCount
                loadingDialog.setText("Uploading…\n${progress.toInt()}%")
            }
        }
    }
    private fun uploadPdfFile() {
        if (!validateAllFields()){
            return
        }
        val pdfFilesName = binding.materialNameAutoComplete.text.toString().trim()

        if (pdfFile == null){
            if (materialUri != null) {
                //new upload
                loadingDialog.show("Uploading...")
                database.collection("$path/PdfFiles")
                    .whereEqualTo("name", pdfFilesName)
                    .limit(1)
                    .get()
                    .addOnSuccessListener {
                        if (it.isEmpty) {
                            uploadPdfFile(pdfFilesName)
                        }
                        else {
                            AlertDialog.Builder(context)
                                .setMessage("Pdf File \"$pdfFilesName\" exists. Do you want to upload a file with the same name?")
                                .setCancelable(false)
                                .setPositiveButton("Upload File"){ d,_->
                                    d.dismiss()
                                    uploadPdfFile(pdfFilesName)
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
        else {
            uploadPdfFile(pdfFilesName, pdfFile!!)
        }
    }
}