package com.example.dreambond.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.dreambond.R

class MusicManager(context: Context) {

    private var mediaPlayer: MediaPlayer? = MediaPlayer.create(context, R.raw.mina_theme)

    // Tracks whether the user has started the music at least once.
    // Prevents auto-play on app launch before the user presses Start.
    var isStarted: Boolean = false
        private set

    init {
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.12f, 0.12f)
    }

    fun play() {
        isStarted = true
        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    // Restarts the BGM from the beginning for a fresh new-day transition.
    fun restart() {
        isStarted = true
        mediaPlayer?.seekTo(0)
        if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun release() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }
}