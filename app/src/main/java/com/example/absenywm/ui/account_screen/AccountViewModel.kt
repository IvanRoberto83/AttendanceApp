package com.example.absenywm.ui.account_screen

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AccountViewModel : ViewModel() {

    val username = MutableLiveData<String>()
    val department = MutableLiveData<String>()
    val phoneNumber = MutableLiveData<String>()
    val shift = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    fun loadUserData(context: android.content.Context) {
        val pref = context.getSharedPreferences("USER_SESSION", android.content.Context.MODE_PRIVATE)
        username.value = pref.getString("USERNAME", "")
        department.value = pref.getString("JABATAN", "")
        phoneNumber.value = pref.getString("PHONENUM", "")
        shift.value = pref.getString("JAMKERJA", "")
        password.value = pref.getString("PASSWORD", "")
    }
}