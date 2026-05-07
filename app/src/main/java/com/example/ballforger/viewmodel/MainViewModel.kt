package com.example.ballforger.viewmodel

import android.content.Context
import android.util.Base64
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ballforger.data.*
import com.example.ballforger.utils.uriToCompressedByteArray
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.ballforger.utils.urlToCompressedByteArray

class MainViewModel(application: Application, private val dao: AppDao) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private val _currentCategory = MutableStateFlow(ItemCategory.BALL)
    val currentCategory: StateFlow<ItemCategory> = _currentCategory

    val allItems: StateFlow<List<AppItem>> = _currentCategory
        .flatMapLatest { category -> dao.getItemsByCategory(category) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allCombinations: StateFlow<List<Combination>> = dao.getAllCombinations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setCategory(category: ItemCategory) {
        _currentCategory.value = category
    }

    fun saveItem(
        id: String? = null, // Có ID thì là Update, Null thì là Add New
        name: String,
        description: String,
        imageUri: Uri?,
        imageUrl: String,
        existingIconData: ByteArray?, // Dữ liệu ảnh cũ khi Edit
        tier: ItemTier,
        slots: List<ComboSlot>
    ) {
        viewModelScope.launch {
            // Xác định ảnh mới hay ảnh cũ
            val imageByteArray = if (imageUri != null) {
                uriToCompressedByteArray(context, imageUri)
            } else if (imageUrl.isNotBlank()) {
                urlToCompressedByteArray(imageUrl)
            } else {
                existingIconData
            }

            if (imageByteArray == null) return@launch

            val itemId = id ?: java.util.UUID.randomUUID().toString()
            val newItem = AppItem(
                id = itemId,
                name = name,
                description = description,
                iconData = imageByteArray,
                category = _currentCategory.value,
                tier = tier
            )

            // Insert với onConflict = REPLACE sẽ tự động Update nếu trùng ID
            dao.insertItem(newItem)

            if (tier == ItemTier.EVOLVED && slots.isNotEmpty()) {
                dao.insertCombination(Combination(evolvedItemId = newItem.id, slots = slots))
            }
        }
    }

    fun deleteItem(item: AppItem) {
        viewModelScope.launch { dao.deleteItem(item) }
    }

    fun getItemById(id: String): AppItem? = allItems.value.find { it.id == id }
    // ==========================================
    // LOGIC EXPORT / IMPORT / PULL
    // ==========================================

    // Class khuôn mẫu để gom dữ liệu thành file JSON
    data class AppItemExport(
        val id: String, val name: String, val description: String,
        val iconDataBase64: String?, val category: ItemCategory, val tier: ItemTier
    )
    data class AppDataExport(
        val items: List<AppItemExport>,
        val combinations: List<Combination>
    )

    // 1. EXPORT
    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val items = dao.getAllItemsList()
                val combos = dao.getAllCombinationsList()

                // Mã hóa ByteArray thành chuỗi Base64
                val exportItems = items.map {
                    AppItemExport(it.id, it.name, it.description, Base64.encodeToString(it.iconData, Base64.NO_WRAP), it.category, it.tier)
                }

                val json = Gson().toJson(AppDataExport(exportItems, combos))
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }

                withContext(Dispatchers.Main) { Toast.makeText(context, "Export thành công!", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { Toast.makeText(context, "Lỗi Export: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // 2. IMPORT TỪ FILE
    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (json != null) {
                    processImportJson(json, context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { Toast.makeText(context, "Lỗi Import: File không hợp lệ", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // 3. PULL TỪ GITHUB
    fun pullDataFromGithub(context: Context, urlString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show() }

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val json = connection.inputStream.bufferedReader().use { it.readText() }
                processImportJson(json, context)

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { Toast.makeText(context, "Lỗi Pull Data: Vui lòng kiểm tra link mạng", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // Hàm dùng chung để giải mã JSON và lưu vào Database
    private suspend fun processImportJson(json: String, context: Context) {
        try {
            val exportData = Gson().fromJson(json, AppDataExport::class.java)

            // Giải mã Base64 ngược lại thành ByteArray
            val itemsToInsert = exportData.items.mapNotNull { exportItem ->
                val byteArray = exportItem.iconDataBase64?.let { Base64.decode(it, Base64.NO_WRAP) }
                if (byteArray != null) {
                    AppItem(exportItem.id, exportItem.name, exportItem.description, byteArray, exportItem.category, exportItem.tier)
                } else null
            }

            dao.insertItems(itemsToInsert)
            dao.insertCombinations(exportData.combinations)

            withContext(Dispatchers.Main) { Toast.makeText(context, "Nhập dữ liệu thành công!", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            throw e
        }
    }
}