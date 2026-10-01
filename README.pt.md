# Blockfy

[English](README.md) · [Português](README.pt.md)

Menos Reels. Mais foco.

App Android gratuito e de código aberto. Sem anúncio e sem assinatura.

Fork do [Blokky](https://github.com/Ronjar/Blokky), do Robin Gebert. Quem mantém é o [Samuel Bueno](https://github.com/buenotty).

## O que é

O Blockfy tira você de vídeo curto (Reels, Shorts, TikTok) e pode manter você longe de sites adultos. Ele usa o serviço de Acessibilidade do Android, só para Instagram, YouTube, TikTok, Facebook, X e navegadores. Nunca observa apps de banco, mensagens nem o que você digita em outros apps. Veja [PRIVACY.md](PRIVACY.md).

Se o celular for do Brasil, o app começa em português. Nos outros lugares começa em inglês. Dá para mudar na aba Ajustes.

## O que faz

- **Uma regra por app.** Escolha o que bloquear (só Reels/Shorts ou o app inteiro), como (bloquear direto ou liberar um limite diário em minutos) e quando (horários e dias da semana). Um resumo em frase no topo do editor diz exatamente o que vai acontecer.
- **Limites diários** zeram à meia-noite e são contados na hora, então o limite vale assim que você o atinge.
- **Escudo de sites adultos**, se você ligar: a Acessibilidade lê só a barra de endereço do navegador e compara com uma lista de sites adultos no próprio celular. Nada é guardado nem enviado. Sem VPN. Os sites entram pelo nome inteiro, então `jerome` não vira `erome`.
- **Modo estrito** até a meia-noite: você ainda pode deixar bloqueios mais rígidos, mas não desligar nem afrouxar. Fora do modo estrito, afrouxar um bloqueio faz você esperar alguns segundos e confirmar.
- Aba **Conceitos**: leituras curtas sobre por que esses bloqueios existem, para o dia em que você quiser desligá-los.
- **Diagnóstico** (aba Ajustes): mostra se o serviço está conectado e o que ele viu por último, para achar o motivo quando um bloqueio não dispara.

As atualizações devem vir da Google Play. O app não instala APK sozinho. Guia de publicação: [docs/PUBLICAR_NA_PLAY.md](docs/PUBLICAR_NA_PLAY.md).

## Requisitos

Android 9 ou mais novo.

## Apoie o projeto

O Blockfy é grátis, sem anúncios e sem pedido de doação dentro do app. Se ele te ajuda, dá pra apoiar por aqui no GitHub:

- Pix: `496f008e-c67d-4175-9fad-e6b3c9bbd248`
- PayPal: [paypal.me/donate](https://www.paypal.com/donate/?business=samuellbuenno%40gmail.com&currency_code=BRL) (samuellbuenno@gmail.com)
- Ou só deixe uma estrela no repositório.

## Créditos

Samuel Bueno ([@buenotty](https://github.com/buenotty)) cuida do Blockfy.

Robin Gebert ([@Ronjar](https://github.com/Ronjar)) fez o Blokky original.

Licença MIT. Veja [LICENSE](LICENSE).

## Assinatura (para quem desenvolve)

A chave de upload da Play não está neste repositório. Copie `keystore.properties.example` para `secrets/keystore.properties` e aponte para o seu PKCS12. Não suba `*.p12`, `*.jks`, `*.keystore` nem a pasta `secrets/`.
