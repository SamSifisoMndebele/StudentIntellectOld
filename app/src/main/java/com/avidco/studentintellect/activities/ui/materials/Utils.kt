package com.avidco.studentintellect.activities.ui.materials

import android.content.Context
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.database.FilesFirestoreDatabase
import com.avidco.studentintellect.models.PdfFile
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object Utils {
    fun MainActivity.reloadFilesList(path: String, databaseHelper : FilesFirestoreDatabase, onComplete : () -> Unit) {
        val prefs = getSharedPreferences("last_files_read_time_prefs", Context.MODE_PRIVATE)
        val currentTime = Timestamp.now()
        val lastReadTime = Timestamp(prefs.getLong("last_read_time", 0),0)
        val lastMapReadTime = prefs.getLong("last_deleted_map_read_time", 0)

        Firebase.firestore.collection("$path/Files")
            .whereGreaterThanOrEqualTo("uploadedTime", lastReadTime)
            .get()
            .addOnCompleteListener { addedTask ->
                Firebase.firestore.collection("$path/Files")
                    .whereGreaterThanOrEqualTo("updatedTime", lastReadTime)
                    .get()
                    .addOnCompleteListener { modifiedTask ->
                        Firebase.firestore.document("$path/Deleted/Files")
                            .get()
                            .addOnCompleteListener { deletedTask ->
                                val deletedIDs = try
                                {
                                    (deletedTask.result?.get("deletedFiles") as Map<*, *>?)
                                        ?.filter { (it.value as Timestamp).seconds > lastMapReadTime }
                                } catch (e: RuntimeExecutionException) {
                                    mapOf()
                                } catch (e: IllegalStateException) {
                                    mapOf()
                                }

                                /*val isSuccess = databaseHelper.updateFilesFromFireStore(
                                    addedTask.result?.toObjects(PdfFile::class.java),
                                    modifiedTask.result?.toObjects(PdfFile::class.java),
                                    deletedIDs?.keys?.toList()
                                )*/
                                /*if (isSuccess){
                                    onComplete()
                                }*/
                                prefs.edit().putLong("last_read_time",currentTime.seconds).apply()
                                if (deletedIDs?.values?.isNotEmpty() == true)
                                    prefs.edit().putLong("last_deleted_map_read_time",
                                        deletedIDs.values.map { it as Timestamp }.maxOf { it.seconds }).apply()


                            }
                    }
            }
    }
}