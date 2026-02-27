package com.example.buck_score

class ScoreUIBuilder(
    private val fragment: ScoreFragment
) {

    fun showSpreadsSection() {
        fragment.showSpreadsSection()
    }

    fun showLengthsSection() {
        fragment.showLengthsSection()
    }

    fun showCircumferenceSection() {
        fragment.showCircumferenceSection()
    }

    fun showAbnormalsSection() {
        fragment.showAbnormalsSection()
    }

    fun showPointsSection() {
        fragment.showPointsSection()
    }

    fun showTypeSection() {
        fragment.showTypeSection()
    }

    // Bridge to existing setup methods
    fun setupDeerSpreads() {
        fragment.setupDeerSpreads()
    }

    fun setupDeerLengths() {
        fragment.setupDeerLengths()
    }

    fun setupDeerCircumferences() {
        fragment.setupDeerCircumferences()
    }

    fun setupDeerAbnormals() {
        fragment.setupDeerAbnormals()
    }
}
