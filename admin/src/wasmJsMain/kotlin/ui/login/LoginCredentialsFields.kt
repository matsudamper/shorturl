package ui.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

private val InputLineHeight = 24.dp

private class FocusInteractionHolder {
    var focus: FocusInteraction.Focus? = null
}

/**
 * パスワードマネージャーに認識させるため、Compose の TextField ではなく HTML の input を form に紐付けて使う
 */
@Composable
internal fun LoginCredentialsFields(
    username: String,
    password: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    usernameLabel: String,
    passwordLabel: String,
    submitEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    HiddenLoginForm(
        onSubmit = onSubmit,
        submitEnabled = submitEnabled,
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HtmlCredentialField(
            label = usernameLabel,
            value = username,
            onValueChange = onUsernameChange,
            inputId = USERNAME_INPUT_ID,
            inputName = "username",
            inputType = "text",
            autocomplete = "username",
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        HtmlCredentialField(
            label = passwordLabel,
            value = password,
            onValueChange = onPasswordChange,
            inputId = PASSWORD_INPUT_ID,
            inputName = "password",
            inputType = "password",
            autocomplete = "current-password",
            textStyle = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun HiddenLoginForm(
    onSubmit: () -> Unit,
    submitEnabled: Boolean,
) {
    DisposableEffect(submitEnabled, onSubmit) {
        val form = document.getElementById(FORM_ID) as HTMLFormElement?
            ?: createHiddenLoginForm()
        form.onsubmit = { event ->
            event.preventDefault()
            if (submitEnabled) {
                onSubmit()
            }
        }
        onDispose {
            form.onsubmit = null
        }
    }
}

private fun createHiddenLoginForm(): HTMLFormElement {
    val form = document.createElement("form") as HTMLFormElement
    form.id = FORM_ID
    form.setAttribute("autocomplete", "on")
    form.style.display = "none"
    // 入力欄が複数ある form は submit ボタンが無いと Enter での暗黙送信が行われない
    val submitButton = document.createElement("button") as HTMLButtonElement
    submitButton.type = "submit"
    form.appendChild(submitButton)
    document.body?.appendChild(form)
    return form
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HtmlCredentialField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    inputId: String,
    inputName: String,
    inputType: String,
    autocomplete: String,
    textStyle: TextStyle,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val focusInteractionHolder = remember { FocusInteractionHolder() }
    val clickInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = clickInteractionSource,
                indication = null,
            ) {
                (document.getElementById(inputId) as? HTMLInputElement)?.focus()
            },
    ) {
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            visualTransformation = VisualTransformation.None,
            innerTextField = {
                HtmlElementView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(InputLineHeight),
                    factory = {
                        val input = document.createElement("input") as HTMLInputElement
                        input.id = inputId
                        input.name = inputName
                        input.type = inputType
                        input.autocomplete = autocomplete
                        input.setAttribute("form", FORM_ID)
                        input.setAttribute("aria-label", label)
                        input.required = true
                        input
                    },
                    update = { input ->
                        applyInputStyle(
                            input = input,
                            fontSizeSp = textStyle.fontSize.value,
                            textColor = colors.onSurface,
                        )
                        bindFocusHandlers(
                            input = input,
                            interactionSource = interactionSource,
                            focusInteractionHolder = focusInteractionHolder,
                        )
                        if (input.value != value) {
                            input.value = value
                        }
                        // パスワードマネージャーの自動入力は input ではなく change だけを発火することがある
                        val notifyValueChange: (Event) -> Unit = { event ->
                            val newValue = readInputValue(event)
                            if (newValue != value) {
                                onValueChange(newValue)
                            }
                        }
                        input.oninput = notifyValueChange
                        input.onchange = notifyValueChange
                    },
                )
            },
            label = { Text(label) },
            singleLine = true,
            enabled = true,
            isError = false,
            interactionSource = interactionSource,
            colors = OutlinedTextFieldDefaults.colors(),
        )
    }
}

private fun bindFocusHandlers(
    input: HTMLInputElement,
    interactionSource: MutableInteractionSource,
    focusInteractionHolder: FocusInteractionHolder,
) {
    input.onfocus = {
        if (focusInteractionHolder.focus == null) {
            val focus = FocusInteraction.Focus()
            focusInteractionHolder.focus = focus
            interactionSource.tryEmit(focus)
        }
    }
    input.onblur = {
        val focus = focusInteractionHolder.focus
        if (focus != null) {
            interactionSource.tryEmit(FocusInteraction.Unfocus(focus))
            focusInteractionHolder.focus = null
        }
    }
}

private fun applyInputStyle(
    input: HTMLInputElement,
    fontSizeSp: Float,
    textColor: Color,
) {
    input.style.apply {
        boxSizing = "border-box"
        display = "block"
        width = "100%"
        height = "100%"
        margin = "0"
        padding = "0"
        border = "none"
        outline = "none"
        backgroundColor = "transparent"
        color = textColor.toCssColor()
        setProperty("appearance", "none")
        setProperty("-webkit-appearance", "none")
        fontSize = "${fontSizeSp}px"
        lineHeight = "normal"
        fontFamily = "inherit"
        letterSpacing = "normal"
    }
}

private fun readInputValue(event: Event): String {
    val target = event.target
    return (target as? HTMLInputElement)?.value.orEmpty()
}

private fun Color.toCssColor(): String {
    val argb = toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "rgb($r, $g, $b)"
}

private const val FORM_ID = "login-credentials-form"
private const val USERNAME_INPUT_ID = "login-username"
private const val PASSWORD_INPUT_ID = "login-password"
