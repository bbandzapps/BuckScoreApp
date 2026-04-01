package com.example.buck_score

import android.view.View
import com.example.buck_score.ScoreFragment.BuckType
import com.example.buck_score.ScoreFragment.MeasurementType
import com.example.buck_score.ScoreFragment.RowConfig
import com.example.buck_score.ScoreFragment.ScoreBreakdown
import com.google.android.material.color.utilities.Score
import kotlin.math.max

class CaribouProfile : ScoringProfile {

    override fun getVisibleSections() = listOf(
        ScoreFragment.Section.POINT_COUNT,
        ScoreFragment.Section.LENGTHS,
        ScoreFragment.Section.SPREADS,
        ScoreFragment.Section.WIDTHS,
        ScoreFragment.Section.CIRCUMFERENCES
    )

    override fun getScoreDisplayConfig() = ScoreFragment.ScoreDisplayConfig(
        showSpread = true,
        showAbnormals = false,
        showCrownPointScore = false
    )

    override fun getSectionConfigs(): List<ScoreFragment.SectionConfig> {
        return listOf(
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.POINT_COUNT,
                title = "Points",
                note = "Number of points on the antlers and the brow tines",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Points on antlers",
                        type = ScoreFragment.MeasurementType.PointCount,
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT_NO_FRAC
                    ),
                    RowConfig(
                        label = "Points on brows",
                        type = ScoreFragment.MeasurementType.BrowPointCount,
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT_NO_FRAC,
                        showDifference = false
                    )
                ),
                maxDynamicRows = 0
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.SPREADS,
                title = "Spreads",
                note = "Distance that the antlers span",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Tip to Tip (Main Beams)",
                        type = ScoreFragment.MeasurementType.TipSpread,
                        layoutType = ScoreFragment.RowLayoutType.SINGLE,
                        showDifference = false
                    ),
                    RowConfig(
                        label = "Greatest Spread",
                        type = ScoreFragment.MeasurementType.GreatestSpread,
                        layoutType = ScoreFragment.RowLayoutType.SINGLE,
                        showDifference = false
                    ),
                    RowConfig(
                        label = "Inner Spread",
                        type = ScoreFragment.MeasurementType.InnerSpread,
                        layoutType = ScoreFragment.RowLayoutType.SINGLE,
                        showDifference = false
                    )
                ),
                maxDynamicRows = 0
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.WIDTHS,
                title = "Widths",
                note = "Widths of the antlers and the brow palms",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Brow palm",
                        type = ScoreFragment.MeasurementType.BrowWidth,
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT,
                        showDifference = false
                    ),
                    RowConfig(
                        label = "Top Palm",
                        type = ScoreFragment.MeasurementType.Width,
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    )
                ),
                maxDynamicRows = 0
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.LENGTHS,
                title = "Lengths",
                note = "Length of regular points and brows",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Main Beams",
                        type = ScoreFragment.MeasurementType.MainBeam,
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "First Point (Brow Palm)",
                        type = ScoreFragment.MeasurementType.G(1),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT,
                        showDifference = false
                    ),
                    RowConfig(
                        label = "Second Point (Bez)",
                        type = ScoreFragment.MeasurementType.G(2),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Rear Point",
                        type = ScoreFragment.MeasurementType.G(3),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Second Longest Top Point",
                        type = ScoreFragment.MeasurementType.G(4),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Longest Top Point",
                        type = ScoreFragment.MeasurementType.G(5),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    )
                ),
                maxDynamicRows = 0
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.CIRCUMFERENCES,
                title = "Circumferences",
                note = "Smallest circumferences of main beam between major points",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Smallest circumference between Brow and Bez points",
                        type = ScoreFragment.MeasurementType.Circumference(1),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Smallest circumference between Bez and Rear points",
                        type = ScoreFragment.MeasurementType.Circumference(2),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Smallest circumference between Rear point and first top point",
                        type = ScoreFragment.MeasurementType.Circumference(3),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Smallest circumference between two longest top palm points",
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

        val gPoints = store.getAll()
            .mapNotNull { it.type as? MeasurementType.G }
            .distinctBy { it.index }
            .map { store.getPaired(it) }

        val circumferences = store.getAll()
            .mapNotNull { it.type as? MeasurementType.Circumference }
            .distinctBy { it.index }
            .map { store.getPaired(it) }

        val mainBeams = store.getPaired(MeasurementType.MainBeam)
        val widths = store.getPaired(MeasurementType.Width)
        val browWidths = store.getPaired(MeasurementType.BrowWidth)
        val pointCount = store.getPaired(MeasurementType.PointCount)
        val browPoints = store.getPaired(MeasurementType.BrowPointCount)

        val total_difference =
            getTotalDifference(gPoints) + getTotalDifference(circumferences) +
                    mainBeams.difference() + widths.difference() +
                    pointCount.difference() - store.getPaired(MeasurementType.G(1)).difference()


        val innerSpread = store.getAll()
            .filter { it.type is MeasurementType.InnerSpread }
            .sumOf { it.value }

        var isCapped = false
        var spreadCredit = innerSpread
        if (spreadCredit > max(mainBeams.left, mainBeams.right)){
            spreadCredit = max(mainBeams.left, mainBeams.right)
            isCapped = true
        }


        val leftSum = leftSum(gPoints) + leftSum(circumferences) +
                mainBeams.left + widths.left + browWidths.left +
                browPoints.left + pointCount.left
        val rightSum = rightSum(gPoints) + rightSum(circumferences) +
                mainBeams.right + widths.right + browWidths.right +
                browPoints.right + pointCount.right

        val subtotal = leftSum + rightSum + spreadCredit
        var finalScore = subtotal - total_difference

        gross = innerSpread + leftSum + rightSum


        return ScoreBreakdown(
            leftSum = leftSum,
            rightSum = rightSum,
            differenceTotal = total_difference,
            abnormalSum = 0.0,
            spreadCredit = spreadCredit,
            subtotal = subtotal,
            crownPointScore = 0.0,
            gross = gross,
            finalScore = finalScore,
            spreadIsCapped = isCapped
        )
    }
}

