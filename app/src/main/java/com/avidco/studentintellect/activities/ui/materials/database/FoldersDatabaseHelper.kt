package com.avidco.studentintellect.activities.ui.materials.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.avidco.studentintellect.models.FolderData
import com.google.firebase.Timestamp

class FoldersDatabaseHelper(val context: Context?, private val tableName: String) :
    SQLiteOpenHelper(context, tableName, null, 1) {
    
    companion object {
        private const val ID = "Id"
        private const val NAME = "Name"
        private const val PATH = "Path"
        private const val FILES_COUNT = "FilesCount"
        private const val CREATE_UID = "CreatorUID"
        private const val CREATOR_NAME = "CreatorName"
        private const val CREATED_TIME_SEC = "CreatedTime"
        private const val IS_VERIFIED = "IsVerified"
        private const val VERIFIED_BY_UID = "VerifiedByUID"
        private const val UPDATED_TIME_SEC = "UpdatedTime"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """CREATE TABLE IF NOT EXISTS $tableName ($ID TEXT PRIMARY KEY UNIQUE NOT NULL, 
                                                       $NAME TEXT NOT NULL,  
                                                       $PATH TEXT NOT NULL, 
                                                       $FILES_COUNT INT NOT NULL,
                                                       $CREATE_UID TEXT NOT NULL,
                                                       $CREATOR_NAME TEXT NOT NULL,
                                                       $CREATED_TIME_SEC LONG NOT NULL,
                                                       $IS_VERIFIED BOOLEAN NOT NULL,
                                                       $VERIFIED_BY_UID TEXT NOT NULL,
                                                       $UPDATED_TIME_SEC LONG NOT NULL)"""
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {
        db.execSQL("""DROP TABLE IF EXISTS $tableName""")
        onCreate(db)
    }

    fun addFolderData(folderData : FolderData): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(ID, folderData.id)
            put(NAME, folderData.name)
            put(PATH, folderData.path)
            put(FILES_COUNT, folderData.filesCount)
            put(CREATE_UID, folderData.creatorUID)
            put(CREATOR_NAME, folderData.creatorName)
            put(CREATED_TIME_SEC, folderData.createdTime.seconds)
            put(IS_VERIFIED, folderData.isVerified)
            put(VERIFIED_BY_UID, folderData.verifiedByUID)
            put(UPDATED_TIME_SEC, folderData.updatedTime.seconds)
        }

        val result = db.insert(tableName, null, contentValues)
        //if inserted incorrectly it will return -1
        return result != -1L
    }

    fun updateFolderData(folderData : FolderData): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(ID, folderData.id)
            put(NAME, folderData.name)
            put(PATH, folderData.path)
            put(FILES_COUNT, folderData.filesCount)
            put(CREATE_UID, folderData.creatorUID)
            put(CREATOR_NAME, folderData.creatorName)
            put(CREATED_TIME_SEC, folderData.createdTime.seconds)
            put(IS_VERIFIED, folderData.isVerified)
            put(VERIFIED_BY_UID, folderData.verifiedByUID)
            put(UPDATED_TIME_SEC, folderData.updatedTime.seconds)
        }

        val result = db.update(tableName, contentValues, "$ID = '${folderData.id}'", null)
        //if inserted incorrectly it will return -1
        return result != -1
    }

    fun updateFolderFilesCount(folderID: String, count: Int) : Boolean {
        val db = this.writableDatabase
        val result = db.update(tableName, ContentValues().apply { put(FILES_COUNT, count) },
            "$ID = '$folderID'", null)
        return result != -1
    }
    
    fun deleteFolderData(folderID: String) {
        val db = this.writableDatabase
        val query = ("""DELETE FROM $tableName WHERE $ID = '$folderID'""")
        db.execSQL(query)
    }

    fun clearFolders() {
        val db = this.writableDatabase
        val query = ("""DELETE FROM $tableName""")
        db.execSQL(query)
    }

    val folderDataList: ArrayList<FolderData> get() {
        val db = this.writableDatabase
        val query = "SELECT * FROM $tableName"
        val cursor = db.rawQuery(query, null)

        val listData = ArrayList<FolderData>()
        while (cursor.moveToNext()) {
            listData.add(
                FolderData(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getInt(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    Timestamp(cursor.getLong(6), 0),
                    cursor.getString(7).toBoolean(),
                    cursor.getString(8),
                    Timestamp(cursor.getLong(9), 0)
                ))
        }
        cursor.close()

        return listData
    }

    val last : FolderData? get() {
        val db = this.writableDatabase
        val query = "SELECT * FROM $tableName ORDER BY $UPDATED_TIME_SEC DESC LIMIT 1"
        val cursor = db.rawQuery(query, null)

        var moduleData : FolderData? = null
        if (cursor.moveToNext()) {
            moduleData = FolderData(
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getInt(3),
                cursor.getString(4),
                cursor.getString(5),
                Timestamp(cursor.getLong(6), 0),
                cursor.getString(7).toBoolean(),
                cursor.getString(8),
                Timestamp(cursor.getLong(9), 0)
            )
        }
        cursor.close()

        return moduleData
    }

    private fun getFolderPath(folderID: String): String {
        val db = this.writableDatabase
        val query = """SELECT $PATH FROM $tableName WHERE $ID = '$folderID'"""
        val cursor = db.rawQuery(query, null)

        val code = cursor.getString(0)
        /*if (cursor.moveToNext()) {
            code = cursor.getString(0)
        }*/
        cursor.close()
        return code
    }
}