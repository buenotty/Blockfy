package com.robingebert.blokky.feature_accessibility

import kotlin.random.Random

object MindfulnessProvocationEngine {

    // 15 frases de impacto na abertura do aplicativo (para quebrar a abertura no piloto automático)
    private val appEntryQuotes = listOf(
        "Você veio aqui com um propósito claro ou abriu no piloto automático?",
        "Vê se sai rápido daqui. O mundo real e seus objetivos te esperam.",
        "Cada minuto perdido aqui é um atraso silencioso no seu futuro.",
        "O algoritmo quer sua atenção. Quem está no controle da sua vida hoje?",
        "O sucesso que você busca não vai aparecer no feed de ninguém.",
        "Sua atenção é sua moeda mais valiosa. Tem certeza que quer gastar agora?",
        "Faça o que precisa fazer e saia antes que o transe comece.",
        "Você é o mestre da sua atenção ou o escravo de uma notificação?",
        "Seus concorrentes e metas agradecem cada minuto que você gasta aqui.",
        "Pare um instante: você realmente precisava abrir esse aplicativo agora?",
        "O feed é infinito, mas o seu tempo no mundo tem prazo de validade.",
        "A vida acontece enquanto você olha para essa tela. Seja intencional.",
        "Não troque a sua história pelas histórias de quem nem sabe que você existe.",
        "Respire fundo. Recupere sua presença e decida conscientemente o que vai fazer.",
        "Um minuto de foco vale mais que uma hora de dopamina barata."
    )

    // 15 frases de choque reflexivo ao entrar no Reels / Shorts / TikTok (para quebrar o loop do transe)
    private val reelsReflectiveQuotes = listOf(
        "Mais 30 minutos na vida de estranhos ou 30 minutos construindo a sua?",
        "Quantos vídeos você já esqueceu que assistiu hoje? O transe começou.",
        "Essa dopamina barata realmente vale o preço dos seus maiores sonhos?",
        "Você não está descansando, está apenas anestesiando sua consciência.",
        "O que você estaria realizando agora se esse celular não existisse?",
        "Daqui a 1 ano, você vai se orgulhar de ter rolado esse feed por horas?",
        "Vídeo rápido, tempo perdido para sempre. Feche e vença essa batalha agora.",
        "Ninguém nunca construiu uma vida extraordinária rolando a tela para cima.",
        "O algoritmo foi desenhado para te prender. Prove que sua vontade é mais forte.",
        "Essa sensação de vazio depois de rolar a tela... quer mesmo senti-la de novo?",
        "O tédio cria genialidade. O feed só cria ansiedade e conformismo.",
        "Enquanto você assiste a vida dos outros, quem está vivendo a sua?",
        "A dopamina fácil de hoje é a frustração e o arrependimento de amanhã.",
        "Você prometeu que ia mudar. Essa rolagem faz parte da sua promessa?",
        "Feche o reels agora, levante a cabeça e faça o que precisa ser feito."
    )

    fun getRandomAppEntryQuote(): String {
        return appEntryQuotes[Random.nextInt(appEntryQuotes.size)]
    }

    fun getRandomReelsQuote(): String {
        return reelsReflectiveQuotes[Random.nextInt(reelsReflectiveQuotes.size)]
    }

    fun getAllQuotes(): List<String> = appEntryQuotes + reelsReflectiveQuotes
}
