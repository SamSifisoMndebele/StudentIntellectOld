package com.avidco.studentintellect.activities.ui.add.modules

import android.Manifest
import android.app.Dialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.transition.Slide
import android.transition.TransitionManager
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.ProfileViewModel
import com.avidco.studentintellect.activities.ui.materials.FoldersAdapter
import com.avidco.studentintellect.activities.ui.materials.database.DatabasesConnect
import com.avidco.studentintellect.activities.ui.materials.database.FoldersDatabaseHelper
import com.avidco.studentintellect.activities.ui.materials.database.ModulesDatabaseHelper
import com.avidco.studentintellect.databinding.FragmentAddModulesBinding
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.isOnline
import com.avidco.studentintellect.utils.Utils.shareApp
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AddModulesFragment : Fragment() {

    private lateinit var binding: FragmentAddModulesBinding
    private var addAdapter : AddModulesAdapter? = null

    private val database = Firebase.firestore
    
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentAddModulesBinding.inflate(inflater, container, false)

        val databaseHelperModules = ModulesDatabaseHelper(activity)

        addAdapter = AddModulesAdapter(databaseHelperModules, (activity as MainActivity?)?.profileViewModel?.modulesList?.value)
        binding.addModuleList.layoutManager = LinearLayoutManager(context)
        binding.addModuleList.setHasFixedSize(true)
        binding.addModuleList.adapter = addAdapter

        GlobalScope.launch {
            val modulesDataListDeferred = async { (activity as MainActivity?)?.let {
                DatabasesConnect.getModulesDataList(it, it.isOnline())
            } }
            val modulesDataList = modulesDataListDeferred.await()
            (activity as MainActivity?)?.runOnUiThread {
                modulesDataList?.let { addAdapter?.setModulesList(it) }
            }
        }

        binding.saveButton.setOnClickListener {
            if (requireActivity().isOnline()){
                if (addAdapter != null) {
                    // (it as CircularProgressButton).startAnimation()
                    val modulesList = addAdapter!!.list()
                    val profileViewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
                    profileViewModel.modulesList
                    /*Firebase.firestore.collection("Users")
                        .document(Firebase.auth.currentUser!!.uid)
                        .collection("MyModules")
                        .document()
                        .set()
                        .addOnCompleteListener { task ->
                            if (task.exception != null) {
                                Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
                            }
                            ViewModelProvider(this)[ProfileViewModel::class.java]
                                .setModules(prefs, modulesList.toMutableSet())
                            activity?.hideKeyboard(binding.root)
                            (requireActivity() as MainActivity).navController.navigateUp()
                        }*/
                } else {
                    it.tempDisable()
                    Toast.makeText(activity, "Please try again.", Toast.LENGTH_SHORT).show()
                }
            } else {
                it.tempDisable()
                Toast.makeText(activity, "Check your internet connection and try again.", Toast.LENGTH_SHORT).show()
            }

        }

        binding.searchView.queryHint = "Search Module..."
        binding.searchView.onActionViewExpanded()
        binding.searchView.clearFocus()
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(string: String?): Boolean {
                binding!!.searchView.clearFocus()
                return false
            }
            override fun onQueryTextChange(string: String): Boolean {
                addAdapter?.filter?.filter(string)
                return false
            }
        })

        return binding.root
    }
}