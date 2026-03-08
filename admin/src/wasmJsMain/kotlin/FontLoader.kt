import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes

private val weights = listOf(
    FontWeight.Thin,
    FontWeight.ExtraLight,
    FontWeight.Light,
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
    FontWeight.Black,
)

@Composable
fun rememberNotoSansJpFontFamily(): FontFamily {
    var fontFamily by remember { mutableStateOf<FontFamily>(FontFamily.Default) }

    LaunchedEffect(Unit) {
        val client = HttpClient(Js)
        val result = runCatching {
            client.get("/fonts/NotoSansJP.ttf").readRawBytes()
        }
        client.close()

        result.onSuccess { bytes ->
            fontFamily = FontFamily(
                weights.map { weight ->
                    Font(
                        identity = "NotoSansJP-${weight.weight}",
                        data = bytes,
                        weight = weight,
                        style = FontStyle.Normal,
                    )
                }
            )
        }.onFailure {
            it.printStackTrace()
        }
    }

    return fontFamily
}
