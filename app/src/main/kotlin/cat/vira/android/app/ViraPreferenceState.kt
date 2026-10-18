package cat.vira.android.app

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/// Una preferència llegida com a estat: es redibuixa quan una altra pantalla (Ajustos, el flux
/// d'entrada) la canvia. L'equivalent d'`@AppStorage`.
@Composable
fun rememberPreference(preferences: SharedPreferences, key: String, default: Boolean): State<Boolean> =
    rememberPreferenceOf(preferences, key) { it.getBoolean(key, default) }

@Composable
fun rememberPreference(preferences: SharedPreferences, key: String, default: String): State<String> =
    rememberPreferenceOf(preferences, key) { it.getString(key, default) ?: default }

@Composable
private fun <T> rememberPreferenceOf(
    preferences: SharedPreferences,
    key: String,
    read: (SharedPreferences) -> T,
): State<T> {
    val state = remember(preferences, key) { mutableStateOf(read(preferences)) }
    DisposableEffect(preferences, key) {
        // `SharedPreferences` només guarda una referència feble a l'escoltador: ha de viure aquí.
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changed ->
            // `changed` és `null` quan algú ho esborra tot (`clear()`), i llavors també cal rellegir.
            if (changed == null || changed == key) state.value = read(prefs)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        state.value = read(preferences)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}
