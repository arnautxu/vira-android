package cat.vira.android.core.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cat.vira.android.R

/// Marcador de l'esquelet: el que es veu a una pestanya mentre el seu fil encara no l'ha
/// calcat. Desapareix quan l'última pestanya és feta.
@Composable
fun PendingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.skeleton_pending),
            textAlign = TextAlign.Center,
        )
    }
}
