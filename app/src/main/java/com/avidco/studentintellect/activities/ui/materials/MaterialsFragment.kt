package com.avidco.studentintellect.activities.ui.materials

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.res.ColorStateList
import android.os.*
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.database.FilesFirestoreDatabase
import com.avidco.studentintellect.activities.ui.database.FilesFirestoreDatabase.Companion.addOnFailureListener
import com.avidco.studentintellect.activities.ui.database.FilesFirestoreDatabase.Companion.addOnSuccessListener
import com.avidco.studentintellect.activities.ui.database.FoldersFirestoreDatabase
import com.avidco.studentintellect.activities.ui.database.FoldersFirestoreDatabase.Companion.addOnFailureListener
import com.avidco.studentintellect.activities.ui.database.FoldersFirestoreDatabase.Companion.addOnSuccessListener
import com.avidco.studentintellect.activities.ui.materials.Utils.reloadFilesList
import com.avidco.studentintellect.activities.ui.materials.files.EditFileFragment
import com.avidco.studentintellect.activities.ui.materials.folders.EditFolderFragment
import com.avidco.studentintellect.activities.ui.materials.files.FilesAdapter
import com.avidco.studentintellect.activities.ui.materials.folders.FoldersAdapter
import com.avidco.studentintellect.databinding.FragmentMaterialsBinding
import com.avidco.studentintellect.models.Folder
import com.avidco.studentintellect.models.Module
import kotlinx.coroutines.*


class MaterialsFragment : Fragment() {

    private lateinit var binding: FragmentMaterialsBinding

    private lateinit var path: String
    private var databaseFolders: FoldersFirestoreDatabase? = null
    private var databaseFiles: FilesFirestoreDatabase? = null
    private var adapterFolders : FoldersAdapter? = null
    private var adapterFiles : FilesAdapter? = null


    private var isOnline = true
    private var numberOfLoads = 0

    private lateinit var myModuleData: Module
    private var folder: Folder? = null
    private var pathDisplayText: String? = null
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("editMode", editMode)
        outState.putParcelable(MY_MODULE_DATA, myModuleData)
        outState.putParcelable(FOLDER_DATA, folder)
        outState.putString(PATH_DISPLAY_TEXT, pathDisplayText)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val myArgs = savedInstanceState ?: requireArguments()

