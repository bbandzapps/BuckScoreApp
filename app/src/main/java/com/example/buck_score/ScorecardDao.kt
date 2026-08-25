package com.example.buck_score

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScorecardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scorecard: ScorecardEntity): Long


    @Query("SELECT * FROM scorecards ORDER BY dateSaved DESC")
    suspend fun getAll(): List<ScorecardEntity>

    @Query("""
SELECT * FROM scorecards
WHERE (LOWER(:search) IS NULL OR LOWER(name) LIKE LOWER('%' || :search || '%')
OR LOWER(species) LIKE LOWER('%' || :search || '%'))
AND (:minScore IS NULL OR netScore >= :minScore)
AND (:maxScore IS NULL OR netScore <= :maxScore)
AND (:startDate IS NULL OR dateSaved >= :startDate)
AND (:endDate IS NULL OR dateSaved <= :endDate)
AND (:species IS NULL OR species = :species)
""")
    suspend fun getFiltered(
        search: String?,
        minScore: Double?,
        maxScore: Double?,
        startDate: Long?,
        endDate: Long?,
        species: String?
    ): List<ScorecardEntity>

    @Query("""SELECT * FROM scorecards WHERE :scorecardId == id""")
    suspend fun getById(scorecardId: Int): ScorecardEntity

    @Delete
    suspend fun delete(scorecard: ScorecardEntity)
}