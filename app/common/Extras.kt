package com.example.vayvene.ui.common

/**
 * Constantes de extras compartidas por Activities.
 * Esta ruta es la que venías usando en varias clases.
 */
object Extras {
    const val EXTRA_UID = "EXTRA_UID"
    const val EXTRA_PROMPT = "EXTRA_PROMPT"
    const val EXTRA_MANAGER_UID = "EXTRA_MANAGER_UID"
}

// Re-export en top-level para quienes importan como constantes sueltas
const val EXTRA_UID = Extras.EXTRA_UID
const val EXTRA_PROMPT = Extras.EXTRA_PROMPT
const val EXTRA_MANAGER_UID = Extras.EXTRA_MANAGER_UID
    