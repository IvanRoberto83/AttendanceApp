package com.example.absenywm.ui.home_screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import com.example.absenywm.TimeUtils
import androidx.core.content.ContextCompat
import com.example.absenywm.MainActivity
import com.example.absenywm.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: MaterialButton
    private lateinit var btnBack: MaterialButton

    private var imageCapture: ImageCapture? = null
    private var isCameraStarted = false

    private var status: String? = null
    private var tukarShift: Boolean = false
    private var shiftPengganti: String? = null
    private var keterangan: String? = null
    private var type: String? = null
    private var lat: Double = 0.0
    private var lng: Double = 0.0

    private var shiftAsli: String? = null

    companion object {
        private const val REQUEST_CODE_CAMERA = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        btnCapture = findViewById(R.id.btnCapture)
        btnBack = findViewById(R.id.btnBack)

        status = intent.getStringExtra("status")
        shiftAsli = intent.getStringExtra("shiftAsli")
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

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted() && !isCameraStarted) {
            startCamera()
        }
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

    private fun getShiftStart(): String {
        return if (tukarShift && !shiftPengganti.isNullOrEmpty()) {
            shiftPengganti!!.split(" - ")[0]
        } else {
            TimeUtils.extractStartTime(shiftAsli)
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
        if (isCameraStarted) return

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

            isCameraStarted = true

        }, ContextCompat.getMainExecutor(this))
    }

    private fun getServerTimeAndSubmit(photoFile: File) {
        val tempRef = FirebaseFirestore.getInstance()
            .collection("serverTime")
            .document("temp")

        val tempData = hashMapOf(
            "t" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        tempRef.set(tempData)
            .addOnSuccessListener {
                tempRef.get()
                    .addOnSuccessListener { snap ->

                        val serverTime = snap.getTimestamp("t")?.toDate() ?: Date()

                        val isValid = TimeUtils.isWithinAbsenTime(serverTime)

                        if (!isValid) {
                            btnCapture.isEnabled = true
                            Toast.makeText(this, "Diluar jam absen", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        uploadToCloudinary(photoFile, serverTime)
                    }
            }
            .addOnFailureListener {
                btnCapture.isEnabled = true
                Toast.makeText(this, "Gagal cek waktu server", Toast.LENGTH_SHORT).show()
            }
    }

    private fun takePhotoAndSubmit() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(
            externalCacheDir,
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        btnCapture.isEnabled = false

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val compressedFile = compressImage(photoFile)
                    getServerTimeAndSubmit(compressedFile)
                }
                override fun onError(exception: ImageCaptureException) {
                    btnCapture.isEnabled = true
                    Toast.makeText(this@CameraActivity, "Gagal ambil foto", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun compressImage(file: File): File {
        val bitmap = android.graphics.BitmapFactory.decodeFile(file.path)

        val maxWidth = 720
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()

        val width: Int
        val height: Int

        if (bitmap.width > bitmap.height) {
            width = maxWidth
            height = (maxWidth / ratio).toInt()
        } else {
            height = maxWidth
            width = (maxWidth * ratio).toInt()
        }

        val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)

        val compressedFile = File(file.parent, "compressed_${file.name}")

        val out = java.io.FileOutputStream(compressedFile)
        resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 65, out)
        out.flush()
        out.close()

        return compressedFile
    }

    private fun uploadToCloudinary(photoFile: File, serverTime: Date) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(serverTime)
        val timeNow = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(serverTime)

        MediaManager.get().upload(photoFile.path)
            .unsigned("WredhaMulya")
            .option("folder", "absensiKaryawan")
            .option("quality", "auto:low")
            .option("fetch_format", "auto")
            .callback(object : UploadCallback {

                override fun onStart(requestId: String?) {}

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {

                    val imageUrl = resultData?.get("secure_url")?.toString()
                    val publicId = resultData?.get("public_id")?.toString()

                    val shiftStart = getShiftStart()

                    val finalStatus = if (status == "Hadir") {
                        if (TimeUtils.isLate(shiftStart, serverTime)) "Telat" else "Hadir"
                    } else {
                        status ?: "Hadir"
                    }

                    val data = hashMapOf(
                        "status" to finalStatus,
                        "tukarShift" to tukarShift,
                        "shiftPengganti" to shiftPengganti,
                        "keterangan" to keterangan,
                        "type" to type,
                        "lat" to lat,
                        "lng" to lng,
                        "foto" to imageUrl,
                        "public_id" to publicId,
                        "tanggal" to dateKey,
                        "waktu" to timeNow,
                        "server_timestamp" to serverTime.time
                    )

                    FirebaseFirestore.getInstance()
                        .collection("absensi")
                        .document(userId)
                        .collection("records")
                        .document("${dateKey}_${type}")
                        .set(data)
                        .addOnSuccessListener {

                            Toast.makeText(this@CameraActivity, "Absen berhasil", Toast.LENGTH_SHORT).show()

                            startActivity(Intent(this@CameraActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            })

                            finish()
                        }
                        .addOnFailureListener {
                            btnCapture.isEnabled = true
                            Toast.makeText(this@CameraActivity, "Gagal simpan", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    btnCapture.isEnabled = true
                    Toast.makeText(
                        this@CameraActivity,
                        "Upload gagal: ${error?.description}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        imageCapture = null
        isCameraStarted = false
    }
}