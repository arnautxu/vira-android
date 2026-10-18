package cat.vira.android.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/// L'idioma amb què es dibuixa la pantalla, per formatar dates i números (`Text(date, format:)`
/// de l'iPhone, que ja fa servir el de l'app).
///
/// **Sempre aquest, mai `Locale.getDefault()`**: l'idioma de l'app (Ajustos → Idiomes de l'app)
/// pot no ser el del sistema, i a les captures `Locale.getDefault()` és l'anglès encara que el
/// `@Preview` sigui en català.
@Composable
@ReadOnlyComposable
fun viraLocale(): Locale = LocalConfiguration.current.locales[0]
