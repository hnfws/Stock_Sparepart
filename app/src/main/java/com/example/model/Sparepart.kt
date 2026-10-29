package com.example.model

import java.text.NumberFormat
import java.util.Locale

enum class StockStatus {
    SAFE,
    LOW,
    EMPTY
}

enum class MutationType {
    IN,
    OUT
}

data class Sparepart(
    val id: String,
    val code: String,
    val name: String,
    val category: String,
    val vehicleBrand: String,
    val qualityType: String, // OEM / Genuine, Aftermarket, Rekondisi
    val hasWarranty: Boolean,
    val isFastMoving: Boolean,
    val stock: Int,
    val minStock: Int,
    val unitPrice: Long,
    val rackLocation: String,
    val supplierName: String,
    val supplierPhone: String,
    val notes: String,
    val updatedAt: Long
) {
    val stockStatus: StockStatus
        get() = when {
            stock <= 0 -> StockStatus.EMPTY
            stock <= minStock -> StockStatus.LOW
            else -> StockStatus.SAFE
        }

    val totalValue: Long
        get() = stock.toLong() * unitPrice
}

data class StockMutation(
    val id: String,
    val sparepartId: String,
    val partCode: String,
    val partName: String,
    val type: MutationType,
    val quantity: Int,
    val resultingStock: Int,
    val note: String,
    val actorName: String,
    val timestamp: String
)

object CurrencyHelper {
    private val rupiahFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    fun formatRupiah(amount: Long): String {
        return rupiahFormatter.format(amount)
    }

    fun formatCompactRupiah(amount: Long): String {
        return when {
            amount >= 1_000_000_000L -> String.format(Locale("id", "ID"), "Rp %.1f M", amount / 1_000_000_000.0)
            amount >= 1_000_000L -> String.format(Locale("id", "ID"), "Rp %.1f Jt", amount / 1_000_000.0)
            else -> formatRupiah(amount)
        }
    }
}
