package com.avidco.studentintellect.activities.ui.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.avidco.studentintellect.models.PdfFile
import com.avidco.studentintellect.models.UserInfo
import com.google.firebase.Timestamp

abstract class LocalFiles(val context: Context?, private val tableName: String) :
    SQLiteOpenHelper(context, tableName, null, 8) {
    
    companion object {
        private const val ID = "Id"
        private const val NAME = "Name"
        private const val MATERIAL_URL = "MaterialUrl"
        private const val MATERIAL_SIZE = "MaterialSize"
        private const val SOLUTIONS_URL = "SolutionsUrl"
        private const val SOLUTIONS_SIZE = "SolutionsSize"
        private const val DOWNLOADS = "Downloads"
        private const val TIME_UPDATED_SEC = "TimeUpdated"
        private const val UPLOADER_UID = "UploaderUID"
        private const val UPLOADER_NAME = "UploaderName"
        private const val IS_EXPORTABLE = "IsExportable"
        private const val IS_VERIFIED = "IsVerified"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """CREATE TABLE IF NOT EXISTS $tableName ($ID TEXT PRIMARY KEY UNIQUE NOT NULL, 
                                                       $NAME TEXT NOT NULL,  
                                                       $MATERIAL_URL TEXT NOT NULL, 
                                                       $MATERIAL_SIZE DOUBLE NOT NULL default 0.0,
                                                       $SOLUTIONS_URL TEXT, 
                                                       $SOLUTIONS_SIZE DOUBLE,
                                                       $DOWNLOADS INT NOT NULL default 0,
                                                       $TIME_UPDATED_SEC LONG NOT NULL,
                                                       $UPLOADER_UID TEXT NOT NULL, 
                                                       $UPLOADER_NAME TEXT NOT NULL, 
                                                       $IS_EXPORTABLE BOOLEAN NOT NULL, 
                                                       $IS_VERIFIED BOOLEAN NOT NULL)"""
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {
        db.execSQL("""DROP TABLE IF EXISTS $tableName""")
        onCreate(db)
    }


    protected fun putPdfFile(pdfFile : PdfFile, updateList: Boolean = true) {
        val contentValues = ContentValues().apply {
            put(ID, pdfFile.id)
            put(NAME, pdfFile.name)
            put(MATERIAL_URL, pdfFile.materialUrl)
            put(MATERIAL_SIZE, pdfFile.materialSize)
            put(SOLUTIONS_URL, pdfFile.solutionsUrl)
            put(SOLUTIONS_SIZE, pdfFile.solutionsSize)
            put(DOWNLOADS, pdfFile.downloads)
            put(TIME_UPDATED_SEC, pdfFile.timeUpdated.seconds)
            put(UPLOADER_UID, pdfFile.uploader.uid)
            put(UPLOADER_NAME, pdfFile.uploader.name)
            put(IS_EXPORTABLE, pdfFile.isExportable)
            put(IS_VERIFIED, pdfFile.isVerified)
        }
        val isSuccess = writableDatabase.insert(tableName, null, contentValues) != -1L
        if (isSuccess && updateList) _pdfFilesList.value = getPdfFilesList()
    }
    protected fun putPdfFiles(pdfFiles : List<PdfFile>, updateList: Boolean = true) {
        var isSuccess = true
        for (pdfFile in pdfFiles) {
            if (pdfFile.id.isEmpty()) continue

            val contentValues = ContentValues().apply {
                put(NAME, pdfFile.name)
                put(MATERIAL_URL, pdfFile.materialUrl)
                put(MATERIAL_SIZE, pdfFile.materialSize)
                put(SOLUTIONS_URL, pdfFile.solutionsUrl)
                put(SOLUTIONS_SIZE, pdfFile.solutionsSize)
                put(DOWNLOADS, pdfFile.downloads)
                put(TIME_UPDATED_SEC, pdfFile.timeUpdated.seconds)
                put(UPLOADER_UID, pdfFile.uploader.uid)
                put(UPLOADER_NAME, pdfFile.uploader.name)
                put(IS_EXPORTABLE, pdfFile.isExportable)
                put(IS_VERIFIED, pdfFile.isVerified)
            }
            isSuccess = isSuccess && (
                    writableDatabase.insert(tableName, null, contentValues.apply { put(ID, pdfFile.id) }) != -1L
                            || writableDatabase.update(tableName, contentValues, "$ID = '${pdfFile.id}'", null) > 0
                    )
        }
        if (isSuccess && updateList) _pdfFilesList.value = getPdfFilesList()
    }

    protected fun updatePdfFile(pdfFile : PdfFile, updateList: Boolean = true) {
        if (pdfFile.id.isEmpty()) return
        val contentValues = ContentValues().apply {
            put(NAME, pdfFile.name)
            put(MATERIAL_URL, pdfFile.materialUrl)
            put(MATERIAL_SIZE, pdfFile.materialSize)
            put(SOLUTIONS_URL, pdfFile.solutionsUrl)
            put(SOLUTIONS_SIZE, pdfFile.solutionsSize)
            put(DOWNLOADS, pdfFile.downloads)
            put(TIME_UPDATED_SEC, pdfFile.timeUpdated.seconds)
            put(UPLOADER_UID, pdfFile.uploader.uid)
            put(UPLOADER_NAME, pdfFile.uploader.name)
            put(IS_EXPORTABLE, pdfFile.isExportable)
            put(IS_VERIFIED, pdfFile.isVerified)
        }

        val isSuccess = writableDatabase.update(tableName, contentValues, "$ID = '${pdfFile.id}'", null) > 0
        if (isSuccess && updateList) _pdfFilesList.value = getPdfFilesList()
    }
    /*protected fun updatePdfFiles(pdfFiles : List<PdfFile>, updateList: Boolean = true) {
        var isSuccess = true
        for (pdfFile in pdfFiles) {
            if (pdfFile.id.isEmpty()) continue
            val contentValues = ContentValues().apply {
            put(NAME, pdfFile.name)
            put(MATERIAL_URL, pdfFile.materialUrl)
            put(MATERIAL_SIZE, pdfFile.materialSize)
            put(SOLUTIONS_URL, pdfFile.solutionsUrl)
            put(SOLUTIONS_SIZE, pdfFile.solutionsSize)
            put(DOWNLOADS, pdfFile.downloads)
            put(TIME_UPDATED_SEC, pdfFile.timeUpdated.seconds)
            put(UPLOADER_UID, pdfFile.uploader.uid)
            put(UPLOADER_NAME, pdfFile.uploader.name)
            put(IS_EXPORTABLE, pdfFile.isExportable)
            put(IS_VERIFIED, pdfFile.isVerified)
            }
            isSuccess = isSuccess && writableDatabase.update(tableName, contentValues, "$ID = '${pdfFile.id}'", null) > 0
        }
        if (isSuccess && updateList) _pdfFilesList.value = getPdfFilesList()
    }*/

    protected fun updatePdfFile(pdfFile : HashMap<String, Any>, updateList: Boolean = true) {
        val id = pdfFile["id"] as String?
        if (id.isNullOrEmpty()) return
        val contentValues = ContentValues().apply {
            val name = pdfFile["name"] as String?
            if (name != null){ put(NAME, name) }
            val materialUrl = pdfFile["materialUrl"] as String?
            if (materialUrl != null){ put(MATERIAL_URL, materialUrl) }
            val materialSize = pdfFile["materialUrl"] as Double?
            if (materialSize != null){ put(MATERIAL_SIZE, materialSize) }
            val solutionsUrl = pdfFile["materialUrl"] as String?
            if (solutionsUrl != null){ put(SOLUTIONS_URL, solutionsUrl) }
            val solutionsSize = pdfFile["materialUrl"] as Double?
            if (solutionsSize != null){ put(SOLUTIONS_SIZE, solutionsSize) }
            val downloads = pdfFile["filesCount"] as Int?
            if (downloads != null){ put(DOWNLOADS, downloads) }
            val timeUpdated = pdfFile["timeUpdated"] as Timestamp?
            if (timeUpdated != null){ put(TIME_UPDATED_SEC, timeUpdated.seconds) }
            val uploader = pdfFile["uploader"] as UserInfo?
            if (uploader?.uid != null){ put(UPLOADER_UID, uploader.uid) }
            if (uploader?.name != null){ put(UPLOADER_NAME, uploader.name) }
            val isExportable = pdfFile["isExportable"] as Boolean?
            if (isExportable != null){ put(IS_EXPORTABLE, isExportable) }
            val isVerified = pdfFile["isVerified"] as Boolean?
            if (isVerified != null){ put(IS_VERIFIED, isVerified) }
        }

        val isSuccess = writableDatabase.update(tableName, contentValues, "$ID = '$id'", null) > 0
        if (isSuccess && updateList) _pdfFilesList.value = getPdfFilesList()
    }
    protected fun updatePdfFilesMap(pdfFilesValues : List<HashMap<String, Any>>, updateList: Boolean = true) {
        var isSuccess = true
        for (pdfFile in pdfFilesValues) {
            val id = pdfFile["id"] as String?
            if (id.isNullOrEmpty()) continue
            val contentValues = ContentValues().apply {
                val name = pdfFile["name"] as String?
                if (name != null){ put(NAME, name) }
                val materialUrl = pdfFile["materialUrl"] as String?
                if (materialUrl != null){ put(MATERIAL_URL, materialUrl) }
                val materialSize = pdfFile["materialUrl"] as Double?
                if (materialSize != null){ put(MATERIAL_SIZE, materialSize) }
                val solutionsUrl = pdfFile["materialUrl"] as String?
                if (solutionsUrl != null){ put(SOLUTIONS_URL, solutionsUrl) }
                val solutionsSize = pdfFile["materialUrl"] as Double?
                if (solutionsSize != null){ put(SOLUTIONS_SIZE, solutionsSize) }
                val downloads = pdfFile["filesCount"] as Int?
                if (downloads != null){ put(DOWNLOADS, downloads) }
                val timeUpdated = pdfFile["timeUpdated"] as Timestamp?
                if (timeUpdated != null){ put(TIME_UPDATED_SEC, timeUpdated.seconds) }
                val uploader = pdfFile["uploader"] as UserInfo?
                if (uploader?.uid != null){ put(UPLOADER_UID, uploader.uid) }
                if (uploader?.name != null){ put(UPLOADER_NAME, uploader.name) }
                val isExportable = pdfFile["isExportable"] as Boolean?
                if (isExportable != null){ put(IS_EXPORTABLE, isExportable) }
                val isVerified = pdfFile["isVerified"] as Boolean?
                if (isVerified != null){ put(IS_VERIFIED, isVerified) }
            }
            isSuccess = isSuccess && writableDatabase.update(tableName, contentValues, "$ID = '$id'", null) > 0
        }
        if (isSuccess && updateList) _pdfFilesList.value = getPdfFilesList()
    }
    protected fun updatePdfFileDownloads(pdfFileID: String, count: Int) {
        writableDatabase.update(tableName, ContentValues().apply { put(DOWNLOADS, count) },
            "$ID = '$pdfFileID'", null) > 0
    }

    protected fun deletePdfFile(pdfFileID: String, updateList: Boolean = true) {
        val isSuccess = writableDatabase.delete(tableName, "$ID = '$pdfFileID'", null) != 0
        if (isSuccess && updateList) _pdfFilesList.value = getPdfFilesList()
    }
    protected fun deletePdfFile(pdfFile: PdfFile, updateList: Boolean = true) {
        val isSuccess = writableDatabase.delete(tableName, "$ID = '${pdfFile.id}'", null) != 0
        if (isSuccess && updateList) _pdfFilesList.value = getPdfFilesList()
    }
    protected fun deletePdfFiles(pdfFiles: List<PdfFile>, updateList: Boolean = true) {
        var isSuccess = true
        for (pdfFile in pdfFiles) {
            if (pdfFile.id.isEmpty()) continue
            isSuccess = isSuccess && writableDatabase.delete(tableName, "$ID = '${pdfFile.id}'", null) != 0
        }
        if (isSuccess && updateList) _pdfFilesList.value = getPdfFilesList()
    }
    protected fun deleteAllPdfFiles() {
        val isSuccess = writableDatabase.delete(tableName, null, null) != 0
        if (isSuccess) _pdfFilesList.value = listOf()
    }

    private fun getPdfFilesList() : List<PdfFile> {
        val query = "SELECT * FROM $tableName"
        val cursor = writableDatabase.rawQuery(query, null)

        val listData = ArrayList<PdfFile>()
        while (cursor.moveToNext()) {
            listData.add(
                PdfFile(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getDouble(3),
                    cursor.getStringOrNull(4),
                    cursor.getDoubleOrNull(5),
                    cursor.getInt(6),
                    Timestamp(cursor.getLong(7), 0),
                    UserInfo(cursor.getString(8),cursor.getString(9),"", -1),
                    cursor.getString(10).toBoolean(),
                    cursor.getString(11).toBoolean()
                ))
        }
        cursor.close()

        return listData
    }

    private val _pdfFilesList = MutableLiveData<List<PdfFile>>().also {
        it.value = getPdfFilesList()
    }

    val pdfFilesList: LiveData<List<PdfFile>> = _pdfFilesList
}