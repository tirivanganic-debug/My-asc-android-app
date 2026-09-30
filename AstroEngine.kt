package com.ascendant.sentiment.engine

import com.ascendant.sentiment.model.*
import kotlin.math.*

object AstroEngine {
    private const val R = Math.PI / 180.0
    private fun rev(x: Double): Double = ((x % 360.0) + 360.0) % 360.0
    private fun sn(x: Double): Double = sin(x * R)
    private fun cs(x: Double): Double = cos(x * R)

    val PLANETS = listOf(
        PlanetDef("Pluto", "♇", 6, -1),
        PlanetDef("Mercury", "☿", 5, 0),
        PlanetDef("Mars", "♂", 4, 1),
        PlanetDef("Moon", "☽", 3, 0),
        PlanetDef("Saturn", "♄", 4, -1),
        PlanetDef("Sun", "☉", 3, 1),
        PlanetDef("Venus", "♀", 4, 1),
        PlanetDef("Uranus", "♅", 3, 0),
        PlanetDef("Jupiter", "♃", 3, 1),
        PlanetDef("Neptune", "♆", 2, 1)
    )

    val ANGLES = listOf(0, 30, 45, 60, 72, 90, 120, 135, 144, 150, 180)
    val NEGATIVE_ANGLES = setOf(45, 90, 135, 150, 180)

    val RED_COEFFS = mapOf(
        0 to 0.50, 45 to 0.50, 90 to 0.62, 135 to 0.50, 150 to 0.50, 180 to 0.24,
        30 to 0.24, 72 to 0.38, 60 to 0.38, 120 to 0.24, 144 to 0.24
    )

    val GREEN_COEFFS = mapOf(
        0 to 0.50, 60 to 0.85, 45 to 0.50, 120 to 0.62, 30 to 0.24, 72 to 0.38,
        144 to 0.24, 90 to 0.38, 135 to 0.24, 150 to 0.24, 180 to 0.24
    )

    private val CONJ_POS = setOf("Moon", "Mercury", "Venus", "Jupiter", "Uranus", "Neptune")
    private val POS_45 = setOf("Moon", "Mercury", "Sun", "Mars", "Uranus")

    private val EL = mapOf(
        "Mercury" to doubleArrayOf(48.3313, 3.24587e-5, 7.0047, 5e-8, 29.1241, 1.01444e-5, 0.387098, 0.0, 0.205635, 5.59e-10, 168.6562, 4.0923344368),
        "Venus" to doubleArrayOf(76.6799, 2.4659e-5, 3.3946, 2.75e-8, 54.891, 1.38374e-5, 0.72333, 0.0, 0.006773, -1.302e-9, 48.0052, 1.6021302244),
        "Mars" to doubleArrayOf(49.5574, 2.11081e-5, 1.8497, -1.78e-8, 286.5016, 2.92961e-5, 1.523688, 0.0, 0.093405, 2.516e-9, 18.6021, 0.5240207766),
        "Jupiter" to doubleArrayOf(100.4542, 2.76854e-5, 1.303, -1.557e-7, 273.8777, 1.64505e-5, 5.20256, 0.0, 0.048498, 4.469e-9, 19.895, 0.0830853001),
        "Saturn" to doubleArrayOf(113.6634, 2.3898e-5, 2.4886, -1.081e-7, 339.3939, 2.97661e-5, 9.55475, 0.0, 0.055546, -9.499e-9, 316.967, 0.0334442282),
        "Uranus" to doubleArrayOf(74.0005, 1.3978e-5, 0.7733, 1.9e-8, 96.6612, 3.0565e-5, 19.18171, -1.55e-8, 0.047318, 7.45e-9, 142.5905, 0.011725806),
        "Neptune" to doubleArrayOf(131.7806, 3.0173e-5, 1.77, -2.55e-7, 272.8461, -6.027e-6, 30.05826, 3.313e-8, 0.008606, 2.15e-9, 260.2471, 0.005995147),
        "Moon" to doubleArrayOf(125.1228, -0.0529538083, 5.1454, 0.0, 318.0634, 0.1643573223, 60.2666, 0.0, 0.0549, 0.0, 115.3654, 13.0649929509)
    )

