package com.example.alarm_app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build // ✅ ADDED
import android.os.Bundle
import android.provider.Settings // ✅ ADDED
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel

class MainActivity: FlutterActivity() {

    private val CHANNEL = "alarm_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->

                if (call.method == "scheduleAlarms") {

                    val start = call.argument<Long>("start")!!
                    val end = call.argument<Long>("end")!!
                    val interval = call.argument<Int>("interval")!!

                    scheduleAlarms(start, end, interval)
                    result.success("Scheduled")
                }
            }
    }

    private fun scheduleAlarms(start: Long, end: Long, interval: Int) {

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        // 🔥 ADDED: Permission check for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return // ❗ stop here until user enables
            }
        }

        var currentTime = start

        while (currentTime <= end) {

            val intent = Intent(this, AlarmReceiver::class.java)
            intent.putExtra("time", currentTime)

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                currentTime.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 🔥 UPDATED: Safe scheduling (fallback + exact)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    currentTime,
                    pendingIntent
                )
            } else {
                // ✅ fallback for older / restricted devices
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    currentTime,
                    pendingIntent
                )
            }

            currentTime += interval * 60 * 1000
        }
    }
}