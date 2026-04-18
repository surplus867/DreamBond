package com.example.dreambond

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.dreambond.audio.MinaVoiceManager
import com.example.dreambond.data.local.DreamBondDatabase
import com.example.dreambond.ui.AppNavGraph

class MainActivity : ComponentActivity() {

    private lateinit var database: DreamBondDatabase

    private lateinit var minaVoiceManager: MinaVoiceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        minaVoiceManager = MinaVoiceManager(this)

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
                    AppNavGraph(
                        navController = navController,
                        database = database,
                        minaVoiceManager = minaVoiceManager
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        minaVoiceManager.stop()
    }

    override fun onDestroy() {
        minaVoiceManager.shutdown()
        super.onDestroy()
    }
}