    private data class OrbitResult(
        val x: Double, val y: Double, val z: Double,
        val r: Double, val M: Double, val w: Double, val N: Double
    )

    private fun orb(name: String, d: Double): OrbitResult {
        val e = EL[name] ?: error("Missing elements for $name")
        val N = e[0] + e[1] * d
        val i = e[2] + e[3] * d
        val w = e[4] + e[5] * d
        val a = e[6] + e[7] * d
        val ec = e[8] + e[9] * d
        val M = rev(e[10] + e[11] * d)
        var E = M + ec / R * sn(M) * (1.0 + ec * cs(M))
        for (k in 0..3) {
            E -= (E - ec / R * sn(E) - M) / (1.0 - ec * cs(E))
        }
        val xv = a * (cs(E) - ec)
        val yv = a * sqrt(1.0 - ec * ec) * sn(E)
        val v = atan2(yv, xv) / R
        val r = hypot(xv, yv)
        val u = v + w
        return OrbitResult(
            x = r * (cs(N) * cs(u) - sn(N) * sn(u) * cs(i)),
            y = r * (sn(N) * cs(u) + cs(N) * sn(u) * cs(i)),
            z = r * sn(u) * sn(i),
            r = r,
            M = M,
            w = w,
            N = N
        )
    }

    private val cache = mutableMapOf<Long, DoubleArray>()

    fun calculatePlanetaryPositions(ms: Long): DoubleArray {
        cache[ms]?.let { return it }
        if (cache.size > 4000) cache.clear()

        val d = ms / 864e5 + 2440587.5 - 2451543.5
        val L = mutableMapOf<String, Double>()

        val sw = 282.9404 + 4.70935e-5 * d
        val se = 0.016709 - 1.151e-9 * d
        val sM = rev(356.047 + 0.9856002585 * d)
        val sE = sM + se / R * sn(sM) * (1.0 + se * cs(sM))
        val sx = cs(sE) - se
        val sy = sqrt(1.0 - se * se) * sn(sE)
        val sl = atan2(sy, sx) / R + sw
        val sr = hypot(sx, sy)
        val xs = sr * cs(sl)
        val ys = sr * sn(sl)
        L["Sun"] = rev(sl)

        val m = orb("Moon", d)
        val Lm = m.M + m.w + m.N
        val Ls = sM + sw
        val D = Lm - Ls
        val F = Lm - m.N
        val Mm = m.M
        L["Moon"] = rev(
            atan2(m.y, m.x) / R - 1.274 * sn(Mm - 2 * D) + 0.658 * sn(2 * D) -
                    0.186 * sn(sM) - 0.059 * sn(2 * Mm - 2 * D) - 0.057 * sn(Mm - 2 * D + sM) +
                    0.053 * sn(Mm + 2 * D) + 0.046 * sn(2 * D - sM) + 0.041 * sn(Mm - sM) -
                    0.035 * sn(D) - 0.031 * sn(Mm + sM) - 0.015 * sn(2 * F - 2 * D) + 0.011 * sn(Mm - 4 * D)
        )

        val O = mutableMapOf<String, OrbitResult>()
        listOf("Mercury", "Venus", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune").forEach { n ->
            O[n] = orb(n, d)
        }

        val Mj = O["Jupiter"]!!.M
        val Ms = O["Saturn"]!!.M
        val Mu = O["Uranus"]!!.M

        val P = mapOf(
            "Jupiter" to -0.332 * sn(2 * Mj - 5 * Ms - 67.6) - 0.056 * sn(2 * Mj - 2 * Ms + 21) +
                    0.042 * sn(3 * Mj - 5 * Ms + 21) - 0.036 * sn(Mj - 2 * Ms) + 0.022 * cs(Mj - Ms) +
                    0.023 * sn(2 * Mj - 3 * Ms + 52) - 0.016 * sn(Mj - 5 * Ms - 69),
            "Saturn" to 0.812 * sn(2 * Mj - 5 * Ms - 67.6) - 0.229 * cs(2 * Mj - 4 * Ms - 2) +
                    0.119 * sn(Mj - 2 * Ms - 3) + 0.046 * sn(2 * Mj - 6 * Ms - 69) + 0.014 * sn(Mj - 3 * Ms + 32),
            "Uranus" to 0.04 * sn(Ms - 2 * Mu + 6) + 0.035 * sn(Ms - 3 * Mu + 33) - 0.015 * sn(Mj - Mu + 20)
        )

        for ((n, o) in O) {
            val lon = atan2(o.y, o.x) / R + (P[n] ?: 0.0)
            val lat = atan2(o.z, hypot(o.x, o.y)) / R
            L[n] = rev(atan2(o.r * cs(lat) * sn(lon) + ys, o.r * cs(lat) * cs(lon) + xs) / R)
        }

        val Sx = 50.03 + 0.033459652 * d
        val Pp = 238.95 + 0.003968789 * d
        val pl = 238.9508 + 0.00400703 * d - 19.799 * sn(Pp) + 19.848 * cs(Pp) + 0.897 * sn(2 * Pp) -
                4.956 * cs(2 * Pp) + 0.61 * sn(3 * Pp) + 1.211 * cs(3 * Pp) - 0.341 * sn(4 * Pp) -
                0.19 * cs(4 * Pp) + 0.128 * sn(5 * Pp) - 0.034 * cs(5 * Pp) - 0.038 * sn(6 * Pp) +
                0.031 * cs(6 * Pp) + 0.02 * sn(Sx - Pp) - 0.01 * cs(Sx - Pp)
        val pb = -5.452 * sn(Pp) - 14.974 * cs(Pp) + 3.527 * sn(2 * Pp) + 1.673 * cs(2 * Pp) -
                1.051 * sn(3 * Pp) + 0.328 * cs(3 * Pp) + 0.179 * sn(4 * Pp) - 0.292 * cs(4 * Pp) +
                0.019 * sn(5 * Pp) + 0.1 * cs(5 * Pp) - 0.031 * sn(6 * Pp) - 0.026 * cs(6 * Pp) + 0.011 * cs(Sx - Pp)
        val pr = 40.72 + 6.68 * sn(Pp) + 6.9 * cs(Pp) - 1.18 * sn(2 * Pp) - 0.03 * cs(2 * Pp) +
                0.15 * sn(3 * Pp) - 0.14 * cs(3 * Pp)
        L["Pluto"] = rev(atan2(pr * cs(pb) * sn(pl) + ys, pr * cs(pb) * cs(pl) + xs) / R)

        val result = DoubleArray(PLANETS.size) { i -> L[PLANETS[i].name] ?: 0.0 }
        cache[ms] = result
        return result
    }

