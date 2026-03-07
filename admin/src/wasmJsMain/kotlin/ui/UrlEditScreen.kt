package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import api.ApiClient
import kotlinx.coroutines.launch
import model.ShortenedUrl

@Composable
fun UrlEditScreen(url: ShortenedUrl, onBack: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var newUrl by remember { mutableStateOf(url.originalUrl) }
    var error by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 戻る") }
            Text("URL編集", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = "/${url.slug}",
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
                    ApiClient.updateUrl(url.id, newUrl).fold(
                        onSuccess = { onSaved() },
                        onFailure = { error = it.message ?: "保存エラー" },
                    )
                    saving = false
                }
            },
            enabled = !saving && newUrl.isNotBlank() && newUrl != url.originalUrl,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (saving) "保存中..." else "保存") }
    }
}
