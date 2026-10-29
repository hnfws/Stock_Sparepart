package com.example

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.data.PrefManager
import com.example.model.Sparepart
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class SparepartFormActivity : AppCompatActivity() {

    private lateinit var prefManager: PrefManager
    private var editingPartId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        prefManager = PrefManager(this)
        prefManager.applySavedTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sparepart_form)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        val root = findViewById<android.view.View>(R.id.formRoot)
        val appBarForm = findViewById<android.view.View>(R.id.appBarForm)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, 0, bars.right, bars.bottom)
            appBarForm.setPadding(0, bars.top, 0, 0)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarForm)
        toolbar.setNavigationOnClickListener { finish() }

        val etCode = findViewById<TextInputEditText>(R.id.etPartCode)
        val etName = findViewById<TextInputEditText>(R.id.etPartName)
        val etVehicle = findViewById<TextInputEditText>(R.id.etVehicleBrand)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerPartCategory)
        val rgQuality = findViewById<RadioGroup>(R.id.rgPartQuality)
        val rbOem = findViewById<RadioButton>(R.id.rbQualityOem)
        val rbAftermarket = findViewById<RadioButton>(R.id.rbQualityAftermarket)
        val rbRecondition = findViewById<RadioButton>(R.id.rbQualityRecondition)
        val cbWarranty = findViewById<CheckBox>(R.id.cbWarranty)
        val cbFastMoving = findViewById<CheckBox>(R.id.cbFastMoving)
        val etStock = findViewById<TextInputEditText>(R.id.etStockQty)
        val etMinStock = findViewById<TextInputEditText>(R.id.etMinStock)
        val etPrice = findViewById<TextInputEditText>(R.id.etUnitPrice)
        val etRack = findViewById<TextInputEditText>(R.id.etRackLocation)
        val etSupplierName = findViewById<TextInputEditText>(R.id.etSupplierName)
        val etSupplierPhone = findViewById<TextInputEditText>(R.id.etSupplierPhone)
        val etNotes = findViewById<TextInputEditText>(R.id.etNotes)
        val btnSave = findViewById<MaterialButton>(R.id.btnSaveSparepart)

        // Setup Spinner Kategori Suku Cadang
        val categories = resources.getStringArray(R.array.sparepart_categories)
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerCategory.adapter = spinnerAdapter

        // Cek apakah mode Edit melalui Intent Extra
        editingPartId = intent.getStringExtra(EXTRA_SPAREPART_ID)
        val existingPart = editingPartId?.let { prefManager.getSparepartById(it) }

        if (existingPart != null) {
            toolbar.title = getString(R.string.title_edit_sparepart)
            etCode.setText(existingPart.code)
            etName.setText(existingPart.name)
            etVehicle.setText(existingPart.vehicleBrand)

            val catIndex = categories.indexOfFirst { it == existingPart.category }
            if (catIndex >= 0) spinnerCategory.setSelection(catIndex)

            when (existingPart.qualityType) {
                getString(R.string.quality_aftermarket) -> rbAftermarket.isChecked = true
                getString(R.string.quality_recondition) -> rbRecondition.isChecked = true
                else -> rbOem.isChecked = true
            }

            cbWarranty.isChecked = existingPart.hasWarranty
            cbFastMoving.isChecked = existingPart.isFastMoving
            etStock.setText(existingPart.stock.toString())
            etMinStock.setText(existingPart.minStock.toString())
            etPrice.setText(existingPart.unitPrice.toString())
            etRack.setText(existingPart.rackLocation)
            etSupplierName.setText(existingPart.supplierName)
            etSupplierPhone.setText(existingPart.supplierPhone)
            etNotes.setText(existingPart.notes)
        }

        btnSave.setOnClickListener {
            val code = etCode.text?.toString()?.trim()?.uppercase().orEmpty()
            val name = etName.text?.toString()?.trim().orEmpty()
            val vehicle = etVehicle.text?.toString()?.trim().orEmpty()
            val category = spinnerCategory.selectedItem?.toString() ?: categories.first()

            val selectedQualityId = rgQuality.checkedRadioButtonId
            val quality = findViewById<RadioButton>(selectedQualityId)?.text?.toString()
                ?: getString(R.string.quality_oem)

            val stock = etStock.text?.toString()?.trim()?.toIntOrNull()
            val minStock = etMinStock.text?.toString()?.trim()?.toIntOrNull() ?: 5
            val price = etPrice.text?.toString()?.trim()?.toLongOrNull()
            val rack = etRack.text?.toString()?.trim().orEmpty().ifEmpty { "Rak Umum" }
            val supplierName = etSupplierName.text?.toString()?.trim().orEmpty().ifEmpty { "-" }
            val supplierPhone = etSupplierPhone.text?.toString()?.trim().orEmpty()
            val notes = etNotes.text?.toString()?.trim().orEmpty()

            if (code.isEmpty()) {
                etCode.error = "Kode SKU wajib diisi"
                etCode.requestFocus()
                return@setOnClickListener
            }
            if (name.isEmpty()) {
                etName.error = "Nama suku cadang wajib diisi"
                etName.requestFocus()
                return@setOnClickListener
            }
            if (vehicle.isEmpty()) {
                etVehicle.error = "Kompatibilitas kendaraan wajib diisi"
                etVehicle.requestFocus()
                return@setOnClickListener
            }
            if (stock == null || stock < 0) {
                etStock.error = "Masukkan jumlah stok yang valid"
                etStock.requestFocus()
                return@setOnClickListener
            }
            if (price == null || price < 0L) {
                etPrice.error = "Masukkan harga satuan yang valid"
                etPrice.requestFocus()
                return@setOnClickListener
            }

            val isNew = existingPart == null
            val partToSave = Sparepart(
                id = existingPart?.id ?: UUID.randomUUID().toString(),
                code = code,
                name = name,
                category = category,
                vehicleBrand = vehicle,
                qualityType = quality,
                hasWarranty = cbWarranty.isChecked,
                isFastMoving = cbFastMoving.isChecked,
                stock = stock,
                minStock = minStock.coerceAtLeast(0),
                unitPrice = price,
                rackLocation = rack,
                supplierName = supplierName,
                supplierPhone = supplierPhone,
                notes = notes,
                updatedAt = System.currentTimeMillis()
            )

            prefManager.addOrUpdateSparepart(partToSave, isNew = isNew)
            Toast.makeText(
                this,
                if (isNew) "Suku cadang baru berhasil ditambahkan" else "Perubahan suku cadang disimpan",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    companion object {
        const val EXTRA_SPAREPART_ID = "extra_sparepart_id"
    }
}
