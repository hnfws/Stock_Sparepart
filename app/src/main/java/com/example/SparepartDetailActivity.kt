package com.example

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.adapter.StockMutationAdapter
import com.example.data.PrefManager
import com.example.model.CurrencyHelper
import com.example.model.MutationType
import com.example.model.Sparepart
import com.example.model.StockStatus
import com.example.util.StockDialogHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SparepartDetailActivity : AppCompatActivity() {

    private lateinit var prefManager: PrefManager
    private lateinit var mutationAdapter: StockMutationAdapter
    private lateinit var headerView: View
    private var sparepartId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        prefManager = PrefManager(this)
        prefManager.applySavedTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sparepart_detail)

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        val root = findViewById<View>(R.id.detailRoot)
        val appBarDetail = findViewById<View>(R.id.appBarDetail)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, 0, bars.right, bars.bottom)
            appBarDetail.setPadding(0, bars.top, 0, 0)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarDetail)
        toolbar.setNavigationOnClickListener { finish() }

        sparepartId = intent.getStringExtra(EXTRA_SPAREPART_ID).orEmpty()
        if (sparepartId.isEmpty()) {
            finish()
            return
        }

        val lvMutations = findViewById<ListView>(R.id.lvPartMutations)
        headerView = LayoutInflater.from(this).inflate(R.layout.header_sparepart_detail, lvMutations, false)
        lvMutations.addHeaderView(headerView, null, false)

        mutationAdapter = StockMutationAdapter(this, emptyList())
        lvMutations.adapter = mutationAdapter

        refreshDetailUi()
    }

    override fun onResume() {
        super.onResume()
        refreshDetailUi()
    }

    private fun refreshDetailUi() {
        val part = prefManager.getSparepartById(sparepartId)
        if (part == null) {
            finish()
            return
        }

        bindHeader(part)
        val history = prefManager.getMutationsForPart(part.id)
        mutationAdapter.updateData(history)
    }

    private fun bindHeader(part: Sparepart) {
        val tvSku = headerView.findViewById<TextView>(R.id.tvDetailSku)
        val tvStatusBadge = headerView.findViewById<TextView>(R.id.tvDetailStatusBadge)
        val tvName = headerView.findViewById<TextView>(R.id.tvDetailName)
        val tvVehicleCategory = headerView.findViewById<TextView>(R.id.tvDetailVehicleCategory)
        val tvStockQty = headerView.findViewById<TextView>(R.id.tvDetailStockQty)
        val tvPrice = headerView.findViewById<TextView>(R.id.tvDetailPrice)

        val tvSpecQuality = headerView.findViewById<TextView>(R.id.tvSpecQuality)
        val tvSpecRack = headerView.findViewById<TextView>(R.id.tvSpecRack)
        val tvSpecMinStock = headerView.findViewById<TextView>(R.id.tvSpecMinStock)
        val tvSpecTotalValue = headerView.findViewById<TextView>(R.id.tvSpecTotalValue)
        val tvSpecBadges = headerView.findViewById<TextView>(R.id.tvSpecBadges)
        val tvSpecNotes = headerView.findViewById<TextView>(R.id.tvSpecNotes)

        val tvSupplierName = headerView.findViewById<TextView>(R.id.tvDetailSupplierName)
        val tvSupplierPhone = headerView.findViewById<TextView>(R.id.tvDetailSupplierPhone)

        val btnStockIn = headerView.findViewById<MaterialButton>(R.id.btnDetailStockIn)
        val btnStockOut = headerView.findViewById<MaterialButton>(R.id.btnDetailStockOut)
        val btnCallSupplier = headerView.findViewById<MaterialButton>(R.id.btnCallSupplier)
        val btnSharePo = headerView.findViewById<MaterialButton>(R.id.btnSharePo)
        val btnEdit = headerView.findViewById<MaterialButton>(R.id.btnEditPartDetail)
        val btnDelete = headerView.findViewById<MaterialButton>(R.id.btnDeletePartDetail)

        tvSku.text = "SKU: ${part.code}"
        tvName.text = part.name
        tvVehicleCategory.text = "${part.category} • ${part.vehicleBrand}"
        tvStockQty.text = "${part.stock} Unit"
        tvPrice.text = CurrencyHelper.formatRupiah(part.unitPrice)

        val (statusLabel, bgRes, textRes) = when (part.stockStatus) {
            StockStatus.EMPTY -> Triple("STOK HABIS", R.color.status_empty_bg, R.color.status_empty_text)
            StockStatus.LOW -> Triple("STOK MENIPIS", R.color.status_low_bg, R.color.status_low_text)
            StockStatus.SAFE -> Triple("STOK AMAN", R.color.status_safe_bg, R.color.status_safe_text)
        }
        tvStatusBadge.text = statusLabel
        tvStatusBadge.setTextColor(ContextCompat.getColor(this, textRes))
        tvStatusBadge.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(ContextCompat.getColor(this@SparepartDetailActivity, bgRes))
        }

        tvSpecQuality.text = part.qualityType
        tvSpecRack.text = part.rackLocation
        tvSpecMinStock.text = "${part.minStock} Unit"
        tvSpecTotalValue.text = CurrencyHelper.formatRupiah(part.totalValue)

        val garantieText = if (part.hasWarranty) "Garansi Resmi" else "Non-Garansi"
        val priorityText = if (part.isFastMoving) "Prioritas Fast Moving" else "Rotasi Reguler"
        tvSpecBadges.text = "$garantieText  •  $priorityText"

        if (part.notes.isNotBlank()) {
            tvSpecNotes.visibility = View.VISIBLE
            tvSpecNotes.text = "Catatan Teknis: ${part.notes}"
        } else {
            tvSpecNotes.visibility = View.GONE
        }

        tvSupplierName.text = part.supplierName
        tvSupplierPhone.text = if (part.supplierPhone.isNotBlank()) {
            "Kontak: ${part.supplierPhone}"
        } else {
            "Nomor telepon belum dicantumkan"
        }

        btnStockIn.setOnClickListener {
            StockDialogHelper.showMutationDialog(this, prefManager, part, MutationType.IN) {
                refreshDetailUi()
            }
        }

        btnStockOut.setOnClickListener {
            StockDialogHelper.showMutationDialog(this, prefManager, part, MutationType.OUT) {
                refreshDetailUi()
            }
        }

        btnCallSupplier.setOnClickListener {
            if (part.supplierPhone.isBlank()) {
                Toast.makeText(this, "Nomor telepon supplier belum tersedia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${part.supplierPhone}")
            }
            startActivity(dialIntent)
        }

        btnSharePo.setOnClickListener {
            val reportText = buildString {
                appendLine("LAPORAN STOK SUKU CADANG - STOCK SPAREPART")
                appendLine("-----------------------------------------")
                appendLine("Kode SKU   : ${part.code}")
                appendLine("Nama Part  : ${part.name}")
                appendLine("Kendaraan  : ${part.vehicleBrand}")
                appendLine("Kategori   : ${part.category} (${part.qualityType})")
                appendLine("Stok Sisa  : ${part.stock} Unit (Min: ${part.minStock})")
                appendLine("Harga/Unit : ${CurrencyHelper.formatRupiah(part.unitPrice)}")
                appendLine("Lokasi Rak : ${part.rackLocation}")
                appendLine("Supplier   : ${part.supplierName} (${part.supplierPhone})")
            }
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Stok ${part.code} - ${part.name}")
                putExtra(Intent.EXTRA_TEXT, reportText)
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan Suku Cadang"))
        }

        btnEdit.setOnClickListener {
            val editIntent = Intent(this, SparepartFormActivity::class.java).apply {
                putExtra(SparepartFormActivity.EXTRA_SPAREPART_ID, part.id)
            }
            startActivity(editIntent)
        }

        btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Hapus Suku Cadang?")
                .setMessage("Hapus '${part.name}' (${part.code}) dari inventaris gudang?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Hapus") { _, _ ->
                    prefManager.deleteSparepart(part.id)
                    Toast.makeText(this, "Suku cadang dihapus", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .show()
        }
    }

    companion object {
        const val EXTRA_SPAREPART_ID = "extra_sparepart_id"
    }
}
