package com.avidco.studentintellect.activities.ui.modules

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.materials.MaterialsFragment
import com.avidco.studentintellect.activities.ui.database.MyModulesLocalDatabase
import com.avidco.studentintellect.models.Module
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.tempDisable
import com.bumptech.glide.Glide
import java.text.DateFormat
import java.util.*

class MyModulesAdapter(
    private val activity: MainActivity,
    private val myModulesList: List<Module>,
    private val databaseHelper: MyModulesLocalDatabase,
    private val requestMultiplePermissions: ActivityResultLauncher<Array<String>>) :
    RecyclerView.Adapter<MyModulesAdapter.ViewHolder>() {

    private fun List<Module>?.itContains(module : Module) : Boolean {
        if (this == null) return false
        return map { it.id.trim() }.contains(module.id.trim())
    }


    private var clickedModule: Module? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val code: TextView = itemView.findViewById(R.id.module_code)
        private val name: TextView = itemView.findViewById(R.id.module_name)
        private val image: ImageView = itemView.findViewById(R.id.material_image)
        private val menu: ImageView = itemView.findViewById(R.id.menu_button)

        @SuppressLint("SetTextI18n")
        fun bind(module: Module) {
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

            val popupMenu = setPopupMenu(module)
            menu.setOnClickListener { menu->
                menu.tempDisable()
                popupMenu.show()
            }
            menu.visibility = View.VISIBLE
        }

        @SuppressLint("NotifyDataSetChanged")
        private fun setPopupMenu(module: Module) : PopupMenu {
            val popupMenu = PopupMenu(activity, menu)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                popupMenu.gravity = Gravity.END
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                popupMenu.setForceShowIcon(true)
            popupMenu.menuInflater.inflate(R.menu.popup_menu_module, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.item_delete -> {
                        databaseHelper.deleteModule(module.id, true)
                    }
                }
                true
            }

            popupMenu.menu.findItem(R.id.item_adder_name).title = "Added by ${module.adder.name}"
            val dateInstance = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            popupMenu.menu.findItem(R.id.item_updated_date).title = "Updated at " +
                    module.timeUpdated.toDate().let { dateInstance.format(it) }
            popupMenu.menu.findItem(R.id.item_verified).title = if (module.isVerified) "Verified" else "Not Verified"

            return popupMenu
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
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }
    fun gotoMaterialsFragment() {
        val bundle = Bundle().apply {
            putParcelable(MaterialsFragment.MY_MODULE_DATA, clickedModule!!)
            putParcelable(MaterialsFragment.FOLDER_DATA, null)
        }
        activity.navController.navigate(R.id.action_materials_fragment, bundle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(
            R.layout.item_module, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < myModulesList.size){
            holder.bind(myModulesList[position])
        }
    }

    override fun getItemCount(): Int {
        return myModulesList.size
    }
}