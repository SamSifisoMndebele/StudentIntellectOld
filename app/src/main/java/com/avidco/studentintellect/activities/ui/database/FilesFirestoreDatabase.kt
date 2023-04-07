package com.avidco.studentintellect.activities.ui.database

import android.content.Context
import android.content.SharedPreferences
import com.avidco.studentintellect.models.PdfFile
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FilesFirestoreDatabase(context: Context, path: String, tableName: String)
    : FilesLocalDatabase(context,tableName) {

    private val prefs: SharedPreferences = context.getSharedPreferences("${tableName}_last_pdfFiles_read_time_prefs", Context.MODE_PRIVATE)
    private val lastReadTime = Timestamp(prefs.getLong("last_read_time", 0),0)
    private val pdfFilesColRef = Firebase.firestore.collection("$path/PdfFiles")

    fun put(pdfFile : PdfFile) : PutPdfFileTask {
        val putTask = pdfFilesColRef.document(pdfFile.id)
            .set(pdfFile)

        return PutPdfFileTask(putTask, pdfFile, this)
    }
    fun put(pdfFiles : List<PdfFile>) : PutPdfFilesTask {

        val putBatchTask = Firebase.firestore.runBatch { batch ->
            for (pdfFile in pdfFiles) {
                if (pdfFile.id.isEmpty()) continue

                batch.set(pdfFilesColRef.document(pdfFile.id), pdfFile)
            }
        }

        return PutPdfFilesTask(putBatchTask, pdfFiles, this)
    }


    fun update(pdfFile: PdfFile) : UpdatePdfFileTask {
        val updateDocRef = pdfFilesColRef.document(pdfFile.id)
        val updateTask = updateDocRef.set(pdfFile, SetOptions.merge())
        return UpdatePdfFileTask(updateTask, pdfFile, this)
    }
    fun update(pdfFileValues: HashMap<String, Any>) : UpdatePdfFileMapTask {
        val updateDocRef = pdfFilesColRef.document(pdfFileValues["id"].toString())
        val updateBatchTask = Firebase.firestore.runBatch { batch ->
            batch.set(updateDocRef, pdfFileValues, SetOptions.merge())
            batch.update(updateDocRef, "timeUpdated", Timestamp.now())
        }
        return UpdatePdfFileMapTask(updateBatchTask, pdfFileValues, this)
    }
    fun update(pdfFilesValues : List<HashMap<String, Any>>) : UpdatePdfFilesMapTask {
        val updateBatchTask = Firebase.firestore.runBatch { batch ->
            for (pdfFileValues in pdfFilesValues) {
                val pdfFileID = pdfFileValues["id"].toString()
                if (pdfFileID.isEmpty()) continue
                val updateDocRef = pdfFilesColRef.document(pdfFileID)
                batch.set(updateDocRef, pdfFileValues, SetOptions.merge())
                batch.update(updateDocRef, "timeUpdated", Timestamp.now())
            }
        }
        return UpdatePdfFilesMapTask(updateBatchTask, pdfFilesValues, this)
    }
    fun updateDownloads(pdfFile: PdfFile) {
        val docRef = pdfFilesColRef.document(pdfFile.id)
        Firebase.firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val newDownloads = snapshot.getLong("downloads")!! + 1
            transaction.update(docRef, "downloads", newDownloads)
            updatePdfFileDownloads(pdfFile.id, newDownloads.toInt())
            null
        }
    }

    fun delete(pdfFileID: String) : DeletePdfFileTask {
        val deleteTask = pdfFilesColRef.document(pdfFileID)
            .update("isDeleted", true)

        return DeletePdfFileTask(deleteTask, pdfFileID, this)
    }
    fun deletePermanently(pdfFileID: String) : DeletePdfFilePermTask {
        val deleteDocRef = pdfFilesColRef.document(pdfFileID)

        val deleteTransaction = Firebase.firestore.runTransaction { transaction ->
            transaction.delete(deleteDocRef)
            transaction.set(pdfFilesColRef.document("DeletedPermanently"),
                mapOf("deletedIds" to FieldValue.arrayUnion(mapOf(pdfFileID to Timestamp.now())),
                    "isDeletedPermanently" to true), SetOptions.merge())

            null
        }

        return DeletePdfFilePermTask(deleteTransaction, pdfFileID, this)
    }


    fun get(isRefresh : Boolean = false) : GetPdfFilesTask {
        val currentTime = Timestamp.now()
        val modifiedTask = if (isRefresh) {
            pdfFilesColRef.whereEqualTo("isDeleted", false).get()
        } else {
            pdfFilesColRef.whereEqualTo("isDeleted", false)
                .whereGreaterThanOrEqualTo("timeUpdated", lastReadTime)
                .get()
        }
        val deletedTask = if (isRefresh) {
            pdfFilesColRef.whereEqualTo("isDeleted", true).get()
        } else {
            pdfFilesColRef.whereEqualTo("isDeleted", true)
                .whereGreaterThanOrEqualTo("timeUpdated", lastReadTime)
                .get()
        }

        return GetPdfFilesTask(modifiedTask, deletedTask, prefs, this, currentTime)
    }


    companion object {

        //Put pdfFiles
        data class PutPdfFileTask(val putTask: Task<Void>, val pdfFile : PdfFile, val database : FilesFirestoreDatabase)
        fun PutPdfFileTask.addOnFailureListener(onFailure: (e : Exception, notAddedPdfFile : PdfFile) -> Unit) : PutPdfFileTask {
            putTask.addOnFailureListener {
                onFailure(it, pdfFile)
            }
            return this
        }
        fun PutPdfFileTask.addOnSuccessListener(onSuccess: (addedPdfFiles: PdfFile) -> Unit) : PutPdfFileTask {
            putTask.addOnSuccessListener {
                onSuccess(pdfFile)
                database.putPdfFile(pdfFile)
            }
            return this
        }
        data class PutPdfFilesTask(val putBatchTask: Task<Void>, val pdfFiles : List<PdfFile>, val database : FilesFirestoreDatabase)
        fun PutPdfFilesTask.addOnFailureListener(onFailure: (e : Exception, notAddedPdfFiles : List<PdfFile>) -> Unit) : PutPdfFilesTask {
            putBatchTask.addOnFailureListener {
                onFailure(it, pdfFiles)
            }
            return this
        }
        fun PutPdfFilesTask.addOnSuccessListener(onSuccess: (addedPdfFiles : List<PdfFile>) -> Unit) : PutPdfFilesTask {
            putBatchTask.addOnSuccessListener {
                onSuccess(pdfFiles)
                database.putPdfFiles(pdfFiles)
            }
            return this
        }


        //Update pdfFiles
        data class UpdatePdfFileTask(val updateTask: Task<Void>, val pdfFile: PdfFile, val database : FilesFirestoreDatabase)
        fun UpdatePdfFileTask.addOnFailureListener(onFailure: (e : Exception) -> Unit) : UpdatePdfFileTask {
            updateTask.addOnFailureListener {
                onFailure(it)
            }
            return this
        }
        fun UpdatePdfFileTask.addOnSuccessListener(onSuccess: () -> Unit) : UpdatePdfFileTask {
            updateTask.addOnSuccessListener {
                database.updatePdfFile(pdfFile)
                onSuccess()
            }
            return this
        }
        data class UpdatePdfFileMapTask(val updateBatchTask: Task<Void>, val pdfFileValues: HashMap<String, Any>, val database : FilesFirestoreDatabase)
        fun UpdatePdfFileMapTask.addOnFailureListener(onFailure: (e : Exception, notUpdatedPdfFileValues: HashMap<String, Any>) -> Unit) : UpdatePdfFileMapTask {
            updateBatchTask.addOnFailureListener {
                onFailure(it, pdfFileValues)
            }
            return this
        }
        fun UpdatePdfFileMapTask.addOnSuccessListener(onSuccess: (updatedPdfFileValues: HashMap<String, Any>) -> Unit) : UpdatePdfFileMapTask {
            updateBatchTask.addOnSuccessListener {
                onSuccess(pdfFileValues)
                database.updatePdfFile(pdfFileValues)
            }
            return this
        }
        data class UpdatePdfFilesMapTask(val updateBatchTask: Task<Void>, val pdfFilesValues : List<HashMap<String, Any>>, val database : FilesFirestoreDatabase)
        fun UpdatePdfFilesMapTask.addOnFailureListener(onFailure: (e : Exception, notUpdatedPdfFilesValues: List<HashMap<String, Any>>) -> Unit) : UpdatePdfFilesMapTask {
            updateBatchTask.addOnFailureListener {
                onFailure(it, pdfFilesValues)
            }
            return this
        }
        fun UpdatePdfFilesMapTask.addOnSuccessListener(onSuccess: (updatedPdfFilesValues: List<HashMap<String, Any>>) -> Unit) : UpdatePdfFilesMapTask {
            updateBatchTask.addOnSuccessListener {
                onSuccess(pdfFilesValues)
                database.updatePdfFilesMap(pdfFilesValues)
            }
            return this
        }


        //Delete pdfFiles
        data class DeletePdfFileTask(val deleteTask: Task<Void>, val pdfFileID: String, val database : FilesFirestoreDatabase)
        fun DeletePdfFileTask.addOnFailureListener(onFailure: (e : Exception, notDeletedPdfFileID: String) -> Unit) : DeletePdfFileTask {
            deleteTask.addOnFailureListener {
                onFailure(it, pdfFileID)
            }
            return this
        }
        fun DeletePdfFileTask.addOnSuccessListener(onSuccess: (deletedPdfFileID: String) -> Unit) : DeletePdfFileTask {
            deleteTask.addOnSuccessListener {
                onSuccess(pdfFileID)
                database.deletePdfFile(pdfFileID)
            }
            return this
        }
        data class DeletePdfFilePermTask(val deleteTransaction: Task<Nothing>, val pdfFileID: String, val database : FilesFirestoreDatabase)
        fun DeletePdfFilePermTask.addOnFailureListener(onFailure: (e : Exception, notDeletedPdfFileID: String) -> Unit) : DeletePdfFilePermTask {
            deleteTransaction.addOnFailureListener {
                onFailure(it, pdfFileID)
            }
            return this
        }
        fun DeletePdfFilePermTask.addOnSuccessListener(onSuccess: (deletedPdfFileID: String) -> Unit) : DeletePdfFilePermTask {
            deleteTransaction.addOnSuccessListener {
                onSuccess(pdfFileID)
                database.deletePdfFile(pdfFileID)
            }
            return this
        }


        //Get pdfFiles
        data class GetPdfFilesTask(val updatedTask : Task<QuerySnapshot>,
                                  val deletedTask : Task<QuerySnapshot>,
                                  val prefs: SharedPreferences,
                                  val database : FilesFirestoreDatabase,
                                  val currentTime: Timestamp)
        fun GetPdfFilesTask.addOnFailureListener (onFailure: (e : Exception) -> Unit) : GetPdfFilesTask {
            var notFailed = true
            updatedTask.addOnFailureListener {
                if (notFailed) {
                    onFailure(it)
                    notFailed = false
                }
            }
            deletedTask.addOnFailureListener {
                if (notFailed) {
                    onFailure(it)
                    notFailed = false
                }
            }
            return this
        }
        fun GetPdfFilesTask.addOnSuccessListener(onSuccess : (updatedList: List<PdfFile>,
                                                             deletedList: List<PdfFile>) -> Unit) : GetPdfFilesTask {
            updatedTask.addOnSuccessListener { updatedSnapshot ->
                deletedTask.addOnSuccessListener { deletedSnapshot ->

                    val updatedList = updatedSnapshot.toObjects(PdfFile::class.java)
                    val deletedList = deletedSnapshot.toObjects(PdfFile::class.java)

                    database.putPdfFiles(updatedList)
                    database.deletePdfFiles(deletedList)

                    onSuccess(updatedList,deletedList)

                    prefs.edit().putLong("last_read_time",currentTime.seconds).apply()
                }
            }

            return this
        }
    }
}