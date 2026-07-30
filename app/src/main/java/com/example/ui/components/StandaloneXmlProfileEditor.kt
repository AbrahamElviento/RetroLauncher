package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.config.ConfigStorageManager
import com.example.data.db.StandaloneProfileEntity

@Composable
fun StandaloneXmlProfileEditor(
    profile: StandaloneProfileEntity?,
    configStorageManager: ConfigStorageManager,
    onDismiss: () -> Unit,
    onSave: (StandaloneProfileEntity) -> Unit
) {
    var rawXml by remember {
        mutableStateOf(
            profile?.rawXmlContent?.ifBlank {
                configStorageManager.generateProfileXmlString(profile)
            } ?: """
<emulator id="azaharplusplus" name="Azahar++ / Citra (3DS)">
  <package>io.github.azahar.emulator</package>
  <activity>org.citra.citra_emu.ui.main.MainActivity</activity>
  <action>android.intent.action.VIEW</action>
  <romPathExtraKey>bootPath</romPathExtraKey>
</emulator>
            """.trimIndent()
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    ScaledDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CUSTOM STANDALONE XML EMULATOR",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Quick XML Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            rawXml = """
<emulator id="azaharplusplus" name="Azahar++ / Citra (3DS)">
  <package>io.github.azahar.emulator</package>
  <activity>org.citra.citra_emu.ui.main.MainActivity</activity>
  <action>android.intent.action.VIEW</action>
  <romPathExtraKey>bootPath</romPathExtraKey>
</emulator>
                            """.trimIndent()
                        },
                        label = { Text("Azahar++") }
                    )
                    AssistChip(
                        onClick = {
                            rawXml = """
<emulator id="nethersx2" name="NetherSX2 / AetherSX2 (PS2)">
  <package>net.nethersx2.android</package>
  <activity>xyz.aethersx2.android.MainActivity</activity>
  <action>android.intent.action.MAIN</action>
  <romPathExtraKey>bootPath</romPathExtraKey>
</emulator>
                            """.trimIndent()
                        },
                        label = { Text("NetherSX2") }
                    )
                    AssistChip(
                        onClick = {
                            rawXml = """
<emulator id="ppsspp_gold" name="PPSSPP Gold (PSP)">
  <package>org.ppsspp.ppssppgold</package>
  <activity>org.ppsspp.ppsspp.PpssppActivity</activity>
  <action>android.intent.action.VIEW</action>
  <romPathExtraKey>PATH</romPathExtraKey>
</emulator>
                            """.trimIndent()
                        },
                        label = { Text("PPSSPP") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Monospace Code Editor
                OutlinedTextField(
                    value = rawXml,
                    onValueChange = {
                        rawXml = it
                        errorMessage = null
                    },
                    label = { Text("Custom Launcher XML Configuration") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val parsed = configStorageManager.parseProfileXmlString(rawXml)
                            if (parsed == null || parsed.packageName.isBlank()) {
                                errorMessage = "Invalid XML format or missing <package> tag!"
                            } else {
                                onSave(parsed.copy(rawXmlContent = rawXml))
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Validate & Save Profile")
                    }
                }
            }
        }
    }
}
