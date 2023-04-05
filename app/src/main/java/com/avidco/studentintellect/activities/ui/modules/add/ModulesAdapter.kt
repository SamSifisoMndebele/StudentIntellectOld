package com.avidco.studentintellect.activities.ui.modules.add

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.activities.ui.modules.MyModulesDatabaseHelper
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.utils.Utils.tempDisable
import java.util.*
import kotlin.collections.ArrayList

class ModulesAdapter(
    private val context: Context?,
    private val modulesList: List<ModuleData>,
    private val databaseHelper: MyModulesDatabaseHelper
) : RecyclerView.Adapter<ModulesAdapter.ViewHolder>(), Filterable {

    init {
        databaseHelper.myModulesList.value?.forEach { module ->
            if (!modulesList.itContains(module)){
                databaseHelper.deleteModule(module.id, true)
                if (context != null) Toast.makeText(context, module.code+" is deleted by an admin.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var snapshotsFiltered = modulesList.sortedBy { it.code }

    private fun List<ModuleData>.itContains(module : ModuleData) : Boolean {
        return map { it.id.trim() }.contains(module.id.trim())
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val code: TextView = itemView.findViewById(R.id.module_code)
        private val name: TextView = itemView.findViewById(R.id.module_name)
        private val checkBox: CheckBox = itemView.findViewById(R.id.select_check_box)

        fun bind(module: ModuleData) {
            code.text = module.code
            name.text = module.id
            checkBox.isChecked = databaseHelper.myModulesList.value?.itContains(module)?:false

            checkBox.isClickable = false
            itemView.setOnClickListener {
                it.tempDisable()
                if (checkBox.isChecked) {
                    checkBox.isChecked = false
                    databaseHelper.deleteModule(module.id)
                } else {
                    checkBox.isChecked = true
                    databaseHelper.addModule(module)
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
                    modulesList.sortedBy { it.code }
                } else {
                    val filteredList = mutableListOf<ModuleData>()
                    for (data in modulesList) {
                        if (data.name.lowercase().contains(pattern) || data.code.lowercase().contains(pattern)) {
                            filteredList.add(data)
                        }
                    }
                    filteredList.sortedBy { it.code }
                }

                return FilterResults().apply { values = snapshotsFiltered }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                snapshotsFiltered = (results.values as List<ModuleData>).sortedBy { it.code }
                notifyDataSetChanged()
            }
        }
    }
}