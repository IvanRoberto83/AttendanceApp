package com.example.absenywm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class CreateAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_account)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etDepartment = findViewById<EditText>(R.id.etDepartment)
        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val etShift = findViewById<AutoCompleteTextView>(R.id.etShift)
        val shiftOptions = listOf("08:00 - 15:00", "15:00 - 22:00", "22:00 - 08:00")

        val adapter = object : ArrayAdapter<String>(this, R.layout.item_shift, shiftOptions) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val selectedText = etShift.text.toString()
                if (view.text == selectedText) {
                    view.setTextColor(ContextCompat.getColor(context, R.color.orange_bold))
                } else {
                    view.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                val selectedText = etShift.text.toString()
                if (view.text == selectedText) {
                    view.setTextColor(ContextCompat.getColor(context, R.color.orange_bold))
                } else {
                    view.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                return view
            }
        }

        etShift.setAdapter(adapter)

        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnCreate = findViewById<Button>(R.id.btnCreate)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnCreate.setOnClickListener {
            val username = etUsername.text.toString()
            val department = etDepartment.text.toString()
            val phoneNumber = etPhoneNumber.text.toString()
            val shift = etShift.text.toString()
            val password = etPassword.text.toString()

            if (username.isEmpty() || department.isEmpty() || phoneNumber.isEmpty() || shift.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap mengisi seluruh kolom", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Akun berhasil dibuat", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}