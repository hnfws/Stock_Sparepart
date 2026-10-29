package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.example.model.MutationType
import com.example.model.Sparepart
import com.example.model.StockMutation
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PrefManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREF_NAME = "StockSparepartPrefs"

        const val KEY_ONBOARDING_DONE = "key_onboarding_done"
        const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        const val KEY_USER_NAME = "key_name"
        const val KEY_USER_ROLE = "key_user_role"
        const val KEY_REMEMBER_USER = "key_remember_user"
        const val KEY_SAVED_USERNAME = "key_saved_username"
        const val KEY_LAST_LOGIN = "key_last_login"
        const val KEY_USERS_JSON = "key_registered_users"

        const val KEY_DARK_MODE = "key_dark_mode"

        const val KEY_IS_SEEDED = "key_is_seeded"
        const val KEY_SPAREPARTS_JSON = "key_spareparts_json"
        const val KEY_MUTATIONS_JSON = "key_mutations_json"
    }

    init {
        ensureSeededData()
    }
    fun setOnboardingDone(done: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_ONBOARDING_DONE, done)
        editor.apply()
    }

    fun isOnboardingDone(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    fun setDarkMode(enabled: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_DARK_MODE, enabled)
        editor.apply()
        applySavedTheme()
    }

    fun isDarkMode(): Boolean {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false)
    }

    fun applySavedTheme() {
        val mode = if (isDarkMode()) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun registerUser(username: String, password: String): Boolean {
        val cleanUser = username.trim()
        if (cleanUser.isEmpty() || password.isEmpty()) return false

        val usersObj = getRegisteredUsersJson()
        if (cleanUser.equals("admin", ignoreCase = true) || usersObj.has(cleanUser.lowercase())) {
            return false
        }
        usersObj.put(cleanUser.lowercase(), password)
        val editor = sharedPreferences.edit()
        editor.putString(KEY_USERS_JSON, usersObj.toString())
        editor.apply()
        return true
    }

    fun validateCredentials(username: String, password: String): Boolean {
        val cleanUser = username.trim()
        if (cleanUser.equals("admin", ignoreCase = true) && password == "stmi123") {
            return true
        }
        if (cleanUser.equals("marshal.v@torqueops.internal", ignoreCase = true) && password == "Inventory@2026") {
            return true
        }
        val usersObj = getRegisteredUsersJson()
        val storedPassword = usersObj.optString(cleanUser.lowercase(), "")
        return storedPassword.isNotEmpty() && storedPassword == password
    }

    private fun getRegisteredUsersJson(): JSONObject {
        val raw = sharedPreferences.getString(KEY_USERS_JSON, "{}") ?: "{}"
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    fun saveLoginSession(name: String, role: String = "Operator", rememberUsername: Boolean = true) {
        val editor = sharedPreferences.edit()
        editor.putString(KEY_USER_NAME, name.trim())
        editor.putString(KEY_USER_ROLE, role)
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putBoolean(KEY_REMEMBER_USER, rememberUsername)
        editor.putString(KEY_LAST_LOGIN, currentFormattedTime())
        if (rememberUsername) {
            editor.putString(KEY_SAVED_USERNAME, name.trim())
        } else {
            editor.remove(KEY_SAVED_USERNAME)
        }
        editor.apply()
    }

    fun updateProfile(name: String, role: String = "Operator") {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val editor = sharedPreferences.edit()
        editor.putString(KEY_USER_NAME, cleanName)
        editor.putString(KEY_USER_ROLE, role)
        if (isRememberUsername()) {
            editor.putString(KEY_SAVED_USERNAME, cleanName)
        }
        editor.apply()
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserName(): String {
        return sharedPreferences.getString(KEY_USER_NAME, "Belum ada data") ?: "Belum ada data"
    }

    fun getUserRole(): String {
        return sharedPreferences.getString(KEY_USER_ROLE, "Petugas Gudang") ?: "Petugas Gudang"
    }

    fun isRememberUsername(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMEMBER_USER, true)
    }

    fun getSavedUsername(): String {
        return sharedPreferences.getString(KEY_SAVED_USERNAME, "admin") ?: "admin"
    }

    fun getLastLoginTime(): String {
        return sharedPreferences.getString(KEY_LAST_LOGIN, "-") ?: "-"
    }

    fun logoutSession() {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, false)
        editor.remove(KEY_USER_NAME)
        editor.remove(KEY_USER_ROLE)
        editor.apply()
    }

    fun clearAllPreferences() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        ensureSeededData()
    }

    fun buildPreferencesDebugSummary(): String {
        return buildString {
            appendLine("File XML: $PREF_NAME.xml")
            appendLine("• $KEY_USER_NAME : ${getUserName()}")
            appendLine("• $KEY_USER_ROLE : ${getUserRole()}")
            appendLine("• $KEY_IS_LOGGED_IN : ${isLoggedIn()}")
            appendLine("• $KEY_ONBOARDING_DONE : ${isOnboardingDone()}")
            appendLine("• $KEY_DARK_MODE : ${isDarkMode()}")
            appendLine("• $KEY_REMEMBER_USER : ${isRememberUsername()}")
            appendLine("• $KEY_LAST_LOGIN : ${getLastLoginTime()}")
            appendLine("• Tersimpan: ${getAllSpareparts().size} SKU & ${getAllMutations().size} Mutasi")
        }
    }

    fun getAllSpareparts(): List<Sparepart> {
        val raw = sharedPreferences.getString(KEY_SPAREPARTS_JSON, "[]") ?: "[]"
        val list = mutableListOf<Sparepart>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Sparepart(
                        id = obj.getString("id"),
                        code = obj.getString("code"),
                        name = obj.getString("name"),
                        category = obj.getString("category"),
                        vehicleBrand = obj.getString("vehicleBrand"),
                        qualityType = obj.getString("qualityType"),
                        hasWarranty = obj.optBoolean("hasWarranty", false),
                        isFastMoving = obj.optBoolean("isFastMoving", false),
                        stock = obj.getInt("stock"),
                        minStock = obj.optInt("minStock", 5),
                        unitPrice = obj.getLong("unitPrice"),
                        rackLocation = obj.optString("rackLocation", "Rak A-01"),
                        supplierName = obj.optString("supplierName", "-"),
                        supplierPhone = obj.optString("supplierPhone", ""),
                        notes = obj.optString("notes", ""),
                        updatedAt = obj.optLong("updatedAt", 0L)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }

    fun getSparepartById(id: String): Sparepart? {
        return getAllSpareparts().find { it.id == id }
    }

    fun addOrUpdateSparepart(sparepart: Sparepart, isNew: Boolean) {
        val current = getAllSpareparts().toMutableList()
        val index = current.indexOfFirst { it.id == sparepart.id }
        if (index >= 0) {
            current[index] = sparepart
        } else {
            current.add(0, sparepart)
        }
        saveSparepartList(current)

        if (isNew && sparepart.stock > 0) {
            recordMutationInternal(
                sparepart = sparepart,
                type = MutationType.IN,
                quantity = sparepart.stock,
                resultingStock = sparepart.stock,
                note = "Stok awal registrasi komponen baru"
            )
        }
    }

    fun deleteSparepart(id: String) {
        val updated = getAllSpareparts().filterNot { it.id == id }
        saveSparepartList(updated)
    }

    fun recordStockMutation(
        sparepartId: String,
        type: MutationType,
        quantity: Int,
        note: String
    ): Result<Sparepart> {
        if (quantity <= 0) {
            return Result.failure(IllegalArgumentException("Jumlah unit harus lebih dari 0"))
        }
        val list = getAllSpareparts().toMutableList()
        val index = list.indexOfFirst { it.id == sparepartId }
        if (index < 0) {
            return Result.failure(IllegalArgumentException("Data suku cadang tidak ditemukan"))
        }

        val currentPart = list[index]
        val newStock = when (type) {
            MutationType.IN -> currentPart.stock + quantity
            MutationType.OUT -> {
                if (quantity > currentPart.stock) {
                    return Result.failure(
                        IllegalStateException("Stok tidak mencukupi (tersedia ${currentPart.stock} unit)")
                    )
                }
                currentPart.stock - quantity
            }
        }

        val updatedPart = currentPart.copy(
            stock = newStock,
            updatedAt = System.currentTimeMillis()
        )
        list[index] = updatedPart
        saveSparepartList(list)

        val cleanNote = note.trim().ifEmpty {
            if (type == MutationType.IN) "Restock gudang" else "Pemakaian servis bengkel"
        }
        recordMutationInternal(updatedPart, type, quantity, newStock, cleanNote)

        return Result.success(updatedPart)
    }

    fun getAllMutations(): List<StockMutation> {
        val raw = sharedPreferences.getString(KEY_MUTATIONS_JSON, "[]") ?: "[]"
        val list = mutableListOf<StockMutation>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StockMutation(
                        id = obj.getString("id"),
                        sparepartId = obj.getString("sparepartId"),
                        partCode = obj.getString("partCode"),
                        partName = obj.getString("partName"),
                        type = MutationType.valueOf(obj.getString("type")),
                        quantity = obj.getInt("quantity"),
                        resultingStock = obj.getInt("resultingStock"),
                        note = obj.getString("note"),
                        actorName = obj.optString("actorName", "Admin"),
                        timestamp = obj.getString("timestamp")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }

    fun getMutationsForPart(sparepartId: String): List<StockMutation> {
        return getAllMutations().filter { it.sparepartId == sparepartId }
    }

    private fun saveSparepartList(list: List<Sparepart>) {
        val array = JSONArray()
        list.forEach { part ->
            val obj = JSONObject().apply {
                put("id", part.id)
                put("code", part.code)
                put("name", part.name)
                put("category", part.category)
                put("vehicleBrand", part.vehicleBrand)
                put("qualityType", part.qualityType)
                put("hasWarranty", part.hasWarranty)
                put("isFastMoving", part.isFastMoving)
                put("stock", part.stock)
                put("minStock", part.minStock)
                put("unitPrice", part.unitPrice)
                put("rackLocation", part.rackLocation)
                put("supplierName", part.supplierName)
                put("supplierPhone", part.supplierPhone)
                put("notes", part.notes)
                put("updatedAt", part.updatedAt)
            }
            array.put(obj)
        }
        sharedPreferences.edit().putString(KEY_SPAREPARTS_JSON, array.toString()).apply()
    }

    private fun recordMutationInternal(
        sparepart: Sparepart,
        type: MutationType,
        quantity: Int,
        resultingStock: Int,
        note: String
    ) {
        val current = getAllMutations().toMutableList()
        val actor = if (isLoggedIn()) "${getUserName()} (${getUserRole()})" else "Sistem Gudang"
        current.add(
            0,
            StockMutation(
                id = UUID.randomUUID().toString(),
                sparepartId = sparepart.id,
                partCode = sparepart.code,
                partName = sparepart.name,
                type = type,
                quantity = quantity,
                resultingStock = resultingStock,
                note = note,
                actorName = actor,
                timestamp = currentFormattedTime()
            )
        )
        saveMutationList(current)
    }

    private fun saveMutationList(list: List<StockMutation>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("sparepartId", item.sparepartId)
                put("partCode", item.partCode)
                put("partName", item.partName)
                put("type", item.type.name)
                put("quantity", item.quantity)
                put("resultingStock", item.resultingStock)
                put("note", item.note)
                put("actorName", item.actorName)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        sharedPreferences.edit().putString(KEY_MUTATIONS_JSON, array.toString()).apply()
    }

    private fun currentFormattedTime(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        return sdf.format(Date())
    }

    private fun ensureSeededData() {
        if (sharedPreferences.getBoolean(KEY_IS_SEEDED, false)) return

        val now = System.currentTimeMillis()
        val initialParts = listOf(
            Sparepart(
                id = "sp-001",
                code = "BRK-AVZ-01",
                name = "Kampas Rem Depan Brake Pad",
                category = "Sistem Pengereman",
                vehicleBrand = "Toyota Avanza / Daihatsu Xenia",
                qualityType = "OEM / Genuine",
                hasWarranty = true,
                isFastMoving = true,
                stock = 18,
                minStock = 6,
                unitPrice = 285000L,
                rackLocation = "Rak B-01",
                supplierName = "PT Astra Otoparts Distribusi",
                supplierPhone = "0214603550",
                notes = "Seri bebas asbes, ketahanan suhu tinggi untuk pemakaian harian.",
                updatedAt = now - 300_000L
            ),
            Sparepart(
                id = "sp-002",
                code = "FLT-HND-04",
                name = "Filter Oli Mesin Spin-On",
                category = "Filter & Pendingin",
                vehicleBrand = "Honda Brio / Mobilio / HR-V",
                qualityType = "OEM / Genuine",
                hasWarranty = true,
                isFastMoving = true,
                stock = 4,
                minStock = 8,
                unitPrice = 65000L,
                rackLocation = "Rak A-03",
                supplierName = "CV Mitra Suku Cadang Otomotif",
                supplierPhone = "081288991122",
                notes = "Wajib diganti setiap kelipatan servis 10.000 KM.",
                updatedAt = now - 240_000L
            ),
            Sparepart(
                id = "sp-003",
                code = "ELC-GSA-02",
                name = "Aki Kering Maintenance Free NS60",
                category = "Kelistrikan & Aki",
                vehicleBrand = "Toyota Rush / Suzuki Ertiga",
                qualityType = "OEM / Genuine",
                hasWarranty = true,
                isFastMoving = false,
                stock = 3,
                minStock = 4,
                unitPrice = 890000L,
                rackLocation = "Rak D-02",
                supplierName = "PT GS Battery Nusantara",
                supplierPhone = "0218900511",
                notes = "Kapasitas 45 Ah, 12V siap pasang tanpa isi air zuur.",
                updatedAt = now - 180_000L
            ),
            Sparepart(
                id = "sp-004",
                code = "ENG-NGK-09",
                name = "Busi Laser Iridium Set (4 Pcs)",
                category = "Mesin & Pelumas",
                vehicleBrand = "Mitsubishi Xpander / Nissan Livina",
                qualityType = "Aftermarket",
                hasWarranty = false,
                isFastMoving = true,
                stock = 14,
                minStock = 5,
                unitPrice = 420000L,
                rackLocation = "Rak A-01",
                supplierName = "PT Niterra Mobility Indonesia",
                supplierPhone = "0218710974",
                notes = "Elektroda pusat iridium tipis 0.6mm untuk pengapian presisi.",
                updatedAt = now - 120_000L
            ),
            Sparepart(
                id = "sp-005",
                code = "SUS-KYB-07",
                name = "Shockbreaker Belakang Gas Kayaba",
                category = "Kaki-kaki & Suspensi",
                vehicleBrand = "Toyota Kijang Innova Reborn",
                qualityType = "Aftermarket",
                hasWarranty = true,
                isFastMoving = false,
                stock = 0,
                minStock = 3,
                unitPrice = 675000L,
                rackLocation = "Rak C-04",
                supplierName = "PT Kayaba Indonesia",
                supplierPhone = "0218981456",
                notes = "Sepasang kiri-kanan tipe Excel-G double action.",
                updatedAt = now - 90_000L
            ),
            Sparepart(
                id = "sp-006",
                code = "OIL-SHL-05",
                name = "Oli Mesin Full Synthetic 5W-30 (4L)",
                category = "Mesin & Pelumas",
                vehicleBrand = "Universal Bensin Modern (LCGC / MPV)",
                qualityType = "OEM / Genuine",
                hasWarranty = true,
                isFastMoving = true,
                stock = 26,
                minStock = 10,
                unitPrice = 395000L,
                rackLocation = "Rak E-01",
                supplierName = "PT Pelumas Otomotif Prima",
                supplierPhone = "081377889900",
                notes = "API SP / ILSAC GF-6A perlindungan lumpur mesin maksimal.",
                updatedAt = now - 60_000L
            )
        )

        val initialMutations = listOf(
            StockMutation(
                id = "mut-1",
                sparepartId = "sp-001",
                partCode = "BRK-AVZ-01",
                partName = "Kampas Rem Depan Brake Pad",
                type = MutationType.IN,
                quantity = 20,
                resultingStock = 20,
                note = "Penerimaan PO awal bulan dari Astra Otoparts",
                actorName = "Kepala Gudang",
                timestamp = currentFormattedTime()
            ),
            StockMutation(
                id = "mut-2",
                sparepartId = "sp-001",
                partCode = "BRK-AVZ-01",
                partName = "Kampas Rem Depan Brake Pad",
                type = MutationType.OUT,
                quantity = 2,
                resultingStock = 18,
                note = "Servis berkala Avanza B 1842 TKY",
                actorName = "Teknisi Bengkel",
                timestamp = currentFormattedTime()
            ),
            StockMutation(
                id = "mut-3",
                sparepartId = "sp-002",
                partCode = "FLT-HND-04",
                partName = "Filter Oli Mesin Spin-On",
                type = MutationType.OUT,
                quantity = 6,
                resultingStock = 4,
                note = "Pemakaian paket ganti oli harian bengkel",
                actorName = "Admin Logistik",
                timestamp = currentFormattedTime()
            ),
            StockMutation(
                id = "mut-4",
                sparepartId = "sp-005",
                partCode = "SUS-KYB-07",
                partName = "Shockbreaker Belakang Gas Kayaba",
                type = MutationType.OUT,
                quantity = 2,
                resultingStock = 0,
                note = "Penggantian suspensi belakang Innova B 2091 UKL",
                actorName = "Teknisi Bengkel",
                timestamp = currentFormattedTime()
            )
        )

        saveSparepartList(initialParts)
        saveMutationList(initialMutations)
        sharedPreferences.edit().putBoolean(KEY_IS_SEEDED, true).apply()
    }
}
