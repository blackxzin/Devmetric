package com.devmetrics.challenge.domain;

/**
 * Situacao que faz um desafio ser escolhido. A ordem do enum e a ordem de prioridade
 * usada pelo gerador: o primeiro gatilho satisfeito vence.
 */
public enum ChallengeTrigger {
    INACTIVE_DAYS,
    STREAK_KEEPER,
    NO_TESTS,
    NO_DOCS,
    LOW_VARIETY,
    NO_NEW_TECH,
    DEFAULT
}
