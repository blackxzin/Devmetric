package com.devmetrics.challenge;

import com.devmetrics.challenge.domain.ChallengeTrigger;
import org.springframework.stereotype.Component;

/**
 * Escolhe qual gatilho de desafio se aplica ao usuario hoje.
 *
 * Funcao pura sobre um retrato da atividade recente: sem banco e sem relogio,
 * entao da para testar cada cenario isoladamente.
 *
 * O tom e sempre de facilitar o comeco, nunca de cobranca: por isso os desafios
 * sao pequenos e tem tempo estimado.
 */
@Component
public class ChallengeGenerator {

    private static final int INACTIVE_DAYS_THRESHOLD = 2;
    private static final int STREAK_AT_RISK = 3;
    private static final int TEST_WINDOW_DAYS = 14;
    private static final double LOW_VARIETY_ENTROPY = 0.3;
    private static final int NEW_TECH_WINDOW_DAYS = 30;

    /**
     * Teto para o "{dias}" exibido no texto do desafio. Sem isso, um usuario que nunca
     * registrou nada (sentinela de "sem atividade alguma") mostraria um numero absurdo
     * de dias. 365 casa com a regra de negocio que ja rejeita atividades mais antigas
     * que isso (ActivityService.validateMoment).
     */
    private static final int MAX_DISPLAY_INACTIVE_DAYS = 365;

    /**
     * @param daysSinceLastActivity dias desde a ultima atividade (0 = hoje ja tem atividade)
     * @param currentStreak         sequencia atual de dias ativos
     * @param hasActivityToday      se ja existe atividade registrada hoje
     * @param testsInWindow         testes registrados nos ultimos 14 dias
     * @param docsInWindow          documentacoes registradas nos ultimos 14 dias
     * @param varietyEntropy        entropia da distribuicao de tipos na janela do score
     * @param newTechnologiesInMonth tecnologias estreadas nos ultimos 30 dias
     */
    public record Context(
            int daysSinceLastActivity,
            int currentStreak,
            boolean hasActivityToday,
            long testsInWindow,
            long docsInWindow,
            double varietyEntropy,
            long newTechnologiesInMonth
    ) {
    }

    public ChallengeTrigger selectTrigger(Context context) {
        if (context.daysSinceLastActivity() >= INACTIVE_DAYS_THRESHOLD) {
            return ChallengeTrigger.INACTIVE_DAYS;
        }
        if (context.currentStreak() >= STREAK_AT_RISK && !context.hasActivityToday()) {
            return ChallengeTrigger.STREAK_KEEPER;
        }
        if (context.testsInWindow() == 0) {
            return ChallengeTrigger.NO_TESTS;
        }
        if (context.docsInWindow() == 0) {
            return ChallengeTrigger.NO_DOCS;
        }
        if (context.varietyEntropy() < LOW_VARIETY_ENTROPY) {
            return ChallengeTrigger.LOW_VARIETY;
        }
        if (context.newTechnologiesInMonth() == 0) {
            return ChallengeTrigger.NO_NEW_TECH;
        }
        return ChallengeTrigger.DEFAULT;
    }

    public int testWindowDays() {
        return TEST_WINDOW_DAYS;
    }

    public int newTechnologyWindowDays() {
        return NEW_TECH_WINDOW_DAYS;
    }

    /**
     * Troca os marcadores do template pelo contexto real do usuario.
     */
    public String render(String template, String projectName, int daysInactive) {
        String project = (projectName == null || projectName.isBlank()) ? "seu projeto atual" : projectName;
        int displayDays = Math.min(MAX_DISPLAY_INACTIVE_DAYS, Math.max(0, daysInactive));
        return template
                .replace("{projeto}", project)
                .replace("{dias}", String.valueOf(displayDays));
    }
}
