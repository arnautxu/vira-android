package cat.vira.android.features.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.backend.ViraSession
import cat.vira.android.core.backend.ViraUser
import cat.vira.android.core.backend.sync.ViraSyncEngine
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraTag
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.features.settings.export.ViraResourceNames

/// Les peces de dades que llegeixen les pantalles d'Ajustos.
val LocalSettingsBackend = staticCompositionLocalOf<SettingsBackend> { error("Ajustos sense backend") }

/// L'usuari connectat, o `null` sense sessió (o sense backend configurat).
@Composable
fun rememberSettingsUser(): ViraUser? {
    val state by LocalSettingsBackend.current.session.collectAsState()
    return (state as? ViraSession.State.SignedIn)?.user
}

/// Qui ets, en una fila: l'avatar amb la skin posada, el malnom i el correu. La comparteixen la
/// targeta de la portada i la del compte, totes dues sobre el degradat lila, així que el text va
/// en `onCard`. A mides d'accessibilitat el nom va a sota de l'avatar.
@Composable
fun SettingsIdentity(modifier: Modifier = Modifier) {
    val backend = LocalSettingsBackend.current
    val user = rememberSettingsUser()
    val nickname by backend.profileName.collectAsStateOr("")
    val skin by backend.equippedSkin.collectAsStateOr(null)
    val ink = ViraColor.onCard(ViraArea.Today)
    val name = nickname.trim().ifEmpty { stringResource(R.string.tab_profile) }
    val avatar = 60.dp * LocalDensity.current.fontScale.coerceAtMost(2f)

    val content: @Composable () -> Unit = {
        SkinAvatar(skin, Modifier.size(avatar))
        Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.textRelated)) {
            Text(name, style = ViraFont.title, color = ink)
            user?.email?.let { Text(it, style = ViraFont.footnote, color = ink) }
        }
    }
    if (LocalViraEnvironment.current.isAccessibilitySize) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(ViraSpacing.s)) { content() }
    } else {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) { content() }
    }
}

/// El retrat de la skin posada, o el Viri de sempre. Les skins per capes (l'exploradora) porten el
/// decorat a `<nom>-backdrop` i Viri retallat a sobre, com `SkinPortrait` de l'iPhone.
@Composable
fun SkinAvatar(skinKey: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val layers = remember(skinKey) {
        val asset = skinKey?.let { "viri-skin-$it" }
        val front = asset?.let { ViraResourceNames.drawable(context, it) }?.takeIf { it != 0 }
        val back = asset?.let { ViraResourceNames.drawable(context, "$it-backdrop") }?.takeIf { it != 0 }
        when {
            front == null -> listOf(R.drawable.viri_entry_waving)
            back != null -> listOf(back, front)
            else -> listOf(front)
        }
    }
    Box(
        modifier
            .clip(CircleShape)
            .background(ViraColor.brandSurface)
            .border(3.dp, ViraColor.surfaceElevated, CircleShape),
    ) {
        layers.forEach { layer ->
            Image(painterResource(layer), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

/// La píndola amb l'estat de la sincronització, sense endolcir-ho: si ha fallat, ho diu.
@Composable
fun SettingsSyncTag() {
    val user = rememberSettingsUser()
    val sync by LocalSettingsBackend.current.syncState.collectAsState()
    val text = when {
        user == null -> R.string.settings_account_local
        sync is ViraSyncEngine.State.Syncing -> R.string.settings_sync_status_working
        sync is ViraSyncEngine.State.Succeeded -> R.string.settings_sync_status_done
        sync is ViraSyncEngine.State.Failed -> R.string.settings_sync_status_failed
        else -> R.string.settings_sync_status_idle
    }
    val pictogram = when {
        sync is ViraSyncEngine.State.Failed && user != null -> R.drawable.picto_plan_important
        user == null -> R.drawable.picto_shield
        else -> R.drawable.picto_plan_check
    }
    ViraTag(stringResource(text), pictogram = pictogram)
}
