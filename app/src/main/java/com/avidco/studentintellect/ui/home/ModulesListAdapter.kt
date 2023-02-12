package com.avidco.studentintellect.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.ui.DataViewModel
import com.avidco.studentintellect.ui.FirestoreAdapter
import com.avidco.studentintellect.ui.MainActivity
import com.avidco.studentintellect.ui.upload.UploadFragment
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*
import kotlin.collections.ArrayList

class ModulesListAdapter(
    private val activity: MainActivity,
    query: Query,
    private val requestMultiplePermissions: ActivityResultLauncher<Array<String>>,
    var userModules: MutableList<String>?
) : FirestoreAdapter<ModulesListAdapter.ViewHolder>(query), Filterable {

    private var snapshotsFiltered: MutableList<DocumentSnapshot>
    private var clickedModule: ModuleData? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val code: TextView = itemView.findViewById(R.id.module_code)
        private val name: TextView = itemView.findViewById(R.id.module_name)
        private val menu: ImageView = itemView.findViewById(R.id.menu_button)

        fun bind(module: ModuleData) {
            code.text = module.code
            name.text = module.name

            itemView.setOnClickListener {
                it.tempDisable(2000)
                itemView.context.hideKeyboard(itemView.rootView)
                clickedModule = module
                checkPermissionsAndOpenMaterialsList()
            }

            itemView.setOnLongClickListener {
                true
            }

            menu.setOnClickListener { menu->
                menu.tempDisable()
                val dialog = Dialog(itemView.context)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setContentView(R.layout.menu_layout_modules)


                dialog.findViewById<TextView>(R.id.module_code).text = module.code
                dialog.findViewById<TextView>(R.id.module_name).text = module.name

                val delete = dialog.findViewById<LinearLayout>(R.id.delete)
                delete.setOnClickListener {
                    it.tempDisable()
                    dialog.dismiss()
                    val tempList = userModules
                    tempList?.remove(module.code.trim())
                    userModules = tempList

                    val prefs = activity.getSharedPreferences("pref", Context.MODE_PRIVATE)
                    val dataViewModel = ViewModelProvider(activity)[DataViewModel::class.java]
                    dataViewModel.setModules(prefs, tempList?.toMutableSet())
                    Firebase.firestore.collection("users")
                        .document(Firebase.auth.currentUser!!.uid)
                        .update("modules", tempList?.toList())

                }

                val addMaterial = dialog.findViewById<LinearLayout>(R.id.upload_material)
                addMaterial.setOnClickListener {
                    it.tempDisable()
                    dialog.dismiss()
                    //activity.bottomBar.menu.select(R.id.nav_upload)
                    Handler(Looper.getMainLooper()).postDelayed({
                        (activity.fragmentManager.findFragmentByTag("uploadFragment") as UploadFragment)
                            .moduleCodeET?.editText?.setText(module.code)
                    }, 100)
                }

                dialog.window!!.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                //dialog.window!!.attributes.windowAnimations = R.style.DialogAnimation
                dialog.window!!.setGravity(Gravity.CENTER)
                dialog.show()
            }
        }
    }

    //Permissions Check
    internal fun checkPermissionsAndOpenMaterialsList() {
        if (ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            // You can use the API that requires the permission.
            gotoMaterialsListActivity()
        }
        else {
            requestMultiplePermissions.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }
    fun gotoMaterialsListActivity() {
        val intent = Intent(activity, MaterialsListActivity::class.java).apply {
            putExtra(MaterialsListActivity.MODULE_CODE, clickedModule!!.code.trim())
        }
        activity.startActivity(intent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(
            R.layout.item_module, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < snapshotsFiltered.size){
            snapshotsFiltered[position].toObject(ModuleData::class.java)?.let { holder.bind(it) }
        }
    }

    override fun getItemCount(): Int {
        return snapshotsFiltered.size
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults {
                val pattern = constraint.toString().lowercase(Locale.getDefault())
                snapshotsFiltered = if (pattern.isEmpty()) {
                    getSnapshots()
                } else {
                    val filteredList = arrayListOf<DocumentSnapshot>()
                    for (snapshot in getSnapshots()) {
                        val data = snapshot.toObject(ModuleData::class.java)!!
                        if (data.code.lowercase().contains(pattern) || data.name.lowercase().contains(pattern)) {
                            filteredList.add(snapshot)
                        }
                    }
                    filteredList
                }

                return FilterResults().apply { values = snapshotsFiltered }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                snapshotsFiltered = results.values as ArrayList<DocumentSnapshot>
                notifyDataSetChanged()
            }
        }
    }

    init {
        this.snapshotsFiltered = getSnapshots()
    }
}