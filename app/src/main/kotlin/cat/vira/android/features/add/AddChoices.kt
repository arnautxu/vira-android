package cat.vira.android.features.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cat.vira.android.R
import cat.vira.android.core.design.components.ViraPictogram
import cat.vira.android.core.design.components.ViraPictogramName
import cat.vira.android.core.design.components.ViraTile
import cat.vira.android.core.design.components.ViraTileStyle
import cat.vira.android.core.design.components.viraPressable
import cat.vira.android.core.design.tokens.ViraArea
import cat.vira.android.core.design.tokens.ViraColor
import cat.vira.android.core.design.tokens.ViraFont
import cat.vira.android.core.design.tokens.ViraGradient
import cat.vira.android.core.design.tokens.ViraRadius
import cat.vira.android.core.design.tokens.ViraSpacing
import cat.vira.android.core.design.tokens.ViraTouch

/// Targeta de color amb les respostes d'un pas (`AddChoiceCard`): la capçalera amb una píndola
/// («Un toc», «Opcional») i, a dins, files o fitxes blanques. La forma d'«El teu dia».
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddChoiceCard(
    title: String,
    pill: String? = null,
    area: ViraArea = ViraArea.Plan,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Column(
        Modifier.fillMaxWidth().background(ViraGradient.card(area), shape).padding(ViraSpacing.s),
        verticalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ViraSpacing.xs),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = ViraFont.headline, color = ViraColor.onCard(area), modifier = Modifier.semantics { heading() })
            if (pill != null) {
                Text(
                    pill,
                    style = ViraFont.footnoteEmphasis,
                    color = ViraColor.onCard(area),
                    modifier = Modifier
                        .border(1.5.dp, ViraColor.onCard(area), CircleShape)
                        .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xxs),
                )
            }
        }
        content()
    }
}

/// Una resposta d'un toc (`AddChoiceRow`): fila blanca amb pictograma, títol, una línia de suport i
/// el cercle que es marca. La marca és un check, a més del color.
@Composable
fun AddChoiceRow(
    pictogram: String,
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    area: ViraArea = ViraArea.Plan,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(ViraRadius.m)
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ViraTouch.comfortable)
            .clip(shape)
            .background(ViraColor.surfaceElevated, shape)
            .viraPressable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { selected = isSelected }
            .padding(horizontal = ViraSpacing.s, vertical = ViraSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ViraSpacing.s),
    ) {
        ViraPictogram(pictogram, size = 32.dp, scalesWithText = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ViraSpacing.hairline)) {
            Text(title, style = ViraFont.headline, color = ViraColor.textPrimary)
            if (subtitle != null) Text(subtitle, style = ViraFont.footnote, color = ViraColor.textSecondary)
        }
        AddChoiceMark(isSelected, area)
    }
}

/// Fitxa triable d'una graella (`AddChoiceTile`): `ViraTile` blanca amb contorn i check quan és la triada.
@Composable
fun AddChoiceTile(
    pictogram: String,
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    area: ViraArea = ViraArea.Plan,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(ViraRadius.l)
    Box(
        modifier
            .viraPressable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { selected = isSelected },
    ) {
        ViraTile(
            area = area,
            pictogram = ViraPictogramName.drawable(pictogram) ?: R.drawable.picto_flag,
            title = title,
            subtitle = subtitle,
            style = ViraTileStyle.Plain,
            modifier = if (isSelected) Modifier.border(2.5.dp, ViraColor.accent(area), shape) else Modifier,
        )
        if (isSelected) {
            AddChoiceMark(true, area, Modifier.align(Alignment.TopEnd).padding(ViraSpacing.xs))
        }
    }
}

/// El cercle d'una resposta: buit, o ple amb un check.
@Composable
fun AddChoiceMark(isSelected: Boolean, area: ViraArea = ViraArea.Plan, modifier: Modifier = Modifier) {
    val side = 26.dp * LocalDensity.current.fontScale.coerceAtMost(2f)
    if (isSelected) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ViraColor.accent(area), modifier = modifier.size(side))
    } else {
        // El cercle buit del símbol: un anell fi a la mida del ple.
        Box(modifier.size(side).padding(side / 12).border(1.5.dp, ViraColor.textSecondary, CircleShape))
    }
}
