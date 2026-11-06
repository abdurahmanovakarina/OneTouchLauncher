package uz.onetouch.home.data

import android.Manifest
import android.app.AlarmManager
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AgendaItem(val time: String, val title: String, val start: Long, val isAlarm: Boolean = false)

data class Agenda(
    /** «Сегодня» или «Завтра». */
    val heading: String,
    val items: List<AgendaItem>,
    /** Ближайшее событие для чипа под погодой. */
    val next: AgendaItem?,
    val nextAlarm: String?,
    val calendarAllowed: Boolean,
)

/** События из системного календаря планшета + ближайший будильник. */
object AgendaRepo {
    private val hm = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun load(context: Context): Agenda = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now()
        // После 20:00 показываем план на завтра, как в макете.
        val showTomorrow = now.hour >= 20
        val day: LocalDate = if (showTomorrow) now.toLocalDate().plusDays(1) else now.toLocalDate()
        val from = if (showTomorrow) day.atStartOfDay(zone).toInstant().toEpochMilli() else System.currentTimeMillis()
        val to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val allowed = context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val events = if (allowed) queryEvents(context, from, to, zone) else emptyList()

        val am = context.getSystemService(AlarmManager::class.java)
        val alarm = am?.nextAlarmClock?.triggerTime
        val alarmText = alarm?.let { hm.format(Instant.ofEpochMilli(it).atZone(zone)) }
        val alarmItem = alarm?.takeIf { it in from until to }?.let {
            AgendaItem(alarmText!!, "Будильник", it, isAlarm = true)
        }

        val items = (listOfNotNull(alarmItem) + events).sortedBy { it.start }.take(4)
        val next = events.firstOrNull { it.start > System.currentTimeMillis() }
            ?: if (allowed) queryEvents(context, System.currentTimeMillis(), System.currentTimeMillis() + 2 * 86_400_000L, zone).firstOrNull() else null

        Agenda(
            heading = if (showTomorrow) "Завтра" else "Сегодня",
            items = items,
            next = next,
            nextAlarm = alarmText,
            calendarAllowed = allowed,
        )
    }

    private fun queryEvents(context: Context, from: Long, to: Long, zone: ZoneId): List<AgendaItem> {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from)
            ContentUris.appendId(it, to)
        }.build()
        val proj = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY,
        )
        val out = mutableListOf<AgendaItem>()
        runCatching {
            context.contentResolver.query(uri, proj, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(0) ?: continue
                    val begin = c.getLong(1)
                    val allDay = c.getInt(2) == 1
                    if (begin < from && !allDay) continue
                    val time = if (allDay) "Весь день" else hm.format(Instant.ofEpochMilli(begin).atZone(zone))
                    out += AgendaItem(time, title, begin)
                }
            }
        }
        return out
    }

    /** «Мастер на час — завтра в 10:00» */
    fun chipText(item: AgendaItem): String {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(item.start).atZone(zone).toLocalDateTime()
        val now = LocalDateTime.now()
        val minutes = java.time.Duration.between(now, start).toMinutes()
        val whenText = when {
            minutes in 0..59 -> "через $minutes мин"
            start.toLocalDate() == now.toLocalDate() -> "сегодня в ${hm.format(start)}"
            start.toLocalDate() == now.toLocalDate().plusDays(1) -> "завтра в ${hm.format(start)}"
            else -> DateTimeFormatter.ofPattern("d MMMM в HH:mm", java.util.Locale.forLanguageTag("ru")).format(start)
        }
        return "${item.title} — $whenText"
    }
}
