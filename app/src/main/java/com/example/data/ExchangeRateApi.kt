package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class ExchangeRateResponse(
    @Json(name = "result") val result: String,
    @Json(name = "rates") val rates: Map<String, Double>
)

interface ExchangeRateApi {
    @GET("v6/latest/USD")
    suspend fun getUsdRates(): ExchangeRateResponse
}
