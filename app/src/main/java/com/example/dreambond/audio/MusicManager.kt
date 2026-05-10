package com.example.dreambond.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.dreambond.R

class MusicManager(context: Context) {

    private val appContext = context.applicationContext
    private var currentThemeResId: Int = R.raw.mina_theme
    private var mediaPlayer: MediaPlayer? = MediaPlayer.create(appContext, currentThemeResId)

    // Tracks whether the user has started the music at least once.
    // Prevents auto-play on app launch before the user presses Start.
    var isStarted: Boolean = false
        private set

    init {
        applyPlayerSettings()
    }

    // Switches BGM based on selected character.
    // Alice uses alice_theme, Mina/default uses mina_theme.
    fun setThemeForCharacter(characterName: String?) {
        val nextThemeResId = when (characterName) {
            "Alice" -> R.raw.alice_theme
            else -> R.raw.mina_theme
        }

        if (nextThemeResId == currentThemeResId) return

        val wasPlaying = mediaPlayer?.isPlaying == true

        mediaPlayer?.stop()
        mediaPlayer?.release()

        currentThemeResId = nextThemeResId
        mediaPlayer = MediaPlayer.create(appContext, currentThemeResId)
        applyPlayerSettings()

        // Keep playback behavior smooth when switching characters mid-session.
        if (wasPlaying) {
            mediaPlayer?.start()
        }
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

    private fun applyPlayerSettings() {
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(0.18f, 0.18f)
    }
}