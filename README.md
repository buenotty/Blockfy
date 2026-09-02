# Blockfy (Block For You) - Stop Doomscrolling

Blockfy uses the Android Accessibility Service to help you stop doomscrolling on short-form video platforms like **Instagram Reels** and **YouTube Shorts**.

> Fork maintained and enhanced by [buenotty](https://github.com/buenotty/Blokky), based on the original project by Robin Gebert.

### Features
- **Instagram Reels Blocker**: Fast detection and redirection to Feed, fixing the reload loop bug.
- **YouTube Shorts Blocker**: Instant redirect to home feed.
- **Daily Usage Limits**: Set maximum daily usage (e.g. 15 minutes of Reels/Shorts per day) before blocking kicks in.
- **Schedule Time Intervals**: Choose specific hours of the day to block or allow reels.
- **100% Offline & Private**: No internet permission required. Your data never leaves your device.

## Enabling Accessibility Service via ADB (if blocked by vendor)
Some vendors (like Xiaomi / MIUI) restrict permissions based on installation source. Run this ADB command:
```bash
adb shell settings put secure enabled_accessibility_services com.buenotty.blockfy/com.robingebert.blokky.feature_accessibility.ReelsBlockAccessibilityService
```


