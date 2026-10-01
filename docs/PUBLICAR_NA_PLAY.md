# Como publicar o Blockfy na Google Play (guia para quem não programa)

Este guia assume que você não mexe com desenvolvimento. Confira cada tela no Play Console: o Google muda os nomes dos menus com frequência.

## 1. O que já está pronto e o que só você pode fazer

**Pronto no código:** alvo Android 36, política de privacidade (`PRIVACY.md`), aviso de Acessibilidade com Aceitar/Recusar, sem VPN, sem leitura de texto digitado, sem botão de doação no app, build de release em formato AAB.

**Só você pode fazer** (envolve conta, dinheiro, senha ou identidade):

1. Criar a conta de desenvolvedor da Google Play (taxa única) e verificar sua identidade.
2. Criar a chave de assinatura e guardá-la com segurança (seção 2).
3. Cadastrar o app, preencher os formulários (seções 4 a 6) e gravar o vídeo da Acessibilidade.
4. Fazer o teste fechado: contas pessoais novas precisam de **12 testadores ativos por 14 dias seguidos** antes de pedir a produção (regra vigente para contas criadas depois de novembro de 2023; confirme no Play Console).

## 2. A chave de assinatura, sem jargão

A chave é o "carimbo" que prova que uma atualização veio de você. O jeito mais seguro:

- **Deixe o Google guardar a chave principal** (opção **Play App Signing**, ligada por padrão em apps novos). Assim, se você perder o seu arquivo, o Google ainda consegue recuperar o acesso.
- Você só cria uma **chave de upload**, que serve para enviar o app. No Android Studio: **Build → Generate Signed App Bundle → Android App Bundle → Create new keystore**. Guarde o arquivo e as senhas em um **gerenciador de senhas** (nunca na mesma pasta do arquivo, nunca no GitHub, nunca no Drive junto com um arquivo de senha).
- **A chave `blockfy.p12` que ficou no histórico do GitHub deve ser considerada vazada.** Não use essa chave. Use uma nova.
- Para o GitHub montar o app sozinho (`.github/workflows/release.yml`), cadastre nos "Secrets" do repositório: `BLOCKFY_STORE_FILE_BASE64`, `BLOCKFY_STORE_PASSWORD`, `BLOCKFY_KEY_ALIAS`, `BLOCKFY_KEY_PASSWORD`. O resultado é um arquivo `.aab` para enviar à Play.

## 3. Textos da loja

**Nome:** Blockfy

**Descrição curta (até 80 caracteres):**
- pt-BR: `Menos Reels e Shorts. Mais foco. Bloqueio de vídeos curtos e sites adultos.`
- en: `Less Reels and Shorts. More focus. Blocks short videos and adult sites.`

**Descrição completa (pt-BR):**

> O Blockfy tira você do ciclo de Reels, Shorts e feeds sem fim.
>
> • Escolha o app (Instagram, YouTube, TikTok, Facebook, X) e o que bloquear: só os vídeos curtos ou o app inteiro.
> • Bloqueie direto ou libere um limite diário em minutos.
> • Defina horários e dias da semana. Um resumo em frase mostra o que vai acontecer.
> • Modo estrito: até a meia-noite você só pode deixar os bloqueios mais rígidos.
> • Escudo de sites adultos: compara a barra de endereço do navegador com uma lista guardada no próprio celular.
> • Aba Conceitos com leituras curtas sobre por que bloquear.
>
> Privacidade: o app não tem anúncios, conta, servidor nem análise de uso. Nada do que o app vê sai do seu celular. Não observa apps de banco, mensagens nem o que você digita em outros apps.
>
> Este app usa o Serviço de Acessibilidade do Android, e só funciona depois que você ativa e aceita o aviso dentro do app.

**Full description (en):** same content, translated; keep the last paragraph about Accessibility.

## 4. Declaração de Acessibilidade (a parte mais difícil de aprovar)

