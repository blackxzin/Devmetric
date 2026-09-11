package com.devmetrics.common.event;

/**
 * Publicado depois que uma atividade e registrada.
 * Desacopla o registro de atividade da avaliacao de conquistas: o modulo de
 * conquistas escuta o evento em vez de ser chamado direto, o que evita
 * dependencia circular entre os dois servicos.
 */
public record ActivityRecordedEvent(Long userId) {
}
