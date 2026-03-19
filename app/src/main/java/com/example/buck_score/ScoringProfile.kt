package com.example.buck_score

import com.example.buck_score.ScoreFragment.PairedMeasurement

interface ScoringProfile {
    fun getVisibleSections():List<ScoreFragment.Section>
    //fun buildUI(builder: ScoreUIBuilder)
    fun getSectionConfigs(): List<ScoreFragment.SectionConfig>

    fun calculateScore(store: ScoreFragment.MeasurementStore, buckType: ScoreFragment.BuckType?): ScoreFragment.ScoreBreakdown

    fun getTotalDifference(pairs: List<PairedMeasurement>): Double =
        pairs.sumOf { it.difference() }
    fun rightSum(pairs: List<PairedMeasurement>): Double =
        pairs.sumOf { it.right }
    fun leftSum(pairs: List<PairedMeasurement>): Double =
        pairs.sumOf { it.left }
}