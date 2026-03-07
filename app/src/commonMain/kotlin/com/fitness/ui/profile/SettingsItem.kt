package com.fitness.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SettingsItem(
    icon: ImageVector, 
    title: String, 
    supportingText: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.clickable { onClick() }
    )
}
