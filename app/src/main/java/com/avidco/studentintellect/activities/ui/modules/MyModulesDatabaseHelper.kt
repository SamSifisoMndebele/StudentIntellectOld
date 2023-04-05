package com.avidco.studentintellect.activities.ui.modules

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.avidco.studentintellect.activities.ui.MainActivity
import com.avidco.studentintellect.activities.ui.database.UserDB
import com.avidco.studentintellect.activities.ui.modules.add.ModulesDatabaseHelper
import com.avidco.studentintellect.models.ModuleData
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class MyModulesDatabaseHelper(val context: Context?) : SQLiteOpenHelper(context, TABLE_NAME, null, 2) {

    companion object {
        private const val TABLE_NAME = "MyModulesDatabaseTable"
        private const val ID = "Id"
        private const val CODE = "Code"
        private const val NAME = "Name"
        private const val IMAGE_URL = "ImageUrl"
        private const val TIME_ADDED = "TimeAdded"
        private const val ADDER_UID = "AdderUID"
        private const val ADDER_NAME = "AdderName"
        private const val TIME_UPDATED = "TimeUpdated"
        private const val IS_VERIFIED = "isVerified"
        private const val VERIFIED_BY_UID = "verifiedByUID"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """CREATE TABLE $TABLE_NAME ($ID TEXT PRIMARY KEY UNIQUE, 
                                                       $CODE TEXT NOT NULL UNIQUE, 
                                                       $NAME TEXT NOT NULL, 
                                                       $IMAGE_URL TEXT,
                                                       $TIME_ADDED LONG NOT NULL,
                                                       $ADDER_UID TEXT NOT NULL,
                                                       $ADDER_NAME TEXT NOT NULL,
                                                       $TIME_UPDATED LONG NOT NULL, 
                                                       $IS_VERIFIED BOOLEAN NOT NULL,
                                                       $VERIFIED_BY_UID TEXT)"""
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, i: Int, i1: Int) {
        db?.execSQL("""DROP TABLE IF EXISTS $TABLE_NAME""")
        onCreate(db)
    }

    //Add all the modules from firebase
    fun addModulesFromFirebase(myModules : List<ModuleData>): Boolean {
        var isSuccess = true
        myModules.forEach { moduleData ->
            val contentValues = ContentValues().apply {
                put(ID, moduleData.id)
                put(CODE, moduleData.code)
                put(NAME, moduleData.name)
                put(IMAGE_URL, moduleData.imageUrl)
                put(TIME_ADDED, moduleData.timeAdded.seconds)
                put(ADDER_UID, moduleData.adderUID)
                put(ADDER_NAME, moduleData.adderName)
                put(TIME_UPDATED, moduleData.timeUpdated.seconds)
                put(IS_VERIFIED, moduleData.isVerified)
                put(VERIFIED_BY_UID, moduleData.verifiedByUID)
            }
            isSuccess = isSuccess && writableDatabase.insert(TABLE_NAME, null, contentValues) != -1L
        }
        return isSuccess
    }

    //Saving the modules to fire store
    fun saveModulesToFirebase(addOnCompleteListener : (task : Task<Void>) -> Unit) {
        val modulesList = getModulesList()
        Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
            .set(mapOf("myModulesList" to modulesList), SetOptions.merge())
            .addOnCompleteListener {
                addOnCompleteListener(it)
            }
    }

    //Add a module
    fun addModule(moduleData : ModuleData): Boolean {
        val contentValues = ContentValues().apply {
            put(ID, moduleData.id)
            put(CODE, moduleData.code)
            put(NAME, moduleData.name)
            put(IMAGE_URL, moduleData.imageUrl)
            put(TIME_ADDED, moduleData.timeAdded.seconds)
            put(ADDER_UID, moduleData.adderUID)
            put(ADDER_NAME, moduleData.adderName)
            put(TIME_UPDATED, moduleData.timeUpdated.seconds)
            put(IS_VERIFIED, moduleData.isVerified)
            put(VERIFIED_BY_UID, moduleData.verifiedByUID)
        }
        val isSuccess = writableDatabase.insert(TABLE_NAME, null, contentValues) != -1L
        if (isSuccess) _myModulesList.value = getModulesList()
        return isSuccess
    }
    fun addModules(myModules : List<ModuleData>): Boolean {
        var isSuccess = true
        myModules.forEach { moduleData ->
            val contentValues = ContentValues().apply {
                put(ID, moduleData.id)
                put(CODE, moduleData.code)
                put(NAME, moduleData.name)
                put(IMAGE_URL, moduleData.imageUrl)
                put(TIME_ADDED, moduleData.timeAdded.seconds)
                put(ADDER_UID, moduleData.adderUID)
                put(ADDER_NAME, moduleData.adderName)
                put(TIME_UPDATED, moduleData.timeUpdated.seconds)
                put(IS_VERIFIED, moduleData.isVerified)
                put(VERIFIED_BY_UID, moduleData.verifiedByUID)
            }
            isSuccess = isSuccess && writableDatabase.insert(TABLE_NAME, null, contentValues) != -1L
        }
        if (isSuccess) _myModulesList.value = getModulesList()
        return isSuccess
    }

    // Update modules
    fun updateModule(moduleData : ModuleData): Boolean {
        val contentValues = ContentValues().apply {
            put(ID, moduleData.id)
            put(CODE, moduleData.code)
            put(NAME, moduleData.name)
            put(IMAGE_URL, moduleData.imageUrl)
            put(TIME_ADDED, moduleData.timeAdded.seconds)
            put(ADDER_UID, moduleData.adderUID)
            put(ADDER_NAME, moduleData.adderName)
            put(TIME_UPDATED, moduleData.timeUpdated.seconds)
            put(IS_VERIFIED, moduleData.isVerified)
            put(VERIFIED_BY_UID, moduleData.verifiedByUID)
        }
        val isSuccess = writableDatabase.update(TABLE_NAME, contentValues, "$ID = '${moduleData.id}'", null) > 0
        if (isSuccess) _myModulesList.value = getModulesList()
        return isSuccess
    }
    fun updateModuleCODE(moduleId: String, code: String) : Boolean {
        Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
            .collection("MyModules")
            .document(moduleId)
            .update("code", code)

        val isSuccess = writableDatabase.update(TABLE_NAME, ContentValues().apply { put(CODE, code) }, "$ID = '$moduleId'", null) > 0
        if (isSuccess) _myModulesList.value = getModulesList()
        return isSuccess
    }
    fun updateModuleNAME(moduleId: String, name: String) : Boolean {
        Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
            .collection("MyModules")
            .document(moduleId)
            .update("name", name)

        val isSuccess = writableDatabase.update(TABLE_NAME, ContentValues().apply { put(NAME, name) }, "$ID = '$moduleId'", null) > 0
        if (isSuccess) _myModulesList.value = getModulesList()
        return isSuccess
    }
    fun updateModuleImageURL(moduleId: String, imageUrl: String) : Boolean {
        Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
            .collection("MyModules")
            .document(moduleId)
            .update("imageUrl", imageUrl)

        val isSuccess = writableDatabase.update(TABLE_NAME, ContentValues().apply { put(IMAGE_URL, imageUrl) }, "$ID = '$moduleId'", null) > 0
        if (isSuccess) _myModulesList.value = getModulesList()
        return isSuccess
    }

    //Delete modules
    fun deleteModule(moduleId: String, removeDB : Boolean = false): Boolean {
        if (removeDB){
            val myModules = getModulesList().toMutableList()
            if (myModules.removeAll(myModules.filter { it.id.trim() == moduleId.trim() }))
                Firebase.firestore.collection("Users")
                    .document(Firebase.auth.currentUser!!.uid)
                    .update("myModulesList", myModules)
        }


        val isSuccess = writableDatabase.delete(TABLE_NAME, "$ID = '$moduleId'", null) != 0
        if (isSuccess) _myModulesList.value = getModulesList()
        return isSuccess
    }
    private fun deleteAllMyModules(): Boolean {
        val isSuccess = writableDatabase.delete(TABLE_NAME, "1", null) != 0
        _myModulesList.value = null
        return isSuccess
    }

    //Save modules to database when logout
    fun doOnSignOut(task : () -> Unit) {
        saveModulesToFirebase {
            UserDB(context, null).deleteUser()
            deleteAllMyModules()
            Firebase.auth.signOut()
            task()
        }
    }

    //Get module code
    fun getModuleCODE(moduleId: String): String {
        val query = """SELECT $CODE FROM $TABLE_NAME WHERE $ID = '$moduleId'"""
        val cursor = writableDatabase.rawQuery(query, null)
        val code = cursor.getString(0)
        cursor.close()
        return code
    }

    //get all my modules list
    private fun getModulesList() : List<ModuleData> {
        val query = "SELECT * FROM $TABLE_NAME"
        val cursor = writableDatabase.rawQuery(query, null)

        val listData = mutableListOf<ModuleData>()
        while (cursor.moveToNext()) {
            listData.add(
                ModuleData(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getStringOrNull(3),
                    Timestamp(cursor.getLong(4),0),
                    cursor.getString(5),
                    cursor.getString(6),
                    Timestamp(cursor.getLong(7),0),
                    cursor.getString(8).toBoolean(),
                    cursor.getString(9)
                ))
        }
        cursor.close()

        return listData
    }
    private val _myModulesList = MutableLiveData<List<ModuleData>?>().also {
        it.value = getModulesList()
    }
    val myModulesList: LiveData<List<ModuleData>?> = _myModulesList
}