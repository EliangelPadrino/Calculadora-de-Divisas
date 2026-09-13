package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class CalculatorViewModel(application: Application, private val repository: CurrencyRepository) : AndroidViewModel(application) {

    // Exchange rates in memory (state flow)
    private val _usdToVes = MutableStateFlow(repository.defaultUsdToVes)
    val usdToVes: StateFlow<Double> = _usdToVes.asStateFlow()

    private val _eurToVes = MutableStateFlow(repository.defaultEurToVes)
    val eurToVes: StateFlow<Double> = _eurToVes.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Default currency applied to newly typed number tokens
    val defaultInputCurrency = MutableStateFlow(CurrencyType.USD)

    // Current output/display target currency for the final calculator result
    private val _targetResultCurrency = MutableStateFlow(CurrencyType.VES)
    val targetResultCurrency: StateFlow<CurrencyType> = _targetResultCurrency.asStateFlow()

    // Active typed tokens in the current formula
    private val _activeTokens = MutableStateFlow<List<CalcToken>>(emptyList())
    val activeTokens: StateFlow<List<CalcToken>> = _activeTokens.asStateFlow()

    // Instant calculated result always maintained in Bolívares (VES)
    private val _instantResultVes = MutableStateFlow(0.0)
    val instantResultVes: StateFlow<Double> = _instantResultVes.asStateFlow()

    // Taped and long-pressed token tracking states
    private val _activeTapTokenId = MutableStateFlow<String?>(null)
    val activeTapTokenId: StateFlow<String?> = _activeTapTokenId.asStateFlow()

    private val _activeLongPressTokenId = MutableStateFlow<String?>(null)
    val activeLongPressTokenId: StateFlow<String?> = _activeLongPressTokenId.asStateFlow()

    // Font size scaling state for reduced sight users
    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    fun increaseFontScale() {
        if (_fontScale.value < 1.6f) {
            _fontScale.value = (_fontScale.value + 0.15f).coerceAtMost(1.6f)
        }
    }

    fun decreaseFontScale() {
        if (_fontScale.value > 0.7f) {
            _fontScale.value = (_fontScale.value - 0.15f).coerceAtLeast(0.7f)
        }
    }

    // Database conversion history
    val history: StateFlow<List<HistoryEntity>> = repository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Alerts Configuration (configurable by user, optional)
    private val _alertsEnabled = MutableStateFlow(true)
    val alertsEnabled: StateFlow<Boolean> = _alertsEnabled.asStateFlow()

    private val _usdUpperThreshold = MutableStateFlow(41.0)
    val usdUpperThreshold: StateFlow<Double> = _usdUpperThreshold.asStateFlow()

    private val _usdLowerThreshold = MutableStateFlow(39.0)
    val usdLowerThreshold: StateFlow<Double> = _usdLowerThreshold.asStateFlow()

    private val _eurUpperThreshold = MutableStateFlow(44.0)
    val eurUpperThreshold: StateFlow<Double> = _eurUpperThreshold.asStateFlow()

    private val _eurLowerThreshold = MutableStateFlow(42.0)
    val eurLowerThreshold: StateFlow<Double> = _eurLowerThreshold.asStateFlow()

    // Active visual notification message inside the app
    private val _activeNotification = MutableStateFlow<String?>(null)
    val activeNotification: StateFlow<String?> = _activeNotification.asStateFlow()

    // Simulated Offline Mode State (allows user to test offline response dynamically!)
    private val _isOfflineSimulated = MutableStateFlow(false)
    val isOfflineSimulated: StateFlow<Boolean> = _isOfflineSimulated.asStateFlow()

    // Timestamp tracking for of last rates synced/cached
    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    init {
        // Collect database-cached rates first, then request network sync
        viewModelScope.launch {
            repository.cachedRatesFlow.collect { cached ->
                cached?.let {
                    _usdToVes.value = it.usdToVes
                    _eurToVes.value = it.eurToVes
                    _lastSyncTimestamp.value = it.timestamp
                }
            }
        }
        syncRates()
    }

    fun syncRates() {
        viewModelScope.launch {
            _isSyncing.value = true
            val success = if (_isOfflineSimulated.value) {
                // If offline is simulated, don't ping the live network API
                false
            } else {
                repository.fetchAndCacheLatestRates()
            }
            val finalRates = repository.getExchangeRates()
            _usdToVes.value = finalRates.usdToVes
            _eurToVes.value = finalRates.eurToVes
            _lastSyncTimestamp.value = finalRates.timestamp
            _isSyncing.value = false
            recalculateInstantResult()
            checkAlerts(finalRates.usdToVes, finalRates.eurToVes)
        }
    }

    // Toggle simulated offline connection
    fun toggleOfflineSimulation(enabled: Boolean) {
        _isOfflineSimulated.value = enabled
        syncRates()
    }

    // Toggle alerts overall
    fun toggleAlerts(enabled: Boolean) {
        _alertsEnabled.value = enabled
        if (enabled) {
            checkAlerts(_usdToVes.value, _eurToVes.value)
        } else {
            _activeNotification.value = null
        }
    }

    // Update threshold bounds
    fun updateUsdBounds(upper: Double, lower: Double) {
        _usdUpperThreshold.value = upper
        _usdLowerThreshold.value = lower
        if (_alertsEnabled.value) {
            checkAlerts(_usdToVes.value, _eurToVes.value)
        }
    }

    fun updateEurBounds(upper: Double, lower: Double) {
        _eurUpperThreshold.value = upper
        _eurLowerThreshold.value = lower
        if (_alertsEnabled.value) {
            checkAlerts(_usdToVes.value, _eurToVes.value)
        }
    }

    // Direct triggering / simulation of a rate fluctuation to verify notifications!
    fun simulateRateJump(usd: Double, eur: Double) {
        _usdToVes.value = usd
        _eurToVes.value = eur
        recalculateInstantResult()
        checkAlerts(usd, eur)
    }

    fun dismissNotification() {
        _activeNotification.value = null
    }

    // Rate limit monitoring logic
    private fun checkAlerts(usd: Double, eur: Double) {
        if (!_alertsEnabled.value) return
        val alerts = mutableListOf<String>()

        if (usd > _usdUpperThreshold.value) {
            alerts.add("Dólar supera límite superior de ${_usdUpperThreshold.value} Bs. (Actual: ${formatValue(usd)} Bs.)")
        } else if (usd < _usdLowerThreshold.value) {
            alerts.add("Dólar cae por debajo de ${_usdLowerThreshold.value} Bs. (Actual: ${formatValue(usd)} Bs.)")
        }

        if (eur > _eurUpperThreshold.value) {
            alerts.add("Euro supera límite superior de ${_eurUpperThreshold.value} Bs. (Actual: ${formatValue(eur)} Bs.)")
        } else if (eur < _eurLowerThreshold.value) {
            alerts.add("Euro cae por debajo de ${_eurLowerThreshold.value} Bs. (Actual: ${formatValue(eur)} Bs.)")
        }

        if (alerts.isNotEmpty()) {
            _activeNotification.value = alerts.joinToString("\n\n")
        } else {
            _activeNotification.value = null
        }
    }

    // Date formatting helper for conversion logs
    fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "Reciente"
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    // Set the specific target display currency (VES, USD, EUR) on click of top buttons
    fun setTargetResultCurrency(currency: CurrencyType) {
        _targetResultCurrency.value = currency
    }

    // Add normal calculator inputs: digits, operators, triggers
    fun onDigitClicked(digit: String) {
        val currentList = _activeTokens.value.toMutableList()
        val defaultCurrency = defaultInputCurrency.value

        if (currentList.isEmpty() || currentList.last() is CalcToken.OperatorToken) {
            val lastOp = (currentList.lastOrNull() as? CalcToken.OperatorToken)?.operator
            if (lastOp == ")") {
                // If the user starts typing a digit immediately after a closed parenthesis, append custom operator first
                currentList.add(
                    CalcToken.OperatorToken(
                        id = UUID.randomUUID().toString(),
                        operator = "×"
                    )
                )
            }
            // Start a new number token
            currentList.add(
                CalcToken.NumberToken(
                    id = UUID.randomUUID().toString(),
                    valueString = if (digit == ",") "0," else digit,
                    currency = defaultCurrency
                )
            )
        } else {
            val lastNum = currentList.last() as CalcToken.NumberToken
            val updatedValue = if (digit == "," && lastNum.valueString.contains(",")) {
                lastNum.valueString // disallow multiple decimal separators
            } else {
                lastNum.valueString + digit
            }
            currentList[currentList.size - 1] = lastNum.copy(valueString = updatedValue)
        }
        _activeTokens.value = currentList
        recalculateInstantResult()
        clearInteractivePopups()
    }

    fun onOperatorClicked(op: String) {
        val currentList = _activeTokens.value.toMutableList()
        if (currentList.isEmpty()) return

        if (currentList.last() is CalcToken.OperatorToken) {
            val lastOp = (currentList.last() as CalcToken.OperatorToken).operator
            if (lastOp == "(" || lastOp == ")") {
                // Append instead of replace since parenthesis is a structural block
                currentList.add(
                    CalcToken.OperatorToken(
                        id = UUID.randomUUID().toString(),
                        operator = op
                    )
                )
            } else {
                // Replace existing basic operator with new one
                currentList[currentList.size - 1] = CalcToken.OperatorToken(
                    id = UUID.randomUUID().toString(),
                    operator = op
                )
            }
        } else {
            // Ensure last number is not empty
            val lastNum = currentList.last() as CalcToken.NumberToken
            if (lastNum.valueString.isNotEmpty()) {
                currentList.add(
                    CalcToken.OperatorToken(
                        id = UUID.randomUUID().toString(),
                        operator = op
                    )
                )
            }
        }
        _activeTokens.value = currentList
        recalculateInstantResult()
        clearInteractivePopups()
    }

    fun onParenthesisClicked(paren: String) {
        val currentList = _activeTokens.value.toMutableList()
        currentList.add(
            CalcToken.OperatorToken(
                id = UUID.randomUUID().toString(),
                operator = paren
            )
        )
        _activeTokens.value = currentList
        recalculateInstantResult()
        clearInteractivePopups()
    }

    fun onPercentClicked() {
        val currentList = _activeTokens.value.toMutableList()
        if (currentList.isEmpty()) return
        val lastIndex = currentList.size - 1
        val lastToken = currentList[lastIndex]
        if (lastToken is CalcToken.NumberToken) {
            val doubleValue = lastToken.parsedValue
            val percentValue = doubleValue / 100.0

            // Format to localized standard format with comma
            val formatted = String.format(Locale.US, "%.6f", percentValue).trimEnd('0')
            val valueString = if (formatted.endsWith(".")) {
                formatted.dropLast(1).replace(".", ",")
            } else {
                formatted.replace(".", ",")
            }

            currentList[lastIndex] = lastToken.copy(valueString = valueString)
            _activeTokens.value = currentList
            recalculateInstantResult()
            clearInteractivePopups()
        }
    }

    fun onDeleteClicked() {
        val currentList = _activeTokens.value.toMutableList()
        if (currentList.isEmpty()) return

        val lastIndex = currentList.size - 1
        val lastToken = currentList[lastIndex]

        if (lastToken is CalcToken.NumberToken) {
            if (lastToken.valueString.length > 1) {
                val updatedString = lastToken.valueString.dropLast(1)
                currentList[lastIndex] = lastToken.copy(valueString = updatedString)
            } else {
                currentList.removeAt(lastIndex)
            }
        } else {
            currentList.removeAt(lastIndex)
        }
        _activeTokens.value = currentList
        recalculateInstantResult()
        clearInteractivePopups()
    }

    fun onClearClicked() {
        _activeTokens.value = emptyList()
        _instantResultVes.value = 0.0
        clearInteractivePopups()
    }

    fun clearInteractivePopups() {
        _activeTapTokenId.value = null
        _activeLongPressTokenId.value = null
    }

    // Single Tap: show tooltip with the individual number's conversion in BS (Bolívares)
    fun onTokenTapped(tokenId: String) {
        _activeLongPressTokenId.value = null
        _activeTapTokenId.value = if (_activeTapTokenId.value == tokenId) null else tokenId
    }

    // Long Press: show selection dropdown/pill panel for another currency
    fun onTokenLongPressed(tokenId: String) {
        _activeTapTokenId.value = null
        _activeLongPressTokenId.value = if (_activeLongPressTokenId.value == tokenId) null else tokenId
    }

    // Alter currency type of a specific number token
    fun setTokenCurrency(tokenId: String, currency: CurrencyType) {
        val currentList = _activeTokens.value.map { token ->
            if (token is CalcToken.NumberToken && token.id == tokenId) {
                token.copy(currency = currency)
            } else {
                token
            }
        }
        _activeTokens.value = currentList
        _activeLongPressTokenId.value = null
        recalculateInstantResult()
    }

    // Equal button pressed: evaluates formula finally, stores it into Room history, and clears or sets result as current
    fun onEqualClicked() {
        val tokens = _activeTokens.value
        if (tokens.isEmpty()) return

        // Evaluate expression
        val finalVes = _instantResultVes.value
        val displayExpr = buildDisplayExpression(tokens)

        val targetCurr = _targetResultCurrency.value
        val finalResVal = when (targetCurr) {
            CurrencyType.VES -> finalVes
            CurrencyType.USD -> if (_usdToVes.value > 0.0) finalVes / _usdToVes.value else 0.0
            CurrencyType.EUR -> if (_eurToVes.value > 0.0) finalVes / _eurToVes.value else 0.0
        }
        val formattedResult = "${formatValue(finalResVal)} ${targetCurr.symbol}"

        viewModelScope.launch {
            repository.addHistoryItem(
                expression = displayExpr,
                result = formattedResult
            )
        }

        // Leave final result as a single token for continuation
        val resultString = String.format(Locale.US, "%.2f", finalResVal).replace(".", ",")
        _activeTokens.value = listOf(
            CalcToken.NumberToken(
                id = UUID.randomUUID().toString(),
                valueString = resultString,
                currency = targetCurr
            )
        )
        recalculateInstantResult()
        clearInteractivePopups()
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Recursive Descent Parser for robust Parentheses & implicit multiplication evaluations
    private class TokenParser(private val tokens: List<Any>) {
        private var pos = 0

        private fun peek(): Any? = if (pos < tokens.size) tokens[pos] else null
        private fun consume(): Any? = if (pos < tokens.size) tokens[pos++] else null

        fun parse(): Double {
            if (tokens.isEmpty()) return 0.0
            return try {
                expr()
            } catch (e: Exception) {
                0.0
            }
        }

        // expr = term (( "+" | "-" ) term)*
        private fun expr(): Double {
            var value = term()
            while (true) {
                val next = peek()
                if (next == "+" || next == "-") {
                    consume()
                    val op = next
                    val nextTerm = term()
                    if (op == "+") {
                        value += nextTerm
                    } else {
                        value -= nextTerm
                    }
                } else {
                    break
                }
            }
            return value
        }

        // term = factor (( "×" | "÷" ) factor)*
        private fun term(): Double {
            var value = factor()
            while (true) {
                val next = peek()
                if (next == "×" || next == "÷") {
                    consume()
                    val op = next
                    val nextFactor = factor()
                    if (op == "×") {
                        value *= nextFactor
                    } else {
                        if (nextFactor != 0.0) {
                            value /= nextFactor
                        } else {
                            value = 0.0
                        }
                    }
                } else {
                    break
                }
            }
            return value
        }

        // factor = NUMBER | "(" expr ")" | "-" factor | "+" factor
        private fun factor(): Double {
            val next = peek() ?: return 0.0
            if (next is Double) {
                consume()
                return next
            }
            if (next == "(") {
                consume() // Consume "("
                val value = expr()
                if (peek() == ")") {
                    consume() // Consume ")"
                }
                return value
            }
            if (next == "-") {
                consume()
                return -factor()
            }
            if (next == "+") {
                consume()
                return factor()
            }
            consume() // Safety sink
            return 0.0
        }
    }

    // Internal calculation engine
    private fun recalculateInstantResult() {
        val tokens = _activeTokens.value
        if (tokens.isEmpty()) {
            _instantResultVes.value = 0.0
            return
        }

        val usdRate = _usdToVes.value
        val eurRate = _eurToVes.value

        // Transform tokens to simplified tokens list, with values resolved to VES and implicit multiplications inserted
        val simplified = mutableListOf<Any>()
        for (token in tokens) {
            when (token) {
                is CalcToken.NumberToken -> {
                    val valueExpressed = token.parsedValue
                    val valueInVes = when (token.currency) {
                        CurrencyType.USD -> valueExpressed * usdRate
                        CurrencyType.EUR -> valueExpressed * eurRate
                        CurrencyType.VES -> valueExpressed
                    }
                    if (simplified.isNotEmpty() && (simplified.last() is Double || simplified.last() == ")")) {
                        simplified.add("×")
                    }
                    simplified.add(valueInVes)
                }
                is CalcToken.OperatorToken -> {
                    if (token.operator == "(" && simplified.isNotEmpty() && (simplified.last() is Double || simplified.last() == ")")) {
                        simplified.add("×")
                    }
                    simplified.add(token.operator)
                }
            }
        }

        val parser = TokenParser(simplified)
        _instantResultVes.value = parser.parse()
    }

    // Builds a user-friendly visual representation of the active calculation formula
    private fun buildDisplayExpression(tokens: List<CalcToken>): String {
        return tokens.joinToString(" ") { token ->
            when (token) {
                is CalcToken.NumberToken -> {
                    // Cuentas are printed without currency symbol inside calculations display
                    token.valueString
                }
                is CalcToken.OperatorToken -> {
                    token.operator
                }
            }
        }
    }

    // Custom formatting function to comply exactly with "Quiero que me expreses los decimales con comas, no utilices el punto para expresar decimales"
    // Also groups thousands with dot e.g., 1.234,56
    fun formatValue(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0,00"
        val parts = String.format(Locale.US, "%.2f", value).split(".")
        val integerPart = parts.getOrNull(0) ?: "0"
        val decimalPart = parts.getOrNull(1) ?: "00"

        val isNegative = integerPart.startsWith("-")
        val absInt = if (isNegative) integerPart.substring(1) else integerPart
        val formattedAbsInt = absInt.reversed().chunked(3).joinToString(".").reversed()
        val finalInt = if (isNegative) "-$formattedAbsInt" else formattedAbsInt

        return "$finalInt,$decimalPart"
    }

    fun getTokenValueInVes(token: CalcToken.NumberToken): Double {
        val value = token.parsedValue
        return when (token.currency) {
            CurrencyType.USD -> value * _usdToVes.value
            CurrencyType.EUR -> value * _eurToVes.value
            CurrencyType.VES -> value
        }
    }
}
