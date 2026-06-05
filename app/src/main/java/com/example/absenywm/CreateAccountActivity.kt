package com.example.absenywm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
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
        val etRole = findViewById<AutoCompleteTextView>(R.id.etRole)
        val etPhoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        val etShift = findViewById<AutoCompleteTextView>(R.id.etShift)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        val etAdminCode = findViewById<EditText>(R.id.etAdminCode)

        val btnCreate = findViewById<Button>(R.id.btnCreate)
        val btnBack = findViewById<Button>(R.id.btnBack)

        val roleOptions = listOf("Administrator", "Karyawan")

        val adapter1 = object : ArrayAdapter<String>(this, R.layout.item_shift, roleOptions) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val selectedText = etRole.text.toString()
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
                val selectedText = etRole.text.toString()
                view.setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (view.text == selectedText) R.color.orange_bold else R.color.black
                    )
                )
                return view
            }
        }

        etRole.setAdapter(adapter1)

        val shiftOptions = listOf("08:00 - 15:00", "15:00 - 22:00", "22:00 - 08:00")

        val adapter2 = object : ArrayAdapter<String>(this, R.layout.item_shift, shiftOptions) {
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

        etShift.setAdapter(adapter2)

        val layoutAdminCode = findViewById<View>(R.id.layoutAdminCode)
        val tvAdminCodeLabel = findViewById<View>(R.id.tvAdminCodeLabel)
        val tvJamKerjaLabel = findViewById<View>(R.id.tvJamKerjaLabel)
        val tvDropDownIcon = findViewById<TextInputLayout>(R.id.tvDropDownIcon)

        etRole.setOnItemClickListener { _, _, position, _ ->
            val selectedRole = roleOptions[position]

            if (selectedRole == "Administrator") {
                layoutAdminCode.visibility = View.VISIBLE
                tvAdminCodeLabel.visibility = View.VISIBLE
                tvJamKerjaLabel.visibility = View.GONE
                tvDropDownIcon.visibility = View.GONE

                etShift.setText("")
                etShift.visibility = View.GONE

            } else {
                layoutAdminCode.visibility = View.GONE
                tvAdminCodeLabel.visibility = View.GONE
                tvJamKerjaLabel.visibility = View.VISIBLE
                tvDropDownIcon.visibility = View.VISIBLE

                etShift.visibility = View.VISIBLE
            }
        }

        btnCreate.setOnClickListener {

            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val role = etRole.text.toString().trim()
            val phoneNumber = etPhoneNumber.text.toString().trim()
            val shift = etShift.text.toString().trim()
            val password = etPassword.text.toString().trim()

            val adminCodeInput = etAdminCode.text.toString().trim()

            if (role == "Administrator" && adminCodeInput.isEmpty()) {
                Toast.makeText(this, "Kode admin wajib diisi", Toast.LENGTH_SHORT).show()
                btnCreate.isEnabled = true
                return@setOnClickListener
            }

            if (username.isEmpty() || email.isEmpty() || role.isEmpty()
                || phoneNumber.isEmpty() || password.isEmpty()
                || (role != "Administrator" && shift.isEmpty())
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

            if (role == "Administrator") {

                db.collection("config")
                    .document("admin")
                    .get()
                    .addOnSuccessListener { doc ->

                        val realCode = doc.getString("code")

                        if (adminCodeInput != realCode) {
                            Toast.makeText(this, "Kode admin salah", Toast.LENGTH_SHORT).show()
                            btnCreate.isEnabled = true
                            return@addOnSuccessListener
                        }

                        createUser(username, email, role, phoneNumber, shift, password)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal cek kode admin", Toast.LENGTH_SHORT).show()
                        btnCreate.isEnabled = true
                    }

            } else {
                createUser(username, email, role, phoneNumber, shift, password)
            }
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun createUser(
        username: String,
        email: String,
        role: String,
        phoneNumber: String,
        shift: String,
        password: String
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val userId = auth.currentUser?.uid

                    if (userId == null) return@addOnCompleteListener

                    val userMap = hashMapOf(
                        "username" to username,
                        "email" to email,
                        "role" to role,
                        "phoneNumber" to phoneNumber,
                        "shift" to if (role == "Administrator") null else shift
                    )

                    db.collection("users")
                        .document(userId)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Akun berhasil dibuat", Toast.LENGTH_SHORT).show()
                            auth.signOut()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal simpan ke Firestore", Toast.LENGTH_SHORT).show()
                        }

                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Gagal membuat akun",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}