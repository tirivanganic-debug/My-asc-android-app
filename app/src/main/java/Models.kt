package com.ascendant.sentiment.model

data class PlanetDef(
    val name: String,
    val glyph: String,
    val weight: Int,
    val defaultPolarity: Int // 1 = positive, -1 = negative, 0 = contextual
)

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
}

data class SentimentBar(
    val timeMs: Long,
    val ascDegree: Double,
    val planetaryLongitudes: DoubleArray,
    val aspects: List<AspectInfo>,
    val sentimentScore: Double
)

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
