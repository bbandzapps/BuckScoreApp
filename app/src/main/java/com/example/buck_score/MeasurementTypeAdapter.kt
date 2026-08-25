package com.example.buck_score

import com.google.gson.*
import java.lang.reflect.Type

class MeasurementTypeAdapter :
    JsonSerializer<MeasurementType>,
    JsonDeserializer<MeasurementType> {

    override fun serialize(
        src: MeasurementType,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {

        val obj = JsonObject()

        when (src) {

            is MeasurementType.G -> {
                obj.addProperty("kind", "G")
                obj.addProperty("index", src.index)
            }

            is MeasurementType.CrownPoint -> {
                obj.addProperty("kind", "CROWN")
                obj.addProperty("index", src.index)
            }

            is MeasurementType.AbnormalPoint -> {
                obj.addProperty("kind", "ABNORMAL")
                obj.addProperty("index", src.index)
            }

            is MeasurementType.Circumference -> {
                obj.addProperty("kind", "CIRC")
                obj.addProperty("index", src.index)
            }

            is MeasurementType.Loc -> {
                obj.addProperty("kind", "LOC")
                obj.addProperty("index", src.index)
            }

            MeasurementType.MainBeam -> {
                obj.addProperty("kind", "MAIN_BEAM")
            }

            MeasurementType.InnerSpread -> {
                obj.addProperty("kind", "INNER_SPREAD")
            }

            MeasurementType.TipSpread -> {
                obj.addProperty("kind", "TIP_SPREAD")
            }

            MeasurementType.GreatestSpread -> {
                obj.addProperty("kind", "GREATEST_SPREAD")
            }

            MeasurementType.PointCount -> {
                obj.addProperty("kind", "POINT_COUNT")
            }

            MeasurementType.BrowPointCount -> {
                obj.addProperty("kind", "BROW_PT")
            }

            MeasurementType.BrowWidth -> {
                obj.addProperty("kind", "BROW_WIDTH")
            }

            MeasurementType.Width -> {
                obj.addProperty("kind", "WIDTH")
            }

            MeasurementType.ProngLength -> {
                obj.addProperty("kind", "PRONG")
            }
        }

        return obj
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): MeasurementType {

        val obj = json.asJsonObject

        return when (obj.get("kind").asString) {

            "G" ->
                MeasurementType.G(obj.get("index").asInt)

            "CROWN" ->
                MeasurementType.G(obj.get("index").asInt)

            "ABNORMAL" ->
                MeasurementType.AbnormalPoint(obj.get("index").asInt)

            "CIRC" ->
                MeasurementType.Circumference(obj.get("index").asInt)

            "LOC" ->
                MeasurementType.Loc(obj.get("index").asInt)

            "MAIN_BEAM" ->
                MeasurementType.MainBeam

            "INNER_SPREAD" ->
                MeasurementType.InnerSpread

            "TIP_SPREAD" ->
                MeasurementType.TipSpread

            "GREATEST_SPREAD" ->
                MeasurementType.GreatestSpread

            "POINT_COUNT" ->
                MeasurementType.PointCount

            "BROW_PT" ->
                MeasurementType.BrowPointCount

            "BROW_WIDTH" ->
                MeasurementType.BrowWidth

            "WIDTH" ->
                MeasurementType.Width

            "PRONG" ->
                MeasurementType.ProngLength

            else ->
                throw IllegalArgumentException("Unknown MeasurementType")
        }
    }
}
