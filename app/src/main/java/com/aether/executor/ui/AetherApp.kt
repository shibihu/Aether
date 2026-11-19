package com.aether.executor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aether.executor.data.model.AppSection
import com.aether.executor.ui.components.statusColor
import com.aether.executor.ui.components.statusLabel
import com.aether.executor.ui.screens.EditorScreen
import com.aether.executor.ui.screens.HistoryScreen
import com.aether.executor.ui.screens.ScriptsScreen
import com.aether.executor.ui.screens.SettingsScreen
import com.aether.executor.ui.theme.AetherPalette

private data class NavItem(
    val section: AppSection,
    val label: String,
    val icon: ImageVector
)

private val navItems = listOf(
    NavItem(AppSection.EDITOR, "Editor", Icons.Filled.Edit),
    NavItem(AppSection.SCRIPTS, "Scripts", Icons.Filled.List),
    NavItem(AppSection.HISTORY, "History", Icons.Filled.DateRange),
    NavItem(AppSection.SETTINGS, "Settings", Icons.Filled.Settings)
)

@Composable
fun AetherApp(vm: AetherViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
            vm.consumeMessage()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = AetherPalette.Navy) {
        Scaffold(
            containerColor = AetherPalette.Navy,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { BrandBar(state) },
            bottomBar = {
                NavigationBar(
                    containerColor = AetherPalette.Panel,
                    contentColor = AetherPalette.Text,
                    tonalElevation = 0.dp
                ) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = state.section == item.section,
                            onClick = { vm.selectSection(item.section) },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AetherPalette.Cyan,
                                selectedTextColor = AetherPalette.Cyan,
                                indicatorColor = AetherPalette.Cyan.copy(alpha = 0.12f),
                                unselectedIconColor = AetherPalette.Muted,
                                unselectedTextColor = AetherPalette.Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (state.section) {
                    AppSection.EDITOR -> {
                        if (!state.tabsReady) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Restoring session…",
                                    color = AetherPalette.Muted,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            EditorScreen(
                                state = state,
                                onSelectTab = vm::selectTab,
                                onNewTab = vm::newTab,
                                onCloseTab = vm::closeTab,
                                onRenameTab = vm::renameTab,
                                onContentChange = vm::updateTabContent,
                                onSave = vm::saveActiveTab,
                                onExecute = vm::execute,
                                onStop = vm::stop,
                                onClearConsole = vm::clearConsole
                            )
                        }
                    }

                    AppSection.SCRIPTS -> ScriptsScreen(
                        scripts = state.scripts,
                        onCreate = vm::createScript,
                        onOpen = vm::openScript,
                        onRename = vm::renameScript,
                        onDelete = vm::deleteScript
                    )

                    AppSection.HISTORY -> HistoryScreen(
                        records = state.history,
                        onReopen = vm::reopenHistory,
                        onDelete = vm::deleteHistory,
                        onClear = vm::clearHistory
                    )

                    AppSection.SETTINGS -> SettingsScreen(
                        settings = state.settings,
                        onChange = vm::updateSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandBar(state: AetherUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AetherPalette.NavyDeep)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    if (state.isRunning) AetherPalette.Green else AetherPalette.Cyan,
                    CircleShape
                )
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "AETHER // EXECUTOR",
                color = AetherPalette.Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Local QuickJS sandbox",
                color = AetherPalette.Muted,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.6.sp
            )
        }
        Text(
            text = statusLabel(state.status),
            color = statusColor(state.status),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.size(2.dp))
    }
}
