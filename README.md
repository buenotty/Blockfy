# Blockfy

[English](README.md) · [Português](README.pt.md)

Less Reels. More focus.

Free Android app, open source. No ads and no subscription.

This is a fork of [Blokky](https://github.com/Ronjar/Blokky) by Robin Gebert. [Samuel Bueno](https://github.com/buenotty) maintains it.

## What it is

Blockfy gets you off short video (Reels, Shorts, TikTok). You can also block known adult sites on the phone. Accessibility only watches Instagram, YouTube, TikTok, Facebook, and X. It does not watch banking apps. The adult shield is a DNS filter that stays on the device.

If the phone is from Brazil, the app starts in Portuguese. Everywhere else it starts in English. You can change that under the gear on the About screen.

## What it does

Reels and Shorts: accessibility is limited to Instagram, YouTube, TikTok, Facebook, and X. Other apps, including banks, are left alone.

Daily limits: per-app quotas, if you turn them on. They reset at midnight.

Adult site shield: a local DNS VPN, if you turn it on. Bank apps stay on your normal connection. Hosts are matched as whole names, so `jerome` is not treated as `erome`.

Updates are meant to come from Google Play. The app does not install APKs itself.

## Requirements

Android 9 or newer.

## Credits

Samuel Bueno ([@buenotty](https://github.com/buenotty)) keeps Blockfy going.

Robin Gebert ([@Ronjar](https://github.com/Ronjar)) made the original Blokky.

MIT. See [LICENSE](LICENSE).

## Signing (for developers)

The Play upload keystore is not in this repo. Copy `keystore.properties.example` to `secrets/keystore.properties` and point it at your PKCS12 file. Do not commit `*.p12`, `*.jks`, `*.keystore`, or `secrets/`.
