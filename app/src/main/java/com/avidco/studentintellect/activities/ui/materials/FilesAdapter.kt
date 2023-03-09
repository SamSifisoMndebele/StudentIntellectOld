package com.avidco.studentintellect.activities.ui.materials

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.pdfview.PdfViewActivity
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.ProfileViewModel
import com.avidco.studentintellect.activities.ui.materials.database.FilesDatabaseHelper
import com.avidco.studentintellect.activities.ui.materials.edit.EditFileFragment
import com.avidco.studentintellect.models.FileData
import com.avidco.studentintellect.models.FolderData
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.models.UserType
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.text.DateFormat
import java.util.*

class FilesAdapter(val activity: MainActivity, private val parentFolderData : FolderData?, private val moduleData : ModuleData,
                   private val databaseHelper: FilesDatabaseHelper) :
    RecyclerView.Adapter<FilesAdapter.MaterialViewHolder>(), Filterable {
    private val viewTypePref = activity.getSharedPreferences("view_type", Context.MODE_PRIVATE)
    private var filesFiltered = databaseHelper.fileDataList.toMutableList()
    private var interstitialAd : InterstitialAd? = null
    private var refreshPosition : Int? = null
    private var isListView = viewTypePref.getBoolean("is_materials_list_view", true)
    private val materialsPath ="Modules/${moduleData.id}" + (parentFolderData?.path?:"")

    @SuppressLint("NotifyDataSetChanged")
    fun setFilesList(files : MutableList<FileData>){
        filesFiltered = files
        notifyDataSetChanged()
    }

    private fun pdfViewActivity(fileData : FileData) {
        val intent = Intent(activity, PdfViewActivity::class.java).apply {
            putExtra(PdfViewActivity.MODULE_DATA, moduleData)
            putExtra(PdfViewActivity.FILE_DATA, fileData)
        }
        activity.startActivity(intent)
    }

    fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity,activity.getString(R.string.activity_materialsList_interstitialAdUnitId), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                this@FilesAdapter.interstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                this@FilesAdapter.interstitialAd = interstitialAd
            }
        })
    }

    private fun showAd(materialData : FileData){
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
            if (fileExists(materialsPath, materialName,0)){
                materialDownload.visibility = View.GONE
                materialDownloaded.visibility = View.VISIBLE
            } else {
                materialDownloaded.visibility = View.GONE
                materialDownload.visibility = View.VISIBLE
            }
        }
        fun bind(fileData: FileData, position: Int) {
            titleMaterial.text = fileData.name

            checkIfDownloaded(fileData.name)

            itemView.setOnClickListener {
                it.tempDisable(2000)
                activity.hideKeyboard(it)

                if (activity.isOnline() || fileExists(materialsPath, fileData.id, 0)) {
                    refreshPosition = if (fileExists(materialsPath, fileData.id,0)) null else position

                    if (interstitialAd != null) {
                        showAd(fileData)
                    } else {
                        pdfViewActivity(fileData)
                    }
                } else {
                    showError(activity.getString(R.string.no_internet_connection))
                }
            }



            val popupMenu = setUpMenu(fileData)
            menu.visibility = View.VISIBLE
            menu.setOnClickListener { menu ->
                menu.tempDisable()
                popupMenu.show()
            }


            checkDownloads(fileData, false)
            materialDownload.setOnClickListener {
                it.tempDisable()
                progressLayout.visibility = View.VISIBLE

                checkDownloads(fileData,  true)
            }

        }

        @SuppressLint("NotifyDataSetChanged")
        private fun setUpMenu(fileData: FileData) : PopupMenu {
            val popupMenu = PopupMenu(activity, menu)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                popupMenu.gravity = Gravity.END
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                popupMenu.setForceShowIcon(true)
            popupMenu.menuInflater.inflate(R.menu.popup_menu_file, popupMenu.menu)

            activity.profileViewModel.userType.observe(activity) {
                when(it){
                    UserType.ADMIN -> {
                        popupMenu.menu.findItem(R.id.item_delete).isVisible = true
                        popupMenu.menu.findItem(R.id.item_edit).isVisible = true
                        popupMenu.setOnMenuItemClickListener { item ->
                            when(item.itemId) {
                                R.id.item_delete -> {
                                    Firebase.firestore.document("$materialsPath/Deleted/Files")
                                        .set(mapOf("ids" to FieldValue.arrayUnion(fileData.id)), SetOptions.merge())
                                        .addOnSuccessListener {
                                            Firebase.firestore.collection("$materialsPath/Files")
                                                .document(fileData.id)
                                                .delete()
                                                .addOnSuccessListener {
                                                    databaseHelper.deleteFileData(fileData.id)
                                                    filesFiltered = databaseHelper.fileDataList
                                                    notifyDataSetChanged()
                                                }
                                        }
                                }
                                R.id.item_edit -> {
                                    val bundle = Bundle().apply {
                                        putParcelable(EditFileFragment.MODULE_DATA, moduleData)
                                        putParcelable(EditFileFragment.PARENT_FOLDER_DATA, parentFolderData)
                                        putParcelable(EditFileFragment.FILE_DATA, fileData)
                                    }
                                    (activity as MainActivity?)?.navController?.navigate(R.id.editFileFragment, bundle)
                                }
                            }
                            true
                        }
                    }
                    UserType.STUDENT, UserType.TUTOR -> {
                        if (fileData.uploaderUID == Firebase.auth.currentUser?.uid){
                            popupMenu.menu.findItem(R.id.item_delete).isVisible = true
                            popupMenu.menu.findItem(R.id.item_edit).isVisible = true
                            popupMenu.setOnMenuItemClickListener { item ->
                                when(item.itemId) {
                                    R.id.item_delete -> {
                                        Firebase.firestore.document("$materialsPath/Deleted/Files")
                                            .set(mapOf("ids" to FieldValue.arrayUnion(fileData.id)), SetOptions.merge())
                                            .addOnSuccessListener {
                                                Firebase.firestore.collection("$materialsPath/Files")
                                                    .document(fileData.id)
                                                    .delete()
                                                    .addOnSuccessListener {
                                                        databaseHelper.deleteFileData(fileData.id)
                                                        filesFiltered = databaseHelper.fileDataList
                                                        notifyDataSetChanged()
                                                    }
                                            }
                                    }
                                    R.id.item_edit -> {

                                    }
                                }
                                true
                            }
                        } else {
                            popupMenu.menu.findItem(R.id.item_delete).isVisible = false
                            popupMenu.menu.findItem(R.id.item_edit).isVisible = false
                        }
                    }
                }
            }

            if (fileData.solutionsUrl != null) {
                popupMenu.menu.findItem(R.id.item_size).title =
                    "Size ${fileData.materialSize + (fileData.solutionsSize?:0.0)} MB"
                popupMenu.menu.findItem(R.id.item_solutions).title = "With  solutions"
            } else {
                popupMenu.menu.findItem(R.id.item_size).title =
                    "Size ${fileData.materialSize} MB"
                popupMenu.menu.findItem(R.id.item_solutions).title = "No solutions"
            }
            popupMenu.menu.findItem(R.id.item_uploader).title = "Uploaded by ${fileData.uploaderName}"
            popupMenu.menu.findItem(R.id.item_downloads).title = "${fileData.downloads} downloads"
            val dateInstance = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            popupMenu.menu.findItem(R.id.item_updated_date).title = "Updated at " +
                    fileData.updatedTime.toDate().let { dateInstance.format(it) }

            return popupMenu
        }

        private fun calculateProgress(materialProgress : Progress, solutionsTask : Progress) : Int {
            if (materialProgress.totalByteCount == -1L || solutionsTask.totalByteCount == -1L) {
                return 0
            }
            val bytesTransferred = (materialProgress.bytesTransferred + solutionsTask.bytesTransferred)/1000.0
            val totalByteCount = (materialProgress.totalByteCount + solutionsTask.totalByteCount)/1000.0
            return (100.0 * bytesTransferred / totalByteCount).toInt()
        }

        private fun checkDownloads(materialData: FileData, download : Boolean) {
            val materialRef = Firebase.storage.getReference("$materialsPath/Files/${materialData.name}.pdf")
            val solutionsRef = Firebase.storage.getReference("$materialsPath/Files/${materialData.name} Solutions.pdf")

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
                        progressBar.visibility = View.VISIBLE
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
                                    checkIfDownloaded(materialData.name)
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
                                    checkIfDownloaded(materialData.name)
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
                        progressBar.visibility = View.VISIBLE
                        materialTasks[0]
                            .addOnSuccessListener {
                                progressLayout.visibility = View.GONE
                                checkIfDownloaded(materialData.name)
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
                        progressBar.visibility = View.VISIBLE
                        solutionsTasks[0]
                            .addOnSuccessListener {
                                progressLayout.visibility = View.GONE
                                checkIfDownloaded(materialData.name)
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
                        val materialFile = checkFile(materialData.name, false)
                        val solutionsFile = if (materialData.solutionsUrl != null) checkFile(materialData.name, true) else null
                        when {
                            materialFile != null && solutionsFile != null -> {
                                progressLayout.visibility = View.VISIBLE
                                progressBar.visibility = View.VISIBLE
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
                                            checkIfDownloaded(materialData.name)
                                        }
                                        Firebase.firestore
                                            .document("$materialsPath/Files/${materialData.name}")
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
                                            checkIfDownloaded(materialData.name)
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
                                progressBar.visibility = View.VISIBLE
                                materialFile.createNewFile()
                                val materialTask = materialRef.getFile(materialFile)
                                materialTask
                                    .addOnSuccessListener {
                                        progressLayout.visibility = View.GONE
                                        checkIfDownloaded(materialData.name)
                                        Firebase.firestore
                                            .document("$materialsPath/Files/${materialData.name}")
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
                                progressBar.visibility = View.VISIBLE
                                solutionsFile.createNewFile()
                                val solutionsTask = solutionsRef.getFile(solutionsFile)
                                solutionsTask
                                    .addOnSuccessListener {
                                        progressLayout.visibility = View.GONE
                                        checkIfDownloaded(materialData.name)
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
                    File(Utils.documentsDir(materialsPath), "$materialName Solutions.pdf")
                else
                    File(Utils.documentsDir(materialsPath), "$materialName Solutions($i).pdf")
            } else {
                if (i == 0)
                    File(Utils.documentsDir(materialsPath), "$materialName.pdf")
                else
                    File(Utils.documentsDir(materialsPath), "$materialName($i).pdf")
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
        viewTypePref.edit().putBoolean("is_materials_list_view", isListView).apply()
        return isListView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        val itemView: View = if (viewType == 0) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_material, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_material_grid, parent, false)
        }
        return MaterialViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        if (position < filesFiltered.size){
            holder.bind(filesFiltered[position], position)
        }
    }

    override fun getItemCount(): Int {
        return filesFiltered.size
    }

    override fun getFilter(): Filter {
        return object : Filter(){
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val pattern = constraint.toString().lowercase(Locale.getDefault())
                filesFiltered = if (pattern.isEmpty()){
                    databaseHelper.fileDataList.toMutableList()
                } else {
                    val resultList = arrayListOf<FileData>()
                    for(file in databaseHelper.fileDataList){
                        if (file.name.lowercase().contains(pattern)){
                            resultList.add(file)
                        }
                    }
                    resultList.toMutableList()
                }

                return FilterResults().apply { values = filesFiltered.toMutableList() }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filesFiltered = (results?.values as ArrayList<FileData>).toMutableList()
                notifyDataSetChanged()
            }
        }
    }
}