package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.SystemEntity

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CompactSystemSelector(
    systems: List<SystemEntity>,
    selectedSystem: SystemEntity?,
    onSystemSelected: (SystemEntity) -> Unit,
    onOpenSystemManager: () -> Unit,
    onOpenMainMenu: () -> Unit = {},
    showSystemTitle: Boolean = true,
    isFocused: Boolean = false,
    focusedItemIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    if (!showSystemTitle) return

    val enabledSystems = remember(systems) { systems.filter { it.isEnabled } }
    val currentIndex = remember(enabledSystems, selectedSystem) {
        if (selectedSystem != null) {
            enabledSystems.indexOfFirst { it.id == selectedSystem.id }.coerceAtLeast(0)
        } else 0
    }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isFocused) 6.dp else 2.dp,
        border = if (isFocused) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Main Menu Direct Shortcut Icon on Left (Index 0)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (focusedItemIndex == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = if (focusedItemIndex == 0) androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                IconButton(
                    onClick = onOpenMainMenu,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dashboard,
                        contentDescription = "System Main Menu",
                        tint = if (focusedItemIndex == 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Previous Button (Index 1)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (focusedItemIndex == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                border = if (focusedItemIndex == 1) androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                IconButton(
                    onClick = {
                        if (enabledSystems.isNotEmpty()) {
                            val prevIndex = (currentIndex - 1 + enabledSystems.size) % enabledSystems.size
                            onSystemSelected(enabledSystems[prevIndex])
                        }
                    },
                    enabled = enabledSystems.size > 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous System",
                        tint = if (focusedItemIndex == 1) MaterialTheme.colorScheme.primary else if (enabledSystems.size > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            // Center Icon + Text Title + Quick Dropdown (Index 2)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (focusedItemIndex == 2) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        else Modifier
                    )
                    .clickable(enabled = enabledSystems.isNotEmpty()) {
                        isDropdownExpanded = true
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectedSystem != null && enabledSystems.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // System Icon Box
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            UniversalIconView(
                                iconNameOrPath = selectedSystem.iconName,
                                contentDescription = selectedSystem.name,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Title + Count
                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedSystem.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = if (focusedItemIndex == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "System List",
                                    tint = if (focusedItemIndex == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "${selectedSystem.shortName} (${currentIndex + 1}/${enabledSystems.size})",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No Systems Enabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Dropdown menu for quick switching
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    enabledSystems.forEachIndexed { idx, sys ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UniversalIconView(
                                        iconNameOrPath = sys.iconName,
                                        contentDescription = sys.name,
                                        tint = if (sys.id == selectedSystem?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sys.name,
                                        fontWeight = if (sys.id == selectedSystem?.id) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sys.id == selectedSystem?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            onClick = {
                                onSystemSelected(sys)
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Next Button (Index 3)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (focusedItemIndex == 3) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                border = if (focusedItemIndex == 3) androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                IconButton(
                    onClick = {
                        if (enabledSystems.isNotEmpty()) {
                            val nextIndex = (currentIndex + 1) % enabledSystems.size
                            onSystemSelected(enabledSystems[nextIndex])
                        }
                    },
                    enabled = enabledSystems.size > 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next System",
                        tint = if (focusedItemIndex == 3) MaterialTheme.colorScheme.primary else if (enabledSystems.size > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Dedicated System Manager Button (Index 4)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (focusedItemIndex == 4) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                border = if (focusedItemIndex == 4) androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                IconButton(
                    onClick = onOpenSystemManager,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Manage Systems",
                        tint = if (focusedItemIndex == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
