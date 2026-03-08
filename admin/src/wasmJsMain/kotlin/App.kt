import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import model.ShortenedUrl
import ui.*

// ---- Route definitions ----
@Serializable
data object LoginRoute : NavKey

@Serializable
data object HashGeneratorRoute : NavKey

@Serializable
data object UrlListRoute : NavKey

@Serializable
data object UrlCreateRoute : NavKey

@Serializable
data class UrlEditRoute(val url: ShortenedUrl) : NavKey

@Serializable
data class AnalyticsRoute(val urlId: String, val slug: String) : NavKey

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(LoginRoute::class, LoginRoute.serializer())
            subclass(HashGeneratorRoute::class, HashGeneratorRoute.serializer())
            subclass(UrlListRoute::class, UrlListRoute.serializer())
            subclass(UrlCreateRoute::class, UrlCreateRoute.serializer())
            subclass(UrlEditRoute::class, UrlEditRoute.serializer())
            subclass(AnalyticsRoute::class, AnalyticsRoute.serializer())
        }
    }
}

private fun createTypography(fontFamily: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
    )
}

@Composable
fun App() {
    val backStack = rememberNavBackStack(navConfig, LoginRoute)
    val fontFamily = rememberNotoSansJpFontFamily()

    MaterialTheme(typography = createTypography(fontFamily)) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
        ) { key: NavKey ->
            when (key) {
                LoginRoute -> NavEntry(key) {
                    LoginScreen(
                        onLoggedIn = { backStack.add(UrlListRoute) },
                        onHashGenerator = { backStack.add(HashGeneratorRoute) },
                    )
                }
                HashGeneratorRoute -> NavEntry(key) {
                    HashGeneratorScreen(
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                UrlListRoute -> NavEntry(key) {
                    UrlListScreen(
                        onCreateNew = { backStack.add(UrlCreateRoute) },
                        onEdit = { backStack.add(UrlEditRoute(it)) },
                        onAnalytics = { backStack.add(AnalyticsRoute(it.id, it.slug)) },
                        onLogout = {
                            backStack.removeAll { true }
                            backStack.add(LoginRoute)
                        },
                    )
                }
                UrlCreateRoute -> NavEntry(key) {
                    UrlCreateScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onCreated = { backStack.removeLastOrNull() },
                    )
                }
                is UrlEditRoute -> NavEntry(key) {
                    UrlEditScreen(
                        url = key.url,
                        onBack = { backStack.removeLastOrNull() },
                        onSaved = { backStack.removeLastOrNull() },
                    )
                }
                is AnalyticsRoute -> NavEntry(key) {
                    AnalyticsScreen(
                        urlId = key.urlId,
                        slug = key.slug,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                else -> NavEntry(key) {}
            }
        }
    }
}
