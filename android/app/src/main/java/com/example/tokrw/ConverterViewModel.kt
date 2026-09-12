package com.example.tokrw

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ConverterState(
    val currency: Currency = Currencies.default,
    val amountInput: String = "0",
    val snapshot: RateSnapshot? = null,
    val loading: Boolean = false,
    val error: String? = null,
) {
    val amount: Double get() = amountInput.toDoubleOrNull() ?: 0.0

    /** 1 [currency] 당 원화. 아직 환율이 없거나 지원하지 않는 통화면 null. */
    val krwPerUnit: Double? get() = snapshot?.krwPerUnit(currency.code)

    val convertedKrw: Double? get() = krwPerUnit?.let { it * amount }

    val unsupported: Boolean
        get() = snapshot != null && !snapshot.supports(currency.code)
}

class ConverterViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = RateRepository(app)

    private val _state = MutableStateFlow(ConverterState(snapshot = repository.cached()))
    val state: StateFlow<ConverterState> = _state.asStateFlow()

    init {
        // 저장해 둔 값이 없거나 하루 넘게 묵었으면 바로 새로 받아 온다.
        // 공급처가 하루에 한 번 값을 올리기 때문에 그보다 자주 받을 이유가 없다.
        val cachedAt = _state.value.snapshot?.fetchedAtMillis ?: 0L
        if (System.currentTimeMillis() - cachedAt > AUTO_REFRESH_INTERVAL_MILLIS) refresh()
    }

    fun refresh() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.refresh() }
                .onSuccess { fresh -> _state.update { it.copy(snapshot = fresh, loading = false) } }
                .onFailure { t -> _state.update { it.copy(loading = false, error = t.message) } }
        }
    }

    fun selectCurrency(currency: Currency) {
        _state.update { it.copy(currency = currency) }
    }

    fun onDigit(digit: Char) {
        _state.update { it.copy(amountInput = appendDigit(it.amountInput, digit)) }
    }

    fun onDecimalPoint() {
        _state.update {
            val current = it.amountInput
            if (current.contains('.')) it else it.copy(amountInput = "$current.")
        }
    }

    fun onBackspace() {
        _state.update {
            val dropped = it.amountInput.dropLast(1)
            it.copy(amountInput = if (dropped.isEmpty()) "0" else dropped)
        }
    }

    fun onClear() {
        _state.update { it.copy(amountInput = "0") }
    }

    private fun appendDigit(current: String, digit: Char): String {
        if (current == "0") return digit.toString()

        val decimals = current.substringAfter('.', "")
        if (current.contains('.') && decimals.length >= MAX_DECIMALS) return current

        val digitCount = current.count { it.isDigit() }
        if (digitCount >= MAX_DIGITS) return current

        return current + digit
    }

    private companion object {
        const val MAX_DIGITS = 12
        const val MAX_DECIMALS = 2
        val AUTO_REFRESH_INTERVAL_MILLIS = TimeUnit.DAYS.toMillis(1)
    }
}

/** 키패드로 찍는 중인 값을 그대로 보여 준다. "1234." 처럼 입력 중인 소수점도 살린다. */
fun formatAmountInput(input: String): String {
    val hasTrailingDot = input.endsWith(".")
    val integerPart = input.substringBefore('.')
    val decimals = input.substringAfter('.', "")

    val grouped = DecimalFormat("#,##0").format(integerPart.toLongOrNull() ?: 0L)
    return when {
        decimals.isNotEmpty() -> "$grouped.$decimals"
        hasTrailingDot -> "$grouped."
        else -> grouped
    }
}

/** 원화는 소수점을 버리고 천 단위로 끊어 보여 준다. */
fun formatKrw(value: Double): String = DecimalFormat("#,##0").format(Math.round(value))

/** 환율처럼 크기를 모르는 값은 자릿수에 맞춰 소수점 개수를 정한다. */
fun formatRate(value: Double): String {
    val pattern = when {
        value >= 1000 -> "#,##0.00"
        value >= 1 -> "#,##0.0000"
        else -> "#,##0.000000"
    }
    return DecimalFormat(pattern).format(value)
}

fun formatUpdatedAt(millis: Long): String {
    if (millis <= 0L) return "-"
    val elapsed = System.currentTimeMillis() - millis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
    return when {
        minutes < 1 -> "방금 전 기준"
        minutes < 60 -> "${minutes}분 전 기준"
        minutes < 60 * 24 -> "${minutes / 60}시간 전 기준"
        else -> String.format(Locale.KOREA, "%d일 전 기준", minutes / (60 * 24))
    }
}
