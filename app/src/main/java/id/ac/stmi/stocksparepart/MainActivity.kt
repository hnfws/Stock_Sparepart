package id.ac.stmi.stocksparepart

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etMasterPin = findViewById<EditText>(R.id.et_master_pin)
        val btnTogglePassword = findViewById<ImageView>(R.id.btn_toggle_password)

        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etMasterPin.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                etMasterPin.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.setImageResource(R.drawable.ic_eye)
            }
            etMasterPin.setSelection(etMasterPin.text.length)
        }

        findViewById<ImageView>(R.id.btn_settings).setOnClickListener {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btn_forgot_key).setOnClickListener {
            Toast.makeText(this, "Forgot security key clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btn_login).setOnClickListener {
            val operatorId = findViewById<EditText>(R.id.et_operator_id).text.toString()
            Toast.makeText(this, "Logging in as $operatorId...", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btn_scan_badge).setOnClickListener {
            Toast.makeText(this, "Scan Badge clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btn_biometric_id).setOnClickListener {
            Toast.makeText(this, "Biometric ID clicked", Toast.LENGTH_SHORT).show()
        }
    }
}
