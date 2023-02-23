package com.avidco.studentintellect.activities.pdfview

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.avidco.studentintellect.models.FileData

class SectionsPagerAdapter(private val fileData: FileData, private val moduleCode : String, fa: FragmentActivity) :
    FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = if (fileData.solutionsUrl != null) 2 else 1

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            PdfViewFragment.newInstance(fileData, moduleCode, false)
        } else {
            PdfViewFragment.newInstance(fileData, moduleCode, true)
        }
    }
}