package com.devmetrics.activity;

import com.devmetrics.activity.domain.Activity;
import com.devmetrics.activity.domain.ActivitySource;
import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.activity.dto.ActivityResponse;
import com.devmetrics.activity.dto.CreateActivityRequest;
import com.devmetrics.activity.dto.UpdateActivityRequest;
import com.devmetrics.activity.repository.ActivityRepository;
import com.devmetrics.activity.repository.ActivitySpecifications;
import com.devmetrics.common.event.ActivityRecordedEvent;
import com.devmetrics.common.exception.BusinessException;
import com.devmetrics.common.exception.ErrorCode;
import com.devmetrics.common.exception.NotFoundException;
import com.devmetrics.config.AppProperties;
import com.devmetrics.project.domain.Project;
import com.devmetrics.project.repository.ProjectRepository;
import com.devmetrics.scoring.DailyStatsService;
import com.devmetrics.scoring.PointsCalculator;
import com.devmetrics.scoring.ScoringRuleService;
import com.devmetrics.scoring.domain.EffectiveRule;
import com.devmetrics.technology.TechnologyService;
import com.devmetrics.technology.domain.Technology;
import com.devmetrics.technology.repository.UserTechnologyRepository;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserTechnologyRepository userTechnologyRepository;
    private final TechnologyService technologyService;
    private final ScoringRuleService scoringRuleService;
    private final PointsCalculator pointsCalculator;
    private final DailyStatsService dailyStatsService;
    private final ApplicationEventPublisher eventPublisher;
    private final double newTechnologyBonus;

    public ActivityService(ActivityRepository activityRepository,
                           ProjectRepository projectRepository,
                           UserRepository userRepository,
                           UserTechnologyRepository userTechnologyRepository,
                           TechnologyService technologyService,
                           ScoringRuleService scoringRuleService,
                           PointsCalculator pointsCalculator,
                           DailyStatsService dailyStatsService,
                           AppProperties properties,
                           ApplicationEventPublisher eventPublisher) {
        this.activityRepository = activityRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.userTechnologyRepository = userTechnologyRepository;
        this.technologyService = technologyService;
        this.scoringRuleService = scoringRuleService;
        this.pointsCalculator = pointsCalculator;
        this.dailyStatsService = dailyStatsService;
        this.eventPublisher = eventPublisher;
        this.newTechnologyBonus = properties.scoring().newTechnologyBonus();
    }

    // ---------------------------------------------------------------- leitura

    @Transactional(readOnly = true)
    public Page<ActivityResponse> search(Long userId, ActivityType type, ActivitySource source,
                                         Long projectId, LocalDate from, LocalDate to, Pageable pageable) {
        Specification<Activity> specification = Specification.where(ActivitySpecifications.ownedBy(userId))
                .and(ActivitySpecifications.ofType(type))
                .and(ActivitySpecifications.ofSource(source))
                .and(ActivitySpecifications.ofProject(projectId))
                .and(ActivitySpecifications.from(from))
                .and(ActivitySpecifications.to(to));
        return activityRepository.findAll(specification, pageable).map(ActivityResponse::from);
    }

    @Transactional(readOnly = true)
    public ActivityResponse get(Long userId, Long activityId) {
        return ActivityResponse.from(requireActivity(userId, activityId));
    }

    // ---------------------------------------------------------------- escrita

    @Transactional
    public ActivityResponse create(Long userId, CreateActivityRequest request) {
        User user = requireUser(userId);
        Instant occurredAt = validateMoment(request.occurredAt());
        Project project = resolveProject(userId, request.projectId());
        Technology technology = request.technologyId() == null
                ? null
                : technologyService.requireTechnology(request.technologyId());

        Activity activity = record(user, request.type(), ActivitySource.MANUAL, request.title(),
                request.description(), occurredAt, project, technology, null, Map.of());
        eventPublisher.publishEvent(new ActivityRecordedEvent(userId));
        return ActivityResponse.from(activity);
    }

    /**
     * Ponto unico de criacao de atividade: manual, sync do GitHub e projeto novo passam por aqui.
     * Calcula os pontos, registra o uso da tecnologia e atualiza o agregado diario.
     *
     * @return a atividade criada, ou null quando o externalId ja existia (deduplicacao do sync).
     */
    @Transactional
    public Activity record(User user, ActivityType type, ActivitySource source, String title,
                           String description, Instant occurredAt, Project project,
                           Technology technology, String externalId, Map<String, Object> metadata) {
        if (externalId != null && activityRepository
                .existsByUserIdAndSourceAndExternalId(user.getId(), source, externalId)) {
            return null;
        }

        LocalDate activityDate = occurredAt.atZone(user.zoneId()).toLocalDate();
        Activity activity = Activity.create(user, type, source, title, occurredAt, activityDate);
        activity.describe(description);
        activity.attachProject(project);
        activity.attachTechnology(technology);
        activity.withExternalId(externalId);
        if (metadata != null) {
            metadata.forEach(activity::putMetadata);
        }

        boolean newTechnology = technologyService.registerUsage(user, technology, occurredAt);
        int occurrenceOfDay = (int) activityRepository
                .countByUserIdAndTypeAndActivityDate(user.getId(), type, activityDate) + 1;
        EffectiveRule rule = scoringRuleService.ruleFor(user.getId(), type);
        BigDecimal points = pointsCalculator.calculate(rule, occurrenceOfDay, newTechnology, newTechnologyBonus);

        activity.applyPoints(points);
        if (newTechnology && technology != null) {
            activity.putMetadata("newTechnology", technology.getName());
        }
        activity.putMetadata("occurrenceOfDay", occurrenceOfDay);

        Activity saved = activityRepository.save(activity);
        dailyStatsService.refreshDay(user, activityDate);
        return saved;
    }

    @Transactional
    public ActivityResponse update(Long userId, Long activityId, UpdateActivityRequest request) {
        Activity activity = requireActivity(userId, activityId);
        User user = activity.getUser();
        LocalDate previousDate = activity.getActivityDate();

        Instant occurredAt = activity.isReadOnly()
                ? activity.getOccurredAt()
                : validateMoment(request.occurredAt());
        LocalDate newDate = occurredAt.atZone(user.zoneId()).toLocalDate();

        activity.updateContent(request.type(), request.title(), request.description(), occurredAt, newDate);
        activity.attachProject(resolveProject(userId, request.projectId()));
        if (request.technologyId() != null) {
            Technology technology = technologyService.requireTechnology(request.technologyId());
            activity.attachTechnology(technology);
            technologyService.registerUsage(user, technology, occurredAt);
        }

        LocalDate from = previousDate.isBefore(newDate) ? previousDate : newDate;
        LocalDate to = previousDate.isBefore(newDate) ? newDate : previousDate;
        recalculate(user, from, to);

        return ActivityResponse.from(activity);
    }

    @Transactional
    public void delete(Long userId, Long activityId) {
        Activity activity = requireActivity(userId, activityId);
        if (activity.isReadOnly()) {
            throw new BusinessException(ErrorCode.READ_ONLY_RESOURCE,
                    "Atividade importada do GitHub nao pode ser removida");
        }
        User user = activity.getUser();
        LocalDate date = activity.getActivityDate();
        activityRepository.delete(activity);
        activityRepository.flush();
        recalculate(user, date, date);
    }

    /**
     * Reprocessa os pontos de um periodo aplicando as regras atuais.
     * Necessario porque activities.points e congelado na criacao: mudar uma regra
     * nao pode reescrever o passado sem um pedido explicito.
     *
     * @return quantidade de atividades reprocessadas
     */
    @Transactional
    public int recalculate(User user, LocalDate from, LocalDate to) {
        List<Activity> activities = activityRepository
                .findByUserIdAndActivityDateBetweenOrderByOccurredAtAsc(user.getId(), from, to);

        Instant rangeStart = from.atStartOfDay(user.zoneId()).toInstant();
        Set<Long> seenTechnologies = new HashSet<>();
        userTechnologyRepository.findAllByUser(user.getId()).stream()
                .filter(userTechnology -> userTechnology.getFirstUsedAt().isBefore(rangeStart))
                .forEach(userTechnology -> seenTechnologies.add(userTechnology.getTechnology().getId()));

        Map<ActivityType, Map<LocalDate, Integer>> counters = new HashMap<>();
        for (Activity activity : activities) {
            Map<LocalDate, Integer> perDate = counters
                    .computeIfAbsent(activity.getType(), key -> new HashMap<>());
            int occurrence = perDate.merge(activity.getActivityDate(), 1, Integer::sum);

            boolean newTechnology = false;
            if (activity.getTechnology() != null) {
                newTechnology = seenTechnologies.add(activity.getTechnology().getId());
            }

            EffectiveRule rule = scoringRuleService.ruleFor(user.getId(), activity.getType());
            activity.applyPoints(pointsCalculator.calculate(rule, occurrence, newTechnology, newTechnologyBonus));
        }
        activityRepository.saveAll(activities);
        activityRepository.flush();
        dailyStatsService.refreshRange(user, from, to);
        return activities.size();
    }

    // ---------------------------------------------------------------- apoio

    private Activity requireActivity(Long userId, Long activityId) {
        return activityRepository.findByIdAndUserId(activityId, userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    private Project resolveProject(Long userId, Long projectId) {
        if (projectId == null) {
            return null;
        }
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PROJECT_NOT_FOUND));
    }

    /**
     * Rejeita data no futuro e data muito antiga: as duas distorcem streak e score.
     */
    private Instant validateMoment(Instant occurredAt) {
        Instant moment = Optional.ofNullable(occurredAt).orElseGet(Instant::now);
        Instant now = Instant.now();
        if (moment.isAfter(now.plus(1, ChronoUnit.HOURS))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "A atividade nao pode estar no futuro");
        }
        if (moment.isBefore(now.minus(365, ChronoUnit.DAYS))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "A atividade nao pode ter mais de 1 ano");
        }
        return moment;
    }
}
