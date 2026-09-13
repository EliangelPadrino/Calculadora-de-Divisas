package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class CurrencyRepository(private val database: AppDatabase) {
    private val currencyDao = database.currencyDao()

    // Default rates to fall back on if nothing is in db and network fails
    val defaultUsdToVes = 40.25
    val defaultEurToVes = 43.15

    val cachedRatesFlow: Flow<RateEntity?> = currencyDao.getCachedRatesFlow()
    val historyFlow: Flow<List<HistoryEntity>> = currencyDao.getHistoryFlow()

    private val api: ExchangeRateApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://open.er-api.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ExchangeRateApi::class.java)
    }

    suspend fun fetchAndCacheLatestRates(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.getUsdRates()
            if (response.result == "success") {
                val usdRates = response.rates
                val usdToVes = usdRates["VES"] ?: defaultUsdToVes
                val usdToEur = usdRates["EUR"] ?: 0.923 // Fallback factor
                val eurToVes = if (usdToEur > 0.0) usdToVes / usdToEur else defaultEurToVes

                val ratesToSave = RateEntity(
                    id = 1,
                    usdToVes = usdToVes,
                    eurToVes = eurToVes,
                    timestamp = System.currentTimeMillis()
                )
                currencyDao.insertRates(ratesToSave)
                Log.d("CurrencyRepository", "Exchange rates fetched and cached: USD=$usdToVes, EUR=$eurToVes")
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e("CurrencyRepository", "Error fetching exchange rates from API, using cached: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun getExchangeRates(): RateEntity {
        return withContext(Dispatchers.IO) {
            val cached = currencyDao.getCachedRates()
            if (cached != null) {
                return@withContext cached
            } else {
                return@withContext RateEntity(
                    id = 1,
                    usdToVes = defaultUsdToVes,
                    eurToVes = defaultEurToVes,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }

    suspend fun addHistoryItem(expression: String, result: String) = withContext(Dispatchers.IO) {
        currencyDao.insertHistoryItem(HistoryEntity(expression = expression, result = result))
    }

    suspend fun deleteHistoryItem(id: Int) = withContext(Dispatchers.IO) {
        currencyDao.deleteHistoryItem(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        currencyDao.clearHistory()
    }
}