    fun calculateAscendant(ms: Long, lat: Double, lon: Double): Double {
        val jd = ms / 864e5 + 2440587.5
        val dd = jd - 2451545.0
        val T = dd / 36525.0
        val th = rev(280.46061837 + 360.98564736629 * dd + 3.87933e-4 * T * T + lon)
        val eps = 23.4393 - 3.563e-7 * (jd - 2451543.5)
        return rev(atan2(cs(th), -(sn(th) * cs(eps) + tan(lat * R) * sn(eps))) / R)
    }

    fun sep(a: Double, b: Double): Double = abs(((a - b + 540.0) % 360.0) - 180.0)

    fun getPolarity(name: String, aspectAngle: Int, defaultPol: Int): Int {
        if (aspectAngle == 0) return if (CONJ_POS.contains(name)) 1 else -1
        if (aspectAngle == 45 && POS_45.contains(name)) return 1
        if ((name == "Mars" || name == "Sun") && NEGATIVE_ANGLES.contains(aspectAngle)) return -1
        return if (defaultPol != 0) defaultPol else if (NEGATIVE_ANGLES.contains(aspectAngle)) -1 else 1
    }

    fun calculateAspects(asDeg: Double, planetLongs: DoubleArray, orbLimit: Double = 3.0): List<AspectInfo> {
        val list = mutableListOf<AspectInfo>()
        PLANETS.forEachIndexed { k, planet ->
            val sp = sep(asDeg, planetLongs[k])
            ANGLES.forEach { A ->
                val d = abs(sp - A)
                if (d <= orbLimit) {
                    val s = getPolarity(planet.name, A, planet.defaultPolarity)
                    var c = (if (s < 0) RED_COEFFS else GREEN_COEFFS)[A] ?: 0.24
                    if (planet.defaultPolarity == 0 && A == 150) c = 0.38
                    list.add(
                        AspectInfo(
                            planetName = planet.name,
                            glyph = planet.glyph,
                            weight = planet.weight,
                            aspectAngle = A,
                            orbDiff = d,
                            polarity = s,
                            coeff = c
                        )
                    )
                }
            }
        }
        return list
    }

