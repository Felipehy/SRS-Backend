package com.siurbinfo.srs.enums;

// Estados possiveis do fluxo de uma reserva (auditorio, veiculo ou sala de reuniao).
public enum ReserveStatus {

    ENVIADA_PARA_ANALISE, // status inicial, aguardando aprovacao
    APROVADA,
    REPROVADA,
    CANCELADA,
    FINALIZADA;


}
