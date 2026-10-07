package com.vyrn.tracker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Richieste arrivate da scorciatoie dell'icona: la schermata di destinazione le "consuma" quando è pronta. */
object DeepLink {
    const val NEW_TX = "com.vyrn.tracker.action.NEW_TX"
    const val NEW_TASK = "com.vyrn.tracker.action.NEW_TASK"
    const val FOCUS = "com.vyrn.tracker.action.FOCUS"

    var action by mutableStateOf<String?>(null)
        private set

    fun handle(requested: String?) {
        if (requested == NEW_TX || requested == NEW_TASK || requested == FOCUS) action = requested
    }

    fun consume() {
        action = null
    }
}
