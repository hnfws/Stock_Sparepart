package com.example

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.data.PrefManager
import com.example.model.MutationType
import com.example.model.Sparepart
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `bottom navigation, detail view, and inventory workflow work end to end`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Stock Sparepart", context.getString(R.string.app_name))

        val prefManager = PrefManager(context)
        prefManager.clearAllPreferences()

        assertEquals(6, prefManager.getAllSpareparts().size)

        assertFalse(prefManager.isOnboardingDone())
        prefManager.setOnboardingDone(true)
        assertTrue(prefManager.isOnboardingDone())

        assertTrue(prefManager.validateCredentials("admin", "stmi123"))
        prefManager.saveLoginSession("Budi Santoso", "Kepala Gudang", rememberUsername = true)
        assertTrue(prefManager.isLoggedIn())

        val newPart = Sparepart(
            id = "test-sp-99",
            code = "CLT-TYT-99",
            name = "Kampas Kopling Set",
            category = "Transmisi & Kopling",
            vehicleBrand = "Toyota Innova",
            qualityType = "OEM / Genuine",
            hasWarranty = true,
            isFastMoving = true,
            stock = 10,
            minStock = 3,
            unitPrice = 750000L,
            rackLocation = "Rak F-02",
            supplierName = "PT Aisin Indonesia",
            supplierPhone = "0218970909",
            notes = "Satu set kopling + matahari",
            updatedAt = System.currentTimeMillis()
        )
        prefManager.addOrUpdateSparepart(newPart, isNew = true)
        val outResult = prefManager.recordStockMutation("test-sp-99", MutationType.OUT, 4, "Servis kopling")
        assertTrue(outResult.isSuccess)

        val activity = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().get()
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val tabDashboard = activity.findViewById<View>(R.id.tabDashboardView)
        val tabSpareparts = activity.findViewById<View>(R.id.tabSparepartsView)
        val tabHistory = activity.findViewById<View>(R.id.tabHistoryView)
        val tabAccount = activity.findViewById<View>(R.id.tabAccountView)

        // Ensure top-right sun/moon icon is removed
        val resId = activity.resources.getIdentifier("btnToolbarThemeToggle", "id", activity.packageName)
        assertEquals(0, resId)
        assertNull(activity.findViewById<View>(resId))

        assertEquals(View.VISIBLE, tabDashboard.visibility)
        assertEquals("Halo, Budi Santoso", activity.findViewById<TextView>(R.id.tvWelcomeUser).text.toString())
        assertEquals("7", activity.findViewById<TextView>(R.id.tvKpiTotalSku).text.toString())

        // Navigate across BottomNavigationView tabs & verify item inflation
        bottomNav.selectedItemId = R.id.nav_spareparts
        assertEquals(View.VISIBLE, tabSpareparts.visibility)
        val lvParts = activity.findViewById<ListView>(R.id.lvSpareparts)
        assertEquals(7, lvParts.adapter.count - lvParts.headerViewsCount)
        val sampleRow = lvParts.adapter.getView(lvParts.headerViewsCount, null, lvParts)
        assertTrue(sampleRow.findViewById<TextView>(R.id.tvItemName).text.isNotEmpty())

        bottomNav.selectedItemId = R.id.nav_history
        assertEquals(View.VISIBLE, tabHistory.visibility)
        val lvMut = activity.findViewById<ListView>(R.id.lvTabMutations)
        val mutRow = lvMut.adapter.getView(0, null, lvMut)
        assertTrue(mutRow.findViewById<TextView>(R.id.tvMutationPartName).text.isNotEmpty())

        bottomNav.selectedItemId = R.id.nav_account
        assertEquals(View.VISIBLE, tabAccount.visibility)
        assertEquals("Budi Santoso", activity.findViewById<TextView>(R.id.tvAccountName).text.toString())
        assertEquals("7 Komponen", activity.findViewById<TextView>(R.id.tvAccountStatParts).text.toString())

        val switchDarkMode = activity.findViewById<MaterialSwitch>(R.id.switchDarkMode)
        assertFalse(prefManager.isDarkMode())
        switchDarkMode.isChecked = true
        assertTrue(prefManager.isDarkMode())

        // Verify colorful SparepartDetailActivity binds all spec tiles cleanly
        val detailIntent = Intent(context, SparepartDetailActivity::class.java).apply {
            putExtra(SparepartDetailActivity.EXTRA_SPAREPART_ID, "test-sp-99")
        }
        val detailActivity = Robolectric.buildActivity(SparepartDetailActivity::class.java, detailIntent)
            .create().start().resume().get()
        assertEquals("Kampas Kopling Set", detailActivity.findViewById<TextView>(R.id.tvDetailName).text.toString())
        assertEquals("OEM / Genuine", detailActivity.findViewById<TextView>(R.id.tvSpecQuality).text.toString())
        assertEquals("Rak F-02", detailActivity.findViewById<TextView>(R.id.tvSpecRack).text.toString())
        assertEquals("3 Unit", detailActivity.findViewById<TextView>(R.id.tvSpecMinStock).text.toString())
        assertEquals("PT Aisin Indonesia", detailActivity.findViewById<TextView>(R.id.tvDetailSupplierName).text.toString())
    }
}
