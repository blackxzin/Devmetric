package com.devmetrics.demo;

import com.devmetrics.achievement.AchievementService;
import com.devmetrics.activity.ActivityService;
import com.devmetrics.activity.domain.ActivitySource;
import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.config.AppProperties;
import com.devmetrics.project.ProjectService;
import com.devmetrics.project.dto.CreateProjectRequest;
import com.devmetrics.project.repository.ProjectRepository;
import com.devmetrics.scoring.ScoreService;
import com.devmetrics.technology.TechnologyService;
import com.devmetrics.technology.domain.Technology;
import com.devmetrics.technology.domain.TechnologyCategory;
import com.devmetrics.user.domain.User;
import com.devmetrics.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Conta demo com um ano de atividade plausivel, para recrutador ou visitante ver o
 * produto funcionando sem criar conta. Ligada so com DEMO_ENABLED=true.
 *
 * Os dados sao recriados no startup e toda madrugada: o que um visitante mudar some.
 * A senha e aleatoria e descartada; o unico jeito de entrar e POST /auth/demo.
 */
@Service
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);
    private static final ActivityType[] DAILY_TYPES = {
            ActivityType.COMMIT, ActivityType.COMMIT, ActivityType.COMMIT, ActivityType.FEATURE,
            ActivityType.BUG_FIX, ActivityType.TEST, ActivityType.TEST, ActivityType.DOCUMENTATION,
            ActivityType.REFACTOR, ActivityType.STUDY, ActivityType.CODE_REVIEW, ActivityType.PULL_REQUEST};

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectService projectService;
    private final ActivityService activityService;
    private final TechnologyService technologyService;
    private final ScoreService scoreService;
    private final AchievementService achievementService;

    public DemoDataService(AppProperties properties,
                           UserRepository userRepository,
                           ProjectRepository projectRepository,
                           PasswordEncoder passwordEncoder,
                           ProjectService projectService,
                           ActivityService activityService,
                           TechnologyService technologyService,
                           ScoreService scoreService,
                           AchievementService achievementService) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
        this.projectService = projectService;
        this.activityService = activityService;
        this.technologyService = technologyService;
        this.scoreService = scoreService;
        this.achievementService = achievementService;
    }

    public boolean enabled() {
        return properties.demo() != null && properties.demo().enabled();
    }

    public String email() {
        return properties.demo().email();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 4 * * *")
    public void reset() {
        if (!enabled()) {
            return;
        }
        long start = System.currentTimeMillis();
        userRepository.findByEmailIgnoreCase(email()).ifPresent(userRepository::delete);
        userRepository.flush();
        Long userId = seed(new Random(42));
        achievementService.evaluate(userId);
        log.info("Conta demo recriada em {} ms", System.currentTimeMillis() - start);
    }

    Long seed(Random random) {
        User user = User.create(email(), passwordEncoder.encode(UUID.randomUUID().toString()),
                "Dev Demo", "America/Sao_Paulo");
        user.changeUsername("demo");
        user.setPublicProfile(true);
        userRepository.save(user);

        LocalDate today = LocalDate.now(user.zoneId());
        Technology java = technologyService.findOrCreate("Java", TechnologyCategory.LANGUAGE);
        Technology spring = technologyService.findOrCreate("Spring Boot", TechnologyCategory.FRAMEWORK);
        Technology postgres = technologyService.findOrCreate("PostgreSQL", TechnologyCategory.DATABASE);
        Technology docker = technologyService.findOrCreate("Docker", TechnologyCategory.TOOL);
        Technology typescript = technologyService.findOrCreate("TypeScript", TechnologyCategory.LANGUAGE);
        Technology react = technologyService.findOrCreate("React", TechnologyCategory.FRAMEWORK);
        Technology junit = technologyService.findOrCreate("JUnit", TechnologyCategory.TESTING);

        record DemoProject(String name, String description, int startedDaysAgo, List<Technology> stack) {
        }
        List<DemoProject> demoProjects = List.of(
                new DemoProject("api-financas", "API REST de controle financeiro pessoal", 340,
                        List.of(java, spring, postgres, junit)),
                new DemoProject("portfolio-web", "Portfolio pessoal em React", 200, List.of(typescript, react)),
                new DemoProject("devmetrics", "Este projeto: metricas reais de evolucao", 90,
                        List.of(java, spring, postgres, docker, junit)));

        for (DemoProject demo : demoProjects) {
            var response = projectService.create(user.getId(), new CreateProjectRequest(demo.name(),
                    demo.description(), today.minusDays(demo.startedDaysAgo()), null,
                    demo.stack().stream().map(Technology::getId).toList()));
            var project = projectRepository.findById(response.id()).orElseThrow();

            for (int daysAgo = demo.startedDaysAgo() - 1; daysAgo >= 0; daysAgo--) {
                // Mais ativo nos dias uteis, com pausas de verdade: o calendario nao fica chapado.
                LocalDate date = today.minusDays(daysAgo);
                boolean weekend = date.getDayOfWeek().getValue() >= 6;
                if (random.nextDouble() < (weekend ? 0.75 : 0.35)) {
                    continue;
                }
                int count = 1 + random.nextInt(3);
                for (int i = 0; i < count; i++) {
                    ActivityType type = DAILY_TYPES[random.nextInt(DAILY_TYPES.length)];
                    Technology technology = demo.stack().get(random.nextInt(demo.stack().size()));
                    Instant occurredAt = date.atTime(LocalTime.of(9 + random.nextInt(12), random.nextInt(60)))
                            .atZone(user.zoneId()).toInstant();
                    if (occurredAt.isAfter(Instant.now())) {
                        occurredAt = Instant.now();
                    }
                    activityService.record(user, type, ActivitySource.MANUAL,
                            type.label() + " em " + demo.name(), null, occurredAt, project, technology,
                            null, Map.of("demo", true));
                }
            }
        }

        // Um snapshot por semana para o grafico de historico ter linha.
        for (int weeksAgo = 52; weeksAgo >= 0; weeksAgo--) {
            scoreService.snapshot(user, today.minusWeeks(weeksAgo));
        }
        return user.getId();
    }
}
