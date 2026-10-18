package cat.vira.android.core.design

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Locale

/// L'entorn que han de tenir totes les captures, abans de dibuixar res. **Tota prova de captura
/// l'ha de fer servir** (dins o fora de `ViraTheme`, és igual):
///
/// ```
/// @PreviewTest @ViraScreenCaptures @Composable
/// fun Today() = ViraCapture { ViraTheme(reduceMotion = true) { … } }
/// ```
///
/// Arregla dues coses de layoutlib (el renderitzador de les captures) que no passen al mòbil:
///
/// - **Les llistes «lazy» sortien en blanc.** Layoutlib deixa `Build.FINGERPRINT` a `null`, i
///   Compose el llegeix en carregar el planificador de les llistes (`PrefetchScheduler`): peta
///   la càrrega de la classe i, des d'aleshores, cap `LazyColumn`/`LazyRow`/`LazyVerticalGrid`
///   d'aquella execució es pot dibuixar —i la CI passava igualment, amb imatges buides. Com que
///   la classe només es carrega un cop per execució, n'hi ha prou que **una** captura sense
///   aquest entorn arribi primer a una llista perquè totes les altres surtin buides.
/// - **Les dates i els números sortien en anglès.** El `locale` de `@Preview` canvia els textos
///   però no `Locale.getDefault()`, que és el que fan servir els formats de data i número.
@Composable
fun ViraCapture(content: @Composable () -> Unit) {
    ViraCaptureFixes.ensureFingerprint()
    // PROVA: sense canviar el locale, per saber quina de les dues peces deixa les captures d'1 px.
    content()
}

private object ViraCaptureFixes {
    /// Qualsevol valor no buit. **Mai `"robolectric"`**: Compose el reconeix i canvia més coses que
    /// el planificador, i les captures sense alçada fixa sortien d'un sol píxel.
    private const val FINGERPRINT = "layoutlib"

    @Volatile private var done = false

    fun ensureFingerprint() {
        if (done) return
        synchronized(this) {
            if (done) return
            done = true
            @Suppress("SENSELESS_COMPARISON")
            if (Build.FINGERPRINT != null) return
            val field = Build::class.java.getDeclaredField("FINGERPRINT")
            // Si no es pot, millor una llista buida que cap captura.
            runCatching {
                if (!Modifier.isFinal(field.modifiers)) {
                    field.isAccessible = true
                    field.set(null, FINGERPRINT)
                } else {
                    writeStaticFinal(field, FINGERPRINT)
                }
            }
        }
    }

    /// Un `static final` no es pot escriure amb reflexió normal des de Java 12; `Unsafe` sí.
    /// Només aquí, a les captures: mai al codi de l'app.
    private fun writeStaticFinal(field: Field, value: Any) {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val unsafe = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
        val base = unsafeClass.getMethod("staticFieldBase", Field::class.java).invoke(unsafe, field)
        val offset = unsafeClass.getMethod("staticFieldOffset", Field::class.java).invoke(unsafe, field) as Long
        unsafeClass.getMethod("putObject", Any::class.java, Long::class.javaPrimitiveType, Any::class.java)
            .invoke(unsafe, base, offset, value)
    }
}
