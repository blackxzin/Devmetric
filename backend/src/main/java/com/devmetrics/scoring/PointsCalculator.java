package com.devmetrics.scoring;

import com.devmetrics.scoring.domain.EffectiveRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula os pontos de UMA atividade.
 *
 * Regras, nesta ordem:
 * 1. regra inativa            -> 0
 * 2. acima do limite diario   -> 0
 * 3. rendimento decrescente   -> base / n, onde n e a posicao da atividade no dia
 * 4. bonus de tecnologia nova -> multiplica pelo fator configurado
 *
 * O rendimento decrescente e o que impede que 50 commits vazios valham 250 pontos.
 */
@Component
public class PointsCalculator {

    private static final int INTERNAL_SCALE = 6;
    private static final int RESULT_SCALE = 2;

    /**
     * @param rule            regra efetiva do tipo de atividade
     * @param occurrenceOfDay posicao da atividade entre as do mesmo tipo no mesmo dia (1 = primeira)
     * @param newTechnology   true quando a atividade estreia uma tecnologia para o usuario
     * @param newTechBonus    fator de bonus (0.5 = +50%)
     */
    public BigDecimal calculate(EffectiveRule rule, int occurrenceOfDay,
                                boolean newTechnology, double newTechBonus) {
        if (rule == null || !rule.active() || rule.basePoints() == null) {
            return BigDecimal.ZERO;
        }
        int position = Math.max(1, occurrenceOfDay);
        if (rule.dailyCap() != null && position > rule.dailyCap()) {
            return BigDecimal.ZERO;
        }

        BigDecimal points = rule.basePoints();
        if (rule.diminishing()) {
            points = points.divide(BigDecimal.valueOf(position), INTERNAL_SCALE, RoundingMode.HALF_UP);
        }
        if (newTechnology && newTechBonus > 0) {
            points = points.multiply(BigDecimal.valueOf(1 + newTechBonus));
        }
        return points.setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }
}
