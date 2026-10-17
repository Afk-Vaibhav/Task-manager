package com.localtaskmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localtaskmanager.app.domain.model.Priority
import com.localtaskmanager.app.ui.theme.PriorityHighColor
import com.localtaskmanager.app.ui.theme.PriorityLowColor
import com.localtaskmanager.app.ui.theme.PriorityNormalColor

@Composable
fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (priority) {
        Priority.HIGH -> PriorityHighColor.copy(alpha = 0.15f) to PriorityHighColor
        Priority.NORMAL -> PriorityNormalColor.copy(alpha = 0.15f) to PriorityNormalColor
        Priority.LOW -> PriorityLowColor.copy(alpha = 0.15f) to PriorityLowColor
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = priority.displayName,
            color = textColor,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
