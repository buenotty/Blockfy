# Textos prontos para a Play Console (versão 1.14 em diante)

Atualiza as seções 3, 4 e 5 de `PUBLICAR_NA_PLAY.md`. Tudo abaixo descreve o que o código faz hoje.

## 1. O que mudou desde a versão aprovada

| Mudança | Efeito para a Play |
|---|---|
| A barra de endereço do navegador também é lida quando há limite de tempo num app que tem site (Instagram, YouTube, TikTok, Facebook, X, Telegram), só para reconhecer o site | **Muda a declaração de Acessibilidade** |
| Permissão opcional "Acesso ao uso" (`PACKAGE_USAGE_STATS`), só tempo em primeiro plano dos apps rastreados | Nova permissão a justificar |
| Jornada (patente, XP, medalhas), jogos curtos, modo foco, tile e widget | Tudo local. Sem efeito em dados |
| O Telegram é bloqueado como app inteiro, sem ler a tela | Sem efeito |

Não foi adicionada leitura de mensagens, de texto digitado nem de conteúdo de outros apps. A verificação de texto dentro dos apps foi **removida** antes da publicação.

## 2. Declaração de Acessibilidade (colar no formulário)

**Português**

> O Blockfy usa o Serviço de Acessibilidade para ajudar o usuário a controlar o tempo em redes sociais, e só para isso: (1) detectar que o usuário está nas telas de vídeo curto (Reels, Shorts, TikTok, Facebook Reels) do Instagram, YouTube, TikTok e Facebook, pelos identificadores de elementos da tela desses apps, e sair delas quando o usuário configurou um bloqueio ou um limite; (2) saber que o Instagram, YouTube, TikTok, Facebook, X ou Telegram está em primeiro plano, para contar o tempo e aplicar os limites que o usuário definiu; (3) quando o usuário define um limite para um desses apps, ler apenas a barra de endereço do navegador para reconhecer o site do mesmo serviço (por exemplo instagram.com) e aplicar o mesmo limite, para que o site não seja uma forma de burlar o limite; (4) opcionalmente, se o usuário ligar o escudo de sites adultos, comparar a barra de endereço com uma lista guardada no aparelho e levar o usuário à tela inicial. O serviço só recebe eventos de uma lista fixa de pacotes (os apps acima e navegadores conhecidos) e só ouve os apps que têm uma regra ativa. O texto da barra de endereço é comparado na memória e descartado: nada é armazenado, registrado nem enviado para fora do aparelho. O app não lê mensagens, senhas, conteúdo de páginas nem texto digitado, e não observa apps de banco. O usuário ativa o serviço nas configurações do Android depois de aceitar um aviso dentro do app, e pode desligá-lo a qualquer momento.

**English**

> Blockfy uses the Accessibility Service to help users control their time on social networks, and only for that: (1) to detect that the user is on the short-video screens (Reels, Shorts, TikTok, Facebook Reels) of Instagram, YouTube, TikTok and Facebook, from the on-screen element identifiers of those apps, and to leave them when the user configured a block or limit; (2) to know that Instagram, YouTube, TikTok, Facebook, X or Telegram is in the foreground, so time is counted and the limits the user set are applied; (3) when the user sets a limit for one of those apps, to read only the browser's address bar in order to recognise the same service's website (for example instagram.com) and apply the same limit, so the website cannot be used to get around it; (4) optionally, if the user turns on the adult-site shield, to compare the address bar with a list stored on the device and take the user to the home screen. The service only receives events from a fixed list of packages (the apps above and known browsers) and only listens to apps that have an active rule. Address-bar text is compared in memory and discarded: nothing is stored, logged or sent off the device. The app does not read messages, passwords, page content or typed text, and does not observe banking apps. The user turns the service on in Android settings after accepting an in-app notice, and can turn it off at any time.

**Mantenha:** o app **não** é ferramenta de acessibilidade (flag `isAccessibilityTool` ausente). O aviso dentro do app (Aceitar/Recusar) já existe.

