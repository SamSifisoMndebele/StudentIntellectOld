package com.avidco.studentintellect.activities.ui.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import androidx.core.database.getStringOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.avidco.studentintellect.activities.ui.modules.MyModulesDatabaseHelper
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.models.User
import com.avidco.studentintellect.models.UserInfo
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class UserDB(context: Context?, user : User? = null) :
    SQLiteOpenHelper(context, TABLE_NAME, null, 1) {

    var currentUser = Firebase.auth.currentUser!!
    private val myModulesList : List<ModuleData>?
    init {
        if (user != null){
            insertUser(user)
        }
        myModulesList = user?.myModulesList
    }

    companion object {
        private const val TABLE_NAME = "UserDatabaseTable"
        private const val UID = "Uid"
        private const val NAME = "Name"
        private const val EMAIL = "Email"
        private const val USER_TYPE = "UserType"
        private const val PHONE = "Phone"
        private const val BALANCE = "Balance"
        private const val IMAGE_URL = "ImageUrl"
        private const val IS_ONLINE = "IsOnline"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """CREATE TABLE IF NOT EXISTS $TABLE_NAME ($UID TEXT PRIMARY KEY UNIQUE NOT NULL, 
                                                       $NAME TEXT NOT NULL,  
                                                       $EMAIL TEXT NOT NULL, 
                                                       $USER_TYPE INT NOT NULL default 0,
                                                       $PHONE TEXT,
                                                       $BALANCE DOUBLE NOT NULL default 0.0,
                                                       $IMAGE_URL TEXT,
                                                       $IS_ONLINE BOOLEAN NOT NULL)"""
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {
        db.execSQL("""DROP TABLE IF EXISTS $TABLE_NAME""")
        onCreate(db)
    }

    private val myUser = getUser()
    private val _user = MutableLiveData<User>().also {
        it.value = myUser
    }
    private val _userInfo = MutableLiveData<UserInfo>().also {
        it.value = myUser.userInfo
    }

    private val _name = MutableLiveData<String>().also {
        it.value = myUser.userInfo.name
    }
    private val _email = MutableLiveData<String>().also {
        it.value = myUser.userInfo.email
    }
    private val _userType = MutableLiveData<Int>().also {
        it.value = myUser.userInfo.userType
    }
    private val _phone = MutableLiveData<String?>().also {
        it.value = myUser.phone
    }
    private val _balance = MutableLiveData<Double>().also {
        it.value = myUser.balance
    }
    private val _imageUrl = MutableLiveData<String?>().also {
        it.value = myUser.imageUrl
    }
    private val _isOnline = MutableLiveData<Boolean>().also {
        it.value = myUser.isOnline
    }

    private fun insertUser(user : User) {
        if (user.userInfo.uid != currentUser.uid) return
        val contentValues = ContentValues().apply {
            put(UID, currentUser.uid)
            put(NAME, user.userInfo.name)
            put(EMAIL, user.userInfo.email)
            put(USER_TYPE, user.userInfo.userType)
            put(PHONE, user.phone)
            put(BALANCE, user.balance)
            put(IMAGE_URL, user.imageUrl)
            put(IS_ONLINE, user.isOnline)
        }
        writableDatabase.insert(TABLE_NAME, null, contentValues) != -1L
    }


/*    fun doOnSignIn(onComplete : (isSuccess: Boolean, e: Exception?, userData: User?) -> Unit) {
        Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener { userDataSnapshot ->
                val userData = userDataSnapshot.toObject<User>()
                //Update user info
                if (userData == null || !userData.isTermsAccepted){
                    onComplete(false, null, userData)
                } else {
                    if (userData.myModulesList.isNullOrEmpty()){
                        onComplete(addModulesFromFirebase(userData), null, userData)
                    } else {
                        onComplete(
                            MyModulesDatabaseHelper(context).addModulesFromFirebase(userData.myModulesList)
                                && addModulesFromFirebase(userData), null, userData)
                    }
                }
            }
            .addOnFailureListener {
                onComplete(false, it, null)
            }
    }*/


    fun setUser(user : User) {
        if (user.userInfo.uid != currentUser.uid) return
        val contentValues = ContentValues().apply {
            put(NAME, user.userInfo.name)
            put(EMAIL, user.userInfo.email)
            put(USER_TYPE, user.userInfo.userType)
            put(PHONE, user.phone)
            put(BALANCE, user.balance)
            put(IMAGE_URL, user.imageUrl)
            put(IS_ONLINE, user.isOnline)
        }
        writableDatabase.update(TABLE_NAME, contentValues, "$UID = '${currentUser.uid}'", null) > 0
        _user.value = user
    }

    fun setUserInfo(userInfo : UserInfo) {
        if (userInfo.uid != currentUser.uid) return
        val contentValues = ContentValues().apply {
            put(NAME, userInfo.name)
            put(EMAIL, userInfo.email)
            put(USER_TYPE, userInfo.userType)
        }
        writableDatabase.update(TABLE_NAME, contentValues, "$UID = '${currentUser.uid}'", null) > 0
        _userInfo.value = userInfo
    }

    fun setName(userName : String) {
        writableDatabase.update(TABLE_NAME, ContentValues().apply { put(NAME, userName) },
            "$UID = '${currentUser.uid}'", null) > 0
        currentUser.updateProfile(userProfileChangeRequest {
            displayName = userName.trim()
        }).addOnSuccessListener {
            _name.value = userName
        }
    }
    fun setEmail(userEmail : String) {
        writableDatabase.update(TABLE_NAME, ContentValues().apply { put(EMAIL, userEmail) },
            "$UID = '${currentUser.uid}'", null) > 0

        _email.value = userEmail
    }
    fun setUserType(userType : Int) {
        writableDatabase.update(TABLE_NAME, ContentValues().apply { put(USER_TYPE, userType) },
            "$UID = '${currentUser.uid}'", null) > 0
        Firebase.firestore.collection("Users")
            .document(currentUser.uid)
            .update("userType", userType)
        _userType.value = userType
    }
    fun setPhone(userPhone : String) {
        writableDatabase.update(TABLE_NAME, ContentValues().apply { put(PHONE, userPhone) },
            "$UID = '${currentUser.uid}'", null) > 0
        _phone.value = userPhone
    }
    fun setBalance(userBalance : Double) {
        writableDatabase.update(TABLE_NAME, ContentValues().apply { put(BALANCE, userBalance) },
            "$UID = '${currentUser.uid}'", null) > 0
        Firebase.firestore.collection("Users")
            .document(currentUser.uid)
            .update("balance", userBalance)
        _balance.value = userBalance
    }
    private fun incrementBalance(amount : Double) {
        Firebase.firestore.collection("Users")
            .document(currentUser.uid)
            .update("balance", FieldValue.increment(amount))
    }
    private fun decrementBalance(amount : Double) {
        Firebase.firestore.collection("Users")
            .document(currentUser.uid)
            .update("balance", FieldValue.increment(-amount))
    }

    /*fun setImageUrl(userImageUri : Uri?) {
        writableDatabase.update(TABLE_NAME, ContentValues().apply { put(IMAGE_URL, userImageUri.toString()) },
            "$UID = '$uid'", null) > 0
        currentUser.updateProfile(userProfileChangeRequest {
            photoUri = userImageUri
        }).addOnSuccessListener {
            _imageUrl.value = userImageUri.toString()
        }
    }*/
    fun setImageUrl(userImageUrl : String?) {
        writableDatabase.update(TABLE_NAME, ContentValues().apply { put(IMAGE_URL, userImageUrl) },
            "$UID = '${currentUser.uid}'", null) > 0
        currentUser.updateProfile(userProfileChangeRequest {
            photoUri = if (userImageUrl == null) null else Uri.parse(userImageUrl)
        }).addOnSuccessListener {
            _imageUrl.value = userImageUrl
        }
    }
    fun setIsOnline(userIsOnline : Boolean) {
        writableDatabase.update(TABLE_NAME, ContentValues().apply { put(IS_ONLINE, userIsOnline) },
            "$UID = '${currentUser.uid}'", null) > 0
        Firebase.firestore.collection("Users")
            .document(currentUser.uid)
            .update("isOnline", userIsOnline)
        _isOnline.value = userIsOnline
    }


    fun deleteUser() {
        writableDatabase.delete(TABLE_NAME, null, null) != 0
    }

    fun addModule(module : ModuleData){
        Firebase.firestore.collection("Users")
            .document(currentUser.uid)
            .collection("MyModules")
            .document(module.id)
            .set(module)
    }
    fun deleteModule(module : ModuleData){
        Firebase.firestore.collection("Users")
            .document(currentUser.uid)
            .collection("MyModules")
            .document(module.id)
            .delete()
    }
    fun deleteModule(moduleId : String){
        Firebase.firestore.collection("Users")
            .document(currentUser.uid)
            .collection("MyModules")
            .document(moduleId)
            .delete()
    }


    private fun getUser() : User {
        val query = "SELECT * FROM $TABLE_NAME"
        val cursor = writableDatabase.rawQuery(query, null)

        var user = User()
        if (cursor.moveToNext()){
            user = User(
                UserInfo(
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getInt(3)
                ),
                cursor.getStringOrNull(4),
                cursor.getDouble(5),
                cursor.getStringOrNull(6),
                cursor.getString(7).toBoolean(),
                myModulesList
            )
        }
        cursor.close()

        return user
    }



    val user: LiveData<User> = _user
    val name: LiveData<String> = _name
    val email: LiveData<String> = _email
    val userType: LiveData<Int> = _userType
    val phone: LiveData<String?> = _phone
    val balance: LiveData<Double> = _balance
    val imageUrl: LiveData<String?> = _imageUrl
    val isOnline: LiveData<Boolean> = _isOnline
}