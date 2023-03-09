package com.avidco.studentintellect.activities.ui.materials

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.materials.database.FilesDatabaseHelper
import com.avidco.studentintellect.activities.ui.materials.database.FoldersDatabaseHelper
import com.avidco.studentintellect.models.FileData
import com.avidco.studentintellect.models.FolderData
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.models.UserType
import com.avidco.studentintellect.utils.Utils
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.isOnline
import com.avidco.studentintellect.utils.Utils.makeFolderIfNotExists
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.io.File
import java.text.DateFormat
import java.util.*

class FoldersAdapter(val activity: MainActivity, private val parentFolderData : FolderData?, private val moduleData : ModuleData,
                     private val databaseHelper: FoldersDatabaseHelper, private val fragment: MaterialsFragment) :
    RecyclerView.Adapter<FoldersAdapter.MaterialViewHolder>(), Filterable {
    private val pref = activity.getSharedPreferences("view_type", Context.MODE_PRIVATE)
    private var isListView = pref.getBoolean("is_materials_list_view", true)
    private var foldersFiltered : MutableList<FolderData> = databaseHelper.folderDataList
    private var refreshPosition : Int? = null
    private val folderPath = parentFolderData?.path ?: ""

    @SuppressLint("NotifyDataSetChanged")
    fun setFoldersList(folders : MutableList<FolderData>){
        foldersFiltered = folders
        notifyDataSetChanged()
    }

    private fun gotoFolderMaterialsFragment(folderData : FolderData) {
        val bundle = Bundle().apply {
            putParcelable(MaterialsFragment.MODULE_DATA, moduleData)
            putParcelable(MaterialsFragment.FOLDER_DATA, folderData)
            putBoolean("editMode", fragment.editMode)
        }
        activity.navController.navigate(R.id.materialsFragment, bundle)
    }

    fun checkItemChanged() {
        refreshPosition?.let { notifyItemChanged(it) }
    }

    data class Progress(val bytesTransferred : Long = 0, val totalByteCount: Long = 0)
    inner class MaterialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderTitle: TextView = itemView.findViewById(R.id.folder_title)
        private val menu: ImageView = itemView.findViewById(R.id.menu_button)
        private val folderImage: ImageView = itemView.findViewById(R.id.folder_image)

        @SuppressLint("SetTextI18n")
        fun bind(folderData: FolderData, position: Int) {
            folderTitle.text = folderData.name

            itemView.setOnClickListener {
                it.tempDisable(2000)
                activity.hideKeyboard(it)

                val path = "$folderPath/Folders/${folderData.id}"
                makeFolderIfNotExists(moduleData.code+path, folderData.id)

                gotoFolderMaterialsFragment(folderData.apply {
                    this.path = path
                })
            }

            val popupMenu = setUpMenu(folderData)
            menu.visibility = View.VISIBLE
            menu.setOnClickListener { menu ->
                menu.tempDisable()
                popupMenu.show()
            }

        }


        @SuppressLint("NotifyDataSetChanged")
        private fun setUpMenu(folderData: FolderData) : PopupMenu {
            val popupMenu = PopupMenu(activity, menu)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                popupMenu.gravity = Gravity.END
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                popupMenu.setForceShowIcon(true)
            popupMenu.menuInflater.inflate(R.menu.popup_menu_folder, popupMenu.menu)

            activity.profileViewModel.userType.observe(activity) {
                when(it){
                    UserType.ADMIN -> {
                        popupMenu.menu.findItem(R.id.item_delete).isVisible = true
                        popupMenu.menu.findItem(R.id.item_edit).isVisible = true
                        popupMenu.setOnMenuItemClickListener { item ->
                            when(item.itemId) {
                                R.id.item_delete -> {
                                    Firebase.firestore.document("$folderPath/Deleted/Folders")
                                        .set(mapOf("ids" to FieldValue.arrayUnion(folderData.id)), SetOptions.merge())
                                        .addOnSuccessListener {
                                            Firebase.firestore.collection("$folderPath/Folders")
                                                .document(folderData.id)
                                                .delete()
                                                .addOnSuccessListener {
                                                    databaseHelper.deleteFolderData(folderData.id)
                                                    foldersFiltered = databaseHelper.folderDataList
                                                    notifyDataSetChanged()
                                                }
                                        }
                                }
                                R.id.item_edit -> {

                                }
                            }
                            true
                        }
                    }
                    UserType.STUDENT, UserType.TUTOR -> {
                        if (folderData.creatorUID == Firebase.auth.currentUser?.uid){
                            popupMenu.menu.findItem(R.id.item_delete).isVisible = true
                            popupMenu.menu.findItem(R.id.item_edit).isVisible = true
                            popupMenu.setOnMenuItemClickListener { item ->
                                when(item.itemId) {
                                    R.id.item_delete -> {
                                        Firebase.firestore.document("$folderPath/Deleted/Folders")
                                            .set(mapOf("ids" to FieldValue.arrayUnion(folderData.id)), SetOptions.merge())
                                            .addOnSuccessListener {
                                                Firebase.firestore.collection("$folderPath/Folders")
                                                    .document(folderData.id)
                                                    .delete()
                                                    .addOnSuccessListener {
                                                        databaseHelper.deleteFolderData(folderData.id)
                                                        foldersFiltered = databaseHelper.folderDataList
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

            popupMenu.menu.findItem(R.id.item_creator).title = "Created by ${folderData.creatorName}"
            popupMenu.menu.findItem(R.id.item_files_count).title = "${folderData.filesCount} files"
            val dateInstance = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            popupMenu.menu.findItem(R.id.item_updated_date).title = "Updated at " +
                    folderData.updatedTime.toDate().let { dateInstance.format(it) }

            return popupMenu
        }

        private fun checkFile(materialName: String, isSolutions : Boolean, i : Int = 0) : File? {
            val file = if (isSolutions) {
                if (i == 0)
                    File(Utils.documentsDir(folderPath), "$materialName Solutions.pdf")
                else
                    File(Utils.documentsDir(folderPath), "$materialName Solutions($i).pdf")
            } else {
                if (i == 0)
                    File(Utils.documentsDir(folderPath), "$materialName.pdf")
                else
                    File(Utils.documentsDir(folderPath), "$materialName($i).pdf")
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
        if (position < foldersFiltered.size){
            holder.bind(foldersFiltered[position], position)
        }
    }

    override fun getItemCount(): Int {
        return foldersFiltered.size
    }

    override fun getFilter(): Filter {
        return object : Filter(){
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val pattern = constraint.toString().lowercase(Locale.getDefault())
                foldersFiltered = if (pattern.isEmpty()){
                    databaseHelper.folderDataList.toMutableList()
                } else {
                    val resultList = arrayListOf<FolderData>()
                    for(folder in databaseHelper.folderDataList){
                        if (folder.name.lowercase().contains(pattern)){
                            resultList.add(folder)
                        }
                    }
                    resultList.toMutableList()
                }

                return FilterResults().apply { values = foldersFiltered.toMutableList() }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                foldersFiltered = (results?.values as ArrayList<FolderData>).toMutableList()
                notifyDataSetChanged()
            }
        }
    }
}