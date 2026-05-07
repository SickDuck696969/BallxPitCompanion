package com.example.ballforger.data

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

enum class ItemCategory { BALL, PASSIVE }
enum class ItemTier { BASE, EVOLVED }

data class ComboSlot(
    val firstMaterialId: String,
    val secondMaterialId: String? = null
)

@Entity(tableName = "items")
data class AppItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val iconData: ByteArray,
    val category: ItemCategory,
    val tier: ItemTier
)

@Entity(
    tableName = "combinations",
    foreignKeys = [
        ForeignKey(
            entity = AppItem::class,
            parentColumns = ["id"],
            childColumns = ["evolvedItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Combination(
    @PrimaryKey val evolvedItemId: String,
    val slots: List<ComboSlot>
)

class Converters {
    private val gson = Gson()
    @TypeConverter
    fun fromComboSlotList(value: List<ComboSlot>?): String = gson.toJson(value)
    @TypeConverter
    fun toComboSlotList(value: String): List<ComboSlot> {
        val type = object : TypeToken<List<ComboSlot>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: AppItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCombination(combination: Combination)

    @Query("SELECT * FROM items WHERE category = :category ORDER BY name ASC")
    fun getItemsByCategory(category: ItemCategory): kotlinx.coroutines.flow.Flow<List<AppItem>>

    @Query("SELECT * FROM combinations")
    fun getAllCombinations(): kotlinx.coroutines.flow.Flow<List<Combination>>
    @Delete
    suspend fun deleteItem(item: AppItem)

    @Query("SELECT * FROM items")
    suspend fun getAllItemsList(): List<AppItem>

    @Query("SELECT * FROM combinations")
    suspend fun getAllCombinationsList(): List<Combination>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<AppItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCombinations(combinations: List<Combination>)
}

@Database(entities = [AppItem::class, Combination::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}