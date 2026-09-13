package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "exchange_rates")
data class RateEntity(
    @PrimaryKey val id: Int = 1,
    val usdToVes: Double,
    val eurToVes: Double,
    val timestamp: Long
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM exchange_rates WHERE id = 1 LIMIT 1")
    fun getCachedRatesFlow(): Flow<RateEntity?>

    @Query("SELECT * FROM exchange_rates WHERE id = 1 LIMIT 1")
    suspend fun getCachedRates(): RateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: RateEntity)

    // History queries
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getHistoryFlow(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Int)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}

@Database(entities = [RateEntity::class, HistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun currencyDao(): CurrencyDao
}
