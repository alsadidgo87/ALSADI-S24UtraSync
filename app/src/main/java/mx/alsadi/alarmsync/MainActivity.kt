package mx.alsadi.alarmsync

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var history: TextView
    private val blue = Color.rgb(22, 58, 95)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlarmRelay.channels(this)
        val outer = ScrollView(this).apply { setBackgroundColor(Color.rgb(246, 248, 251)) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 36, 32, 32) }
        outer.addView(box)
        fun heading(t: String, size: Float, color: Int = blue) = TextView(this).apply {
            text = t; textSize = size; setTextColor(color); setPadding(0, 12, 0, 12); box.addView(this)
        }
        fun button(t: String, action: () -> Unit) = Button(this).apply { text = t; isAllCaps = false; setOnClickListener { action() }; box.addView(this) }
        heading("ALSADI AlarmSync", 27f)
        heading("Samsung Galaxy S24 Ultra + Redmi Watch 5 Active", 15f, Color.DKGRAY)
        heading("Mi Fitness permanece instalado y conserva el emparejamiento. Esta app NO controla Bluetooth ni instala alarmas en el reloj.", 14f, Color.DKGRAY)
        status = heading("", 15f, blue)
        val toggle = Switch(this).apply {
            text = "Detección y retransmisión activas"
            isChecked = AlarmStore.enabled(this@MainActivity)
            setOnCheckedChangeListener { _, checked -> AlarmStore.setEnabled(this@MainActivity, checked); if (!checked) AlarmRelay.stop(this@MainActivity); AlarmStore.event(this@MainActivity, "Monitor habilitado=$checked"); update() }
            box.addView(this)
        }
        button("1. Permitir acceso a notificaciones") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        button("2. Permitir avisos de ALSADI AlarmSync") { if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42) else Toast.makeText(this, "Permiso ya disponible en esta versión de Android", Toast.LENGTH_SHORT).show() }
        button("3. Enviar notificación de prueba") { AlarmRelay.test(this); update() }
        button("4. Abrir alarmas de Samsung") {
            try { startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).setPackage("com.sec.android.app.clockpackage")) }
            catch (_: Exception) { try { startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS)) } catch (e: Exception) { toast("No se pudo abrir el reloj: ${e.message}") } }
        }
        button("5. Configurar notificaciones de Mi Fitness") {
            try {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, "com.xiaomi.wearable"))
            } catch (_: Exception) { toast("Abre los ajustes de Mi Fitness manualmente") }
        }
        button("6. Diagnóstico y actualización") { update() }
        heading("Historial de eventos", 20f)
        history = heading("", 12f, Color.DKGRAY).apply { movementMethod = ScrollingMovementMethod() }
        button("Limpiar historial") { AlarmStore.clear(this); update() }
        heading("CONFIGURACIÓN IMPORTANTE", 17f)
        heading("En Mi Fitness habilita las notificaciones de ALSADI AlarmSync. Desactiva restricciones de batería para Mi Fitness y AlarmSync. Ejecuta una alarma real del Samsung para comprobar si el monitor la reconoce. El resultado físico en el Redmi no puede confirmarse desde esta app.", 14f, Color.DKGRAY)
        heading("Limitaciones: no se leen alarmas programadas privadas, no se sincronizan sus horarios con el reloj, no se controla la vibración del Redmi y no se garantiza detección si Samsung no publica un aviso accesible.", 13f, Color.DKGRAY)
        setContentView(outer)
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private fun update() {
        val list = Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
        val connected = list.contains(packageName)
        val permitted = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        status.text = "Monitor autorizado: ${if (connected) "SÍ" else "NO"}  |  Publicar avisos: ${if (permitted) "SÍ" else "NO"}  |  Retransmisión: ${if (AlarmStore.enabled(this)) "ACTIVA" else "PAUSADA"}\nLa recepción en el reloj sólo se verifica observándolo."
        history.text = AlarmStore.history(this)
    }
    override fun onResume() { super.onResume(); if (::status.isInitialized) update() }
}
