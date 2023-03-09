package com.avidco.studentintellect.activities.ui.materials

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.res.ColorStateList
import android.os.*
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.materials.database.DatabasesConnect.getFilesDataList
import com.avidco.studentintellect.activities.ui.materials.database.DatabasesConnect.getFoldersDataList
import com.avidco.studentintellect.activities.ui.materials.database.FilesDatabaseHelper
import com.avidco.studentintellect.activities.ui.materials.database.FoldersDatabaseHelper
import com.avidco.studentintellect.activities.ui.materials.edit.EditFileFragment
import com.avidco.studentintellect.activities.ui.materials.edit.EditFolderFragment
import com.avidco.studentintellect.databinding.FragmentMaterialsBinding
import com.avidco.studentintellect.models.FileData
import com.avidco.studentintellect.models.FolderData
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.utils.LoadingDialog
import com.avidco.studentintellect.utils.Utils.dpToPx
import com.avidco.studentintellect.utils.Utils.isOnline
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.collection.LLRBNode
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*


class MaterialsFragment : Fragment() {

    private lateinit var binding: FragmentMaterialsBinding
    private var foldersAdapter : FoldersAdapter? = null
    private var filesAdapter : FilesAdapter? = null
    private var isListView = false

    private lateinit var moduleData: ModuleData
    private var folderData: FolderData? = null
    private lateinit var folderPath: String
    private lateinit var path: String
    private var isOnline = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as MainActivity?)?.let {
            it.profileViewModel.checkIsOnline(it)
        }

        isListView = requireActivity().getSharedPreferences("view_type", MODE_PRIVATE)
            .getBoolean("is_materials_list_view", false)
        editMode = requireArguments().getBoolean("editMode", false)

        moduleData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(MODULE_DATA, ModuleData::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(MODULE_DATA)!!
        }
        folderData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(FOLDER_DATA, FolderData::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(FOLDER_DATA)
        }

        val modulePath = "Modules/${moduleData.code}"
        folderPath = folderData?.path ?: "" //if (folderData != null) "/Folders/${folderData.name}" else ""
        path = modulePath + folderPath
    }

    private var numberOfLoads = 0

    private var reloadOnUp = false

    @SuppressLint("NotifyDataSetChanged")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMaterialsBinding.inflate(inflater, container,false)
        (activity as MainActivity?)?.supportActionBar?.title = moduleData.code + " Materials"

        folderPath.replace("/Folders", "").removePrefix("/").ifEmpty {
            binding.pathDirectoryText.visibility = View.GONE
            null
        }?.let {
            binding.pathDirectoryText.visibility = View.VISIBLE
            binding.pathDirectoryText.text = it
        }


        (activity as MainActivity?)?.profileViewModel?.isOnline?.observe(viewLifecycleOwner) {
            isOnline = it
        }

        val nameFolders = path.replace(" ", "")
            .replace("Modules", "Folders")
            .replace("/", "_")
        val databaseHelperFolders = FoldersDatabaseHelper(activity, "${nameFolders}_Table")
        val nameFiles = path.replace(" ", "")
            .replace("Modules", "Files")
            .replace("/", "_")
        val databaseHelperFiles = FilesDatabaseHelper(activity, "${nameFiles}_Table")

        foldersAdapter = (activity as MainActivity?)?.let {
            FoldersAdapter(it,folderData, moduleData, databaseHelperFolders, this@MaterialsFragment)
        }
        itemViewTypeItem(isListView, null, binding.foldersList)
        binding.foldersList.setHasFixedSize(true)
        binding.foldersList.adapter = foldersAdapter

        filesAdapter = (activity as MainActivity?)?.let {
            FilesAdapter(it,folderData, moduleData, databaseHelperFiles)
        }
        itemViewTypeItem(isListView, null, binding.filesList)
        binding.filesList.setHasFixedSize(true)
        binding.filesList.adapter = filesAdapter
        filesAdapter!!.loadAd()


        if (reloadOnUp){
            GlobalScope.launch {
                val folderDataListDeferred = async { (activity as MainActivity?)?.let { getFoldersDataList(it, path, isOnline) } }
                val folderDataList = folderDataListDeferred.await()
                (activity as MainActivity?)?.runOnUiThread {
                    folderDataList?.let { foldersAdapter?.setFoldersList(it) }
                }

                val fileDataListDeferred = async { (activity as MainActivity?)?.let { getFilesDataList(it, path, isOnline) } }
                val fileDataList = fileDataListDeferred.await()
                (activity as MainActivity?)?.runOnUiThread {
                    fileDataList?.let { filesAdapter?.setFilesList(it) }
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
        //binding.swipeRefresh.setSlingshotDistance(64.dpToPx().toInt())
        binding.swipeRefresh.setOnRefreshListener {
            GlobalScope.launch {
                val folderDataListDeferred = async { (activity as MainActivity?)?.let { getFoldersDataList(it, path, numberOfLoads<3&&isOnline) } }
                val folderDataList = folderDataListDeferred.await()
                (activity as MainActivity?)?.runOnUiThread {
                    folderDataList?.let { foldersAdapter?.setFoldersList(it) }
                }

                val fileDataListDeferred = async { (activity as MainActivity?)?.let { getFilesDataList(it, path, numberOfLoads<3&&isOnline) } }
                val fileDataList = fileDataListDeferred.await()
                (activity as MainActivity?)?.runOnUiThread {
                    fileDataList?.let { filesAdapter?.setFilesList(it) }
                    binding.swipeRefresh.isRefreshing = false
                }

                numberOfLoads++
            }
        }

        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)

        binding.addFolder.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable(EditFolderFragment.MODULE_DATA, moduleData)
                putParcelable(EditFolderFragment.PARENT_FOLDER_DATA, folderData)
            }
            (activity as MainActivity?)?.navController?.navigate(R.id.editFolderFragment, bundle)
        }
        binding.addFile.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable(EditFileFragment.MODULE_DATA, moduleData)
                putParcelable(EditFileFragment.PARENT_FOLDER_DATA, folderData)
            }
            (activity as MainActivity?)?.navController?.navigate(R.id.editFileFragment, bundle)
            //(activity as MainActivity?)?.navController?.navigate(R.id.action_materialsFragment_to_editFileFragment, bundle)
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper()).postDelayed({
            foldersAdapter?.checkItemChanged()
            filesAdapter?.checkItemChanged()
        },300)
    }

    private var clickable = true
    var editMode = false
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_materials, menu)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            menu.findItem(R.id.search_bar).iconTintList = ColorStateList.valueOf(0x009688)
        }
        val searchView = menu.findItem(R.id.search_bar)?.actionView as SearchView
        searchView.animate()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                searchView.clearFocus()
                return false
            }
            override fun onQueryTextChange(string: String?): Boolean {
                foldersAdapter?.filter?.filter(string)
                filesAdapter?.filter?.filter(string)
                return false
            }
        })
        val viewType = menu.findItem(R.id.view_type)
        itemViewTypeItem(isListView, viewType, null)
        viewType.setOnMenuItemClickListener {
            if (clickable){
                itemViewTypeItem(foldersAdapter?.toggleItemViewType() == true, it, binding.foldersList)
                itemViewTypeItem(filesAdapter?.toggleItemViewType() == true, it, binding.filesList)
                clickable = false
                Handler(Looper.getMainLooper()).postDelayed({
                    clickable = true
                }, 500)
            }
            true
        }

        val editMaterials = menu.findItem(R.id.edit_materials)
        editMaterialsItem(editMode, editMaterials)
        editMaterials.setOnMenuItemClickListener {
            editMode = !editMode
            editMaterialsItem(editMode, editMaterials)
            true
        }

    }

    private fun itemViewTypeItem(isListView : Boolean, item: MenuItem?, recyclerView: RecyclerView?) {
        if (isListView){
            item?.setIcon(R.drawable.ic_grid)
            item?.setTitle(R.string.grid_view)
            recyclerView?.layoutManager = LinearLayoutManager(context)
        } else {
            item?.setIcon(R.drawable.ic_list)
            item?.setTitle(R.string.list_view)
            recyclerView?.layoutManager = GridLayoutManager(context, 2)
        }
    }
    private fun editMaterialsItem(editMode : Boolean, item: MenuItem) {
        if (editMode){
            item.setIcon(R.drawable.ic_edit_off)
            item.setTitle(R.string.edit_off)
            binding.addFolder.visibility = View.VISIBLE
            binding.addFile.visibility = View.VISIBLE
        } else {
            item.setIcon(R.drawable.ic_edit)
            item.setTitle(R.string.edit_on)
            binding.addFolder.visibility = View.GONE
            binding.addFile.visibility = View.GONE
        }
    }

    companion object {
        const val MODULE_DATA = "arg_module_data"
        const val FOLDER_DATA = "arg_folder_data"
    }
}
