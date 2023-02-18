package com.avidco.studentintellect.activities.ui.categories

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.pdfview.PdfViewActivity
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.models.MaterialData
import com.avidco.studentintellect.utils.Utils
import com.avidco.studentintellect.utils.Utils.fileExists
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.isOnline
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.daimajia.numberprogressbar.NumberProgressBar
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.text.DateFormat
import java.util.*

class FoldersAdapter(val activity: MainActivity, private val moduleCode : String, private val materialIDs : MutableList<String>) :
    RecyclerView.Adapter<FoldersAdapter.MaterialViewHolder>(), Filterable {
    private val pref = activity.getSharedPreferences("view_type", Context.MODE_PRIVATE)
    private var materialsFiltered = materialIDs.sorted().toMutableList()
    private var interstitialAd : InterstitialAd? = null
    private var refreshPosition : Int? = null
    private var isListView = pref.getBoolean("is_materials_list_view", true)

    private fun pdfViewActivity(materialData : MaterialData) {
        try {
            val intent = Intent(activity, PdfViewActivity::class.java).apply {
                putExtra(PdfViewActivity.MODULE_CODE, moduleCode)
                putExtra(PdfViewActivity.MATERIAL_DATA, materialData)
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity,activity.getString(R.string.activity_materialsList_interstitialAdUnitId), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                this@FoldersAdapter.interstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                this@FoldersAdapter.interstitialAd = interstitialAd
            }
        })
    }

    private fun showAd(materialData : MaterialData){
        interstitialAd!!.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdClicked() {}
            override fun onAdDismissedFullScreenContent() {
                pdfViewActivity(materialData)
                loadAd()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                pdfViewActivity(materialData)
                loadAd()
            }
            override fun onAdImpression() {}
            override fun onAdShowedFullScreenContent() {}
        }
        interstitialAd!!.show(activity)
    }

    fun checkItemChanged() {
        refreshPosition?.let { notifyItemChanged(it) }
    }

    data class Progress(val bytesTransferred : Long = 0, val totalByteCount: Long = 0)
    inner class MaterialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleMaterial: TextView = itemView.findViewById(R.id.material_title)
        private val menu: ImageView = itemView.findViewById(R.id.menu_button)
        private val materialDownload: ImageView = itemView.findViewById(R.id.material_download)
        private val materialDownloaded: ImageView = itemView.findViewById(R.id.material_downloaded)
        private val progressLayout: LinearLayout = itemView.findViewById(R.id.progress_layout)
        private val progressText: TextView = itemView.findViewById(R.id.progress_text)
        private val progressBar: NumberProgressBar = itemView.findViewById(R.id.progress_bar)


        private fun showError(errorMessage: String) {
            val errorLayout: LinearLayout = itemView.findViewById(R.id.error_layout)
            itemView.findViewById<TextView>(R.id.error_text).text = errorMessage
            errorLayout.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                errorLayout.visibility = View.GONE
            }, 3000)
        }
        private fun checkIfDownloaded(materialName: String){
            if (fileExists(moduleCode, materialName,0)){
                materialDownload.visibility = View.GONE
                materialDownloaded.visibility = View.VISIBLE
            } else {
                materialDownloaded.visibility = View.GONE
                materialDownload.visibility = View.VISIBLE
            }
        }
        fun bind(materialName: String, position: Int) {
            titleMaterial.text = materialName

            checkIfDownloaded(materialName)

            val document = Firebase.firestore.document("modules/$moduleCode/materials/$materialName")
            document.get(Source.CACHE)
                .addOnSuccessListener { cacheDoc ->
                    if (cacheDoc.exists()) {
                        val materialData = cacheDoc.toObject(MaterialData::class.java)
                        menu.visibility = View.VISIBLE
                        materialData?.let { bind(it, position) }
                    } else {
                        document.get(Source.SERVER)
                            .addOnSuccessListener { serverDoc ->
                                val materialData = serverDoc.toObject(MaterialData::class.java)
                                menu.visibility = View.VISIBLE
                                materialData?.let { bind(it, position) }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(activity, e.message, Toast.LENGTH_LONG).show()
                                activity.navController.navigateUp()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(activity, e.message, Toast.LENGTH_LONG).show()
                    activity.navController.navigateUp()
                }
        }

        @SuppressLint("SetTextI18n")
        private fun bind(materialData: MaterialData, position: Int){
            itemView.setOnClickListener {
                it.tempDisable(2000)
                activity.hideKeyboard(it)

                if (activity.isOnline() || fileExists(moduleCode, materialData.id, 0)) {
                    refreshPosition = if (fileExists(moduleCode, materialData.id,0)) null else position

                    if (interstitialAd != null) {
                        showAd(materialData)
                    } else {
                        pdfViewActivity(materialData)
                    }
                } else {
                    showError(activity.getString(R.string.no_internet_connection))
                }
            }

            menu.setOnClickListener { menu ->
                menu.tempDisable()
                val dialog = Dialog(itemView.context)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(R.layout.menu_layout_material)

                dialog.findViewById<TextView>(R.id.module_code).text = moduleCode
                dialog.findViewById<TextView>(R.id.material_title).text = materialData.id
                dialog.findViewById<TextView>(R.id.material_size).text = "${materialData.size?.materialSize} MB".takeIf { it != "0.0 MB" }
                dialog.findViewById<TextView>(R.id.material_uploader_name).text = materialData.uploader?.name ?: "Anonymous"
                dialog.findViewById<TextView>(R.id.material_downloads_number).text = materialData.downloads.toString()
                if (materialData.withSolutions) {
                    dialog.findViewById<LinearLayout>(R.id.material_solutions).visibility = View.VISIBLE
                    dialog.findViewById<TextView>(R.id.solutions_size).text = " | ${materialData.size?.solutionsSize} MB".takeIf { it != " | 0.0 MB" }
                }

                val dateInstance = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                dialog.findViewById<TextView>(R.id.material_upload_date).text =
                    materialData.uploadedTime?.toDate()?.let { dateInstance.format(it) } ?: "Not available"

                /*val delete = dialog.findViewById<LinearLayout>(R.id.delete)
                delete.setOnClickListener {
                    it.tempDisable()
                    dialog.dismiss()
                }*/

                dialog.window!!.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.window!!.setGravity(Gravity.CENTER)
                dialog.show()
            }

            checkDownloads(materialData, false)
            materialDownload.setOnClickListener {
                it.tempDisable()
                progressLayout.visibility = View.VISIBLE

                checkDownloads(materialData,  true)
            }
        }


        private fun calculateProgress(materialProgress : Progress, solutionsTask : Progress) : Int {
            if (materialProgress.totalByteCount == -1L || solutionsTask.totalByteCount == -1L) {
                return 0
            }
            val bytesTransferred = (materialProgress.bytesTransferred + solutionsTask.bytesTransferred)/1000.0
            val totalByteCount = (materialProgress.totalByteCount + solutionsTask.totalByteCount)/1000.0
            return (100.0 * bytesTransferred / totalByteCount).toInt()
        }

        private fun checkDownloads(materialData: MaterialData, download : Boolean) {
            val materialRef = Firebase.storage.getReference("modules/$moduleCode/materials/${materialData.id}.pdf")
            val solutionsRef = Firebase.storage.getReference("modules/$moduleCode/materials/${materialData.id} Solutions.pdf")

            if (!activity.isOnline()) {
                if (download)
                    showError(itemView.context.getString(R.string.no_internet_connection))
                progressLayout.visibility = View.GONE
            }
            else {
                val materialTasks = materialRef.activeDownloadTasks
                val solutionsTasks = solutionsRef.activeDownloadTasks
                when {
                    materialTasks.isNotEmpty() && solutionsTasks.isNotEmpty() -> {
                        progressLayout.visibility = View.VISIBLE
                        progressText.text = activity.getString(R.string.material_with_solutions_downloading)
                        var materialProgress = Progress()
                        var solutionsProgress = Progress()

                        var materialTaskDone = false
                        var solutionsTaskDone = false

                        materialTasks[0]
                            .addOnSuccessListener {
                                materialTaskDone = true
                                if (solutionsTaskDone){
                                    progressLayout.visibility = View.GONE
                                    checkIfDownloaded(materialData.id)
                                }
                            }
                            .addOnFailureListener {
                                progressLayout.visibility = View.GONE
                            }
                            .addOnProgressListener {
                                materialProgress = Progress(it.bytesTransferred, it.totalByteCount)
                                progressBar.progress = calculateProgress(materialProgress, solutionsProgress)
                            }
                        solutionsTasks[0]
                            .addOnSuccessListener {
                                solutionsTaskDone = true
                                if (materialTaskDone){
                                    progressLayout.visibility = View.GONE
                                    checkIfDownloaded(materialData.id)
                                }
                            }
                            .addOnFailureListener {
                                progressLayout.visibility = View.GONE
                            }
                            .addOnProgressListener {
                                solutionsProgress = Progress(it.bytesTransferred, it.totalByteCount)
                                progressBar.progress = calculateProgress(materialProgress, solutionsProgress)
                            }
                    }
                    materialTasks.isNotEmpty() -> {
                        progressText.text = activity.getString(R.string.material_downloading)
                        progressLayout.visibility = View.VISIBLE
                        materialTasks[0]
                            .addOnSuccessListener {
                                progressLayout.visibility = View.GONE
                                checkIfDownloaded(materialData.id)
                            }
                            .addOnFailureListener {
                                progressLayout.visibility = View.GONE
                            }
                            .addOnProgressListener {
                                val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount)
                                progressBar.progress = progress.toInt()
                            }
                    }
                    solutionsTasks.isNotEmpty() -> {
                        progressText.text = activity.getString(R.string.solutions_downloading)
                        progressLayout.visibility = View.VISIBLE
                        solutionsTasks[0]
                            .addOnSuccessListener {
                                progressLayout.visibility = View.GONE
                                checkIfDownloaded(materialData.id)
                            }
                            .addOnFailureListener {
                                progressLayout.visibility = View.GONE
                            }
                            .addOnProgressListener {
                                val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount)
                                progressBar.progress = progress.toInt()
                            }
                    }
                    download -> {
                        val materialFile = checkFile(materialData.id, false)
                        val solutionsFile = if (materialData.withSolutions) checkFile(materialData.id, true) else null
                        when {
                            materialFile != null && solutionsFile != null -> {
                                progressLayout.visibility = View.VISIBLE
                                progressText.text = activity.getString(R.string.material_with_solutions_downloading)
                                var materialProgress = Progress()
                                var solutionsProgress = Progress()

                                var materialTaskDone = false
                                var solutionsTaskDone = false

                                materialFile.createNewFile()
                                val materialTask = materialRef.getFile(materialFile)
                                materialTask
                                    .addOnSuccessListener {
                                        materialTaskDone = true
                                        if (solutionsTaskDone){
                                            progressLayout.visibility = View.GONE
                                            checkIfDownloaded(materialData.id)
                                        }
                                        Firebase.firestore
                                            .document("modules/$moduleCode/materials/${materialData.id}")
                                            .update("downloads", FieldValue.increment(1))
                                    }
                                    .addOnFailureListener {
                                        it.message?.let { it1 -> showError(it1) }
                                        if (materialFile.length() == 0L)
                                            materialFile.delete()
                                        materialTask.cancel()
                                        progressLayout.visibility = View.GONE
                                    }
                                    .addOnProgressListener {
                                        materialProgress = Progress(it.bytesTransferred, it.totalByteCount)
                                        progressBar.progress = calculateProgress(materialProgress, solutionsProgress)
                                    }

                                solutionsFile.createNewFile()
                                val solutionsTask = solutionsRef.getFile(solutionsFile)
                                solutionsTask
                                    .addOnSuccessListener {
                                        solutionsTaskDone = true
                                        if (materialTaskDone){
                                            progressLayout.visibility = View.GONE
                                            checkIfDownloaded(materialData.id)
                                        }
                                    }
                                    .addOnFailureListener {
                                        it.message?.let { it1 -> showError(it1) }
                                        if (solutionsFile.length() == 0L)
                                            solutionsFile.delete()
                                        solutionsTask.cancel()
                                        progressLayout.visibility = View.GONE
                                    }
                                    .addOnProgressListener {
                                        solutionsProgress = Progress(it.bytesTransferred, it.totalByteCount)
                                        progressBar.progress = calculateProgress(materialProgress, solutionsProgress)
                                    }
                            }
                            materialFile != null -> {
                                progressText.text = activity.getString(R.string.material_downloading)
                                progressLayout.visibility = View.VISIBLE
                                materialFile.createNewFile()
                                val materialTask = materialRef.getFile(materialFile)
                                materialTask
                                    .addOnSuccessListener {
                                        progressLayout.visibility = View.GONE
                                        checkIfDownloaded(materialData.id)
                                        Firebase.firestore
                                            .document("modules/$moduleCode/materials/${materialData.id}")
                                            .update("downloads", FieldValue.increment(1))
                                    }
                                    .addOnFailureListener {
                                        it.message?.let { it1 -> showError(it1) }
                                        if (materialFile.length() == 0L)
                                            materialFile.delete()
                                        materialTask.cancel()
                                        progressLayout.visibility = View.GONE
                                    }
                                    .addOnProgressListener {
                                        val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount)
                                        progressBar.progress = progress.toInt()
                                    }
                            }
                            solutionsFile != null -> {
                                progressText.text = activity.getString(R.string.solutions_downloading)
                                progressLayout.visibility = View.VISIBLE
                                solutionsFile.createNewFile()
                                val solutionsTask = solutionsRef.getFile(solutionsFile)
                                solutionsTask
                                    .addOnSuccessListener {
                                        progressLayout.visibility = View.GONE
                                        checkIfDownloaded(materialData.id)
                                    }
                                    .addOnFailureListener {
                                        // Handle any errors
                                        it.message?.let { it1 -> showError(it1) }
                                        if (solutionsFile.length() == 0L)
                                            solutionsFile.delete()
                                        solutionsTask.cancel()
                                        progressLayout.visibility = View.GONE
                                    }
                                    .addOnProgressListener {
                                        val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount)
                                        progressBar.progress = progress.toInt()
                                    }
                            }
                        }
                    }
                }
            }

        }

        private fun checkFile(materialName: String, isSolutions : Boolean, i : Int = 0) : File? {
            val file = if (isSolutions) {
                if (i == 0)
                    File(Utils.documentsDir(moduleCode), "$materialName Solutions.pdf")
                else
                    File(Utils.documentsDir(moduleCode), "$materialName Solutions($i).pdf")
            } else {
                if (i == 0)
                    File(Utils.documentsDir(moduleCode), "$materialName.pdf")
                else
                    File(Utils.documentsDir(moduleCode), "$materialName($i).pdf")
            }

            return if (file.exists() && file.length() != 0L) {
                if (file.canRead()) {
                    null
                } else {
                    checkFile(materialName,isSolutions, i+1)
                }
            } else {
                try { file.deleteOnExit() } catch (_: Exception) { }
                file
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isListView) 0 else 1
    }

    fun toggleItemViewType() : Boolean {
        isListView = !isListView
        pref.edit().putBoolean("is_materials_list_view", isListView).apply()
        return isListView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        val itemView: View = if (viewType == 0) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_folder_grid, parent, false)
        }
        return MaterialViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        if (position < materialsFiltered.size){
            holder.bind(materialsFiltered[position], position)
        }
    }

    override fun getItemCount(): Int {
        return materialsFiltered.size
    }

    override fun getFilter(): Filter {
        return object : Filter(){
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val pattern = constraint.toString().lowercase(Locale.getDefault())
                materialsFiltered = if (pattern.isEmpty()){
                    materialIDs.sorted().toMutableList()
                } else {
                    val resultList = arrayListOf<String>()
                    for(material in materialIDs){
                        if (material.lowercase().contains(pattern)){
                            resultList.add(material)
                        }
                    }
                    resultList.sorted().toMutableList()
                }

                return FilterResults().apply { values = materialsFiltered.sorted().toMutableList() }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                materialsFiltered = (results?.values as ArrayList<String>).sorted().toMutableList()
                notifyDataSetChanged()
            }
        }
    }
}