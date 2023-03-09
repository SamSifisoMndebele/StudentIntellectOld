package com.avidco.studentintellect.activities.ui.materials.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getFloatOrNull
import androidx.core.database.getStringOrNull
import com.avidco.studentintellect.models.FileData
import com.google.firebase.Timestamp

class FilesDatabaseHelper(val context: Context?, private val tableName: String) :
    SQLiteOpenHelper(context, tableName, null, 1) {
    
    companion object {
        private const val ID = "Id"
        private const val NAME = "Name"
        private const val MATERIALS_URL = "MaterialUrl"
        private const val MATERIALS_SIZE = "MaterialSize"
        private const val SOLUTIONS_URL = "SolutionsUrl"
        private const val SOLUTIONS_SIZE = "SolutionsSize"
        private const val DOWNLOADS = "Downloads"
        private const val UPLOADER_UID = "UploaderUID"
        private const val UPLOADER_NAME = "UploaderName"
        private const val UPLOADED_TIME_SEC = "UploadedTime"
        private const val IS_VERIFIED = "IsVerified"
        private const val VERIFIED_BY_UID = "VerifiedByUID"
        private const val IS_EXPORTABLE= "IsExportable"
        private const val UPDATED_TIME_SEC = "UpdatedTime"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """CREATE TABLE IF NOT EXISTS $tableName ($ID TEXT PRIMARY KEY UNIQUE NOT NULL, 
                                                       $NAME TEXT NOT NULL, 
                                                       $MATERIALS_URL TEXT NOT NULL, 
                                                       $MATERIALS_SIZE DOUBLE NOT NULL,
                                                       $SOLUTIONS_URL TEXT,
                                                       $SOLUTIONS_SIZE DOUBLE,
                                                       $DOWNLOADS INT NOT NULL,
                                                       $UPLOADER_UID TEXT NOT NULL, 
                                                       $UPLOADER_NAME TEXT NOT NULL, 
                                                       $UPLOADED_TIME_SEC LONG NOT NULL,
                                                       $IS_VERIFIED BOOLEAN NOT NULL,
                                                       $VERIFIED_BY_UID TEXT NOT NULL,
                                                       $IS_EXPORTABLE BOOLEAN NOT NULL,
                                                       $UPDATED_TIME_SEC LONG NOT NULL)"""
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {
        db.execSQL("""DROP TABLE IF EXISTS $tableName""")
        onCreate(db)
    }

    fun addFileData(fileData : FileData): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(ID, fileData.id)
            put(NAME, fileData.name)
            put(MATERIALS_URL, fileData.materialUrl)
            put(MATERIALS_SIZE, fileData.materialSize)
            put(SOLUTIONS_URL, fileData.solutionsUrl)
            put(SOLUTIONS_SIZE, fileData.solutionsSize)
            put(DOWNLOADS, fileData.downloads)
            put(UPLOADER_UID, fileData.uploaderUID)
            put(UPLOADER_NAME, fileData.uploaderName)
            put(UPLOADED_TIME_SEC, fileData.updatedTime.seconds)
            put(IS_VERIFIED, fileData.isVerified)
            put(VERIFIED_BY_UID, fileData.verifiedByUID)
            put(IS_EXPORTABLE, fileData.isExportable)
            put(UPDATED_TIME_SEC, fileData.updatedTime.seconds)
        }

        val result = db.insert(tableName, null, contentValues)
        //if inserted incorrectly it will return -1
        return result != -1L
    }

    fun updateFileData(fileData : FileData): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(ID, fileData.id)
            put(NAME, fileData.name)
            put(MATERIALS_URL, fileData.materialUrl)
            put(MATERIALS_SIZE, fileData.materialSize)
            put(SOLUTIONS_URL, fileData.solutionsUrl)
            put(SOLUTIONS_SIZE, fileData.solutionsSize)
            put(DOWNLOADS, fileData.downloads)
            put(UPLOADER_UID, fileData.uploaderUID)
            put(UPLOADER_NAME, fileData.uploaderName)
            put(UPLOADED_TIME_SEC, fileData.updatedTime.seconds)
            put(IS_VERIFIED, fileData.isVerified)
            put(VERIFIED_BY_UID, fileData.verifiedByUID)
            put(IS_EXPORTABLE, fileData.isExportable)
            put(UPDATED_TIME_SEC, fileData.updatedTime.seconds)
        }

        val result = db.update(tableName, contentValues, "$ID = '${fileData.id}'", null)
        //if inserted incorrectly it will return -1
        return result != -1
    }

    fun updateFileIsExportable(fileID: String, isExportable: Boolean) : Boolean {
        val db = this.writableDatabase
        val result = db.update(tableName, ContentValues().apply { put(IS_EXPORTABLE, isExportable) },
            "$ID = '$fileID'", null)
        return result != -1
    }
    
    fun deleteFileData(fileID: String) {
        val db = this.writableDatabase
        val query = ("""DELETE FROM $tableName WHERE $ID = '$fileID'""")
        db.execSQL(query)
    }

    fun clearFiles() {
        val db = this.writableDatabase
        val query = ("""DELETE FROM $tableName""")
        db.execSQL(query)
    }

    val fileDataList: ArrayList<FileData> get() {
        val db = this.writableDatabase
        val query = "SELECT * FROM $tableName"
        val cursor = db.rawQuery(query, null)

        val listData = ArrayList<FileData>()
        while (cursor.moveToNext()) {
            listData.add(
                FileData(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getDouble(3),
                    cursor.getStringOrNull(4),
                    cursor.getDoubleOrNull(5),
                    cursor.getInt(6),
                    cursor.getString(7),
                    cursor.getString(8),
                    Timestamp(cursor.getLong(9), 0),
                    cursor.getString(10).toBoolean(),
                    cursor.getString(11),
                    cursor.getString(12).toBoolean(),
                    Timestamp(cursor.getLong(13), 0)
                ))
            println( "Data: cursor: ${cursor.getString(0)}")
        }
        cursor.close()

        return listData
    }

    val last : FileData? get() {
        val db = this.writableDatabase
        val query = "SELECT * FROM $tableName ORDER BY $UPDATED_TIME_SEC DESC LIMIT 1"
        val cursor = db.rawQuery(query, null)

        var moduleData : FileData? = null
        if (cursor.moveToNext()) {
            moduleData = FileData(
                cursor.getString(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getDouble(3),
                cursor.getStringOrNull(4),
                cursor.getDoubleOrNull(5),
                cursor.getInt(6),
                cursor.getString(7),
                cursor.getString(8),
                Timestamp(cursor.getLong(9), 0),
                cursor.getString(10).toBoolean(),
                cursor.getString(11),
                cursor.getString(12).toBoolean(),
                Timestamp(cursor.getLong(13), 0)
            )
        }
        cursor.close()

        return moduleData
    }
}