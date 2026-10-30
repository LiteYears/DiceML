package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.DiceDatabase
import com.example.data.DiceRepository
import com.example.ui.DiceGameUiState
import com.example.ui.DiceGameViewModel
import com.example.ui.components.CompactBettingAndRollPadCard
import com.example.ui.components.Custom3DDicePairStage
import com.example.ui.components.ManualOrEditRollDialog
import com.example.ui.components.PermanentResponsibleNoticeBanner
import com.example.ui.components.SavedWeightsManagerDialog
import com.example.ui.components.StreamlinedRollHistorySection
import com.example.ui.components.WinningBetAndAutoLearnCard
import com.example.ui.screens.AnalyticsDeepDiveScreen
import com.example.ui.screens.HistoryManagementScreen
import com.example.ui.screens.StatisticalSettingsScreen
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DeepFeltSurface
import com.example.ui.theme.ElevatedFeltCard
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GlassFeltBorder
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.ImperialGoldLight
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextPrimaryCream
import com.example.ui.theme.TextSecondarySage
import com.example.ui.theme.WinGreen
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val repository = remember(context) {
                    DiceRepository(DiceDatabase.getInstance(context).diceRollDao())
                }
                val viewModel: DiceGameViewModel = viewModel(
                    factory = DiceGameViewModel.provideFactory(repository)
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                DiceIntelligenceApp(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun DiceIntelligenceApp(
    uiState: DiceGameUiState,
    viewModel: DiceGameViewModel
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    if (selectedTab != 0) {
        BackHandler {
            selectedTab = 0
        }
    }

    if (uiState.controls.showManualRollSheet || uiState.controls.editingRollRecord != null) {
        val editing = uiState.controls.editingRollRecord
        ManualOrEditRollDialog(
            editingRecord = editing,
            onDismiss = {
                viewModel.toggleManualRollSheet(false)
                viewModel.dismissEditRollDialog()
            },
            onSaveRoll = { d1, d2 ->
                if (editing != null) {
                    viewModel.updateExistingRoll(editing, d1, d2)
                } else {
                    viewModel.recordExternalRoll(d1, d2)
                }
            }
        )
    }

    if (uiState.controls.showWeightsManagerSheet) {
        SavedWeightsManagerDialog(
            currentEpochs = uiState.mlReport.trainedEpochsCount,
            currentSamples = uiState.mlReport.totalHistoricalSamples,
            currentBestPick = uiState.mlReport.smartPickCategory,
            savedCheckpoints = uiState.savedCheckpoints,
            onDismiss = { viewModel.toggleWeightsManagerSheet(false) },
            onSaveNewCheckpoint = { label -> viewModel.saveCurrentWeightsCheckpoint(label) },
            onLoadCheckpoint = { cp -> viewModel.loadWeightsCheckpoint(cp) },
            onDeleteCheckpoint = { id -> viewModel.deleteWeightsCheckpoint(id) }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AmoledBlack),
        containerColor = AmoledBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopCompactStatusHeader(
                bankrollEur = uiState.controls.bankrollEur,
                netProfitEur = uiState.netSessionProfitEur,
                totalRolls = uiState.rollsDesc.size,
                onOpenWeightsModal = { viewModel.toggleWeightsManagerSheet(true) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DeepFeltSurface,
                contentColor = TextPrimaryCream,
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                NavigationBarItem(
                    selected = (selectedTab == 0),
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Dashboard", maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AmoledBlack,
                        selectedTextColor = ImperialGoldLight,
                        indicatorColor = ImperialGold,
                        unselectedIconColor = TextSecondarySage,
                        unselectedTextColor = TextSecondarySage
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = (selectedTab == 1),
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("History", maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AmoledBlack,
                        selectedTextColor = ImperialGoldLight,
                        indicatorColor = ImperialGold,
                        unselectedIconColor = TextSecondarySage,
                        unselectedTextColor = TextSecondarySage
                    ),
                    modifier = Modifier.testTag("nav_tab_history")
                )

                NavigationBarItem(
                    selected = (selectedTab == 2),
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                            contentDescription = "Analytics"
                        )
                    },
                    label = { Text("Analytics", maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AmoledBlack,
                        selectedTextColor = ImperialGoldLight,
                        indicatorColor = ImperialGold,
                        unselectedIconColor = TextSecondarySage,
                        unselectedTextColor = TextSecondarySage
                    ),
                    modifier = Modifier.testTag("nav_tab_analytics")
                )

                NavigationBarItem(
                    selected = (selectedTab == 3),
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings", maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AmoledBlack,
                        selectedTextColor = ImperialGoldLight,
                        indicatorColor = ImperialGold,
                        unselectedIconColor = TextSecondarySage,
                        unselectedTextColor = TextSecondarySage
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.widthIn(max = 560.dp)) {
                when (selectedTab) {
                    0 -> MainStatisticalDashboardScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    1 -> HistoryManagementScreen(
                        rollsDesc = uiState.rollsDesc,
                        rollsChronological = uiState.rollsChronological,
                        selectedFilter = uiState.controls.historyFilter,
                        sortAscending = uiState.controls.historySortAscending,
                        showCsvDialog = uiState.controls.showCsvExportDialog,
                        exportedCsvText = uiState.exportedCsvText,
                        onSelectFilter = viewModel::setHistoryFilter,
                        onToggleSortOrder = viewModel::toggleHistorySortOrder,
                        onOpenAddManualRoll = { viewModel.toggleManualRollSheet(true) },
                        onOpenEditRoll = viewModel::openEditRollDialog,
                        onDeleteRoll = viewModel::deleteRoll,
                        onClearAll = viewModel::clearAllHistoryAndReset,
                        onToggleCsvDialog = viewModel::toggleCsvExportDialog
                    )
                    2 -> AnalyticsDeepDiveScreen(
                        mlReport = uiState.mlReport,
                        currentStakeEur = uiState.controls.currentStakeEur
                    )
                    3 -> StatisticalSettingsScreen(
                        config = uiState.controls.statisticalConfig,
                        currentStakeEur = uiState.controls.currentStakeEur,
                        onUpdateStake = viewModel::updateCurrentStake,
                        onUpdateConfig = viewModel::updateStatisticalConfig
                    )
                }
            }
        }
    }
}

@Composable
private fun TopCompactStatusHeader(
    bankrollEur: Double,
    netProfitEur: Double,
    totalRolls: Int,
    onOpenWeightsModal: () -> Unit
) {
    Surface(
        color = DeepFeltSurface,
        border = BorderStroke(1.dp, GlassFeltBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dice Intelligence",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryCream,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$totalRolls Rolls • Start 3 & 2",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondarySage,
                    modifier = Modifier.testTag("total_recorded_rolls_text")
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenWeightsModal),
                    color = ElevatedFeltCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GlassFeltBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.US, "€%.2f", bankrollEur),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryCream
                        )
                        val sign = if (netProfitEur >= 0.0) " +" else " "
                        Text(
                            text = String.format(Locale.US, "%s%.1f€", sign, netProfitEur),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (netProfitEur >= 0.0) WinGreen else LossCrimson
                        )
                    }
                }
            }
        }
    }
}

