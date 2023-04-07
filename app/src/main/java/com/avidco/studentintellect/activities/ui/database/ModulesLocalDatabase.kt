package com.avidco.studentintellect.activities.ui.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.avidco.studentintellect.models.Module
import com.avidco.studentintellect.models.UserInfo
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ModulesLocalDatabase(val context: Context?) : SQLiteOpenHelper(context, TABLE_NAME, null, 1) {

    companion object {
        private const val TABLE_NAME = "ModulesDatabaseTable"
        private const val ID = "Id"
        private const val CODE = "Code"
        private const val NAME = "Name"
        private const val IMAGE_URL = "ImageUrl"
        private const val TIME_UPDATED = "TimeUpdated"
        private const val ADDER_UID = "AdderUID"
        private const val ADDER_NAME = "AdderName"
        private const val ADDER_EMAIL = "AdderEmail"
        private const val ADDER_USER_TYPE = "AdderUserType"
        private const val IS_VERIFIED = "isVerified"
        private const val VERIFIER_UID = "VerifierUID"
        private const val VERIFIER_NAME = "VerifierName"
        private const val VERIFIER_EMAIL = "VerifierEmail"
        private const val VERIFIER_USER_TYPE = "VerifierUserType"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """CREATE TABLE $TABLE_NAME ($ID TEXT PRIMARY KEY UNIQUE, 
                                                       $CODE TEXT NOT NULL UNIQUE, 
                                                       $NAME TEXT NOT NULL, 
                                                       $IMAGE_URL TEXT,
                                                       $TIME_UPDATED LONG NOT NULL,
                                                       $ADDER_UID TEXT NOT NULL,
                                                       $ADDER_NAME TEXT NOT NULL,
                                                       $ADDER_EMAIL TEXT NOT NULL,
                                                       $ADDER_USER_TYPE INT NOT NULL,
                                                       $IS_VERIFIED BOOLEAN NOT NULL,
                                                       $VERIFIER_UID TEXT,
                                                       $VERIFIER_NAME TEXT,
                                                       $VERIFIER_EMAIL TEXT,
                                                       $VERIFIER_USER_TYPE INT)"""
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {
        db.execSQL("""DROP TABLE IF EXISTS $TABLE_NAME""")
        onCreate(db)
    }

    //Add a module
    fun addModule(module : Module): Boolean {
        Firebase.firestore.collection("Modules")
            .document(module.id)
            .set(module, SetOptions.merge())

        val contentValues = ContentValues().apply {
            put(ID, module.id)
            put(CODE, module.code)
            put(NAME, module.name)
            put(IMAGE_URL, module.imageUrl)
            put(TIME_UPDATED, module.timeUpdated.seconds)
            put(ADDER_UID, module.adder.uid)
            put(ADDER_NAME, module.adder.name)
            put(ADDER_EMAIL, module.adder.email)
            put(ADDER_USER_TYPE, module.adder.userType)
            put(IS_VERIFIED, module.isVerified)
            put(VERIFIER_UID, module.verifier?.uid)
            put(VERIFIER_NAME, module.verifier?.name)
            put(VERIFIER_EMAIL, module.verifier?.email)
            put(VERIFIER_USER_TYPE, module.verifier?.userType)
        }
        val isSuccess = writableDatabase.insert(TABLE_NAME, null, contentValues) != -1L
        if (isSuccess) _modulesList.value = getModulesList()
        return isSuccess
    }
    fun addModules(myModules : List<Module>?, updateList: Boolean = true): Boolean {
        var isSuccess = true

        if (!myModules.isNullOrEmpty()){
            for (module in myModules){
                if (module.id.isEmpty()) continue
                Firebase.firestore.collection("Modules")
                    .document(module.id)
                    .set(module, SetOptions.merge())

                val contentValues = ContentValues().apply {
                    put(ID, module.id)
                    put(CODE, module.code)
                    put(NAME, module.name)
                    put(IMAGE_URL, module.imageUrl)
                    put(TIME_UPDATED, module.timeUpdated.seconds)
                    put(ADDER_UID, module.adder.uid)
                    put(ADDER_NAME, module.adder.name)
                    put(ADDER_EMAIL, module.adder.email)
                    put(ADDER_USER_TYPE, module.adder.userType)
                    put(IS_VERIFIED, module.isVerified)
                    put(VERIFIER_UID, module.verifier?.uid)
                    put(VERIFIER_NAME, module.verifier?.name)
                    put(VERIFIER_EMAIL, module.verifier?.email)
                    put(VERIFIER_USER_TYPE, module.verifier?.userType)
                }
                isSuccess = isSuccess && writableDatabase.insert(TABLE_NAME, null, contentValues) != -1L
            }
        }
        else return false

        if (isSuccess && updateList) _modulesList.value = getModulesList()
        return isSuccess
    }

    // Update modules
    fun updateModule(module : Module, updateList: Boolean = true): Boolean {
        Firebase.firestore.collection("Modules")
            .document(module.id)
            .set(module, SetOptions.merge())

        val contentValues = ContentValues().apply {
            put(ID, module.id)
            put(CODE, module.code)
            put(NAME, module.name)
            put(IMAGE_URL, module.imageUrl)
            put(TIME_UPDATED, module.timeUpdated.seconds)
            put(ADDER_UID, module.adder.uid)
            put(ADDER_NAME, module.adder.name)
            put(ADDER_EMAIL, module.adder.email)
            put(ADDER_USER_TYPE, module.adder.userType)
            put(IS_VERIFIED, module.isVerified)
            put(VERIFIER_UID, module.verifier?.uid)
            put(VERIFIER_NAME, module.verifier?.name)
            put(VERIFIER_EMAIL, module.verifier?.email)
            put(VERIFIER_USER_TYPE, module.verifier?.userType)
        }
        val isSuccess = writableDatabase.update(TABLE_NAME, contentValues, "$ID = '${module.id}'", null) > 0
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
                it.toObjects(Module::class.java).forEach { module ->
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
    fun listUpdateFromFireStore(addedModules : List<Module>?, modifiedModules : List<Module>?, deletedModules : List<*>?)
    {
        val isSuccess1 = addModules(addedModules, false)
        var isSuccess2 = false
        if (!modifiedModules.isNullOrEmpty()){
            for (module in modifiedModules) {
                isSuccess2 = isSuccess2 || updateModule(module, false)
            }
        }
        var isSuccess3 = false
        if (!deletedModules.isNullOrEmpty()){
            val myModulesDatabaseHelper = MyModulesLocalDatabase(context)
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
    private fun getModulesList() : ArrayList<Module> {
        val query = "SELECT * FROM $TABLE_NAME"
        val cursor = writableDatabase.rawQuery(query, null)

        val listData = ArrayList<Module>()
        while (cursor.moveToNext()) {
            listData.add(
                Module(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getStringOrNull(3),
                    Timestamp(cursor.getLong(4),0),
                    UserInfo(cursor.getString(5),
                        cursor.getString(6),
                        cursor.getString(7),
                        cursor.getInt(8)),
                    cursor.getString(9).toBoolean(),
                    UserInfo(cursor.getStringOrNull(10)?:"",
                        cursor.getStringOrNull(11)?:"",
                        cursor.getStringOrNull(12)?:"",
                        cursor.getIntOrNull(13)?:-1)
                ))
        }
        cursor.close()

        return listData
    }

    private val _modulesList = MutableLiveData<ArrayList<Module>?>().also {
        it.value = getModulesList()
    }
    val modulesList: LiveData<ArrayList<Module>?> = _modulesList
}