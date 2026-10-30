package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.BetCategory
import com.example.data.DiceRollRecord
import com.example.domain.MlPredictionReport
import com.example.domain.stats.StatisticalConfig
import com.example.ui.components.PermanentResponsibleNoticeBanner
import com.example.ui.components.categoryAccentColor
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DeepFeltSurface
import com.example.ui.theme.ElevatedFeltCard
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GlassFeltBorder
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.ImperialGoldLight
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.OverCoral
import com.example.ui.theme.SevenGold
import com.example.ui.theme.TextPrimaryCream
import com.example.ui.theme.TextSecondarySage
import com.example.ui.theme.UnderCyan
import com.example.ui.theme.WinGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryManagementScreen(
    rollsDesc: List<DiceRollRecord>,
    rollsChronological: List<DiceRollRecord>,
    selectedFilter: String,
    sortAscending: Boolean,
    showCsvDialog: Boolean,
    exportedCsvText: String,
    onSelectFilter: (String) -> Unit,
    onToggleSortOrder: () -> Unit,
    onOpenAddManualRoll: () -> Unit,
    onOpenEditRoll: (DiceRollRecord) -> Unit,
    onDeleteRoll: (Long) -> Unit,
    onClearAll: () -> Unit,
    onToggleCsvDialog: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val baseRolls = if (sortAscending) rollsChronological else rollsDesc
    val filteredRolls = when (selectedFilter) {
        "UNDER" -> baseRolls.filter { it.outcomeCategory == BetCategory.UNDER.id }
        "SEVEN" -> baseRolls.filter { it.outcomeCategory == BetCategory.SEVEN.id }
        "OVER" -> baseRolls.filter { it.outcomeCategory == BetCategory.OVER.id }
        else -> baseRolls
    }

    if (showCsvDialog) {
        AlertDialog(
            onDismissRequest = { onToggleCsvDialog(false) },
            containerColor = DeepFeltSurface,
            titleContentColor = TextPrimaryCream,
            textContentColor = TextSecondarySage,
            title = { Text("Export Roll History (CSV)") },
            text = {
                Column {
                    Text(
                        text = "${rollsDesc.size} records formatted with Unique ID, Die 1, Die 2, Sum, Classification, and ISO Timestamp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySage
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = AmoledBlack,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GlassFeltBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exportedCsvText.lineSequence().take(10).joinToString("\n"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimaryCream,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "Dice Intelligence Roll History CSV")
                            putExtra(Intent.EXTRA_TEXT, exportedCsvText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Export CSV"))
                        onToggleCsvDialog(false)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImperialGold,
                        contentColor = AmoledBlack
                    )
                ) {
                    Text("Share CSV")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("dice_rolls.csv", exportedCsvText))
                        onToggleCsvDialog(false)
                    }
                ) {
                    Text("Copy CSV", color = ImperialGoldLight)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("roll_history_screen")
    ) {
        PermanentResponsibleNoticeBanner()
        Spacer(modifier = Modifier.height(10.dp))

        // Top Actions Card: Add Roll, Sort, CSV Export, Clear
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
            border = BorderStroke(1.dp, GlassFeltBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ROLL HISTORY DATABASE (${rollsDesc.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryCream
                        )
                        Text(
                            text = if (sortAscending) "Order: Chronological (Oldest → Newest)" else "Order: Reverse Chronological (Newest → Oldest)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondarySage
                        )
                    }

                    Button(
                        onClick = onOpenAddManualRoll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImperialGold,
                            contentColor = AmoledBlack
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("history_add_roll_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Roll",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Roll", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onToggleSortOrder,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GlassFeltBorder),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Toggle Sort",
                            tint = ImperialGoldLight,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (sortAscending) "Oldest First" else "Newest First",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimaryCream
                        )
                    }

                    OutlinedButton(
                        onClick = { onToggleCsvDialog(true) },
                        enabled = rollsDesc.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            tint = ImperialGoldLight,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Export CSV",
                            style = MaterialTheme.typography.labelSmall,
                            color = ImperialGoldLight
                        )
                    }

                    if (rollsDesc.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearAll,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, LossCrimson.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = LossCrimson,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips
        val filters = listOf(
            "ALL" to "All (${rollsDesc.size})",
            "UNDER" to "Under 2–6 (${rollsDesc.count { it.outcomeCategory == BetCategory.UNDER.id }})",
            "SEVEN" to "Seven 7 (${rollsDesc.count { it.outcomeCategory == BetCategory.SEVEN.id }})",
            "OVER" to "Over 8–12 (${rollsDesc.count { it.outcomeCategory == BetCategory.OVER.id }})"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { (key, label) ->
                FilterChip(
                    selected = (selectedFilter == key),
                    onClick = { onSelectFilter(key) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ImperialGold,
                        selectedLabelColor = AmoledBlack,
                        containerColor = DeepFeltSurface,
                        labelColor = TextPrimaryCream
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredRolls.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No roll records found. Use Roll Dice on the Dashboard or Add Roll above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondarySage,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredRolls, key = { it.id }) { roll ->
                    val cat = BetCategory.fromSum(roll.totalSum)
                    val accent = categoryAccentColor(cat)
                    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
                        border = BorderStroke(1.dp, GlassFeltBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = accent.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, accent)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .width(52.dp)
                                            .padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Sum ${roll.totalSum}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryCream
                                        )
                                        Text(
                                            text = "${roll.die1} + ${roll.die2}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accent
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = "${cat.title} (${cat.rangeLabel}) • Roll #${roll.rollNumber}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryCream
                                    )
                                    Text(
                                        text = "ID: ${roll.id} • Die 1: ${roll.die1}, Die 2: ${roll.die2}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondarySage
                                    )
                                    Text(
                                        text = fmt.format(Date(roll.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondarySage
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = { onOpenEditRoll(roll) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit roll",
                                        tint = ImperialGoldLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteRoll(roll.id) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete roll",
                                        tint = LossCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsDeepDiveScreen(
    mlReport: MlPredictionReport,
    currentStakeEur: Double,
    modifier: Modifier = Modifier
) {
    val chiSq = mlReport.chiSquareReport
    val rolling = mlReport.rollingWindowReport
    val markov = mlReport.markovReport
    val wf = mlReport.walkForwardReport

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PermanentResponsibleNoticeBanner()
        }

        // 1. Historical Frequency vs Theoretical Frequency
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
                border = BorderStroke(1.dp, GlassFeltBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. HISTORICAL VS THEORETICAL FREQUENCY",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimaryCream
                    )
                    Text(
                        text = "Compares observed empirical frequencies (N = ${mlReport.totalHistoricalSamples}) against exact 2-dice combinatorial probabilities (15/36, 6/36, 15/36) and Bayesian Posterior.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySage
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    BetCategory.entries.forEach { cat ->
                        val est = mlReport.bayesianEstimates[cat] ?: return@forEach
                        val accent = categoryAccentColor(cat)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmoledBlack)
                                .border(1.dp, GlassFeltBorder, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${cat.title} (${cat.rangeLabel}) • Hits: ${est.historicalCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "Hist: %.1f%% | Theory: %.1f%% | Post: %.1f%%",
                                        est.historicalFrequency * 100.0,
                                        est.theoreticalProb * 100.0,
                                        est.posteriorMean * 100.0
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimaryCream
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { est.historicalFrequency.toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = accent,
                                trackColor = DeepFeltSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. Chi-Square Goodness-of-Fit & Bias Detection
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
                border = BorderStroke(1.dp, GlassFeltBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. CHI-SQUARE GOODNESS-OF-FIT & BIAS DETECTION",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimaryCream
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = chiSq.diagnosticSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = ImperialGoldLight
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMetricBox(
                            title = "Category χ² (df=2)",
                            value = String.format(Locale.US, "%.2f", chiSq.categoryChiSquare),
                            sub = String.format(Locale.US, "p = %.3f", chiSq.categoryPValue),
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricBox(
                            title = "11-Sum χ² (df=10)",
                            value = String.format(Locale.US, "%.2f", chiSq.sumChiSquare),
                            sub = String.format(Locale.US, "p = %.3f", chiSq.sumPValue),
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricBox(
                            title = "Cramér's V Effect",
                            value = String.format(Locale.US, "%.3f", chiSq.cramersV),
                            sub = "α = ${chiSq.significanceAlpha}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Rolling-Window Trends & Markov Transition Matrix
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
                border = BorderStroke(1.dp, GlassFeltBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. ROLLING-WINDOW (W=${rolling.windowSize}) & MARKOV TRANSITIONS",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimaryCream
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMetricBox(
                            title = "Rolling Mean Sum",
                            value = String.format(Locale.US, "%.2f", rolling.rollingMeanSum),
                            sub = "Theory: 7.00",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricBox(
                            title = "Rolling Std Dev",
                            value = String.format(Locale.US, "%.2f", rolling.rollingStdDevSum),
                            sub = "Theory: 2.42",
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricBox(
                            title = "Mean Z-Score",
                            value = String.format(Locale.US, "%+.2f", rolling.meanZScore),
                            sub = "Window N=${rolling.effectiveCount}",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "Markov Independence Test (df=4): χ² = %.2f, p = %.3f (%s)",
                            markov.independenceChiSquare,
                            markov.independencePValue,
                            if (markov.hasSignificantTransitionSignal) "Significant" else "Independent / Random"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondarySage
                    )
                }
            }
        }

        // 4. Chronological Walk-Forward Validation & Calibration
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
                border = BorderStroke(1.dp, GlassFeltBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. WALK-FORWARD BACKTESTING & CALIBRATION",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimaryCream
                    )
                    Text(
                        text = "Strictly chronological out-of-sample evaluation (never trained on future rolls). Distinguishes historical fit from genuine predictive ability.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySage
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = wf.comparisonSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldLight
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMetricBox(
                            title = "Model Accuracy",
                            value = String.format(Locale.US, "%.1f%%", wf.modelHitRate * 100.0),
                            sub = String.format(Locale.US, "Base: %.1f%%", wf.theoreticalBaselineHitRate * 100.0),
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricBox(
                            title = "Brier Score",
                            value = String.format(Locale.US, "%.3f", wf.modelBrierScore),
                            sub = String.format(Locale.US, "Base: %.3f", wf.theoreticalBrierScore),
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricBox(
                            title = "Calibration ECE",
                            value = String.format(Locale.US, "%.3f", wf.expectedCalibrationError),
                            sub = "${wf.evaluatedSteps} WF Steps",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. Expected Value (EV) & Monte Carlo Simulations
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
                border = BorderStroke(1.dp, GlassFeltBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "5. EXPECTED VALUE (EV) & MONTE CARLO (${mlReport.config.monteCarloTrials} TRIALS)",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimaryCream
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    BetCategory.entries.forEach { cat ->
                        val mc = mlReport.monteCarloSimulations[cat] ?: return@forEach
                        val accent = categoryAccentColor(cat)
                        val stakeEv = mc.posteriorEvPerEur * currentStakeEur

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmoledBlack)
                                .border(1.dp, GlassFeltBorder, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${cat.title} (${mlReport.config.multiplierFor(cat)}×)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "Post EV/€1: %+.3f € | Base EV/€1: %+.3f €",
                                        mc.posteriorEvPerEur,
                                        mc.theoreticalEvPerEur
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimaryCream
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "For %.0f € Stake: EV = %+.2f € • 20-Roll Monte Carlo P(Profit) = %.1f%% [5th–95th: %+.1f€ to %+.1f€ / €1]",
                                    currentStakeEur,
                                    stakeEv,
                                    mc.probabilityOfProfitAfter20Rolls * 100.0,
                                    mc.percentile5WorstPerEur,
                                    mc.percentile95BestPerEur
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondarySage
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricBox(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AmoledBlack)
            .border(1.dp, GlassFeltBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondarySage)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryCream
        )
        Text(text = sub, style = MaterialTheme.typography.labelSmall, color = ImperialGoldLight)
    }
}

@Composable
fun StatisticalSettingsScreen(
    config: StatisticalConfig,
    currentStakeEur: Double,
    onUpdateStake: (Double) -> Unit,
    onUpdateConfig: ((StatisticalConfig) -> StatisticalConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PermanentResponsibleNoticeBanner()
        }

        // 1. Payout Multipliers & Return Mode Configuration
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
                border = BorderStroke(1.dp, GlassFeltBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GAME MECHANICS & PAYOUT CONFIGURATION",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimaryCream
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Multipliers Include Original Stake",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryCream
                            )
                            Text(
                                text = if (config.multipliersIncludeStake) {
                                    "Active: Total return = Stake × Multiplier (Net EV = P·M − 1)"
                                } else {
                                    "Active: Net profit = Stake × Multiplier (Net EV = P·(M+1) − 1)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondarySage
                            )
                        }
                        Switch(
                            checked = config.multipliersIncludeStake,
                            onCheckedChange = { enabled ->
                                onUpdateConfig { it.copy(multipliersIncludeStake = enabled) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AmoledBlack,
                                checkedTrackColor = ImperialGold
                            ),
                            modifier = Modifier.testTag("toggle_multiplier_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = String.format(Locale.US, "Over / Under Multiplier: %.1f× (Default 2.3×)", config.underOverMultiplier),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimaryCream
                    )
                    Slider(
                        value = config.underOverMultiplier.toFloat(),
                        onValueChange = { v ->
                            val rounded = (v * 10f).roundToInt() / 10.0
                            onUpdateConfig { it.copy(underOverMultiplier = rounded) }
                        },
                        valueRange = 1.5f..3.5f,
                        steps = 19,
                        colors = SliderDefaults.colors(thumbColor = ImperialGoldLight, activeTrackColor = ImperialGold)
                    )

                    Text(
                        text = String.format(Locale.US, "Seven Multiplier: %.1f× (Default 5.8×)", config.sevenMultiplier),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimaryCream
                    )
                    Slider(
                        value = config.sevenMultiplier.toFloat(),
                        onValueChange = { v ->
                            val rounded = (v * 10f).roundToInt() / 10.0
                            onUpdateConfig { it.copy(sevenMultiplier = rounded) }
                        },
                        valueRange = 4.0f..7.0f,
                        steps = 29,
                        colors = SliderDefaults.colors(thumbColor = ImperialGoldLight, activeTrackColor = ImperialGold)
                    )

                    Text(
                        text = String.format(Locale.US, "Active Stake Amount: %.0f EUR (Min €1 – Max €30)", currentStakeEur),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimaryCream
                    )
                    Slider(
                        value = currentStakeEur.toFloat().coerceIn(1f, 30f),
                        onValueChange = { onUpdateStake(it.roundToInt().toDouble()) },
                        valueRange = 1f..30f,
                        steps = 28,
                        colors = SliderDefaults.colors(thumbColor = ImperialGoldLight, activeTrackColor = ImperialGold)
                    )
                }
            }
        }

        // 2. Statistical Engine Hyperparameters
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
                border = BorderStroke(1.dp, GlassFeltBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "STATISTICAL ENGINE PARAMETERS",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimaryCream
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Bayesian Dirichlet Prior Concentration (α₀ = ${config.dirichletPriorStrength.toInt()})",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimaryCream
                    )
                    Text(
                        text = "Higher α₀ anchors probabilities more strongly toward the 15/36, 6/36, 15/36 theoretical baseline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySage
                    )
                    Slider(
                        value = config.dirichletPriorStrength.toFloat(),
                        onValueChange = { v ->
                            onUpdateConfig { it.copy(dirichletPriorStrength = v.roundToInt().toDouble()) }
                        },
                        valueRange = 12f..108f,
                        steps = 7,
                        colors = SliderDefaults.colors(thumbColor = EmeraldLight, activeTrackColor = EmeraldLight)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Rolling Window Size (W = ${config.rollingWindowSize} rolls)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimaryCream
                    )
                    Slider(
                        value = config.rollingWindowSize.toFloat(),
                        onValueChange = { v ->
                            onUpdateConfig { it.copy(rollingWindowSize = v.roundToInt().coerceIn(5, 100)) }
                        },
                        valueRange = 10f..60f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = EmeraldLight, activeTrackColor = EmeraldLight)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Minimum Sample Size for Bias Inference (N_min = ${config.minSampleSizeForInference} rolls)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimaryCream
                    )
                    Slider(
                        value = config.minSampleSizeForInference.toFloat(),
                        onValueChange = { v ->
                            onUpdateConfig { it.copy(minSampleSizeForInference = v.roundToInt().coerceIn(10, 100)) }
                        },
                        valueRange = 10f..60f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = EmeraldLight, activeTrackColor = EmeraldLight)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onUpdateConfig { StatisticalConfig() } },
                        border = BorderStroke(1.dp, ImperialGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Restore Default Statistical Parameters", color = ImperialGoldLight)
                    }
                }
            }
        }
    }
}
