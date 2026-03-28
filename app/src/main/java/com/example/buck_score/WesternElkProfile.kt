package com.example.buck_score

import android.view.View
import com.example.buck_score.ScoreFragment.BuckType
import com.example.buck_score.ScoreFragment.MeasurementType
import com.example.buck_score.ScoreFragment.RowConfig
import com.example.buck_score.ScoreFragment.ScoreBreakdown
import com.google.android.material.color.utilities.Score
import kotlin.math.max

class WesternElkProfile : ScoringProfile {

    override fun getVisibleSections() = listOf(
        ScoreFragment.Section.POINT_COUNT,
        ScoreFragment.Section.SPREADS,
        ScoreFragment.Section.CROWN_POINTS,
        ScoreFragment.Section.ABNORMALS,
        ScoreFragment.Section.LENGTHS,
        ScoreFragment.Section.CIRCUMFERENCES
    )

    override fun getScoreDisplayConfig() = ScoreFragment.ScoreDisplayConfig(
        showSpread = true,
        showAbnormals = true,
        showCrownPointScore = true
    )

    override fun getSectionConfigs(): List<ScoreFragment.SectionConfig> {
        return listOf(
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.POINT_COUNT,
                title = "Points",
                note = "Number of normal points that are at least 1 inch long",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "",
                        type = ScoreFragment.MeasurementType.PointCount,
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT_NO_FRAC
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
                        layoutType = ScoreFragment.RowLayoutType.SINGLE
                    ),
                    RowConfig(
                        label = "Greatest Spread",
                        type = ScoreFragment.MeasurementType.GreatestSpread,
                        layoutType = ScoreFragment.RowLayoutType.SINGLE
                    ),
                    RowConfig(
                        label = "Inside Main Beams",
                        type = ScoreFragment.MeasurementType.InnerSpread,
                        layoutType = ScoreFragment.RowLayoutType.SINGLE
                    )
                ),
                maxDynamicRows = 0
            ),
            ScoreFragment.SectionConfig(
                type = ScoreFragment.Section.CROWN_POINTS,
                title = "Crown Points",
                note = "Lengths of nontypical points on the G4 or higher",
                subNote = null,
                rows = listOf(
                    RowConfig(
                        label = "Crown Point 1",
                        type = ScoreFragment.MeasurementType.CrownPoint(1),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Crown Point 2",
                        type = ScoreFragment.MeasurementType.CrownPoint(2),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Crown Point 3",
                        type = ScoreFragment.MeasurementType.CrownPoint(3),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Crown Point 4",
                        type = ScoreFragment.MeasurementType.CrownPoint(4),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Crown Point 5",
                        type = ScoreFragment.MeasurementType.CrownPoint(5),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    )
                ),
                maxDynamicRows = 10,
                dynamicBaseType = MeasurementType.CrownPoint(1)
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
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Abnormal Point 2",
                        type = ScoreFragment.MeasurementType.AbnormalPoint(2),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
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
                    ),
                    RowConfig(
                        label = "G5: Fifth Point",
                        type = ScoreFragment.MeasurementType.G(5),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "G6: Sixth Point",
                        type = ScoreFragment.MeasurementType.G(6),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "G7: Seventh Point",
                        type = ScoreFragment.MeasurementType.G(7),
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
                subNote = "Note: If an animal does not have enough points to record all circumference measurements, the circumference between the main beam and the last point should be done halfway between the tip of the beam and the base of the last point.",
                rows = listOf(
                    RowConfig(
                        label = "Smallest circumference between first and second points",
                        type = ScoreFragment.MeasurementType.Circumference(1),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Smallest circumference between second and third points",
                        type = ScoreFragment.MeasurementType.Circumference(2),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Smallest circumference between third and fourth points",
                        type = ScoreFragment.MeasurementType.Circumference(3),
                        layoutType = ScoreFragment.RowLayoutType.LEFT_RIGHT
                    ),
                    RowConfig(
                        label = "Smallest circumference between fourth and fifth points",
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

        val crownPoints = store.getAll().filter { it.type is MeasurementType.CrownPoint }
        val crownSum = crownPoints.sumOf { it.value }

        val mainBeams = store.getPaired(MeasurementType.MainBeam)

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

        val total_difference =
            getTotalDifference(gPoints) + getTotalDifference(circumferences) + mainBeams.difference()

        val leftSum = leftSum(gPoints) + leftSum(circumferences) + mainBeams.left
        val rightSum = rightSum(gPoints) + rightSum(circumferences) + mainBeams.right

        val subtotal = leftSum + rightSum + spreadCredit + crownSum
        var finalScore = max(0.0, (subtotal - total_difference - abnormalSum))

        gross = innerSpread + leftSum + rightSum + abnormalSum + crownSum


        return ScoreBreakdown(
            leftSum = leftSum,
            rightSum = rightSum,
            differenceTotal = total_difference,
            abnormalSum = abnormalSum,
            spreadCredit = spreadCredit,
            subtotal = subtotal,
            crownPointScore = crownSum,
            gross = gross,
            finalScore = finalScore,
            spreadIsCapped = isCapped
        )
    }
}