### Vídeo da declaração (só se a Play pedir de novo)

Roteiro de 60 s, sem conteúdo explícito:
1. Abrir o Blockfy, mostrar o aviso e tocar em **Aceitar**.
2. Ligar o serviço em Configurações > Acessibilidade.
3. Definir um limite de 1 min para o Instagram e estourar.
4. Abrir `instagram.com` no Chrome: o Blockfy leva para a tela inicial e abre a pergunta de exceção.
5. Mostrar o escudo adulto com um site de teste.

## 3. Segurança dos dados (formulário "Data safety")

Nada sai do aparelho, então a resposta continua a mesma:

| Pergunta | Resposta |
|---|---|
| O app coleta ou compartilha algum dos tipos de dados exigidos? | **Não** |
| Dados coletados | Nenhum |
| Dados compartilhados | Nenhum |
| Segurança: criptografia em trânsito | Não se aplica (o app não transmite dados) |
| Segurança: o usuário pode pedir a exclusão dos dados | Sim: desinstalar o app apaga tudo; não há conta nem servidor |
| Política de privacidade | `https://github.com/buenotty/Blockfy/blob/main/PRIVACY.md` |

Observações para evitar dúvida do revisor: o tempo de uso, a barra de endereço, o check-in, as medalhas e o histórico são processados **só no aparelho** e nunca transmitidos. Isso não é "coleta" na definição da Play (dado que sai do aparelho).

## 4. Justificativa da permissão "Acesso ao uso" (`PACKAGE_USAGE_STATS`)

**Português:** "Opcional. Permite ao Blockfy ler, no próprio aparelho, por quanto tempo cada app rastreado (Instagram, YouTube, TikTok, Facebook, X, Telegram) ficou em primeiro plano hoje, para que os limites de tempo que o usuário definiu coincidam com o tempo que o próprio Android mostra. Só o tempo dos apps rastreados é consultado, nada é enviado e o app funciona sem a permissão."

**English:** "Optional. Lets Blockfy read, on the device, how long each tracked app (Instagram, YouTube, TikTok, Facebook, X, Telegram) was in the foreground today, so the time limits the user set match the time Android itself shows. Only the tracked apps are queried, nothing is sent, and the app works without the permission."

Código: `feature_monitor/UsageStatsReader.kt` filtra por pacote (um teste garante); a permissão nunca é pedida sem o usuário abrir a tela de ajustes.

## 5. Atualização da ficha (acrescentar à descrição completa)

**pt-BR**

> • Jornada: suba de Recruta a General e depois de prestígio, ganhe medalhas e veja sua meta de tempo limpo.
> • Bateu a vontade? Respire, jogue ou aguente firme por dois minutos.
> • Modo foco e tile de configurações rápidas para bloquear tudo por 1 ou 2 horas.
> • Limites por sessão (com pausa) e por número de aberturas.
> • Se o limite acabar, uma conversa curta ajuda a decidir se vale mesmo pedir mais alguns minutos.
> • O site de um app com limite conta como o app.

**English**

> • Journey: climb from Recruit to General, then prestige, earn medals and track your clean-time goal.
> • Hit by an urge? Breathe, play or hold on for two minutes.
> • Focus mode and a Quick Settings tile to block everything for 1 or 2 hours.
> • Session limits (with a break) and daily open limits.
> • When a limit is reached, a short chat helps you decide if you really need a few more minutes.
> • The website of a limited app counts like the app.

## 6. Notas da versão

**pt-BR:** "Novo: Jornada com patentes e medalhas, jogos curtos para a hora da vontade, modo foco, limites por sessão e por aberturas, e o site do app agora conta no limite. Correções no tempo contado do Reels e Shorts."

**en:** "New: Journey with ranks and medals, short games for the moment an urge hits, focus mode, session and open limits, and an app's website now counts toward its limit. Fixes to the time counted for Reels and Shorts."
