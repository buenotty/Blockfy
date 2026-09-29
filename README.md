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

Adult content shield, if you turn it on: Accessibility reads on-screen and typed text in browsers, X, Telegram and Reddit, checks it on the phone and never stores or sends it. No VPN. Bank apps are never watched. See [PRIVACY.md](PRIVACY.md). Hosts are matched as whole names, so `jerome` is not treated as `erome`.

Updates are meant to come from Google Play. The app does not install APKs itself.

## Requirements

Android 9 or newer.

## Support the project

Blockfy is free, with no ads and no donation prompts inside the app. If it helps you, you can support it here on GitHub:

- Pix (Brazil): `496f008e-c67d-4175-9fad-e6b3c9bbd248`
- PayPal: [paypal.me/donate](https://www.paypal.com/donate/?business=samuellbuenno%40gmail.com&currency_code=BRL) (samuellbuenno@gmail.com)
- Or just star the repo.

## Credits

Samuel Bueno ([@buenotty](https://github.com/buenotty)) keeps Blockfy going.

Robin Gebert ([@Ronjar](https://github.com/Ronjar)) made the original Blokky.

MIT. See [LICENSE](LICENSE).

## Signing (for developers)

The Play upload keystore is not in this repo. Copy `keystore.properties.example` to `secrets/keystore.properties` and point it at your PKCS12 file. Do not commit `*.p12`, `*.jks`, `*.keystore`, or `secrets/`.
