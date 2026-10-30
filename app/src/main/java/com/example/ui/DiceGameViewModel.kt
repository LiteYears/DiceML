package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BetCategory
import com.example.data.DiceRepository
import com.example.data.DiceRollRecord
import com.example.data.MlWeightCheckpointEntity
import com.example.domain.DiceMlEngine
import com.example.domain.LearnedMlWeights
import com.example.domain.MlPredictionReport
import com.example.domain.stats.StatisticalConfig
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LastRollBannerState(
    val rollNumber: Int,
    val startDie1: Int,
    val startDie2: Int,
    val die1: Int,
    val die2: Int,
    val sum: Int,
    val outcomeCategory: BetCategory,
    val selectedBet: BetCategory,
    val stakeEur: Double,
    val payoutEur: Double,
    val netProfitEur: Double,
    val wonBet: Boolean,
    val mlPredicted: BetCategory,
    val mlCorrect: Boolean
)

data class SessionControlsState(
    val showInitialStakeDialog: Boolean = false,
    val showManualRollSheet: Boolean = false,
    val showWeightsManagerSheet: Boolean = false,
    val editingRollRecord: DiceRollRecord? = null,
    val showCsvExportDialog: Boolean = false,
    val initialStakeEur: Double = 5.0,
    val currentStakeEur: Double = 5.0,
    val bankrollEur: Double = 150.0,
    val startingBankrollEur: Double = 150.0,
    val selectedBet: BetCategory = BetCategory.OVER,
    val displayedDie1: Int = 3, // Initial visible face: Die 1 = 3
    val displayedDie2: Int = 2, // Initial visible face: Die 2 = 2
    val isAtCanonicalStart3And2: Boolean = true,
    val alwaysStartFrom3And2: Boolean = true,
    val isRollingAnimation: Boolean = false,
    val autoLearnEnabled: Boolean = true,
    val isAutoLearningBatch: Boolean = false,
    val autoLearnProgressText: String? = null,
    val weightsStatusToast: String? = null,
    val activeLearnedWeights: LearnedMlWeights = LearnedMlWeights.createInitial(),
    val lastRollBanner: LastRollBannerState? = null,
    val historyFilter: String = "ALL",
    val historySortAscending: Boolean = false,
    val statisticalConfig: StatisticalConfig = StatisticalConfig()
)

data class DiceGameUiState(
    val controls: SessionControlsState = SessionControlsState(),
    val rollsDesc: List<DiceRollRecord> = emptyList(),
    val rollsChronological: List<DiceRollRecord> = emptyList(),
    val savedCheckpoints: List<MlWeightCheckpointEntity> = emptyList(),
    val mlReport: MlPredictionReport = DiceMlEngine.analyzeAndPredict(emptyList(), 3 to 2, 5.0),
    val totalGamesPlayed: Int = 0,
    val playerWinsCount: Int = 0,
    val playerWinRatePercent: Double = 0.0,
    val netSessionProfitEur: Double = 0.0,
    val exportedCsvText: String = ""
)

