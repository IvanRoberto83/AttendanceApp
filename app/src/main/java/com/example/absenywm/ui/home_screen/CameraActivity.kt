package com.example.absenywm.ui.home_screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.absenywm.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: MaterialButton
    private lateinit var btnBack: MaterialButton

    private var imageCapture: ImageCapture? = null

    private var status: String? = null
    private var tukarShift: Boolean = false
    private var shiftPengganti: String? = null
    private var keterangan: String? = null
    private var type: String? = null
    private var lat: Double = 0.0
    private var lng: Double = 0.0

    companion object {
        private const val REQUEST_CODE_CAMERA = 100
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_CAMERA) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startCamera() 
            } else {
                Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        btnCapture = findViewById(R.id.btnCapture)
        btnBack = findViewById(R.id.btnBack)

        status = intent.getStringExtra("status")
        tukarShift = intent.getBooleanExtra("tukarShift", false)
        shiftPengganti = intent.getStringExtra("shiftPengganti")
        keterangan = intent.getStringExtra("keterangan")
        type = intent.getStringExtra("type")
        lat = intent.getDoubleExtra("lat", 0.0)
        lng = intent.getDoubleExtra("lng", 0.0)

        setupUI()

        btnBack.setOnClickListener { finish() }
        btnCapture.setOnClickListener { takePhotoAndSubmit() }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_CAMERA
            )
        }
    }

    private fun setupUI() {
        val color = if (type == "masuk") {
            R.color.blue_bold
        } else {
            R.color.orange_bold
        }

        btnCapture.backgroundTintList =
            ContextCompat.getColorStateList(this, color)
        btnBack.setTextColor(ContextCompat.getColorStateList(this, color))
        btnBack.iconTint = ContextCompat.getColorStateList(this, color)

        btnBack.background = null
        btnBack.rippleColor = null
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture
            )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhotoAndSubmit() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            externalCacheDir,
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(photoFile)

                    Toast.makeText(this@CameraActivity, "Foto diambil", Toast.LENGTH_SHORT).show()

                    uploadToFirebase(uri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@CameraActivity, "Gagal ambil foto", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun uploadToFirebase(photoUri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("absensi/${userId}_${System.currentTimeMillis()}.jpg")

        fileRef.putFile(photoUri)
            .addOnSuccessListener {

                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->

                    val now = Date()
                    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
                    val timeNow = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)

                    val data = hashMapOf(
                        "status" to status,
                        "tukarShift" to tukarShift,
                        "shiftPengganti" to shiftPengganti,
                        "keterangan" to keterangan,
                        "type" to type,
                        "lat" to lat,
                        "lng" to lng,
                        "foto" to downloadUri.toString(),
                        "tanggal" to dateKey,
                        "waktu" to timeNow
                    )

                    FirebaseFirestore.getInstance()
                        .collection("absensi")
                        .document(userId)
                        .collection("records")
                        .document("${dateKey}_${type}")
                        .set(data)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Absen berhasil", Toast.LENGTH_SHORT).show()

                            finishAffinity()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Gagal simpan", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload gagal", Toast.LENGTH_SHORT).show()
            }
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}