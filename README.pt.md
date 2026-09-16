# Blockfy

[English](README.md) · [Português](README.pt.md)

**Menos Reels. Mais foco. / Less Reels. More focus.**

Aplicativo Android gratuito e de código aberto. Sem anúncios, sem assinatura.

Fork do [Blokky](https://github.com/Ronjar/Blokky), de Robin Gebert. Mantido por [Samuel Bueno](https://github.com/buenotty).

---

## Resumo

O Blockfy ajuda você a sair do loop de vídeos curtos (Reels, Shorts, TikTok) e, se quiser, bloquear sites adultos conhecidos no próprio celular. A acessibilidade só observa Instagram, YouTube, TikTok, Facebook e X — não apps de banco. O escudo adulto é um filtro DNS no aparelho. O idioma padrão é inglês; português fica em Configurações.

## Summary

Blockfy helps you get off short-video loops (Reels, Shorts, TikTok) and, if you want, block known adult sites on the phone. Accessibility only watches Instagram, YouTube, TikTok, Facebook and X — not banking apps. The adult shield is an on-device DNS filter. The app language defaults to English; Portuguese is in Settings.

---

## O que ele faz

- **Bloqueio de Reels / Shorts** — A acessibilidade fica limitada a Instagram, YouTube, TikTok, Facebook e X. Não lê outros apps, inclusive bancos.
- **Limites diários** — Cotas opcionais por aplicativo, que zeram à meia-noite.
- **Escudo de sites adultos** — VPN DNS local opcional. Apps de banco seguem na conexão normal. Os sites são reconhecidos pelo nome inteiro, então uma palavra como `jerome` não é tratada como `erome`.
- **Idioma** — O padrão é inglês. Português se escolhe em Configurações.
- **Atualizações** — Pensadas para vir da Google Play. O app não instala APKs sozinho.

## Requisitos

Android 9 ou mais novo.

## Créditos

- **Samuel Bueno** ([@buenotty](https://github.com/buenotty)) — mantenedor do Blockfy
- **Robin Gebert** ([@Ronjar](https://github.com/Ronjar)) — Blokky original

Licença MIT. Veja [LICENSE](LICENSE).

## Assinatura (para quem desenvolve)

A chave de upload da Play **não** está neste repositório. Copie `keystore.properties.example` para `secrets/keystore.properties` e aponte para o seu arquivo PKCS12. Nunca envie `*.p12`, `*.jks`, `*.keystore` ou a pasta `secrets/` para o git.
