package com.example.alarm_app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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

                    // ✅ CHANGE 1: Cancel old alarms BEFORE scheduling new ones
                    cancelAlarms(start, end, interval)

                    scheduleAlarms(start, end, interval)
                    result.success("Scheduled")
                }

                // ✅ CHANGE 2: Handle stop from Flutter
                else if (call.method == "stopAlarms") {
                    val start = call.argument<Long>("start") ?: 0L
                    val end = call.argument<Long>("end") ?: 0L
                    val interval = call.argument<Int>("interval") ?: 1

                    cancelAlarms(start, end, interval)
                    result.success("Stopped")
                }
            }
    }

    private fun scheduleAlarms(start: Long, end: Long, interval: Int) {

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        // ✅ Already correct (kept)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        var currentTime = start

        while (currentTime <= end) {

            val intent = Intent(this, AlarmReceiver::class.java)
            intent.putExtra("time", currentTime)

            val pendingIntent = PendingIntent.getBroadcast(
                this,

                // ✅ CHANGE 3: FIX requestCode (NO more toInt overflow)
                currentTime.hashCode(),

                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    currentTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    currentTime,
                    pendingIntent
                )
            }

            currentTime += interval * 60 * 1000
        }
    }

    // ✅ CHANGE 4: Cancel alarms properly
    private fun cancelAlarms(start: Long, end: Long, interval: Int) {

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        var currentTime = start

        while (currentTime <= end) {

            val intent = Intent(this, AlarmReceiver::class.java)

            val pendingIntent = PendingIntent.getBroadcast(
                this,

                // ✅ SAME requestCode logic used here
                currentTime.hashCode(),

                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)

            currentTime += interval * 60 * 1000
        }
    }
}