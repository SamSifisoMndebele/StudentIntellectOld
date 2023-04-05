package com.avidco.studentintellect.activities.ui.database

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AlertDialog
import com.avidco.studentintellect.models.Folder
import com.avidco.studentintellect.models.FolderDeleted
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirestoreFolders(context: Context, path: String, tableName: String)
    : LocalFolders(context,tableName) {

    private val prefs: SharedPreferences = context.getSharedPreferences("${tableName}_last_folders_read_time_prefs", Context.MODE_PRIVATE)
    private val lastReadTime = Timestamp(prefs.getLong("last_read_time", 0),0)
    private val foldersColRef = Firebase.firestore.collection("$path/Folders")

    fun putNew(folderName : String) : PutFolderTask {
        val document = foldersColRef.document()
        val folder = Folder(document.id, folderName, document.path)
        val putTask = document.set(folder)

        return PutFolderTask(putTask, folder, this)
    }
    fun put(folder : Folder) : PutFolderTask {
        val putTask = foldersColRef.document(folder.id)
            .set(folder)

        return PutFolderTask(putTask, folder, this)
    }
    fun put(folders : List<Folder>) : PutFoldersTask {

        val putBatchTask = Firebase.firestore.runBatch { batch ->
            for (folder in folders) {
                if (folder.id.isEmpty()) continue

                batch.set(foldersColRef.document(folder.id), folder)
            }
        }

        return PutFoldersTask(putBatchTask, folders, this)
    }


    fun update(folder: Folder) : UpdateFolderTask {
        val updateDocRef = foldersColRef.document(folder.id)
        val updateTask = updateDocRef.set(folder, SetOptions.merge())
        return UpdateFolderTask(updateTask,folder,this)
    }
    fun update(folderValues: HashMap<String, Any>) : UpdateFolderMapTask {
        val updateDocRef = foldersColRef.document(folderValues["id"].toString())
        val updateBatchTask = Firebase.firestore.runBatch { batch ->
            batch.set(updateDocRef, folderValues, SetOptions.merge())
            batch.update(updateDocRef, "timeUpdated", Timestamp.now())
        }
        return UpdateFolderMapTask(updateBatchTask, folderValues, this)
    }
    fun update(foldersValues : List<HashMap<String, Any>>) : UpdateFoldersMapTask {
        val updateBatchTask = Firebase.firestore.runBatch { batch ->
            for (folderValues in foldersValues) {
                val folderID = folderValues["id"].toString()
                if (folderID.isEmpty()) continue
                val updateDocRef = foldersColRef.document(folderID)
                batch.set(updateDocRef, folderValues, SetOptions.merge())
                batch.update(updateDocRef, "timeUpdated", Timestamp.now())
            }
        }
        return UpdateFoldersMapTask(updateBatchTask, foldersValues, this)
    }

    fun delete(folderID: String) : DeleteFolderTask {
        val deleteTask = foldersColRef.document(folderID)
            .update("isDelete", true)

        return DeleteFolderTask(deleteTask, folderID, this)
    }
    fun deletePermanently(folderID: String) : DeleteFolderPermTask {
        val deleteDocRef = foldersColRef.document(folderID)

        val deleteTransaction = Firebase.firestore.runTransaction { transaction ->
            transaction.delete(deleteDocRef)
            transaction.set(deleteDocRef, FolderDeleted(folderID))
            null
        }

        /*val deleteTask = foldersColRef.document(folderID).delete()
        val deletedSetTask = foldersColRef.document(folderID)
            .set(FolderDeleted(folderID))*/

        return DeleteFolderPermTask(deleteTransaction, folderID, this)
    }


    fun get(isRefresh : Boolean = false) : GetFoldersTask {
        val currentTime = Timestamp.now()
        val modifiedTask = if (isRefresh) {
            foldersColRef.whereEqualTo("isDelete", false).get()
        } else {
            foldersColRef.whereEqualTo("isDelete", false)
                .whereGreaterThanOrEqualTo("timeUpdated", lastReadTime)
                .get()
        }
        val deletedTask = if (isRefresh) {
            foldersColRef.whereEqualTo("isDelete", true).get()
        } else {
            foldersColRef.whereEqualTo("isDelete", true)
                .whereGreaterThanOrEqualTo("timeDeleted", lastReadTime)
                .get()
        }

        return GetFoldersTask(modifiedTask, deletedTask, prefs, this, currentTime)
    }


    companion object {

        //Put folders
        data class PutFolderTask(val putTask: Task<Void>, val folder : Folder, val database : FirestoreFolders)
        fun PutFolderTask.addOnFailureListener(onFailure: (e : Exception, notAddedFolder : Folder) -> Unit) : PutFolderTask {
            putTask.addOnFailureListener {
                onFailure(it, folder)
            }
            return this
        }
        fun PutFolderTask.addOnSuccessListener(onSuccess: (addedFolders: Folder) -> Unit) : PutFolderTask {
            putTask.addOnSuccessListener {
                onSuccess(folder)
                database.putFolder(folder, true)
            }
            return this
        }
        data class PutFoldersTask(val putBatchTask: Task<Void>, val folders : List<Folder>, val database : FirestoreFolders)
        fun PutFoldersTask.addOnFailureListener(onFailure: (e : Exception, notAddedFolders : List<Folder>) -> Unit) : PutFoldersTask {
            putBatchTask.addOnFailureListener {
                onFailure(it, folders)
            }
            return this
        }
        fun PutFoldersTask.addOnSuccessListener(onSuccess: (addedFolders : List<Folder>) -> Unit) : PutFoldersTask {
            putBatchTask.addOnSuccessListener {
                onSuccess(folders)
                AlertDialog.Builder(this.database.context!!)
                    .setMessage("qwerty")
                    .show()
                database.putFolders(folders, true)
            }
            return this
        }


        //Update folders
        data class UpdateFolderTask(val updateTask: Task<Void>,val folder: Folder, val database : FirestoreFolders)
        fun UpdateFolderTask.addOnFailureListener(onFailure: (e : Exception) -> Unit) : UpdateFolderTask {
            updateTask.addOnFailureListener {
                onFailure(it)
            }
            return this
        }
        fun UpdateFolderTask.addOnSuccessListener(onSuccess: () -> Unit) : UpdateFolderTask {
            updateTask.addOnSuccessListener {
                database.updateFolder(folder, true)
                onSuccess()
            }
            return this
        }
        data class UpdateFolderMapTask(val updateBatchTask: Task<Void>, val folderValues: HashMap<String, Any>, val database : FirestoreFolders)
        fun UpdateFolderMapTask.addOnFailureListener(onFailure: (e : Exception) -> Unit) : UpdateFolderMapTask {
            updateBatchTask.addOnFailureListener {
                onFailure(it)
            }
            return this
        }
        fun UpdateFolderMapTask.addOnSuccessListener(onSuccess: () -> Unit) : UpdateFolderMapTask {
            updateBatchTask.addOnSuccessListener {
                database.updateFolder(folderValues, true)
                onSuccess()
            }
            return this
        }
        data class UpdateFoldersMapTask(val updateBatchTask: Task<Void>, val foldersValues : List<HashMap<String, Any>>, val database : FirestoreFolders)
        fun UpdateFoldersMapTask.addOnFailureListener(onFailure: (e : Exception) -> Unit) : UpdateFoldersMapTask {
            updateBatchTask.addOnFailureListener {
                onFailure(it)
            }
            return this
        }
        fun UpdateFoldersMapTask.addOnSuccessListener(onSuccess: () -> Unit) : UpdateFoldersMapTask {
            updateBatchTask.addOnSuccessListener {
                database.updateFoldersMap(foldersValues, true)
                onSuccess()
            }
            return this
        }


        //Delete folders
        data class DeleteFolderTask(val deleteTask: Task<Void>, val folderID: String, val database : FirestoreFolders)
        fun DeleteFolderTask.addOnFailureListener(onFailure: (e : Exception, notDeletedFolderID: String) -> Unit) : DeleteFolderTask {
            deleteTask.addOnFailureListener {
                onFailure(it, folderID)
            }
            return this
        }
        fun DeleteFolderTask.addOnSuccessListener(onSuccess: (deletedFolderID: String) -> Unit) : DeleteFolderTask {
            deleteTask.addOnSuccessListener {
                onSuccess(folderID)
                database.deleteFolder(folderID, true)
            }
            return this
        }
        data class DeleteFolderPermTask(val deleteTransaction: Task<Nothing>, val folderID: String, val database : FirestoreFolders)
        fun DeleteFolderPermTask.addOnFailureListener(onFailure: (e : Exception, notDeletedFolderID: String) -> Unit) : DeleteFolderPermTask {
            deleteTransaction.addOnFailureListener {
                onFailure(it, folderID)
            }
            return this
        }
        fun DeleteFolderPermTask.addOnSuccessListener(onSuccess: (deletedFolderID: String) -> Unit) : DeleteFolderPermTask {
            deleteTransaction.addOnSuccessListener {
                onSuccess(folderID)
                database.deleteFolder(folderID, true)
            }
            return this
        }


        //Get folders
        data class GetFoldersTask(val updatedTask : Task<QuerySnapshot>,
                                  val deletedTask : Task<QuerySnapshot>,
                                  val prefs: SharedPreferences,
                                  val database : FirestoreFolders,
                                  val currentTime: Timestamp)
        fun GetFoldersTask.addOnFailureListener (onFailure: (e : Exception) -> Unit) : GetFoldersTask {
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
        fun GetFoldersTask.addOnSuccessListener(onSuccess : (updatedList: List<Folder>,
                                                             deletedList: List<Folder>) -> Unit) : GetFoldersTask {
            updatedTask.addOnSuccessListener { updatedSnapshot ->
                deletedTask.addOnSuccessListener { deletedSnapshot ->

                    val updatedList = updatedSnapshot.toObjects(Folder::class.java)
                    val deletedList = deletedSnapshot.toObjects(Folder::class.java)

                    database.putFolders(updatedList)
                    database.deleteFolders(deletedList, true)

                    onSuccess(updatedList,deletedList)

                    prefs.edit().putLong("last_read_time",currentTime.seconds).apply()
                }
            }

            return this
        }
    }
}