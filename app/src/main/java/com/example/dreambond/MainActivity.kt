package com.example.dreambond

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.dreambond.audio.MinaVoiceManager
import com.example.dreambond.audio.MusicManager
import com.example.dreambond.data.local.DreamBondDatabase
import com.example.dreambond.ui.AppNavGraph

class MainActivity : ComponentActivity() {

    private lateinit var database: DreamBondDatabase

    private lateinit var minaVoiceManager: MinaVoiceManager
    private lateinit var musicManager: MusicManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        minaVoiceManager = MinaVoiceManager(this)
        musicManager = MusicManager(this)
        // Music starts only when the user presses Start or Next Day, not on app launch.

        database = Room.databaseBuilder(
            applicationContext,
            DreamBondDatabase::class.java,
            "dreambond_database"
        ).build()

        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                Surface {
                    val navController = rememberNavController()

                    var isMusicEnabled by remember { mutableStateOf(true) }

                    AppNavGraph(
                        navController = navController,
                        database = database,
                        minaVoiceManager = minaVoiceManager,
                        musicManager = musicManager,
                        isMusicEnabled = isMusicEnabled,
                        onToggleMusic = {
                            isMusicEnabled = !isMusicEnabled

                            if (isMusicEnabled) {
                                musicManager.play()
                            } else {
                                musicManager.pause()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Only resume if the user has already started music (not on first launch at home screen).
        if (musicManager.isStarted) {
            musicManager.play()
        }
    }

    override fun onPause() {
        musicManager.pause()
        super.onPause()
    }

    override fun onDestroy() {
        minaVoiceManager.shutdown()
        musicManager.release()
        super.onDestroy()
    }
}