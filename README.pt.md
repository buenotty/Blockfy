# Blockfy

[English](README.md) · [Português](README.pt.md)

Menos Reels. Mais foco.

App Android gratuito e de código aberto. Sem anúncio e sem assinatura.

Fork do [Blokky](https://github.com/Ronjar/Blokky), do Robin Gebert. Quem mantém é o [Samuel Bueno](https://github.com/buenotty).

## O que é

O Blockfy tira você do vídeo curto (Reels, Shorts, TikTok). Dá para bloquear sites adultos conhecidos no próprio celular. A acessibilidade só observa Instagram, YouTube, TikTok, Facebook e X. Não observa apps de banco. O escudo adulto é um filtro DNS que fica no aparelho.

Se o celular é do Brasil, o app abre em português. Fora isso, abre em inglês. Dá para mudar isso na engrenagem da tela Sobre.

## O que ele faz

Reels e Shorts: a acessibilidade fica só no Instagram, YouTube, TikTok, Facebook e X. Os outros apps, inclusive banco, ele não mexe.

Limite diário: cota por aplicativo, se você ligar. Zera à meia-noite.

Escudo de conteúdo adulto, se você ligar: a Acessibilidade lê o texto na tela e o digitado em navegadores, X, Telegram e Reddit, analisa no celular e nunca guarda nem envia. Sem VPN. Apps de banco nunca são observados. Veja [PRIVACY.md](PRIVACY.md). Os sites entram pelo nome inteiro, então `jerome` não vira `erome`.

As atualizações devem vir da Google Play. O app não instala APK sozinho.

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
