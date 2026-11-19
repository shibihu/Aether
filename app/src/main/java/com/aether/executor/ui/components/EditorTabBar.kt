package com.aether.executor.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.executor.data.model.EditorTab
import com.aether.executor.ui.theme.AetherPalette

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorTabBar(
    tabs: List<EditorTab>,
    activeTabId: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onRename: (String) -> Unit,
    onNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tabs, key = { it.id }) { tab ->
                val active = tab.id == activeTabId
                Row(
                    modifier = Modifier
                        .width(168.dp)
                        .background(
                            if (active) AetherPalette.PanelAlt else AetherPalette.Panel,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (active) AetherPalette.Cyan else AetherPalette.Border,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .combinedClickable(
                            onClick = { onSelect(tab.id) },
                            onLongClick = { onRename(tab.id) }
                        )
                        .padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tab.dirty) {
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(6.dp)
                                .background(AetherPalette.Amber, CircleShape)
                        )
                    }
                    Text(
                        text = tab.title,
                        color = if (active) AetherPalette.Cyan else AetherPalette.Text,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onClose(tab.id) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close ${tab.title}",
                            tint = AetherPalette.Muted,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
        IconButton(onClick = onNew, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "New tab",
                tint = AetherPalette.Cyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
