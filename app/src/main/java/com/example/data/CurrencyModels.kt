package com.example.data

enum class CurrencyType(val code: String, val symbol: String, val label: String) {
    USD("USD", "$", "USD (Dólar)"),
    EUR("EUR", "€", "EUR (Euro)"),
    VES("VES", "Bs", "VES (Bolívar)")
}

sealed class CalcToken {
    abstract val id: String

    data class NumberToken(
        override val id: String,
        val valueString: String,
        val currency: CurrencyType = CurrencyType.USD
    ) : CalcToken() {
        val parsedValue: Double
            get() = valueString.replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    data class OperatorToken(
        override val id: String,
        val operator: String // "+", "-", "×", "÷"
    ) : CalcToken()
}
