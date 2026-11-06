package uz.onetouch.home.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Состояние умного дома.
 *
 * Сейчас это локальная демо-модель: экраны работают, состояние сохраняется,
 * но реальные устройства не управляются. Чтобы подключить настоящие устройства
 * (Home Assistant, Tuya, Zigbee-шлюз), достаточно реализовать [HomeBackend]
 * и передать его в [SmartHome] — интерфейс лаунчера менять не нужно.
 */
interface HomeBackend {
    fun apply(state: HomeState) {}
}

data class Device(val id: String, val name: String, val on: Boolean, val note: String? = null)

data class HomeState(
    val roomName: String = "Гостиная",
    val brightness: Int = 70,
    /** 0 — тёплый (2700 K), 1 — холодный (6500 K). */
    val colorTemp: Float = 0.22f,
    val roomScene: String = "Вечер",
    val climateTemp: Int = 22,
    val humidity: Int = 41,
    val curtainsClosed: Boolean = true,
    val securityOn: Boolean = true,
    val devices: List<Device> = listOf(
        Device("chandelier", "Люстра", true),
        Device("lamp", "Торшер", true),
        Device("tvlight", "Подсветка ТВ", false),
        Device("ac", "Кондиционер", true, "22° · авто"),
    ),
) {
    val kelvin: Int get() = (2700 + colorTemp * 3800).toInt() / 100 * 100
    val tempName: String get() = when {
        colorTemp < 0.34f -> "Тёплый"
        colorTemp < 0.67f -> "Нейтральный"
        else -> "Холодный"
    }
    val lightOn: Boolean get() = brightness > 0
}

data class Scenario(
    val id: String,
    val title: String,
    val description: String,
    val trigger: String,
    val icon: String,
)

class SmartHome(context: Context, private val backend: HomeBackend = object : HomeBackend {}) {
    private val sp = context.getSharedPreferences("home", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    val scenarios = listOf(
        Scenario("night", "Спокойной ночи", "Свет, шторы, охрана, режим «Ночь»", "23:30 · будни", "moon"),
        Scenario("morning", "Доброе утро", "Рассвет, шторы, плейлист на кухне", "07:00 · с будильником", "sun"),
        Scenario("leave", "Ухожу из дома", "Всё выключить, охрана, камеры на запись", "Последний ушёл", "exit"),
        Scenario("cinema", "Кино", "Свет 10%, шторы, звук на телевизоре", "Голосом", "tv"),
        Scenario("cleaning", "Уборка дома", "Свет 100%, шторы открыть", "Вручную", "home"),
        Scenario("guests", "Гости", "Свет 80%, тёплый, музыка в гостиной", "Вручную", "users"),
    )

    fun update(block: (HomeState) -> HomeState) {
        val s = block(_state.value)
        _state.value = s
        save(s)
        backend.apply(s)
    }

    fun setDevice(id: String, on: Boolean) = update { st ->
        st.copy(devices = st.devices.map { if (it.id == id) it.copy(on = on) else it })
    }

    fun setBrightness(v: Int) = update { st ->
        val b = v.coerceIn(0, 100)
        st.copy(
            brightness = b,
            devices = st.devices.map { if (it.id == "chandelier" || it.id == "lamp") it.copy(on = b > 0) else it }
        )
    }

    fun roomScene(name: String) = update {
        when (name) {
            "Вечер" -> it.copy(roomScene = name, brightness = 45, colorTemp = 0.15f)
            "Кино" -> it.copy(roomScene = name, brightness = 10, colorTemp = 0.1f, curtainsClosed = true)
            "Чтение" -> it.copy(roomScene = name, brightness = 85, colorTemp = 0.55f)
            "Гости" -> it.copy(roomScene = name, brightness = 80, colorTemp = 0.3f)
            else -> it.copy(roomScene = name)
        }
    }

    /** Возвращает короткое описание выполненного — для голосового ответа и подсказки. */
    fun runScenario(id: String): String {
        when (id) {
            "night" -> update { st ->
                st.copy(brightness = 0, curtainsClosed = true, securityOn = true,
                    devices = st.devices.map { it.copy(on = it.id == "ac") })
            }
            "morning" -> update { it.copy(brightness = 60, colorTemp = 0.35f, curtainsClosed = false, securityOn = false) }
            "leave" -> update { st ->
                st.copy(brightness = 0, securityOn = true, devices = st.devices.map { it.copy(on = false) })
            }
            "cinema" -> update { st ->
                st.copy(brightness = 10, colorTemp = 0.1f, curtainsClosed = true, roomScene = "Кино",
                    devices = st.devices.map { if (it.id == "tvlight") it.copy(on = true) else it })
            }
            "cleaning" -> update { it.copy(brightness = 100, colorTemp = 0.7f, curtainsClosed = false) }
            "guests" -> update { it.copy(brightness = 80, colorTemp = 0.3f, roomScene = "Гости") }
        }
        return scenarios.firstOrNull { it.id == id }?.let { "Сценарий «${it.title}»" } ?: ""
    }

    private fun save(s: HomeState) {
        val js = JSONObject()
            .put("b", s.brightness).put("ct", s.colorTemp.toDouble()).put("scene", s.roomScene)
            .put("curt", s.curtainsClosed).put("sec", s.securityOn)
        s.devices.forEach { js.put("d_${it.id}", it.on) }
        sp.edit().putString("state", js.toString()).apply()
    }

    private fun load(): HomeState {
        val def = HomeState()
        val raw = sp.getString("state", null) ?: return def
        return runCatching {
            val js = JSONObject(raw)
            def.copy(
                brightness = js.optInt("b", def.brightness),
                colorTemp = js.optDouble("ct", def.colorTemp.toDouble()).toFloat(),
                roomScene = js.optString("scene", def.roomScene),
                curtainsClosed = js.optBoolean("curt", def.curtainsClosed),
                securityOn = js.optBoolean("sec", def.securityOn),
                devices = def.devices.map { it.copy(on = js.optBoolean("d_${it.id}", it.on)) },
            )
        }.getOrDefault(def)
    }
}