O Blockfy **não** é uma ferramenta de acessibilidade para pessoas com deficiência. Por isso:

- **Não** marque o app como ferramenta de acessibilidade.
- Mostre o aviso dentro do app antes de pedir a permissão (já existe: tela com **Aceitar** e **Recusar**).
- Preencha o formulário de declaração da permissão com este texto:

> O Blockfy usa o Serviço de Acessibilidade para (1) detectar quando o usuário abre as telas de vídeo curto (Reels, Shorts) do Instagram, YouTube, TikTok, Facebook e X e sair delas quando o usuário configurou um bloqueio ou limite diário; e (2) opcionalmente, se o usuário ativar o escudo de sites adultos, ler apenas a barra de endereço de navegadores para compará-la com uma lista de sites adultos guardada no aparelho e levar o usuário à tela inicial. O serviço é restrito a uma lista fixa de pacotes (apps sociais e navegadores). Nenhum conteúdo é armazenado, registrado ou enviado para fora do aparelho. O app não lê mensagens, senhas nem texto digitado em outros apps.

- **Vídeo de demonstração (30 a 60 segundos):** grave a tela mostrando (a) o aviso com Aceitar, (b) a tela de Acessibilidade do Android com o Blockfy sendo ligado, (c) um bloqueio acontecendo no Instagram/YouTube, (d) o escudo adulto levando para a tela inicial ao abrir um site da lista (use um site de teste, nunca conteúdo explícito na gravação).

**Risco real:** a Play pode recusar o uso de Acessibilidade para um app que não é de acessibilidade. Se recusar, o plano B é publicar sem o escudo adulto e sem o bloqueio de Reels (o que esvazia o app) ou distribuir o APK pelo GitHub. Prepare-se para isso.

## 5. Segurança dos dados (formulário "Data safety")

Respostas coerentes com o código atual:

- O app coleta ou compartilha dados? **Não.** Tudo fica no aparelho.
- Criptografia em trânsito: não se aplica (o app não envia dados).
- O usuário pode pedir para apagar os dados? Desinstalar o app apaga tudo.
- **URL da política de privacidade:** `https://github.com/buenotty/Blockfy/blob/main/PRIVACY.md`

## 6. Outras respostas

- **Anúncios:** não tem.
- **Compras no app:** não tem. **Não coloque botão de doação no app**; apoio só pelo GitHub.
- **Público-alvo e classificação:** o app trata de bloqueio de pornografia, mas não mostra conteúdo adulto. Responda ao questionário de classificação com honestidade; o resultado típico é livre ou 12+.
- **Verificação de desenvolvedor (Android, Brasil):** o Google passa a exigir que apps instalados em aparelhos certificados venham de desenvolvedores verificados. Apps publicados pela Play fazem parte desse processo; confirme o prazo e os passos no Play Console.

## 6.1 Permissão de bateria (declaração obrigatória)

O app pede ao Android para não ser suspenso (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`), como outros apps de bloqueio fazem. A Play restringe essa permissão e pode pedir uma declaração no Play Console. Use este texto: "O Blockfy bloqueia apps no horário que o usuário configurou. Se o sistema suspender o serviço de acessibilidade para economizar bateria, os bloqueios deixam de funcionar sem aviso, e a função principal do app falha. O pedido aparece uma única vez, na configuração inicial, e o usuário pode recusar." **Risco:** se a Play recusar a declaração, remova a permissão do `AndroidManifest.xml` e use só a tela de ajustes de bateria do sistema.

## 7. Depois de publicar

- Use o painel **Android vitals** do Play Console para acompanhar bateria, travamentos e despertares do app nos testadores da beta.
- Os apps de rede social mudam a tela deles sem avisar. Se Reels/Shorts deixarem de ser detectados depois de uma atualização do Instagram ou do YouTube, a detecção precisa de ajuste (peça aos testadores uma gravação de tela do problema).
