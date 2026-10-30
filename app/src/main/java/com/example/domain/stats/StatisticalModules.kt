package com.example.domain.stats

import com.example.data.BetCategory
import com.example.data.DiceRollRecord
import java.util.Random
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class StatisticalConfig(
    val underOverMultiplier: Double = 2.3,
    val sevenMultiplier: Double = 5.8,
    val multipliersIncludeStake: Boolean = true,
    val dirichletPriorStrength: Double = 36.0,
    val rollingWindowSize: Int = 20,
    val minSampleSizeForInference: Int = 30,
    val significanceAlpha: Double = 0.05,
    val monteCarloTrials: Int = 1000
) {
    fun multiplierFor(category: BetCategory): Double = when (category) {
        BetCategory.UNDER, BetCategory.OVER -> underOverMultiplier
        BetCategory.SEVEN -> sevenMultiplier
    }

    /**
     * Expected net value per €1 staked for probability [p] and category [category].
     * - When [multipliersIncludeStake] is true (default): total return is M, so EV = p * M - 1.0
     * - When [multipliersIncludeStake] is false: net profit on win is M, so total return is (M + 1) and EV = p * (M + 1.0) - 1.0
     */
    fun expectedValuePerEur(p: Double, category: BetCategory): Double {
        val m = multiplierFor(category)
        val totalReturnMultiple = if (multipliersIncludeStake) m else (m + 1.0)
        return p * totalReturnMultiple - 1.0
    }

    fun grossReturnMultiple(category: BetCategory): Double {
        val m = multiplierFor(category)
        return if (multipliersIncludeStake) m else (m + 1.0)
    }
}

enum class EvidenceStatus(val badgeTitle: String) {
    INSUFFICIENT_EVIDENCE("Insufficient Sample Size"),
    NO_SIGNIFICANT_BIAS("Consistent with Fair Randomness"),
    STATISTICALLY_SIGNIFICANT_BIAS("Statistically Significant Deviation")
}

data class CategoryBayesianEstimate(
    val category: BetCategory,
    val theoreticalProb: Double,
    val historicalFrequency: Double,
    val historicalCount: Int,
    val posteriorMean: Double,
    val posteriorStdDev: Double,
    val credibleInterval95Lower: Double,
    val credibleInterval95Upper: Double
)

data class SumStatisticalPoint(
    val sum: Int,
    val category: BetCategory,
    val theoreticalProb: Double,
    val historicalFrequency: Double,
    val posteriorProb: Double,
    val count: Int
)

data class ChiSquareBiasReport(
    val sampleSize: Int,
    val minRequiredSamples: Int,
    val categoryChiSquare: Double,
    val categoryDegreesOfFreedom: Int = 2,
    val categoryPValue: Double,
    val sumChiSquare: Double,
    val sumDegreesOfFreedom: Int = 10,
    val sumPValue: Double,
    val cramersV: Double,
    val significanceAlpha: Double,
    val evidenceStatus: EvidenceStatus,
    val diagnosticSummary: String
)

data class RollingWindowReport(
    val windowSize: Int,
    val effectiveCount: Int,
    val rollingUnderFreq: Double,
    val rollingSevenFreq: Double,
    val rollingOverFreq: Double,
    val rollingMeanSum: Double,
    val theoreticalMeanSum: Double = 7.0,
    val rollingStdDevSum: Double,
    val theoreticalStdDevSum: Double = sqrt(35.0 / 6.0),
    val meanZScore: Double,
    val rollingMeanSeries: List<Double>
)

data class MarkovTransitionReport(
    val transitionProbabilities: Array<DoubleArray>, // 3x3 [fromOrdinal][toOrdinal]
    val transitionCounts: Array<IntArray>,
    val currentConditionedProbs: Map<BetCategory, Double>,
    val independenceChiSquare: Double,
    val independencePValue: Double,
    val hasSignificantTransitionSignal: Boolean
)

data class CalibrationBin(
    val binLabel: String,
    val meanPredictedProb: Double,
    val empiricalWinFreq: Double,
    val sampleCount: Int
)

data class WalkForwardBacktestReport(
    val evaluatedSteps: Int,
    val modelHitRate: Double,
    val theoreticalBaselineHitRate: Double,
    val majorityBaselineHitRate: Double,
    val modelBrierScore: Double,
    val theoreticalBrierScore: Double,
    val modelLogLoss: Double,
    val theoreticalLogLoss: Double,
    val expectedCalibrationError: Double,
    val calibrationBins: List<CalibrationBin>,
    val beatsTheoreticalBaseline: Boolean,
    val comparisonSummary: String
)

