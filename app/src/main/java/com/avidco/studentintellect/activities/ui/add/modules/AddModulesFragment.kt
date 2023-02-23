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

class AddModulesFragment : Fragment() {

    private lateinit var binding: FragmentAddModulesBinding
    private var addAdapter : AddModulesAdapter? = null

    private val database = Firebase.firestore
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        binding = FragmentAddModulesBinding.inflate(inflater, container, false)
        val prefs = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
        //binding!!.progressBar.showProgressDialog(binding!!.avi)

        val userModulesSet = prefs?.getStringSet("user_modules", null)
        val allModulesSet = prefs.getStringSet("all_modules_set", null)
        addAdapter = AddModulesAdapter(allModulesSet?.toMutableList()?:mutableListOf(), userModulesSet?.toMutableList())
        binding.addModuleList.adapter = addAdapter
        /*val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        homeViewModel.text.observe(viewLifecycleOwner) {

        }*/


        val currentTime = Timestamp.now()
        if (allModulesSet.isNullOrEmpty()) {
            FirebaseFirestore
                .getInstance()
                .collection("Modules")
                .get()
                .addOnSuccessListener {
                    val modules = mutableSetOf<String>()
                    it.documents.forEach { snapshot ->
                        val data = snapshot.toObject(ModuleData::class.java)!!
                        modules.add(data.code)
                    }
                    prefs.edit().putStringSet("all_modules_set", modules).apply()
                    prefs.edit().putLong("last_read_all_modules_time",currentTime.seconds).apply()

                    addAdapter = AddModulesAdapter(modules.toMutableList(), userModulesSet?.toMutableList())
                    binding.addModuleList.layoutManager = LinearLayoutManager(context)
                    binding.addModuleList.setHasFixedSize(true)
                    binding.addModuleList.adapter = addAdapter
                    //binding!!.progressBar.hideProgressDialog(binding!!.avi)
                }
                .addOnFailureListener {
                    //binding!!.progressBar.hideProgressDialog(binding!!.avi)
                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                }
        }
        else {
            val lastReadTime = Timestamp(prefs.getLong("last_read_all_modules_time", 0),0)
            FirebaseFirestore
                .getInstance()
                .collection("Modules")
                .whereGreaterThanOrEqualTo("dateAdded", lastReadTime)
                .get()
                .addOnSuccessListener {
                    val modules = mutableSetOf<String>()
                    modules.addAll(allModulesSet)
                    it.documents.forEach { snapshot ->
                        val data = snapshot.toObject(ModuleData::class.java)!!
                        modules.add(data.code)
                    }
                    prefs.edit().putStringSet("all_modules_set", modules).apply()
                    prefs.edit().putLong("last_read_all_modules_time",currentTime.seconds).apply()
                    addAdapter = AddModulesAdapter(modules.toMutableList(), userModulesSet?.toMutableList())
                    binding!!.addModuleList.layoutManager = LinearLayoutManager(context)
                    binding!!.addModuleList.setHasFixedSize(true)
                    binding!!.addModuleList.adapter = addAdapter
                    // binding!!.progressBar.hideProgressDialog(binding!!.avi)
                }
                .addOnFailureListener { e ->
                    // binding!!.progressBar.hideProgressDialog(binding!!.avi)
                    context?.let { Toast.makeText(it, e.message, Toast.LENGTH_LONG).show() }
                }
        }

        binding!!.saveButton.setOnClickListener {
            if (requireActivity().isOnline()){
                if (addAdapter != null) {
                    // (it as CircularProgressButton).startAnimation()
                    val modulesList = addAdapter!!.list()
                    Firebase.firestore.collection("users")
                        .document(Firebase.auth.currentUser!!.uid)
                        .update("Modules", modulesList)
                        .addOnCompleteListener { task ->
                            if (task.exception != null) {
                                Toast.makeText(context, task.exception?.message, Toast.LENGTH_LONG).show()
                            }
                            ViewModelProvider(this)[ProfileViewModel::class.java]
                                .setModules(prefs, modulesList.toMutableSet())
                            activity?.hideKeyboard(binding.root)
                            (requireActivity() as MainActivity).navController.navigateUp()
                        }
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