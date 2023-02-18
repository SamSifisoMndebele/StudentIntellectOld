package com.avidco.studentintellect.activities.pdfview

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.avidco.studentintellect.models.MaterialData

class SectionsPagerAdapter(private val materialData: MaterialData, private val moduleCode : String, fa: FragmentActivity) :
    FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = if (materialData.withSolutions) 2 else 1

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            PdfViewFragment.newInstance(materialData, moduleCode, false)
        } else {
            PdfViewFragment.newInstance(materialData, moduleCode, true)
        }
    }
}