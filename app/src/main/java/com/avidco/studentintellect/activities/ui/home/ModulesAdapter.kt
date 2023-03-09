package com.avidco.studentintellect.activities.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.ProfileViewModel
import com.avidco.studentintellect.activities.ui.FirestoreAdapter
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.activities.ui.materials.MaterialsFragment
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class ModulesAdapter(
    private val activity: MainActivity,
    query: Query,
    private val requestMultiplePermissions: ActivityResultLauncher<Array<String>>,
    var userModules: MutableList<ModuleData>?
) : FirestoreAdapter<ModulesAdapter.ViewHolder>(query) {

    private var modulesSnapshots = getSnapshots()
    private var clickedModule: ModuleData? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val image: ImageView = itemView.findViewById(R.id.material_image)
        private val code: TextView = itemView.findViewById(R.id.module_code)
        private val name: TextView = itemView.findViewById(R.id.module_name)
        private val menu: ImageView = itemView.findViewById(R.id.menu_button)

        fun bind(module: ModuleData) {
            code.text = module.code
            name.text = module.name
            if (module.imageUrl != null) {
                Glide.with(activity)
                    .load(module.imageUrl)
                    .into(image)
            }

            itemView.setOnClickListener {
                it.tempDisable(2000)
                itemView.context.hideKeyboard(itemView.rootView)
                clickedModule = module
                checkPermissionsAndOpenMaterialsList()
            }

            /*itemView.setOnLongClickListener {
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
                    val dataViewModel = ViewModelProvider(activity)[ProfileViewModel::class.java]
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
                true
            }*/

            menu.setOnClickListener { menu->
                menu.tempDisable()

                val popupMenu = PopupMenu(activity, menu)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    popupMenu.gravity = Gravity.END
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    popupMenu.setForceShowIcon(true)
                popupMenu.menuInflater.inflate(R.menu.popup_menu_module, popupMenu.menu)

                popupMenu.setOnMenuItemClickListener {
                    when(it.itemId) {
                        R.id.item_delete -> {
                            val dataViewModel = ViewModelProvider(activity)[ProfileViewModel::class.java]
                            dataViewModel.deleteModule(module)
                        }
                    }
                    true
                }
                popupMenu.show()
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
            gotoMaterialsFragment()
        }
        else {
            requestMultiplePermissions.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }
    fun gotoMaterialsFragment() {
        val bundle = Bundle().apply {
            putParcelable(MaterialsFragment.MODULE_DATA, clickedModule!!)
            putParcelable(MaterialsFragment.FOLDER_DATA, null)
        }
        activity.navController.navigate(R.id.materialsFragment, bundle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(
            R.layout.item_module, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < modulesSnapshots.size){
            modulesSnapshots[position].toObject(ModuleData::class.java)?.let { holder.bind(it) }
        }
    }

    override fun getItemCount(): Int {
        return modulesSnapshots.size
    }
}