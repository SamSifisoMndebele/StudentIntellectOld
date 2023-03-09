package com.avidco.studentintellect.activities.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface.IfdType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.avidco.studentintellect.R
import com.avidco.studentintellect.models.ModuleData
import com.avidco.studentintellect.models.UserData
import com.avidco.studentintellect.models.UserType
import com.avidco.studentintellect.utils.Utils.hideKeyboard
import com.avidco.studentintellect.utils.Utils.isOnline
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class ProfileViewModel : ViewModel() {
    private val _displayName = MutableLiveData<String?>().apply {
        this.value = Firebase.auth.currentUser?.displayName
    }
    private val _photoUrl = MutableLiveData<String?>().apply {
        this.value = Firebase.auth.currentUser?.photoUrl.toString().ifEmpty { null }
    }

    private val _isOnline = MutableLiveData<Boolean>()
    private val _modulesList = MutableLiveData<MutableList<ModuleData>?>()
    private val _userTypeConst = MutableLiveData<Int>()
    private val _balance = MutableLiveData<Double>()
    private val _termsAccepted = MutableLiveData<Boolean>()

    fun init(context: Context) {
        _isOnline.value = context.isOnline()

        val userRef = Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
        userRef.collection("MyModules")
            .addSnapshotListener { snapshot, error ->
                if(error != null){
                    Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                _modulesList.value = snapshot?.toObjects(ModuleData::class.java)
            }
        userRef.addSnapshotListener { snapshot, error ->
                if(error != null || snapshot == null){
                    Toast.makeText(context, error?.message, Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                snapshot.toObject<UserData>()?.let { userData ->
                    _isOnline.value = userData.isOnline
                    _userTypeConst.value = userData.userType
                    _balance.value = userData.balance
                    _termsAccepted.value = userData.isTermsAccepted
                }
            }
    }


    fun setDisplayName(name: String) {
        Firebase.auth.currentUser!!.updateProfile(userProfileChangeRequest {
            displayName = name.trim()
        }).addOnSuccessListener {
            _displayName.value = name
        }
    }
    fun setPhotoUrl(photoUri: Uri?){
        Firebase.auth.currentUser!!.updateProfile(userProfileChangeRequest {
            this.photoUri = photoUri
        }).addOnSuccessListener {
            _photoUrl.value = photoUri.toString()
        }
    }
    fun checkIsOnline(context: Context) {
        Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
            .update("isOnline", context.isOnline())
    }
    fun addModule(module : ModuleData){
        Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
            .collection("MyModules")
            .document(module.id)
            .set(module)
    }
    fun deleteModule(module : ModuleData){
        Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
            .collection("MyModules")
            .document(module.id)
            .delete()
    }
    fun deleteModule(moduleId : String){
        Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
            .collection("MyModules")
            .document(moduleId)
            .delete()
    }
    fun setUserType(userTypeConst: Int){
        Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
            .update("userType", userTypeConst)
    }
    fun setBalance(amount: Double){
        Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
            .update("balance", FieldValue.increment(amount))
    }
    fun setTermsAccepted(accepted: Boolean){
        Firebase.firestore.collection("Users")
            .document(Firebase.auth.currentUser!!.uid)
            .update("termsAccepted", accepted)
    }


    val displayName: LiveData<String?> = _displayName
    val photoUrl: LiveData<String?> = _photoUrl
    val isOnline: LiveData<Boolean> = _isOnline
    val modulesList: LiveData<MutableList<ModuleData>?> = _modulesList
    val userType: LiveData<Int> = _userTypeConst
    val balance: LiveData<Double> = _balance
    val termsAccepted: LiveData<Boolean> = _termsAccepted
}