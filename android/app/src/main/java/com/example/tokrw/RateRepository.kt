package com.example.tokrw

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * USD 를 기준으로 한 환율 표 한 벌.
 *
 * 통화 하나마다 따로 조회하지 않고 USD 기준 표를 한 번만 받아서 교차 환율을 계산한다.
 * 그래서 통화를 바꿔도 네트워크 호출이 다시 일어나지 않는다.
 */
data class RateSnapshot(
    val rates: Map<String, Double>,
    val fetchedAtMillis: Long,
    val providerLabel: String,
) {
    /** 1 [code] 가 몇 원인지. 표에 없는 통화면 null. */
    fun krwPerUnit(code: String): Double? {
        if (code == "KRW") return 1.0
        val krw = rates["KRW"] ?: return null
        val unit = rates[code] ?: return null
        if (unit <= 0.0) return null
        return krw / unit
    }

    fun supports(code: String): Boolean = code == "KRW" || rates.containsKey(code)
}

class RateRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 저장해 둔 마지막 결과. 앱을 켜자마자 보여줄 값이고, 오프라인일 때의 대비책이기도 하다. */
    fun cached(): RateSnapshot? {
        val json = prefs.getString(KEY_RATES, null) ?: return null
        val at = prefs.getLong(KEY_FETCHED_AT, 0L)
        val provider = prefs.getString(KEY_PROVIDER, "저장된 값") ?: "저장된 값"
        return runCatching { RateSnapshot(parseRates(JSONObject(json)), at, provider) }.getOrNull()
    }

    /**
     * 새 환율을 받아 온다. 1차 공급처가 실패하면 2차로 넘어가고,
     * 둘 다 실패하면 [IOException] 을 던진다.
     */
    suspend fun refresh(): RateSnapshot = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()

        for (provider in PROVIDERS) {
            try {
                val body = get(provider.url)
                val root = JSONObject(body)
                val table = provider.extract(root)
                val rates = parseRates(table)
                require(rates.containsKey("KRW")) { "응답에 KRW 가 없습니다" }

                val snapshot = RateSnapshot(rates, System.currentTimeMillis(), provider.label)
                prefs.edit()
                    .putString(KEY_RATES, table.toString())
                    .putLong(KEY_FETCHED_AT, snapshot.fetchedAtMillis)
                    .putString(KEY_PROVIDER, provider.label)
                    .apply()
                return@withContext snapshot
            } catch (t: Throwable) {
                errors += "${provider.label}: ${t.message ?: t::class.java.simpleName}"
            }
        }

        throw IOException("환율을 가져오지 못했습니다 (${errors.joinToString(" / ")})")
    }

    private fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRates(table: JSONObject): Map<String, Double> {
        val out = HashMap<String, Double>(table.length())
        val keys = table.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = table.optDouble(key, Double.NaN)
            if (!value.isNaN() && value > 0.0) out[key] = value
        }
        out["USD"] = 1.0
        return out
    }

    private class Provider(
        val label: String,
        val url: String,
        val extract: (JSONObject) -> JSONObject,
    )

    private companion object {
        const val PREFS = "tokrw_rates"
        const val KEY_RATES = "rates_json"
        const val KEY_FETCHED_AT = "fetched_at"
        const val KEY_PROVIDER = "provider"

        /** 둘 다 키가 필요 없는 무료 공개 API 다. 위에서부터 차례로 시도한다. */
        val PROVIDERS = listOf(
            Provider(
                label = "open.er-api.com",
                url = "https://open.er-api.com/v6/latest/USD",
                extract = { it.getJSONObject("rates") },
            ),
            Provider(
                label = "frankfurter.dev",
                url = "https://api.frankfurter.dev/v1/latest?base=USD",
                extract = { it.getJSONObject("rates") },
            ),
        )
    }
}
