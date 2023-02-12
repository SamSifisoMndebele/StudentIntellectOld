package com.avidco.studentintellect.ui.profile

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.avidco.studentintellect.R
import com.avidco.studentintellect.ui.MainActivity

class ListAdapter(val activity: MainActivity, modulesSet: MutableSet<String>?) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {
    private var userModulesSet = modulesSet?:mutableSetOf()

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        val v: View = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_module_codes, viewGroup, false)
        return ViewHolder(v)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.code.text = userModulesSet.elementAt(i)
        /*viewHolder.itemView.setOnLongClickListener {
            AlertDialog.Builder(viewHolder.itemView.context)
                .setMessage("Remove ${userModulesSet.elementAt(i)} from your modules?")
                .setNegativeButton("Cancel"){d,_-> d.dismiss() }
                .setPositiveButton("Remove"){d,_->
                    d.dismiss()
                    val tempSet = userModulesSet
                    tempSet.remove(userModulesSet.elementAt(i))
                    userModulesSet = tempSet

                    val prefs = activity.getSharedPreferences("pref", Context.MODE_PRIVATE)
                    val dataViewModel = ViewModelProvider(activity)[DataViewModel::class.java]
                    dataViewModel.setModules(prefs,userModulesSet)
                    notifyDataSetChanged()

                    Firebase.firestore.collection("users")
                        .document(Firebase.auth.currentUser!!.uid)
                        .update("modules", userModulesSet.toList())
                }
                .create()
                .show()
            true
        }*/
    }

    override fun getItemCount(): Int {
        return userModulesSet.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val code: TextView = itemView.findViewById(R.id.module_code)
    }
}