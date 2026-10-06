package com.devmetrics.challenge;

import com.devmetrics.activity.domain.Activity;
import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.activity.repository.ActivityRepository;
import com.devmetrics.activity.repository.projection.TypeAggregate;
import com.devmetrics.challenge.domain.ChallengeStatus;
import com.devmetrics.challenge.domain.ChallengeTemplate;
import com.devmetrics.challenge.domain.ChallengeTrigger;
import com.devmetrics.challenge.domain.DailyChallenge;
import com.devmetrics.challenge.dto.ChallengeResponse;
import com.devmetrics.challenge.repository.ChallengeTemplateRepository;
import com.devmetrics.challenge.repository.DailyChallengeRepository;
import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.common.exception.NotFoundException;
import com.devmetrics.scoring.DevScoreCalculator;
import com.devmetrics.technology.repository.UserTechnologyRepository;
import com.devmetrics.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ChallengeService {

    private static final int SCORE_WINDOW_DAYS = 90;

    private final DailyChallengeRepository dailyChallengeRepository;
    private final ChallengeTemplateRepository templateRepository;
    private final ActivityRepository activityRepository;
    private final UserTechnologyRepository userTechnologyRepository;
    private final ChallengeGenerator generator;
    private final DevScoreCalculator devScoreCalculator;
    private final ChallengeAiWriter aiWriter;

    public ChallengeService(DailyChallengeRepository dailyChallengeRepository,
                            ChallengeTemplateRepository templateRepository,
                            ActivityRepository activityRepository,
                            UserTechnologyRepository userTechnologyRepository,
                            ChallengeGenerator generator,
                            DevScoreCalculator devScoreCalculator,
                            ChallengeAiWriter aiWriter) {
        this.dailyChallengeRepository = dailyChallengeRepository;
        this.templateRepository = templateRepository;
        this.activityRepository = activityRepository;
        this.userTechnologyRepository = userTechnologyRepository;
        this.generator = generator;
        this.devScoreCalculator = devScoreCalculator;
        this.aiWriter = aiWriter;
    }

    @Transactional
    public ChallengeResponse today(User user) {
        LocalDate today = LocalDate.now(user.zoneId());
        return dailyChallengeRepository.findByUserAndDate(user.getId(), today)
                .map(ChallengeResponse::from)
                .orElseGet(() -> ChallengeResponse.from(generateFor(user, today, null)));
    }

    @Transactional
    public ChallengeResponse complete(User user, Long challengeId, Long activityId) {
        DailyChallenge challenge = requireChallenge(user.getId(), challengeId);
        if (challenge.getStatus() == ChallengeStatus.COMPLETED) {
            return ChallengeResponse.from(challenge);
        }
        Activity activity = null;
        if (activityId != null) {
            activity = activityRepository.findByIdAndUserId(activityId, user.getId())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.ACTIVITY_NOT_FOUND));
        }
        challenge.complete(activity);
        return ChallengeResponse.from(challenge);
    }

    @Transactional
    public ChallengeResponse skip(User user, Long challengeId) {
        DailyChallenge challenge = requireChallenge(user.getId(), challengeId);
        if (challenge.getStatus() == ChallengeStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Desafio ja concluido");
        }
        ChallengeTemplate current = challenge.getTemplate();
        ChallengeTemplate replacement = pickTemplate(current.getTrigger(), current.getId());
        String text = generator.render(replacement.getDescriptionTemplate(),
                lastProjectName(user), daysSinceLastActivity(user, LocalDate.now(user.zoneId())));
        challenge.skip(replacement, text);
        return ChallengeResponse.from(challenge);
    }

    @Transactional(readOnly = true)
    public Page<ChallengeResponse> history(Long userId, Pageable pageable) {
        return dailyChallengeRepository.findHistory(userId, pageable).map(ChallengeResponse::from);
    }

    @Transactional(readOnly = true)
    public long completedCount(Long userId) {
        return dailyChallengeRepository.countByUserIdAndStatus(userId, ChallengeStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public long completedBetween(Long userId, LocalDate from, LocalDate to) {
        return dailyChallengeRepository.countByUserIdAndStatusAndChallengeDateBetween(
                userId, ChallengeStatus.COMPLETED, from, to);
    }

    // ---------------------------------------------------------------- geracao

    private DailyChallenge generateFor(User user, LocalDate today, Long excludeTemplateId) {
        ChallengeGenerator.Context context = buildContext(user, today);
        ChallengeTrigger trigger = generator.selectTrigger(context);
        ChallengeTemplate template = pickTemplate(trigger, excludeTemplateId);
        String lastProject = lastProjectName(user);
        String text = generator.render(template.getDescriptionTemplate(),
                lastProject, context.daysSinceLastActivity());
        if (aiWriter.enabled()) {
            List<String> technologies = userTechnologyRepository.findAllByUser(user.getId()).stream()
                    .limit(5)
                    .map(userTechnology -> userTechnology.getTechnology().getName())
                    .toList();
            text = aiWriter.rewrite(text, trigger, context, technologies, lastProject).orElse(text);
        }
        return dailyChallengeRepository.save(DailyChallenge.create(user, template, today, text));
    }

    ChallengeGenerator.Context buildContext(User user, LocalDate today) {
        Long userId = user.getId();
        int daysSinceLastActivity = daysSinceLastActivity(user, today);
        boolean hasActivityToday = activityRepository
                .countByUserIdAndActivityDateBetween(userId, today, today) > 0;

        List<LocalDate> activeDates = activityRepository.findActiveDatesDesc(userId, today);
        int streak = currentStreak(activeDates, today);

        LocalDate testWindowStart = today.minusDays(generator.testWindowDays() - 1L);
        long tests = activityRepository.countByUserIdAndTypeAndActivityDateBetween(
                userId, ActivityType.TEST, testWindowStart, today);
        long docs = activityRepository.countByUserIdAndTypeAndActivityDateBetween(
                userId, ActivityType.DOCUMENTATION, testWindowStart, today);

        Map<ActivityType, BigDecimal> pointsByType = new EnumMap<>(ActivityType.class);
        for (TypeAggregate aggregate : activityRepository.aggregateByType(
                userId, today.minusDays(SCORE_WINDOW_DAYS - 1L), today)) {
            pointsByType.put(ActivityType.valueOf(aggregate.getType()),
                    aggregate.getPoints() == null ? BigDecimal.ZERO : aggregate.getPoints());
        }
        double entropy = devScoreCalculator.shannonEntropy(pointsByType);

        Instant monthAgo = today.minusDays(generator.newTechnologyWindowDays())
                .atStartOfDay(user.zoneId()).toInstant();
        long newTechnologies = userTechnologyRepository
                .countByUserIdAndFirstUsedAtAfter(userId, monthAgo);

        return new ChallengeGenerator.Context(daysSinceLastActivity, streak, hasActivityToday,
                tests, docs, entropy, newTechnologies);
    }

    private ChallengeTemplate pickTemplate(ChallengeTrigger trigger, Long excludeTemplateId) {
        List<ChallengeTemplate> candidates =
                new ArrayList<>(templateRepository.findByTriggerAndActiveTrue(trigger));
        if (excludeTemplateId != null && candidates.size() > 1) {
            candidates.removeIf(template -> template.getId().equals(excludeTemplateId));
        }
        if (candidates.isEmpty()) {
            candidates = new ArrayList<>(templateRepository.findByTriggerAndActiveTrue(ChallengeTrigger.DEFAULT));
        }
        if (candidates.isEmpty()) {
            candidates = new ArrayList<>(templateRepository.findByActiveTrue());
        }
        if (candidates.isEmpty()) {
            throw new NotFoundException(ErrorCode.CHALLENGE_NOT_FOUND,
                    "Nenhum template de desafio cadastrado");
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    /**
     * Sentinela para "nunca teve atividade" — grande o bastante para disparar sempre
     * o gatilho de inatividade, mas plausivel se algum lugar chegar a exibi-lo ou
     * some-lo, ao contrario de Integer.MAX_VALUE (que jah vazou como "2147483647 dias"
     * no texto do desafio de usuarios novos).
     */
    private static final int NEVER_ACTIVE_SENTINEL_DAYS = 9_999;

    private int daysSinceLastActivity(User user, LocalDate today) {
        Optional<Activity> last = activityRepository.findFirstByUserIdOrderByOccurredAtDesc(user.getId());
        if (last.isEmpty()) {
            return NEVER_ACTIVE_SENTINEL_DAYS;
        }
        LocalDate lastDate = last.get().getActivityDate();
        return (int) ChronoUnit.DAYS.between(lastDate, today);
    }

    private int currentStreak(List<LocalDate> activeDatesDesc, LocalDate today) {
        if (activeDatesDesc.isEmpty()) {
            return 0;
        }
        java.util.Set<LocalDate> dates = new java.util.HashSet<>(activeDatesDesc);
        int streak = 0;
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private String lastProjectName(User user) {
        return activityRepository.findFirstByUserIdOrderByOccurredAtDesc(user.getId())
                .map(Activity::getProject)
                .map(project -> project.getName())
                .orElse(null);
    }

    private DailyChallenge requireChallenge(Long userId, Long challengeId) {
        return dailyChallengeRepository.findByIdAndUserId(challengeId, userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CHALLENGE_NOT_FOUND));
    }
}
