package uz.onetouch.home.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

data class AppEntry(
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
)

/** Список приложений через LauncherApps; обновляется сам при установке/удалении. */
class AppsRepo(private val context: Context, private val scope: CoroutineScope) {
    private val la = context.getSystemService(LauncherApps::class.java)
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val iconPx = (context.resources.displayMetrics.density * 56).toInt().coerceAtLeast(96)

    private val cb = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = reload()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = reload()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = reload()
    }

    init {
        la?.registerCallback(cb, Handler(Looper.getMainLooper()))
        reload()
    }

    fun reload() {
        scope.launch(Dispatchers.IO) {
            val collator = Collator.getInstance(Locale.forLanguageTag("ru"))
            val list = runCatching {
                la!!.getActivityList(null, Process.myUserHandle())
            }.getOrDefault(emptyList())
                .filter { it.applicationInfo.packageName != context.packageName }
                .map { it.toEntry() }
                .sortedWith { a, b -> collator.compare(a.label, b.label) }
            _apps.value = list
        }
    }

    private fun LauncherActivityInfo.toEntry() = AppEntry(
        label = label.toString(),
        packageName = applicationInfo.packageName,
        component = componentName,
        user = user,
        icon = getBadgedIcon(0).toBitmap(iconPx, iconPx).asImageBitmap(),
    )

    fun launch(app: AppEntry) {
        runCatching { la?.startMainActivity(app.component, app.user, null, null) }
            .onFailure {
                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            }
    }

    fun openInfo(app: AppEntry) {
        runCatching { la?.startAppDetailsActivity(app.component, app.user, null, null) }
            .onFailure {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
    }

    /** Поиск по названию для голосовой команды «открой …». */
    fun find(query: String): AppEntry? {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return null
        val list = _apps.value
        return list.firstOrNull { it.label.lowercase() == q }
            ?: list.firstOrNull { it.label.lowercase().startsWith(q) }
            ?: list.firstOrNull { it.label.lowercase().contains(q) }
            ?: list.firstOrNull { q.contains(it.label.lowercase()) }
    }
}
