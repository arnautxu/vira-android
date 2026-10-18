package cat.vira.android.core.backend.sync

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import cat.vira.android.core.backend.ViraBackendError
import cat.vira.android.core.backend.ViraBackendException
import cat.vira.android.core.backend.ViraRestClient
import cat.vira.android.core.backend.ViraRestClient.Param
import cat.vira.android.core.backend.ViraSession
import cat.vira.android.core.data.ViraDataEraser
import cat.vira.android.core.data.ViraDatabase
import cat.vira.android.core.data.ViraStore
import cat.vira.android.core.data.ViraTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/// Puja el que s'ha fet al telèfon i baixa el que s'ha fet als altres (`ViraSyncEngine` de l'iPhone).
///
/// **El protocol és el de l'iPhone byte a byte** —mateixes files, cursors, làpides, marge de cinc
/// minuts i «l'últim escriu»—, perquè els dos mòbils escriuen a les mateixes taules i un que ho fes
/// una mica diferent trepitjaria l'altre.
///
/// No bloqueja res: sense xarxa, l'app funciona igual i el següent intent recupera el retard.
class ViraSyncEngine(
    private val session: ViraSession,
    private val db: ViraDatabase,
    private val preferences: SharedPreferences,
    private val rest: ViraRestClient = ViraRestClient(),
    private val store: ViraStore = ViraStore(db),
    private val clock: () -> Instant = ViraTime::now,
    /// El que arriba del compte ja es va guanyar en un altre telèfon: el fil de la Botiga hi connecta
    /// `ViraCardCelebrations.absorb` perquè no es celebri dues vegades.
    private val afterProgressApplied: suspend (ViraDatabase) -> Unit = {},
) {

    sealed interface State {
        data object Idle : State
        data object Syncing : State
        data class Succeeded(val at: Instant) : State
        data class Failed(val error: ViraBackendError) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val isRunning = AtomicBoolean(false)
    @Volatile private var identity: Identity? = null
    private val cursors = ViraSyncCursors(preferences)

    /// L'ordre importa: els pares abans que els fills. Un registre d'hàbit que arriba abans que el
    /// seu hàbit es descartaria.
    val tables: List<ViraSyncTable<*>> = listOf(
        ProfileSyncTable { store },
        PlanItemSyncTable,
        PlanAssistanceSyncTable,
        PlanAssistanceLinkSyncTable,
        HabitSyncTable,
        HabitLogSyncTable,
        FocusProfileSyncTable,
        FocusSessionSyncTable,
        ProgressCounterSyncTable({ store }, afterProgressApplied),
        StreakSyncTable({ store }, afterProgressApplied),
        CoinTransactionSyncTable { store },
        PersonalRewardSyncTable,
        SkinOwnershipSyncTable,
        AccessoryOwnershipSyncTable,
        ChallengeClaimSyncTable,
        EmotionEntrySyncTable,
        MedicationSyncTable,
        MedicationIntakeSyncTable,
    )

    /// Sincronitza només si fa prou estona de l'última que va anar bé: en tornar a primer pla, la
    /// passada periòdica i el canvi d'estat en demanarien dues alhora.
    suspend fun syncIfStale() {
        val last = (state.value as? State.Succeeded)?.at
        if (last != null && Duration.between(last, clock()) < AUTOMATIC_INTERVAL) return
        sync()
    }

    /// Sincronitza si té sentit fer-ho. Silenciosa: no interromp res ni demana res.
    suspend fun sync() {
        val user = session.user ?: return
        if (!session.requiresAccount || !isRunning.compareAndSet(false, true)) return
        identity = Identity(user.id, session.dataGeneration)
        _state.value = State.Syncing
        try {
            run()
            requireCurrentAccount()
            _state.value = State.Succeeded(clock())
        } catch (_: AccountChanged) {
            _state.value = State.Idle
        } catch (error: ViraBackendException) {
            log("Sincronització fallida: ${error.error}")
            _state.value = State.Failed(error.error)
        } catch (error: CancellationException) {
            _state.value = State.Idle
            // La sessió avisa d'un canvi de compte amb una cancel·lació; només la que ve de fora es propaga.
            currentCoroutineContext().ensureActive()
        } catch (error: Exception) {
            log("Sincronització fallida: ${error.javaClass.simpleName}")
            _state.value = State.Failed(ViraBackendError.UNAVAILABLE)
        } finally {
            identity = null
            isRunning.set(false)
        }
    }

    /// **Cada taula és independent**: una que falli no atura les següents (2026-08-12, una columna
    /// que el servidor no tenia va deixar catorze taules sense sincronitzar durant dies). El primer
    /// error es guarda i es llança al final, perquè Ajustos pugui dir per què.
    private suspend fun run() {
        val scope = ViraSyncScope.current(session, preferences)
        val token = session.accessToken()
        requireCurrentAccount()

        var firstFailure: Exception? = null
        suspend fun attempt(name: String, work: suspend () -> Unit) {
            try {
                work()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                log("Sincronització de $name fallida: ${(error as? ViraBackendException)?.error ?: error.javaClass.simpleName}")
                if (firstFailure == null) firstFailure = error
            }
        }

        attempt("tombstones") { flushTombstones(token) }
        for (table in tables) attempt(table.table) { sync(table, scope, token) }

        requireCurrentAccount()
        firstFailure?.let { throw it }
    }

    private suspend fun <R : ViraSyncRow> sync(table: ViraSyncTable<R>, scope: ViraSyncScope, token: String) {
        requireCurrentAccount()
        if (!scope.allows(table.layer) || !allowsNow(table)) return
        // La primera passada d'aquest compte en aquest telèfon mira el servidor **abans** d'enviar-hi
        // res: el que hi hagi aquí acabat de néixer no ha de trepitjar la història del compte.
        if (cursors.get("pull.${table.table}") == null) {
            pull(table, token)
            push(table, token)
        } else {
            push(table, token)
            pull(table, token)
        }
    }

    // Pujada

    private suspend fun <R : ViraSyncRow> push(table: ViraSyncTable<R>, token: String) {
        requireCurrentAccount()
        if (!allowsNow(table)) return
        val since = cursors.get("push.${table.table}")
        val startedAt = clock()
        val outgoing = outgoing(table.local(db), since)
        for (chunk in outgoing.chunked(PAGE_SIZE)) {
            requireCurrentAccount()
            if (!allowsNow(table)) return
            rest.upsert(
                table.table, chunk.map(table::encode), token,
                onConflict = table.conflictTarget,
                nullColumns = ViraSyncNullColumns.forTable(table.table),
            )
            requireCurrentAccount()
        }
        cursors.set("push.${table.table}", startedAt)
    }

    // Baixada

    private suspend fun <R : ViraSyncRow> pull(table: ViraSyncTable<R>, token: String) {
        requireCurrentAccount()
        val identity = identity ?: throw AccountChanged()
        val since = cursors.get("pull.${table.table}")
        var boundary: Param? = null
        var newest: Instant? = null

        while (true) {
            requireCurrentAccount()
            val query = buildList {
                add(Param("user_id", "eq.${identity.userId}"))
                add(Param("order", table.order))
                add(Param("limit", PAGE_SIZE.toString()))
                // Sense cursor és la primera vegada: es baixa tot, sense filtre de data.
                if (since != null) add(Param("updated_at", "gt.${ViraTime.format(since)}"))
                boundary?.let(::add)
            }
            val page = rest.select(table.table, query, token).map { it.jsonObject }
            requireCurrentAccount()
            if (!allowsNow(table)) return
            val batchNewest = page.map(table::decode).maxOfOrNull { it.updatedAt } ?: break
            // Si l'esborrat d'aquest telèfon encara no ha arribat al servidor (ha fallat), la fila
            // torna a baixar: no s'ha de ressuscitar.
            val buried: Set<UUID?> = db.tombstones().recordIDs(table.table).toSet()
            val rows = page.filter { it.recordID() !in buried }.map(table::decode)

            // Res d'això és un canvi local: s'escriu en cru, sense segellar. Si es datés amb l'hora
            // d'ara, es tornaria a enviar tot seguit i la pilota no pararia mai.
            db.withTransaction { table.apply(rows, db) }

            newest = maxOf(newest ?: batchNewest, batchNewest)
            if (page.size < PAGE_SIZE) break
            boundary = ViraSyncPage.after(ViraSyncPage.boundary(page.last(), table.tieColumns))
        }
        newest?.let { cursors.set("pull.${table.table}", it) }
    }

    // Esborrats

    /// Els esborrats van primer: no té sentit baixar-se una fila que aquest telèfon acaba de treure.
    private suspend fun flushTombstones(token: String) {
        requireCurrentAccount()
        val identity = identity ?: throw AccountChanged()
        val tombstones = db.tombstones().all()
        // A trossos: tots els ids van a l'adreça, i una de massa llarga no passaria mai.
        for (group in tombstones.groupBy { it.table }.values.flatMap { it.chunked(TOMBSTONE_CHUNK) }) {
            rest.delete(
                group.first().table,
                listOf(
                    Param("user_id", "eq.${identity.userId}"),
                    Param("id", "in.(${group.joinToString(",") { it.recordID.toString() }})"),
                ),
                token,
            )
            requireCurrentAccount()
            db.tombstones().deleteRaw(group)
        }
    }

    /// «Esborrar-ho tot», telèfon **i** compte (2026-08-12).
    ///
    /// **Primer el servidor**: si falla, no s'esborra res del telèfon i l'error puja a la pantalla.
    /// Val més no haver esborrat que dir que sí i veure-ho tornar demà. L'àmbit no hi pinta res:
    /// governa què **puja**, no què s'esborra.
    suspend fun eraseEverything(alsoErase: suspend () -> Unit = {}) {
        if (!isRunning.compareAndSet(false, true)) throw ViraBackendException(ViraBackendError.UNAVAILABLE)
        try {
            val user = session.user
            if (session.requiresAccount && user != null) {
                identity = Identity(user.id, session.dataGeneration)
                val token = session.accessToken()
                requireCurrentAccount()
                for (table in syncedTables) {
                    rest.delete(table, listOf(Param("user_id", "eq.${user.id}")), token)
                    requireCurrentAccount()
                }
                cursors.reset()
                _state.value = State.Succeeded(clock())
            }
            ViraDataEraser.eraseEverything(db, preferences, alsoErase)
        } finally {
            identity = null
            isRunning.set(false)
        }
    }

    /// Les taules d'aquest compte al servidor, fills abans que pares: l'ordre de pujada al revés.
    val syncedTables: List<String> = tables.map { it.table }.reversed()

    // Identitat de la passada

    /// La identitat de la passada, no la del moment en què acaba una petició: si entremig s'ha
    /// tancat la sessió o ha entrat un altre compte, el que arriba no és d'aquest telèfon.
    private data class Identity(val userId: UUID, val generation: UUID)

    private class AccountChanged : CancellationException("Ha canviat el compte")

    private fun requireCurrentAccount() {
        val identity = identity ?: throw AccountChanged()
        if (session.user?.id != identity.userId || session.dataGeneration != identity.generation) {
            throw AccountChanged()
        }
    }

    /// Torna a mirar l'àmbit en cada pas: apagar l'interruptor a mitja passada atura la pujada.
    private fun allowsNow(table: ViraSyncTable<*>) =
        ViraSyncScope.current(session, preferences).allows(table.layer)

    private fun JsonObject.recordID(): UUID? =
        (get("id") as? JsonPrimitive)?.contentOrNull?.let { runCatching { UUID.fromString(it) }.getOrNull() }

    private fun log(line: String) {
        try {
            Log.w("ViraSync", line)
        } catch (_: RuntimeException) {
            // Proves de JVM sense Android a sota.
        }
    }

    companion object {
        /// Files per pàgina. PostgREST en torna 1.000 com a màxim.
        const val PAGE_SIZE = 500

        /// Ids per esborrat: prou curt perquè l'adreça no passi de 8 kB.
        const val TOMBSTONE_CHUNK = 100

        /// Cada quant es torna a sincronitzar sola: cinc minuts, com a l'iPhone.
        val AUTOMATIC_INTERVAL: Duration = Duration.ofMinutes(5)

        /// Què surt en una pujada. Els singletons que ningú ha tocat aquí no surten mai: enviar un
        /// comptador buit seria esborrar la història del compte des d'un telèfon que no en sap res.
        fun <R : ViraSyncRow> outgoing(rows: List<R>, since: Instant?): List<R> =
            rows.filter { !it.isPlaceholder }.filter { since == null || it.updatedAt > since }
    }
}
