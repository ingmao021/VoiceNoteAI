package com.voicenoteai.wear.domain.model

/**
 * Errores al enviar la nota al backend (PRD §5). En todos, el texto se conserva para reintentar
 * sin volver a dictar (regla transversal de §5), salvo [RELINK_ACCOUNT], que exige otra vinculación.
 */
enum class SubmissionError(val retryable: Boolean = true) {
    /** FA-09. */
    NO_CONNECTION,

    /** FA-13: acceso a Google revocado, o token de la app inválido. */
    RELINK_ACCOUNT(retryable = false),

    /** FA-16, y cualquier respuesta inesperada del backend. */
    NOT_SAVED,

    /** FA-17: se agotaron los 90 s (RNF-05). */
    TIMED_OUT,
}
