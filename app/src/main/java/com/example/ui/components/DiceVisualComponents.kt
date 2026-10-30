package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.BetCategory
import com.example.data.DiceRollRecord
import com.example.ui.LastRollBannerState
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.DeepFeltSurface
import com.example.ui.theme.ElevatedFeltCard
import com.example.ui.theme.EmeraldAccent
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
import java.util.Locale

@Composable
fun Custom3DDicePairStage(
    die1: Int,
    die2: Int,
    isAtCanonicalStart3And2: Boolean,
    isRolling: Boolean,
    lastRollBanner: LastRollBannerState?,
    onResetTo3And2: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSum = die1 + die2
    val currentCategory = BetCategory.fromSum(currentSum)
    val scaleAnim by animateFloatAsState(
        targetValue = if (isRolling) 1.07f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dice_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dice_stage_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
        border = BorderStroke(1.dp, GlassFeltBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Compact Top Row: Start 3 & 2 status + Reset pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isAtCanonicalStart3And2) ImperialGoldLight else EmeraldLight)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAtCanonicalStart3And2) {
                            "Start Faces: 3 & 2"
                        } else {
                            "Landed: $die1 & $die2 (From 3 & 2)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondarySage
                    )
                }

                if (!isAtCanonicalStart3And2) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ElevatedFeltCard)
                            .clickable(onClick = onResetTo3And2)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("reset_dice_3_2_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset to 3 and 2",
                            tint = ImperialGoldLight,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Set 3 & 2",
                            style = MaterialTheme.typography.labelSmall,
                            color = ImperialGoldLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact 68dp 3D Dice + Center Total Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scaleAnim),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DieFaceCanvas(
                    faceValue = die1,
                    size = 68.dp,
                    isStartCanonical = (die1 == 3 && isAtCanonicalStart3And2),
                    modifier = Modifier.testTag("die_1_canvas")
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(
                            color = AmoledBlack,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.5.dp,
                            color = categoryAccentColor(currentCategory),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = currentSum.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimaryCream,
                        modifier = Modifier.testTag("current_dice_sum_text")
                    )
                    Text(
                        text = currentCategory.shortName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = categoryAccentColor(currentCategory)
                    )
                }

                DieFaceCanvas(
                    faceValue = die2,
                    size = 68.dp,
                    isStartCanonical = (die2 == 2 && isAtCanonicalStart3And2),
                    modifier = Modifier.testTag("die_2_canvas")
                )
            }

            // Compact Outcome Toast
            AnimatedVisibility(
                visible = lastRollBanner != null && !isRolling,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                lastRollBanner?.let { banner ->
                    val isWin = banner.wonBet
                    val statusColor = if (isWin) WinGreen else LossCrimson
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("last_roll_outcome_banner"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isWin) {
                                "WIN! Rolled ${banner.die1}+${banner.die2}=${banner.sum} (${banner.selectedBet.shortName})"
                            } else {
                                "Rolled ${banner.die1}+${banner.die2}=${banner.sum} (${banner.outcomeCategory.shortName})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isWin) WinGreen else TextPrimaryCream
                        )
                        Text(
                            text = if (isWin) {
                                String.format(Locale.US, "+€%.2f", banner.payoutEur)
                            } else {
                                String.format(Locale.US, "-€%.0f", banner.stakeEur)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DieFaceCanvas(
    faceValue: Int,
    size: Dp,
    isStartCanonical: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val corner = CornerRadius(w * 0.22f, h * 0.22f)

        drawRoundRect(
            color = if (isStartCanonical) ImperialGold.copy(alpha = 0.25f) else EmeraldAccent.copy(alpha = 0.2f),
            topLeft = Offset(0f, h * 0.04f),
            size = Size(w, h),
            cornerRadius = corner
        )

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1A4336),
                    Color(0xFF0E2920),
                    Color(0xFF081913)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = corner
        )

        val inset = w * 0.08f
        drawRoundRect(
            color = Color(0xFF0B211A),
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2, h - inset * 2),
            cornerRadius = CornerRadius(w * 0.16f, h * 0.16f)
        )

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = if (isStartCanonical) {
                    listOf(ImperialGoldLight, ImperialGold, Color(0xFFB45309))
                } else {
                    listOf(ImperialGoldLight, EmeraldLight, GlassFeltBorder)
                }
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = corner,
            style = Stroke(width = w * 0.045f)
        )

        val pipRadius = w * 0.085f
        val leftX = w * 0.28f
        val centerX = w * 0.50f
        val rightX = w * 0.72f
        val topY = h * 0.28f
        val centerY = h * 0.50f
        val bottomY = h * 0.72f

        val positions = when (faceValue.coerceIn(1, 6)) {
            1 -> listOf(Offset(centerX, centerY))
            2 -> listOf(Offset(leftX, topY), Offset(rightX, bottomY))
            3 -> listOf(Offset(leftX, topY), Offset(centerX, centerY), Offset(rightX, bottomY))
            4 -> listOf(
                Offset(leftX, topY), Offset(rightX, topY),
                Offset(leftX, bottomY), Offset(rightX, bottomY)
            )
            5 -> listOf(
                Offset(leftX, topY), Offset(rightX, topY),
                Offset(centerX, centerY),
                Offset(leftX, bottomY), Offset(rightX, bottomY)
            )
            else -> listOf(
                Offset(leftX, topY), Offset(rightX, topY),
                Offset(leftX, centerY), Offset(rightX, centerY),
                Offset(leftX, bottomY), Offset(rightX, bottomY)
            )
        }

        for (pos in positions) {
            drawCircle(
                color = ImperialGold.copy(alpha = 0.35f),
                radius = pipRadius * 1.45f,
                center = pos
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFBEB), ImperialGoldLight, ImperialGold),
                    center = pos,
                    radius = pipRadius * 1.1f
                ),
                radius = pipRadius,
                center = pos
            )
        }
    }
}

/**
 * Compact Recent Rolls Strip for the main Dashboard (no clutter, fits cleanly on one screen).
 */
@Composable
fun StreamlinedRollHistorySection(
    rollsDesc: List<DiceRollRecord>,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_history_ribbon_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepFeltSurface),
        border = BorderStroke(1.dp, GlassFeltBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = ImperialGoldLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val hits = rollsDesc.count { it.wonBet }
                    val hitPct = if (rollsDesc.isNotEmpty()) (hits * 100) / rollsDesc.size else 0
                    Text(
                        text = if (rollsDesc.isEmpty()) {
                            "RECENT ROLLS (0)"
                        } else {
                            "RECENT ROLLS (${rollsDesc.size}) • WON $hits ($hitPct%)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimaryCream
                    )
                }

                if (rollsDesc.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onClearHistory)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("clear_history_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Reset",
                            tint = TextSecondarySage,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondarySage
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (rollsDesc.isEmpty()) {
                Text(
                    text = "Tap ROLL DICE or Auto-Learn above — recent throws appear here.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondarySage
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rollsDesc.take(20).forEach { roll ->
                        val cat = BetCategory.fromId(roll.outcomeCategory)
                        val accent = categoryAccentColor(cat)
                        Surface(
                            color = AmoledBlack,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (roll.wonBet) WinGreen else accent.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${roll.totalSum}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryCream
                                )
                                Text(
                                    text = "${roll.die1}+${roll.die2}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun categoryAccentColor(category: BetCategory): Color = when (category) {
    BetCategory.UNDER -> UnderCyan
    BetCategory.SEVEN -> SevenGold
    BetCategory.OVER -> OverCoral
}
