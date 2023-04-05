package com.avidco.studentintellect.activities.pdfview

import android.content.Context
import android.content.SharedPreferences
import android.os.*
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.avidco.studentintellect.databinding.FragmentPdfBinding
import com.avidco.studentintellect.models.PdfFile
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.utils.Utils.documentsDir
import com.avidco.studentintellect.utils.Utils.loadPdf
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.File


class PdfViewFragment : Fragment() {

    companion object {
        fun newInstance(pdfFile: PdfFile, moduleData : ModuleData, isSolutions : Boolean = false): PdfViewFragment {
            val fragment = PdfViewFragment().apply {
                val bundle = Bundle().apply {
                    putParcelable(PdfViewActivity.FILE_DATA, pdfFile)
                    putParcelable(PdfViewActivity.MODULE_DATA, moduleData)
                    putBoolean("isSolutions",isSolutions) }
                arguments = bundle
            }
            return fragment
        }
    }

    private lateinit var binding: FragmentPdfBinding
    private lateinit var pdfFile: PdfFile
    private lateinit var moduleCode : String
    private lateinit var storageRef : StorageReference
    private var isSolutions : Boolean = false
    private var isOpen = false
    private lateinit var file : File

    private var prefs : SharedPreferences? = null
    private var pageKey = "page_key"

    override fun onDestroyView() {
        super.onDestroyView()
        isOpen = false
        prefs?.edit()?.putInt(pageKey, binding.pdfView.currentPage)?.apply()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPdfBinding.inflate(inflater, container, false)
        //binding.progressLayout.visibility = View.VISIBLE
        prefs = context?.getSharedPreferences("page_numbers", Context.MODE_PRIVATE)

        pdfFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(PdfViewActivity.FILE_DATA, PdfFile::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(PdfViewActivity.FILE_DATA)!!
        }
        val moduleData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(PdfViewActivity.MODULE_DATA, ModuleData::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(PdfViewActivity.MODULE_DATA)!!
        }
        moduleCode = moduleData.code
        isSolutions = requireArguments().getBoolean("isSolutions")
        storageRef = Firebase.storage.getReference(if (isSolutions) {
            "modules/$moduleCode/materials/${pdfFile.name} Solutions.pdf"
        } else {
            "modules/$moduleCode/materials/${pdfFile.name}.pdf"
        })
        isOpen = true

        pageKey = if (isSolutions) {
            "${moduleCode}_${pdfFile.name}_Solutions"
        } else {
            "${moduleCode}_${pdfFile.name}"
        }
        val page = prefs?.getInt(pageKey, 0)?:0
        checkIfFileExists(page = page)

        return binding.root
    }

    private var retry = true
    private fun checkIfFileExists(i : Int = 0, page: Int = 0) {
        file = if (i == 0) {
            if (isSolutions) {
                File(documentsDir(moduleCode), "${pdfFile.name} Solutions.pdf")
            } else {
                File(documentsDir(moduleCode), "${pdfFile.name}.pdf")
            }
        } else {
            if (isSolutions) {
                File(documentsDir(moduleCode), "${pdfFile.name} Solutions(${i}).pdf")
            } else {
                File(documentsDir(moduleCode), "${pdfFile.name}(${i}).pdf")
            }
        }

        if (file.exists() && file.length() != 0L) {
            if (file.canRead()) {
                val tasks = storageRef.activeDownloadTasks
                if (tasks.isEmpty()) {
                    if (isOpen){
                        binding.loadPdf(file, {
                            if (retry){
                                checkIfFileExists(page = page)
                                retry = false
                            } else {
                                activity?.finish()
                            }
                        }, {
                            (activity as PdfViewActivity).menuItem?.isVisible = true
                        }, page)
                    }
                } else {
                    downloadPdf(tasks[0])
                }
                
            } else {
                checkIfFileExists(i+1, page)
            }
        }
        else {
            downloadPdf()
        }
    }

    private fun downloadPdf( task : FileDownloadTask? = null) {
       // binding.progressLayout.visibility = View.VISIBLE
        if (!true) {
            binding.progressBar.visibility = View.GONE
           // binding.progressText.text = getString(R.string.no_internet_connection)
            /*binding.refreshBtn.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                binding.refreshBtn.revertAnimation()
            },2000)
            binding.refreshBtn.setOnClickListener {
                it.tempDisable()
                binding.refreshBtn.startAnimation()
                downloadPdf()
            }*/
        } else {
            //binding.refreshBtn.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            try {
                if (task == null) {
                    file.createNewFile()
                    storageRef.getFile(file).addTaskListener()
                } else {
                    task.addTaskListener(true)
                }
            } catch (e: Exception){
                e.printStackTrace()
                Toast.makeText(binding.pdfView.context, e.message, Toast.LENGTH_LONG).show()
                activity?.finish()
            }
        }
    }

    private fun FileDownloadTask.addTaskListener(continueTask : Boolean = false) {
        this
            .addOnSuccessListener {
                if (isOpen) {
                    binding.loadPdf(file, {
                        if (retry){
                            checkIfFileExists()
                            retry = false
                        } else {
                            activity?.finish()
                        }
                    }, {
                        (activity as PdfViewActivity).menuItem?.isVisible = true
                    })
                }
                if (!isSolutions) {
                    Firebase.firestore
                        .document("modules/$moduleCode/materials/${pdfFile.name}")
                        .update("downloads", FieldValue.increment(1))
                }
            }
            .addOnFailureListener {
                // Handle any errors
                Toast.makeText(binding.pdfView.context, it.message, Toast.LENGTH_SHORT).show()
                if (!continueTask){
                    if (file.length() == 0L)
                        file.delete()
                    cancel()
                }
                activity?.finish()
            }
            .addOnProgressListener {
                val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount)/*.pow(0.5)*10*/
               // binding.progressText.text = activity?.getString(R.string.downloading, progress.toInt())
                binding.progressBar.progress = progress.toInt()
            }
    }

}