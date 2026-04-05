package com.example.alarm_app

import android.app.Activity
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button

class AlarmActivity : Activity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val button = Button(this)
        button.text = "STOP ALARM ❌"
        setContentView(button)

        // 🔊 Play sound
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        // 🛑 Stop button
        button.setOnClickListener {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            finish()
        }
    }
}