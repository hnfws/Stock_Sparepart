package com.example

import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.adapter.StockMutationAdapter
import com.example.data.PrefManager
import com.example.model.MutationType
import com.google.android.material.appbar.MaterialToolbar

class StockHistoryActivity : AppCompatActivity() {

    private lateinit var prefManager: PrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        prefManager = PrefManager(this)
        prefManager.applySavedTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_stock_history)

        val root = findViewById<View>(R.id.historyRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbarHistory)
        toolbar.setNavigationOnClickListener { finish() }

        val tvTotalIn = findViewById<TextView>(R.id.tvHistoryTotalIn)
        val tvTotalOut = findViewById<TextView>(R.id.tvHistoryTotalOut)
        val tvEmpty = findViewById<TextView>(R.id.tvHistoryEmpty)
        val lvHistory = findViewById<ListView>(R.id.lvAllHistory)

        val mutations = prefManager.getAllMutations()
        val totalUnitsIn = mutations.filter { it.type == MutationType.IN }.sumOf { it.quantity }
        val totalUnitsOut = mutations.filter { it.type == MutationType.OUT }.sumOf { it.quantity }

        tvTotalIn.text = "+$totalUnitsIn Unit"
        tvTotalOut.text = "-$totalUnitsOut Unit"

        if (mutations.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            lvHistory.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            lvHistory.visibility = View.VISIBLE
            lvHistory.adapter = StockMutationAdapter(this, mutations)
        }
    }
}
