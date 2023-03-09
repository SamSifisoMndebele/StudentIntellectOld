package com.avidco.studentintellect.activities.ui.materials.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getStringOrNull
import com.avidco.studentintellect.models.ModuleData
import com.google.firebase.Timestamp

class ModulesDatabaseHelper(val context: Context?) : SQLiteOpenHelper(context, TABLE_NAME, null, 1) {

    companion object {
        private const val TABLE_NAME = "AllModulesTable"
        private const val ID = "Id"
        private const val CODE = "Code"
        private const val NAME = "Name"
        private const val IMAGE_URL = "ImageUrl"
        private const val DATE_ADDED = "DateAdded"
        private const val DATE_ADDED_NANO = "DateAdded"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """CREATE TABLE $TABLE_NAME ($ID TEXT PRIMARY KEY UNIQUE, 
                                                       $CODE TEXT NOT NULL UNIQUE, 
                                                       $NAME TEXT NOT NULL UNIQUE, 
                                                       $IMAGE_URL TEXT,
                                                       $DATE_ADDED LONG NOT NULL,
                                                        $DATE_ADDED_NANO INT NOT NULL)"""
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {
        db.execSQL("""DROP TABLE IF EXISTS $TABLE_NAME""")
        onCreate(db)
    }

    fun addModuleData(moduleData : ModuleData): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(ID, moduleData.id)
            put(CODE, moduleData.code)
            put(NAME, moduleData.name)
            put(IMAGE_URL, moduleData.imageUrl)
            put(DATE_ADDED, moduleData.timeAdded.seconds)
            put(DATE_ADDED_NANO, moduleData.timeAdded.nanoseconds)
        }

        val result = db.insert(TABLE_NAME, null, contentValues)
        println( "addData: Adding $moduleData to $TABLE_NAME")
        //if inserted incorrectly it will return -1
        return result != -1L
    }

    fun updateModuleData(moduleData : ModuleData): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(ID, moduleData.id)
            put(CODE, moduleData.code)
            put(NAME, moduleData.name)
            put(IMAGE_URL, moduleData.imageUrl)
            put(DATE_ADDED, moduleData.timeAdded.seconds)
            put(DATE_ADDED_NANO, moduleData.timeAdded.nanoseconds)
        }

        val result = db.update(TABLE_NAME, contentValues, "$CODE = '${moduleData.code}'", null)
        println( "addData: Adding $moduleData to $TABLE_NAME")
        //if inserted incorrectly it will return -1
        return result != -1
    }

    fun updateModuleDataNAME(moduleCode: String, name: String) : Boolean {
        val db = this.writableDatabase
        val result = db.update(TABLE_NAME, ContentValues().apply { put(NAME, name) }, "$CODE = '$moduleCode'", null)
        return result != -1
    }
    fun updateModuleDataImageURL(moduleCode: String, imageUrl: String) : Boolean {
        val db = this.writableDatabase
        val result = db.update(TABLE_NAME, ContentValues().apply { put(IMAGE_URL, imageUrl) }, "$CODE = '$moduleCode'", null)
        return result != -1
    }
    
    fun deleteModuleData(id: String) {
        val db = this.writableDatabase
        val query = ("""DELETE FROM $TABLE_NAME WHERE $ID = '$id'""")
        println("deleteName: query: $query, \nDeleting $id from database.")
        db.execSQL(query)
    }

    fun clearStrings() {
        val db = this.writableDatabase
        val query = ("""DELETE FROM $TABLE_NAME""")
        db.execSQL(query)
    }

    val moduleDataList: ArrayList<ModuleData> get() {
        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_NAME"
        val cursor = db.rawQuery(query, null)

        val listData = ArrayList<ModuleData>()
        while (cursor.moveToNext()) {
            listData.add(
                ModuleData(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getStringOrNull(3),
                    Timestamp(cursor.getLong(4), 0),
                    Timestamp(cursor.getLong(5), 0)
                ))
            println( "Data: cursor: ${cursor.getString(0)}")
        }
        cursor.close()

        return listData
    }

    val last : ModuleData? get() {
        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_NAME ORDER BY $DATE_ADDED DESC LIMIT 1"
        val cursor = db.rawQuery(query, null)

        var moduleData : ModuleData? = null
        if (cursor.moveToNext()) {
            moduleData = ModuleData(
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getStringOrNull(3),
                Timestamp(cursor.getLong(4), 0),
                Timestamp(cursor.getLong(5), 0)
            )
        }
        cursor.close()

        return moduleData
    }

    private fun getModuleCODE(id: String): String {
        val db = this.writableDatabase
        val query = """SELECT $CODE FROM $TABLE_NAME WHERE $ID = '$id'"""
        val cursor = db.rawQuery(query, null)

        val code = cursor.getString(0)
        /*if (cursor.moveToNext()) {
            code = cursor.getString(0)
        }*/
        cursor.close()
        return code
    }
}