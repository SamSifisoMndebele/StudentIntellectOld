package com.avidco.studentintellect.activities.pdfview

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.avidco.studentintellect.models.PdfFile
import com.avidco.studentintellect.models.ModuleData

class SectionsPagerAdapter(private val pdfFile: PdfFile, private val moduleData : ModuleData, fa: FragmentActivity) :
    FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = if (pdfFile.solutionsUrl != null) 2 else 1

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            PdfViewFragment.newInstance(pdfFile, moduleData, false)
        } else {
            PdfViewFragment.newInstance(pdfFile, moduleData, true)
        }
    }
}