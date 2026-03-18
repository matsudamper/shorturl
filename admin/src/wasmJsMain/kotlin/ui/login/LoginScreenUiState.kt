package ui.login

import androidx.compose.runtime.Immutable

data class LoginScreenUiState(
    val callbacks: Callbacks,
    val state: State,
) {
    @Immutable
    interface Callbacks {
        fun updateUsername(value: String)
        fun updatePassword(value: String)
        fun login()
    }

    data class State(
        val username: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
    )
}
