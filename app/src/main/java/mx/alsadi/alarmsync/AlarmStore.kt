package mx.alsadi.alarmsync

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AlarmStore {
    private const val PREF = "alsadi_estado"
    private const val KEY_LOG = "eventos"
    fun enabled(ctx: Context): Boolean = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean("activo", false)
    fun setEnabled(ctx: Context, value: Boolean) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean("activo", value).apply()
    fun event(ctx: Context, message: String) {
        val prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("es", "MX")).format(Date())
        val lines = prefs.getString(KEY_LOG, "").orEmpty().lines().filter { it.isNotBlank() }
        val next = (listOf("$stamp | $message") + lines).take(120).joinToString("\n")
        prefs.edit().putString(KEY_LOG, next).apply()
    }
    fun history(ctx: Context): String = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_LOG, "Sin eventos todavía.").orEmpty()
    fun clear(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY_LOG).apply()
}
