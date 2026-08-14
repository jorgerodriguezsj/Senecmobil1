package com.vozmayores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vozmayores.ui.PushToTalkScreen
import com.vozmayores.ui.VozTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VozTheme {
                PushToTalkScreen()
            }
        }
    }
}
