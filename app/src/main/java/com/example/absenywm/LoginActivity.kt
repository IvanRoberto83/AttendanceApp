package com.example.absenywm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnCreate = findViewById<Button>(R.id.btnCreateAccount)

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap mengisi seluruh kolom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        val userId = auth.currentUser?.uid

                        if (userId == null) {
                            Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }

                        db.collection("users")
                            .document(userId)
                            .get()
                            .addOnSuccessListener { document ->

                                if (document.exists()) {

                                    val username = document.getString("username") ?: ""
                                    val department = document.getString("department") ?: ""
                                    val phoneNumber = document.getString("phoneNumber") ?: ""
                                    val shift = document.getString("shift") ?: ""

                                    val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
                                    val editor = sharedPref.edit()

                                    editor.putBoolean("IS_LOGIN", true)
                                    editor.putString("USERNAME", username)
                                    editor.putString("EMAIL", email)
                                    editor.putString("DEPARTMENT", department)
                                    editor.putString("PHONENUM", phoneNumber)
                                    editor.putString("SHIFT", shift)

                                    editor.apply()

                                    Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()

                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()

                                } else {
                                    Toast.makeText(this, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                            }

                    } else {
                        val errorMessage = when (task.exception) {
                            is FirebaseAuthInvalidUserException -> "Email tidak terdaftar"
                            is FirebaseAuthInvalidCredentialsException -> "Email atau password salah"
                            else -> "Login gagal: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }

        btnCreate.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
            finish()
        }
    }
}