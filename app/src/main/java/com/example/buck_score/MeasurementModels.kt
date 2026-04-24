package com.example.buck_score

enum class BuckType {
    TYPICAL,
    NONTYPICAL
}

enum class Species(val displayName: String) {
    WHITETAIL("Whitetail Deer"),
    COUES("Coues Deer"),
    SITKA_BLACKTAIL("Sitka Blacktail Deer"),
    COLUMBIA_BLACKTAIL("Colombia Blacktail Deer"),
    MULE_DEER("Mule Deer"),

    ROCKY_MOUNTAIN_ELK("Rocky Mountain Elk"),
    ROOSEVELT_ELK("Roosevelt Elk"),
    TULE_ELK("Tule Elk"),

    YUKON_MOOSE("Alaska-Yukon Moose"),
    CANADA_MOOSE("Canada Moose"),
    SHIRAS_MOOSE("Shiras Moose"),

    BIGHORN_SHEEP("Bighorn Sheep"),
    DALL_SHEEP("Dall Sheep"),
    DESERT_SHEEP("Desert Sheep"),
    STONE_SHEEP("Stone Sheep"),

    BARREN_GROUND_CARIBOU("Barren Ground Caribou"),
    CC_BARREN_GROUND_CARIBOU("Central Canada Barren Ground Caribou"),
    MOUNTAIN_CARIBOU("Mountain Caribou"),
    QUEBEC_LABRADOR_CARIBOU("Quebec-Labrador Caribou"),
    WOODLAND_CARIBOU("Woodland Caribou"),

    MOUNTAIN_GOAT("Mountain Goat"),
    BISON("Bison"),
    MUSK_OX("Musk Ox"),
    PRONGHORN("Pronghorn")
}

data class PairedMeasurement(
    val type: MeasurementType,
    val left: Double,
    val right: Double
) {
    fun difference(): Double = kotlin.math.abs(left - right)
    fun sum(): Double = left + right
}

sealed class MeasurementType {
    data class G(val index: Int) : MeasurementType()
    data class AbnormalPoint(val index: Int) : MeasurementType()
    object MainBeam : MeasurementType()
    data class Circumference(val index: Int) : MeasurementType()
    object InnerSpread : MeasurementType()
    object TipSpread : MeasurementType()
    object GreatestSpread : MeasurementType()
    object PointCount : MeasurementType()
    object BrowPointCount : MeasurementType()
    object Width: MeasurementType()
    object BrowWidth: MeasurementType()
    object ProngLength: MeasurementType()
    data class CrownPoint(val index: Int) : MeasurementType()
    data class Loc(val index: Int) : MeasurementType()
}

enum class Side { LEFT, RIGHT }

data class ScoreBreakdown(
    val leftSum: Double,
    val rightSum: Double,
    val differenceTotal: Double,
    val abnormalSum: Double,
    val spreadCredit: Double,
    val subtotal: Double,
    val crownPointScore: Double,
    val gross: Double,
    val finalScore: Double,
    val spreadIsCapped: Boolean = false
)

data class MeasurementValue(
    val type: MeasurementType,
    val side: Side?,
    val value: Double
)


class MeasurementStore {

    private val values = mutableListOf<MeasurementValue>()

    fun update(type: MeasurementType, side: Side?, value: Double) {
        val newVal = MeasurementValue(
            type,
            side,
            value
        )

        values.removeAll { it.type == newVal.type && it.side == newVal.side }
        values.add(newVal)
    }

    fun get(type: MeasurementType, side: Side? = null): Double {
        return values.find { it.type == type && it.side == side }?.value ?: 0.0
    }

    fun getPaired(type: MeasurementType): PairedMeasurement {
        val left = get(type, Side.LEFT)
        val right = get(type, Side.RIGHT)
        return PairedMeasurement(type, left, right)
    }

    fun getPairedList(
        predicate: (MeasurementType) -> Boolean
    ): List<PairedMeasurement> =
        values
            .mapNotNull { it.type.takeIf(predicate) }
            .distinctBy { it }
            .sortedBy {
                when (it) {
                    is MeasurementType.Circumference -> it.index
                    is MeasurementType.G -> it.index
                    is MeasurementType.AbnormalPoint -> it.index
                    is MeasurementType.CrownPoint -> it.index
                    else -> 0
                }
            }
            .map { getPaired(it) }

    fun getAll(): List<MeasurementValue> = values
    fun clear(){
        values.clear()
    }
}
