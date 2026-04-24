package com.example.buck_score

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

val gson = Gson()

fun MeasurementStore.toJson(): String {
    return gson.toJson(getAll())
}

fun jsonToMeasurements(json: String): List<MeasurementValue> {
    val type = object : TypeToken<List<MeasurementValue>>() {}.type
    return gson.fromJson(json, type)
}
