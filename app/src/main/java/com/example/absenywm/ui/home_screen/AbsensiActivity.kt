package com.example.absenywm.ui.home_screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.absenywm.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import com.google.android.gms.location.*
import com.google.android.gms.common.api.ResolvableApiException
import android.content.IntentSender
import android.location.LocationManager
import java.text.SimpleDateFormat
import java.util.*

class AbsensiActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()

    private val LOCATION_PERMISSION_CODE = 101
    private val GPS_REQUEST_CODE = 1001

    private lateinit var locationCallback: LocationCallback

    private val officeLat = -7.772596552088771
    private val officeLng = 110.37066349571364
    private val radiusMeters = 200.0

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var tvTitle: TextView
    private lateinit var tvDistance: TextView

    private lateinit var btnHadir: MaterialButton
    private lateinit var btnSakit: MaterialButton
    private lateinit var btnIzin: MaterialButton
    private lateinit var btnTukarYa: MaterialButton
    private lateinit var btnTukarTidak: MaterialButton

    private lateinit var layoutShift: TextInputLayout
    private lateinit var dropdownShift: AutoCompleteTextView

    private val allShifts = listOf(
        "08:00 - 15:00",
        "15:00 - 22:00",
        "22:00 - 08:00"
    )

    private var userShift = "08:00 - 15:00"

    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var etKeterangan: TextInputEditText

    private var selectedStatus = "Hadir"
    private var tukarShift = false
    private var absenType = "masuk"
    private var userLatLng: GeoPoint? = null

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            checkGPS()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                checkGPS()
            } else {
                Toast.makeText(this, "Izin lokasi diperlukan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkGPS() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this, GPS_REQUEST_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "GPS tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GPS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "GPS harus aktif", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                userLatLng = GeoPoint(location.latitude, location.longitude)

                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    officeLat, officeLng,
                    results
                )

                val distanceM = results[0].toInt()
                tvDistance.text = "Jarak anda ke kantor sekitar $distanceM Meter"

                updateUserMarker()
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private var userMarker: Marker? = null

    private fun updateUserMarker() {
        userLatLng?.let {

            if (userMarker == null) {
                userMarker = Marker(mapView)
                userMarker!!.infoWindow = null

                val drawable = ContextCompat.getDrawable(this, R.drawable.baseline_man_24)
                if (absenType == "masuk") {
                    drawable?.setTint(ContextCompat.getColor(this, R.color.blue_bold))
                } else {
                    drawable?.setTint(ContextCompat.getColor(this, R.color.orange_bold))
                }

                userMarker!!.icon = drawable
                mapView.overlays.add(userMarker)
            }

            userMarker!!.position = it

            mapView.controller.animateTo(it)
            mapView.invalidate()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()

        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sheet_absensi)

        absenType = intent.getStringExtra("type") ?: "masuk"

        Configuration.getInstance().userAgentValue = packageName

        initViews()
        getUserShift()
        setupTypeUI()
        setupDefaultState()
        setupMap()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkLocationPermission()

        setupStatusButtons()
        setupTukarShiftButtons()
        dropdownShift.setOnClickListener {
            dropdownShift.showDropDown()
        }

        btnSubmit.setOnClickListener { goToCamera() }
        btnBack.setOnClickListener { finish() }
    }

    private fun initViews() {
        tvTitle = findViewById(R.id.tvTitle)
        tvDistance = findViewById(R.id.tvDistance)

        btnHadir = findViewById(R.id.btnHadir)
        btnSakit = findViewById(R.id.btnSakit)
        btnIzin = findViewById(R.id.btnIzin)

        btnTukarYa = findViewById(R.id.btnTukarYa)
        btnTukarTidak = findViewById(R.id.btnTukarTidak)

        layoutShift = findViewById(R.id.layoutShift)
        dropdownShift = findViewById(R.id.dropdownShift)

        btnSubmit = findViewById(R.id.btnSubmitAbsen)
        btnBack = findViewById(R.id.btnBack)
        etKeterangan = findViewById(R.id.etKeterangan)

        mapView = findViewById(R.id.mapView)
    }

    @SuppressLint("ResourceAsColor")
    private fun setupTypeUI() {
        if (absenType == "masuk") {
            tvTitle.text = "Absensi Masuk"
            tvTitle.setTextColor(getColor(R.color.blue_bold))

            btnSubmit.text = "Selanjutnya"
            btnSubmit.setBackgroundColor(getColor(R.color.blue_bold))

            btnBack.setTextColor(getColor(R.color.blue_bold))
            btnBack.setStrokeColorResource(R.color.blue_bold)
        } else {
            tvTitle.text = "Absensi Keluar"
            tvTitle.setTextColor(getColor(R.color.orange_bold))

            btnSubmit.text = "Selanjutnya"
            btnSubmit.setBackgroundColor(getColor(R.color.orange_bold))

            btnBack.setTextColor(getColor(R.color.orange_bold))
            btnBack.setStrokeColorResource(R.color.orange_bold)
        }
    }

    private fun setupDefaultState() {
        listOf(btnHadir, btnSakit, btnIzin).forEach { setUnselected(it) }
        listOf(btnTukarYa, btnTukarTidak).forEach { setUnselected(it) }

        setSelected(btnHadir)
        setSelected(btnTukarTidak)
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(false)
        mapView.isClickable = false

        val officePoint = GeoPoint(officeLat, officeLng)

        mapView.controller.setZoom(16.0)
        mapView.controller.setCenter(officePoint)

        val marker = Marker(mapView)
        marker.position = officePoint

        marker.infoWindow = null

        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        val drawable = ContextCompat.getDrawable(this, R.drawable.baseline_location_pin_24)
        if (absenType == "masuk") {
            drawable?.setTint(ContextCompat.getColor(this, R.color.blue_bold))
        } else {
            drawable?.setTint(ContextCompat.getColor(this, R.color.orange_bold))
        }

        drawable?.setBounds(0, 0, 80, 80)

        marker.icon = drawable

        mapView.overlays.add(marker)

        val circlePoints = ArrayList<GeoPoint>()
        for (i in 0 until 360) {
            val angle = Math.toRadians(i.toDouble())
            val lat = officeLat + (radiusMeters / 111320) * Math.cos(angle)
            val lng = officeLng + (radiusMeters / (111320 * Math.cos(Math.toRadians(officeLat)))) * Math.sin(angle)
            circlePoints.add(GeoPoint(lat, lng))
        }

        val circle = Polygon(mapView)
        circle.points = circlePoints
        circle.infoWindow = null

        if (absenType == "masuk") {
            circle.fillColor = 0x220000FF
            circle.strokeColor = 0xFF0000FF.toInt()
        } else {
            circle.fillColor = 0x22FF0000
            circle.strokeColor = 0xFFFF0000.toInt()
        }

        circle.strokeWidth = 2f
        mapView.overlays.add(circle)

        mapView.invalidate()
    }

    private fun setupStatusButtons() {
        val buttons = listOf(btnHadir, btnSakit, btnIzin)

        buttons.forEach { button ->
            button.setOnClickListener {
                selectedStatus = button.text.toString()

                buttons.forEach { setUnselected(it) }
                setSelected(button)
            }
        }

        setSelected(btnHadir)
    }

    private fun getUserShift() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    userShift = document.getString("shift") ?: "08.00 - 15.00"

                    setupShiftDropdown()
                } else {
                    Toast.makeText(this, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil shift", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupTukarShiftButtons() {
        val buttons = listOf(btnTukarYa, btnTukarTidak)

        buttons.forEach { button ->
            button.setOnClickListener {
                tukarShift = button.text == "Ya"

                buttons.forEach { setUnselected(it) }
                setSelected(button)

                if (tukarShift) {
                    layoutShift.visibility = View.VISIBLE
                    setupShiftDropdown()
                } else {
                    layoutShift.visibility = View.GONE
                }
            }
        }

        setSelected(btnTukarTidak)
        layoutShift.visibility = View.GONE
    }

    private fun setSelected(btn: MaterialButton) {
        if (absenType == "masuk") {
            btn.setTextColor(getColor(android.R.color.white))
            btn.setBackgroundColor(getColor(R.color.blue_bold))
            btn.strokeWidth = 0
        } else {
            btn.setTextColor(getColor(android.R.color.white))
            btn.setBackgroundColor(getColor(R.color.orange_bold))
            btn.strokeWidth = 0
        }
    }

    private fun setUnselected(btn: MaterialButton) {
        if (absenType == "masuk") {
            btn.setTextColor(getColor(R.color.blue_bold))
            btn.setBackgroundColor(android.graphics.Color.parseColor("#EEF3FF"))
            btn.strokeWidth = 2
            btn.strokeColor = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#B5C8F9")
            )
        } else {
            btn.setTextColor(getColor(R.color.orange_bold))
            btn.setBackgroundColor(android.graphics.Color.parseColor("#FFF0E9"))
            btn.strokeWidth = 2
            btn.strokeColor = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#FAC9AC")
            )
        }
    }

    private fun setupShiftDropdown() {
        val availableShifts = allShifts.filter { it != userShift }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            availableShifts
        )

        dropdownShift.setAdapter(adapter)

        dropdownShift.setOnClickListener {
            dropdownShift.showDropDown()
        }
    }

    private fun goToCamera() {
        val keterangan = etKeterangan.text.toString().trim()

        val selectedShift = if (tukarShift) {
            dropdownShift.text.toString()
        } else {
            null
        }

        val intent = Intent(this, CameraActivity::class.java)

        intent.putExtra("status", selectedStatus)
        intent.putExtra("tukarShift", tukarShift)
        intent.putExtra("shiftPengganti", selectedShift)
        intent.putExtra("keterangan", keterangan)
        intent.putExtra("type", absenType)
        intent.putExtra("lat", userLatLng?.latitude ?: 0.0)
        intent.putExtra("lng", userLatLng?.longitude ?: 0.0)

        startActivity(intent)
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onDestroy() { super.onDestroy(); mapView.onDetach() }
}
