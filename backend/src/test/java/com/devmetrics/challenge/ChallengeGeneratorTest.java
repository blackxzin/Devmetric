package com.devmetrics.challenge;

import com.devmetrics.challenge.domain.ChallengeTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChallengeGeneratorTest {

    private final ChallengeGenerator generator = new ChallengeGenerator();

    private ChallengeGenerator.Context active() {
        // Usuario em dia: nada disparado, cai no DEFAULT.
        return new ChallengeGenerator.Context(0, 5, true, 4, 3, 1.2, 2);
    }

    @Test
    @DisplayName("dois dias sem atividade disparam o gatilho de inatividade")
    void inactivityHasTopPriority() {
        ChallengeGenerator.Context context = new ChallengeGenerator.Context(
                3, 0, false, 0, 0, 0.0, 0);

        assertThat(generator.selectTrigger(context)).isEqualTo(ChallengeTrigger.INACTIVE_DAYS);
    }

    @Test
    @DisplayName("sequencia viva sem atividade hoje dispara o gatilho de manter a sequencia")
    void streakAtRiskComesBeforeQualityTriggers() {
        ChallengeGenerator.Context context = new ChallengeGenerator.Context(
                1, 5, false, 0, 0, 0.0, 0);

        assertThat(generator.selectTrigger(context)).isEqualTo(ChallengeTrigger.STREAK_KEEPER);
    }

    @Test
    @DisplayName("sem testes na janela dispara o gatilho de teste")
    void noTestsTrigger() {
        ChallengeGenerator.Context context = new ChallengeGenerator.Context(
                0, 1, true, 0, 5, 1.2, 2);

        assertThat(generator.selectTrigger(context)).isEqualTo(ChallengeTrigger.NO_TESTS);
    }

    @Test
    @DisplayName("sem documentacao na janela dispara o gatilho de documentacao")
    void noDocsTrigger() {
        ChallengeGenerator.Context context = new ChallengeGenerator.Context(
                0, 1, true, 3, 0, 1.2, 2);

        assertThat(generator.selectTrigger(context)).isEqualTo(ChallengeTrigger.NO_DOCS);
    }

    @Test
    @DisplayName("baixa variedade dispara o gatilho de variedade")
    void lowVarietyTrigger() {
        ChallengeGenerator.Context context = new ChallengeGenerator.Context(
                0, 1, true, 3, 3, 0.1, 2);

        assertThat(generator.selectTrigger(context)).isEqualTo(ChallengeTrigger.LOW_VARIETY);
    }

    @Test
    @DisplayName("um mes sem tecnologia nova dispara o gatilho de descoberta")
    void noNewTechnologyTrigger() {
        ChallengeGenerator.Context context = new ChallengeGenerator.Context(
                0, 1, true, 3, 3, 1.2, 0);

        assertThat(generator.selectTrigger(context)).isEqualTo(ChallengeTrigger.NO_NEW_TECH);
    }

    @Test
    @DisplayName("usuario em dia recebe o desafio padrao")
    void healthyUserGetsDefaultChallenge() {
        assertThat(generator.selectTrigger(active())).isEqualTo(ChallengeTrigger.DEFAULT);
    }

    @Test
    @DisplayName("o texto do desafio troca os marcadores pelo contexto real")
    void renderReplacesPlaceholders() {
        String rendered = generator.render(
                "Voce esta ha {dias} dias sem atividade. Abra {projeto} e faca um commit.",
                "devmetrics-api", 3);

        assertThat(rendered).isEqualTo(
                "Voce esta ha 3 dias sem atividade. Abra devmetrics-api e faca um commit.");
    }

    @Test
    @DisplayName("sem projeto conhecido o texto usa um termo generico")
    void renderFallsBackWhenProjectIsUnknown() {
        assertThat(generator.render("Abra {projeto}.", null, 0)).isEqualTo("Abra seu projeto atual.");
        assertThat(generator.render("Abra {projeto}.", "  ", 0)).isEqualTo("Abra seu projeto atual.");
    }

    @Test
    @DisplayName("um sentinela de 'nunca teve atividade' nao aparece cru no texto")
    void renderClampsAbsurdInactivityCounts() {
        String rendered = generator.render("Ha {dias} dias sem atividade.", "proj", Integer.MAX_VALUE);

        assertThat(rendered).isEqualTo("Ha 365 dias sem atividade.");
        assertThat(rendered).doesNotContain("2147483647");
    }
}
