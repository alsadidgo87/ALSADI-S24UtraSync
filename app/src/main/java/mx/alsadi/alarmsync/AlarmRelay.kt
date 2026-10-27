package mx.alsadi.alarmsync

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

object AlarmRelay {
    const val CHANNEL = "alarma_urgente"
    private const val TEST_CHANNEL = "diagnostico"
    private const val ACTIVE_ID = 9001
    private const val TEST_ID = 9002
    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Avisos de alarma", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Notificaciones de alarma detectadas para reenvío mediante Mi Fitness"
            enableVibration(true)
            setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
        })
        nm.createNotificationChannel(NotificationChannel(TEST_CHANNEL, "Pruebas de conexión", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Notificaciones de prueba de ALSADI AlarmSync"
            enableVibration(true)
        })
    }
    private fun allowed(ctx: Context): Boolean = Build.VERSION.SDK_INT < 33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    fun relay(ctx: Context, source: String, content: String) {
        channels(ctx)
        if (!allowed(ctx)) { AlarmStore.event(ctx, "ERROR: permiso de notificaciones denegado"); return }
        val intent = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("ALARMA DEL SAMSUNG")
            .setContentText("Posible alarma del Reloj de Samsung")
            .setStyle(Notification.BigTextStyle().bigText("Aviso detectado en Samsung Clock. Confirma su recepción en el reloj."))
            .setCategory(Notification.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setContentIntent(intent)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(ACTIVE_ID, notification)
        AlarmStore.event(ctx, "Aviso generado desde $source; entrega al reloj NO confirmada")
    }
    fun stop(ctx: Context) = ctx.getSystemService(NotificationManager::class.java).cancel(ACTIVE_ID)
    fun test(ctx: Context) {
        channels(ctx)
        if (!allowed(ctx)) { AlarmStore.event(ctx, "ERROR: habilita notificaciones antes de probar"); return }
        val n = Notification.Builder(ctx, TEST_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("ALSADI AlarmSync · PRUEBA")
            .setContentText("Si Mi Fitness reenvía este aviso, debe aparecer en tu Redmi")
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(TEST_ID, n)
        AlarmStore.event(ctx, "Notificación de prueba publicada; recepción en Redmi por comprobar")
    }
}