        editMode = myArgs.getBoolean("editMode", false)
        myModuleData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            myArgs.getParcelable(MY_MODULE_DATA, Module::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            myArgs.getParcelable(MY_MODULE_DATA)!!
        }
        folder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            myArgs.getParcelable(FOLDER_DATA, Folder::class.java)
        } else {
            @Suppress("DEPRECATION")
            myArgs.getParcelable(FOLDER_DATA)
        }
        pathDisplayText = if (folder == null){
            null
        } else{
            myArgs.getString(PATH_DISPLAY_TEXT)
        }
    }

    override fun onStart() {
        super.onStart()
        (activity as MainActivity?)?.supportActionBar?.title = myModuleData.code + " Materials"
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMaterialsBinding.inflate(inflater, container,false)
        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)


        if (pathDisplayText == null){
            binding.pathDisplayView.visibility = View.GONE
        } else{
            binding.pathDisplayView.visibility = View.VISIBLE
            binding.pathDisplayView.text = pathDisplayText+ "/${folder?.name?:"\b"}"
        }

        path = if (folder?.path.isNullOrEmpty()) "Modules/${myModuleData.id}" else folder!!.path

        val foldersTableName = path.replace(" ", "")
            .replace("Modules", "Folders")
            .replace("/", "_")+"_Table"
        val filesTableName = path.replace(" ", "")
            .replace("Modules", "Files")
            .replace("/", "_")+"_Table"


        (activity as MainActivity?)?.apply {
            userDB?.isOnline?.observe(viewLifecycleOwner) { isOnline = it }

            databaseFolders = FoldersFirestoreDatabase(requireContext(), path, foldersTableName)
            databaseFolders!!.get().addOnSuccessListener{_,_->}
            databaseFolders!!.foldersList.observe(this) { foldersList ->
                if (foldersList.isNullOrEmpty()) {
                    binding.foldersList.visibility = View.GONE
                } else {
                    binding.foldersList.visibility = View.VISIBLE
                    adapterFolders = FoldersAdapter(this, myModuleData, folder, databaseFolders!!, this@MaterialsFragment,
                        if(pathDisplayText != null) pathDisplayText+"/${folder!!.name}" else myModuleData.code)
                    binding.foldersList.setHasFixedSize(true)
                    binding.foldersList.adapter = adapterFolders
                }
            }


            databaseFiles = FilesFirestoreDatabase(this, path, filesTableName)
            databaseFiles!!.get().addOnSuccessListener{_,_->}
            databaseFiles!!.pdfFilesList.observe(this) { filesList ->
                if (filesList.isNullOrEmpty()) {
                    binding.filesList.visibility = View.GONE
                } else {
                    binding.filesList.visibility = View.VISIBLE
                    adapterFiles = FilesAdapter(this,folder, myModuleData, databaseFiles!!,
                        if(pathDisplayText != null) pathDisplayText+"/${folder!!.name}" else myModuleData.code)
                    binding.filesList.setHasFixedSize(true)
                    binding.filesList.adapter = adapterFiles
                    adapterFiles!!.loadAd()
                }
            }

            binding.swipeRefresh.setOnRefreshListener {
                if (numberOfLoads<3&&isOnline) {
                    databaseFolders?.get(numberOfLoads<1)
                        ?.addOnFailureListener {
                            binding.swipeRefresh.isRefreshing = false
                            AlertDialog.Builder(this)
                                .setMessage("Folders\n" +
                                        "\nError:\n"+it.message)
                                .show()
                        }
                        ?.addOnSuccessListener { updatedList, deletedList ->
                            binding.swipeRefresh.isRefreshing = false
                            numberOfLoads++

                            AlertDialog.Builder(this)
                                .setMessage(
                                    "Folders\n\nUpdated List\n" + updatedList.joinToString("\n") { it.name }
                                            + "\n\nDeleted List\n" + deletedList.joinToString("\n") { it.name }
                                )
                                .show()

                        }
                    databaseFiles?.get(numberOfLoads<1)
                        ?.addOnFailureListener {
                            binding.swipeRefresh.isRefreshing = false
                            AlertDialog.Builder(this)
                                .setMessage("Files\n" +
                                        "\nError:\n"+it.message)
                                .show()
                        }
                        ?.addOnSuccessListener { updatedList, deletedList ->
                            binding.swipeRefresh.isRefreshing = false
                            numberOfLoads++

                            AlertDialog.Builder(this)
                                .setMessage(
                                    "Files\n\nUpdated List\n" + updatedList.joinToString("\n") { it.name }
                                            + "\n\nDeleted List\n" + deletedList.joinToString("\n") { it.name }
                                )
                                .show()
                        }
                } else
                    binding.swipeRefresh.isRefreshing = false
            }
        }


        binding.addFolder.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable(EditFolderFragment.MY_MODULE_DATA, myModuleData)
                putParcelable(EditFolderFragment.PARENT_FOLDER_DATA, folder)
                putString(EditFolderFragment.PATH_DISPLAY_TEXT, if(pathDisplayText != null) pathDisplayText+"/${folder!!.name}" else myModuleData.code)
            }
            (activity as MainActivity?)?.navController?.navigate(R.id.action_edit_folder_fragment, bundle)
        }
        binding.addFile.setOnClickListener {
            val bundle = Bundle().apply {
                putParcelable(EditFileFragment.ARG_MY_MODULE, myModuleData)
                putParcelable(EditFileFragment.ARG_PARENT_FOLDER, folder)
                putString(EditFileFragment.ARG_PATH_DISPLAY_TEXT, if(pathDisplayText != null) pathDisplayText+"/${folder!!.name}" else myModuleData.code)
            }
            (activity as MainActivity?)?.navController?.navigate(R.id.action_edit_file_fragment, bundle)
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper()).postDelayed({
            adapterFolders?.checkItemChanged()
            //adapterFiles?.checkItemChanged()
        },300)
    }


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
                adapterFolders?.filter?.filter(string)
                adapterFiles?.filter?.filter(string)
                return false
            }
        })

        val viewTypeItem = menu.findItem(R.id.view_type)
        val viewTypePrefs = context?.getSharedPreferences("materials_view_type", MODE_PRIVATE)
        if (viewTypePrefs?.getBoolean("is_list_view", true) == true){
            viewTypeItem?.setIcon(R.drawable.ic_grid)
            viewTypeItem?.setTitle(R.string.grid_view)
            binding.filesList.layoutManager = LinearLayoutManager(context)
            binding.foldersList.layoutManager = LinearLayoutManager(context)
        } else {
            viewTypeItem?.setIcon(R.drawable.ic_list)
            viewTypeItem?.setTitle(R.string.list_view)
            binding.filesList.layoutManager = GridLayoutManager(context, 2)
            binding.foldersList.layoutManager = GridLayoutManager(context, 2)
        }


        val editMaterials = menu.findItem(R.id.edit_materials)
        editMaterialsItem(editMode, editMaterials)
        editMaterials.setOnMenuItemClickListener {
            editMode = !editMode
            editMaterialsItem(editMode, editMaterials)
            true
        }
    }

    private var isListView = true
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.view_type -> {
                val viewTypePrefs = context?.getSharedPreferences("materials_view_type", MODE_PRIVATE)
                val isListView = viewTypePrefs?.getBoolean("is_list_view", true) == true
                val isCommitted = viewTypePrefs?.edit()?.putBoolean("is_list_view", !isListView)?.commit() == true
                if (isCommitted && isListView) {
                    item.setIcon(R.drawable.ic_list)
                    item.setTitle(R.string.list_view)
                    binding.filesList.layoutManager = GridLayoutManager(context, 2)
                    binding.foldersList.layoutManager = GridLayoutManager(context, 2)
                } else {
                    item.setIcon(R.drawable.ic_grid)
                    item.setTitle(R.string.grid_view)
                    binding.filesList.layoutManager = LinearLayoutManager(context)
                    binding.foldersList.layoutManager = LinearLayoutManager(context)
                }
            }
        }
        return super.onOptionsItemSelected(item)
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
        const val MY_MODULE_DATA = "arg_my_module_data"
        const val FOLDER_DATA = "arg_folder_data"
        const val PATH_DISPLAY_TEXT = "arg_path_display_text"
    }
}
