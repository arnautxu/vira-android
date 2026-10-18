package cat.vira.android.features.parent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cat.vira.android.ViraApp
import cat.vira.android.core.backend.ViraSession
import cat.vira.android.core.data.ViraStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/// El que fan servir les pantalles del panell: la sessió (per al testimoni), els dos clients i,
/// per acceptar un recordatori, el magatzem local.
internal class ParentDeps(
    val session: ViraSession,
    val store: ViraStore,
    val client: ParentLinkClient = ParentLinkClient(),
    val loader: ParentChildSummaryLoader = ParentChildSummaryLoader(),
)

@Composable
internal fun rememberParentDeps(): ParentDeps {
    val app = LocalContext.current.applicationContext as ViraApp
    return remember(app) { ParentDeps(app.backend.session, ViraStore(app.backend.db)) }
}

/// On viuen els desats que no poden morir amb la pantalla. Tancar un àmbit i tocar Enrere de
/// seguida ha de tancar-lo igualment: si el desat anés lligat a la pantalla, es cancel·laria a mig
/// camí i el pare o la mare seguiria veient el que el fill o la filla creu que ha tancat.
internal val parentSaveScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
