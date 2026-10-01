# Blockfy

[English](README.md) · [Português](README.pt.md)

Less Reels. More focus.

Free Android app, open source. No ads and no subscription.

This is a fork of [Blokky](https://github.com/Ronjar/Blokky) by Robin Gebert. [Samuel Bueno](https://github.com/buenotty) maintains it.

## What it is

Blockfy gets you off short video (Reels, Shorts, TikTok) and can keep you off adult sites. It works through Android's Accessibility service, only for Instagram, YouTube, TikTok, Facebook, X and web browsers. It never watches banking apps, messages or what you type in other apps. See [PRIVACY.md](PRIVACY.md).

If the phone is from Brazil, the app starts in Portuguese. Everywhere else it starts in English. You can change that in the Settings tab.

## What it does

- **One rule per app.** Pick what to block (only Reels/Shorts, or the whole app), how (block it, or allow a daily limit in minutes), and when (hours and weekdays). A plain-language summary at the top of the editor says exactly what will happen.
- **Daily limits** reset at midnight and are counted live, so a limit is enforced as soon as you reach it.
- **Adult site shield**, if you turn it on: Accessibility reads only the browser's address bar and compares it with a list of adult sites on the phone. Nothing is stored or sent. No VPN. Hosts match as whole names, so `jerome` is not treated as `erome`.
- **Strict mode** until midnight: you can still make blocks stricter, but not turn them off or loosen them. Outside strict mode, loosening a block makes you wait a few seconds and confirm.
- **Concepts** tab: short readings on why these blocks exist, for the day you want to turn them off.
- **Diagnostics** (Settings tab): shows whether the service is connected and what it last saw, so a block that does not fire can be debugged.

Updates are meant to come from Google Play. The app does not install APKs itself. Publishing guide (Portuguese): [docs/PUBLICAR_NA_PLAY.md](docs/PUBLICAR_NA_PLAY.md).

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
