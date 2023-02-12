package com.avidco.studentintellect.ui.home

import android.content.Context
import android.content.Intent
import android.os.*
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.auth.AuthActivity
import com.avidco.studentintellect.databinding.ActivityMaterialsBinding
import com.avidco.studentintellect.models.MaterialData
import com.avidco.studentintellect.utils.Utils.getAdSize
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MaterialsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaterialsBinding
    private var adapter : MaterialsListAdapter? = null
    private var isListView = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaterialsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signOut()
            startActivity(Intent(this, AuthActivity::class.java))
            finishAffinity()
            return
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        isListView = getSharedPreferences("view_type", Context.MODE_PRIVATE).getBoolean("is_materials_list_view", true)

        val moduleCode = intent.extras?.getString(MODULE_CODE)!!
        supportActionBar?.title = moduleCode

        val prefs = getSharedPreferences("pref", MODE_PRIVATE)
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

                    adapter = MaterialsListAdapter(this, moduleCode, materials.toMutableList())
                    itemViewType(isListView, null, binding.materialList)
                    binding.materialList.setHasFixedSize(true)
                    binding.materialList.adapter = adapter
                    adapter!!.loadAd()
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

                    adapter = MaterialsListAdapter(this, moduleCode, materials.toMutableList())
                    itemViewType(isListView, null, binding.materialList)
                    binding.materialList.setHasFixedSize(true)
                    binding.materialList.adapter = adapter
                    adapter!!.loadAd()
                   // binding.progressBar.visibility = View.GONE
                }
        }


        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        val adView = AdView(this)
        adView.adUnitId = getString(R.string.activity_material_list_bannerAdUnitId)
        binding.adViewContainer.addView(adView)
        // Since we're loading the banner based on the adContainerView size, we need
        // to wait until this view is laid out before we can get the width.
        var initialLayoutComplete = false
        binding.adViewContainer.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                initialLayoutComplete = true
                adView.setAdSize(getAdSize(binding.adViewContainer))
                adView.loadAd(adRequest)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        hideKeyboard(binding.root)
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onRestart() {
        super.onRestart()
        Handler(Looper.getMainLooper()).postDelayed({
            adapter?.checkItemChanged()
        },300)
    }

    private var clickabele = true
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.material_menu, menu)
        val searchView = menu?.findItem(R.id.search_bar)?.actionView as SearchView
        searchView.animate()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                searchView.clearFocus()
                return false
            }
            override fun onQueryTextChange(string: String?): Boolean {
                adapter?.filter?.filter(string)
                return false
            }
        })
        val viewType = menu.findItem(R.id.view_type)
        itemViewType(isListView, viewType, null)
        viewType.setOnMenuItemClickListener {
            if (clickabele){
                itemViewType(adapter?.toggleItemViewType() == true, it, binding.materialList)
                clickabele = false
                Handler(Looper.getMainLooper()).postDelayed({
                    clickabele = true
                }, 500)
            }
            true
        }
        return true
    }

    private fun itemViewType(isListView : Boolean, item: MenuItem?, recyclerView: RecyclerView?) {
        if (isListView){
            item?.setIcon(R.drawable.ic_grid)
            recyclerView?.layoutManager = LinearLayoutManager(this)
        } else {
            item?.setIcon(R.drawable.ic_list)
            recyclerView?.layoutManager = GridLayoutManager(this, 2)
        }
    }

    companion object {
        const val MODULE_CODE = "arg_module_code_1"
    }
}
