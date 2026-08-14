package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.db.GameRomEntity
import com.example.data.db.SystemEntity
import com.example.data.util.GameIconResolver
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDialog(
    allRoms: List<GameRomEntity>,
    systems: List<SystemEntity>,
    currentSystemId: String?,
    onDismiss: () -> Unit,
    onLaunchGame: (GameRomEntity) -> Unit
) {
    val context = LocalContext.current
    val customIcons = remember(context) {
        com.example.data.config.ConfigStorageManager(context).loadCustomIcons()
    }
    var searchQuery by remember { mutableStateOf("") }
    var filterByCurrentSystemOnly by remember { mutableStateOf(false) }

    val systemMap = remember(systems) { systems.associateBy { it.id } }

    val filteredRoms = remember(allRoms, searchQuery, filterByCurrentSystemOnly, currentSystemId) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allRoms.filter { rom ->
                val matchesSystem = if (filterByCurrentSystemOnly && currentSystemId != null) {
                    rom.systemId == currentSystemId
                } else true

                val matchesQuery = rom.title.contains(searchQuery, ignoreCase = true) ||
                        rom.fileName.contains(searchQuery, ignoreCase = true)

                matchesSystem && matchesQuery
            }
        }
    }

    ScaledDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Games",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Search Games & Apps",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Search across all emulator systems",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Type game title, filename or app name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Option Chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !filterByCurrentSystemOnly,
                        onClick = { filterByCurrentSystemOnly = false },
                        label = { Text("All Systems (${allRoms.size})") }
                    )

                    if (currentSystemId != null) {
                        val sysName = systemMap[currentSystemId]?.name ?: "Current System"
                        FilterChip(
                            selected = filterByCurrentSystemOnly,
                            onClick = { filterByCurrentSystemOnly = true },
                            label = { Text(sysName) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Results list
                if (searchQuery.isBlank()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start typing above to search games",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (filteredRoms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No games or apps found matching '$searchQuery'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredRoms, key = { it.id }) { rom ->
                            val system = systemMap[rom.systemId]

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onLaunchGame(rom)
                                        onDismiss()
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Icon / Artwork
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val actualSystem = remember(rom, systems) {
                                                systems.find { it.id == rom.systemId }
                                            }
                                            val isAndroidApp = rom.systemId in listOf("android_apps", "android_games", "android_emulators") ||
                                                    actualSystem?.defaultLaunchMode == "ANDROID_APP" ||
                                                    rom.systemId.startsWith("android_")

                                            if (isAndroidApp) {
                                                val appIcon = remember(rom.filePath) {
                                                    AndroidAppIconHelper.getAppIconBitmap(context, rom.filePath)
                                                }
                                                if (appIcon != null) {
                                                    Image(
                                                        bitmap = appIcon,
                                                        contentDescription = rom.title,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Smartphone,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            } else {
                                                val resolvedIcon = remember(rom, system, customIcons) {
                                                    GameIconResolver.resolveRomIcon(rom, system, customIcons, systems, context)
                                                }
                                                UniversalIconView(
                                                    iconNameOrPath = resolvedIcon,
                                                    contentDescription = rom.title,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = rom.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = system?.name ?: rom.systemId,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = " • .${rom.extension}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onLaunchGame(rom)
                                            onDismiss()
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
