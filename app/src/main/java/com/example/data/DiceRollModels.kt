package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow

enum class BetCategory(
    val id: String,
    val title: String,
    val rangeLabel: String,
    val defaultMultiplier: Double,
    val theoreticalProb: Double
) {
    UNDER(
        id = "UNDER",
        title = "Under (2–6)",
        rangeLabel = "2–6",
        defaultMultiplier = 2.3,
        theoreticalProb = 15.0 / 36.0
    ),
    SEVEN(
        id = "SEVEN",
        title = "Seven (7)",
        rangeLabel = "7",
        defaultMultiplier = 5.8,
        theoreticalProb = 6.0 / 36.0
    ),
    OVER(
        id = "OVER",
        title = "Over (8–12)",
        rangeLabel = "8–12",
        defaultMultiplier = 2.3,
        theoreticalProb = 15.0 / 36.0
    );

    val multiplier: Double
        get() = defaultMultiplier

    val shortName: String
        get() = when (this) {
            UNDER -> "UNDER"
            SEVEN -> "SEVEN"
            OVER -> "OVER"
        }

    companion object {
        fun fromSum(sum: Int): BetCategory = when {
            sum in 2..6 -> UNDER
            sum == 7 -> SEVEN
            else -> OVER
        }

        fun fromId(id: String): BetCategory = entries.find { it.id == id } ?: UNDER
    }
}

@Entity(tableName = "dice_rolls")
data class DiceRollRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rollNumber: Int,
    val startDie1: Int = 3,
    val startDie2: Int = 2,
    val die1: Int,
    val die2: Int,
    val totalSum: Int = die1 + die2,
    val outcomeCategory: String = BetCategory.fromSum(die1 + die2).id,
    val selectedBet: String = BetCategory.UNDER.id,
    val mlPredictedCategory: String = BetCategory.UNDER.id,
    val mlConfidence: Double = 15.0 / 36.0,
    val stakeEur: Double = 5.0,
    val payoutEur: Double = 0.0,
    val netProfitEur: Double = 0.0,
    val wonBet: Boolean = false,
    val mlCorrect: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Persisted Machine Learning Weight Checkpoint stored in Room.
 * Stores serialized neural network weights (W1, b1, W2, b2), learned Markov transition matrix,
 * Dirichlet category pseudo-counts, ensemble expert weights, training epochs, and accuracy.
 */
@Entity(tableName = "ml_weight_checkpoints")
data class MlWeightCheckpointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val isAutoSavedActive: Boolean = false,
    val trainedEpochs: Int,
    val trainedSampleCount: Int,
    val validationAccuracyPercent: Double,
    val modelLoss: Double,
    val serializedWeightsPayload: String,
    val bestCategoryId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DiceRollDao {
    @Query("SELECT * FROM dice_rolls ORDER BY timestamp DESC, id DESC")
    fun observeAllRollsDesc(): Flow<List<DiceRollRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoll(roll: DiceRollRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRolls(rolls: List<DiceRollRecord>)

    @Update
    suspend fun updateRoll(roll: DiceRollRecord)

    @Query("DELETE FROM dice_rolls WHERE id = :id")
    suspend fun deleteRollById(id: Long)

    @Query("DELETE FROM dice_rolls")
    suspend fun clearAllRolls()

    // ML Weight Checkpoints CRUD
    @Query("SELECT * FROM ml_weight_checkpoints WHERE isAutoSavedActive = 0 ORDER BY timestamp DESC")
    fun observeSavedWeightCheckpoints(): Flow<List<MlWeightCheckpointEntity>>

    @Query("SELECT * FROM ml_weight_checkpoints WHERE isAutoSavedActive = 1 ORDER BY timestamp DESC LIMIT 1")
    fun observeActiveAutoSavedWeights(): Flow<MlWeightCheckpointEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightCheckpoint(checkpoint: MlWeightCheckpointEntity): Long

    @Query("DELETE FROM ml_weight_checkpoints WHERE isAutoSavedActive = 1")
    suspend fun clearActiveAutoSavedWeights()

    @Query("DELETE FROM ml_weight_checkpoints WHERE id = :id")
    suspend fun deleteWeightCheckpointById(id: Long)
}

@Database(
    entities = [DiceRollRecord::class, MlWeightCheckpointEntity::class],
    version = 3,
    exportSchema = false
)
abstract class DiceDatabase : RoomDatabase() {
    abstract fun diceRollDao(): DiceRollDao

    companion object {
        @Volatile
        private var INSTANCE: DiceDatabase? = null

        fun getInstance(context: Context): DiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DiceDatabase::class.java,
                    "diceml_predictor.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class DiceRepository(private val dao: DiceRollDao) {
    val allRollsDesc: Flow<List<DiceRollRecord>> = dao.observeAllRollsDesc()
    val savedWeightCheckpoints: Flow<List<MlWeightCheckpointEntity>> = dao.observeSavedWeightCheckpoints()
    val activeAutoSavedWeights: Flow<MlWeightCheckpointEntity?> = dao.observeActiveAutoSavedWeights()

    suspend fun insertRoll(roll: DiceRollRecord): Long = dao.insertRoll(roll)

    suspend fun insertRolls(rolls: List<DiceRollRecord>) = dao.insertRolls(rolls)

    suspend fun updateRoll(roll: DiceRollRecord) = dao.updateRoll(roll)

    suspend fun deleteRollById(id: Long) = dao.deleteRollById(id)

    suspend fun clearAllRolls() = dao.clearAllRolls()

    suspend fun saveActiveAutoWeights(checkpoint: MlWeightCheckpointEntity) {
        dao.clearActiveAutoSavedWeights()
        dao.insertWeightCheckpoint(checkpoint.copy(isAutoSavedActive = true))
    }

    suspend fun saveNamedWeightCheckpoint(checkpoint: MlWeightCheckpointEntity): Long {
        return dao.insertWeightCheckpoint(checkpoint.copy(id = 0, isAutoSavedActive = false))
    }

    suspend fun deleteWeightCheckpoint(id: Long) {
        dao.deleteWeightCheckpointById(id)
    }

    fun exportToCsv(rolls: List<DiceRollRecord>): String {
        val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val sb = StringBuilder()
        sb.appendLine("id,roll_number,die1,die2,calculated_sum,classification,timestamp_epoch_ms,timestamp_iso")
        val chronological = rolls.sortedWith(compareBy<DiceRollRecord> { it.timestamp }.thenBy { it.id })
        chronological.forEachIndexed { idx, r ->
            val iso = isoFormatter.format(Date(r.timestamp))
            sb.appendLine("${r.id},${idx + 1},${r.die1},${r.die2},${r.totalSum},${r.outcomeCategory},${r.timestamp},$iso")
        }
        return sb.toString()
    }
}
