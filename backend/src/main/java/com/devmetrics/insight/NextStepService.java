package com.devmetrics.insight;

import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.activity.repository.ActivityRepository;
import com.devmetrics.insight.dto.NextStepResponse;
import com.devmetrics.project.repository.ProjectRepository;
import com.devmetrics.technology.domain.TechnologyCategory;
import com.devmetrics.technology.domain.UserTechnology;
import com.devmetrics.technology.repository.UserTechnologyRepository;
import com.devmetrics.user.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * "Proximo Passo": olha o que o usuario ja usa e sugere a proxima tecnologia.
 *
 * Sem IA. Sao regras de co-ocorrencia escritas a partir de como stacks reais evoluem:
 * quem escreve backend em Java acaba precisando de container; quem tem container
 * acaba precisando de CI; quem tem CI acaba precisando de teste automatizado.
 *
 * A primeira regra satisfeita vence; as demais viram alternativas.
 */
@Service
public class NextStepService {

    private static final int TEST_WINDOW_DAYS = 30;

    private record Rule(
            Predicate<Context> applies,
            String technology,
            TechnologyCategory category,
            String reason,
            String firstStep,
            int estimatedMinutes
    ) {
    }

    record Context(
            Set<String> technologySlugs,
            Set<TechnologyCategory> categories,
            List<String> dominantTechnologies,
            long backendLanguageCount,
            long projectCount,
            long testsInWindow
    ) {

        boolean has(String slug) {
            return technologySlugs.contains(slug);
        }

        boolean hasCategory(TechnologyCategory category) {
            return categories.contains(category);
        }
    }

    private static final Set<String> BACKEND_LANGUAGES =
            Set.of("java", "kotlin", "c", "c-sharp", "csharp", "python", "go", "php", "ruby", "rust");

    private final List<Rule> rules = List.of(
            new Rule(context -> context.backendLanguageCount() > 0 && !context.has("docker"),
                    "Docker", TechnologyCategory.TOOL,
                    "Voce ja escreve backend, mas nenhum projeto seu usa containers. "
                            + "Docker e o que faz o projeto rodar igual na sua maquina e na de quem avalia.",
                    "Crie um Dockerfile para o seu projeto principal e rode a aplicacao com docker run.",
                    45),

            new Rule(context -> context.testsInWindow() == 0,
                    "Testes automatizados", TechnologyCategory.TESTING,
                    "Nos ultimos 30 dias voce nao registrou nenhum teste. "
                            + "E o item que mais pesa numa avaliacao tecnica de codigo.",
                    "Escreva um teste para a funcao mais importante do seu projeto atual.",
                    30),

            new Rule(context -> context.has("docker") && !context.has("github-actions"),
                    "GitHub Actions", TechnologyCategory.CLOUD,
                    "Voce ja usa Docker. O passo natural e automatizar build e testes a cada push.",
                    "Crie .github/workflows/ci.yml rodando o build e os testes do projeto.",
                    40),

            new Rule(context -> context.backendLanguageCount() > 0
                    && !context.hasCategory(TechnologyCategory.DATABASE),
                    "PostgreSQL", TechnologyCategory.DATABASE,
                    "Seus projetos backend ainda nao tocam em banco de dados relacional. "
                            + "Persistencia e o assunto mais cobrado em entrevista de backend.",
                    "Suba um Postgres com docker compose e persista uma entidade do seu projeto.",
                    60),

            new Rule(context -> context.projectCount() >= 2
                    && !context.hasCategory(TechnologyCategory.CLOUD),
                    "Deploy em nuvem", TechnologyCategory.CLOUD,
                    "Voce tem projetos prontos, mas nenhum publicado. "
                            + "Um link funcionando no curriculo vale mais que um repositorio a mais.",
                    "Publique um projeto seu em Render, Railway ou Fly.io e coloque o link no README.",
                    50),

            new Rule(context -> context.technologySlugs().size() >= 3
                    && context.dominantTechnologies().size() <= 2,
                    "Uma segunda linguagem", TechnologyCategory.LANGUAGE,
                    "Sua atividade esta concentrada em poucas tecnologias. "
                            + "Aprender uma segunda linguagem melhora a forma como voce usa a primeira.",
                    "Reescreva um exercicio pequeno que voce ja fez, em outra linguagem.",
                    60),

            new Rule(context -> !context.has("swagger") && !context.has("openapi"),
                    "OpenAPI / Swagger", TechnologyCategory.TOOL,
                    "Nenhum projeto seu documenta a propria API. "
                            + "Documentacao e o que permite alguem usar seu codigo sem te perguntar nada.",
                    "Adicione Swagger ao seu projeto de API e documente tres endpoints.",
                    35)
    );

