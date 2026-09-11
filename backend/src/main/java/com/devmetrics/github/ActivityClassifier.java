package com.devmetrics.github;

import com.devmetrics.activity.domain.ActivityType;
import com.devmetrics.common.util.TextNormalizer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Traduz um commit do GitHub em um tipo de atividade. Sem IA: heuristica deterministica
 * e testavel, aplicada nesta ordem (o primeiro match vence):
 *
 * 1. CONVENTIONAL - prefixo da mensagem no padrao Conventional Commits (feat:, fix:, test:...)
 * 2. PATH         - caminho dos arquivos alterados (tudo em src/test -> teste, tudo .md -> doc)
 * 3. KEYWORD      - palavras-chave na mensagem, sem acento e em minusculo
 * 4. FALLBACK     - COMMIT
 *
 * A origem da decisao vai para metadata.classifiedBy, entao a heuristica e auditavel
 * e o usuario pode corrigir o tipo manualmente depois.
 */
@Component
public class ActivityClassifier {

    public record Classification(ActivityType type, String classifiedBy) {
    }

    private static final Map<String, ActivityType> CONVENTIONAL_PREFIXES = Map.ofEntries(
            Map.entry("feat", ActivityType.FEATURE),
            Map.entry("feature", ActivityType.FEATURE),
            Map.entry("fix", ActivityType.BUG_FIX),
            Map.entry("bugfix", ActivityType.BUG_FIX),
            Map.entry("hotfix", ActivityType.BUG_FIX),
            Map.entry("test", ActivityType.TEST),
            Map.entry("tests", ActivityType.TEST),
            Map.entry("docs", ActivityType.DOCUMENTATION),
            Map.entry("doc", ActivityType.DOCUMENTATION),
            Map.entry("refactor", ActivityType.REFACTOR),
            Map.entry("perf", ActivityType.REFACTOR),
            Map.entry("style", ActivityType.REFACTOR),
            Map.entry("ci", ActivityType.DEPLOY),
            Map.entry("build", ActivityType.DEPLOY),
            Map.entry("deploy", ActivityType.DEPLOY),
            Map.entry("chore", ActivityType.COMMIT)
    );

    private static final Pattern CONVENTIONAL_PATTERN =
            Pattern.compile("^([a-zA-Z]+)(\\([^)]*\\))?!?:.*", Pattern.DOTALL);

    private static final Pattern TEST_FILE = Pattern.compile(
            "(^|/)(src/test/|tests?/|__tests__/|spec/).*"
                    + "|.*([._-](test|tests|spec)\\.[a-z0-9]+)$"
                    + "|.*[A-Za-z0-9]+(Test|Tests|IT|Spec)\\.[a-z0-9]+$"
                    + "|(^|/)test_[a-z0-9_]+\\.py$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DOC_FILE = Pattern.compile(
            "(^|/)docs?/.*|.*\\.(md|adoc|rst|txt)$", Pattern.CASE_INSENSITIVE);

    private static final Pattern INFRA_FILE = Pattern.compile(
            "(^|/)(Dockerfile|docker-compose(\\.[a-z]+)?\\.ya?ml|Procfile)$"
                    + "|(^|/)\\.github/workflows/.*"
                    + "|(^|/)(k8s|kubernetes|helm|terraform)/.*",
            Pattern.CASE_INSENSITIVE);

    private static final List<Map.Entry<Pattern, ActivityType>> KEYWORD_RULES = List.of(
            Map.entry(Pattern.compile(
                    "\\b(fix|fixes|fixed|corrige|corrigi|corrigido|corrigindo|bug|bugfix|resolve|resolvido|closes #|fixes #)\\b"),
                    ActivityType.BUG_FIX),
            Map.entry(Pattern.compile(
                    "\\b(test|teste|testes|testing|cobertura|coverage|unit test)\\b"),
                    ActivityType.TEST),
            Map.entry(Pattern.compile(
                    "\\b(doc|docs|documentacao|documentation|readme|javadoc|swagger)\\b"),
                    ActivityType.DOCUMENTATION),
            Map.entry(Pattern.compile(
                    "\\b(refactor|refatora|refatorando|refatoracao|cleanup|limpeza|renomeia|rename)\\b"),
                    ActivityType.REFACTOR),
            Map.entry(Pattern.compile(
                    "\\b(deploy|release|publica|dockeriza|docker|pipeline|ci/cd)\\b"),
                    ActivityType.DEPLOY),
            Map.entry(Pattern.compile(
                    "\\b(add|adiciona|adicionando|implementa|implementando|cria|criando|nova funcionalidade|feature|novo endpoint)\\b"),
                    ActivityType.FEATURE)
    );

    public Classification classify(String message, List<String> changedFiles) {
        Classification conventional = byConventionalCommit(message);
        if (conventional != null) {
            return conventional;
        }
        Classification byPath = byChangedFiles(changedFiles);
        if (byPath != null) {
            return byPath;
        }
        Classification byKeyword = byKeyword(message);
        if (byKeyword != null) {
            return byKeyword;
        }
        return new Classification(ActivityType.COMMIT, "FALLBACK");
    }

    /**
     * Diz se vale gastar uma requisicao extra buscando os arquivos do commit.
     * Se mensagem ja decidiu, nao vale.
     */
    public boolean needsFileInspection(String message) {
        return byConventionalCommit(message) == null && byKeyword(message) == null;
    }

    private Classification byConventionalCommit(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String firstLine = message.strip().lines().findFirst().orElse("").strip();
        var matcher = CONVENTIONAL_PATTERN.matcher(firstLine);
        if (!matcher.matches()) {
            return null;
        }
        ActivityType type = CONVENTIONAL_PREFIXES.get(matcher.group(1).toLowerCase(Locale.ROOT));
        return type == null ? null : new Classification(type, "CONVENTIONAL");
    }

    private Classification byChangedFiles(List<String> changedFiles) {
        if (changedFiles == null || changedFiles.isEmpty()) {
            return null;
        }
        if (changedFiles.stream().allMatch(file -> TEST_FILE.matcher(file).matches())) {
            return new Classification(ActivityType.TEST, "PATH");
        }
        if (changedFiles.stream().allMatch(file -> DOC_FILE.matcher(file).matches())) {
            return new Classification(ActivityType.DOCUMENTATION, "PATH");
        }
        if (changedFiles.stream().anyMatch(file -> INFRA_FILE.matcher(file).matches())) {
            return new Classification(ActivityType.DEPLOY, "PATH");
        }
        return null;
    }

    private Classification byKeyword(String message) {
        String normalized = TextNormalizer.normalize(message);
        if (normalized.isEmpty()) {
            return null;
        }
        for (Map.Entry<Pattern, ActivityType> rule : KEYWORD_RULES) {
            if (rule.getKey().matcher(normalized).find()) {
                return new Classification(rule.getValue(), "KEYWORD");
            }
        }
        return null;
    }
}
