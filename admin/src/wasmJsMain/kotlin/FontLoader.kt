import androidx.compose.runtime.*
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import io.ktor.client.*
import io.ktor.client.engine.js.*
//import io.ktor.client.plugins.logging.EMPTY
//import io.ktor.client.plugins.logging.LogLevel
//import io.ktor.client.plugins.logging.Logger
//import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.coroutineContext


private val LocalCustomFontsFlow = staticCompositionLocalOf {
    LocalCustomFontsState()
}

@Stable
private class LocalCustomFontsState {
    private var isLoading = false
    private val fontsFlow = MutableStateFlow<Map<FontSet, Font>>(mapOf())
    var fontFamily: FontFamily by mutableStateOf(FontFamily.Default)

    suspend fun load() {
        if (isLoading) return
        isLoading = true
        CoroutineScope(Job() + coroutineContext).launch {
            while (isActive) delay(100)
            isLoading = false
        }
        fonts
            .filter { it !in fontsFlow.value.keys }
            .forEach { fontSet ->
                runCatching {
                    HttpClient(Js) {
//                        install(Logging) {
//                            logger = Logger.EMPTY
//                            level = LogLevel.NONE
//                        }
                    }.get(Url("/fonts/${fontSet.fileName}"))
                }.onFailure {
                    it.printStackTrace()
                }.onSuccess { response ->
                    val byteArray = response.readRawBytes()
                    fontsFlow.update {
                        it.plus(
                            fontSet to
                                    Font(
                                        identity = fontSet.fileName,
                                        data = byteArray,
                                        weight = fontSet.weight,
                                        style = fontSet.style,
                                    ),
                        )
                    }
                    fontFamily = FontFamily(
                        fontsFlow.value.values.toList(),
                    )
                }
            }
    }

    private data class FontSet(
        val fileName: String,
        val weight: FontWeight,
        val style: FontStyle,
    )

    private val fonts: List<FontSet> = listOf(
        FontSet("NotoColorEmoji-Regular.ttf", FontWeight.Normal, FontStyle.Normal),
        FontSet("NotoSansJP-Medium.ttf", FontWeight.Medium, FontStyle.Normal),
        FontSet("NotoSansJP-Bold.ttf", FontWeight.Bold, FontStyle.Normal),
        FontSet("NotoSansJP-Regular.ttf", FontWeight.W400, FontStyle.Normal),
        FontSet("NotoSansJP-Black.ttf", FontWeight.Black, FontStyle.Normal),
        FontSet("NotoSansJP-ExtraBold.ttf", FontWeight.ExtraBold, FontStyle.Normal),
        FontSet("NotoSansJP-ExtraLight.ttf", FontWeight.ExtraLight, FontStyle.Normal),
        FontSet("NotoSansJP-Light.ttf", FontWeight.Light, FontStyle.Normal),
        FontSet("NotoSansJP-SemiBold.ttf", FontWeight.SemiBold, FontStyle.Normal),
        FontSet("NotoSansJP-Thin.ttf", FontWeight.Thin, FontStyle.Normal),
    )
}

@Composable
public fun rememberCustomFontFamily(): FontFamily {
    val fontsFlow: LocalCustomFontsState = LocalCustomFontsFlow.current

    LaunchedEffect(Unit) {
        fontsFlow.load()
    }
    return fontsFlow.fontFamily
}

@Composable
public fun rememberFontFamilyResolver(): FontFamily.Resolver {
    return remember { androidx.compose.ui.text.font.createFontFamilyResolver() }
}
