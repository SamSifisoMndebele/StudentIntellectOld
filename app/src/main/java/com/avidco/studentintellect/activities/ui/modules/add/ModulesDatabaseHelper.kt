package com.avidco.studentintellect.activities.ui.modules.add

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.avidco.studentintellect.activities.ui.modules.MyModulesDatabaseHelper
import com.avidco.studentintellect.models.ModuleData
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ModulesDatabaseHelper(val context: Context?) : SQLiteOpenHelper(context, TABLE_NAME, null, 2) {

    companion object {
        private const val TABLE_NAME = "ModulesDatabaseTable2"
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

    override fun onCreate(db: SQLiteDatabase) {
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
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {
        db.execSQL("""DROP TABLE IF EXISTS $TABLE_NAME""")
        onCreate(db)
    }

    //Add a module
    fun addModule(moduleData : ModuleData): Boolean {
        Firebase.firestore.collection("Modules")
            .document(moduleData.id)
            .set(moduleData, SetOptions.merge())

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
        if (isSuccess) _modulesList.value = getModulesList()
        return isSuccess
    }
    fun addModules(myModules : List<ModuleData>?, updateList: Boolean = true): Boolean {
        var isSuccess = true

        if (!myModules.isNullOrEmpty()){
            for (moduleData in myModules){
                if (moduleData.id.isEmpty()) continue
                Firebase.firestore.collection("Modules")
                    .document(moduleData.id)
                    .set(moduleData, SetOptions.merge())

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
        }
        else return false

        if (isSuccess && updateList) _modulesList.value = getModulesList()
        return isSuccess
    }

    // Update modules
    fun updateModule(moduleData : ModuleData, updateList: Boolean = true): Boolean {
        Firebase.firestore.collection("Modules")
            .document(moduleData.id)
            .set(moduleData, SetOptions.merge())

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
        if (isSuccess && updateList) _modulesList.value = getModulesList()
        return isSuccess
    }
    fun updateModuleCODE(moduleId: String, code: String) : Boolean {
        Firebase.firestore.collection("Modules")
            .document(moduleId)
            .update("code", code)

        val isSuccess = writableDatabase.update(TABLE_NAME, ContentValues().apply { put(
            CODE, code) }, "$ID = '$moduleId'", null) > 0
        if (isSuccess) _modulesList.value = getModulesList()
        return isSuccess
    }
    fun updateModuleNAME(moduleId: String, name: String) : Boolean {
        Firebase.firestore.collection("Modules")
            .document(moduleId)
            .update("name", name)

        val isSuccess = writableDatabase.update(TABLE_NAME, ContentValues().apply { put(
            NAME, name) }, "$ID = '$moduleId'", null) > 0
        if (isSuccess) _modulesList.value = getModulesList()
        return isSuccess
    }
    fun updateModuleImageURL(moduleId: String, imageUrl: String) : Boolean {
        Firebase.firestore.collection("Modules")
            .document(moduleId)
            .update("imageUrl", imageUrl)

        val isSuccess = writableDatabase.update(TABLE_NAME, ContentValues().apply {
            put(IMAGE_URL, imageUrl) }, "$ID = '$moduleId'", null) > 0
        if (isSuccess) _modulesList.value = getModulesList()
        return isSuccess
    }

    //Delete modules
    fun deleteModule(moduleId: String, updateList: Boolean = true): Boolean {
        Firebase.firestore.document("Deleted/Modules")
            .set(mapOf("deletedModules" to FieldValue.arrayUnion(moduleId)), SetOptions.merge())
            .addOnSuccessListener {
                Firebase.firestore.collection("Modules")
                    .document(moduleId)
                    .delete()
            }
        val isSuccess = writableDatabase.delete(TABLE_NAME, "$ID = '$moduleId'", null) != 0
        if (isSuccess && updateList) _modulesList.value = getModulesList()
        return isSuccess
    }
    private fun deleteAllModules(): Boolean {
        val modules = Firebase.firestore.collection("Modules")
        modules.get()
            .addOnSuccessListener {
                it.toObjects(ModuleData::class.java).forEach { module ->
                    modules.document(module.id).delete()
                }
            }

        val isSuccess = writableDatabase.delete(TABLE_NAME, "1", null) != 0
        _modulesList.value = null
        return isSuccess
    }
    private fun deleteAllLocalModules(): Boolean {
        val isSuccess = writableDatabase.delete(TABLE_NAME, "1", null) != 0
        _modulesList.value = null
        return isSuccess
    }

    //List Update
    fun listUpdateFromFireStore(addedModules : List<ModuleData>?, modifiedModules : List<ModuleData>?, deletedModules : List<*>?)
    {
        val isSuccess1 = addModules(addedModules, false)
        var isSuccess2 = false
        if (!modifiedModules.isNullOrEmpty()){
            for (moduleData in modifiedModules) {
                isSuccess2 = isSuccess2 || updateModule(moduleData, false)
            }
        }
        var isSuccess3 = false
        if (!deletedModules.isNullOrEmpty()){
            val myModulesDatabaseHelper = MyModulesDatabaseHelper(context)
            for (id in deletedModules){
                myModulesDatabaseHelper.deleteModule(id.toString(), true)
                isSuccess3 = isSuccess3 || deleteModule(id.toString(), false)
            }
        }

        if (isSuccess1 || isSuccess2 || isSuccess3) _modulesList.value = getModulesList()
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
    private fun getModulesList() : ArrayList<ModuleData> {
        val query = "SELECT * FROM $TABLE_NAME"
        val cursor = writableDatabase.rawQuery(query, null)

        val listData = ArrayList<ModuleData>()
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
    private val _modulesList = MutableLiveData<ArrayList<ModuleData>?>().also {
        it.value = getModulesList()
    }
    val modulesList: LiveData<ArrayList<ModuleData>?> = _modulesList
}