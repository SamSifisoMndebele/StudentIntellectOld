package com.avidco.studentintellect.activities.ui.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.avidco.studentintellect.models.Folder
import com.avidco.studentintellect.models.UserInfo
import com.google.firebase.Timestamp

abstract class FoldersLocalDatabase(val context: Context?, private val tableName: String) :
    SQLiteOpenHelper(context, tableName, null, 6) {
    
    companion object {
        private const val ID = "Id"
        private const val NAME = "Name"
        private const val PATH = "Path"
        private const val FILES_COUNT = "FilesCount"
        private const val TIME_UPDATED_SEC = "TimeUpdated"
        private const val CREATOR_UID = "CreatorUID"
        private const val CREATOR_NAME = "CreatorName"
        private const val CREATOR_EMAIL = "CreatorEmail"
        private const val CREATOR_USER_TYPE = "CreatorUserType"
        private const val IS_VERIFIED = "IsVerified"
        private const val VERIFIER_UID = "VerifierUID"
        private const val VERIFIER_NAME = "VerifierName"
        private const val VERIFIER_EMAIL = "VerifierEmail"
        private const val VERIFIER_USER_TYPE = "VerifierUserType"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """CREATE TABLE IF NOT EXISTS $tableName ($ID TEXT PRIMARY KEY UNIQUE NOT NULL, 
                                                       $NAME TEXT NOT NULL,  
                                                       $PATH TEXT NOT NULL, 
                                                       $FILES_COUNT LONG NOT NULL default 0,
                                                       $TIME_UPDATED_SEC LONG NOT NULL,
                                                       $CREATOR_UID TEXT NOT NULL,
                                                       $CREATOR_NAME TEXT NOT NULL,
                                                       $CREATOR_EMAIL TEXT NOT NULL,
                                                       $CREATOR_USER_TYPE INT NOT NULL,
                                                       $IS_VERIFIED BOOLEAN NOT NULL,
                                                       $VERIFIER_UID TEXT,
                                                       $VERIFIER_NAME TEXT,
                                                       $VERIFIER_EMAIL TEXT,
                                                       $VERIFIER_USER_TYPE TEXT)"""
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {
        db.execSQL("""DROP TABLE IF EXISTS $tableName""")
        onCreate(db)
    }

    protected fun putFolder(folder : Folder, updateList: Boolean = true) {
        val contentValues = ContentValues().apply {
            put(ID, folder.id)
            put(NAME, folder.name)
            put(PATH, folder.path)
            put(FILES_COUNT, folder.filesCount)
            put(TIME_UPDATED_SEC, folder.timeUpdated.seconds)
            put(CREATOR_UID, folder.creator.uid)
            put(CREATOR_NAME, folder.creator.name)
            put(CREATOR_EMAIL, folder.creator.email)
            put(CREATOR_USER_TYPE, folder.creator.userType)
            put(IS_VERIFIED, folder.isVerified)
            put(VERIFIER_UID, folder.verifier?.uid)
            put(VERIFIER_NAME, folder.verifier?.name)
            put(VERIFIER_EMAIL, folder.verifier?.email)
            put(VERIFIER_USER_TYPE, folder.verifier?.userType)
        }
        val isSuccess = writableDatabase.insert(tableName, null, contentValues) != -1L
        if (isSuccess && updateList) _foldersList.value = getFoldersList()
    }
    protected fun putFolders(folders : List<Folder>, updateList: Boolean = true) {
        var isSuccess = true
        for (folder in folders) {
            if (folder.id.isEmpty()) continue

            val contentValues = ContentValues().apply {
                put(ID, folder.id)
                put(NAME, folder.name)
                put(PATH, folder.path)
                put(FILES_COUNT, folder.filesCount)
                put(TIME_UPDATED_SEC, folder.timeUpdated.seconds)
                put(CREATOR_UID, folder.creator.uid)
                put(CREATOR_NAME, folder.creator.name)
                put(CREATOR_EMAIL, folder.creator.email)
                put(CREATOR_USER_TYPE, folder.creator.userType)
                put(IS_VERIFIED, folder.isVerified)
                put(VERIFIER_UID, folder.verifier?.uid)
                put(VERIFIER_NAME, folder.verifier?.name)
                put(VERIFIER_EMAIL, folder.verifier?.email)
                put(VERIFIER_USER_TYPE, folder.verifier?.userType)
            }
            isSuccess = isSuccess && (
                    writableDatabase.insert(tableName, null, contentValues.apply { put(ID, folder.id) }) != -1L
                            || writableDatabase.update(tableName, contentValues, "$ID = '${folder.id}'", null) > 0
                    )
        }
        if (isSuccess && updateList) _foldersList.value = getFoldersList()
    }

    protected fun updateFolder(folder : Folder, updateList: Boolean = true) {
        if (folder.id.isEmpty()) return
        val contentValues = ContentValues().apply {
            put(NAME, folder.name)
            put(PATH, folder.path)
            put(FILES_COUNT, folder.filesCount)
            put(TIME_UPDATED_SEC, folder.timeUpdated.seconds)
            put(CREATOR_UID, folder.creator.uid)
            put(CREATOR_NAME, folder.creator.name)
            put(CREATOR_EMAIL, folder.creator.email)
            put(CREATOR_USER_TYPE, folder.creator.userType)
            put(IS_VERIFIED, folder.isVerified)
            put(VERIFIER_UID, folder.verifier?.uid)
            put(VERIFIER_NAME, folder.verifier?.name)
            put(VERIFIER_EMAIL, folder.verifier?.email)
            put(VERIFIER_USER_TYPE, folder.verifier?.userType)
        }

        val isSuccess = writableDatabase.update(tableName, contentValues, "$ID = '${folder.id}'", null) > 0
        if (isSuccess && updateList) _foldersList.value = getFoldersList()
    }
    /*protected fun updateFolders(folders : List<Folder>, updateList: Boolean = true) {
        var isSuccess = true
        for (folder in folders) {
            if (folder.id.isEmpty()) continue
            val contentValues = ContentValues().apply {

            put(NAME, folder.name)
            put(PATH, folder.path)
            put(FILES_COUNT, folder.filesCount)
            put(TIME_UPDATED_SEC, folder.timeUpdated.seconds)
            put(CREATOR_UID, folder.creator.uid)
            put(CREATOR_NAME, folder.creator.name)
            put(CREATOR_EMAIL, folder.creator.email)
            put(CREATOR_USER_TYPE, folder.creator.userType)
            put(IS_VERIFIED, folder.isVerified)
            put(VERIFIER_UID, folder.verifier?.uid)
            put(VERIFIER_NAME, folder.verifier?.name)
            put(VERIFIER_EMAIL, folder.verifier?.email)
            put(VERIFIER_USER_TYPE, folder.verifier?.userType)
            }
            isSuccess = isSuccess && writableDatabase.update(tableName, contentValues, "$ID = '${folder.id}'", null) > 0
        }
        if (isSuccess && updateList) _foldersList.value = getFoldersList()
    }*/

    protected fun updateFolder(folder : HashMap<String, Any>, updateList: Boolean = true) {
        val id = folder["id"] as String?
        if (id.isNullOrEmpty()) return
        val contentValues = ContentValues().apply {
            val name = folder["name"] as String?
            if (name != null){ put(NAME, name) }
            val path = folder["path"] as String?
            if (path != null){ put(PATH, path) }
            val filesCount = folder["filesCount"] as Long?
            if (filesCount != null){ put(FILES_COUNT, filesCount) }
            val timeUpdated = folder["timeUpdated"] as Timestamp?
            if (timeUpdated != null){ put(TIME_UPDATED_SEC, timeUpdated.seconds) }
            val creator = folder["creator"] as UserInfo?
            if (creator?.uid != null){ put(CREATOR_UID, creator.uid) }
            if (creator?.name != null){ put(CREATOR_NAME, creator.name) }
            if (creator?.email != null){ put(CREATOR_EMAIL, creator.email) }
            if (creator?.userType != null){ put(CREATOR_USER_TYPE, creator.userType) }
            val isVerified = folder["isVerified"] as Boolean?
            if (isVerified != null){ put(IS_VERIFIED, isVerified) }
            val verifier = folder["verifier"] as UserInfo?
            if (verifier?.uid != null){ put(VERIFIER_UID, verifier.uid) }
            if (verifier?.name != null){ put(VERIFIER_NAME, verifier.name) }
            if (verifier?.email != null){ put(VERIFIER_EMAIL, verifier.email) }
            if (verifier?.userType != null){ put(VERIFIER_USER_TYPE, verifier.userType) }
        }

        val isSuccess = writableDatabase.update(tableName, contentValues, "$ID = '$id'", null) > 0
        if (isSuccess && updateList) _foldersList.value = getFoldersList()
    }
    protected fun updateFoldersMap(foldersValues : List<HashMap<String, Any>>, updateList: Boolean = true) {
        var isSuccess = true
        for (folder in foldersValues) {
            val id = folder["id"] as String?
            if (id.isNullOrEmpty()) continue
            val contentValues = ContentValues().apply {
                val name = folder["name"] as String?
                if (name != null){ put(NAME, name) }
                val path = folder["path"] as String?
                if (path != null){ put(PATH, path) }
                val filesCount = folder["filesCount"] as Long?
                if (filesCount != null){ put(FILES_COUNT, filesCount) }
                val timeUpdated = folder["timeUpdated"] as Timestamp?
                if (timeUpdated != null){ put(TIME_UPDATED_SEC, timeUpdated.seconds) }
                val creator = folder["creator"] as UserInfo?
                if (creator?.uid != null){ put(CREATOR_UID, creator.uid) }
                if (creator?.name != null){ put(CREATOR_NAME, creator.name) }
                if (creator?.email != null){ put(CREATOR_EMAIL, creator.email) }
                if (creator?.userType != null){ put(CREATOR_USER_TYPE, creator.userType) }
                val isVerified = folder["isVerified"] as Boolean?
                if (isVerified != null){ put(IS_VERIFIED, isVerified) }
                val verifier = folder["verifier"] as UserInfo?
                if (verifier?.uid != null){ put(VERIFIER_UID, verifier.uid) }
                if (verifier?.name != null){ put(VERIFIER_NAME, verifier.name) }
                if (verifier?.email != null){ put(VERIFIER_EMAIL, verifier.email) }
                if (verifier?.userType != null){ put(VERIFIER_USER_TYPE, verifier.userType) }
            }
            isSuccess = isSuccess && writableDatabase.update(tableName, contentValues, "$ID = '$id'", null) > 0
        }
        if (isSuccess && updateList) _foldersList.value = getFoldersList()
    }
    protected fun updateFolderFilesCount(folderID: String, count: Int) {
        writableDatabase.update(tableName, ContentValues().apply { put(FILES_COUNT, count) },
            "$ID = '$folderID'", null) > 0
    }

    protected fun deleteFolder(folderID: String, updateList: Boolean = true) {
        val isSuccess = writableDatabase.delete(tableName, "$ID = '$folderID'", null) != 0
        if (isSuccess && updateList) _foldersList.value = getFoldersList()
    }
    protected fun deleteFolder(folder: Folder, updateList: Boolean = true) {
        val isSuccess = writableDatabase.delete(tableName, "$ID = '${folder.id}'", null) != 0
        if (isSuccess && updateList) _foldersList.value = getFoldersList()
    }
    protected fun deleteFolders(folders: List<Folder>, updateList: Boolean = true) {
        var isSuccess = true
        for (folder in folders) {
            if (folder.id.isEmpty()) continue
            isSuccess = isSuccess && writableDatabase.delete(tableName, "$ID = '${folder.id}'", null) != 0
        }
        if (isSuccess && updateList) _foldersList.value = getFoldersList()
    }
    protected fun deleteAllFolders() {
        val isSuccess = writableDatabase.delete(tableName, null, null) != 0
        if (isSuccess) _foldersList.value = listOf()
    }

    private fun getFoldersList() : List<Folder> {
        val query = "SELECT * FROM $tableName"
        val cursor = writableDatabase.rawQuery(query, null)

        val listData = ArrayList<Folder>()
        while (cursor.moveToNext()) {
            listData.add(
                Folder(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getLong(3),
                    Timestamp(cursor.getLong(4), 0),
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

    private val _foldersList = MutableLiveData<List<Folder>>().also {
        it.value = getFoldersList()
    }

    val foldersList: LiveData<List<Folder>> = _foldersList
}