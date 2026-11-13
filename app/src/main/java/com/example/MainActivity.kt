package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.SettingsScreen
import com.example.ui.TaggerWorkspaceScreen
import com.example.ui.theme.TagSwipeTheme
import com.example.viewmodel.TagSwipeViewModel

private const val ROUTE_WORKSPACE = "workspace"
private const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            TagSwipeTheme {
                val navController = rememberNavController()
                val viewModel: TagSwipeViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                val folderPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree(),
                ) { uri ->
                    if (uri != null) {
                        runCatching {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                            )
                        }
                        viewModel.onDirectoryPicked(uri)
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_WORKSPACE,
                ) {
                    composable(ROUTE_WORKSPACE) {
                        TaggerWorkspaceScreen(
                            uiState = uiState,
                            onPickDirectory = { folderPickerLauncher.launch(null) },
                            onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                            onSwipeDirection = viewModel::onSwipeDirection,
                            onTriggerDefaultSwipe = viewModel::onTriggerDefaultSwipe,
                            onPreviousImage = viewModel::onPreviousImage,
                            onToggleRandomOrder = viewModel::toggleRandomOrder,
                            onToggleWidescreenView = viewModel::toggleWidescreenView,
                            onSelectMode = viewModel::setMode,
                            onUpdateDirectionTag = viewModel::updateDirectionTag,
                            onSelectDefaultDirection = viewModel::setDefaultDirection,
                            onAddManualTags = viewModel::onAddManualTags,
                            onRemoveTag = viewModel::onRemoveTag,
                            onRequestDelete = viewModel::onDeleteRequested,
                            onConfirmDelete = viewModel::executeDeleteCurrent,
                            onCancelDelete = viewModel::cancelDelete,
                            onHardwareKeyDown = viewModel::onHardwareKeyDown,
                            onDismissBanner = viewModel::dismissBanner,
                        )
                    }

                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            settings = uiState.settings,
                            onBack = { navController.popBackStack() },
                            onPickDirectory = { folderPickerLauncher.launch(null) },
                            onSelectMode = viewModel::setMode,
                            onSelectLayoutMode = viewModel::setLayoutMode,
                            onSetRandomOrder = viewModel::setRandomOrder,
                            onSetConfirmBeforeDelete = viewModel::setConfirmBeforeDelete,
                            onSetAutoAdvanceOnHotkey = viewModel::setAutoAdvanceOnHotkey,
                            onSetDefaultDirection = viewModel::setDefaultDirection,
                            onUpdateDirectionTag = viewModel::updateDirectionTag,
                            onUpdateHotkey = viewModel::updateHotkey,
                            onResetDirections = viewModel::resetDirectionsToDefaults,
                            onResetHotkeys = viewModel::resetHotkeysToDefaults,
                        )
                    }
                }
            }
        }
    }
}
