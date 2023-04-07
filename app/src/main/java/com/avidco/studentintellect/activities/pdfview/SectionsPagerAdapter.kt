package com.avidco.studentintellect.activities.pdfview

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.avidco.studentintellect.models.Folder
import com.avidco.studentintellect.models.PdfFile
import com.avidco.studentintellect.models.Module

class SectionsPagerAdapter(private val pdfFile: PdfFile, private val parentFolder : Folder, fa: FragmentActivity) :
    FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = if (pdfFile.solutionsUrl.isNullOrEmpty()) 1 else 2

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            PdfFileFragment.newInstance(pdfFile, parentFolder)
        } else {
            PdfSolutionsFragment.newInstance(pdfFile, parentFolder)
        }
    }
}