data class MonteCarloCategorySimulation(
    val category: BetCategory,
    val trials: Int,
    val horizonRolls: Int,
    val theoreticalEvPerEur: Double,
    val posteriorEvPerEur: Double,
    val meanNetReturn20RollsPerEur: Double,
    val medianNetReturn20RollsPerEur: Double,
    val percentile5WorstPerEur: Double,
    val percentile95BestPerEur: Double,
    val probabilityOfProfitAfter20Rolls: Double
)

/**
 * 1. Independent Module: Bayesian Probability Estimation with Configurable Priors.
 */
object BayesianEstimatorModule {

    val SUM_COMBINATORIAL_WAYS = mapOf(
        2 to 1, 3 to 2, 4 to 3, 5 to 4, 6 to 5,
        7 to 6,
        8 to 5, 9 to 4, 10 to 3, 11 to 2, 12 to 1
    )

    fun estimateCategories(
        rolls: List<DiceRollRecord>,
        priorStrengthAlpha: Double
    ): Map<BetCategory, CategoryBayesianEstimate> {
        val n = rolls.size
        val alpha0 = priorStrengthAlpha.coerceIn(1.0, 500.0)
        val counts = mutableMapOf(
            BetCategory.UNDER to 0,
            BetCategory.SEVEN to 0,
            BetCategory.OVER to 0
        )
        for (r in rolls) {
            val cat = BetCategory.fromSum(r.totalSum)
            counts[cat] = (counts[cat] ?: 0) + 1
        }

        val totalPosteriorAlpha = alpha0 + n
        return BetCategory.entries.associateWith { cat ->
            val count = counts[cat] ?: 0
            val histFreq = if (n > 0) count.toDouble() / n else cat.theoreticalProb
            val alphaCat = alpha0 * cat.theoreticalProb + count
            val mean = alphaCat / totalPosteriorAlpha
            val variance = (alphaCat * (totalPosteriorAlpha - alphaCat)) /
                (totalPosteriorAlpha * totalPosteriorAlpha * (totalPosteriorAlpha + 1.0))
            val stdDev = sqrt(max(0.0, variance))
            val lower95 = (mean - 1.96 * stdDev).coerceIn(0.0, 1.0)
            val upper95 = (mean + 1.96 * stdDev).coerceIn(0.0, 1.0)

            CategoryBayesianEstimate(
                category = cat,
                theoreticalProb = cat.theoreticalProb,
                historicalFrequency = histFreq,
                historicalCount = count,
                posteriorMean = mean,
                posteriorStdDev = stdDev,
                credibleInterval95Lower = lower95,
                credibleInterval95Upper = upper95
            )
        }
    }

    fun estimateSums(
        rolls: List<DiceRollRecord>,
        priorStrengthAlpha: Double
    ): List<SumStatisticalPoint> {
        val n = rolls.size
        val alpha0 = priorStrengthAlpha.coerceIn(1.0, 500.0)
        val sumCounts = IntArray(13)
        for (r in rolls) {
            if (r.totalSum in 2..12) sumCounts[r.totalSum]++
        }

        val totalPosteriorAlpha = alpha0 + n
        return (2..12).map { s ->
            val ways = SUM_COMBINATORIAL_WAYS[s] ?: 1
            val theoryP = ways / 36.0
            val count = sumCounts[s]
            val histP = if (n > 0) count.toDouble() / n else theoryP
            val postP = (alpha0 * theoryP + count) / totalPosteriorAlpha
            SumStatisticalPoint(
                sum = s,
                category = BetCategory.fromSum(s),
                theoreticalProb = theoryP,
                historicalFrequency = histP,
                posteriorProb = postP,
                count = count
            )
        }
    }
}

/**
 * 2. Independent Module: Historical Frequency Analysis & Chi-Square Goodness-of-Fit Testing.
 */
object FrequencyAndChiSquareModule {

