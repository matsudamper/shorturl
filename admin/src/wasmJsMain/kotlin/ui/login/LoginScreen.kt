package ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onHashGenerator: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(scope) { LoginScreenViewModel(coroutineScope = scope) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.eventHandler.receiveAsFlow().collect {
            it(object : LoginScreenViewModel.Event {
                override fun onLoggedIn() {
                    onLoggedIn()
                }

                override fun showSnackBar(message: String) {
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            })
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            LoginContent(
                state = uiState.state,
                callbacks = uiState.callbacks,
                onHashGenerator = onHashGenerator,
            )
        }
    }
}

@Composable
private fun LoginContent(
    state: LoginScreenUiState.State,
    callbacks: LoginScreenUiState.Callbacks,
    onHashGenerator: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.width(360.dp).padding(24.dp),
    ) {
        Text("ShortURL 管理画面 ⚙\uFE0F", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = state.username,
            onValueChange = { callbacks.updateUsername(it) },
            label = { Text("ユーザー名") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { callbacks.updatePassword(it) },
            label = { Text("パスワード") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { callbacks.login() },
            enabled = !state.isLoading && state.username.isNotBlank() && state.password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isLoading) "ログイン中..." else "ログイン")
        }

        TextButton(onClick = onHashGenerator) {
            Text("ハッシュ生成ツール（ユーザー登録用）")
        }
    }
}
