package com.example.jewels

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.room.Room
import com.example.jewels.data.local.db.AppDatabase
import com.example.jewels.ui.theme.JewelsTheme

class MainActivity : ComponentActivity() {

    // Base de datos (temporal, luego va en Hilt)
    lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Room
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "jewels_db"
        ).fallbackToDestructiveMigration()
            .build()

        enableEdgeToEdge()

        setContent {
            JewelsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // Por ahora solo pantalla base
                    AppRoot(modifier = Modifier.padding(innerPadding))

                }
            }
        }
    }
}

@Composable
fun AppRoot(modifier: Modifier = Modifier) {
    // Luego aquí va Navigation + BottomBar
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    JewelsTheme {
        AppRoot()
    }
}
