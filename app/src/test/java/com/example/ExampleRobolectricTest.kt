package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BetCategory
import com.example.data.DiceRollRecord
import com.example.domain.DiceMlEngine
import com.example.domain.LearnedMlWeights
import com.example.domain.stats.BayesianEstimatorModule
import com.example.domain.stats.EvidenceStatus
import com.example.domain.stats.FrequencyAndChiSquareModule
import com.example.domain.stats.MonteCarloAndWalkForwardModule
import com.example.domain.stats.RollingWindowAndMarkovModule
import com.example.domain.stats.StatisticalConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DiceML Predictor", appName)
    }

    @Test
    fun `verify theoretical baseline probabilities and configurable multiplier return modes`() {
        assertEquals(15.0 / 36.0, BetCategory.UNDER.theoreticalProb, 1e-6)
        assertEquals(6.0 / 36.0, BetCategory.SEVEN.theoreticalProb, 1e-6)
        assertEquals(15.0 / 36.0, BetCategory.OVER.theoreticalProb, 1e-6)

        val totalReturnConfig = StatisticalConfig(
            underOverMultiplier = 2.3,
            sevenMultiplier = 5.8,
            multipliersIncludeStake = true
        )
        assertEquals((15.0 / 36.0) * 2.3 - 1.0, totalReturnConfig.expectedValuePerEur(15.0 / 36.0, BetCategory.UNDER), 1e-6)
        assertEquals((6.0 / 36.0) * 5.8 - 1.0, totalReturnConfig.expectedValuePerEur(6.0 / 36.0, BetCategory.SEVEN), 1e-6)

        val netProfitConfig = StatisticalConfig(
            underOverMultiplier = 2.3,
            sevenMultiplier = 5.8,
            multipliersIncludeStake = false
        )
        assertEquals((15.0 / 36.0) * 3.3 - 1.0, netProfitConfig.expectedValuePerEur(15.0 / 36.0, BetCategory.UNDER), 1e-6)
    }

    @Test
    fun `verify Auto-Learn weight training, serialization, loading, and winning bet recommendation`() {
        val overHeavyRolls = (1..20).map { idx ->
            DiceRollRecord(
                id = idx.toLong(),
                rollNumber = idx,
                startDie1 = 3,
                startDie2 = 2,
                die1 = 5,
                die2 = 4,
                totalSum = 9
            )
        }

        val trainedWeights = DiceMlEngine.trainWeights(
            rollsChronological = overHeavyRolls,
            baseWeights = LearnedMlWeights.createInitial(),
            extraEpochs = 15
        )
        assertTrue(trainedWeights.trainedEpochs >= 15)
        assertEquals(20, trainedWeights.trainedSamples)

        // Save (serialize) and Load (deserialize) weights
        val payload = trainedWeights.serialize()
        val loadedWeights = LearnedMlWeights.deserialize(payload)
        assertNotNull(loadedWeights)
        assertEquals(trainedWeights.trainedEpochs, loadedWeights!!.trainedEpochs)
        assertEquals(trainedWeights.lastLoss, loadedWeights.lastLoss, 1e-6)

        val report = DiceMlEngine.analyzeAndPredict(
            rollsChronological = overHeavyRolls,
            currentStartFaces = 3 to 2,
            initialStakeEur = 5.0,
            activeWeights = loadedWeights
        )
        assertEquals(BetCategory.OVER, report.smartPickCategory)
        assertTrue(report.winningBetHeadline.contains("OVER"))
    }

    @Test
    fun `verify Bayesian estimator and Chi-Square distinguish insufficient data from significant bias`() {
        val smallSample = listOf(
            DiceRollRecord(id = 1, rollNumber = 1, die1 = 3, die2 = 2, totalSum = 5),
            DiceRollRecord(id = 2, rollNumber = 2, die1 = 4, die2 = 3, totalSum = 7)
        )
        val smallChi = FrequencyAndChiSquareModule.evaluateGoodnessOfFit(
            rolls = smallSample,
            minSampleSize = 30,
            significanceAlpha = 0.05
        )
        assertEquals(EvidenceStatus.INSUFFICIENT_EVIDENCE, smallChi.evidenceStatus)

        val biasedRolls = (1..45).map { idx ->
            DiceRollRecord(id = idx.toLong(), rollNumber = idx, die1 = 4, die2 = 3, totalSum = 7)
        }
        val biasedChi = FrequencyAndChiSquareModule.evaluateGoodnessOfFit(
            rolls = biasedRolls,
            minSampleSize = 30,
            significanceAlpha = 0.05
        )
        assertEquals(EvidenceStatus.STATISTICALLY_SIGNIFICANT_BIAS, biasedChi.evidenceStatus)
        assertTrue(biasedChi.categoryPValue < 0.001)

        val bayes = BayesianEstimatorModule.estimateCategories(biasedRolls, priorStrengthAlpha = 36.0)
        assertTrue((bayes[BetCategory.SEVEN]?.posteriorMean ?: 0.0) > 0.50)
    }

    @Test
    fun `verify chronological walk-forward validation and Markov modules`() {
        val rolls = (1..25).map { i ->
            val d1 = (i % 6) + 1
            val d2 = ((i * 2) % 6) + 1
            DiceRollRecord(id = i.toLong(), rollNumber = i, die1 = d1, die2 = d2, totalSum = d1 + d2)
        }
        val wf = MonteCarloAndWalkForwardModule.runWalkForwardValidation(rolls, priorStrengthAlpha = 36.0)
        assertEquals(23, wf.evaluatedSteps)
        assertEquals(3, wf.calibrationBins.size)

        val markov = RollingWindowAndMarkovModule.analyzeMarkovTransitions(rolls, significanceAlpha = 0.05)
        assertEquals(1.0, markov.currentConditionedProbs.values.sum(), 1e-5)

        val fullReport = DiceMlEngine.analyzeAndPredict(rolls, 3 to 2, 5.0)
        assertEquals(3 to 2, fullReport.currentStartFaces)
        assertFalse(fullReport.signalAdvisoryBanner.isBlank())
    }
}
