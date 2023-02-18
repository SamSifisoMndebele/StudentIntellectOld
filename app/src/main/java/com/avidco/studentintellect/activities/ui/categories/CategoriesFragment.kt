package com.avidco.studentintellect.activities.ui.categories

import android.content.Context
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
import com.avidco.studentintellect.activities.ui.materials.MaterialsAdapter
import com.avidco.studentintellect.databinding.FragmentFilesBinding
import com.avidco.studentintellect.databinding.FragmentMaterialsBinding
import com.avidco.studentintellect.databinding.FragmentModulesBinding
import com.avidco.studentintellect.models.MaterialData
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class CategoriesFragment : Fragment() {

    private lateinit var binding: FragmentFilesBinding
    private var foldersAdapter : FoldersAdapter? = null
    private var filesAdapter : FilesAdapter? = null
    private var isListView = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilesBinding.inflate(inflater, container,false)
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

        isListView = requireActivity().getSharedPreferences("view_type", Context.MODE_PRIVATE).getBoolean("is_materials_list_view", true)

        val moduleCode = arguments?.getString(MODULE_CODE)!!
        (requireActivity() as MainActivity).supportActionBar?.title = moduleCode

        val prefs = requireActivity().getSharedPreferences("pref", MODE_PRIVATE)
        val materialsSet = prefs.getStringSet("${moduleCode}_materials_set", null)
        val currentTime = Timestamp.now()

        if (!materialsSet.isNullOrEmpty()) {
            val lastReadTime = Timestamp(prefs.getLong("materials_last_read_time", 0),0)
            Firebase.firestore
                .collection("modules/$moduleCode/materials")
                .whereGreaterThanOrEqualTo("dateAdded", lastReadTime)
                .get()
                .addOnSuccessListener {
                    val materials = mutableSetOf<String>()
                    materials.addAll(materialsSet)
                    it.documents.forEach { snapshot ->
                        val data = snapshot.toObject(MaterialData::class.java)!!
                        materials.add(data.id)
                    }
                    prefs.edit().putStringSet("${moduleCode}_materials_set", materials).apply()
                    prefs.edit().putLong("materials_last_read_time",currentTime.seconds).apply()

                    foldersAdapter = FoldersAdapter(requireActivity() as MainActivity, moduleCode, materials.toMutableList())
                    itemViewType(isListView, null, binding.foldersList)
                    binding.foldersList.setHasFixedSize(true)
                    binding.foldersList.adapter = foldersAdapter
                    foldersAdapter!!.loadAd()

                    filesAdapter = FilesAdapter(requireActivity() as MainActivity, moduleCode, materials.toMutableList())
                    itemViewType(isListView, null, binding.filesList)
                    binding.filesList.setHasFixedSize(true)
                    binding.filesList.adapter = filesAdapter
                    filesAdapter!!.loadAd()
                    // binding.progressBar.visibility = View.GONE
                }
        } else {
            Firebase.firestore
                .collection("modules/$moduleCode/materials")
                .get()
                .addOnSuccessListener {
                    val materials = mutableSetOf<String>()
                    it.documents.forEach { snapshot ->
                        val data = snapshot.toObject(MaterialData::class.java)!!
                        materials.add(data.id)
                    }
                    prefs.edit().putStringSet("${moduleCode}_materials_set", materials).apply()
                    prefs.edit().putLong("materials_last_read_time",currentTime.seconds).apply()


                    foldersAdapter = FoldersAdapter(requireActivity() as MainActivity, moduleCode, materials.toMutableList())
                    itemViewType(isListView, null, binding.foldersList)
                    binding.foldersList.setHasFixedSize(true)
                    binding.foldersList.adapter = foldersAdapter
                    foldersAdapter!!.loadAd()

                    filesAdapter = FilesAdapter(requireActivity() as MainActivity, moduleCode, materials.toMutableList())
                    itemViewType(isListView, null, binding.filesList)
                    binding.filesList.setHasFixedSize(true)
                    binding.filesList.adapter = filesAdapter
                    filesAdapter!!.loadAd()
                    // binding.progressBar.visibility = View.GONE
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
