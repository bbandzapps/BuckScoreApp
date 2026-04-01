package com.example.buck_score

import android.view.View
import com.example.buck_score.ScoreFragment.BuckType
import com.example.buck_score.ScoreFragment.MeasurementType
import com.example.buck_score.ScoreFragment.RowConfig
import com.example.buck_score.ScoreFragment.ScoreBreakdown
import com.google.android.material.color.utilities.Score
import kotlin.math.max

class DeerProfile : ScoringProfile {

    override fun getVisibleSections() = listOf(
        ScoreFragment.Section.TYPE,
        ScoreFragment.Section.POINT_COUNT,
        ScoreFragment.Section.LENGTHS,
        ScoreFragment.Section.SPREADS,
        ScoreFragment.Section.CIRCUMFERENCES,
        ScoreFragment.Section.ABNORMALS
    )

    override fun getScoreDisplayConfig() = ScoreFragment.ScoreDisplayConfig(
        showSpread = true,
        showAbnormals = true,
        showCrownPointScore = false
    )

    override fun getSectionConfigs(): List<ScoreFragment.SectionConfig> {
        return listOf(
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.POINT_COUNT,
                title = "Points",
                note = "Number of points that are at least 1 inch long",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "",
                        type = ScoreFragment.MeasurementType.PointCount,
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
                type = ScoreFragment.Section.ABNORMALS,
                title = "Abnormal Points",
                note = "Length of abnormal points (droptines, points off of a burr, points off of other points, etc.)",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Abnormal Point 1",
                        type = ScoreFragment.MeasurementType.AbnormalPoint(1),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT,
                        showDifference = false
                    ),
                    RowConfig(
                        label = "Abnormal Point 2",
                        type = ScoreFragment.MeasurementType.AbnormalPoint(2),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT,
                        showDifference = false
                    )
                ),
                maxDynamicRows = 50,
                dynamicBaseType = MeasurementType.AbnormalPoint(1)
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.LENGTHS,
                title = "Lengths",
                note = "Length of regular points and main beams",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Main Beams",
                        type = ScoreFragment.MeasurementType.MainBeam,
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "G1: First Point",
                        type = ScoreFragment.MeasurementType.G(1),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "G2: Second Point",
                        type = ScoreFragment.MeasurementType.G(2),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "G3: Third Point",
                        type = ScoreFragment.MeasurementType.G(3),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "G4: Fourth Point",
                        type = ScoreFragment.MeasurementType.G(4),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    )
                ),
                maxDynamicRows = 15,
                dynamicBaseType = MeasurementType.G(1)
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.CIRCUMFERENCES,
                title = "Circumferences",
                note = "Smallest circumferences of main beam between points",
                subNote = "Note: If a buck does not have enough points to record all circumference measurements, the circumference between the main beam and the last point should be done halfway between the tip of the beam and the base of the last point.",
                rows = listOf(
                    RowConfig(
                        label = "Smallest circumference between G1 and the burr",
                        type = ScoreFragment.MeasurementType.Circumference(1),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Smallest circumference between G1 and G2",
                        type = ScoreFragment.MeasurementType.Circumference(2),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Smallest circumference between G2 and G3",
                        type = ScoreFragment.MeasurementType.Circumference(3),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Smallest circumference between G3 and G4",
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

        val total_difference =
            getTotalDifference(gPoints) + getTotalDifference(circumferences) + mainBeams.difference()

        val abnormalPoints = store.getAll().filter { it.type is MeasurementType.AbnormalPoint }
        val abnormalSum = abnormalPoints.sumOf { it.value }

        val innerSpread = store.getAll()
            .filter { it.type is MeasurementType.InnerSpread }
            .sumOf { it.value }

        var isCapped = false
        var spreadCredit = innerSpread
        if (spreadCredit > max(mainBeams.left, mainBeams.right)){
            spreadCredit = max(mainBeams.left, mainBeams.right)
            isCapped = true
        }



        val leftSum = leftSum(gPoints) + leftSum(circumferences) + mainBeams.left
        val rightSum = rightSum(gPoints) + rightSum(circumferences) + mainBeams.right

        val subtotal = leftSum + rightSum + spreadCredit
        var finalScore = 0.0
        if (buckType == BuckType.TYPICAL)
            finalScore = max(0.0, (subtotal - total_difference - abnormalSum))
        else
            finalScore = subtotal - total_difference + abnormalSum

        gross = innerSpread + leftSum + rightSum + abnormalSum


    return ScoreBreakdown(
        leftSum = leftSum,
        rightSum = rightSum,
        differenceTotal = total_difference,
        abnormalSum = abnormalSum,
        spreadCredit = spreadCredit,
        subtotal = subtotal,
        crownPointScore = 0.0,
        gross = gross,
        finalScore = finalScore,
        spreadIsCapped = isCapped
        )
    }
}

