# Blockfy

Less Reels. More focus.

Blockfy is a free, open-source Android app that helps you leave short-video feeds and, optionally, block known adult sites on the device. It is a fork of [Blokky](https://github.com/Ronjar/Blokky) by Robin Gebert.

The default language is **English**. Portuguese is available in Settings.

## What it does

- **Reels / Shorts blocker** — Accessibility is limited to Instagram, YouTube, TikTok, Facebook and X. It walks the event source of those apps only. It does not read the active window of other apps, including banks.
- **Daily limits** — Optional per-app quotas that reset at midnight.
- **Adult site shield** — Optional local DNS VPN. Banking apps are excluded. Host matching uses whole labels, so a name like `jerome` is not treated as `erome`.
- **No in-app APK installs** — Updates are meant to come from Google Play. `REQUEST_INSTALL_PACKAGES` is not used.

## Requirements

Android 9 (API 28) or newer.

## Credits

- **Samuel Bueno** ([@buenotty](https://github.com/buenotty)) — Blockfy maintainer
- **Robin Gebert** ([@Ronjar](https://github.com/Ronjar)) — original Blokky

This project stays under the original MIT license. See [LICENSE](LICENSE).

## Signing (Play upload key)

The Play upload keystore is **not** in git. Copy `keystore.properties.example` to `secrets/keystore.properties` and point it at your PKCS12 file.

GitHub Actions release signing uses these repository secrets:

- `BLOCKFY_STORE_FILE_BASE64`
- `BLOCKFY_STORE_PASSWORD`
- `BLOCKFY_KEY_ALIAS`
- `BLOCKFY_KEY_PASSWORD`

Never commit `*.p12`, `*.jks`, `*.keystore`, or `secrets/`.
