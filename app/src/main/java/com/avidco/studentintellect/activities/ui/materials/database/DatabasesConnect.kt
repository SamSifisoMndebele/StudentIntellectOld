package com.avidco.studentintellect.activities.ui.materials.database

import android.content.Context
import android.widget.Toast
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.models.FileData
import com.avidco.studentintellect.models.FolderData
import com.avidco.studentintellect.models.ModuleData
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.protobuf.LazyStringArrayList
import kotlinx.coroutines.tasks.await

object DatabasesConnect {

    suspend fun getFoldersDataList(activity: MainActivity, path: String, loadData : Boolean = true) : ArrayList<FolderData> {
        val name = path.replace(" ", "")
            .replace("Modules", "Folders")
            .replace("/", "_")
        val databaseHelper = FoldersDatabaseHelper(activity, "${name}_Table")

        if (loadData){
            val prefs = activity.getSharedPreferences("last_folders_read_time_prefs", Context.MODE_PRIVATE)

            val currentTime = Timestamp.now()
            val lastReadTime = Timestamp(prefs.getLong(name, 0),0)

            val addedFoldersSnapshot = Firebase.firestore.collection("$path/Folders")
                .whereGreaterThanOrEqualTo("createdTime", lastReadTime)
                .get().await()
            val modifiedFoldersSnapshot = Firebase.firestore.collection("$path/Folders")
                .whereGreaterThanOrEqualTo("updatedTime", lastReadTime)
                .get().await()
            val deletedFoldersSnapshot = Firebase.firestore.document("$path/Deleted/Folders")
                .get().await()

            prefs.edit().putLong(name,currentTime.seconds).apply()

            if (!addedFoldersSnapshot.isEmpty){
                for (folder in addedFoldersSnapshot.toObjects(FolderData::class.java))
                    databaseHelper.addFolderData(folder)
            }
            if (!modifiedFoldersSnapshot.isEmpty){
                for (folder in modifiedFoldersSnapshot.toObjects(FolderData::class.java))
                    databaseHelper.updateFolderData(folder)
            }
            if (deletedFoldersSnapshot.exists()){
                val ids = deletedFoldersSnapshot.get("ids") as List<*>?
                ids?.forEach {
                    databaseHelper.deleteFolderData(it.toString())
                }
            }
        }

        return databaseHelper.folderDataList
    }

    suspend fun getFilesDataList(activity: MainActivity, path: String, loadData : Boolean = true) : ArrayList<FileData> {
        val name = path.replace(" ", "")
            .replace("Modules", "Files")
            .replace("/", "_")
        val databaseHelper = FilesDatabaseHelper(activity, "${name}_Table")

        if (loadData) {
            val prefs = activity.getSharedPreferences("last_files_read_time_prefs", Context.MODE_PRIVATE)

            val currentTime = Timestamp.now()
            val lastReadTime = Timestamp(prefs.getLong(name, 0),0)

            val addedFilesSnapshot = Firebase.firestore.collection("$path/Files")
                .whereGreaterThanOrEqualTo("uploadedTime", lastReadTime)
                .get().await()
            val modifiedFilesSnapshot = Firebase.firestore.collection("$path/Files")
                .whereGreaterThanOrEqualTo("updatedTime", lastReadTime)
                .get().await()
            val deletedFilesSnapshot = Firebase.firestore.document("$path/Deleted/Files")
                .get().await()

            prefs.edit().putLong(name,currentTime.seconds).apply()

            if (!addedFilesSnapshot.isEmpty){
                for (file in addedFilesSnapshot.toObjects(FileData::class.java))
                    databaseHelper.addFileData(file)
            }
            if (!modifiedFilesSnapshot.isEmpty){
                for (file in modifiedFilesSnapshot.toObjects(FileData::class.java))
                    databaseHelper.updateFileData(file)
            }
            if (deletedFilesSnapshot.exists()){
                val ids = deletedFilesSnapshot.get("ids") as List<*>?
                ids?.forEach {
                    databaseHelper.deleteFileData(it.toString())
                }
            }
        }

        return databaseHelper.fileDataList
    }

    suspend fun getModulesDataList(activity: MainActivity, loadData : Boolean = true) : ArrayList<ModuleData> {
        val databaseHelper = ModulesDatabaseHelper(activity)

        if (loadData){
            val prefs = activity.getSharedPreferences("last_modules_read_time_prefs", Context.MODE_PRIVATE)

            val currentTime = Timestamp.now()
            val lastReadTime = Timestamp(prefs.getLong("last_read_time", 0),0)

            val addedModulesSnapshot = Firebase.firestore.collection("Modules")
                .whereGreaterThanOrEqualTo("timeAdded", lastReadTime)
                .get().await()
            val modifiedModulesSnapshot = Firebase.firestore.collection("Modules")
                .whereGreaterThanOrEqualTo("timeUpdated", lastReadTime)
                .get().await()
            val deletedModulesSnapshot = Firebase.firestore.collection("Deleted")
                .document("Modules")
                .get().await()

            prefs.edit().putLong("last_read_time",currentTime.seconds).apply()

            if (!addedModulesSnapshot.isEmpty){
                for (moduleData in addedModulesSnapshot.toObjects(ModuleData::class.java))
                    databaseHelper.addModuleData(moduleData)
            }
            if (!modifiedModulesSnapshot.isEmpty){
                for (moduleData in modifiedModulesSnapshot.toObjects(ModuleData::class.java))
                    databaseHelper.updateModuleData(moduleData)
            }
            if (deletedModulesSnapshot.exists()){
                val ids = deletedModulesSnapshot.get("ids") as List<*>?
                ids?.forEach {
                    databaseHelper.deleteModuleData(it.toString())
                }
            }
        }

        return databaseHelper.moduleDataList
    }
}