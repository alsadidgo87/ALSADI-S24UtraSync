package mx.alsadi.alarmsync

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Solo examina indicadores de alarma publicados por Samsung Clock. */
class AlarmListener : NotificationListenerService() {
    private val active = mutableSetOf<String>()
    private val clockPackage = "com.sec.android.app.clockpackage"

    override fun onListenerConnected() {
        super.onListenerConnected()
        AlarmStore.event(this, "Monitor conectado; los avisos requieren consentimiento")
    }

    override fun onListenerDisconnected() {
        active.clear()
        AlarmRelay.stop(this)
        AlarmStore.event(this, "Monitor desconectado")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!AlarmStore.enabled(this) || sbn.packageName != clockPackage) return
        val n = sbn.notification
        // No registrar ni retransmitir el contenido original de notificaciones.
        // La categoría indica una posible alarma, no garantiza que esté sonando.
        if (n.category != Notification.CATEGORY_ALARM) return
        if (active.add(sbn.key) && active.size == 1) {
            AlarmRelay.relay(this, clockPackage, "Posible alarma activa del Samsung")
            AlarmStore.event(this, "Aviso de alarma detectado; entrega al reloj no confirmada")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (active.remove(sbn.key) && active.isEmpty()) {
            AlarmRelay.stop(this)
            AlarmStore.event(this, "Aviso de alarma finalizado")
        }
    }
}