    fun evaluateGoodnessOfFit(
        rolls: List<DiceRollRecord>,
        minSampleSize: Int,
        significanceAlpha: Double
    ): ChiSquareBiasReport {
        val n = rolls.size
        if (n == 0) {
            return ChiSquareBiasReport(
                sampleSize = 0,
                minRequiredSamples = minSampleSize,
                categoryChiSquare = 0.0,
                categoryPValue = 1.0,
                sumChiSquare = 0.0,
                sumPValue = 1.0,
                cramersV = 0.0,
                significanceAlpha = significanceAlpha,
                evidenceStatus = EvidenceStatus.INSUFFICIENT_EVIDENCE,
                diagnosticSummary = "No rolls recorded yet (0/$minSampleSize minimum sample size). Using theoretical baseline probabilities (Under 41.67%, Seven 16.67%, Over 41.67%)."
            )
        }

        val catCounts = IntArray(3)
        val sumCounts = IntArray(13)
        for (r in rolls) {
            val c = BetCategory.fromSum(r.totalSum).ordinal
            catCounts[c]++
            if (r.totalSum in 2..12) sumCounts[r.totalSum]++
        }

        val catProbs = doubleArrayOf(15.0 / 36.0, 6.0 / 36.0, 15.0 / 36.0)
        var catChiSq = 0.0
        for (c in 0..2) {
            val expected = n * catProbs[c]
            val diff = catCounts[c] - expected
            catChiSq += (diff * diff) / expected
        }
        // Exact analytical survival function for Chi-Square with df = 2: P(X >= x) = exp(-x / 2)
        val catPValue = exp(-catChiSq / 2.0).coerceIn(0.0, 1.0)

        var sumChiSq = 0.0
        for (s in 2..12) {
            val ways = BayesianEstimatorModule.SUM_COMBINATORIAL_WAYS[s] ?: 1
            val expected = n * (ways / 36.0)
            val diff = sumCounts[s] - expected
            sumChiSq += (diff * diff) / expected
        }
        val sumPValue = chiSquarePValueApproximation(sumChiSq, df = 10)

        // Cramer's V for k = 3 categories (df = 2): V = sqrt(chi2 / (N * (k - 1)))
        val cramersV = sqrt(catChiSq / (n * 2.0)).coerceIn(0.0, 1.0)

        val status = when {
            n < minSampleSize -> EvidenceStatus.INSUFFICIENT_EVIDENCE
            catPValue < significanceAlpha || sumPValue < significanceAlpha ->
                EvidenceStatus.STATISTICALLY_SIGNIFICANT_BIAS
            else -> EvidenceStatus.NO_SIGNIFICANT_BIAS
        }

        val summary = when (status) {
            EvidenceStatus.INSUFFICIENT_EVIDENCE ->
                "Insufficient evidence ($n/$minSampleSize rolls recorded). Observed deviations from 15/36, 6/36, 15/36 cannot yet be distinguished from random sampling noise."
            EvidenceStatus.NO_SIGNIFICANT_BIAS ->
                "No statistically meaningful bias detected (Category χ² = ${fmt2(catChiSq)}, p = ${fmt3(catPValue)} ≥ α=${significanceAlpha}). Observed deviations are consistent with fair two-dice randomness."
            EvidenceStatus.STATISTICALLY_SIGNIFICANT_BIAS ->
                "Statistically significant deviation from theoretical baseline detected (Category χ² = ${fmt2(catChiSq)}, p = ${fmt3(catPValue)} < α=${significanceAlpha}, Cramér's V = ${fmt2(cramersV)})."
        }

        return ChiSquareBiasReport(
            sampleSize = n,
            minRequiredSamples = minSampleSize,
            categoryChiSquare = catChiSq,
            categoryPValue = catPValue,
            sumChiSquare = sumChiSq,
            sumPValue = sumPValue,
            cramersV = cramersV,
            significanceAlpha = significanceAlpha,
            evidenceStatus = status,
            diagnosticSummary = summary
        )
    }

    /**
     * Wilson-Hilferty normal approximation for Chi-Square upper-tail p-value with [df] degrees of freedom.
     */
    fun chiSquarePValueApproximation(chiSq: Double, df: Int): Double {
        if (chiSq <= 0.0) return 1.0
        if (df == 2) return exp(-chiSq / 2.0).coerceIn(0.0, 1.0)
        val k = df.toDouble()
        val term = 2.0 / (9.0 * k)
        val z = ((chiSq / k).pow(1.0 / 3.0) - (1.0 - term)) / sqrt(term)
        return standardNormalUpperTail(z)
    }

