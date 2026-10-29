package com.example.util

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.example.R
import com.example.data.PrefManager
import com.example.model.MutationType
import com.example.model.Sparepart
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

object StockDialogHelper {

    fun showMutationDialog(
        context: Context,
        prefManager: PrefManager,
        sparepart: Sparepart,
        type: MutationType,
        onSuccess: (Sparepart) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_stock_mutation, null)
        val tvSummary = dialogView.findViewById<TextView>(R.id.tvDialogPartSummary)
        val etQty = dialogView.findViewById<TextInputEditText>(R.id.etMutationQty)
        val etReason = dialogView.findViewById<TextInputEditText>(R.id.etMutationReason)

        val title = if (type == MutationType.IN) {
            "Catat Barang Masuk (+ Restock)"
        } else {
            "Catat Barang Keluar (- Pemakaian)"
        }
        tvSummary.text = "${sparepart.code} — ${sparepart.name}\nStok saat ini: ${sparepart.stock} Unit"

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(dialogView)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan Mutasi") { _, _ ->
                val qty = etQty.text?.toString()?.trim()?.toIntOrNull() ?: 0
                val reason = etReason.text?.toString()?.trim().orEmpty()
                val result = prefManager.recordStockMutation(sparepart.id, type, qty, reason)
                result.fold(
                    onSuccess = { updated ->
                        val msg = if (type == MutationType.IN) {
                            "Stok bertambah +$qty unit (Total: ${updated.stock})"
                        } else {
                            "Stok berkurang -$qty unit (Sisa: ${updated.stock})"
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        onSuccess(updated)
                    },
                    onFailure = { err ->
                        Toast.makeText(
                            context,
                            err.message ?: "Gagal menyimpan mutasi stok",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
            .show()
    }
}
