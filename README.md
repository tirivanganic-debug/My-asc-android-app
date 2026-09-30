# Ascendant Sentiment Engine - Native Android Application

This directory contains the **genuine native Android application** for the Ascendant Sentiment Engine, built with **Kotlin** and **Jetpack Compose**.

## Architecture & Features

- **Pure Kotlin Astrological Engine (`com.ascendant.sentiment.engine.AstroEngine`)**:
  - Implements Meeus & Schlyter planetary ephemeris calculations for Sun, Moon, Mercury, Venus, Mars, Jupiter, Saturn, Uranus, Neptune, and Pluto.
  - Computes geocentric ecliptic longitudes, Ascendant degree for arbitrary latitude/longitude, and angular aspects.
  - Evaluates sentiment score $S = \frac{\sum (S_p \cdot W_p \cdot C_j)}{\sum W_p}$ over active aspects within the configured orb.
  - Zero-crossing sub-minute interpolation for exact Ascendant hits.
  - **100% Offline**: Requires zero network calls or external APIs.
- **Jetpack Compose UI**:
  - `SentimentChart`: Custom hardware-accelerated Compose `Canvas` with touch dragging & tap scrubbing, bull/bear background bands, and real-time "Now" cursor.
  - `SentimentGaugeCard`: Big numeric $S$ readout, animated Bullish/Bearish/Neutral pill badges, and zodiac degree formatting.
  - `ActiveAspectsCard`: Real-time aspect breakdown with planetary glyphs and net contributions.
  - `ExactHitsCard`: Event crossing timeline with countdown/past indicators.
- **Native Android Hardware Integration**:
  - Tactile haptic feedback via Android `Vibrator` / `VibratorManager` on scrubbing and aspect crossings.
  - Edge-to-edge system bar integration (`enableEdgeToEdge`).

## How to Build the Native Android APK

### Option 1: Open in Android Studio
1. Open **Android Studio** (Koala, Ladybug, Meerkat, or newer).
2. Select **Open** and choose the `android` folder in this repository.
3. Allow Gradle to sync.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Transfer the generated `app-debug.apk` or `app-release.apk` directly to your Android device via USB, ADB, or Google Drive, and tap to install.

### Option 2: Command Line (Gradle)
```bash
cd android
./gradlew assembleDebug
```
The compiled APK will be located at:
```
android/app/build/outputs/apk/debug/app-debug.apk
```
Install it to a connected phone using ADB:
```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
```
