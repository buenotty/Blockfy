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
- 🔄 **In-App Self Updater (Atualizador Automático OTA)**:
  - O Blockfy verifica novas versões diretamente na API do GitHub, exibe as notas da atualização, baixa o APK com barra de progresso em tempo real e inicia a instalação nativa do Android com um toque, sem precisar abrir o navegador ou baixar manualmente!
- 🔒 **100% Private & Open Source**:
  - A permissão de Internet é utilizada exclusivamente pelo atualizador para consultar o GitHub e baixar a nova versão. O serviço de acessibilidade e o controle de tempo são 100% locais no dispositivo, sem rastreamento nem telemetria.
- 🎨 **Modern Cyber Focus Theme**:
  - Interface Jetpack Compose Material 3 escura de alto contraste, adaptada às cores da nova logo oficial.

---

## 🛡️ Segurança e Verificação no VirusTotal (100% Seguro)

> **0 Detecções no VirusTotal — Aplicativo 100% Limpo e Seguro**

Muitos usuários ficam receosos ao conceder permissões do Serviço de Acessibilidade no Android por conta dos avisos genéricos do sistema operacional. Com o **Blockfy**, sua segurança e privacidade são garantidas:

- ✅ **0 Detecções no VirusTotal**: O APK passa por escaneamento em mais de 70 antivírus líderes de mercado com **0 detecções** (totalmente livre de malwares, vírus ou spywares).
- ✅ **Privacidade Total**: A conexão com a internet é restrita estritamente às requisições do atualizador integrado para consultar novos lançamentos no GitHub. Seus hábitos, cliques, dados de tela e tempos de uso nunca saem do seu aparelho.
- ✅ **Código 100% Aberto e Transparente**: Todo o código-fonte está disponível publicamente para auditoria de qualquer desenvolvedor.
- ✅ **Sem Anúncios e Sem Rastreadores**: Não contém SDKs de publicidade, analytics invasivos ou telemetria em segundo plano.

---

## 📲 Instalação e Como Ativar (Passo a Passo)

### 1. Download do APK
Baixe a versão mais recente em [**Releases**](https://github.com/buenotty/Blockfy/releases) ou pela aba [**Actions (Artifacts)**](https://github.com/buenotty/Blockfy/actions).

### 2. Aviso do Google Play Protect
Como o APK é instalado manualmente (fora da Play Store), o Google Play Protect pode exibir um aviso de "App bloqueado":
- Toque em **"Mais detalhes"** (ou na setinha para baixo).
- Selecione **"Instalar assim mesmo"**.
- *(O app é 100% seguro e auditado com 0 detecções no VirusTotal).*

---

## ⚙️ Como Ativar o Serviço de Acessibilidade por Fabricante

### 📱 Samsung (One UI), Motorola e Android 13+ ("Configuração Restrita")
No Android 13 ou superior, o sistema operacional bloqueia serviços de acessibilidade de apps baixados manualmente com o aviso: **"Configuração restrita"**.

**Como desbloquear em 30 segundos:**
1. Abra as **Configurações** do seu celular → vá em **Aplicativos** (ou segure o ícone do Blockfy na tela inicial e toque no **"i"** de Informações);
2. No canto superior direito da tela de Informações do Blockfy, toque nos **3 pontinhos (⋮)**;
3. Selecione **"Permitir configurações restritas"**;
4. Confirme com sua impressão digital ou PIN/desenho;
5. Agora abra o Blockfy, toque no card do serviço e ative o botão normalmente em **Acessibilidade**!

---

### 📱 Xiaomi, HyperOS e MIUI
1. Vá em Informações do App Blockfy e ative a opção **"Início Automático"**;
2. Em **"Outras permissões"**, marque "Exibir pop-ups enquanto estiver em segundo plano";
3. Ao ativar em Acessibilidade → Aplicativos baixados → Blockfy, aguarde a contagem de 10 segundos da Xiaomi e toque em **"OK"**;
4. *(Opcional)* Se a sua versão da MIUI ainda bloquear, use o comando rápido via ADB:
```bash
adb shell settings put secure enabled_accessibility_services com.buenotty.blockfy/com.robingebert.blokky.feature_accessibility.ReelsBlockAccessibilityService
```
*(No computador, basta executar `GrantAccessibilityPermission.bat` no Windows ou `./grant_accessibility_permission.sh` no Mac/Linux).*

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
