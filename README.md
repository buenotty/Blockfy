# 🛡️ Blockfy — Stop Doomscrolling | Block Instagram Reels & YouTube Shorts on Android

<p align="center">
  <img src="app/src/main/res/drawable/ic_policy.xml" width="100" height="100" alt="Blockfy Logo"/>
</p>

<p align="center">
  <strong>Blockfy (Block For You)</strong> — Reclaim your focus, stop mindless scrolling, and boost your productivity.
</p>

<p align="center">
  <a href="https://github.com/buenotty/Blockfy/actions/workflows/build-debug-apk.yml"><img src="https://github.com/buenotty/Blockfy/actions/workflows/build-debug-apk.yml/badge.svg" alt="Build Status"/></a>
  <img src="https://img.shields.io/badge/Platform-Android%209.0%2B-3DDC84.svg?logo=android&logoColor=white" alt="Platform"/>
  <img src="https://img.shields.io/badge/VirusTotal-0%2F70%20Clean%20(100%25%20Safe)-brightgreen.svg?logo=virustotal&logoColor=white" alt="VirusTotal Clean"/>
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline%20(Zero%20Internet)-4F46E5.svg" alt="Privacy"/>
  <img src="https://img.shields.io/badge/Open%20Source-Yes-blue.svg" alt="Open Source"/>
  <a href="https://github.com/buenotty/Blockfy/stargazers"><img src="https://img.shields.io/github/stars/buenotty/Blockfy?style=social" alt="GitHub Stars"/></a>
</p>

---

## 🎯 What is Blockfy?

**Blockfy** is a lightweight, open-source Android app designed to curb doomscrolling on addictive short-form video feeds (**Instagram Reels** and **YouTube Shorts**) using the native Android Accessibility Service.

Whether you're dealing with ADHD, striving for a digital detox, or simply want to stop losing hours to algorithmic feeds, Blockfy puts you back in control of your screen time.

> 🚀 **Enhanced Fork**: Developed and maintained by [Samuel Bueno (@buenotty)](https://github.com/buenotty), based on the original project by [Robin Gebert (@Ronjar)](https://github.com/Ronjar/Blokky).

---

## ✨ Key Features (SEO & Benefits)

- 🚫 **Instagram Reels Blocker (Reload Loop Bug Fixed)**:
  - Intercepts Reels and returns you safely to your main feed without triggering the endless feed reload loop found in older apps.
- ⚡ **YouTube Shorts Blocker**:
  - Automatically navigates away from Shorts back to your home/subscriptions.
- ⏳ **Smart Daily Usage Limits (e.g., 15 min/day)**:
  - Set a daily screen time allowance (5m, 10m, 15m, 30m, 45m, 60m).
  - Enjoy short videos up to your limit, with **countdown warnings every 5 minutes** and **minute-by-minute alerts in the final 5 minutes**!
  - Once reached, short videos are locked until midnight.
- ⏰ **Custom Scheduled Focus Windows**:
  - Choose specific time intervals during the day (e.g. 09:00 to 18:00) to enforce blocking.
- 🔒 **100% Private & Completely Offline**:
  - **Zero Internet Permission (`android.permission.INTERNET` is not even requested)**.
  - Your data, usage stats, and habits never leave your phone.
- 🎨 **Modern Material 3 Design**:
  - Clean Jetpack Compose interface with dark mode support, quick toggle chips, and refreshed vector graphics.

---

## 🛡️ Segurança e Verificação no VirusTotal (100% Seguro)

> **0 Detecções no VirusTotal — Aplicativo 100% Limpo e Seguro**

Muitos usuários ficam receosos ao conceder permissões do Serviço de Acessibilidade no Android por conta dos avisos genéricos do sistema operacional. Com o **Blockfy**, sua segurança e privacidade são garantidas:

- ✅ **0 Detecções no VirusTotal**: O APK passa por escaneamento em mais de 70 antivírus líderes de mercado com **0 detecções** (totalmente livre de malwares, vírus ou spywares).
- ✅ **Zero Permissão de Internet**: O Blockfy **não possui** a permissão `android.permission.INTERNET` declarada em seu código. Isso torna **tecnicamente e fisicamente impossível** que o aplicativo envie qualquer dado, clique ou histórico de tela para servidores externos.
- ✅ **Código 100% Aberto e Transparente**: Todo o código-fonte está disponível publicamente para auditoria de qualquer desenvolvedor.
- ✅ **Sem Anúncios e Sem Rastreadores**: Não contém SDKs de publicidade, analytics invasivos ou telemetria em segundo plano.

---

## 📲 Installation & How to Download

### Option 1: Direct APK Download (Recommended)
1. Go to the [**GitHub Actions**](https://github.com/buenotty/Blockfy/actions) tab.
2. Click on the latest workflow run.
3. Under the **Artifacts** section at the bottom, download **`Blockfy-Debug-APK`**.
4. Extract the `.zip` and install `app-debug.apk` on your Android phone.

*(Or check the [Releases](https://github.com/buenotty/Blockfy/releases) tab for published APKs).*

---

## ⚙️ Enabling the Accessibility Service

1. Open **Blockfy** on your phone.
2. Tap the **Accessibility Service** card.
3. In Android Settings, navigate to **Downloaded Apps / Installed Services** → Tap **Blockfy** → Enable the switch.

### Notice for Xiaomi / MIUI / HyperOS / Vivo Users
Some custom Android skins restrict accessibility permissions for sideloaded apps. If restricted, simply connect your phone with USB debugging enabled and run:

```bash
adb shell settings put secure enabled_accessibility_services com.buenotty.blockfy/com.robingebert.blokky.feature_accessibility.ReelsBlockAccessibilityService
```
*(You can also double-click `GrantAccessibilityPermission.bat` on Windows or run `./grant_accessibility_permission.sh` on macOS/Linux).*

---

## ☕ Support the Creator / Apoiar o Criador

Blockfy is 100% free, open-source, and free of trackers or advertisements. If this app helps you save hours and regain focus, consider supporting the development!

### 🇧🇷 Apoio via Pix (Brasil)
- **Chave Pix Aleatória**: `496f008e-c67d-4175-9fad-e6b3c9bbd248`
*(Você também pode copiar a chave com um clique direto dentro do aplicativo no botão "Apoiar o Criador").*

### ⭐ Star the Project
Leave a star on the [GitHub Repository](https://github.com/buenotty/Blockfy) — it helps others discover the project!

---

## 👥 Authors & Credits

- **Fork Maintainer & Enhancements**: [Samuel Bueno (buenotty)](https://github.com/buenotty) — Bug fixes, daily time limits, countdown warnings, UI revamp, and CI/CD automation.
- **Original Project Creator**: [Robin Gebert (Ronjar)](https://github.com/Ronjar) — Creator of the original Blokky concept.

---

## 📄 License

This project is licensed under the terms of the original open-source license. See [LICENSE](LICENSE) for details.
