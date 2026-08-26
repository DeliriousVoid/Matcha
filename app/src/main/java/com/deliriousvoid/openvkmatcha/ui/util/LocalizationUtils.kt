package com.deliriousvoid.openvkmatcha.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.stringResource
import com.deliriousvoid.openvkmatcha.util.LanguageManager

val LocalLanguageManager = compositionLocalOf<LanguageManager?> {
    null
}

@Composable
@ReadOnlyComposable
fun matchaStringResource(id: Int): String {
    val languageManager = LocalLanguageManager.current
    return languageManager?.getString(id) ?: stringResource(id)
}

@Composable
@ReadOnlyComposable
fun matchaStringResource(id: Int, vararg formatArgs: Any): String {
    val languageManager = LocalLanguageManager.current
    val customString = languageManager?.getString(id)
    return if (customString != null) {
        try {
            String.format(customString, *formatArgs)
        } catch (e: Exception) {
            stringResource(id, *formatArgs)
        }
    } else {
        stringResource(id, *formatArgs)
    }
}
