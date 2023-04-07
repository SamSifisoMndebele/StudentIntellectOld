package com.avidco.studentintellect.activities.pdfview

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.avidco.studentintellect.R
import com.avidco.studentintellect.databinding.FragmentPdfFileBinding
import com.avidco.studentintellect.models.Folder
import com.avidco.studentintellect.models.Module
import com.avidco.studentintellect.models.PdfFile
import com.avidco.studentintellect.utils.Utils
import com.avidco.studentintellect.utils.pdfviewer.scroll.MyScrollHandle
import com.avidco.studentintellect.utils.pdfviewer.util.FitPolicy
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.File
import kotlin.math.pow

class PdfSolutionsFragment : Fragment() {

    companion object {
        private const val ARG_PDF_FILE = "arg_pdf_file"
        private const val ARG_PARENT_FOLDER = "arg_parent_folder"
        @JvmStatic
        fun newInstance(pdfFile: PdfFile, parentFolder : Folder): PdfSolutionsFragment {
            return PdfSolutionsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PDF_FILE, pdfFile)
                    putParcelable(ARG_PARENT_FOLDER, parentFolder)
                }
            }
        }
    }

    private var _binding: FragmentPdfFileBinding? = null
    private val binding get() = _binding!!
    private var prefs : SharedPreferences? = null
    private lateinit var pdfFile: PdfFile
    private lateinit var parentFolder: Folder
    private var isOpen = false
    private var pageKey = "page_key"
    private lateinit var storageRef : StorageReference
    private lateinit var file : File

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_PDF_FILE, pdfFile)
        outState.putParcelable(ARG_PARENT_FOLDER, parentFolder)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isOpen = true
        val args = savedInstanceState ?: requireArguments()
        pdfFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            args.getParcelable(ARG_PDF_FILE, PdfFile::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            args.getParcelable(ARG_PDF_FILE)!!
        }
        parentFolder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            args.getParcelable(ARG_PARENT_FOLDER, Folder::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            args.getParcelable(ARG_PARENT_FOLDER)!!
        }
        storageRef = Firebase.storage.getReferenceFromUrl(pdfFile.solutionsUrl!!)
        prefs = activity?.getSharedPreferences("last_page_number",Context.MODE_PRIVATE)
        pageKey = "${parentFolder.id}_${pdfFile.id}_sltns"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPdfFileBinding.inflate(inflater, container, false)
        val page = prefs?.getInt(pageKey, 0)?:0
        checkIfPdfFileExistsAndOpenOrDownload(page)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isOpen = false
    }

    override fun onStop() {
        super.onStop()
        prefs?.edit()?.putInt(pageKey, binding.pdfView.currentPage)?.apply()
    }


    private var retry = true
    private fun checkIfPdfFileExistsAndOpenOrDownload(page: Int = 0, i : Int = 0) {
        val filePath = parentFolder.path.replace("Modules/", "")
            .replace("/Folders","")
            .replace(" ", "")
        file = if (i == 0) {
            File(Utils.filesDir(filePath), "${pdfFile.id}_sltns.pdf")
        } else {
            File(Utils.filesDir(filePath), "${pdfFile.id}(${i})_sltns.pdf")
        }

        if (file.exists() && file.length() != 0L) {
            if (file.canRead()) {
                val tasks = storageRef.activeDownloadTasks
                if (tasks.isEmpty()) {
                    if (isOpen){
                        setPdfFile(file, page) {
                            if (it == null) {
                                (activity as PdfFileActivity).menu?.setGroupVisible(0, true)
                            } else {
                                Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                                if (retry){
                                    checkIfPdfFileExistsAndOpenOrDownload(page)
                                    retry = false
                                } else {
                                    activity?.finish()
                                }
                            }
                        }
                    }
                } else {
                    downloadPdfFile(tasks[0])
                }
            } else {
                checkIfPdfFileExistsAndOpenOrDownload(page, i+1)
            }
        }
        else {
            downloadPdfFile()
        }
    }

    private fun setPdfFile(file: File, page : Int = 0, onComplete : (e : Throwable?) -> Unit) {
        binding.pdfView.fromFile(file)
            .enableSwipe(true)
            .enableDoubletap(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .enableAnnotationRendering(true)
            .defaultPage(page)
            .onLoad {
                onComplete(null)
            }
            .onError {
                try { file.delete() } catch (_: Exception) {}
                onComplete(it)
            }
            .onTap { false }
            .enableAntialiasing(true)
            .spacing(0)
            .password(null)
            .scrollHandle(object : MyScrollHandle(requireContext()){})
            .autoSpacing(false)
            .pageFitPolicy(FitPolicy.WIDTH)
            .fitEachPage(true)
            .nightMode(requireContext().getString(R.string.night_mode) == "on")
            .load()
    }


    private fun downloadPdfFile(task : FileDownloadTask? = null) {
        // binding.progressLayout.visibility = View.VISIBLE
        if (true) {
            //binding.refreshBtn.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            try {
                if (task == null) {
                    file.createNewFile()
                    storageRef.getFile(file).addTaskListeners()
                } else {
                    task.addTaskListeners(true)
                }
            } catch (e: Exception){
                e.printStackTrace()
                Toast.makeText(binding.pdfView.context, e.message, Toast.LENGTH_LONG).show()
                activity?.finish()
            }
        } else {
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
        }
    }

    private fun FileDownloadTask.addTaskListeners(continueTask : Boolean = false) {
        this
            .addOnSuccessListener {
                if (isOpen) {
                    setPdfFile(file){
                        if (it == null) {
                            binding.progressBar.visibility = View.GONE
                            (activity as PdfFileActivity).menu?.setGroupVisible(0, true)
                        } else {
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                            if (retry){
                                checkIfPdfFileExistsAndOpenOrDownload()
                                retry = false
                            } else {
                                activity?.finish()
                            }
                        }
                    }
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
                val progress = ((100.0 * it.bytesTransferred) / it.totalByteCount).pow(0.5)*10
                binding.progressBar.progress = progress.toInt()
            }
    }
}