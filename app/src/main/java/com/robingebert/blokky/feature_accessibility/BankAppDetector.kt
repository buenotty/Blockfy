package com.robingebert.blokky.feature_accessibility

/**
 * Detects banking and financial apps that actively scan for and block
 * Accessibility Services on Android.
 */
object BankAppDetector {

    /**
     * Comprehensive list of known banking app package names in Brazil and worldwide.
     */
    val BANK_PACKAGES: Set<String> = setOf(
        // ── Principais Bancos Brasileiros ──
        "com.nu.production",                    // Nubank
        "com.nu.production.tablet",             // Nubank Tablet
        "br.com.itau",                          // Itaú
        "br.com.itau.personnalite",             // Itaú Personnalité
        "br.com.itau.empresas",                 // Itaú Empresas
        "com.bradesco",                         // Bradesco
        "com.bradesco.prime",                   // Bradesco Prime
        "br.com.bradesco.next",                 // Next
        "br.com.bb.android",                    // Banco do Brasil
        "br.com.santander.app",                 // Santander
        "br.com.santander.way",                 // Santander Way
        "br.com.caixa.tem",                     // Caixa Tem
        "br.com.gabba.Caixa",                   // Caixa Econômica Federal
        "br.com.inter",                         // Banco Inter
        "br.com.intermedium",                   // Inter (legado)
        "com.c6bank.app",                       // C6 Bank
        "br.com.original.bank",                 // Banco Original
        "com.picpay",                           // PicPay
        "com.mercadopago.wallet",               // Mercado Pago
        "br.com.pagseguro.app",                 // PagBank / PagSeguro
        "br.com.neon",                          // Neon
        "com.btgpactual.app.btgmas",            // BTG Pactual
        "com.stone.banking",                    // Stone
        "br.com.xp.investor",                   // XP Investimentos
        "br.com.safra.SafraApp",                // Banco Safra
        "br.com.sicoob.app",                    // Sicoob
        "br.com.sicredi.app",                   // Sicredi
        "br.com.votorantim.bv",                 // BV
        "br.com.daycoval.app",                  // Banco Daycoval
        "com.iti.itau",                         // iti Itaú
        "br.com.agibank",                       // Agibank
        "br.com.bmg.app",                       // Banco BMG
        "br.com.banrisul.app",                  // Banrisul
        "br.com.bancopan",                      // Banco Pan
        "com.will.app",                         // Will Bank
        "br.com.digio",                         // digio
        "com.superdigital.app",                 // Superdigital
        "br.com.modal.more",                    // Banco Modal
        "br.com.claropay.app",                  // Claro Pay
        "br.com.recargapay",                    // RecargaPay
        "br.com.avenue.app",                    // Avenue
        "com.rico.app",                         // Rico Investimentos
        "br.com.nuinvest",                      // NuInvest
        "com.sofisadireto",                     // Sofisa Direto
        "com.infinitepay.app",                  // InfinitePay
        "com.ton.mobile",                       // Ton

        // ── Bancos Internacionais / Wallets ──
        "com.revolut.revolut",                  // Revolut
        "com.wise.android",                     // Wise
        "com.chase.sig.android",                // Chase
        "com.wf.wellsfargomobile",              // Wells Fargo
        "com.infonow.bofa",                     // Bank of America
        "com.citi.citimobile",                  // Citi Mobile
        "com.paypal.android.p2pmobile",         // PayPal
        "com.squareup.cash",                    // Cash App
        "com.venmo",                            // Venmo
        "piuk.blockchain.android",              // Blockchain.com
        "com.binance.dev"                       // Binance
    )

    /**
     * Checks whether the given package belongs to a banking/financial app.
     */
    fun isBankApp(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return packageName in BANK_PACKAGES
    }
}
