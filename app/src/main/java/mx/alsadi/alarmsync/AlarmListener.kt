package mx.alsadi.alarmsync

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** No lee la base de datos de Samsung Clock: sólo notificaciones expuestas por Android. */
class AlarmListener : NotificationListenerService() {
    private val sourcePackages = setOf("com.sec.android.app.clockpackage", "com.google.android.deskclock")
    private val matching = mutableSetOf<String>()

    override fun onListenerConnected() { super.onListenerConnected(); AlarmStore.event(this, "Monitor conectado al sistema") }
    override fun onListenerDisconnected() { matching.clear(); AlarmStore.event(this, "Monitor desconectado") }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in sourcePackages || !AlarmStore.enabled(this)) return
        val n = sbn.notification
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val isAlarm = n.category == Notification.CATEGORY_ALARM ||
            (n.fullScreenIntent != null && n.category != Notification.CATEGORY_CALL)
        // Un encabezado tipo 'Próxima alarma' NO se interpreta como alarma en curso.
        if (!isAlarm) {
            AlarmStore.event(this, "Notificación observada (no verificada como alarma activa): ${sbn.packageName}: ${title.take(50)}")
            return
        }
        val wasEmpty = matching.isEmpty()
        matching.add(sbn.key)
        AlarmStore.event(this, "Candidato de alarma activa: ${sbn.packageName}; categoría=${n.category}")
        if (wasEmpty) AlarmRelay.relay(this, sbn.packageName, listOf(title, body).filter { it.isNotBlank() }.joinToString(" · "))
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (matching.remove(sbn.key)) {
            AlarmStore.event(this, "Notificación de alarma retirada: ${sbn.packageName}")
            if (matching.isEmpty()) AlarmRelay.stop(this)
        }
    }
}
