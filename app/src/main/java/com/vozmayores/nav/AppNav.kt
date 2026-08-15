package com.vozmayores.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Estado de navegación del mini-OS. Pila simple: solo distingue
 * "estamos en Home" vs "estamos en una sub-pantalla" — el botón
 * atrás siempre vuelve a Home.
 */
class AppNav {
    var current by mutableStateOf(Screen.Home)
        private set

    fun open(screen: Screen) {
        current = screen
    }

    fun home() {
        current = Screen.Home
    }

    val isAtHome: Boolean get() = current == Screen.Home
}
