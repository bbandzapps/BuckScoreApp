package com.example.buck_score

import android.view.View
import com.example.buck_score.ScoreFragment.BuckType
import com.example.buck_score.ScoreFragment.MeasurementType
import com.example.buck_score.ScoreFragment.RowConfig
import com.example.buck_score.ScoreFragment.ScoreBreakdown
import com.google.android.material.color.utilities.Score
import kotlin.math.max

class PronghornProfile : ScoringProfile {

    override fun getVisibleSections() = listOf(
        ScoreFragment.Section.LENGTHS,
        ScoreFragment.Section.SPREADS,
        ScoreFragment.Section.CIRCUMFERENCES
    )

    override fun getScoreDisplayConfig() = ScoreFragment.ScoreDisplayConfig(
        showSpread = false,
        showAbnormals = false
    )

    override fun getSectionConfigs(): List<ScoreFragment.SectionConfig> {
        return listOf(
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.SPREADS,
                title = "Spreads",
                note = "Distance between left and right horns",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Tip to Tip Spread",
                        type = ScoreFragment.MeasurementType.TipSpread,
                        layoutType = ScoreFragment.RowLayoutType.SINGLE
                    ),
                    RowConfig(
                        label = "Inner Spread",
                        type = ScoreFragment.MeasurementType.InnerSpread,
                        layoutType = ScoreFragment.RowLayoutType.SINGLE
                    ),
                ),
                maxDynamicRows = 0
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.LENGTHS,
                title = "Lengths",
                note = "Entire length of each horn and prong",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Length of main horns",
                        type = ScoreFragment.MeasurementType.MainBeam,
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Length of prongs",
                        type = ScoreFragment.MeasurementType.ProngLength,
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    )
                ),
                maxDynamicRows = 0
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.CIRCUMFERENCES,
                title = "Circumferences",
                note = "Circumference measurements at different parts of each horn",
                subNote = "Note: The location of each measurement on both horns is based on the percentage of the longer horn. Animals where the shorter horn doesn't reach a measurement mark receive a 0 for that side's circumference measurement",
                rows = listOf(
                    RowConfig(
                        label = "Base",
                        type = ScoreFragment.MeasurementType.Circumference(1),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "1st Quarter (25% length of longer horn)",
                        type = ScoreFragment.MeasurementType.Circumference(2),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "2nd Quarter (50% length of longer horn)",
                        type = ScoreFragment.MeasurementType.Circumference(3),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Third Quarter (75% length of longer horn)",
                        type = ScoreFragment.MeasurementType.Circumference(4),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    )
                ),
                maxDynamicRows = 0
            )

        )
    }


    override fun calculateScore(
        store: ScoreFragment.MeasurementStore,
        buckType: BuckType?
    ): ScoreBreakdown {

        var gross = 0.0

        val circumferences = store.getAll()
            .mapNotNull { it.type as? MeasurementType.Circumference }
            .distinctBy { it.index }
            .map { store.getPaired(it) }

        val mainBeams = store.getPaired(MeasurementType.MainBeam)
        val prongLengths = store.getPaired(MeasurementType.ProngLength)

        val total_difference = getTotalDifference(circumferences) + mainBeams.difference() + prongLengths.difference()


        val leftSum = leftSum(circumferences) + mainBeams.left + prongLengths.left
        val rightSum = rightSum(circumferences) + mainBeams.right + prongLengths.right

        val subtotal = leftSum + rightSum
        var finalScore = subtotal - total_difference

        gross = leftSum + rightSum

        return ScoreBreakdown(
            leftSum = leftSum,
            rightSum = rightSum,
            differenceTotal = total_difference,
            abnormalSum = 0.0,
            spreadCredit = 0.0,
            subtotal = subtotal,
            gross = gross,
            finalScore = finalScore
        )
    }
}

