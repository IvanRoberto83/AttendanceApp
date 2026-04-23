package com.example.absenywm

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AbsenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_absen)

        val btnAbsen = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAbsen)
        val btnTutup = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnTutup)

        btnAbsen.setOnClickListener {
            Toast.makeText(this, "Absensi berhasil!", Toast.LENGTH_SHORT).show()
        }

        btnTutup.setOnClickListener {
            finish()
        }
    }
}