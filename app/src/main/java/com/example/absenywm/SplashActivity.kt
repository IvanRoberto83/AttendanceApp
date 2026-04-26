package com.example.absenywm

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        autoDeleteOldPhotos()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
        val isLogin = sharedPref.getBoolean("IS_LOGIN", false)
        val savedRole = sharedPref.getString("ROLE", null)

        val user = auth.currentUser

        if (isLogin && user != null) {

            if (!savedRole.isNullOrEmpty()) {
                navigateByRole(savedRole)
            } else {
                db.collection("users")
                    .document(user.uid)
                    .get()
                    .addOnSuccessListener { doc ->

                        val role = doc.getString("role") ?: ""

                        sharedPref.edit().putString("ROLE", role).apply()

                        navigateByRole(role)
                    }
                    .addOnFailureListener {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
            }

        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun autoDeleteOldPhotos() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val threeMonthsAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)

        FirebaseFirestore.getInstance()
            .collection("absensi")
            .document(userId)
            .collection("records")
            .get()
            .addOnSuccessListener { result ->

                for (doc in result) {

                    val timestamp = doc.getLong("timestamp") ?: continue

                    if (timestamp < threeMonthsAgo) {

                        val publicId = doc.getString("public_id") ?: continue

                        try {
                            MediaManager.get().cloudinary
                                .uploader()
                                .destroy(publicId, mapOf("invalidate" to true))

                            doc.reference.delete()

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
    }

    private fun navigateByRole(role: String) {
        if (role == "Administrator") {
            startActivity(Intent(this, AdminActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}