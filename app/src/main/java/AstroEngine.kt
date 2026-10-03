package com.astro.sentiment

import kotlin.math.abs

enum class SentimentState(val value: Int) {
    BULLISH(1),   // Green (+1)
    NEUTRAL(0),   // Neutral (0)
    BEARISH(-1)   // Red (-1)
}

data class AspectInfo(
    val aspectingPlanet: String,
    val targetPlanet: String,
    val angle: Double,
    val orb: Double,
    val aspectType: String,
    val isHardAspect: Boolean,
    val cjValue: Double,
    val polarity: Int,
    val pmValue: Double
)

class AstroEngine {

    // Planet Base Weights (Wp)
    private val planetWeights = mapOf(
        "Pluto" to 10.0,
        "Mercury" to 9.0,
        "Saturn" to 8.0,
        "Mars" to 7.0,
        "Venus" to 6.0,
        "Moon" to 5.0,
        "Jupiter" to 4.0,
        "Sun" to 3.0,
        "Uranus" to 2.0,
        "Neptune" to 1.0
    )

    // Base Sentiment States (Sp)
    private val planetSentiments = mapOf(
        "Venus" to SentimentState.BULLISH,
        "Jupiter" to SentimentState.BULLISH,
        "Neptune" to SentimentState.BULLISH,
        "Pluto" to SentimentState.BEARISH,
        "Saturn" to SentimentState.BEARISH,
        "Mercury" to SentimentState.NEUTRAL,
        "Mars" to SentimentState.NEUTRAL,
        "Moon" to SentimentState.NEUTRAL,
        "Sun" to SentimentState.NEUTRAL,
        "Uranus" to SentimentState.NEUTRAL
    )

    // Sign Dignity Scale (Sd)
    private val dignityValues = mapOf(
        "DOMICILE_EXALTATION" to 1.125,
        "NEUTRAL" to 1.000,
        "FALL_DETRIMENT" to 0.875
    )

    /**
     * Retrieves Cj coefficient based on aspect angle and aspecting planet sentiment state.
     */
    fun getCjValue(angle: Double, sentiment: SentimentState): Double {
        val roundedAngle = kotlin.math.round(angle).toInt()
        return when (sentiment) {
            SentimentState.BULLISH -> when (roundedAngle) {
                90 -> 0.375
                60 -> 0.750
                45 -> 0.625
                0, 30, 120 -> 0.500
                72, 135, 150 -> 0.250
                144, 180 -> 0.125
                else -> 0.0
            }
            SentimentState.NEUTRAL -> when (roundedAngle) {
                90 -> 0.875
                60 -> 0.750
                45 -> 0.625
                0, 30, 120 -> 0.500
                135 -> 0.375
                72, 150 -> 0.250
                144, 180 -> 0.125
                else -> 0.0
            }
            SentimentState.BEARISH -> when (roundedAngle) {
                90 -> 0.875
                45 -> 0.750
                30 -> 0.625
                0, 135 -> 0.500
                60, 150 -> 0.375
                72, 120, 180 -> 0.250
                144 -> 0.125
                else -> 0.0
            }
        }
    }

    /**
     * Determines aspecting planet polarity, applying the refined Ascendant rules.
     */
    fun determinePolarity(
        aspectingPlanet: String,
        targetPoint: String,
        angle: Double,
        isHardAspect: Boolean,
        hasNegativeAspects: Boolean
    ): Int {
        val baseSentiment = planetSentiments[aspectingPlanet] ?: SentimentState.NEUTRAL

        if (baseSentiment != SentimentState.NEUTRAL) {
            return baseSentiment.value
        }

        val roundedAngle = kotlin.math.round(angle).toInt()

        // Rules specifically targeting the Ascendant
        if (targetPoint.equals("Ascendant", ignoreCase = true)) {
            // Rule 1: Tainted neutral logic restricted exclusively to 72° and 144° aspects to Ascendant
            if (roundedAngle == 72 || roundedAngle == 144) {
                return if (hasNegativeAspects) -1 else 1
            }

            // Exception: 45° aspect is excluded from standard hard-aspect red polarity
            if (roundedAngle == 45) {
                return 1 // Preserved as non-red exception
            }

            // Rule 2: Hard aspects -> Red (-1), Soft aspects -> Green (+1)
            return if (isHardAspect) -1 else 1
        }

        // Default neutral behavior for other targets
        return if (isHardAspect) -1 else 1
    }

    /**
     * Computes the Dynamic Weight for a target planet.
     *
     * Dynamic Weight = (Target Weight * Sign Dignity) + Sum(Pm / 10)
     * Where Pm = Aspecting Weight * Cj * Polarity
     */
    fun calculateDynamicWeight(
        targetPlanet: String,
        targetDignityStatus: String, // "DOMICILE_EXALTATION", "NEUTRAL", or "FALL_DETRIMENT"
        incomingAspects: List<AspectInfo>
    ): Double {
        val wpTarget = planetWeights[targetPlanet] ?: 1.0
        val sdTarget = dignityValues[targetDignityStatus] ?: 1.000

        val baseDignifiedWeight = wpTarget * sdTarget

        val sumPmOverTen = incomingAspects.sumOf { aspect ->
            (aspect.pmValue) / 10.0
        }

        return baseDignifiedWeight + sumPmOverTen
    }

    /**
     * Helper to build AspectInfo structure with calculated Pm value.
     */
    fun createAspectInfo(
        aspectingPlanet: String,
        targetPoint: String,
        angle: Double,
        orb: Double,
        isHardAspect: Boolean,
        hasNegativeAspects: Boolean
    ): AspectInfo {
        val wpAspecting = planetWeights[aspectingPlanet] ?: 1.0
        val sentiment = planetSentiments[aspectingPlanet] ?: SentimentState.NEUTRAL
        val cj = getCjValue(angle, sentiment)
        val polarity = determinePolarity(aspectingPlanet, targetPoint, angle, isHardAspect, hasNegativeAspects)

        // Pm = Weight of aspecting planet * Cj * Polarity
        val pm = wpAspecting * cj * polarity

        return AspectInfo(
            aspectingPlanet = aspectingPlanet,
            targetPlanet = targetPoint,
            angle = angle,
            orb = orb,
            aspectType = "${kotlin.math.round(angle).toInt()}°",
            isHardAspect = isHardAspect,
            cjValue = cj,
            polarity = polarity,
            pmValue = pm
        )
    }
}
