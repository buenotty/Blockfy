package com.buenotty.blockfy.feature_monitor

/**
 * Banking and payment apps that must never share Blockfy's network path.
 * Used only to exclude them from the DNS shield so Pix/login keep using the
 * device's normal connection.
 */
object BankPackages {
    val ALL: Set<String> = setOf(
        "com.nu.production",
        "com.nu.nubank",
        "com.bradesco",
        "com.bradesco.banco",
        "com.bradesco.next",
        "com.itau",
        "com.itau.itu",
        "com.banco.itau",
        "com.santander.app",
        "br.com.bb.android",
        "br.com.bancointer.bancointerapp",
        "br.com.intermedium",
        "com.c6bank.app",
        "com.picpay",
        "com.mercadopago.wallet",
        "br.com.pagseguro.wallet",
        "com.paypal.android.p2pmobile",
        "br.com.neon",
        "br.com.original.bank",
        "com.btg.pactual.digital",
        "br.com.gabba.Caixa",
        "br.com.sicoobnet",
        "br.com.sicredi.mobile",
        "com.bankpan.digital",
        "br.com.brainylab.banco.bmg",
        "com.xp.banco",
        "com.wise.android",
        "com.google.android.apps.nbu.paisa.user",
        "com.phonepe.app",
        "net.one97.paytm"
    )
}
