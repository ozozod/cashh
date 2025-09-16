package com.example.vayvene.ui.common

/**
 * Constantes compartidas para Intents/Extras.
 * (Ruta nueva que usaremos en código actual)
 */
object Extras {
    const val EXTRA_UID = "EXTRA_UID"
    const val EXTRA_PROMPT = "EXTRA_PROMPT"
    const val EXTRA_MANAGER_UID = "EXTRA_MANAGER_UID"
}

// Además exponemos constantes top-level por si las importaban así:
const val EXTRA_UID = Extras.EXTRA_UID
const val EXTRA_PROMPT = Extras.EXTRA_PROMPT
const val EXTRA_MANAGER_UID = Extras.EXTRA_MANAGER_UID
