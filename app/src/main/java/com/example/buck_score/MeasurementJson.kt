package com.example.buck_score

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

private val gson = GsonBuilder()
    .registerTypeAdapter(
        MeasurementType::class.java,
        MeasurementTypeAdapter()
    )
    .create()

fun MeasurementStore.toJson(): String {
    return gson.toJson(getAll())
}

fun jsonToMeasurements(json: String): List<MeasurementValue> {
    val type = object : TypeToken<List<MeasurementValue>>() {}.type
    return gson.fromJson(json, type)
}
