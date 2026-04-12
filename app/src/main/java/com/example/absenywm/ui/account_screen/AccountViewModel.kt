package com.example.absenywm.ui.account_screen

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

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

    fun deleteAccount(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            onError("User tidak ditemukan")
            return
        }

        val uid = user.uid

        val database = FirebaseDatabase.getInstance().getReference("users")

        database.child(uid).removeValue()
            .addOnSuccessListener {
                user.delete()
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener {
                        it.printStackTrace()
                        onError("Auth error: ${it.message}")
                    }
            }
            .addOnFailureListener {
                it.printStackTrace()
                onError("Database error: ${it.message}")
            }
    }
}