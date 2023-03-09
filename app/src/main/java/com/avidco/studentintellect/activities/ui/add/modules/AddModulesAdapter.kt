package com.avidco.studentintellect.activities.ui.add.modules

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.materials.database.FoldersDatabaseHelper
import com.avidco.studentintellect.activities.ui.materials.database.ModulesDatabaseHelper
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.utils.Utils.tempDisable
import java.util.*
import kotlin.collections.ArrayList

class AddModulesAdapter(private val databaseHelper: ModulesDatabaseHelper, userModules: MutableList<ModuleData>?) :
    RecyclerView.Adapter<AddModulesAdapter.ViewHolder>(), Filterable {

    private var snapshotsFiltered = databaseHelper.moduleDataList.sortedBy { it.code }.toMutableList()
    private var selectedList = userModules?:mutableListOf()

    @SuppressLint("NotifyDataSetChanged")
    fun setModulesList(moreModules: MutableList<ModuleData>){
        snapshotsFiltered = moreModules
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val code: TextView = itemView.findViewById(R.id.module_code)
        private val checkBox: CheckBox = itemView.findViewById(R.id.select_check_box)

        fun bind(module: ModuleData) {
            code.text = module.code
            checkBox.isChecked = selectedList.contains(module)

            checkBox.isClickable = false
            itemView.setOnClickListener {
                it.tempDisable()
                if (checkBox.isChecked) {
                    checkBox.isChecked = false
                    selectedList.remove(module)
                } else {
                    checkBox.isChecked = true
                    selectedList.add(module)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(
            R.layout.item_module_select, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < snapshotsFiltered.size){
            holder.bind(snapshotsFiltered[position])
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
                    databaseHelper.moduleDataList.sortedBy { it.code }.toMutableList()
                } else {
                    val filteredList = arrayListOf<ModuleData>()
                    for (data in databaseHelper.moduleDataList) {
                        if (data.name.lowercase().contains(pattern) || data.code.lowercase().contains(pattern)
                        ) {
                            filteredList.add(data)
                        }
                    }
                    filteredList.sortedBy { it.code }.toMutableList()
                }

                return FilterResults().apply { values = snapshotsFiltered }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                snapshotsFiltered = (results.values as ArrayList<ModuleData>).sortedBy { it.code }.toMutableList()
                notifyDataSetChanged()
            }
        }
    }

    fun list(): List<ModuleData> {
        return selectedList.toList()
    }
}