package uz.onetouch.home.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

data class HourSlot(val label: String, val temp: Int, val highlight: Boolean = false)

data class Weather(
    val temp: Int,
    val condition: String,
    val slots: List<HourSlot>,
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Погода из бесплатного Open-Meteo (без ключа API). */
object WeatherRepo {

    suspend fun load(city: City): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val url = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                    "&current=temperature_2m,weather_code,is_day&hourly=temperature_2m" +
                    "&timezone=auto&forecast_days=2",
                city.lat, city.lon
            )
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            parse(JSONObject(body))
        }.getOrNull()
    }

    private fun parse(js: JSONObject): Weather {
        val cur = js.getJSONObject("current")
        val temp = cur.getDouble("temperature_2m").roundToInt()
        val code = cur.getInt("weather_code")
        val isDay = cur.optInt("is_day", 1) == 1
        val hourly = js.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val now = LocalDateTime.now()
        val list = (0 until times.length()).map {
            LocalDateTime.parse(times.getString(it), fmt) to temps.getDouble(it).roundToInt()
        }
        val startIdx = list.indexOfFirst { !it.first.isBefore(now.withMinute(0).withSecond(0).withNano(0)) }.coerceAtLeast(0)

        val slots = mutableListOf(HourSlot("Сейчас", temp))
        for (k in 1..3) {
            list.getOrNull(startIdx + k * 3)?.let { (t, v) -> slots += HourSlot("%02d:00".format(t.hour), v) }
        }
        // Пятая колонка: днём — «Ночью» (минимум), вечером — «Днём» завтра (максимум).
        if (now.hour in 6..15) {
            val night = list.filter { it.first.isAfter(now) && (it.first.hour >= 22 || it.first.hour <= 6) }.take(9)
            night.minByOrNull { it.second }?.let { slots += HourSlot("Ночью", it.second) }
        } else {
            val tomorrow = now.toLocalDate().plusDays(if (now.hour >= 16) 1 else 0)
            val day = list.filter { it.first.toLocalDate() == tomorrow && it.first.hour in 10..17 }
            day.maxByOrNull { it.second }?.let { slots += HourSlot("Днём", it.second) }
        }
        val max = slots.maxOf { it.temp }
        val marked = slots.mapIndexed { i, s -> if (i > 0 && s.temp == max && max > temp) s.copy(highlight = true) else s }
        return Weather(temp, describe(code, isDay), marked)
    }

    fun describe(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "Солнечно" else "Ясно"
        1, 2 -> "Малооблачно"
        3 -> "Пасмурно"
        45, 48 -> "Туман"
        51, 53, 55, 56, 57 -> "Морось"
        61, 63, 65, 66, 67 -> "Дождь"
        71, 73, 75, 77 -> "Снег"
        80, 81, 82 -> "Ливень"
        85, 86 -> "Снегопад"
        95, 96, 99 -> "Гроза"
        else -> ""
    }
}
