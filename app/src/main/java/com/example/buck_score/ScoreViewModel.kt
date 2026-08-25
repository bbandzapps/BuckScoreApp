package com.example.buck_score
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ScoreViewModel: ViewModel() {
    var name: String=""
    var species: Species = Species.WHITETAIL
    var buckType: BuckType = BuckType.TYPICAL
    var measurementStore: MeasurementStore ?= null
    var imagePath: String? = null
    var editingId: Int? = null
}