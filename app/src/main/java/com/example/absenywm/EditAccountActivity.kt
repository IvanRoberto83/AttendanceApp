package com.example.absenywm

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

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
        val dtPassword = findViewById<EditText>(R.id.dtPassword)

        dtProfile.text = sharedPref.getString("USERNAME","")?.firstOrNull()?.uppercase()
        dtUsername.setText(sharedPref.getString("USERNAME", ""))
        dtDepartment.setText(sharedPref.getString("JABATAN", ""))
        dtPhoneNumber.setText(sharedPref.getString("PHONENUM", ""))
        dtShift.setText(sharedPref.getString("JAMKERJA", ""))

        val shiftOptions = listOf("08:00 - 15:00", "15:00 - 22:00", "22:00 - 08:00")

        val adapter = object : ArrayAdapter<String>(this, R.layout.item_shift, shiftOptions) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val selectedText = dtShift.text.toString()
                if (view.text == selectedText) {
                    view.setTextColor(ContextCompat.getColor(context, R.color.orange_bold))
                } else {
                    view.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                val selectedText = dtShift.text.toString()
                if (view.text == selectedText) {
                    view.setTextColor(ContextCompat.getColor(context, R.color.orange_bold))
                } else {
                    view.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                return view
            }
        }

        dtShift.setAdapter(adapter)

        dtPassword.setText(sharedPref.getString("PASSWORD", ""))

        val btnEdit = findViewById<Button>(R.id.btnEdit)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnEdit.setOnClickListener {
            val username = dtUsername.text.toString()
            val department = dtDepartment.text.toString()
            val phoneNumber = dtPhoneNumber.text.toString()
            val shift = dtShift.text.toString()
            val password = dtPassword.text.toString()

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
            setResult(RESULT_OK)
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}