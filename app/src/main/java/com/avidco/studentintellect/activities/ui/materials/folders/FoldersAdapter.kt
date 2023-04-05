package com.avidco.studentintellect.activities.ui.materials.folders

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.database.FirestoreFolders
import com.avidco.studentintellect.activities.ui.database.FirestoreFolders.Companion.addOnSuccessListener
import com.avidco.studentintellect.activities.ui.materials.MaterialsFragment
import com.avidco.studentintellect.models.Folder
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.models.UserType
import com.avidco.studentintellect.utils.Utils
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.makeFolderIfNotExists
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.File
import java.text.DateFormat
import java.util.*

class FoldersAdapter(val activity: MainActivity, private val moduleData : ModuleData, private val parentFolder : Folder?,
                     private val databaseFolders: FirestoreFolders, private val fragment: MaterialsFragment, private val pathDisplayText : String
) :
    RecyclerView.Adapter<FoldersAdapter.MaterialViewHolder>(), Filterable {

    private var foldersFiltered : List<Folder> = databaseFolders.foldersList.value?: listOf()
    private var refreshPosition : Int? = null
    private val folderPath = parentFolder?.path ?: ""
    private val isListView : Boolean
        get() {
            val viewTypePrefs = activity.getSharedPreferences("materials_view_type", Context.MODE_PRIVATE)
            return viewTypePrefs.getBoolean("is_list_view", true)
        }

    private fun gotoFolderMaterialsFragment(folder : Folder) {
        val bundle = Bundle().apply {
            putParcelable(MaterialsFragment.MY_MODULE_DATA, moduleData)
            putParcelable(MaterialsFragment.FOLDER_DATA, folder)
            putBoolean("editMode", fragment.editMode)
            putString(MaterialsFragment.PATH_DISPLAY_TEXT, pathDisplayText)
        }
        activity.navController.navigate(R.id.action_materials_fragment, bundle)
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
        fun bind(folder: Folder) {
            folderTitle.text = folder.name

            itemView.setOnClickListener {
                it.tempDisable(2000)
                activity.hideKeyboard(it)

                val path = "$folderPath/Folders/${folder.id}"
                makeFolderIfNotExists(moduleData.id+path, folder.id)

                gotoFolderMaterialsFragment(folder)
            }

            val popupMenu = setUpMenu(folder)
            menu.visibility = View.VISIBLE
            menu.setOnClickListener { menu ->
                menu.tempDisable()
                popupMenu.show()
            }

        }


        @SuppressLint("NotifyDataSetChanged")
        private fun setUpMenu(folder: Folder) : PopupMenu {
            val popupMenu = PopupMenu(activity, menu)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                popupMenu.gravity = Gravity.END
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                popupMenu.setForceShowIcon(true)
            popupMenu.menuInflater.inflate(R.menu.popup_menu_folder, popupMenu.menu)

            when(activity.userDB.userType.value){
                UserType.ADMIN -> {
                    popupMenu.menu.findItem(R.id.item_delete).isVisible = true
                    popupMenu.menu.findItem(R.id.item_edit).isVisible = true
                    popupMenu.setOnMenuItemClickListener { item ->
                        when(item.itemId) {
                            R.id.item_delete -> {
                                databaseFolders.delete(folder.id)
                                    .addOnSuccessListener {  }
                            }
                            R.id.item_edit -> {
                                val bundle = Bundle().apply {
                                    putParcelable(EditFolderFragment.MY_MODULE_DATA, moduleData)
                                    putParcelable(EditFolderFragment.FOLDER_DATA, folder)
                                    putBoolean("editMode", fragment.editMode)
                                    putString(EditFolderFragment.PATH_DISPLAY_TEXT, pathDisplayText)
                                }
                                activity.navController.navigate(R.id.action_edit_folder_fragment, bundle)
                            }
                        }
                        true
                    }
                }
                UserType.STUDENT, UserType.TUTOR -> {
                    if (folder.creator.uid == Firebase.auth.currentUser?.uid){
                        popupMenu.menu.findItem(R.id.item_delete).isVisible = true
                        popupMenu.menu.findItem(R.id.item_edit).isVisible = true
                        popupMenu.setOnMenuItemClickListener { item ->
                            when(item.itemId) {
                                R.id.item_delete -> {
                                    databaseFolders.delete(folder.id)
                                        .addOnSuccessListener {  }
                                }
                                R.id.item_edit -> {
                                    val bundle = Bundle().apply {
                                        putParcelable(EditFolderFragment.MY_MODULE_DATA, moduleData)
                                        putParcelable(EditFolderFragment.FOLDER_DATA, folder)
                                        putBoolean("editMode", fragment.editMode)
                                        putString(EditFolderFragment.PATH_DISPLAY_TEXT, pathDisplayText)
                                    }
                                    activity.navController.navigate(R.id.action_edit_folder_fragment, bundle)
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

            popupMenu.menu.findItem(R.id.item_creator).title = "Created by ${folder.creator.name}"
            popupMenu.menu.findItem(R.id.item_files_count).title = "${folder.filesCount} files"
            val dateInstance = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            popupMenu.menu.findItem(R.id.item_updated_date).title = "Updated at " +
                    folder.timeUpdated.toDate().let { dateInstance.format(it) }
            popupMenu.menu.findItem(R.id.item_verified).title = if (folder.isVerified) "Verified" else "Not Verified"

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
            holder.bind(foldersFiltered[position])
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
                    databaseFolders.foldersList.value?: listOf()
                } else {
                    val resultList = arrayListOf<Folder>()
                    for(folder in databaseFolders.foldersList.value?: listOf()){
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
                foldersFiltered = (results?.values as ArrayList<Folder>).toMutableList()
                notifyDataSetChanged()
            }
        }
    }
}