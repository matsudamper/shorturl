package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import api.ApiClient
import api.isUnauthorizedApiError
import kotlinx.coroutines.launch
import model.ShortenedUrl

@Composable
fun UrlEditScreen(
    urlId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onUnauthorized: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf<ShortenedUrl?>(null) }
    var newUrl by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(urlId) {
        loading = true
        error = ""
        url = null
        newUrl = ""
        ApiClient.getUrl(urlId).fold(
            onSuccess = {
                url = it
                newUrl = it.originalUrl
            },
            onFailure = {
                if (it.isUnauthorizedApiError()) {
                    onUnauthorized()
                } else {
                    error = it.message ?: "読込エラー"
                }
            },
        )
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 戻る") }
            Text("URL編集", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val currentUrl = url
        if (currentUrl == null) {
            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            return@Column
        }

        OutlinedTextField(
            value = "/${currentUrl.slug}",
            onValueChange = {},
            label = { Text("スラッグ（変更不可）") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Text(
                "⚠ 301リダイレクトはブラウザにキャッシュされます。変更後も古い宛先にリダイレクトされる場合があります。",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = newUrl,
            onValueChange = { newUrl = it },
            label = { Text("新しいリダイレクト先URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)

        Button(
            onClick = {
                scope.launch {
                    saving = true
                    error = ""
                    ApiClient.updateUrl(currentUrl.id, newUrl).fold(
                        onSuccess = { onSaved() },
                        onFailure = {
                            if (it.isUnauthorizedApiError()) {
                                onUnauthorized()
                            } else {
                                error = it.message ?: "保存エラー"
                            }
                        },
                    )
                    saving = false
                }
            },
            enabled = !saving && newUrl.isNotBlank() && newUrl != currentUrl.originalUrl,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (saving) "保存中..." else "保存") }
    }
}