    private final UserTechnologyRepository userTechnologyRepository;
    private final ProjectRepository projectRepository;
    private final ActivityRepository activityRepository;

    public NextStepService(UserTechnologyRepository userTechnologyRepository,
                           ProjectRepository projectRepository,
                           ActivityRepository activityRepository) {
        this.userTechnologyRepository = userTechnologyRepository;
        this.projectRepository = projectRepository;
        this.activityRepository = activityRepository;
    }

    @Transactional(readOnly = true)
    public NextStepResponse nextStep(User user) {
        Context context = buildContext(user);

        List<NextStepResponse.Suggestion> matches = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.applies().test(context)) {
                matches.add(new NextStepResponse.Suggestion(rule.technology(), rule.category().name(),
                        rule.reason(), rule.firstStep(), rule.estimatedMinutes()));
            }
        }

        NextStepResponse.Suggestion main = matches.isEmpty() ? fallback() : matches.get(0);
        List<NextStepResponse.Suggestion> alternatives = matches.size() <= 1
                ? List.of()
                : matches.subList(1, Math.min(matches.size(), 4));

        return new NextStepResponse(main,
                new NextStepResponse.BasedOn(context.dominantTechnologies(), context.projectCount(),
                        context.technologySlugs().size(), context.categories().size()),
                alternatives);
    }

    Context buildContext(User user) {
        Long userId = user.getId();
        LocalDate today = LocalDate.now(user.zoneId());

        List<UserTechnology> technologies = userTechnologyRepository.findAllByUser(userId);
        Set<String> slugs = new HashSet<>();
        Set<TechnologyCategory> categories = new HashSet<>();
        List<String> dominant = new ArrayList<>();

        for (UserTechnology userTechnology : technologies) {
            slugs.add(userTechnology.getTechnology().getSlug());
            categories.add(userTechnology.getTechnology().getCategory());
            if (dominant.size() < 3) {
                dominant.add(userTechnology.getTechnology().getName());
            }
        }

        long backendLanguages = technologies.stream()
                .filter(userTechnology ->
                        userTechnology.getTechnology().getCategory() == TechnologyCategory.LANGUAGE)
                .filter(userTechnology -> BACKEND_LANGUAGES
                        .contains(userTechnology.getTechnology().getSlug().toLowerCase(Locale.ROOT)))
                .count();

        long tests = activityRepository.countByUserIdAndTypeAndActivityDateBetween(
                userId, ActivityType.TEST, today.minusDays(TEST_WINDOW_DAYS - 1L), today);

        return new Context(slugs, categories, dominant, backendLanguages,
                projectRepository.countByUserId(userId), tests);
    }

    private NextStepResponse.Suggestion fallback() {
        return new NextStepResponse.Suggestion("Aprofundar o que voce ja usa", "OTHER",
                "Sua stack ja cobre linguagem, banco, container e testes. "
                        + "O proximo ganho vem de profundidade, nao de mais uma ferramenta.",
                "Pegue o projeto de que voce mais gosta e melhore a cobertura de testes dele.",
                60);
    }
}
