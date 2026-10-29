package com.example

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextMenu
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.adapter.SparepartAdapter
import com.example.adapter.StockMutationAdapter
import com.example.data.PrefManager
import com.example.model.CurrencyHelper
import com.example.model.MutationType
import com.example.model.Sparepart
import com.example.model.StockMutation
import com.example.model.StockStatus
import com.example.util.StockDialogHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private enum class SortOrder { NEWEST, NAME_ASC, STOCK_ASC, PRICE_DESC }
    private enum class HistoryFilter { ALL, IN_ONLY, OUT_ONLY }

    private data class CategoryTheme(
        val bgColorRes: Int,
        val strokeColorRes: Int,
        val accentColorRes: Int
    )

    private lateinit var prefManager: PrefManager
    private lateinit var appBarContainer: View
    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fabAdd: ExtendedFloatingActionButton

    private lateinit var tabDashboardView: View
    private lateinit var tabSparepartsView: View
    private lateinit var tabHistoryView: View
    private lateinit var tabAccountView: View

    private lateinit var lvSpareparts: ListView
    private lateinit var filterHeaderView: View
    private lateinit var spinnerFilter: Spinner
    private lateinit var cbLowStock: CheckBox
    private lateinit var sparepartAdapter: SparepartAdapter
    private lateinit var historyTabAdapter: StockMutationAdapter

    private var searchQuery: String = ""
    private var selectedCategory: String = "Semua Kategori"
    private var onlyLowStock: Boolean = false
    private var currentSort: SortOrder = SortOrder.NEWEST
    private var historyFilter: HistoryFilter = HistoryFilter.ALL
    private var isUpdatingThemeSwitch: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        prefManager = PrefManager(this)
        prefManager.applySavedTheme()
        super.onCreate(savedInstanceState)

        if (!prefManager.isOnboardingDone()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        if (!prefManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val isDark = prefManager.isDarkMode()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = !isDark
        }

        appBarContainer = findViewById(R.id.appBarContainer)
        toolbar = findViewById(R.id.toolbarMain)
        bottomNav = findViewById(R.id.bottomNavigation)
        fabAdd = findViewById(R.id.fabAddSparepart)
        tabDashboardView = findViewById(R.id.tabDashboardView)
        tabSparepartsView = findViewById(R.id.tabSparepartsView)
        tabHistoryView = findViewById(R.id.tabHistoryView)
        tabAccountView = findViewById(R.id.tabAccountView)

        val mainRoot = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, 0, bars.right, 0)
            appBarContainer.setPadding(0, bars.top, 0, 0)
            bottomNav.setPadding(0, 0, 0, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        setSupportActionBar(toolbar)

        setupSparepartsTab()
        setupDashboardTab()
        setupHistoryTab()
        setupAccountTab()
        setupBottomNavigation(savedInstanceState?.getInt(STATE_SELECTED_TAB, R.id.nav_dashboard) ?: R.id.nav_dashboard)

        fabAdd.setOnClickListener {
            startActivity(Intent(this, SparepartFormActivity::class.java))
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (bottomNav.selectedItemId != R.id.nav_dashboard) {
                    bottomNav.selectedItemId = R.id.nav_dashboard
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        refreshAllTabs()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::bottomNav.isInitialized) {
            outState.putInt(STATE_SELECTED_TAB, bottomNav.selectedItemId)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::prefManager.isInitialized && prefManager.isLoggedIn() && ::sparepartAdapter.isInitialized) {
            refreshAllTabs()
        }
    }

    private fun setupBottomNavigation(initialTabId: Int) {
        bottomNav.setOnItemSelectedListener { item ->
            switchTab(item.itemId)
            true
        }
        bottomNav.selectedItemId = initialTabId
        switchTab(initialTabId)
    }

    private fun switchTab(tabId: Int) {
        tabDashboardView.visibility = if (tabId == R.id.nav_dashboard) View.VISIBLE else View.GONE
        tabSparepartsView.visibility = if (tabId == R.id.nav_spareparts) View.VISIBLE else View.GONE
        tabHistoryView.visibility = if (tabId == R.id.nav_history) View.VISIBLE else View.GONE
        tabAccountView.visibility = if (tabId == R.id.nav_account) View.VISIBLE else View.GONE

        when (tabId) {
            R.id.nav_dashboard -> {
                toolbar.title = getString(R.string.app_name)
                toolbar.subtitle = getString(R.string.dashboard_subtitle)
                fabAdd.show()
            }
            R.id.nav_spareparts -> {
                toolbar.title = getString(R.string.nav_spareparts)
                toolbar.subtitle = getString(R.string.spareparts_subtitle)
                fabAdd.show()
            }
            R.id.nav_history -> {
                toolbar.title = getString(R.string.title_stock_history)
                toolbar.subtitle = getString(R.string.history_subtitle)
                fabAdd.hide()
            }
            R.id.nav_account -> {
                toolbar.title = getString(R.string.nav_account)
                toolbar.subtitle = getString(R.string.account_subtitle)
                fabAdd.hide()
            }
        }
    }

    private fun setupDashboardTab() {
        val cardKpiTotalSku = tabDashboardView.findViewById<MaterialCardView>(R.id.cardKpiTotalSku)
        val cardKpiLowStock = tabDashboardView.findViewById<MaterialCardView>(R.id.cardKpiLowStock)
        val btnReviewLowStock = tabDashboardView.findViewById<MaterialButton>(R.id.btnReviewLowStock)
        val btnGoToHistory = tabDashboardView.findViewById<MaterialButton>(R.id.btnGoToHistoryTab)

        cardKpiTotalSku.setOnClickListener {
            cbLowStock.isChecked = false
            spinnerFilter.setSelection(0)
            bottomNav.selectedItemId = R.id.nav_spareparts
        }
        val jumpToCriticalStock = View.OnClickListener {
            spinnerFilter.setSelection(0)
            cbLowStock.isChecked = true
            bottomNav.selectedItemId = R.id.nav_spareparts
        }
        cardKpiLowStock.setOnClickListener(jumpToCriticalStock)
        btnReviewLowStock.setOnClickListener(jumpToCriticalStock)
        btnGoToHistory.setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_history
        }
    }

    private fun setupSparepartsTab() {
        lvSpareparts = tabSparepartsView.findViewById(R.id.lvSpareparts)
        filterHeaderView = LayoutInflater.from(this).inflate(R.layout.header_spareparts_filter, lvSpareparts, false)
        lvSpareparts.addHeaderView(filterHeaderView, null, false)

        sparepartAdapter = SparepartAdapter(
            context = this,
            items = emptyList(),
            onMoreMenuClicked = { anchor, sparepart -> showItemPopupMenu(anchor, sparepart) }
        )
        lvSpareparts.adapter = sparepartAdapter

        registerForContextMenu(lvSpareparts)

        lvSpareparts.setOnItemClickListener { parent, _, position, _ ->
            val clickedPart = parent.getItemAtPosition(position) as? Sparepart ?: return@setOnItemClickListener
            openPartDetail(clickedPart.id)
        }

        val etSearch = filterHeaderView.findViewById<TextInputEditText>(R.id.etSearchSparepart)
        spinnerFilter = filterHeaderView.findViewById<Spinner>(R.id.spinnerCategoryFilter)
        val btnSort = filterHeaderView.findViewById<MaterialButton>(R.id.btnSortPopup)
        cbLowStock = filterHeaderView.findViewById<CheckBox>(R.id.cbFilterLowStockOnly)

        val filterCategories = resources.getStringArray(R.array.filter_categories)
        spinnerFilter.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            filterCategories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = filterCategories[position]
                refreshSparepartsList(prefManager.getAllSpareparts())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim().orEmpty()
                refreshSparepartsList(prefManager.getAllSpareparts())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        cbLowStock.setOnCheckedChangeListener { _, isChecked ->
            onlyLowStock = isChecked
            refreshSparepartsList(prefManager.getAllSpareparts())
        }

        btnSort.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menuInflater.inflate(R.menu.menu_sort_popup, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                currentSort = when (menuItem.itemId) {
                    R.id.sort_name_asc -> SortOrder.NAME_ASC
                    R.id.sort_stock_asc -> SortOrder.STOCK_ASC
                    R.id.sort_price_desc -> SortOrder.PRICE_DESC
                    else -> SortOrder.NEWEST
                }
                refreshSparepartsList(prefManager.getAllSpareparts())
                true
            }
            popup.show()
        }
    }

    private fun setupHistoryTab() {
        val lvTabMutations = tabHistoryView.findViewById<ListView>(R.id.lvTabMutations)
        val rgFilter = tabHistoryView.findViewById<RadioGroup>(R.id.rgHistoryFilter)

        historyTabAdapter = StockMutationAdapter(this, emptyList())
        lvTabMutations.adapter = historyTabAdapter

        lvTabMutations.setOnItemClickListener { parent, _, position, _ ->
            val mut = parent.getItemAtPosition(position) as? StockMutation ?: return@setOnItemClickListener
            if (prefManager.getSparepartById(mut.sparepartId) != null) {
                openPartDetail(mut.sparepartId)
            }
        }

        rgFilter.setOnCheckedChangeListener { _, checkedId ->
            historyFilter = when (checkedId) {
                R.id.rbHistIn -> HistoryFilter.IN_ONLY
                R.id.rbHistOut -> HistoryFilter.OUT_ONLY
                else -> HistoryFilter.ALL
            }
            refreshHistoryTab()
        }
    }

    private fun setupAccountTab() {
        val btnEditProfile = tabAccountView.findViewById<MaterialButton>(R.id.btnEditProfile)
        val switchDarkMode = tabAccountView.findViewById<MaterialSwitch>(R.id.switchDarkMode)
        val btnShare = tabAccountView.findViewById<MaterialButton>(R.id.btnAccountShareSummary)
        val btnOnboarding = tabAccountView.findViewById<MaterialButton>(R.id.btnAccountOnboarding)
        val btnReset = tabAccountView.findViewById<MaterialButton>(R.id.btnAccountResetData)
        val btnLogout = tabAccountView.findViewById<MaterialButton>(R.id.btnAccountLogout)

        btnEditProfile.setOnClickListener { showEditProfileDialog() }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingThemeSwitch && isChecked != prefManager.isDarkMode()) {
                prefManager.setDarkMode(isChecked)
            }
        }

        btnShare.setOnClickListener { shareWarehouseSummary() }
        btnOnboarding.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                putExtra(OnboardingActivity.EXTRA_FROM_DASHBOARD, true)
            })
        }
        btnReset.setOnClickListener { confirmResetData() }
        btnLogout.setOnClickListener { confirmLogout() }
    }

    private fun refreshAllTabs() {
        val allParts = prefManager.getAllSpareparts()
        refreshDashboardTab(allParts)
        refreshSparepartsList(allParts)
        refreshHistoryTab()
        refreshAccountTab(allParts.size)
    }

    private fun refreshDashboardTab(allParts: List<Sparepart>) {
        val tvRoleBadge = tabDashboardView.findViewById<TextView>(R.id.tvSessionRoleBadge)
        val tvWelcomeUser = tabDashboardView.findViewById<TextView>(R.id.tvWelcomeUser)
        val tvTotalAssetHero = tabDashboardView.findViewById<TextView>(R.id.tvTotalAssetHero)
        val tvSessionMeta = tabDashboardView.findViewById<TextView>(R.id.tvSessionMetaInfo)
        val tvKpiSku = tabDashboardView.findViewById<TextView>(R.id.tvKpiTotalSku)
        val tvKpiUnits = tabDashboardView.findViewById<TextView>(R.id.tvKpiTotalUnits)
        val tvKpiLow = tabDashboardView.findViewById<TextView>(R.id.tvKpiLowStock)
        val cardLowBanner = tabDashboardView.findViewById<MaterialCardView>(R.id.cardLowStockBanner)
        val tvLowMessage = tabDashboardView.findViewById<TextView>(R.id.tvLowStockAlertMessage)
        val tvDashMutIn = tabDashboardView.findViewById<TextView>(R.id.tvDashMutIn)
        val tvDashMutOut = tabDashboardView.findViewById<TextView>(R.id.tvDashMutOut)
        val llCategoryBreakdown = tabDashboardView.findViewById<LinearLayout>(R.id.llCategoryBreakdown)

        tvRoleBadge.visibility = View.GONE
        tvWelcomeUser.text = "Halo, ${prefManager.getUserName()}"

        val totalUnits = allParts.sumOf { it.stock }
        val totalAssetValue = allParts.sumOf { it.totalValue }
        val criticalParts = allParts.filter { it.stockStatus != StockStatus.SAFE }

        tvTotalAssetHero.text = CurrencyHelper.formatRupiah(totalAssetValue)
        tvSessionMeta.text = "${allParts.size} jenis komponen  •  $totalUnits unit tersedia"

        tvKpiSku.text = "${allParts.size}"
        tvKpiUnits.text = "$totalUnits"
        tvKpiLow.text = "${criticalParts.size}"

        if (criticalParts.isNotEmpty()) {
            cardLowBanner.visibility = View.VISIBLE
            val previewNames = criticalParts.take(2).joinToString(", ") { "${it.name} (${it.stock})" }
            val extra = if (criticalParts.size > 2) " dan ${criticalParts.size - 2} lainnya" else ""
            tvLowMessage.text = "${criticalParts.size} komponen perlu pengadaan ulang: $previewNames$extra"
        } else {
            cardLowBanner.visibility = View.GONE
        }

        val allMutations = prefManager.getAllMutations()
        val unitsIn = allMutations.filter { it.type == MutationType.IN }.sumOf { it.quantity }
        val unitsOut = allMutations.filter { it.type == MutationType.OUT }.sumOf { it.quantity }
        tvDashMutIn.text = "+$unitsIn Unit"
        tvDashMutOut.text = "-$unitsOut Unit"

        llCategoryBreakdown.removeAllViews()
        val density = resources.displayMetrics.density
        val filterCategories = resources.getStringArray(R.array.filter_categories)
        val grouped = allParts.groupBy { it.category }

        val palettes = listOf(
            CategoryTheme(R.color.tile_blue_bg, R.color.tile_blue_stroke, R.color.brand_primary),
            CategoryTheme(R.color.tile_teal_bg, R.color.tile_teal_stroke, R.color.btn_teal),
            CategoryTheme(R.color.tile_indigo_bg, R.color.tile_indigo_stroke, R.color.tile_indigo_text),
            CategoryTheme(R.color.tile_amber_bg, R.color.tile_amber_stroke, R.color.brand_accent_amber),
            CategoryTheme(R.color.status_safe_bg, R.color.status_safe_stroke, R.color.btn_emerald)
        )

        grouped.entries.sortedByDescending { entry -> entry.value.sumOf { it.stock } }
            .forEachIndexed { index, (cat, list) ->
                val catUnits = list.sumOf { it.stock }
                val palette = palettes[index % palettes.size]
                val strokePx = (1 * density).toInt().coerceAtLeast(1)
                val padH = (12 * density).toInt()
                val padV = (10 * density).toInt()

                val row = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        if (index > 0) topMargin = (8 * density).toInt()
                    }
                    orientation = LinearLayout.VERTICAL
                    setPadding(padH, padV, padH, padV)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 12f * density
                        setColor(ContextCompat.getColor(this@MainActivity, palette.bgColorRes))
                        setStroke(strokePx, ContextCompat.getColor(this@MainActivity, palette.strokeColorRes))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        val idx = filterCategories.indexOfFirst { it == cat }
                        if (idx >= 0) spinnerFilter.setSelection(idx)
                        cbLowStock.isChecked = false
                        bottomNav.selectedItemId = R.id.nav_spareparts
                    }
                }

                val topLabelRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                val tvCatName = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text = cat
                    textSize = 13f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                }
                val tvCatStats = TextView(this).apply {
                    text = "${list.size} item • $catUnits unit"
                    textSize = 12f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(this@MainActivity, palette.accentColorRes))
                }
                topLabelRow.addView(tvCatName)
                topLabelRow.addView(tvCatStats)

                val progressBar = LinearProgressIndicator(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (7 * density).toInt() }
                    max = totalUnits.coerceAtLeast(1)
                    progress = catUnits
                    trackCornerRadius = (4 * density).toInt()
                    trackThickness = (6 * density).toInt()
                    setIndicatorColor(ContextCompat.getColor(this@MainActivity, palette.accentColorRes))
                    trackColor = ContextCompat.getColor(this@MainActivity, R.color.bg_surface)
                }

                row.addView(topLabelRow)
                row.addView(progressBar)
                llCategoryBreakdown.addView(row)
            }
    }

    private fun refreshSparepartsList(allParts: List<Sparepart>) {
        val tvListCountTitle = filterHeaderView.findViewById<TextView>(R.id.tvListCountTitle)
        val cardEmptyState = filterHeaderView.findViewById<MaterialCardView>(R.id.cardEmptyState)

        val filtered = allParts.filter { part ->
            val matchesQuery = searchQuery.isEmpty() ||
                part.name.contains(searchQuery, ignoreCase = true) ||
                part.code.contains(searchQuery, ignoreCase = true) ||
                part.vehicleBrand.contains(searchQuery, ignoreCase = true) ||
                part.rackLocation.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == "Semua Kategori" || part.category == selectedCategory
            val matchesLowStock = !onlyLowStock || part.stockStatus != StockStatus.SAFE
            matchesQuery && matchesCategory && matchesLowStock
        }.let { list ->
            when (currentSort) {
                SortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                SortOrder.STOCK_ASC -> list.sortedBy { it.stock }
                SortOrder.PRICE_DESC -> list.sortedByDescending { it.unitPrice }
                SortOrder.NEWEST -> list.sortedByDescending { it.updatedAt }
            }
        }

        tvListCountTitle.text = "Daftar Suku Cadang (${filtered.size})"
        cardEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        sparepartAdapter.updateData(filtered)
    }

    private fun refreshHistoryTab() {
        val tvIn = tabHistoryView.findViewById<TextView>(R.id.tvTabHistoryIn)
        val tvOut = tabHistoryView.findViewById<TextView>(R.id.tvTabHistoryOut)
        val tvEmpty = tabHistoryView.findViewById<TextView>(R.id.tvTabHistoryEmpty)
        val lvMutations = tabHistoryView.findViewById<ListView>(R.id.lvTabMutations)

        val allMutations = prefManager.getAllMutations()
        val totalIn = allMutations.filter { it.type == MutationType.IN }.sumOf { it.quantity }
        val totalOut = allMutations.filter { it.type == MutationType.OUT }.sumOf { it.quantity }

        tvIn.text = "+$totalIn Unit"
        tvOut.text = "-$totalOut Unit"

        val filtered = when (historyFilter) {
            HistoryFilter.ALL -> allMutations
            HistoryFilter.IN_ONLY -> allMutations.filter { it.type == MutationType.IN }
            HistoryFilter.OUT_ONLY -> allMutations.filter { it.type == MutationType.OUT }
        }

        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        lvMutations.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        historyTabAdapter.updateData(filtered)
    }

    private fun refreshAccountTab(totalPartsCount: Int) {
        val tvInitials = tabAccountView.findViewById<TextView>(R.id.tvAccountInitials)
        val tvName = tabAccountView.findViewById<TextView>(R.id.tvAccountName)
        val tvRole = tabAccountView.findViewById<TextView>(R.id.tvAccountRole)
        val tvLastLogin = tabAccountView.findViewById<TextView>(R.id.tvAccountLastLogin)
        val tvStatParts = tabAccountView.findViewById<TextView>(R.id.tvAccountStatParts)
        val tvStatMutations = tabAccountView.findViewById<TextView>(R.id.tvAccountStatMutations)
        val switchDarkMode = tabAccountView.findViewById<MaterialSwitch>(R.id.switchDarkMode)

        val userName = prefManager.getUserName()
        val initials = userName.trim().split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "AD" }

        tvInitials.text = initials
        tvName.text = userName
        tvRole.visibility = View.GONE
        tvLastLogin.text = "Masuk sejak ${prefManager.getLastLoginTime()}"
        tvStatParts.text = "$totalPartsCount Komponen"
        tvStatMutations.text = "${prefManager.getAllMutations().size} Transaksi"

        isUpdatingThemeSwitch = true
        switchDarkMode.isChecked = prefManager.isDarkMode()
        isUpdatingThemeSwitch = false
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etEditProfileName)

        etName?.setText(prefManager.getUserName())

        MaterialAlertDialogBuilder(this)
            .setTitle("Ubah Profil Petugas")
            .setView(dialogView)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val newName = etName?.text?.toString()?.trim().orEmpty()
                if (newName.isEmpty()) {
                    Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                prefManager.updateProfile(newName)
                Toast.makeText(this, "Profil petugas berhasil diperbarui", Toast.LENGTH_SHORT).show()
                refreshAllTabs()
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share_summary -> {
                shareWarehouseSummary()
                true
            }
            R.id.action_view_onboarding -> {
                startActivity(Intent(this, OnboardingActivity::class.java).apply {
                    putExtra(OnboardingActivity.EXTRA_FROM_DASHBOARD, true)
                })
                true
            }
            R.id.action_reset_data -> {
                confirmResetData()
                true
            }
            R.id.action_logout -> {
                confirmLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val info = menuInfo as? AdapterView.AdapterContextMenuInfo ?: return
        val part = lvSpareparts.getItemAtPosition(info.position) as? Sparepart ?: return
        menu.setHeaderTitle("${part.code} • ${part.name}")
        menuInflater.inflate(R.menu.menu_sparepart_context, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? AdapterView.AdapterContextMenuInfo
            ?: return super.onContextItemSelected(item)
        val part = lvSpareparts.getItemAtPosition(info.position) as? Sparepart
            ?: return super.onContextItemSelected(item)

        return when (item.itemId) {
            R.id.ctx_view_detail -> { openPartDetail(part.id); true }
            R.id.ctx_edit_part -> { openPartEdit(part.id); true }
            R.id.ctx_quick_restock -> {
                prefManager.recordStockMutation(part.id, MutationType.IN, 5, "Penambahan stok cepat (+5 unit)")
                Toast.makeText(this, "Stok ${part.name} bertambah 5 unit", Toast.LENGTH_SHORT).show()
                refreshAllTabs()
                true
            }
            R.id.ctx_delete_part -> { confirmDeletePart(part); true }
            else -> super.onContextItemSelected(item)
        }
    }

    private fun showItemPopupMenu(anchor: View, part: Sparepart) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_sparepart_popup, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.popup_stock_in -> {
                    StockDialogHelper.showMutationDialog(this, prefManager, part, MutationType.IN) { refreshAllTabs() }
                    true
                }
                R.id.popup_stock_out -> {
                    StockDialogHelper.showMutationDialog(this, prefManager, part, MutationType.OUT) { refreshAllTabs() }
                    true
                }
                R.id.popup_edit -> { openPartEdit(part.id); true }
                R.id.popup_call_supplier -> {
                    if (part.supplierPhone.isBlank()) {
                        Toast.makeText(this, "Nomor telepon pemasok belum tersedia", Toast.LENGTH_SHORT).show()
                    } else {
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${part.supplierPhone}")))
                    }
                    true
                }
                R.id.popup_delete -> { confirmDeletePart(part); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun openPartDetail(partId: String) {
        startActivity(Intent(this, SparepartDetailActivity::class.java).apply {
            putExtra(SparepartDetailActivity.EXTRA_SPAREPART_ID, partId)
        })
    }

    private fun openPartEdit(partId: String) {
        startActivity(Intent(this, SparepartFormActivity::class.java).apply {
            putExtra(SparepartFormActivity.EXTRA_SPAREPART_ID, partId)
        })
    }

    private fun confirmDeletePart(part: Sparepart) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Komponen?")
            .setMessage("Hapus '${part.name}' (${part.code}) dari daftar inventaris?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                prefManager.deleteSparepart(part.id)
                Toast.makeText(this, "${part.name} dihapus", Toast.LENGTH_SHORT).show()
                refreshAllTabs()
            }
            .show()
    }

    private fun confirmResetData() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Atur Ulang Data Gudang?")
            .setMessage("Seluruh catatan suku cadang, riwayat mutasi, dan sesi petugas akan dikembalikan ke pengaturan awal.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Atur Ulang") { _, _ ->
                prefManager.clearAllPreferences()
                Toast.makeText(this, "Data gudang berhasil diatur ulang", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
            }
            .show()
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Keluar Akun?")
            .setMessage("Anda perlu masuk kembali untuk mengelola data inventaris gudang.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Keluar") { _, _ ->
                prefManager.logoutSession()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .show()
    }

    private fun shareWarehouseSummary() {
        val allParts = prefManager.getAllSpareparts()
        val totalUnits = allParts.sumOf { it.stock }
        val totalValue = allParts.sumOf { it.totalValue }
        val lowCount = allParts.count { it.stockStatus != StockStatus.SAFE }

        val summary = buildString {
            appendLine("LAPORAN INVENTARIS GUDANG - STOCK SPAREPART")
            appendLine("Petugas : ${prefManager.getUserName()} (${prefManager.getUserRole()})")
            appendLine("Komponen : ${allParts.size} Jenis ($totalUnits Unit)")
            appendLine("Perlu Restock : $lowCount Komponen")
            appendLine("Nilai Inventaris : ${CurrencyHelper.formatRupiah(totalValue)}")
            appendLine("-----------------------------------------")
            allParts.forEachIndexed { idx, p ->
                appendLine("${idx + 1}. [${p.code}] ${p.name} — Stok: ${p.stock} (${CurrencyHelper.formatRupiah(p.unitPrice)})")
            }
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Inventaris Gudang")
            putExtra(Intent.EXTRA_TEXT, summary)
        }
        startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan Gudang"))
    }

    companion object {
        private const val STATE_SELECTED_TAB = "state_selected_tab"
    }
}
