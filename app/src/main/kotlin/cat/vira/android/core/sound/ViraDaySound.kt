package cat.vira.android.core.sound

import android.content.Context
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.model.PlanItem
import cat.vira.android.core.data.model.PlanItemStatus
import cat.vira.android.core.data.model.status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/// L'escala del dia (`ViraDaySound` de l'iPhone): cada tasca o hàbit que marques avui sona una nota
/// més amunt que l'anterior, i l'última tasca del dia toca l'acord sencer.
///
/// Es compta sobre el que hi ha guardat i no amb un comptador propi: marcar insereix el registre i
/// desmarcar l'esborra, així que el recompte ja diu la veritat després d'un «Desfer», d'un reinici
/// de l'app o d'un canvi de dia.
///
/// **La medicació no hi entra**: no passa per aquí i no ha de sonar com una cosa aconseguida.
object ViraDaySound {

    /// Es crida just després d'inserir el registre o de marcar la tasca, que ja compten.
    ///
    /// `item` només quan el que s'ha marcat és una tasca: «dia complet» vol dir que no en queda cap
    /// de pendent avui, i només una tasca d'avui el pot tancar.
    suspend fun playDone(
        context: Context,
        db: ViraDatabase,
        closingDayOf: PlanItem? = null,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ) {
        val today = LocalDate.ofInstant(now, zone)
        fun isToday(day: Instant) = LocalDate.ofInstant(day, zone) == today
        val tasks = db.planItems().all().filter { isToday(it.day) }
        val doneTasks = tasks.count { it.status == PlanItemStatus.DONE }
        val habits = db.habitLogs().all().count { isToday(it.day) }
        val closes = closingDayOf != null && isToday(closingDayOf.day) && closesDay(tasks.size, doneTasks)
        withContext(Dispatchers.Main) {
            ViraSound.playDone(context, doneToday = doneTasks + habits, closesDay = closes)
        }
    }

    /// Una sola tasca no fa un dia: amb una, cada cop que la marquessis sonaria l'acord gran.
    fun closesDay(total: Int, done: Int): Boolean = total >= 2 && done == total
}
