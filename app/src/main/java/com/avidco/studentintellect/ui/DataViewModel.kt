package com.avidco.studentintellect.ui

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class DataViewModel : ViewModel() {

    private val _modulesList = MutableLiveData<MutableSet<String>?>()
    private val _photoUrl = MutableLiveData<String?>().apply {
        this.value = Firebase.auth.currentUser?.photoUrl.toString().ifEmpty { null }
    }
    private val _displayName = MutableLiveData<String?>().apply {
        this.value = Firebase.auth.currentUser?.displayName
    }
    private val _balance = MutableLiveData<Float>()

    fun init(prefs: SharedPreferences){
        _modulesList.value = prefs.getStringSet("user_modules", null)?.sorted()?.take(10)?.toMutableSet()
        _balance.value = prefs.getFloat("user_balance", 0f)
    }

    fun setModules(prefs: SharedPreferences, modulesList : MutableSet<String>?){
        prefs.edit().putStringSet("user_modules", modulesList).apply()
        _modulesList.value = modulesList
    }
    fun setPhotoUrl(photoUrl: String?){
        _photoUrl.value = photoUrl?.ifEmpty { null }
    }
    fun setDisplayName(name: String){
        _displayName.value = name
    }
    fun setBalance(prefs: SharedPreferences, amount: Float){
        val newAmount = (_balance.value?:0f) + amount
        prefs.edit().putFloat("user_balance", newAmount).apply()
        _balance.value = newAmount
    }


    val balance: LiveData<Float> = _balance
    val displayName: LiveData<String?> = _displayName
    val photoUrl: LiveData<String?> = _photoUrl
    val modulesList: LiveData<MutableSet<String>?> = _modulesList
}