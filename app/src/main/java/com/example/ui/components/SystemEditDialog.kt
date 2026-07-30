package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.data.db.StandaloneProfileEntity
import com.example.data.db.SystemEntity

@Composable
fun SystemEditDialog(
    system: SystemEntity?,
    standaloneProfiles: List<StandaloneProfileEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (SystemEntity) -> Unit,
    onDelete: ((SystemEntity) -> Unit)? = null,
    onOpenAppVisibility: (() -> Unit)? = null
) {
    if (system != null) {
        SystemEditDetailDialog(
            system = system,
            isNew = false,
            onDismiss = onDismiss,
            onSave = onSave,
            onOpenAppVisibility = onOpenAppVisibility
        )
    }
}
