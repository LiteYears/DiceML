package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BetCategory
import com.example.data.DiceRollRecord
import com.example.data.MlWeightCheckpointEntity
import com.example.domain.MlPredictionReport
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DeepFeltSurface
import com.example.ui.theme.ElevatedFeltCard
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GlassFeltBorder
import com.example.ui.theme.ImperialGold
import com.example.ui.theme.ImperialGoldLight
import com.example.ui.theme.LossCrimson
import com.example.ui.theme.TextPrimaryCream
import com.example.ui.theme.TextSecondarySage
import kotlin.math.roundToInt

/**
 * Compact, Unobtrusive Responsible Notice Pill.
 */
@Composable
fun PermanentResponsibleNoticeBanner(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .testTag("responsible_reporting_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = TextSecondarySage,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "Statistical analysis tool • Past frequencies do not guarantee future rolls",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondarySage,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Unified, High-Contrast Winning Prediction & Auto-Learn Card.
 * Combines the Winning Bet Callout + Auto-Learn / Save / Load actions in ONE compact card!
 */
@Composable
fun WinningBetAndAutoLearnCard(
    mlReport: MlPredictionReport,
    isAutoLearningBatch: Boolean,
    autoLearnProgressText: String?,
    weightsStatusToast: String?,
    savedCheckpointsCount: Int,
    onSelectRecommendedBet: () -> Unit,
    onRunAutoLearnBatch: () -> Unit,
    onQuickSaveWeights: () -> Unit,
    onOpenWeightsManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bestBet = mlReport.smartPickCategory
    val accent = categoryAccentColor(bestBet)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ml_prediction_banner_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
        border = BorderStroke(1.8.dp, accent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.16f),
                            DeepFeltSurface,
                            AmoledBlack
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Top Winning Bet Callout Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelectRecommendedBet),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ImperialGoldLight,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "NEXT WINNING PICK (${mlReport.smartPickConfidencePercent}%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ImperialGoldLight
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mlReport.winningBetHeadline,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.testTag("ml_top_category_text")
                    )
                    Text(
                        text = mlReport.winningBetExplanation,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimaryCream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (autoLearnProgressText != null) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = ImperialGold,
                    trackColor = AmoledBlack
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = autoLearnProgressText,
                    style = MaterialTheme.typography.labelSmall,
                    color = ImperialGoldLight
                )
            } else if (weightsStatusToast != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = weightsStatusToast,
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact Action Strip: Auto-Learn +20 | Save Weights | Load Weights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1.25f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !isAutoLearningBatch, onClick = onRunAutoLearnBatch)
                        .testTag("run_auto_learn_button"),
                    color = EmeraldAccent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = AmoledBlack,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAutoLearningBatch) "Training..." else "Auto-Learn +20",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AmoledBlack,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(0.95f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !isAutoLearningBatch, onClick = onQuickSaveWeights)
                        .testTag("save_weights_button"),
                    color = ElevatedFeltCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Weights",
                            tint = ImperialGoldLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save ML",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ImperialGoldLight,
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !isAutoLearningBatch, onClick = onOpenWeightsManager)
                        .testTag("load_weights_button"),
                    color = ElevatedFeltCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GlassFeltBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Load Weights",
                            tint = TextPrimaryCream,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Load ($savedCheckpointsCount)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimaryCream,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Unified Betting Pad:
 * - 3 Probability & Bet Selection Cards (Under 2–6, Seven 7, Over 8–12)
 * - Compact Stake Bar (€1–€30)
 * - Big ROLL DICE + Manual Entry buttons
 */
