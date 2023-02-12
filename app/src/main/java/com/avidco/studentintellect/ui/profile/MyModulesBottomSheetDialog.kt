package com.avidco.studentintellect.ui.profile

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.avidco.studentintellect.databinding.SheetModulesBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.avidco.studentintellect.R
import com.avidco.studentintellect.ui.MainActivity
import com.avidco.studentintellect.ui.home.ModulesListFragment
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.tempDisable

class MyModulesBottomSheetDialog(val activity: MainActivity) : BottomSheetDialogFragment() {

    private lateinit var binding: SheetModulesBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            (this as BottomSheetDialog).behavior.setPeekHeight(1280/*BottomSheetBehavior.PEEK_HEIGHT_AUTO*/, true)
        }
    }
    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = SheetModulesBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cancelButton.setOnClickListener {
            it.tempDisable()
            context?.hideKeyboard(binding.root)
            dismiss()
        }

        val prefs = requireActivity().getSharedPreferences("pref", Context.MODE_PRIVATE)
        val userModulesSet = prefs?.getStringSet("user_modules", null)

        val adapter = ListAdapter( activity, userModulesSet)

        binding.moduleList.layoutManager = LinearLayoutManager(context)
        binding.moduleList.adapter = adapter

        binding.addModuleButton.setOnClickListener {
            it.tempDisable()
            dismiss()
            //(activity as MainActivity).bottomBar.menu.select(R.id.nav_download)
            Handler(Looper.getMainLooper()).postDelayed({
                (activity.fragmentManager.findFragmentByTag("downloadsFragment") as ModulesListFragment)
                    .addModulesView()
            }, 300)

        }
    }
}








