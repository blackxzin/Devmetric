package com.devmetrics.achievement;

import com.devmetrics.common.event.ActivityRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avalia conquistas depois que a atividade foi realmente gravada.
 *
 * AFTER_COMMIT garante que a avaliacao ve o estado final; a transacao nova e
 * necessaria porque a original ja foi encerrada. Uma falha aqui nunca pode
 * derrubar o registro da atividade.
 */
@Component
public class AchievementListener {

    private static final Logger log = LoggerFactory.getLogger(AchievementListener.class);

    private final AchievementService achievementService;

    public AchievementListener(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onActivityRecorded(ActivityRecordedEvent event) {
        try {
            achievementService.evaluate(event.userId());
        } catch (RuntimeException ex) {
            log.warn("Falha ao avaliar conquistas do usuario {}: {}", event.userId(), ex.getMessage());
        }
    }
}