/**
 * Clean, Compact 4-Card Dashboard that fits comfortably on a phone screen with zero clutter:
 * 1. Winning Bet Pick & Auto-Learn / Save / Load Strip
 * 2. Compact 3D Dice Pair Stage (Start 3 & 2)
 * 3. Compact 3-Bet Selector + Stake + Roll Dice Button
 * 4. Compact Recent Rolls Strip + Single-line Notice
 */
@Composable
private fun MainStatisticalDashboardScreen(
    uiState: DiceGameUiState,
    viewModel: DiceGameViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("game_table_arena_list"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Hero Winning Bet Callout + Auto-Learn & Save/Load Weights Strip
        item {
            WinningBetAndAutoLearnCard(
                mlReport = uiState.mlReport,
                isAutoLearningBatch = uiState.controls.isAutoLearningBatch,
                autoLearnProgressText = uiState.controls.autoLearnProgressText,
                weightsStatusToast = uiState.controls.weightsStatusToast,
                savedCheckpointsCount = uiState.savedCheckpoints.size,
                onSelectRecommendedBet = { viewModel.applyMlRecommendation(false) },
                onRunAutoLearnBatch = { viewModel.runAutoLearnSession(autoRollCount = 20, extraEpochs = 25) },
                onQuickSaveWeights = { viewModel.saveCurrentWeightsCheckpoint(null) },
                onOpenWeightsManager = { viewModel.toggleWeightsManagerSheet(true) }
            )
        }

        // 2. Compact 3D Two-Dice Stage (Initial visible faces: Die 1 = 3, Die 2 = 2)
        item {
            Custom3DDicePairStage(
                die1 = uiState.controls.displayedDie1,
                die2 = uiState.controls.displayedDie2,
                isAtCanonicalStart3And2 = uiState.controls.isAtCanonicalStart3And2,
                isRolling = uiState.controls.isRollingAnimation,
                lastRollBanner = uiState.controls.lastRollBanner,
                onResetTo3And2 = viewModel::resetDiceToStart3And2
            )
        }

        // 3. Compact Betting Pad (Under 2-6 | Seven 7 | Over 8-12), Stake (€1–€30), and Roll Button
        item {
            CompactBettingAndRollPadCard(
                mlReport = uiState.mlReport,
                selectedBet = uiState.controls.selectedBet,
                currentStakeEur = uiState.controls.currentStakeEur,
                isRolling = uiState.controls.isRollingAnimation,
                onSelectBet = viewModel::selectBet,
                onUpdateStake = viewModel::updateCurrentStake,
                onStepStake = viewModel::stepCurrentStake,
                onThrowDice = viewModel::throwDice,
                onOpenManualRollModal = { viewModel.toggleManualRollSheet(true) }
            )
        }

        // 4. Compact Recent Rolls Strip
        item {
            StreamlinedRollHistorySection(
                rollsDesc = uiState.rollsDesc,
                selectedFilter = uiState.controls.historyFilter,
                onSelectFilter = viewModel::setHistoryFilter,
                onClearHistory = viewModel::clearAllHistoryAndReset
            )
        }

        // 5. Compact 1-Line Statistical Notice
        item {
            PermanentResponsibleNoticeBanner()
        }
    }
}
