package com.example.absenywm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnCreate = findViewById<Button>(R.id.btnCreateAccount)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()
            val password = etPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap mengisi seluruh kolom", Toast.LENGTH_SHORT).show()
            }
            else if (username == "admin" && password == "admin") {
                val id = "YWM-001"
                val departemen = "Staff Operasional"
                val noTelp = "08123456789"
                val jamKerja = "08:00 - 15:00"

                val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putBoolean("IS_LOGIN", true)

                editor.putString("USERNAME", username)
                editor.putString("JABATAN", departemen)
                editor.putString("IDKARYAWAN", id)
                editor.putString("PHONENUM", noTelp)
                editor.putString("JAMKERJA", jamKerja)
                editor.putString("PASSWORD", password)
                editor.apply()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            else {
                Toast.makeText(this, "Nama atau Password salah", Toast.LENGTH_SHORT).show()
            }
        }

        btnCreate.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
            finish()
        }
    }
}