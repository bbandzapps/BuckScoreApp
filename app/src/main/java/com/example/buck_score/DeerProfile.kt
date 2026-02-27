package com.example.buck_score

class DeerProfile : ScoringProfile {

    override fun buildUI(builder: ScoreUIBuilder) {

        builder.showSpreadsSection()
        builder.setupDeerSpreads()

        builder.showLengthsSection()
        builder.setupDeerLengths()

        builder.showCircumferenceSection()
        builder.setupDeerCircumferences()

        builder.showAbnormalsSection()
        builder.setupDeerAbnormals()

        builder.showPointsSection()
        builder.showTypeSection()
    }

    override fun calculateScore(
        store: ScoreFragment.MeasurementStore
    ): ScoreFragment.ScoreBreakdown {

        // For now, return a placeholder.
        // We will move deer scoring here later.
        return ScoreFragment.ScoreBreakdown()
    }
}
