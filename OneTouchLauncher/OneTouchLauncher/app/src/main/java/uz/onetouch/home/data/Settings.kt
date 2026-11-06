package uz.onetouch.home.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val title: String) { AUTO("Авто"), DARK("Тёмная"), LIGHT("Светлая") }

enum class SaverStyle(val title: String) {
    AUTO("Авто"), ORBITS("Орбиты"), LIGHT("Свет"), MUSIC("Музыка"), NIGHT("Ночь")
}

data class City(val name: String, val lat: Double, val lon: Double)

val Cities = listOf(
    City("Ташкент", 41.2995, 69.2401),
    City("Самарканд", 39.6542, 66.9597),
    City("Бухара", 39.7681, 64.4556),
    City("Наманган", 40.9983, 71.6726),
    City("Андижан", 40.7821, 72.3442),
    City("Фергана", 40.3864, 71.7864),
    City("Нукус", 42.4531, 59.6103),
    City("Москва", 55.7558, 37.6173),
)

data class LauncherSettings(
    val theme: ThemeMode = ThemeMode.AUTO,
    val saver: SaverStyle = SaverStyle.AUTO,
    /** 0 — заставка выключена. */
    val saverMinutes: Int = 2,
    val cityIndex: Int = 0,
    val residents: List<String> = listOf("М", "А", "Д"),
    /** Затемнять экран ночью (заставка «Ночь»). */
    val dimAtNight: Boolean = true,
    /** Скрытые из списка приложений пакеты. */
    val hidden: Set<String> = emptySet(),
) {
    val city: City get() = Cities.getOrElse(cityIndex) { Cities[0] }
}

class SettingsStore(context: Context) {
    private val sp = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<LauncherSettings> = _state.asStateFlow()

    private fun read() = LauncherSettings(
        theme = runCatching { ThemeMode.valueOf(sp.getString("theme", "AUTO")!!) }.getOrDefault(ThemeMode.AUTO),
        saver = runCatching { SaverStyle.valueOf(sp.getString("saver", "AUTO")!!) }.getOrDefault(SaverStyle.AUTO),
        saverMinutes = sp.getInt("saverMinutes", 2),
        cityIndex = sp.getInt("city", 0),
        residents = (sp.getString("residents", "М,А,Д") ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() },
        dimAtNight = sp.getBoolean("dim", true),
        hidden = sp.getStringSet("hidden", emptySet())?.toSet() ?: emptySet(),
    )

    fun update(block: (LauncherSettings) -> LauncherSettings) {
        val s = block(_state.value)
        sp.edit()
            .putString("theme", s.theme.name)
            .putString("saver", s.saver.name)
            .putInt("saverMinutes", s.saverMinutes)
            .putInt("city", s.cityIndex)
            .putString("residents", s.residents.joinToString(","))
            .putBoolean("dim", s.dimAtNight)
            .putStringSet("hidden", s.hidden)
            .apply()
        _state.value = s
    }
}
