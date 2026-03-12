import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
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


private val LocalCustomFontsFlow = staticCompositionLocalOf {
    LocalCustomFontsState()
}

@Stable
private class LocalCustomFontsState {
    private var isLoading = false
    var fontFamily: FontFamily by mutableStateOf(FontFamily.Default)
    var isReady: Boolean by mutableStateOf(false)
    private val httpClient = HttpClient(Js)

    suspend fun load(fontFamilyResolver: FontFamily.Resolver) {
        if (isLoading || isReady) return
        isLoading = true
        try {
            val textFonts = buildList {
                for (fontSet in textFontSets) {
                    loadFont(fontSet)?.let(::add)
                }
            }
            if (textFonts.isNotEmpty()) {
                fontFamily = FontFamily(textFonts)
            }

            loadFont(emojiFontSet)?.let { emojiFont ->
                fontFamilyResolver.preload(FontFamily(listOf(emojiFont)))
            }
        } finally {
            isReady = true
            isLoading = false
        }
    }

    private suspend fun loadFont(fontSet: FontSet): Font? {
        return runCatching {
            val response = httpClient.get(Url("/fonts/${fontSet.fileName}"))
            Font(
                identity = fontSet.fileName,
                data = response.readRawBytes(),
                weight = fontSet.weight,
                style = fontSet.style,
            )
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    private data class FontSet(
        val fileName: String,
        val weight: FontWeight,
        val style: FontStyle,
    )

    private val textFontSets: List<FontSet> = listOf(
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

    private val emojiFontSet = FontSet(
        fileName = "NotoColorEmoji-Regular.ttf",
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
    )
}

@Composable
public fun rememberCustomFontFamily(): FontFamily {
    return rememberCustomFontState().fontFamily
}

@Immutable
public data class CustomFontState(
    val fontFamily: FontFamily,
    val isReady: Boolean,
)

@Composable
public fun rememberCustomFontState(): CustomFontState {
    val fontsFlow: LocalCustomFontsState = LocalCustomFontsFlow.current
    val fontFamilyResolver = LocalFontFamilyResolver.current

    LaunchedEffect(fontFamilyResolver) {
        fontsFlow.load(fontFamilyResolver)
    }
    return CustomFontState(
        fontFamily = fontsFlow.fontFamily,
        isReady = fontsFlow.isReady,
    )
}
