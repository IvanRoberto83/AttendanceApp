package com.example.absenywm

import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_account)

        val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)

        val dtProfile = findViewById<TextView>(R.id.tvAvatarInitials)
        val dtUsername = findViewById<EditText>(R.id.dtUsername)
        val dtDepartment = findViewById<EditText>(R.id.dtDepartment)
        val dtPhoneNumber = findViewById<EditText>(R.id.dtPhoneNumber)
        val dtShift = findViewById<AutoCompleteTextView>(R.id.dtShift)
        val etPassword = findViewById<EditText>(R.id.dtPassword)

        dtProfile.text = sharedPref.getString("USERNAME","")?.firstOrNull()?.uppercase()
        dtUsername.setText(sharedPref.getString("USERNAME", ""))
        dtDepartment.setText(sharedPref.getString("JABATAN", ""))
        dtPhoneNumber.setText(sharedPref.getString("PHONENUM", ""))
        dtShift.setText(sharedPref.getString("JAMKERJA", ""))

        val btnEdit = findViewById<Button>(R.id.btnEdit)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnEdit.setOnClickListener {
            val username = dtUsername.text.toString()
            val department = dtDepartment.text.toString()
            val phoneNumber = dtPhoneNumber.text.toString()
            val shift = dtShift.text.toString()
            val password = etPassword.text.toString()

            if(username.isEmpty() || department.isEmpty() || phoneNumber.isEmpty() || shift.isEmpty()) {
                Toast.makeText(this, "Harap mengisi seluruh kolom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sharedPref.edit()
                .putString("USERNAME", username)
                .putString("JABATAN", department)
                .putString("PHONENUM", phoneNumber)
                .putString("JAMKERJA", shift)
                .putString("PASSWORD", password)
                .apply()

            Toast.makeText(this, "Akun berhasil diedit", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}