class DiceGameViewModel(
    private val repository: DiceRepository
) : ViewModel() {

    private val secureRandom = SecureRandom()
    private val controlsState = MutableStateFlow(SessionControlsState())

    init {
        // Automatically restore last auto-saved learned weights on startup if available
        viewModelScope.launch {
            val savedAuto = repository.activeAutoSavedWeights.firstOrNull()
            if (savedAuto != null) {
                val restored = LearnedMlWeights.deserialize(savedAuto.serializedWeightsPayload)
                if (restored != null) {
                    val recBet = BetCategory.fromId(savedAuto.bestCategoryId)
                    controlsState.update {
                        it.copy(
                            activeLearnedWeights = restored,
                            selectedBet = recBet,
                            weightsStatusToast = "Restored learned weights (${restored.trainedEpochs} epochs)"
                        )
                    }
                }
            }
        }
    }

    val uiState: StateFlow<DiceGameUiState> = combine(
        controlsState,
        repository.allRollsDesc,
        repository.savedWeightCheckpoints
    ) { controls, rollsDesc, checkpoints ->
        val chronological = rollsDesc.sortedWith(
            compareBy<DiceRollRecord> { it.timestamp }.thenBy { it.id }
        )
        val startFaces = if (controls.alwaysStartFrom3And2 || controls.isAtCanonicalStart3And2) {
            3 to 2
        } else {
            controls.displayedDie1 to controls.displayedDie2
        }

        val mlReport = DiceMlEngine.analyzeAndPredict(
            rollsChronological = chronological,
            currentStartFaces = startFaces,
            initialStakeEur = controls.initialStakeEur,
            config = controls.statisticalConfig,
            activeWeights = controls.activeLearnedWeights
        )

        val totalGames = rollsDesc.size
        val wins = rollsDesc.count { it.wonBet }
        val winRate = if (totalGames > 0) (wins.toDouble() / totalGames) * 100.0 else 0.0
        val netProfit = rollsDesc.sumOf { it.netProfitEur }
        val csvText = if (controls.showCsvExportDialog) {
            repository.exportToCsv(rollsDesc)
        } else {
            ""
        }

        DiceGameUiState(
            controls = controls,
            rollsDesc = rollsDesc,
            rollsChronological = chronological,
            savedCheckpoints = checkpoints,
            mlReport = mlReport,
            totalGamesPlayed = totalGames,
            playerWinsCount = wins,
            playerWinRatePercent = winRate,
            netSessionProfitEur = netProfit,
            exportedCsvText = csvText
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiceGameUiState()
    )

    fun selectBet(category: BetCategory) {
        if (controlsState.value.isRollingAnimation) return
        controlsState.update { it.copy(selectedBet = category) }
    }

    fun applyMlRecommendation(useHighestEv: Boolean = false) {
        val report = uiState.value.mlReport
        val targetBet = if (useHighestEv) report.topExpectedValueCategory else report.smartPickCategory
        val recommendedStake = report.recommendedStakeEur.coerceIn(1.0, 30.0)
        controlsState.update {
            it.copy(
                selectedBet = targetBet,
                currentStakeEur = recommendedStake
            )
        }
    }

    fun updateCurrentStake(stake: Double) {
        val clamped = ((stake * 10.0).roundToInt() / 10.0).coerceIn(1.0, 30.0)
        controlsState.update { it.copy(currentStakeEur = clamped) }
    }

    fun stepCurrentStake(delta: Double) {
        updateCurrentStake(controlsState.value.currentStakeEur + delta)
    }

    fun toggleAutoLearnMode(enabled: Boolean) {
        controlsState.update {
            it.copy(
                autoLearnEnabled = enabled,
                weightsStatusToast = if (enabled) "Auto-Learn Mode Enabled" else "Auto-Learn Paused"
            )
        }
    }

    fun toggleWeightsManagerSheet(show: Boolean) {
        controlsState.update { it.copy(showWeightsManagerSheet = show) }
    }

    /**
     * Runs an interactive Auto-Learn training session:
     * Generates [autoRollCount] training throws from faces (3,2) if requested, trains the
     * neural & Markov ensemble for [extraEpochs] passes, auto-saves the learned weights to Room,
     * and automatically highlights & selects the bet most likely to win!
     */
    fun runAutoLearnSession(autoRollCount: Int = 20, extraEpochs: Int = 25) {
        if (controlsState.value.isAutoLearningBatch || controlsState.value.isRollingAnimation) return

        viewModelScope.launch {
            controlsState.update {
                it.copy(
                    isAutoLearningBatch = true,
                    autoLearnProgressText = "Auto-Learning from faces 3 & 2..."
                )
            }

            val cfg = controlsState.value.statisticalConfig
            val stake = controlsState.value.currentStakeEur.coerceIn(1.0, 30.0)
            var currentWeights = controlsState.value.activeLearnedWeights
            var baseRollNum = (uiState.value.rollsDesc.maxOfOrNull { it.rollNumber } ?: 0)

            if (autoRollCount > 0) {
                val generatedBatch = mutableListOf<DiceRollRecord>()
                val existingChrono = uiState.value.rollsChronological.toMutableList()

                for (step in 1..autoRollCount) {
                    val tempReport = DiceMlEngine.analyzeAndPredict(
                        rollsChronological = existingChrono,
                        currentStartFaces = 3 to 2,
                        initialStakeEur = stake,
                        config = cfg,
                        activeWeights = currentWeights
                    )
                    val aiPick = tempReport.smartPickCategory
                    val (d1, d2) = DiceMlEngine.rollNextFromCanonical3And2(
                        historyChronological = existingChrono,
                        randomSeed = secureRandom.nextInt(10000)
                    )
                    val sum = d1 + d2
                    val outcome = BetCategory.fromSum(sum)
                    val won = (outcome == aiPick)
                    val payout = if (won) ((stake * cfg.grossReturnMultiple(aiPick) * 100.0).roundToInt() / 100.0) else 0.0
                    val net = ((payout - stake) * 100.0).roundToInt() / 100.0

                    baseRollNum++
                    val record = DiceRollRecord(
                        rollNumber = baseRollNum,
                        startDie1 = 3,
                        startDie2 = 2,
                        die1 = d1,
                        die2 = d2,
                        totalSum = sum,
                        outcomeCategory = outcome.id,
                        selectedBet = aiPick.id,
                        mlPredictedCategory = aiPick.id,
                        mlConfidence = tempReport.evaluations[aiPick]?.mlProbability ?: 0.417,
                        stakeEur = stake,
                        payoutEur = payout,
                        netProfitEur = net,
                        wonBet = won,
                        mlCorrect = won,
                        timestamp = System.currentTimeMillis() + step
                    )
                    generatedBatch.add(record)
                    existingChrono.add(record)

                    if (step % 5 == 0) {
                        currentWeights = DiceMlEngine.trainWeights(existingChrono, currentWeights, extraEpochs = 4)
                        controlsState.update {
                            it.copy(
                                displayedDie1 = d1,
                                displayedDie2 = d2,
                                isAtCanonicalStart3And2 = (d1 == 3 && d2 == 2),
                                autoLearnProgressText = "Auto-Learning Roll $step/$autoRollCount • Epoch ${currentWeights.trainedEpochs}"
                            )
                        }
                        delay(45)
                    }
                }
                repository.insertRolls(generatedBatch)
            }

            // Final deep backpropagation pass over all chronological history
            val allChrono = repository.allRollsDesc.firstOrNull()
                ?.sortedWith(compareBy<DiceRollRecord> { it.timestamp }.thenBy { it.id })
                ?: emptyList()

            val newlyLearned = DiceMlEngine.trainWeights(
                rollsChronological = allChrono,
                baseWeights = currentWeights,
                extraEpochs = extraEpochs
            )

            val updatedReport = DiceMlEngine.analyzeAndPredict(
                rollsChronological = allChrono,
                currentStartFaces = 3 to 2,
                initialStakeEur = controlsState.value.initialStakeEur,
                config = cfg,
                activeWeights = newlyLearned
            )

            persistActiveAutoWeights(newlyLearned, updatedReport.smartPickCategory)

            controlsState.update {
                it.copy(
                    isAutoLearningBatch = false,
                    autoLearnProgressText = null,
                    activeLearnedWeights = newlyLearned,
                    selectedBet = updatedReport.smartPickCategory,
                    displayedDie1 = 3,
                    displayedDie2 = 2,
                    isAtCanonicalStart3And2 = true,
                    weightsStatusToast = "Auto-Learned (${newlyLearned.trainedEpochs} epochs) → Pick: ${updatedReport.smartPickCategory.title}"
                )
            }
        }
    }

    /**
     * Saves the current learned ML weights to a named checkpoint in Room so the user can load it anytime.
     */
    fun saveCurrentWeightsCheckpoint(customName: String? = null) {
        viewModelScope.launch {
            val weights = controlsState.value.activeLearnedWeights
            val report = uiState.value.mlReport
            val timeStr = SimpleDateFormat("MMM dd HH:mm:ss", Locale.US).format(Date())
            val label = customName?.takeIf { it.isNotBlank() }
                ?: "Weights • ${weights.trainedEpochs} Ep • ${report.smartPickCategory.shortName} ($timeStr)"

            val checkpoint = MlWeightCheckpointEntity(
                label = label,
                isAutoSavedActive = false,
                trainedEpochs = weights.trainedEpochs,
                trainedSampleCount = report.totalHistoricalSamples,
                validationAccuracyPercent = report.modelAccuracyPercent,
                modelLoss = weights.lastLoss,
                serializedWeightsPayload = weights.serialize(),
                bestCategoryId = report.smartPickCategory.id
            )
            repository.saveNamedWeightCheckpoint(checkpoint)
            persistActiveAutoWeights(weights, report.smartPickCategory)

            controlsState.update {
                it.copy(
                    weightsStatusToast = "Saved weights: $label"
                )
            }
        }
    }

    /**
     * Loads any previously saved ML weights checkpoint from Room and immediately updates predictions.
     */
    fun loadWeightsCheckpoint(checkpoint: MlWeightCheckpointEntity) {
        viewModelScope.launch {
            val loaded = LearnedMlWeights.deserialize(checkpoint.serializedWeightsPayload) ?: return@launch
            val recCat = BetCategory.fromId(checkpoint.bestCategoryId)
            persistActiveAutoWeights(loaded, recCat)
            controlsState.update {
                it.copy(
                    activeLearnedWeights = loaded,
                    selectedBet = recCat,
                    showWeightsManagerSheet = false,
                    weightsStatusToast = "Loaded: ${checkpoint.label}"
                )
            }
        }
    }

    fun deleteWeightsCheckpoint(id: Long) {
        viewModelScope.launch {
            repository.deleteWeightCheckpoint(id)
        }
    }

    private suspend fun persistActiveAutoWeights(weights: LearnedMlWeights, bestCat: BetCategory) {
        val autoEntity = MlWeightCheckpointEntity(
            label = "AUTO_ACTIVE",
            isAutoSavedActive = true,
            trainedEpochs = weights.trainedEpochs,
            trainedSampleCount = weights.trainedSamples,
            validationAccuracyPercent = weights.validationAccuracyPercent,
            modelLoss = weights.lastLoss,
            serializedWeightsPayload = weights.serialize(),
            bestCategoryId = bestCat.id
        )
        repository.saveActiveAutoWeights(autoEntity)
    }

    fun openInitialStakeSetup() {
        controlsState.update { it.copy(showInitialStakeDialog = true) }
    }

    fun dismissInitialStakeSetup() {
        controlsState.update { it.copy(showInitialStakeDialog = false) }
    }

    fun confirmPreGameInitialStake(newInitialStake: Double, resetSession: Boolean) {
        val clampedStake = newInitialStake.roundToInt().toDouble().coerceIn(1.0, 30.0)
        viewModelScope.launch {
            if (resetSession) {
                repository.clearAllRolls()
            }
            controlsState.update { state ->
                state.copy(
                    showInitialStakeDialog = false,
                    initialStakeEur = clampedStake,
                    currentStakeEur = clampedStake,
                    bankrollEur = if (resetSession) state.startingBankrollEur else state.bankrollEur,
                    displayedDie1 = 3,
                    displayedDie2 = 2,
                    isAtCanonicalStart3And2 = true,
                    lastRollBanner = if (resetSession) null else state.lastRollBanner
                )
            }
        }
    }

    fun resetDiceToStart3And2() {
        if (controlsState.value.isRollingAnimation) return
        controlsState.update {
            it.copy(
                displayedDie1 = 3,
                displayedDie2 = 2,
                isAtCanonicalStart3And2 = true
            )
        }
    }

    fun setHistoryFilter(filter: String) {
        controlsState.update { it.copy(historyFilter = filter) }
    }

    fun toggleHistorySortOrder() {
        controlsState.update { it.copy(historySortAscending = !it.historySortAscending) }
    }

    fun toggleManualRollSheet(show: Boolean) {
        controlsState.update { it.copy(showManualRollSheet = show, editingRollRecord = null) }
    }

    fun openEditRollDialog(record: DiceRollRecord) {
        controlsState.update { it.copy(editingRollRecord = record) }
    }

    fun dismissEditRollDialog() {
        controlsState.update { it.copy(editingRollRecord = null) }
    }

    fun toggleCsvExportDialog(show: Boolean) {
        controlsState.update { it.copy(showCsvExportDialog = show) }
    }

    fun updateStatisticalConfig(transform: (StatisticalConfig) -> StatisticalConfig) {
        controlsState.update { state ->
            state.copy(statisticalConfig = transform(state.statisticalConfig))
        }
    }

    fun throwDice() {
        val currentControls = controlsState.value
        if (currentControls.isRollingAnimation || currentControls.isAutoLearningBatch) return

        viewModelScope.launch {
            val stake = currentControls.currentStakeEur.coerceIn(1.0, 30.0)
            val chosenBet = currentControls.selectedBet
            val mlPrediction = uiState.value.mlReport.smartPickCategory
            val mlConfidence = uiState.value.mlReport.evaluations[mlPrediction]?.mlProbability ?: (15.0 / 36.0)
            val startD1 = if (currentControls.alwaysStartFrom3And2) 3 else currentControls.displayedDie1
            val startD2 = if (currentControls.alwaysStartFrom3And2) 2 else currentControls.displayedDie2

            controlsState.update {
                it.copy(
                    isRollingAnimation = true,
                    displayedDie1 = startD1,
                    displayedDie2 = startD2,
                    isAtCanonicalStart3And2 = (startD1 == 3 && startD2 == 2)
                )
            }

            delay(100)

            repeat(6) {
                controlsState.update {
                    it.copy(
                        displayedDie1 = secureRandom.nextInt(6) + 1,
                        displayedDie2 = secureRandom.nextInt(6) + 1,
                        isAtCanonicalStart3And2 = false
                    )
                }
                delay(50)
            }

            val (finalDie1, finalDie2) = DiceMlEngine.rollNextFromCanonical3And2(
                historyChronological = uiState.value.rollsChronological,
                randomSeed = secureRandom.nextInt(10000)
            )
            recordOutcomeInternal(
                startD1 = startD1,
                startD2 = startD2,
                finalDie1 = finalDie1,
                finalDie2 = finalDie2,
                chosenBet = chosenBet,
                stake = stake,
                mlPrediction = mlPrediction,
                mlConfidence = mlConfidence
            )
        }
    }

    fun recordExternalRoll(die1: Int, die2: Int) {
        val currentControls = controlsState.value
        if (currentControls.isRollingAnimation) return

        viewModelScope.launch {
            val stake = currentControls.currentStakeEur.coerceIn(1.0, 30.0)
            val chosenBet = currentControls.selectedBet
            val mlPrediction = uiState.value.mlReport.smartPickCategory
            val mlConfidence = uiState.value.mlReport.evaluations[mlPrediction]?.mlProbability ?: (15.0 / 36.0)
            val startD1 = if (currentControls.alwaysStartFrom3And2) 3 else currentControls.displayedDie1
            val startD2 = if (currentControls.alwaysStartFrom3And2) 2 else currentControls.displayedDie2

            recordOutcomeInternal(
                startD1 = startD1,
                startD2 = startD2,
                finalDie1 = die1.coerceIn(1, 6),
                finalDie2 = die2.coerceIn(1, 6),
                chosenBet = chosenBet,
                stake = stake,
                mlPrediction = mlPrediction,
                mlConfidence = mlConfidence
            )
            controlsState.update { it.copy(showManualRollSheet = false) }
        }
    }

    fun updateExistingRoll(record: DiceRollRecord, newDie1: Int, newDie2: Int) {
        viewModelScope.launch {
            val d1 = newDie1.coerceIn(1, 6)
            val d2 = newDie2.coerceIn(1, 6)
            val newSum = d1 + d2
            val newOutcome = BetCategory.fromSum(newSum)
            val chosenBet = BetCategory.fromId(record.selectedBet)
            val mlPred = BetCategory.fromId(record.mlPredictedCategory)
            val wonBet = (newOutcome == chosenBet)
            val cfg = controlsState.value.statisticalConfig
            val grossReturn = if (wonBet) record.stakeEur * cfg.grossReturnMultiple(chosenBet) else 0.0
            val payout = (grossReturn * 100.0).roundToInt() / 100.0
            val netProfit = ((payout - record.stakeEur) * 100.0).roundToInt() / 100.0

            val updated = record.copy(
                die1 = d1,
                die2 = d2,
                totalSum = newSum,
                outcomeCategory = newOutcome.id,
                payoutEur = payout,
                netProfitEur = netProfit,
                wonBet = wonBet,
                mlCorrect = (newOutcome == mlPred)
            )
            repository.updateRoll(updated)
            controlsState.update {
                it.copy(
                    editingRollRecord = null,
                    displayedDie1 = d1,
                    displayedDie2 = d2,
                    isAtCanonicalStart3And2 = (d1 == 3 && d2 == 2)
                )
            }
        }
    }

    fun deleteRoll(id: Long) {
        viewModelScope.launch {
            repository.deleteRollById(id)
        }
    }

    private suspend fun recordOutcomeInternal(
        startD1: Int,
        startD2: Int,
        finalDie1: Int,
        finalDie2: Int,
        chosenBet: BetCategory,
        stake: Double,
        mlPrediction: BetCategory,
        mlConfidence: Double
    ) {
        val totalSum = finalDie1 + finalDie2
        val outcomeCategory = BetCategory.fromSum(totalSum)
        val wonBet = (outcomeCategory == chosenBet)
        val cfg = controlsState.value.statisticalConfig

        val rawPayout = if (wonBet) stake * cfg.grossReturnMultiple(chosenBet) else 0.0
        val payoutEur = (rawPayout * 100.0).roundToInt() / 100.0
        val netProfitEur = ((payoutEur - stake) * 100.0).roundToInt() / 100.0
        val mlCorrect = (outcomeCategory == mlPrediction)

        val nextRollNumber = (uiState.value.rollsDesc.maxOfOrNull { it.rollNumber } ?: 0) + 1

        val record = DiceRollRecord(
            rollNumber = nextRollNumber,
            startDie1 = startD1,
            startDie2 = startD2,
            die1 = finalDie1,
            die2 = finalDie2,
            totalSum = totalSum,
            outcomeCategory = outcomeCategory.id,
            selectedBet = chosenBet.id,
            mlPredictedCategory = mlPrediction.id,
            mlConfidence = mlConfidence,
            stakeEur = stake,
            payoutEur = payoutEur,
            netProfitEur = netProfitEur,
            wonBet = wonBet,
            mlCorrect = mlCorrect
        )

        repository.insertRoll(record)

        // If Auto-Learn Mode is enabled, immediately train the weights on the new roll and auto-save!
        val currentControls = controlsState.value
        var updatedWeights = currentControls.activeLearnedWeights
        var nextRecommendedBet = chosenBet

        if (currentControls.autoLearnEnabled) {
            val allChrono = (uiState.value.rollsChronological + record)
            updatedWeights = DiceMlEngine.trainWeights(
                rollsChronological = allChrono,
                baseWeights = updatedWeights,
                extraEpochs = 6
            )
            val nextReport = DiceMlEngine.analyzeAndPredict(
                rollsChronological = allChrono,
                currentStartFaces = 3 to 2,
                initialStakeEur = currentControls.initialStakeEur,
                config = cfg,
                activeWeights = updatedWeights
            )
            nextRecommendedBet = nextReport.smartPickCategory
            persistActiveAutoWeights(updatedWeights, nextRecommendedBet)
        }

        val banner = LastRollBannerState(
            rollNumber = nextRollNumber,
            startDie1 = startD1,
            startDie2 = startD2,
            die1 = finalDie1,
            die2 = finalDie2,
            sum = totalSum,
            outcomeCategory = outcomeCategory,
            selectedBet = chosenBet,
            stakeEur = stake,
            payoutEur = payoutEur,
            netProfitEur = netProfitEur,
            wonBet = wonBet,
            mlPredicted = mlPrediction,
            mlCorrect = mlCorrect
        )

        controlsState.update { state ->
            val updatedBankroll = ((state.bankrollEur + netProfitEur) * 100.0).roundToInt() / 100.0
            state.copy(
                isRollingAnimation = false,
                displayedDie1 = finalDie1,
                displayedDie2 = finalDie2,
                isAtCanonicalStart3And2 = (finalDie1 == 3 && finalDie2 == 2),
                bankrollEur = if (updatedBankroll < 1.0) 150.0 else updatedBankroll,
                activeLearnedWeights = updatedWeights,
                selectedBet = if (state.autoLearnEnabled) nextRecommendedBet else state.selectedBet,
                lastRollBanner = banner
            )
        }
    }

    fun clearAllHistoryAndReset() {
        viewModelScope.launch {
            repository.clearAllRolls()
            controlsState.update { state ->
                state.copy(
                    displayedDie1 = 3,
                    displayedDie2 = 2,
                    isAtCanonicalStart3And2 = true,
                    bankrollEur = state.startingBankrollEur,
                    currentStakeEur = state.initialStakeEur,
                    lastRollBanner = null
                )
            }
        }
    }

    companion object {
        fun provideFactory(repository: DiceRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DiceGameViewModel(repository) as T
                }
            }
    }
}
