package com.avidco.studentintellect.activities.ui.upload

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.avidco.studentintellect.databinding.FragmentMaterialUploadBinding
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.utils.Constants
import com.avidco.studentintellect.utils.OpenDocumentContract
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.ProfileViewModel
import com.avidco.studentintellect.models.FileData
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import java.io.IOException
import java.lang.StringBuilder


class UploadMultipleFragment : Fragment() {

    companion object {
        private const val SELECT_TITLE = "Select Title"
        private const val SELECT_NUMBER = "Select Number"
        private const val SELECT_YEAR = "Select Year"
    }

    private lateinit var binding: FragmentMaterialUploadBinding

    private val auth = Firebase.auth
    private val database = Firebase.firestore
    private val storage = Firebase.storage

    private var materialUri : Uri? = null
    private var solutionsUri : Uri? = null
    private var materialTitle : String = SELECT_TITLE
    private var materialNumber : String? = SELECT_NUMBER
    private var materialYear : String = SELECT_YEAR

    private var solutionsSize = 0.0
    private var materialSize = 0.0

    private val selectPdfsActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val data: Intent? = result.data
                //If multiple Pdf selected
                if (data?.clipData != null) {
                    binding.selectMaterialImage.setImageResource(R.drawable.ic_pdf)
                    val name = StringBuilder()
                    val count = data.clipData?.itemCount ?: 0
                    for (i in 0 until count) {
                        val pdfUri: Uri = data.clipData!!.getItemAt(i)!!.uri

                        var cursor : Cursor? = null
                        try {
                            cursor = this.context?.contentResolver?.query(pdfUri, null, null, null, null)
                            if (cursor != null && cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                cursor.moveToFirst()
                                val documentName = cursor.getString(nameIndex)
                                name.append("$documentName\n")
                                //multiPdfUri[documentName.split('_', '.')] = pdfUri
                            }
                        } finally {
                            cursor?.close()
                        }
                    }
                    binding.selectSolutionsText.text = name.toString().trim()
                   // binding.materialTitle?.setText(name.toString().trim())
                }
                //If single Pdf selected
                else if (data?.data != null) {
                    val pdfUri: Uri = data.data!!
                    materialUri = pdfUri
                   // binding.toggleMultiple.isChecked = false
                    binding.selectMaterialImage.setImageResource(R.drawable.ic_pdf)

                    /*var cursor : Cursor? = null
                    try {
                        cursor = this.applicationContext.contentResolver.query(pdfUri, null, null, null, null)
                        if (cursor != null && cursor.moveToFirst()){
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            cursor.moveToFirst()
                            val documentName = cursor.getString(nameIndex)
                            binding.selectMaterialText.text = documentName
                            binding.materialTitle.editText?.setText(documentName.split('.')[0])
                        }
                    } finally {
                        cursor?.close()
                    }*/
                }
            }
        }

    private fun selectMultipleFiles(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "application/pdf"
        selectPdfsActivityResult.launch(intent)
    }


    /*private fun addMultipleMaterial(){
        if (multiPdfUri.isEmpty()){
            Toast.makeText(this, "Choose Material Files", Toast.LENGTH_LONG).show()
            return
        }

        binding.errorResponse.visibility = View.VISIBLE
        binding.errorResponse.setTextColor(Color.GRAY)
        binding.materialSubmit.startAnimation()
        for (pdfName in multiPdfUri.keys) {
            val document = database.collection("modules").document(pdfName[0])
            val module = ModuleData(pdfName[0], pdfName[0], Timestamp.now())
            document.set(module, SetOptions.merge())

            val collection = document.collection("materials")

            uploadPdf(collection,pdfName)

            *//*collection.whereEqualTo("id", materialName)
                .limit(1)
                .get()
                .addOnCompleteListener { task ->
                    val isEmpty = task.result.isEmpty
                    if (isEmpty) {
                        uploadPdf(collection,materialName,pdfName)
                    } else {
                        binding.errorResponse.text = "Material, $materialName Exist."
                        if (pdfName == multiPdfUri.keys.last()){
                            val checkMark = BitmapFactory.decodeResource(resources, R.drawable.ic_checked)
                            binding.materialSubmit.doneLoadingAnimation(Color.parseColor("#3BB54A"), checkMark)

                            Handler(Looper.getMainLooper()).postDelayed({
                                binding.errorResponse.visibility = View.GONE
                                multiPdfUri.clear()
                                popupWindow.dismiss()
                                popupWindow = initPopupWindow()
                                binding.selectMultiMaterialImage.setImageResource(R.drawable.ic_pdf_upload)
                                binding.selectMultiMaterialText.text = getString(R.string.select_pdf_file)
                            }, 500)
                        }
                    }
                }*//*
        }

        *//*AlertDialog.Builder(this).apply {
            setMessage(multiPdfUri.toString())
            show()
        }*//*
    }*/
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

    var moduleCodeET : TextInputLayout? = null

    private var rewardedAd: RewardedAd? = null
    private var rewardItem : RewardItem? = null
    private fun loadAd(showAd : Boolean = false) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(requireActivity(),requireActivity().getString(R.string.upload_material_rewardedAdUnitId), adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                println( adError.toString())
                rewardedAd = null
                //binding.watchAd.setCardBackgroundColor(resources.getColor(R.color.grey))
                /*binding.watchAd.setOnClickListener {
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
                //binding.watchAd.setCardBackgroundColor(resources.getColor(R.color.primaryColor))

                if (showAd && validateAllFields()) {
                    rewardedAd?.show(requireActivity()) { rewardItem->
                        this@UploadMultipleFragment.rewardItem = rewardItem
                        val amount = if (solutionsUri != null) {
                            (rewardItem.amount.toDouble()/100f).roundToRand()
                        } else {
                            (rewardItem.amount.toDouble()/200f).roundToRand()
                        }
                        binding.rewardBalance.text = amount
                        Toast.makeText(context, "You earned the R$amount reward.", Toast.LENGTH_SHORT).show()
                        rewardedAd = null
                      //  binding.watchAd.setCardBackgroundColor(resources.getColor(R.color.grey))
                    }
                    //binding.watchAd.setOnClickListener(null)
                } else {
                    /*binding.watchAd.setOnClickListener {
                        it.tempDisable()
                        if (validateAllFields()) {
                            rewardedAd?.show(requireActivity()) { rewardItem->
                                this@UploadMultipleFragment.rewardItem = rewardItem
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

    private fun clear() {
        materialUri = null
        solutionsUri = null
        materialTitle = SELECT_TITLE
        materialNumber = SELECT_NUMBER
        materialYear = SELECT_YEAR
        binding.moduleCode.editText?.text = null
        binding.moduleDescription.visibility = View.GONE
        binding.moduleDescription.editText?.text = null
        binding.selectSolutionsImage.setImageResource(R.drawable.ic_pdf_upload)
        binding.selectMaterialImage.setImageResource(R.drawable.ic_pdf_upload)
        binding.selectSolutionsText.text = getString(R.string.select_the_study_material_solutions_pdf_file)
        binding.selectMaterialText.text = getString(R.string.select_a_study_material_pdf_file)
        binding.materialTitleSpinner.setSelection(0)
        binding.materialNumberSpinner.setSelection(1)
        binding.materialYearSpinner.setSelection(0)
        binding.materialNumberSpinner.isEnabled = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentMaterialUploadBinding.inflate(inflater,container,false)
        moduleCodeET  = binding.moduleCode

        loadAd()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.uploadMaterial.setOnClickListener {
            it.tempDisable()
            activity?.hideKeyboard(it)
            validateAndUploadMaterial()
        }

        /*binding.clearAllButton.setOnClickListener {
            it.tempDisable()
            activity?.hideKeyboard(it)
            clear()
        }*/
        
        binding.selectMaterial.setOnClickListener {
            it.tempDisable()
            activity?.hideKeyboard(it)
            selectFile(false)
        }
        binding.selectSolutions.setOnClickListener { it.tempDisable();
            activity?.hideKeyboard(it)
            selectFile(true) }

        val prefs = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
        val materials = prefs.getStringSet("all_modules_set", null)?.toMutableList()?:mutableListOf()
        val moduleCodeAdapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_dropdown_item, materials)
        binding.moduleCodeAutoComplete.setAdapter(moduleCodeAdapter)

        val titleAdapter = ArrayAdapter.createFromResource(requireContext(),R.array.materials_array,
            android.R.layout.simple_spinner_dropdown_item)
        binding.materialTitleSpinner.adapter = titleAdapter
        var prevPos = -1
        binding.materialTitleSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?,
                    position: Int, id: Long
                ) {
                    materialTitle = parent.getItemAtPosition(position).toString()
                    when (position) {
                        0 -> {
                            binding.materialNumberSpinner.setSelection(1)
                            binding.materialYearSpinner.setSelection(0)
                            binding.materialNumberSpinner.isEnabled = false
                            binding.materialYearSpinner.isEnabled = false
                            binding.materialNumberInput.hint = SELECT_NUMBER
                            binding.materialYearInput.hint = SELECT_YEAR
                            materialNumber = null
                            materialYear = SELECT_YEAR
                        }
                        else -> {
                            when(prevPos) {
                                0,1,2,3 -> {
                                    binding.materialNumberSpinner.setSelection(0)
                                    binding.materialNumberInput.hint = "$materialTitle Number *"
                                    materialNumber = SELECT_NUMBER
                                }
                            }
                            when(position) {
                                1,2,3 -> {
                                    binding.materialNumberSpinner.setSelection(1)
                                    binding.materialNumberInput.hint = "$materialTitle Number"
                                    materialNumber = null
                                }
                            }
                            binding.materialNumberSpinner.isEnabled = when(position){1,2,3 -> false else -> true}
                            binding.materialYearSpinner.isEnabled = true
                            binding.materialYearInput.hint = "$materialTitle Year *"
                        }
                    }
                    prevPos = position
                }
                override fun onNothingSelected(parent : AdapterView<*>) {
                    materialTitle = parent.getItemAtPosition(0).toString()
                    binding.materialNumberSpinner.setSelection(1)
                    binding.materialNumberSpinner.isEnabled = false
                }
            }
        val numberAdapter = ArrayAdapter.createFromResource(requireContext(),R.array.materials_array,
            android.R.layout.simple_spinner_dropdown_item)
        binding.materialNumberSpinner.adapter = numberAdapter
        binding.materialNumberSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?,
                    position: Int, id: Long
                ) {
                    val number = parent.getItemAtPosition(position).toString()
                    materialNumber = if (number == "NA") null else number
                    when (binding.materialTitleSpinner.selectedItemPosition){
                        0,1,2,3 -> {}
                        else -> {
                            if (parent.selectedItemPosition == 1){
                                parent.setSelection(0)
                            }
                        }
                    }
                }
                override fun onNothingSelected(parent : AdapterView<*>) {
                    materialNumber = parent.getItemAtPosition(0).toString()
                }
            }

        val yearAdapter = ArrayAdapter.createFromResource(requireContext(),R.array.years_array,
            android.R.layout.simple_spinner_dropdown_item)
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
            }
    }

    private fun selectFile(isSolutions : Boolean) {
        if (isSolutions) {
            if (solutionsUri != null){
                val choice = arrayOf<CharSequence>("Change File","Remove File")
                AlertDialog.Builder(context).apply {
                    setItems(choice) { _, item ->
                        when {
                            choice[item] == "Change File" -> {
                                //selectSolutionsResult.launch(arrayOf("application/pdf"))
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
                //selectSolutionsResult.launch(arrayOf("application/pdf"))
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
        val moduleCode = binding.moduleCode.editText?.text.toString().trim().uppercase()

        if (moduleCode.isEmpty()) {
            binding.moduleCode.showError()
            return false
        }
        if (!moduleCode.matches(Constants.MODULE_CODE_PATTERN.toRegex())) {
            binding.moduleCode.error = "Invalid module code."
            Handler(Looper.getMainLooper()).postDelayed({
                binding.moduleCode.isErrorEnabled = false
            }, 1500)
            binding.moduleCode.showError()
            return false
        }

        //Check materialUri
        if (materialUri == null) {
            binding.selectMaterialInput.showError()
            return false
        }

        if (materialTitle == SELECT_TITLE) {
            binding.materialTitleInput.showError()
            return false
        }
        if (materialNumber == SELECT_NUMBER) {
            binding.materialNumberInput.showError()
            return false
        }

        if (materialYear == SELECT_YEAR) {
            binding.materialYearInput.showError()
            return false
        }

        if (binding.moduleDescription.isVisible) {
            val moduleDescription = binding.moduleDescription.editText?.text.toString().trim()
            if (moduleDescription.isEmpty()) {
                binding.moduleDescription.showError()
                return false
            }
            if (moduleDescription.length < 10) {
                binding.moduleDescription.error = "Module description is too short."
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.moduleDescription.isErrorEnabled = false
                }, 1500)
                binding.moduleDescription.showError()
                return false
            }
            database.collection("modules").document(moduleCode)
                .set(ModuleData(moduleCode, moduleDescription))
        }

        return true
    }

    private fun validateAndUploadMaterial() {

        if (!validateAllFields()){
            return
        }

        val moduleCode = binding.moduleCode.editText?.text.toString().trim().uppercase()

        if (requireActivity().isOnline()) {
            //Check if the module is available
            var checkOnServer = true
            database.collection("modules")
                .whereEqualTo("code", moduleCode)
                .limit(1)
                .get(Source.CACHE)
                .addOnSuccessListener {
                    if (it.isEmpty) {
                        if (checkOnServer){
                            database.collection("modules")
                                .whereEqualTo("code", moduleCode)
                                .limit(1)
                                .get(Source.SERVER)
                                .addOnSuccessListener { it1 ->
                                    if (it1.isEmpty) {
                                        binding.moduleDescription.visibility = View.VISIBLE
                                        Toast.makeText(requireActivity(),
                                            "Module does not exist in our server,\ntype the module description, or correct the module code.",
                                            Toast.LENGTH_LONG).show()
                                        checkOnServer = false
                                        binding.moduleCode.editText?.doOnTextChanged { _, _, _, _ ->
                                            binding.moduleDescription.visibility = View.GONE
                                            checkOnServer = true
                                            binding.moduleCode.editText?.doOnTextChanged { _,_,_,_-> }
                                        }
                                    }
                                    else {
                                        //binding.progressLayout.showProgress()
                                        uploadMaterial(moduleCode)
                                    }
                                }
                        }
                        else {
                            binding.moduleDescription.visibility = View.VISIBLE
                            Toast.makeText(requireActivity(),
                                "Module does not exist in our server,\ntype the module description, or correct the module code.",
                                Toast.LENGTH_LONG).show()
                            checkOnServer = false
                            binding.moduleCode.editText?.doOnTextChanged { _, _, _, _ ->
                                binding.moduleDescription.visibility = View.GONE
                                checkOnServer = true
                                binding.moduleCode.editText?.doOnTextChanged { _,_,_,_-> }
                            }
                        }
                    }
                    else {
                       // binding.progressLayout.showProgress()
                        uploadMaterial(moduleCode)
                    }
                }
        }
        else {
            Toast.makeText(requireActivity(), "There is no internet connection.\nConnect to internet and try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadMaterial(moduleCode : String) {
        val materialName = when (materialNumber) {
            null -> "$materialTitle $materialYear"
            else -> "$materialTitle $materialNumber $materialYear"
        }

        database.collection("modules/$moduleCode/materials")
            .whereEqualTo("id", materialName)
            .limit(1)
            .get()
            .addOnSuccessListener {
                val isEmpty = it.isEmpty
                if (isEmpty) {
                    val document = database.document("modules/$moduleCode/materials/$materialName")
                    val storageRef = storage.getReference("modules/$moduleCode/materials")
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
                            .addOnSuccessListener {
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
                                        .addOnSuccessListener {
                                            document.set(FileData(document.id,  document.id, ))
                                                .addOnSuccessListener {
                                                    clear()
                                                   // binding.progressLayout.hideProgress()

                                                    if (rewardItem != null) {
                                                        val amount = rewardItem!!.amount.toDouble()/100f
                                                        Toast.makeText(requireActivity(), "Uploaded successfully.\nYou earned R${amount.roundToRand()} reward.", Toast.LENGTH_SHORT).show()

                                                        database.collection("users")
                                                            .document(auth.currentUser!!.uid)
                                                            .update("balance", FieldValue.increment(amount.toDouble()))

                                                        val prefs = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
                                                        val dataViewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
                                                        dataViewModel.setBalance(amount)
                                                        binding.rewardBalance.text = getString(R.string.zero_rand)
                                                        rewardItem = null
                                                    } else {
                                                        Toast.makeText(requireActivity(), "Uploaded successfully.", Toast.LENGTH_SHORT).show()
                                                    }
                                                    loadAd()
                                                }
                                                .addOnFailureListener { err ->
                                                    Toast.makeText(context, err.message, Toast.LENGTH_LONG).show()

                                                   // binding.progressLayout.hideProgress()
                                                    showError(err.message.toString())
                                                }
                                        }
                                        .addOnFailureListener { err ->
                                           // binding.progressLayout.hideProgress()
                                            showError(err.message.toString())
                                        }

                                    solutionsUploadTask.addOnProgressListener { uploadTaskSnap->
                                        val progress = (100.0 * uploadTaskSnap.bytesTransferred) / uploadTaskSnap.totalByteCount
                                       // binding.progressLayout.showProgress("Uploading Solutions…\n${progress.toInt()}%")
                                    }
                                }
                                else {
                                    document.set(FileData(document.id))
                                        .addOnSuccessListener {
                                            clear()
                                           // binding.progressLayout.hideProgress()

                                            if (rewardItem != null){
                                                val amount = rewardItem!!.amount.toDouble()/200f

                                                database.collection("users")
                                                    .document(auth.currentUser!!.uid)
                                                    .update("balance", FieldValue.increment(amount.toDouble()))

                                                Toast.makeText(requireActivity(), "Uploaded successfully.\nYou earned R${amount.roundToRand()} reward.", Toast.LENGTH_SHORT).show()

                                                val prefs = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
                                                val dataViewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
                                                dataViewModel.setBalance(amount)
                                                binding.rewardBalance.text = getString(R.string.zero_rand)
                                                rewardItem = null
                                            } else {
                                                Toast.makeText(requireActivity(), "Uploaded successfully.", Toast.LENGTH_SHORT).show()
                                            }
                                            loadAd()
                                        }
                                        .addOnFailureListener { error->
                                           // binding.progressLayout.hideProgress()
                                            showError(error.message.toString())
                                        }
                                }
                            }
                            .addOnFailureListener { error ->
                               // binding.progressLayout.hideProgress()
                                showError(error.message.toString())
                            }

                        uploadTask.addOnProgressListener { uploadTaskSnap->
                            val progress = (100.0 * uploadTaskSnap.bytesTransferred) / uploadTaskSnap.totalByteCount
                           // binding.progressLayout.showProgress("Uploading Material…\n${progress.toInt()}%")
                        }

                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                    }
                }
                else {
                   // binding.progressLayout.hideProgress()
                    showError("Material Exist!")
                }
            }
            .addOnFailureListener {
               // binding.progressLayout.hideProgress()
                it.message?.let { it1 -> showError(it1) }
            }
    }

    private fun showError(message: String){
        binding.errorResponse.visibility = View.VISIBLE
        binding.errorResponse.text = message
        Handler(Looper.getMainLooper()).postDelayed({
            binding.errorResponse.visibility = View.GONE
        }, 2000)
    }
}








