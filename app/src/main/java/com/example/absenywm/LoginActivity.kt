package com.example.absenywm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

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

            // Login ke Firebase Auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        val userId = auth.currentUser?.uid

                        if (userId == null) {
                            Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
                            return@addOnCompleteListener
                        }

                        // Ambil data dari Realtime Database
                        database.child(userId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {

                                override fun onDataChange(snapshot: DataSnapshot) {

                                    if (snapshot.exists()) {

                                        val username = snapshot.child("username").value.toString()
                                        val department = snapshot.child("department").value.toString()
                                        val phoneNumber = snapshot.child("phoneNumber").value.toString()
                                        val shift = snapshot.child("shift").value.toString()

                                        // Simpan ke SharedPreferences
                                        val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
                                        val editor = sharedPref.edit()

                                        editor.putBoolean("IS_LOGIN", true)
                                        editor.putString("USERNAME", username)
                                        editor.putString("EMAIL", email)
                                        editor.putString("DEPARTMENT", department)
                                        editor.putString("PHONENUM", phoneNumber)
                                        editor.putString("SHIFT", shift)

                                        editor.apply()

                                        Toast.makeText(this@LoginActivity, "Login berhasil", Toast.LENGTH_SHORT).show()

                                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                        finish()

                                    } else {
                                        Toast.makeText(this@LoginActivity, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(this@LoginActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                                }
                            })

                    } else {
                        val errorMessage = when {
                            task.exception?.message?.contains("badly formatted") == true ->
                                "Format email salah"
                            task.exception?.message?.contains("password is invalid") == true ->
                                "Password salah"
                            task.exception?.message?.contains("no user record") == true ->
                                "Email tidak terdaftar"
                            else -> "Login gagal"
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