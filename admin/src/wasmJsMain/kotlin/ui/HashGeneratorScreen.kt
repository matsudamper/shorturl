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
    var password by remember { mutableStateOf("") }
    var hash by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.width(480.dp).padding(24.dp),
        ) {
            Text("ハッシュ生成ツール", style = MaterialTheme.typography.headlineMedium)
            Text(
                "生成されたハッシュをXodus DBに直接投入してユーザーを登録します",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
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

            if (hash.isNotBlank()) {
                OutlinedTextField(
                    value = hash,
                    onValueChange = {},
                    label = { Text("bcryptハッシュ") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            TextButton(onClick = onBack) { Text("← ログイン画面に戻る") }
        }
    }
}
