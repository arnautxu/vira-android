package cat.vira.android.features.parent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cat.vira.android.ViraApp
import cat.vira.android.core.backend.ViraSession
import cat.vira.android.core.data.ViraStore

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
