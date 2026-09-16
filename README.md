# Blockfy

[English](README.md) · [Português](README.pt.md)

**Less Reels. More focus. / Menos Reels. Mais foco.**

Free, open-source Android app. No ads, no subscription.

Fork of [Blokky](https://github.com/Ronjar/Blokky) by Robin Gebert. Maintained by [Samuel Bueno](https://github.com/buenotty).

---

## Summary

Blockfy helps you get off short-video loops (Reels, Shorts, TikTok) and, if you want, block known adult sites on the phone. Accessibility only watches Instagram, YouTube, TikTok, Facebook and X — not banking apps. The adult shield is an on-device DNS filter. The app language defaults to English; Portuguese is in Settings.

## Resumo

O Blockfy ajuda você a sair do loop de vídeos curtos (Reels, Shorts, TikTok) e, se quiser, bloquear sites adultos conhecidos no próprio celular. A acessibilidade só observa Instagram, YouTube, TikTok, Facebook e X — não apps de banco. O escudo adulto é um filtro DNS no aparelho. O idioma padrão é inglês; português fica em Configurações.

---

## What it does

- **Reels / Shorts blocker** — Accessibility is limited to Instagram, YouTube, TikTok, Facebook and X. It does not read other apps, including banks.
- **Daily limits** — Optional per-app quotas that reset at midnight.
- **Adult site shield** — Optional local DNS VPN. Bank apps stay on your normal connection. Sites are matched as whole names, so a word like `jerome` is not treated as `erome`.
- **Language** — Default is English. Switch to Portuguese in Settings.
- **Updates** — Meant to come from Google Play. The app does not install APKs itself.

## Requirements

Android 9 or newer.

## Credits

- **Samuel Bueno** ([@buenotty](https://github.com/buenotty)) — Blockfy maintainer
- **Robin Gebert** ([@Ronjar](https://github.com/Ronjar)) — original Blokky

MIT license. See [LICENSE](LICENSE).

## Signing (for developers)

The Play upload keystore is **not** in this repository. Copy `keystore.properties.example` to `secrets/keystore.properties` and point it at your PKCS12 file. Never commit `*.p12`, `*.jks`, `*.keystore`, or `secrets/`.