@Composable
fun CompactBettingAndRollPadCard(
    mlReport: MlPredictionReport,
    selectedBet: BetCategory,
    currentStakeEur: Double,
    isRolling: Boolean,
    onSelectBet: (BetCategory) -> Unit,
    onUpdateStake: (Double) -> Unit,
    onStepStake: (Double) -> Unit,
    onThrowDice: () -> Unit,
    onOpenManualRollModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bestWinCategory = mlReport.smartPickCategory

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("betting_console_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
        border = BorderStroke(1.dp, GlassFeltBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // 3 Bet Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BetCategory.entries.forEach { category ->
                    val eval = mlReport.evaluations[category] ?: return@forEach
                    val accent = categoryAccentColor(category)
                    val isSelected = (selectedBet == category)
                    val isBestPick = (bestWinCategory == category)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) accent.copy(alpha = 0.20f) else AmoledBlack
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) accent else GlassFeltBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectBet(category) }
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                            .testTag("prob_card_${category.id.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isBestPick) {
                                Text(
                                    text = "★ WIN PICK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ImperialGoldLight,
                                    maxLines = 1
                                )
                            } else {
                                Text(
                                    text = "${eval.multiplier}× PAYOUT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondarySage,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = category.shortName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) accent else TextPrimaryCream,
                                maxLines = 1
                            )

                            Text(
                                text = "Sum ${category.rangeLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondarySage,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${(eval.mlProbability * 100.0).roundToInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact Stake Row: [-] €5 [+] + Quick Presets (1€, 5€, 10€, 20€, 30€)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = { onStepStake(-1.0) },
                    enabled = currentStakeEur > 1.0 && !isRolling,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedFeltCard)
                        .testTag("stake_minus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease stake",
                        tint = TextPrimaryCream,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Surface(
                    color = AmoledBlack,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "€${currentStakeEur.roundToInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ImperialGoldLight,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("active_stake_value_text")
                    )
                }

                IconButton(
                    onClick = { onStepStake(1.0) },
                    enabled = currentStakeEur < 30.0 && !isRolling,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedFeltCard)
                        .testTag("stake_plus_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase stake",
                        tint = TextPrimaryCream,
                        modifier = Modifier.size(16.dp)
                    )
                }

                val presets = listOf(1.0, 5.0, 10.0, 20.0, 30.0)
                presets.forEach { preset ->
                    val isSel = (currentStakeEur.roundToInt() == preset.roundToInt())
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isRolling) { onUpdateStake(preset) },
                        color = if (isSel) ImperialGold else ElevatedFeltCard,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${preset.toInt()}€",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) AmoledBlack else TextPrimaryCream,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Action Row: ROLL DICE + Manual Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onThrowDice,
                    enabled = !isRolling,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImperialGold,
                        contentColor = AmoledBlack
                    ),
                    contentPadding = PaddingValues(vertical = 13.dp, horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("throw_dice_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRolling) {
                            "ROLLING..."
                        } else {
                            "ROLL DICE • BET ${selectedBet.shortName}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onOpenManualRollModal,
                    enabled = !isRolling,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GlassFeltBorder),
                    contentPadding = PaddingValues(vertical = 13.dp, horizontal = 10.dp),
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("manual_log_roll_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Enter Roll",
                        tint = ImperialGoldLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Enter Roll",
                        style = MaterialTheme.typography.labelMedium,
                        color = ImperialGoldLight,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Dialog to Save & Load Learned ML Weights Checkpoints anytime.
 */
@Composable
fun SavedWeightsManagerDialog(
    currentEpochs: Int,
    currentSamples: Int,
    currentBestPick: BetCategory,
    savedCheckpoints: List<MlWeightCheckpointEntity>,
    onDismiss: () -> Unit,
    onSaveNewCheckpoint: (String) -> Unit,
    onLoadCheckpoint: (MlWeightCheckpointEntity) -> Unit,
    onDeleteCheckpoint: (Long) -> Unit
) {
    var checkpointName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepFeltSurface,
        titleContentColor = TextPrimaryCream,
        textContentColor = TextSecondarySage,
        title = {
            Text(
                text = "Save & Load Learned ML Weights",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Active Model: $currentEpochs Epochs • $currentSamples Rolls • Winning Pick: ${currentBestPick.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldLight
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = checkpointName,
                    onValueChange = { checkpointName = it },
                    label = { Text("Checkpoint Name (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onSaveNewCheckpoint(checkpointName)
                        checkpointName = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImperialGold,
                        contentColor = AmoledBlack
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_save_weights_confirm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Current Learned Weights")
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SAVED CHECKPOINTS (${savedCheckpoints.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimaryCream
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (savedCheckpoints.isEmpty()) {
                    Text(
                        text = "No checkpoints saved yet. Tap 'Save Current Learned Weights' above to store a snapshot you can reload anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondarySage
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedCheckpoints, key = { it.id }) { cp ->
                            val bestCat = BetCategory.fromId(cp.bestCategoryId)
                            Surface(
                                color = AmoledBlack,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GlassFeltBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cp.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryCream,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${cp.trainedEpochs} Ep • ${cp.trainedSampleCount} Rolls • Pick: ${bestCat.shortName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ImperialGoldLight
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onLoadCheckpoint(cp) },
                                            color = EmeraldAccent,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Load",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AmoledBlack,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteCheckpoint(cp.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete checkpoint",
                                                tint = LossCrimson,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ImperialGoldLight)
            }
        }
    )
}

@Composable
fun ManualOrEditRollDialog(
    editingRecord: DiceRollRecord?,
    onDismiss: () -> Unit,
    onSaveRoll: (die1: Int, die2: Int) -> Unit
) {
    var d1 by remember(editingRecord) { mutableIntStateOf(editingRecord?.die1 ?: 3) }
    var d2 by remember(editingRecord) { mutableIntStateOf(editingRecord?.die2 ?: 2) }
    val sum = d1 + d2
    val cat = BetCategory.fromSum(sum)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepFeltSurface,
        titleContentColor = TextPrimaryCream,
        textContentColor = TextSecondarySage,
        title = {
            Text(
                text = if (editingRecord != null) {
                    "Edit Roll #${editingRecord.rollNumber}"
                } else {
                    "Enter Dice Roll"
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Tap Die 1 and Die 2 faces (1–6). The ML model immediately learns from this roll and updates the next winning pick.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondarySage
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Die 1: $d1",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimaryCream
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..6).forEach { face ->
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { d1 = face },
                            color = if (d1 == face) ImperialGold else ElevatedFeltCard
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$face",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (d1 == face) AmoledBlack else TextPrimaryCream
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Die 2: $d2",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimaryCream
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..6).forEach { face ->
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { d2 = face },
                            color = if (d2 == face) ImperialGold else ElevatedFeltCard
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$face",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (d2 == face) AmoledBlack else TextPrimaryCream
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Sum: $sum • ${cat.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = categoryAccentColor(cat)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveRoll(d1, d2) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImperialGold,
                    contentColor = AmoledBlack
                ),
                modifier = Modifier.testTag("confirm_manual_roll_button")
            ) {
                Text(if (editingRecord != null) "Update" else "Save Roll ($d1, $d2)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondarySage)
            }
        }
    )
}
