package com.avidco.studentintellect.activities.ui.materials

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.*
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.auth.AuthActivity
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.databinding.FragmentMaterialsBinding
import com.avidco.studentintellect.models.FileData
import com.avidco.studentintellect.models.FolderData
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MaterialsFragment : Fragment() {

    private lateinit var binding: FragmentMaterialsBinding
    private var foldersAdapter : FoldersAdapter? = null
    private var filesAdapter : FilesAdapter? = null
    private var isListView = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMaterialsBinding.inflate(inflater, container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signOut()
            startActivity(Intent(requireActivity(), AuthActivity::class.java))
            activity?.finishAffinity()
            return
        }

        isListView = requireActivity().getSharedPreferences("view_type", MODE_PRIVATE)
            .getBoolean("is_materials_list_view", true)

        val moduleCode = arguments?.getString(MODULE_CODE)!!
        (requireActivity() as MainActivity).supportActionBar?.title = moduleCode

        val prefs = requireActivity().getSharedPreferences("pref", MODE_PRIVATE)
        val foldersIDs = prefs.getStringSet("${moduleCode}_foldersIDs", null)
        val filesIDs = prefs.getStringSet("${moduleCode}_filesIDs", null)
        val currentTime = Timestamp.now()

        val modulePath = arguments?.getString("modulePath") ?: "Materials/$moduleCode"

        when {
            !foldersIDs.isNullOrEmpty() && !filesIDs.isNullOrEmpty() -> {
                val foldersIDsLastReadTime = Timestamp(prefs.getLong("foldersIDs_last_read_time", 0),0)
                Firebase.firestore
                    .collection("$modulePath/Folders")
                    .whereGreaterThanOrEqualTo("dateAdded", foldersIDsLastReadTime)
                    .get()
                    .addOnSuccessListener {
                        val foldersIDsSet = mutableSetOf<String>()
                        foldersIDsSet.addAll(foldersIDs)
                        it.documents.forEach { snapshot ->
                            val data = snapshot.toObject(FolderData::class.java)!!
                            foldersIDsSet.add(data.name)
                        }
                        prefs.edit().putStringSet("${moduleCode}_foldersIDs", foldersIDsSet)
                            .putLong("foldersIDs_last_read_time",currentTime.seconds).apply()

                        foldersAdapter = FoldersAdapter(requireActivity() as MainActivity, moduleCode,
                            "$modulePath/Folders", foldersIDsSet.toMutableList())
                        itemViewType(isListView, null, binding.foldersList)
                        binding.foldersList.setHasFixedSize(true)
                        binding.foldersList.adapter = foldersAdapter
                        foldersAdapter!!.loadAd()
                        // binding.progressBar.visibility = View.GONE
                    }
                val filesIDsLastReadTime = Timestamp(prefs.getLong("filesIDs_last_read_time", 0),0)
                Firebase.firestore
                    .collection("$modulePath/Files")
                    .whereGreaterThanOrEqualTo("dateAdded", filesIDsLastReadTime)
                    .get()
                    .addOnSuccessListener {
                        val filesIDsSet = mutableSetOf<String>()
                        filesIDsSet.addAll(filesIDs)
                        it.documents.forEach { snapshot ->
                            val data = snapshot.toObject(FileData::class.java)!!
                            filesIDsSet.add(data.name)
                        }
                        prefs.edit().putStringSet("${moduleCode}_filesIDs", filesIDsSet)
                            .putLong("filesIDs_last_read_time",currentTime.seconds).apply()

                        filesAdapter = FilesAdapter(requireActivity() as MainActivity, moduleCode, filesIDsSet.toMutableList())
                        itemViewType(isListView, null, binding.filesList)
                        binding.filesList.setHasFixedSize(true)
                        binding.filesList.adapter = filesAdapter
                        filesAdapter!!.loadAd()
                        // binding.progressBar.visibility = View.GONE
                    }
            }
            !foldersIDs.isNullOrEmpty() -> {
                val foldersIDsLastReadTime = Timestamp(prefs.getLong("foldersIDs_last_read_time", 0),0)
                Firebase.firestore
                    .collection("$modulePath/Folders" )
                    .whereGreaterThanOrEqualTo("dateAdded", foldersIDsLastReadTime)
                    .get()
                    .addOnSuccessListener {
                        val foldersIDsSet = mutableSetOf<String>()
                        foldersIDsSet.addAll(foldersIDs)
                        it.documents.forEach { snapshot ->
                            val data = snapshot.toObject(FolderData::class.java)!!
                            foldersIDsSet.add(data.name)
                        }
                        prefs.edit().putStringSet("${moduleCode}_foldersIDs", foldersIDsSet)
                            .putLong("foldersIDs_last_read_time",currentTime.seconds).apply()

                        foldersAdapter = FoldersAdapter(requireActivity() as MainActivity, moduleCode,
                            "$modulePath/Folders", foldersIDsSet.toMutableList())
                        itemViewType(isListView, null, binding.foldersList)
                        binding.foldersList.setHasFixedSize(true)
                        binding.foldersList.adapter = foldersAdapter
                        foldersAdapter!!.loadAd()
                        // binding.progressBar.visibility = View.GONE
                    }
                Firebase.firestore
                    .collection("$modulePath/Files")
                    .get()
                    .addOnSuccessListener {
                        val filesIDsSet = mutableSetOf<String>()
                        it.documents.forEach { snapshot ->
                            val data = snapshot.toObject(FileData::class.java)!!
                            filesIDsSet.add(data.name)
                        }
                        prefs.edit().putStringSet("${moduleCode}_filesIDs", filesIDsSet)
                            .putLong("filesIDs_last_read_time",currentTime.seconds).apply()

                        filesAdapter = FilesAdapter(requireActivity() as MainActivity, moduleCode, filesIDsSet.toMutableList())
                        itemViewType(isListView, null, binding.filesList)
                        binding.filesList.setHasFixedSize(true)
                        binding.filesList.adapter = filesAdapter
                        filesAdapter!!.loadAd()
                        // binding.progressBar.visibility = View.GONE
                    }
            }
            !filesIDs.isNullOrEmpty() -> {
                Firebase.firestore
                    .collection("$modulePath/Folders")
                    .get()
                    .addOnSuccessListener {
                        val foldersIDsSet = mutableSetOf<String>()
                        it.documents.forEach { snapshot ->
                            val data = snapshot.toObject(FolderData::class.java)!!
                            foldersIDsSet.add(data.name)
                        }
                        prefs.edit().putStringSet("${moduleCode}_foldersIDs", foldersIDsSet)
                            .putLong("foldersIDs_last_read_time",currentTime.seconds).apply()

                        foldersAdapter = FoldersAdapter(requireActivity() as MainActivity, moduleCode,
                            "$modulePath/Folders", foldersIDsSet.toMutableList())
                        itemViewType(isListView, null, binding.foldersList)
                        binding.foldersList.setHasFixedSize(true)
                        binding.foldersList.adapter = foldersAdapter
                        foldersAdapter!!.loadAd()

                        // binding.progressBar.visibility = View.GONE
                    }
                val filesIDsLastReadTime = Timestamp(prefs.getLong("filesIDs_last_read_time", 0),0)
                Firebase.firestore
                    .collection("$modulePath/Files")
                    .whereGreaterThanOrEqualTo("dateAdded", filesIDsLastReadTime)
                    .get()
                    .addOnSuccessListener {
                        val filesIDsSet = mutableSetOf<String>()
                        filesIDsSet.addAll(filesIDs)
                        it.documents.forEach { snapshot ->
                            val data = snapshot.toObject(FileData::class.java)!!
                            filesIDsSet.add(data.name)
                        }
                        prefs.edit().putStringSet("${moduleCode}_filesIDs", filesIDsSet)
                            .putLong("filesIDs_last_read_time",currentTime.seconds).apply()

                        filesAdapter = FilesAdapter(requireActivity() as MainActivity, moduleCode, filesIDsSet.toMutableList())
                        itemViewType(isListView, null, binding.filesList)
                        binding.filesList.setHasFixedSize(true)
                        binding.filesList.adapter = filesAdapter
                        filesAdapter!!.loadAd()
                        // binding.progressBar.visibility = View.GONE
                    }
            }
            else -> {
                Firebase.firestore
                    .collection("$modulePath/Folders")
                    .get()
                    .addOnSuccessListener {
                        val foldersIDsSet = mutableSetOf<String>()
                        it.documents.forEach { snapshot ->
                            val data = snapshot.toObject(FolderData::class.java)!!
                            foldersIDsSet.add(data.name)
                        }
                        prefs.edit().putStringSet("${moduleCode}_foldersIDs", foldersIDsSet)
                            .putLong("foldersIDs_last_read_time",currentTime.seconds).apply()

                        foldersAdapter = FoldersAdapter(requireActivity() as MainActivity, moduleCode,
                            "$modulePath/Folders", foldersIDsSet.toMutableList())
                        itemViewType(isListView, null, binding.foldersList)
                        binding.foldersList.setHasFixedSize(true)
                        binding.foldersList.adapter = foldersAdapter
                        foldersAdapter!!.loadAd()

                        // binding.progressBar.visibility = View.GONE
                    }
                Firebase.firestore
                    .collection("$modulePath/Files")
                    .get()
                    .addOnSuccessListener {
                        val filesIDsSet = mutableSetOf<String>()
                        it.documents.forEach { snapshot ->
                            val data = snapshot.toObject(FileData::class.java)!!
                            filesIDsSet.add(data.name)
                        }
                        prefs.edit().putStringSet("${moduleCode}_filesIDs", filesIDsSet)
                            .putLong("filesIDs_last_read_time",currentTime.seconds).apply()

                        filesAdapter = FilesAdapter(requireActivity() as MainActivity, moduleCode, filesIDsSet.toMutableList())
                        itemViewType(isListView, null, binding.filesList)
                        binding.filesList.setHasFixedSize(true)
                        binding.filesList.adapter = filesAdapter
                        filesAdapter!!.loadAd()
                        // binding.progressBar.visibility = View.GONE
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper()).postDelayed({
            foldersAdapter?.checkItemChanged()
            filesAdapter?.checkItemChanged()
        },300)
    }

    private var clickabele = true
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.material_menu, menu)
        val searchView = menu?.findItem(R.id.search_bar)?.actionView as SearchView
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
        itemViewType(isListView, viewType, null)
        viewType.setOnMenuItemClickListener {
            if (clickabele){
                //itemViewType(adapter?.toggleItemViewType() == true, it, binding.materialList)
                clickabele = false
                Handler(Looper.getMainLooper()).postDelayed({
                    clickabele = true
                }, 500)
            }
            true
        }
    }

    private fun itemViewType(isListView : Boolean, item: MenuItem?, recyclerView: RecyclerView?) {
        if (isListView){
            item?.setIcon(R.drawable.ic_grid)
            recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        } else {
            item?.setIcon(R.drawable.ic_list)
            recyclerView?.layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    companion object {
        const val MODULE_CODE = "arg_module_code_1"
    }
}
