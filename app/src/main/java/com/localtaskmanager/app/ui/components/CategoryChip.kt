package com.localtaskmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localtaskmanager.app.domain.model.Category

@Composable
fun CategoryChip(
    category: Category,
    modifier: Modifier = Modifier
) {
    val categoryColor = Color(category.colorArgb)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(color = categoryColor.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color = categoryColor, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = category.name,
            color = categoryColor,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
