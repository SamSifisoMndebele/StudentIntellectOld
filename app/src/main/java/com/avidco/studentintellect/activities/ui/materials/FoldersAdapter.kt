package com.avidco.studentintellect.activities.ui.materials

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.pdfview.PdfViewActivity
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.models.FolderData
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

class FoldersAdapter(val activity: MainActivity, private val moduleCode : String,
                     private val path: String, private val foldersIDs : MutableList<String>) :
    RecyclerView.Adapter<FoldersAdapter.MaterialViewHolder>(), Filterable {
    private val pref = activity.getSharedPreferences("view_type", Context.MODE_PRIVATE)
    private var materialsFiltered = foldersIDs.sorted().toMutableList()
    private var interstitialAd : InterstitialAd? = null
    private var refreshPosition : Int? = null
    private var isListView = pref.getBoolean("is_materials_list_view", true)

    private fun gotoMaterialsFragment(folderData : FolderData) {
        val bundle = Bundle().apply {
            putString(MaterialsFragment.MODULE_CODE, moduleCode)
            putString("modulePath", "$path/${folderData.name}")
            putParcelable("folderData", folderData)
        }
        activity.navController.navigate(R.id.materialsFragment, bundle)
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

    private fun showAd(folderData : FolderData){
        interstitialAd!!.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdClicked() {}
            override fun onAdDismissedFullScreenContent() {
                gotoMaterialsFragment(folderData)
                loadAd()
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                gotoMaterialsFragment(folderData)
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
        private val folderTitle: TextView = itemView.findViewById(R.id.folder_title)
        private val menu: ImageView = itemView.findViewById(R.id.menu_button)
        private val folderImage: ImageView = itemView.findViewById(R.id.folder_image)

        private fun showError(errorMessage: String) {
            val errorLayout: LinearLayout = itemView.findViewById(R.id.error_layout)
            itemView.findViewById<TextView>(R.id.error_text).text = errorMessage
            errorLayout.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                errorLayout.visibility = View.GONE
            }, 3000)
        }
        fun bind(materialName: String, position: Int) {
            folderTitle.text = materialName

            val document = Firebase.firestore.document("modules/$moduleCode/materials/$materialName")
            document.get(Source.CACHE)
                .addOnSuccessListener { cacheDoc ->
                    if (cacheDoc.exists()) {
                        val folderData = cacheDoc.toObject(FolderData::class.java)
                        menu.visibility = View.VISIBLE
                        folderData?.let { bind(it, position) }
                    } else {
                        document.get(Source.SERVER)
                            .addOnSuccessListener { serverDoc ->
                                val folderData = serverDoc.toObject(FolderData::class.java)
                                menu.visibility = View.VISIBLE
                                folderData?.let { bind(it, position) }
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
        private fun bind(folderData: FolderData, position: Int){
            itemView.setOnClickListener {
                it.tempDisable(2000)
                activity.hideKeyboard(it)

                if (activity.isOnline() || fileExists(moduleCode, folderData.name, 0)) {
                    refreshPosition = if (fileExists(moduleCode, folderData.name,0)) null else position

                    if (interstitialAd != null) {
                        showAd(folderData)
                    } else {
                        gotoMaterialsFragment(folderData)
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
                dialog.findViewById<TextView>(R.id.material_title).text = folderData.name

                val dateInstance = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                dialog.findViewById<TextView>(R.id.material_upload_date).text =
                    folderData.createdTime?.toDate()?.let { dateInstance.format(it) } ?: "Not available"

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
                    foldersIDs.sorted().toMutableList()
                } else {
                    val resultList = arrayListOf<String>()
                    for(material in foldersIDs){
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