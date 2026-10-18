package cat.vira.android.features.parent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.viraLocale
import cat.vira.android.core.design.LocalViraEnvironment
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing

/// Píndola d'àmbit. L'obert porta el pictograma sobre lila; el tancat, la clau, contorn i el mot
/// «Tancat» per a TalkBack: mai només el color (`DESIGN_RULES.md` §4).
@Composable
internal fun ParentScopePill(scope: ParentScope, isOpen: Boolean, modifier: Modifier = Modifier) {
    val title = stringResource(scope.title)
    val spoken = if (isOpen) title else "$title, ${stringResource(R.string.parent_tile_closed_tag)}"
    val contrast = LocalViraEnvironment.current.increasedContrast
    Row(
        modifier = modifier
            .background(if (isOpen) ViraColor.brandSurface else Color.Transparent, CircleShape)
            .then(
                if (!isOpen || contrast) {
                    Modifier.border(1.5.dp, if (contrast) ViraColor.textPrimary else ViraColor.separator, CircleShape)
                } else Modifier,
            )
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs + ViraSpacing.hairline),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xxs),
    ) {
        ViraPictogram(
            if (isOpen) scope.pictogram else R.drawable.picto_key,
            size = 18.dp, scalesWithText = true,
            modifier = Modifier.alpha(if (isOpen) 1f else 0.7f),
        )
        Text(
            title,
            style = if (isOpen) ViraFont.footnoteEmphasis else ViraFont.footnote,
            color = if (isOpen) ViraColor.brandOnSurface else ViraColor.textSecondary,
        )
    }
}

/// La targeta d'un fill o filla a l'arrel del panell: inicial, nom (si l'ha obert), des de quan
/// i els quatre àmbits.
@Composable
internal fun ParentChildCard(link: ParentLink, name: String?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(ViraRadius.l)
    val environment = LocalViraEnvironment.current
    val title = name ?: stringResource(R.string.parent_child_title)
    val since = stringResource(R.string.parent_child_since, dayMonth(link.createdAt, viraLocale()))
    val closedTag = stringResource(R.string.parent_tile_closed_tag)
    val labels = ParentScope.entries.map { stringResource(it.title) }
    val scopes = ParentScope.entries.zip(labels).joinToString(", ") { (scope, label) ->
        if (scope.isOpen(link)) label else "$label, $closedTag"
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(3.dp, shape, ambientColor = ViraColor.shadow.copy(alpha = 0.05f), spotColor = ViraColor.shadow.copy(alpha = 0.05f))
            .background(ViraColor.surfaceElevated, shape)
            .then(if (environment.increasedContrast) Modifier.border(2.dp, ViraColor.textPrimary, shape) else Modifier)
            .viraPressable(onClick = onOpen)
            .clearAndSetSemantics { contentDescription = "$title, $since, $scopes" }
            .padding(ViraSpacing.m),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s)) {
            Avatar(name)
            Column(Modifier.weight(1f)) {
                Text(title, style = ViraFont.headline, color = ViraColor.textPrimary)
                Text(since, style = ViraFont.footnote, color = ViraColor.textSecondary)
            }
            ParentChevron()
        }
        // Dues per fila; a mides d'accessibilitat, una sota l'altra.
        if (environment.isAccessibilitySize) {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                ParentScope.entries.forEach { ParentScopePill(it, it.isOpen(link)) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                listOf(ParentScope.USAGE to ParentScope.HABITS, ParentScope.EMOTIONS to ParentScope.MEDICATION).forEach { (a, b) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs)) {
                        ParentScopePill(a, a.isOpen(link))
                        ParentScopePill(b, b.isOpen(link))
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(name: String?) {
    val side = 48.dp * LocalDensity.current.fontScale.coerceAtMost(1.6f)
    val initial = name?.trim()?.firstOrNull()?.uppercase()
    Box(
        Modifier
            .size(side)
            .background(ViraColor.brandSurface, CircleShape)
            .border(2.dp, ViraColor.brand, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            Text(initial, style = ViraFont.title, color = ViraColor.brandOnSurface)
        } else {
            ViraPictogram(R.drawable.picto_profile, size = side * 0.8f)
        }
    }
}

@Composable
internal fun ParentChevron() {
    val ink = ViraColor.textSecondary
    val side = 14.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
    Canvas(Modifier.size(width = side * 0.7f, height = side)) {
        val path = Path().apply {
            moveTo(size.width * 0.3f, size.height * 0.12f)
            lineTo(size.width * 0.75f, size.height * 0.5f)
            lineTo(size.width * 0.3f, size.height * 0.88f)
        }
        drawPath(path, ink, style = Stroke(size.width * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
