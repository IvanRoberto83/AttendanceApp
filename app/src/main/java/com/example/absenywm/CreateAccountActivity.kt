package com.example.absenywm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateAccountActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_account)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etDepartment = findViewById<EditText>(R.id.etDepartment)
        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val etShift = findViewById<AutoCompleteTextView>(R.id.etShift)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        val btnCreate = findViewById<Button>(R.id.btnCreate)
        val btnBack = findViewById<Button>(R.id.btnBack)

        val shiftOptions = listOf("08:00 - 15:00", "15:00 - 22:00", "22:00 - 08:00")

        val adapter = object : ArrayAdapter<String>(this, R.layout.item_shift, shiftOptions) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val selectedText = etShift.text.toString()
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
                val selectedText = etShift.text.toString()
                view.setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (view.text == selectedText) R.color.orange_bold else R.color.black
                    )
                )
                return view
            }
        }

        etShift.setAdapter(adapter)

        btnCreate.setOnClickListener {

            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val department = etDepartment.text.toString().trim()
            val phoneNumber = etPhoneNumber.text.toString().trim()
            val shift = etShift.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || department.isEmpty()
                || phoneNumber.isEmpty() || shift.isEmpty() || password.isEmpty()
            ) {
                Toast.makeText(this, "Harap mengisi seluruh kolom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnCreate.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        val userId = auth.currentUser?.uid

                        if (userId == null) {
                            Toast.makeText(this, "User ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                            btnCreate.isEnabled = true
                            return@addOnCompleteListener
                        }

                        val userMap = hashMapOf(
                            "username" to username,
                            "email" to email,
                            "department" to department,
                            "phoneNumber" to phoneNumber,
                            "shift" to shift
                        )

                        db.collection("users")
                            .document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Akun berhasil dibuat", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Gagal simpan ke Firestore", Toast.LENGTH_SHORT).show()
                                btnCreate.isEnabled = true
                            }

                    } else {
                        val errorMessage = when {
                            task.exception?.message?.contains("already in use") == true ->
                                "Email sudah terdaftar"
                            task.exception?.message?.contains("badly formatted") == true ->
                                "Format email salah"
                            else -> "Gagal membuat akun"
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        btnCreate.isEnabled = true
                    }
                }
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}