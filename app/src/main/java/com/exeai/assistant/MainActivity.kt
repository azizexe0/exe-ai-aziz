package com.exeai.assistant

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var command: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
            setBackgroundColor(0xFF090909.toInt())
        }
        val title = TextView(this).apply {
            text = "EXE AI"
            textSize = 32f
            setTextColor(0xFFFF3030.toInt())
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Kişisel asistan • İlk sürüm"
            textSize = 14f
            setTextColor(0xFFBBBBBB.toInt())
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "Merhaba. Bir komut yazabilirsin.\nÖrnek: YouTube'u aç"
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 34, 0, 22)
        }
        command = EditText(this).apply {
            hint = "EXE'ye komut ver..."
            setHintTextColor(0xFF888888.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setSingleLine(true)
            setPadding(16, 12, 16, 12)
            setBackgroundColor(0xFF242424.toInt())
        }
        val run = makeButton("KOMUTU ÇALIŞTIR") { runCommand(command.text.toString()) }
        val schedule = makeButton("SAAT AYARLA / HATIRLAT") { scheduleReminder() }
        val settings = makeButton("TABLET AYARLARINI AÇ") {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
        root.addView(title, matchWrap())
        root.addView(subtitle, matchWrap())
        root.addView(status, matchWrap())
        root.addView(command, matchWrap(58))
        root.addView(run, matchWrap(54))
        root.addView(schedule, matchWrap(54))
        root.addView(settings, matchWrap(54))
        setContentView(root)
    }

    private fun matchWrap(height: Int = ViewGroup.LayoutParams.WRAP_CONTENT) =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply { bottomMargin = 12 }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0xFFB00020.toInt())
        setOnClickListener { action() }
    }

    private fun runCommand(raw: String) {
        val text = raw.trim().lowercase()
        when {
            text.contains("youtube") -> openUrl("https://www.youtube.com")
            text.contains("google") -> openUrl("https://www.google.com")
            text.startsWith("site aç ") || text.startsWith("web sitesi aç ") -> {
                val address = raw.substringAfter("aç", "").trim()
                if (address.isBlank()) show("Site adresini de yaz.")
                else openUrl(if (address.startsWith("http")) address else "https://$address")
            }
            text.contains("saat kaç") -> show("Şu an: ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
            text.contains("ayar") -> startActivity(Intent(Settings.ACTION_SETTINGS))
            else -> show("Bu komut ilk sürümde henüz yok. Şimdilik YouTube, Google, site açma, saat ve Ayarlar komutlarını deneyebilirsin.")
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            show("Açılıyor: $url")
        } catch (_: Exception) { show("Bu bağlantı açılamadı.") }
    }

    private fun scheduleReminder() {
        val input = EditText(this).apply { hint = "Saat (HH:mm), örn. 16:30"; setSingleLine(true) }
        android.app.AlertDialog.Builder(this)
            .setTitle("Hatırlatma saati")
            .setMessage("Bu ilk sürüm belirtilen saatte bildirim gösterir. Android, uygulamaların başka uygulamaları kendiliğinden açmasını kısıtlar.")
            .setView(input)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Kur") { _, _ ->
                val parts = input.text.toString().trim().split(":")
                if (parts.size != 2) { show("Saati 16:30 biçiminde yaz."); return@setPositiveButton }
                val hour = parts[0].toIntOrNull(); val minute = parts[1].toIntOrNull()
                if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) { show("Geçerli bir saat yaz."); return@setPositiveButton }
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1) }
                val intent = Intent(this, ReminderReceiver::class.java)
                val pending = PendingIntent.getBroadcast(this, 11, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarm.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
                show("Hatırlatma kuruldu: %02d:%02d".format(hour, minute))
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7)
                }
            }.show()
    }

    private fun show(message: String) {
        status.text = message
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
