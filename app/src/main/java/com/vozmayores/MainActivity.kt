package com.vozmayores

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vozmayores.ui.PushToTalkScreen
import com.vozmayores.ui.VozTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La pantalla no se apaga mientras la app está en primer plano.
        // Sin este flag es incómodo para una persona mayor que esté
        // pensando qué decir y la pantalla se le apaga en la cara.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            VozTheme {
                PushToTalkScreen()
            }
        }
    }
}
