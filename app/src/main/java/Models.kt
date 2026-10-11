package com.ascendant.sentiment.model

data class PlanetDef(
    val name: String,
    val glyph: String,
    val weight: Int,
    val defaultPolarity: Int // 1 = positive, -1 = negative, 0 = contextual
)

// v3.4: hard aspects (same set as the engine's HARD_ANGLES) used for the Pluto highlight
private val PLUTO_HARD_ANGLES = setOf(45, 90, 135, 150, 180)

data class AspectInfo(
    val planetName: String,
    val glyph: String,
    val weight: Int,
    val aspectAngle: Int,
    val orbDiff: Double,
    val polarity: Int,
    val coeff: Double,
    val dynWeight: Double = weight.toDouble() // v3.0: dynamic weight (Wp x Sd + Pm)
) {
    val contribution: Double
        get() = polarity * dynWeight * coeff

    /** v3.4: a hard aspect (45, 90, 135, 150, 180) made by Pluto. */
    val isHardPluto: Boolean
        get() = planetName == "Pluto" && aspectAngle in PLUTO_HARD_ANGLES
}

data class SentimentBar(
    val timeMs: Long,
    val ascDegree: Double,
    val planetaryLongitudes: DoubleArray,
    val aspects: List<AspectInfo>,
    val sentimentScore: Double,
    // v3.2: Midheaven vs the fixed birth chart points
    val mcDegree: Double = 0.0,
    val mcAspects: List<AspectInfo> = emptyList(),
    val mcScore: Double = 0.0
) {
    /** v3.4: any hard Pluto aspect to the Ascendant on this bar. */
    val hasHardPluto: Boolean
        get() = aspects.any { it.isHardPluto }

    /** v3.4: any hard Pluto aspect between the Midheaven and natal Pluto on this bar. */
    val mcHasHardPluto: Boolean
        get() = mcAspects.any { it.isHardPluto }
}

data class HitEvent(
    val timeMs: Long,
    val planetName: String,
    val glyph: String,
    val weight: Int,
    val aspectAngle: Int,
    val polarity: Int,
    val coeff: Double
)

data class GeoLocation(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val market: String
)
