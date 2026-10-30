# Astra Trader

Native Android companion for Standoff 2 marketplace analysis.

## Version 0.1.0
- Kotlin + Jetpack Compose Android application.
- Buy/sell price calculator with configurable 20% default fee.
- Net proceeds, profit and ROI calculation.
- GitHub Actions builds a debug APK artifact.

## Current limitations
- Prices are entered manually; no live market feed yet.
- No game login, credentials, or account access.
- No automatic purchases or sales. This first milestone is analysis-only.
- Software cannot generate gold from a zero balance; a legitimate in-game earning path is required.
- Market automation can put the game account at risk.

## Build
The Android APK is published as a workflow artifact after a push or manual workflow run. Package: com.astratrade.app.