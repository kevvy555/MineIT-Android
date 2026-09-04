package com.mineit.android.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MineItPanel(
    modifier: Modifier = Modifier,
    raised: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .background(
                color = if (raised) MineItPalette.RaisedPanel else MineItPalette.Panel,
                shape = RoundedCornerShape(MineItRadius.Medium),
            )
            .border(1.dp, MineItPalette.Line, RoundedCornerShape(MineItRadius.Medium))
            .padding(MineItSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
        content = content,
    )
}

@Composable
fun MineItSectionHeader(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier,
    color: Color = MineItPalette.Accent,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MineItPalette.Muted)
        }
    }
}

@Composable
fun MineItResourceCard(
    label: String,
    value: String,
    detail: String? = null,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(accent.copy(alpha = .10f), RoundedCornerShape(MineItRadius.Small))
            .border(1.dp, accent.copy(alpha = .28f), RoundedCornerShape(MineItRadius.Small))
            .padding(horizontal = 5.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
        Text(value, style = MaterialTheme.typography.labelLarge, color = MineItPalette.Text, maxLines = 1)
        detail?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted, maxLines = 1)
        }
    }
}

@Composable
fun MineItStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    stateColor: Color = MineItPalette.Text,
) {
    Column(
        modifier = modifier
            .background(MineItPalette.Control, RoundedCornerShape(MineItRadius.Small))
            .border(1.dp, MineItPalette.Line, RoundedCornerShape(MineItRadius.Small))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MineItPalette.Muted, maxLines = 1)
        Text(value, style = MaterialTheme.typography.labelMedium, color = stateColor, maxLines = 1)
    }
}

@Composable
fun MineItStatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .background(color.copy(alpha = .13f), RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = .45f), RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MineItPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = MineItTouch.Minimum),
        enabled = enabled,
        shape = RoundedCornerShape(MineItRadius.Medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = MineItPalette.Accent.copy(alpha = .22f),
            contentColor = MineItPalette.Text,
            disabledContainerColor = MineItPalette.Control,
            disabledContentColor = MineItPalette.Disabled,
        ),
        border = BorderStroke(1.dp, if (enabled) MineItPalette.Accent else MineItPalette.Line),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun MineItSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    accent: Color = MineItPalette.Accent,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MineItTouch.Minimum),
        enabled = enabled,
        shape = RoundedCornerShape(MineItRadius.Medium),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) accent.copy(alpha = .15f) else Color.Transparent,
            contentColor = if (selected) accent else MineItPalette.Text,
            disabledContentColor = MineItPalette.Disabled,
        ),
        border = BorderStroke(1.dp, if (selected) accent else MineItPalette.Line),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun MineItDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MineItTouch.Minimum),
        enabled = enabled,
        shape = RoundedCornerShape(MineItRadius.Medium),
        border = BorderStroke(1.dp, MineItPalette.Critical.copy(alpha = if (enabled) .8f else .25f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MineItPalette.Critical.copy(alpha = if (enabled) .08f else .02f),
            contentColor = MineItPalette.Critical,
            disabledContentColor = MineItPalette.Disabled,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun MineItActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MineItSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
