package com.avidco.studentintellect.activities.ui.modules.add

import android.content.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.modules.MyModulesAdapter
import com.avidco.studentintellect.activities.ui.modules.MyModulesDatabaseHelper
import com.avidco.studentintellect.databinding.FragmentAddModulesBinding
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.utils.LoadingDialog
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class ModulesFragment : Fragment() {

    private lateinit var binding: FragmentAddModulesBinding
    private lateinit var loadingDialog: LoadingDialog
    private lateinit var databaseHelperMyModules: MyModulesDatabaseHelper
    private var addAdapter : ModulesAdapter? = null

    private fun MainActivity.reloadModules(databaseHelper : ModulesDatabaseHelper, exitOnCancel: Boolean = false) {
        val prefs = getSharedPreferences("last_modules_read_time_prefs", Context.MODE_PRIVATE)
        val currentTime = Timestamp.now()
        val lastReadTime = Timestamp(prefs.getLong("last_read_time", 0),0)
        val lastMapReadTime = prefs.getLong("last_deleted_map_read_time", 0)

        Firebase.firestore.collection("Modules")
            .whereGreaterThanOrEqualTo("timeAdded", lastReadTime)
            .get()
            .addOnCompleteListener { addedTask ->
                Firebase.firestore.collection("Modules")
                    .whereGreaterThanOrEqualTo("timeUpdated", lastReadTime)
                    .get()
                    .addOnCompleteListener { modifiedTask ->
                        Firebase.firestore.collection("Deleted")
                            .document("Modules")
                            .get()
                            .addOnCompleteListener { deletedTask ->
                                val deletedIDs = (deletedTask.result?.get("deletedModules") as Map<*, *>?)
                                    ?.filter { (it.value as Timestamp).seconds > lastMapReadTime }

                                databaseHelper.listUpdateFromFireStore(
                                    addedTask.result?.toObjects(ModuleData::class.java),
                                    modifiedTask.result?.toObjects(ModuleData::class.java),
                                    deletedIDs?.keys?.toList()
                                )
                                prefs.edit().putLong("last_read_time",currentTime.seconds).apply()
                                if (deletedIDs?.values?.isNotEmpty() == true)
                                    prefs.edit().putLong("last_deleted_map_read_time",
                                        deletedIDs.values.map { it as Timestamp }.maxOf { it.seconds }).apply()

                                loadingDialog.dismiss()
                                binding.swipeRefresh.isRefreshing = false
                            }
                    }
            }

        loadingDialog.onCancel {
            navController.navigateUp()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentAddModulesBinding.inflate(inflater, container, false)
        loadingDialog = LoadingDialog(activity)
        databaseHelperMyModules = MyModulesDatabaseHelper(activity as MainActivity?)

        loadingDialog.show()
        (activity as MainActivity?)?.apply {
            val databaseHelper = ModulesDatabaseHelper(this)
            databaseHelper.modulesList.observe(this) { modulesList ->
                if (!modulesList.isNullOrEmpty()) {
                    addAdapter = ModulesAdapter(this, modulesList, databaseHelperMyModules)
                    binding.addModuleList.layoutManager = LinearLayoutManager(this)
                    binding.addModuleList.setHasFixedSize(true)
                    binding.addModuleList.adapter = addAdapter
                    loadingDialog.dismiss()
                } else {
                    reloadModules(databaseHelper, true)
                }
            }

            binding.swipeRefresh.setOnRefreshListener {
                reloadModules(databaseHelper)
            }
        }


        binding.doneButton.setOnClickListener {
            it.tempDisable()
            activity?.hideKeyboard(binding.root)
            (requireActivity() as MainActivity).navController.navigateUp()
            databaseHelperMyModules.saveModulesToFirebase { task ->
                if (task.exception != null)
                    Toast.makeText(context, task.exception!!.message, Toast.LENGTH_SHORT).show()
            }
        }

        binding.searchView.queryHint = "Search Module..."
        binding.searchView.onActionViewExpanded()
        binding.searchView.clearFocus()
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(string: String?): Boolean {
                binding.searchView.clearFocus()
                return false
            }
            override fun onQueryTextChange(string: String): Boolean {
                addAdapter?.filter?.filter(string)
                return false
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        databaseHelperMyModules.saveModulesToFirebase { task ->
            if (task.exception != null)
                Toast.makeText(context, task.exception!!.message, Toast.LENGTH_SHORT).show()
        }
        super.onDestroyView()
    }
}