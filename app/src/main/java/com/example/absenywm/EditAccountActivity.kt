package com.example.absenywm

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditAccountActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_account)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)

        val dtProfile = findViewById<TextView>(R.id.tvAvatarInitials)
        val dtEmail = findViewById<EditText>(R.id.dtEmail)
        val dtUsername = findViewById<EditText>(R.id.dtUsername)
        val dtPhoneNumber = findViewById<EditText>(R.id.dtPhoneNumber)
        val dtShift = findViewById<AutoCompleteTextView>(R.id.dtShift)
        val dtPassword = findViewById<EditText>(R.id.dtPassword)

        val email = sharedPref.getString("EMAIL", "") ?: ""
        val username = sharedPref.getString("USERNAME", "") ?: ""
        val phone = sharedPref.getString("PHONENUM", "") ?: ""
        val shift = sharedPref.getString("SHIFT", "") ?: ""

        dtProfile.text = username.firstOrNull()?.uppercase() ?: "?"
        dtEmail.setText(email)
        dtUsername.setText(username)
        dtPhoneNumber.setText(phone)
        dtShift.setText(shift, false)

        val shiftOptions = listOf("08:00 - 15:00", "15:00 - 22:00", "22:00 - 08:00")

        val adapter = object : ArrayAdapter<String>(this, R.layout.item_shift, shiftOptions) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val selectedText = dtShift.text.toString()
                view.setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (view.text == selectedText) R.color.orange_bold else R.color.black
                    )
                )
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                val selectedText = dtShift.text.toString()
                view.setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (view.text == selectedText) R.color.orange_bold else R.color.black
                    )
                )
                return view
            }
        }

        dtShift.setAdapter(adapter)

        val btnEdit = findViewById<Button>(R.id.btnEdit)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnEdit.setOnClickListener {

            val newEmail = dtEmail.text.toString().trim()
            val newUsername = dtUsername.text.toString().trim()
            val newPhone = dtPhoneNumber.text.toString().trim()
            val newShift = dtShift.text.toString().trim()
            val newPassword = dtPassword.text.toString().trim()

            if (newEmail.isEmpty() || newUsername.isEmpty() || newPhone.isEmpty() || newShift.isEmpty()) {
                Toast.makeText(this, "Harap mengisi seluruh kolom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid

            if (userId == null) {
                Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userMap = mapOf(
                "email" to newEmail,
                "username" to newUsername,
                "phoneNumber" to newPhone,
                "shift" to newShift
            )

            db.collection("users")
                .document(userId)
                .update(userMap)
                .addOnSuccessListener {

                    if (newPassword.isNotEmpty()) {
                        auth.currentUser?.updatePassword(newPassword)
                    }

                    sharedPref.edit()
                        .putString("EMAIL", newEmail)
                        .putString("USERNAME", newUsername)
                        .putString("PHONENUM", newPhone)
                        .putString("SHIFT", newShift)
                        .apply()

                    Toast.makeText(this, "Akun berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal update Firestore", Toast.LENGTH_SHORT).show()
                }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}