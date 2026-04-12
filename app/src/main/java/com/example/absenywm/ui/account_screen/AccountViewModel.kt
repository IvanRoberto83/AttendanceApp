package com.example.absenywm.ui.account_screen

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AccountViewModel : ViewModel() {
    val email = MutableLiveData<String>()
    val username = MutableLiveData<String>()
    val department = MutableLiveData<String>()
    val phoneNumber = MutableLiveData<String>()
    val shift = MutableLiveData<String>()

    fun loadUserData(context: android.content.Context) {
        val pref = context.getSharedPreferences("USER_SESSION", android.content.Context.MODE_PRIVATE)

        email.value = pref.getString("EMAIL","")?: ""
        username.value = pref.getString("USERNAME", "") ?: ""
        department.value = pref.getString("DEPARTMENT", "") ?: ""
        phoneNumber.value = pref.getString("PHONENUM", "") ?: ""
        shift.value = pref.getString("SHIFT", "") ?: ""
    }
}