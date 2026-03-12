package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import api.ApiClient
import kotlinx.coroutines.launch

@Composable
fun HashGeneratorScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hash by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val createUserCommand = buildCreateUserCommand(username = username, hash = hash)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp).padding(24.dp),
        ) {
            Text("ハッシュ生成ツール", style = MaterialTheme.typography.headlineMedium)
            Text(
                "生成されたハッシュを Gradle の createUser タスクにそのまま渡してユーザー登録できます",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("ユーザー名") },
                placeholder = { Text("admin") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("パスワード") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = ""
                        hash = ""
                        ApiClient.generateHash(password).fold(
                            onSuccess = { hash = it },
                            onFailure = { error = it.message ?: "エラー" },
                        )
                        loading = false
                    }
                },
                enabled = !loading && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (loading) "生成中..." else "ハッシュを生成")
            }

            OutlinedTextField(
                value = hash,
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

            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            TextButton(onClick = onBack) { Text("← ログイン画面に戻る") }
        }
    }
}

private fun buildCreateUserCommand(username: String, hash: String): String {
    val resolvedUsername = username.ifBlank { "<username>" }
    val resolvedHash = hash.ifBlank { "<bcrypt-hash>" }
    return "./gradlew :server:createUser -Pusername='$resolvedUsername' -PpasswordHash='$resolvedHash'"
}
