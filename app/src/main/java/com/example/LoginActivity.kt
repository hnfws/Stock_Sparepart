package com.example

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.data.PrefManager

/**
 * LoginActivity mengelola autentikasi pengguna dengan antarmuka terkini.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var prefManager: PrefManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        prefManager = PrefManager(this)
        prefManager.applySavedTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val isDark = prefManager.isDarkMode()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        val root = findViewById<View>(R.id.main)
        root?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }

        val etUsername = findViewById<EditText>(R.id.et_operator_id)
        val etPassword = findViewById<EditText>(R.id.et_master_pin)
        val btnTogglePassword = findViewById<ImageView>(R.id.btn_toggle_password)
        val btnLogin = findViewById<View>(R.id.btn_login)
        val btnForgotKey = findViewById<TextView>(R.id.btn_forgot_key)
        val btnSettings = findViewById<ImageView>(R.id.btn_settings)

        val tvTitle = findViewById<View>(R.id.tv_title)
        val labelOperatorId = findViewById<View>(R.id.label_operator_id)
        val containerOperatorId = findViewById<View>(R.id.container_operator_id)
        val labelMasterPin = findViewById<View>(R.id.label_master_pin)
        val containerMasterPin = findViewById<View>(R.id.container_master_pin)

        // Fade in login form fields smoothly
        listOfNotNull(tvTitle, labelOperatorId, containerOperatorId, labelMasterPin, containerMasterPin, btnForgotKey, btnLogin, btnSettings).forEach { view ->
            view.alpha = 0f
            view.animate().alpha(1f).setDuration(280).start()
        }

        // Pre-fill username dari SharedPreferences jika tersedia
        val savedUsername = prefManager.getSavedUsername()
        if (savedUsername.isNotBlank() && savedUsername != "Belum ada data") {
            etUsername?.setText(savedUsername)
        }

        // Toggle visibilitas kata sandi / PIN
        btnTogglePassword?.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword?.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                etPassword?.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            etPassword?.text?.let { text -> etPassword.setSelection(text.length) }
        }

        // Tombol Login
        btnLogin?.setOnClickListener {
            val username = etUsername?.text?.toString()?.trim().orEmpty()
            val password = etPassword?.text?.toString().orEmpty()

            if (username.isEmpty()) {
                etUsername?.error = "Masukkan Operator ID / Nama Pengguna"
                etUsername?.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword?.error = "Masukkan Master PIN / Kata Sandi"
                etPassword?.requestFocus()
                return@setOnClickListener
            }

            if (prefManager.validateCredentials(username, password)) {
                prefManager.saveLoginSession(
                    name = username,
                    role = "Operator",
                    rememberUsername = true
                )

                Toast.makeText(
                    this,
                    "Selamat datang, $username",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Operator ID atau Master PIN tidak sesuai",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Action tambahan
        btnForgotKey?.setOnClickListener {
            Toast.makeText(this, "Silakan hubungi administrator gudang untuk reset PIN", Toast.LENGTH_SHORT).show()
        }

        btnSettings?.setOnClickListener {
            Toast.makeText(this, "Pengaturan Akses Gudang", Toast.LENGTH_SHORT).show()
        }
    }
}