    fun calculateScore(aspects: List<AspectInfo>): Double {
        if (aspects.isEmpty()) return 0.0
        var num = 0.0
        var den = 0.0
        aspects.forEach {
            num += it.polarity * it.weight * it.coeff
            den += it.weight
        }
        return if (den == 0.0) 0.0 else num / den
    }

    fun generateSeries(
        centerMs: Long,
        windowHours: Int,
        lat: Double,
        lon: Double,
        orbLimit: Double = 3.0
    ): List<SentimentBar> {
        val st = 300_000L // 5 minutes
        val halfWindow = windowHours * 1_800_000L
        val t0 = ((centerMs - halfWindow + st / 2) / st) * st
        val count = windowHours * 12
        val list = ArrayList<SentimentBar>(count + 1)

        for (i in 0..count) {
            val t = t0 + i * st
            val longs = calculatePlanetaryPositions(t)
            val asc = calculateAscendant(t, lat, lon)
            val h = calculateAspects(asc, longs, orbLimit)
            val s = calculateScore(h)
            list.add(SentimentBar(t, asc, longs, h, s))
        }
        return list
    }

    fun calculateExactHits(series: List<SentimentBar>, orbLimit: Double = 3.0): List<HitEvent> {
        val ev = mutableListOf<HitEvent>()
        for (i in 0 until series.size - 1) {
            val a = series[i]
            val b = series[i + 1]
            PLANETS.forEachIndexed { k, planet ->
                val sa = sep(a.ascDegree, a.planetaryLongitudes[k])
                val sb = sep(b.ascDegree, b.planetaryLongitudes[k])
                ANGLES.forEach { A ->
                    val fa = sa - A
                    val fb = sb - A
                    if (fa * fb < 0 && abs(fa) < 8 && abs(fb) < 8) {
                        val s = getPolarity(planet.name, A, planet.defaultPolarity)
                        var c = (if (s < 0) RED_COEFFS else GREEN_COEFFS)[A] ?: 0.24
                        if (planet.defaultPolarity == 0 && A == 150) c = 0.38
                        val exactT = (a.timeMs + (fa / (fa - fb)) * 300_000).toLong()
                        ev.add(HitEvent(exactT, planet.name, planet.glyph, planet.weight, A, s, c))
                    }
                }
            }
        }
        return ev.sortedBy { it.timeMs }
    }

    val ZODIAC = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces")
    val ZODIAC_GLYPHS = listOf("♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓")

    fun formatDMS(deg: Double): String {
        val signIdx = (floor(deg / 30.0).toInt()) % 12
        val inSign = deg % 30.0
        val d = floor(inSign).toInt()
        val m = floor((inSign - d) * 60.0).toInt()
        return "%d°%02d′ %s %s".format(d, m, ZODIAC[signIdx], ZODIAC_GLYPHS[signIdx])
    }

    val PRESET_LOCATIONS = listOf(
        GeoLocation("london", "London (Brent Hub)", 51.5074, -0.1278, "ICE Brent"),
        GeoLocation("newyork", "New York (WTI Hub)", 40.7128, -74.006, "NYMEX WTI"),
        GeoLocation("harare", "Harare (CAT Reference)", -17.8292, 31.0522, "Southern Africa"),
        GeoLocation("dubai", "Dubai (DME Oman)", 25.2048, 55.2708, "Middle East"),
        GeoLocation("singapore", "Singapore (Asia Brent)", 1.3521, 103.8198, "Asia Pacific"),
        GeoLocation("tokyo", "Tokyo (TOCOM)", 35.6762, 139.6503, "East Asia")
    )
}