    fun standardNormalUpperTail(z: Double): Double {
        if (z < -8.0) return 1.0
        if (z > 8.0) return 0.0
        val x = abs(z)
        val t = 1.0 / (1.0 + 0.2316419 * x)
        val d = 0.3989422804014327 * exp(-0.5 * x * x)
        val prob = d * t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + 1.330274429 * t))))
        return if (z >= 0.0) prob.coerceIn(0.0, 1.0) else (1.0 - prob).coerceIn(0.0, 1.0)
    }

    private fun fmt2(v: Double) = String.format(java.util.Locale.US, "%.2f", v)
    private fun fmt3(v: Double) = String.format(java.util.Locale.US, "%.3f", v)
}

/**
 * 3. Independent Module: Rolling-Window & First-Order Markov Transition Analysis.
 */
object RollingWindowAndMarkovModule {

    fun analyzeRollingWindow(
        rollsChronological: List<DiceRollRecord>,
        windowSize: Int
    ): RollingWindowReport {
        val w = windowSize.coerceIn(5, 200)
        val slice = rollsChronological.takeLast(w)
        val count = slice.size
        val theoStd = sqrt(35.0 / 6.0)

        if (count == 0) {
            return RollingWindowReport(
                windowSize = w,
                effectiveCount = 0,
                rollingUnderFreq = 15.0 / 36.0,
                rollingSevenFreq = 6.0 / 36.0,
                rollingOverFreq = 15.0 / 36.0,
                rollingMeanSum = 7.0,
                rollingStdDevSum = theoStd,
                meanZScore = 0.0,
                rollingMeanSeries = emptyList()
            )
        }

        val u = slice.count { BetCategory.fromSum(it.totalSum) == BetCategory.UNDER }.toDouble() / count
        val s = slice.count { BetCategory.fromSum(it.totalSum) == BetCategory.SEVEN }.toDouble() / count
        val o = slice.count { BetCategory.fromSum(it.totalSum) == BetCategory.OVER }.toDouble() / count

        val mean = slice.map { it.totalSum }.average()
        val variance = slice.map { (it.totalSum - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        val stdError = theoStd / sqrt(count.toDouble())
        val zScore = (mean - 7.0) / stdError

        // Compute up to 15 recent rolling average points for trend line visualization
        val series = mutableListOf<Double>()
        val startIdx = max(0, rollsChronological.size - 15)
        for (idx in startIdx until rollsChronological.size) {
            val subStart = max(0, idx - w + 1)
            val subSlice = rollsChronological.subList(subStart, idx + 1)
            series.add(subSlice.map { it.totalSum }.average())
        }

        return RollingWindowReport(
            windowSize = w,
            effectiveCount = count,
            rollingUnderFreq = u,
            rollingSevenFreq = s,
            rollingOverFreq = o,
            rollingMeanSum = mean,
            rollingStdDevSum = stdDev,
            meanZScore = zScore,
            rollingMeanSeries = series
        )
    }

    fun analyzeMarkovTransitions(
        rollsChronological: List<DiceRollRecord>,
        significanceAlpha: Double
    ): MarkovTransitionReport {
        val priors = doubleArrayOf(15.0 / 36.0, 6.0 / 36.0, 15.0 / 36.0)
        val counts = Array(3) { IntArray(3) }
        for (i in 1 until rollsChronological.size) {
            val prev = BetCategory.fromSum(rollsChronological[i - 1].totalSum).ordinal
            val curr = BetCategory.fromSum(rollsChronological[i].totalSum).ordinal
            counts[prev][curr]++
        }

        // Prior smoothing weight = 6.0 per row anchored at theoretical proportions
        val smoothing = 6.0
        val probs = Array(3) { r ->
            val rowSum = counts[r].sum() + smoothing
            DoubleArray(3) { c ->
                (counts[r][c] + smoothing * priors[c]) / rowSum
            }
        }

        // Chi-square test of Markov independence (df = (3-1)*(3-1) = 4)
        val totalTransitions = (rollsChronological.size - 1).coerceAtLeast(0)
        var chiSq = 0.0
        if (totalTransitions >= 10) {
            val rowTotals = IntArray(3) { r -> counts[r].sum() }
            val colTotals = IntArray(3) { c -> counts[0][c] + counts[1][c] + counts[2][c] }
            for (r in 0..2) {
                for (c in 0..2) {
                    val expected = (rowTotals[r].toDouble() * colTotals[c].toDouble()) / totalTransitions
                    if (expected > 0.0) {
                        val diff = counts[r][c] - expected
                        chiSq += (diff * diff) / expected
                    }
                }
            }
        }
        val pVal = if (totalTransitions >= 10) {
            FrequencyAndChiSquareModule.chiSquarePValueApproximation(chiSq, df = 4)
        } else {
            1.0
        }

        // Current state is last roll category, or UNDER (Die 1 = 3, Die 2 = 2 -> Sum 5) when empty
        val currentStateOrdinal = if (rollsChronological.isNotEmpty()) {
            BetCategory.fromSum(rollsChronological.last().totalSum).ordinal
        } else {
            BetCategory.UNDER.ordinal
        }

        val currentConditioned = mapOf(
            BetCategory.UNDER to probs[currentStateOrdinal][0],
            BetCategory.SEVEN to probs[currentStateOrdinal][1],
            BetCategory.OVER to probs[currentStateOrdinal][2]
        )

        return MarkovTransitionReport(
            transitionProbabilities = probs,
            transitionCounts = counts,
            currentConditionedProbs = currentConditioned,
            independenceChiSquare = chiSq,
            independencePValue = pVal,
            hasSignificantTransitionSignal = (totalTransitions >= 30 && pVal < significanceAlpha)
        )
    }
}

/**
 * 4. Independent Module: Chronological Walk-Forward Validation, Calibration & Monte Carlo Simulation.
 * Strictly never trains on future data.
 */
object MonteCarloAndWalkForwardModule {

    fun runWalkForwardValidation(
        rollsChronological: List<DiceRollRecord>,
        priorStrengthAlpha: Double
    ): WalkForwardBacktestReport {
        val n = rollsChronological.size
        val theoProbs = doubleArrayOf(15.0 / 36.0, 6.0 / 36.0, 15.0 / 36.0)

        if (n < 3) {
            return WalkForwardBacktestReport(
                evaluatedSteps = 0,
                modelHitRate = 0.0,
                theoreticalBaselineHitRate = 15.0 / 36.0,
                majorityBaselineHitRate = 0.0,
                modelBrierScore = 0.638,
                theoreticalBrierScore = 0.638,
                modelLogLoss = 1.030,
                theoreticalLogLoss = 1.030,
                expectedCalibrationError = 0.0,
                calibrationBins = emptyList(),
                beatsTheoreticalBaseline = false,
                comparisonSummary = "Walk-forward validation requires at least 3 chronological rolls. No future data is ever used during evaluation."
            )
        }

        var modelHits = 0
        var theoHits = 0
        var majorityHits = 0
        var modelBrierSum = 0.0
        var theoBrierSum = 0.0
        var modelLogLossSum = 0.0
        var theoLogLossSum = 0.0

        val runningCounts = IntArray(3)
        // 3 calibration buckets: Low (<25%), Medium (25%–40%), High (>40%)
        val binPredSums = DoubleArray(3)
        val binActualSums = DoubleArray(3)
        val binCounts = IntArray(3)

        val alpha0 = priorStrengthAlpha.coerceIn(1.0, 500.0)
        var steps = 0

        for (t in 0 until n) {
            val actualCat = BetCategory.fromSum(rollsChronological[t].totalSum).ordinal
            if (t >= 2) {
                steps++
                // Strictly use only past counts [0 .. t-1]
                val totalPast = t.toDouble()
                val stepProbs = DoubleArray(3) { c ->
                    (alpha0 * theoProbs[c] + runningCounts[c]) / (alpha0 + totalPast)
                }

                val predClass = stepProbs.indices.maxByOrNull { stepProbs[it] } ?: 0
                val majClass = runningCounts.indices.maxByOrNull { runningCounts[it] } ?: 0
                // Theoretical baseline ties Under (15/36) and Over (15/36)
                val theoHit = (actualCat == 0)

                if (predClass == actualCat) modelHits++
                if (theoHit) theoHits++
                if (majClass == actualCat) majorityHits++

                for (c in 0..2) {
                    val y = if (c == actualCat) 1.0 else 0.0
                    modelBrierSum += (stepProbs[c] - y).pow(2)
                    theoBrierSum += (theoProbs[c] - y).pow(2)

                    val pPred = stepProbs[c]
                    val bIdx = when {
                        pPred < 0.25 -> 0
                        pPred < 0.40 -> 1
                        else -> 2
                    }
                    binPredSums[bIdx] += pPred
                    binActualSums[bIdx] += y
                    binCounts[bIdx]++
                }

                modelLogLossSum += -ln(max(1e-6, stepProbs[actualCat]))
                theoLogLossSum += -ln(max(1e-6, theoProbs[actualCat]))
            }
            // Update counts AFTER predicting step t (strictly causal walk-forward)
            runningCounts[actualCat]++
        }

        val modelHitRate = modelHits.toDouble() / steps
        val theoHitRate = theoHits.toDouble() / steps
        val majHitRate = majorityHits.toDouble() / steps
        val modelBrier = modelBrierSum / steps
        val theoBrier = theoBrierSum / steps
        val modelLL = modelLogLossSum / steps
        val theoLL = theoLogLossSum / steps

        val binLabels = listOf("10%–25% (Seven)", "25%–40% (Mid)", "40%+ (Under/Over)")
        val totalBinSamples = binCounts.sum().coerceAtLeast(1)
        var ece = 0.0
        val bins = (0..2).map { b ->
            val cnt = binCounts[b]
            val meanP = if (cnt > 0) binPredSums[b] / cnt else 0.0
            val empFreq = if (cnt > 0) binActualSums[b] / cnt else 0.0
            ece += (cnt.toDouble() / totalBinSamples) * abs(meanP - empFreq)
            CalibrationBin(
                binLabel = binLabels[b],
                meanPredictedProb = meanP,
                empiricalWinFreq = empFreq,
                sampleCount = cnt
            )
        }

        val beatsBaseline = (modelLL < theoLL - 0.005) && (steps >= 20)
        val summary = if (beatsBaseline) {
            "Walk-forward model achieved lower out-of-sample log-loss (${fmt3(modelLL)}) than the theoretical 15/36, 6/36, 15/36 baseline (${fmt3(theoLL)}) across $steps strictly chronological steps."
        } else {
            "Out-of-sample walk-forward log-loss (${fmt3(modelLL)}) does not outperform the theoretical baseline (${fmt3(theoLL)}). Historical deviations reflect descriptive in-sample noise rather than exploitable predictive signal."
        }

        return WalkForwardBacktestReport(
            evaluatedSteps = steps,
            modelHitRate = modelHitRate,
            theoreticalBaselineHitRate = theoHitRate,
            majorityBaselineHitRate = majHitRate,
            modelBrierScore = modelBrier,
            theoreticalBrierScore = theoBrier,
            modelLogLoss = modelLL,
            theoreticalLogLoss = theoLL,
            expectedCalibrationError = ece,
            calibrationBins = bins,
            beatsTheoreticalBaseline = beatsBaseline,
            comparisonSummary = summary
        )
    }

    fun runMonteCarloSimulations(
        bayesianEstimates: Map<BetCategory, CategoryBayesianEstimate>,
        config: StatisticalConfig,
        horizonRolls: Int = 20
    ): Map<BetCategory, MonteCarloCategorySimulation> {
        val trials = config.monteCarloTrials.coerceIn(100, 5000)
        val rng = Random(422358L) // Deterministic seed for stable UI display

        return BetCategory.entries.associateWith { cat ->
            val pPosterior = bayesianEstimates[cat]?.posteriorMean ?: cat.theoreticalProb
            val pTheory = cat.theoreticalProb
            val grossWinReturn = config.grossReturnMultiple(cat)
            val netWinPerEur = grossWinReturn - 1.0

            val netOutcomes = DoubleArray(trials)
            var profitableCount = 0

            for (t in 0 until trials) {
                var net = 0.0
                for (r in 0 until horizonRolls) {
                    val won = rng.nextDouble() < pPosterior
                    net += if (won) netWinPerEur else -1.0
                }
                netOutcomes[t] = net
                if (net > 0.0) profitableCount++
            }
            netOutcomes.sort()

            val meanNet = netOutcomes.average()
            val medianNet = netOutcomes[trials / 2]
            val p5 = netOutcomes[(trials * 0.05).toInt().coerceIn(0, trials - 1)]
            val p95 = netOutcomes[(trials * 0.95).toInt().coerceIn(0, trials - 1)]

            MonteCarloCategorySimulation(
                category = cat,
                trials = trials,
                horizonRolls = horizonRolls,
                theoreticalEvPerEur = config.expectedValuePerEur(pTheory, cat),
                posteriorEvPerEur = config.expectedValuePerEur(pPosterior, cat),
                meanNetReturn20RollsPerEur = meanNet,
                medianNetReturn20RollsPerEur = medianNet,
                percentile5WorstPerEur = p5,
                percentile95BestPerEur = p95,
                probabilityOfProfitAfter20Rolls = profitableCount.toDouble() / trials
            )
        }
    }

    private fun fmt3(v: Double) = String.format(java.util.Locale.US, "%.3f", v)
}
