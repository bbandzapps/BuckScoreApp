package com.example.buck_score

import android.view.View
import com.example.buck_score.ScoreFragment.BuckType
import com.example.buck_score.ScoreFragment.MeasurementType
import com.example.buck_score.ScoreFragment.RowConfig
import com.example.buck_score.ScoreFragment.ScoreBreakdown
import com.google.android.material.color.utilities.Score
import kotlin.math.max

class MooseProfile : ScoringProfile {

    override fun getVisibleSections() = listOf(
        ScoreFragment.Section.POINT_COUNT,
        ScoreFragment.Section.LENGTHS,
        ScoreFragment.Section.WIDTHS,
        ScoreFragment.Section.SPREADS,
        ScoreFragment.Section.CIRCUMFERENCES,
        ScoreFragment.Section.ABNORMALS
    )

    override fun getScoreDisplayConfig() = ScoreFragment.ScoreDisplayConfig(
        showSpread = true,
        showAbnormals = false
    )

    override fun getSectionConfigs(): List<ScoreFragment.SectionConfig> {
        //TODO("Not yet implemented")
        return listOf(
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.POINT_COUNT,
                title = "Normal Points",
                note = "Points covered by the typical scoresheet",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Number of normal points",
                        type = ScoreFragment.MeasurementType.PointCount,
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT_NO_FRAC
                    )
                ),
                maxDynamicRows = 0
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.SPREADS,
                title = "Spreads",
                note = "Total distance that the antlers span",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Greatest Spread",
                        type = ScoreFragment.MeasurementType.GreatestSpread,
                        layoutType = ScoreFragment.RowLayoutType.SINGLE
                    )
                ),
                maxDynamicRows = 0
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.ABNORMALS,
                title = "Abnormal Points",
                note = "Points not covered by the typical scoresheet",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Number of abnormal points",
                        type = ScoreFragment.MeasurementType.AbnormalPoint(1),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT_NO_FRAC
                    )
                ),
                maxDynamicRows = 0,
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.LENGTHS,
                title = "Length of Palms",
                note = "Entire vertical length of the palms",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Length of palms including brow palm",
                        type = ScoreFragment.MeasurementType.MainBeam,
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    )
                ),
                maxDynamicRows = 0
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.WIDTHS,
                title = "Width of Palms",
                note = "Entire horizontal width of each palm",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Width of palms",
                        type = ScoreFragment.MeasurementType.Width,
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    )
                ),
                maxDynamicRows = 0
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.CIRCUMFERENCES,
                title = "Circumference of beams",
                note = "Smallest circumferences of main beam between the base and palm",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Smallest circumference of main beams",
                        type = ScoreFragment.MeasurementType.Circumference(1),
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

        // For now, return a placeholder.
        // We will move deer scoring here later.
        //return ScoreFragment.ScoreBreakdown()
        //TODO("Not yet implemented")
        var gross = 0.0

        val normalPoints = store.getPaired(MeasurementType.PointCount)
        val circumferences = store.getPaired(MeasurementType.Circumference(1))
        val lengths = store.getPaired(MeasurementType.MainBeam)
        val abnormals = store.getPaired(MeasurementType.AbnormalPoint(1))
        val widths = store.getPaired(MeasurementType.Width)
        val greatestSpread = store.getAll()
            .filter { it.type is MeasurementType.GreatestSpread }
            .sumOf { it.value }

        val total_difference =
            normalPoints.difference() + circumferences.difference() + abnormals.difference() +
                    lengths.difference() + widths.difference()

        val abnormalPoints = store.getAll().filter { it.type is MeasurementType.AbnormalPoint }
        val abnormalSum = abnormalPoints.sumOf { it.value }


        val leftSum = normalPoints.left + circumferences.left + abnormals.left + lengths.left + widths.left
        val rightSum = normalPoints.right + circumferences.right + abnormals.right + lengths.right + widths.right

        val subtotal = leftSum + rightSum + greatestSpread
        var finalScore = 0.0

        gross = greatestSpread + leftSum + rightSum


        return ScoreBreakdown(
            leftSum = leftSum,
            rightSum = rightSum,
            differenceTotal = total_difference,
            abnormalSum = abnormalSum,
            spreadCredit = greatestSpread,
            subtotal = subtotal,
            gross = gross,
            finalScore = finalScore
        )
    }
}

