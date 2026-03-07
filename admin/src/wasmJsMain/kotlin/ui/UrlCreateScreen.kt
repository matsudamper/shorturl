package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import api.ApiClient
import kotlinx.coroutines.launch

private val SLUG_TYPES = listOf(
    "ALPHANUMERIC" to "英数字",
    "LOWERCASE_DIGITS" to "小文字+数字",
    "DIGITS" to "数字のみ",
    "PRONOUNCEABLE" to "発音可能",
    "EMOJI" to "絵文字",
)

private fun defaultLength(type: String) = if (type == "PRONOUNCEABLE") 4 else if (type == "EMOJI") 2 else 3
private fun minLength(type: String) = if (type == "PRONOUNCEABLE") 4 else 2
private fun maxLength(type: String) = when (type) { "PRONOUNCEABLE" -> 12; "EMOJI" -> 6; else -> 10 }

@Composable
fun UrlCreateScreen(onBack: () -> Unit, onCreated: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isAutoMode by remember { mutableStateOf(true) }

    // Auto mode state
    var selectedType by remember { mutableStateOf("ALPHANUMERIC") }
    var slugLength by remember { mutableStateOf(3) }
    var previewSlug by remember { mutableStateOf("") }
    var generating by remember { mutableStateOf(false) }

    // Common state
    var originalUrl by remember { mutableStateOf("") }
    var manualSlug by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 戻る") }
            Text("URL作成", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(16.dp))

        // モード切り替え
        Row {
            FilterChip(
                selected = isAutoMode,
                onClick = { isAutoMode = true },
                label = { Text("自動生成") },
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = !isAutoMode,
                onClick = { isAutoMode = false },
                label = { Text("手動指定") },
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = originalUrl,
            onValueChange = { originalUrl = it },
            label = { Text("リダイレクト先URL") },
            placeholder = { Text("https://example.com/...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        if (isAutoMode) {
            // 生成タイプ選択
            Text("スラッグタイプ", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SLUG_TYPES.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedType == key,
                        onClick = {
                            selectedType = key
                            slugLength = defaultLength(key)
                            previewSlug = ""
                        },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 文字数スライダー
            val min = minLength(selectedType).toFloat()
            val max = maxLength(selectedType).toFloat()
            Text("文字数: $slugLength", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = slugLength.toFloat(),
                    onValueChange = { slugLength = it.toInt(); previewSlug = "" },
                    valueRange = min..max,
                    steps = (max - min - 1).toInt(),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = slugLength.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.coerceIn(min.toInt(), max.toInt())?.let { v ->
                            slugLength = v; previewSlug = ""
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.width(72.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            // 生成ボタン + プレビュー
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        scope.launch {
                            generating = true
                            error = ""
                            ApiClient.generateSlug(selectedType, slugLength).fold(
                                onSuccess = { previewSlug = it },
                                onFailure = { error = it.message ?: "生成エラー" },
                            )
                            generating = false
                        }
                    },
                    enabled = !generating,
                ) { Text(if (generating) "生成中..." else "スラッグを生成") }

                if (previewSlug.isNotBlank()) {
                    Spacer(Modifier.width(16.dp))
                    Text("プレビュー: /${previewSlug}", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)

            Button(
                onClick = {
                    scope.launch {
                        saving = true
                        error = ""
                        ApiClient.createUrl(originalUrl, previewSlug, isAutoGenerated = true).fold(
                            onSuccess = { onCreated() },
                            onFailure = { error = it.message ?: "登録エラー" },
                        )
                        saving = false
                    }
                },
                enabled = !saving && previewSlug.isNotBlank() && originalUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (saving) "登録中..." else "登録") }
        } else {
            // 手動スラッグ入力
            OutlinedTextField(
                value = manualSlug,
                onValueChange = { manualSlug = it },
                label = { Text("スラッグ（2〜128文字）") },
                placeholder = { Text("my-custom-slug") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Unicode（絵文字・日本語）可") },
            )

            Spacer(Modifier.height(8.dp))

            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)

            Button(
                onClick = {
                    scope.launch {
                        saving = true
                        error = ""
                        ApiClient.createUrl(originalUrl, manualSlug, isAutoGenerated = false).fold(
                            onSuccess = { onCreated() },
                            onFailure = { error = it.message ?: "登録エラー" },
                        )
                        saving = false
                    }
                },
                enabled = !saving && manualSlug.isNotBlank() && originalUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (saving) "登録中..." else "登録") }
        }
    }
}
