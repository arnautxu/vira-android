package cat.vira.android.core.resources

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/// Plural amb el text del zero de l'iPhone. Android no fa servir mai la quantitat `zero` en
/// català, castellà ni anglès, així que `export-strings.py` el treu a part com a `<nom>_zero`
/// i aquí es tria quan el recompte és 0, com fa el catàleg de l'iPhone.
@Composable
fun viraPluralString(@PluralsRes id: Int, count: Int, @StringRes zero: Int? = null, vararg args: Any): String =
    if (count == 0 && zero != null) {
        stringResource(zero, *args)
    } else {
        pluralStringResource(id, count, *args)
    }
