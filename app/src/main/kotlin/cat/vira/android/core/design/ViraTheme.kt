package cat.vira.android.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/// Tema de l'app. Provisional: el fil Sistema de disseny hi posa els tokens de l'iPhone
/// (`ViraColor`, `ViraFont`, `ViraGradient`, `ViraLayout`, `ViraMotion`) i els components.
/// Cap pantalla pot fer servir un color literal: sempre a través dels tokens d'aquí.
@Composable
fun ViraTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
