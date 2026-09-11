package com.devmetrics.scoring;

import com.devmetrics.auth.repository.RefreshTokenRepository;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
public class ScoreScheduler {

    private static final Logger log = LoggerFactory.getLogger(ScoreScheduler.class);

    private final UserRepository userRepository;
    private final ScoreService scoreService;
    private final DailyStatsService dailyStatsService;
    private final RefreshTokenRepository refreshTokenRepository;

    public ScoreScheduler(UserRepository userRepository,
                          ScoreService scoreService,
                          DailyStatsService dailyStatsService,
                          RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.scoreService = scoreService;
        this.dailyStatsService = dailyStatsService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Foto diaria do Dev Score de cada usuario. E o que alimenta o grafico de evolucao:
     * sem snapshot nao existe historico, porque o score e sempre calculado sobre uma janela movel.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void takeDailySnapshots() {
        List<User> users = userRepository.findAll();
        int processed = 0;
        for (User user : users) {
            try {
                LocalDate today = LocalDate.now(user.zoneId());
                dailyStatsService.refreshIntensity(user, today);
                scoreService.snapshot(user, today);
                processed++;
            } catch (RuntimeException ex) {
                log.warn("Falha ao gerar snapshot do usuario {}: {}", user.getId(), ex.getMessage());
            }
        }
        log.info("Snapshots diarios gerados para {} de {} usuarios", processed, users.size());
    }

    /** Limpeza de refresh tokens vencidos ha mais de 30 dias. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        int removed = refreshTokenRepository.deleteExpiredBefore(Instant.now().minusSeconds(30L * 86400));
        if (removed > 0) {
            log.info("Refresh tokens expirados removidos: {}", removed);
        }
    }
}
