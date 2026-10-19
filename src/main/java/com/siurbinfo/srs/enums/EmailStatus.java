package com.siurbinfo.srs.enums;

// Estados de um e-mail na outbox (outbox_email). Coluna status e varchar(20), entao
// os nomes precisam caber nesse limite.
public enum EmailStatus {

    PENDING, // gravado na outbox, ainda nao foi entregue ao SMTP. Uma tentativa que
             // falha continua PENDING (com next_attempt_at no futuro): e assim que o
             // worker volta a encontrar a linha na rodada seguinte.
    SENT,
    DEAD     // estourou o limite de tentativas, nao sera reenviado automaticamente

}
