package com.example.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.MutationType
import com.example.model.StockMutation
import com.google.android.material.card.MaterialCardView

class StockMutationAdapter(
    private val context: Context,
    private var items: List<StockMutation>
) : BaseAdapter() {

    fun updateData(newItems: List<StockMutation>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): StockMutation = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_stock_mutation, parent, false)
            holder = ViewHolder(
                cardRoot = view.findViewById(R.id.cardMutationRoot),
                viewAccent = view.findViewById(R.id.viewMutationAccent),
                tvBadge = view.findViewById(R.id.tvMutationBadge),
                tvPartName = view.findViewById(R.id.tvMutationPartName),
                tvNote = view.findViewById(R.id.tvMutationNote),
                tvMeta = view.findViewById(R.id.tvMutationMeta)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val mutation = items[position]
        val isStockIn = mutation.type == MutationType.IN

        val accentColor = ContextCompat.getColor(
            context,
            if (isStockIn) R.color.btn_emerald else R.color.btn_coral
        )
        val strokeColor = ContextCompat.getColor(
            context,
            if (isStockIn) R.color.status_safe_stroke else R.color.status_empty_stroke
        )
        val textColor = ContextCompat.getColor(
            context,
            if (isStockIn) R.color.status_safe_text else R.color.status_empty_text
        )
        val bgColor = ContextCompat.getColor(
            context,
            if (isStockIn) R.color.status_safe_bg else R.color.status_empty_bg
        )
        val strokePx = (1 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

        holder.viewAccent.setBackgroundColor(accentColor)
        holder.cardRoot.strokeColor = strokeColor

        holder.tvBadge.text = if (isStockIn) "+${mutation.quantity}" else "-${mutation.quantity}"
        holder.tvBadge.setTextColor(textColor)
        holder.tvBadge.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(bgColor)
            setStroke(strokePx, strokeColor)
        }

        holder.tvPartName.text = "${mutation.partCode} • ${mutation.partName}"
        holder.tvNote.text = mutation.note
        holder.tvMeta.text = "${mutation.timestamp}  •  Sisa Stok: ${mutation.resultingStock} Unit  •  ${mutation.actorName}"

        return view
    }

    private data class ViewHolder(
        val cardRoot: MaterialCardView,
        val viewAccent: View,
        val tvBadge: TextView,
        val tvPartName: TextView,
        val tvNote: TextView,
        val tvMeta: TextView
    )
}
