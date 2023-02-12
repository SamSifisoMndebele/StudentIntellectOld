package com.avidco.studentintellect.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.utils.Utils.tempDisable
import java.util.*
import kotlin.collections.ArrayList

class AddModulesAdapter(val allModules: MutableList<String>, userModules: MutableList<String>?) :
    RecyclerView.Adapter<AddModulesAdapter.ViewHolder>(), Filterable {

    private var snapshotsFiltered = allModules.sorted().toMutableList()
    private var selectedList = userModules?:mutableListOf()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val code: TextView = itemView.findViewById(R.id.module_code)
        private val checkBox: CheckBox = itemView.findViewById(R.id.select_check_box)

        fun bind(module: String) {
            code.text = module
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
                    allModules.sorted().toMutableList()
                } else {
                    val filteredList = arrayListOf<String>()
                    for (data in allModules) {
                        if (data.lowercase().contains(pattern) || data.lowercase().contains(pattern)
                        ) {
                            filteredList.add(data)
                        }
                    }
                    filteredList.sorted().toMutableList()
                }

                return FilterResults().apply { values = snapshotsFiltered.sorted().toMutableList() }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                snapshotsFiltered = (results.values as ArrayList<String>).sorted().toMutableList()
                notifyDataSetChanged()
            }
        }
    }

    fun list(): List<String> {
        return selectedList.toList()
    }
}