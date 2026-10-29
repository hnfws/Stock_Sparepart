package com.example.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.CurrencyHelper
import com.example.model.Sparepart
import com.example.model.StockStatus

class SparepartAdapter(
    private val context: Context,
    private var items: List<Sparepart>,
    private val onMoreMenuClicked: (anchor: View, sparepart: Sparepart) -> Unit
) : BaseAdapter() {

    fun updateData(newItems: List<Sparepart>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Sparepart = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_sparepart, parent, false)
            holder = ViewHolder(
                viewAccent = view.findViewById(R.id.viewItemAccent),
                tvSku = view.findViewById(R.id.tvItemSku),
                tvQuality = view.findViewById(R.id.tvItemQuality),
                btnMore = view.findViewById(R.id.btnItemMore),
                tvName = view.findViewById(R.id.tvItemName),
                tvSubtitle = view.findViewById(R.id.tvItemSubtitle),
                tvMetaTags = view.findViewById(R.id.tvItemMetaTags),
                tvPrice = view.findViewById(R.id.tvItemPrice),
                tvStockStatus = view.findViewById(R.id.tvItemStockStatus)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val item = items[position]
        holder.tvSku.text = item.code

        val (qualBg, qualText) = when {
            item.qualityType.contains("OEM", ignoreCase = true) ->
                R.color.tile_indigo_bg to R.color.tile_indigo_text
            item.qualityType.contains("Aftermarket", ignoreCase = true) ->
                R.color.tile_teal_bg to R.color.tile_teal_text
            else ->
                R.color.tile_amber_bg to R.color.tile_amber_text
        }
        holder.tvQuality.text = item.qualityType
        holder.tvQuality.setTextColor(ContextCompat.getColor(context, qualText))
        holder.tvQuality.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(ContextCompat.getColor(context, qualBg))
        }

        holder.tvName.text = item.name
        holder.tvSubtitle.text = "${item.category} • ${item.vehicleBrand}"

        val badges = mutableListOf("Rak ${item.rackLocation}")
        if (item.hasWarranty) badges.add("Garansi Resmi")
        if (item.isFastMoving) badges.add("Fast Moving")
        holder.tvMetaTags.text = badges.joinToString("  •  ")

        holder.tvPrice.text = CurrencyHelper.formatRupiah(item.unitPrice)

        val (statusText, bgColorRes, textColorRes) = when (item.stockStatus) {
            StockStatus.EMPTY -> Triple(
                "Habis (0 Unit)",
                R.color.status_empty_bg,
                R.color.status_empty_text
            )
            StockStatus.LOW -> Triple(
                "Menipis (${item.stock} Unit)",
                R.color.status_low_bg,
                R.color.status_low_text
            )
            StockStatus.SAFE -> Triple(
                "Aman (${item.stock} Unit)",
                R.color.status_safe_bg,
                R.color.status_safe_text
            )
        }

        val accentColorRes = when (item.stockStatus) {
            StockStatus.EMPTY -> R.color.btn_coral
            StockStatus.LOW -> R.color.brand_accent_amber
            StockStatus.SAFE -> R.color.brand_primary
        }
        holder.viewAccent.setBackgroundColor(ContextCompat.getColor(context, accentColorRes))

        val strokeColorRes = when (item.stockStatus) {
            StockStatus.EMPTY -> R.color.status_empty_stroke
            StockStatus.LOW -> R.color.status_low_stroke
            StockStatus.SAFE -> R.color.status_safe_stroke
        }
        val strokePx = (1 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

        holder.tvStockStatus.text = statusText
        holder.tvStockStatus.setTextColor(ContextCompat.getColor(context, textColorRes))
        holder.tvStockStatus.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(ContextCompat.getColor(context, bgColorRes))
            setStroke(strokePx, ContextCompat.getColor(context, strokeColorRes))
        }

        holder.btnMore.setOnClickListener { anchor ->
            onMoreMenuClicked(anchor, item)
        }

        return view
    }

    private data class ViewHolder(
        val viewAccent: View,
        val tvSku: TextView,
        val tvQuality: TextView,
        val btnMore: ImageButton,
        val tvName: TextView,
        val tvSubtitle: TextView,
        val tvMetaTags: TextView,
        val tvPrice: TextView,
        val tvStockStatus: TextView
    )
}
