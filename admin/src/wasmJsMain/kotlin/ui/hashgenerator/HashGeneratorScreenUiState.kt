package ui.hashgenerator

import androidx.compose.runtime.Immutable

data class HashGeneratorScreenUiState(
    val callbacks: Callbacks,
    val state: State,
) {
    @Immutable
    interface Callbacks {
        fun updateUsername(value: String)
        fun updatePassword(value: String)
        fun generateHash()
    }

    data class State(
        val username: String = "",
        val password: String = "",
        val hash: String = "",
        val isLoading: Boolean = false,
    )
}
