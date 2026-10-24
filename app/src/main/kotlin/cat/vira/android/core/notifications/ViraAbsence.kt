package cat.vira.android.core.notifications

import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/// Quan algú deixa d'obrir l'app, Vira es retira amb amabilitat (`ViraAbsence` de l'iPhone,
/// Arnau, 2026-10-23).
///
/// Es compta en dies des de l'últim cop que es va obrir l'app (o es va respondre un avís):
/// - **Dies 0–2**: tot igual (`NORMAL`).
/// - **Dies 3–13**: només un avís cada 3 dies, el del vespre (`SPARSE`, dies 3, 6, 9 i 12).
/// - **Dia 14**: un últim avís de Viri (`FAREWELL`) i, després, silenci (`SILENT`) fins que torni.
///
/// **Només els avisos que ningú no ha demanat** (el del matí, el del vespre i l'emocional). La
/// medicació, els hàbits i les tasques amb hora no es toquen: la medicació no es retira mai.
object ViraAbsence {

    enum class Allowance { NORMAL, SPARSE, FAREWELL, SILENT }

    const val NORMAL_DAYS = 3
    const val SPARSE_EVERY = 3
    const val FAREWELL_DAY = 14

    /// Sense el prefix `settings.`, com a l'iPhone: és «quan s'ha vist algú en aquest telèfon», no
    /// una preferència, i un canvi de compte no la toca.
    const val LAST_OPENED_AT = "app.lastOpenedAt"

    /// Dies sencers entre el dia en què es va obrir l'app i `day`.
    fun daysAway(day: LocalDate, opened: Instant, zone: ZoneId): Int =
        ChronoUnit.DAYS.between(LocalDate.ofInstant(opened, zone), day).toInt()

    fun allowance(day: LocalDate, opened: Instant, zone: ZoneId): Allowance {
        val away = daysAway(day, opened, zone)
        return when {
            away < NORMAL_DAYS -> Allowance.NORMAL
            away == FAREWELL_DAY -> Allowance.FAREWELL
            away > FAREWELL_DAY -> Allowance.SILENT
            away % SPARSE_EVERY == 0 -> Allowance.SPARSE
            else -> Allowance.SILENT
        }
    }

    /// L'últim cop que l'usuari era aquí. Sense registre (primera arrencada), `null`: qui programa
    /// ho tracta com a «ara».
    fun lastOpened(p: SharedPreferences): Instant? =
        p.getLong(LAST_OPENED_AT, 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli)

    /// L'app ha passat a primer pla, o l'usuari ha respost un avís sense obrir-la. **No** es crida
    /// des del treball de segon pla: que Android desperti l'app no vol dir que hi hagi ningú.
    fun noteOpened(p: SharedPreferences, now: Instant = Instant.now()) =
        p.edit { putLong(LAST_OPENED_AT, now.toEpochMilli()) }
}
