package com.example.buck_score

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scorecards")
data class ScorecardEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,
    val species: String,
    val buckType: String?, // nullable

    val measurementsJson: String, // serialized MeasurementStore

    val imagePath: String?, // file path to saved image

    val netScore: Double,

    val dateSaved: Long
)
