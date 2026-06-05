package com.example.absenywm

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.absenywm.databinding.ActivityAdminMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("USER_SESSION", MODE_PRIVATE)
        val isLogin = sharedPref.getBoolean("IS_LOGIN", false)

        if (!isLogin) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_admin)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_admin_home,
                R.id.navigation_admin_list,
                R.id.navigation_account
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val title = destination.label?.toString() ?: ""

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }

            val imageView = ImageView(this).apply {
                setImageResource(R.drawable.icon)
                layoutParams = LinearLayout.LayoutParams(100, 100)
            }

            val textView = TextView(this).apply {
                text = title
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setPadding(25, 0, 0, 0)
            }

            layout.addView(imageView)
            layout.addView(textView)

            supportActionBar?.apply {
                displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
                customView = layout
            }
        }
    }
}