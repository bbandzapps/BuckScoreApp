package com.example.buck_score

interface ScoringProfile {

    fun buildUI(builder: ScoreUIBuilder)

    fun calculateScore(store: ScoreFragment.MeasurementStore): ScoreFragment.ScoreBreakdown
}