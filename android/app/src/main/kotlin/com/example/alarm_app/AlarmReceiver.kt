package com.example.alarm_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer

class AlarmReceiver : BroadcastReceiver() {
override fun onReceive(context: Context, intent: Intent) {

    val i = Intent(context, AlarmActivity::class.java)
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    context.startActivity(i)
}
}