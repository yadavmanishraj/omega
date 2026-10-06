package com.manishraj.saavnmusic.domain

/**
 * UI-facing state for a screen section. Lives in :core:common (it is a UI /
 * state concern, not a domain model) but keeps the historical
 * `com.manishraj.saavnmusic.domain` package so no import sites change.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Success<T>(
        val data: T,
    ) : UiState<T>

    data class Error(
        val message: String,
    ) : UiState<Nothing>
}
