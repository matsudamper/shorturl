package ui.hashgenerator

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
fun HashGeneratorScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(scope) { HashGeneratorScreenViewModel(coroutineScope = scope) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.eventHandler.receiveAsFlow().collect {
            it(object : HashGeneratorScreenViewModel.Event {
                override fun showSnackBar(message: String) {
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            })
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← 戻る") }
                Text("ハッシュ生成ツール", style = MaterialTheme.typography.headlineMedium)
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            HashGeneratorContent(
                state = uiState.state,
                callbacks = uiState.callbacks,
            )
        }
    }
}

@Composable
private fun HashGeneratorContent(
    state: HashGeneratorScreenUiState.State,
    callbacks: HashGeneratorScreenUiState.Callbacks,
) {
    val createUserCommand = buildCreateUserCommand(username = state.username, hash = state.hash)

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp).padding(24.dp),
    ) {
        Text(
            "生成されたハッシュを Gradle の createUser タスクにそのまま渡してユーザー登録できます",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )

        OutlinedTextField(
            value = state.username,
            onValueChange = { callbacks.updateUsername(it) },
            label = { Text("ユーザー名") },
            placeholder = { Text("admin") },
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
            onClick = { callbacks.generateHash() },
            enabled = !state.isLoading && state.password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isLoading) "生成中..." else "ハッシュを生成")
        }

        OutlinedTextField(
            value = state.hash,
            onValueChange = {},
            label = { Text("bcryptハッシュ") },
            readOnly = true,
            placeholder = { Text("生成後にここへ表示されます") },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = createUserCommand,
            onValueChange = {},
            label = { Text("Gradleコマンド") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            supportingText = {
                Text("username が未入力の間は <username> のままです")
            },
        )
    }
}

private fun buildCreateUserCommand(username: String, hash: String): String {
    val resolvedUsername = username.ifBlank { "<username>" }
    val resolvedHash = hash.ifBlank { "<bcrypt-hash>" }
    return "./gradlew :server:createUser -Pusername='$resolvedUsername' -PpasswordHash='$resolvedHash'"
}
