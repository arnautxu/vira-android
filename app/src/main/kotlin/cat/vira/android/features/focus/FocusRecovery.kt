package cat.vira.android.features.focus

import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.model.FocusSession
import java.time.Instant

/// Sessions que el procés no va poder tancar: Android el va matar amb una sessió oberta. La fila hi
/// és, sense `endedAt`; aquí es tanca amb el que se'n sap.
///
/// Si el temps planificat ja s'havia complert, compta com a completada i dona el que hauria donat (la
/// pausa no se sap, i es fa com a l'iPhone: el temps de rellotge). Si no, queda interrompuda ara.
internal object FocusRecovery {

    suspend fun closeOrphans(store: ViraStore, now: Instant) {
        val live = FocusRuntime.current.value?.openSessionID
        store.db.focusSessions().all()
            .filter { it.endedAt == null && it.id != live }
            .forEach { close(store, it, now) }
    }

    private suspend fun close(store: ViraStore, open: FocusSession, now: Instant) {
        val end = open.startedAt.plusSeconds(open.plannedSeconds.toLong())
        val completed = !now.isBefore(end)
        val closed = store.save(store.db.focusSessions(), open.finished(at = if (completed) end else now, completed = completed))
        if (completed) FocusProgress.reward(store, closed, now)
    }
}
