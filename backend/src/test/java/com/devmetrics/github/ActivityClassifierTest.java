package com.devmetrics.github;

import com.devmetrics.activity.domain.ActivityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityClassifierTest {

    private final ActivityClassifier classifier = new ActivityClassifier();

    @ParameterizedTest(name = "\"{0}\" e classificado como {1}")
    @CsvSource({
            "'feat: adiciona endpoint de login', FEATURE",
            "'feat(auth): adiciona endpoint', FEATURE",
            "'feat!: muda contrato da API', FEATURE",
            "'fix: corrige calculo de pontos', BUG_FIX",
            "'hotfix: erro em producao', BUG_FIX",
            "'test: cobre o servico de score', TEST",
            "'docs: atualiza README', DOCUMENTATION",
            "'refactor: extrai metodo', REFACTOR",
            "'perf: reduz consultas', REFACTOR",
            "'ci: adiciona workflow', DEPLOY",
            "'chore: atualiza dependencias', COMMIT"
    })
    @DisplayName("prefixo Conventional Commits decide o tipo")
    void conventionalCommitPrefixWins(String message, ActivityType expected) {
        ActivityClassifier.Classification classification = classifier.classify(message, List.of());

        assertThat(classification.type()).isEqualTo(expected);
        assertThat(classification.classifiedBy()).isEqualTo("CONVENTIONAL");
    }

    @Test
    @DisplayName("commit que so altera arquivos de teste e classificado como teste")
    void allTestFilesMeansTest() {
        ActivityClassifier.Classification classification = classifier.classify(
                "ajustes gerais",
                List.of("src/test/java/com/devmetrics/ScoreTest.java",
                        "src/test/java/com/devmetrics/PointsTest.java"));

        assertThat(classification.type()).isEqualTo(ActivityType.TEST);
        assertThat(classification.classifiedBy()).isEqualTo("PATH");
    }

    @Test
    @DisplayName("arquivos de teste em outras linguagens tambem sao reconhecidos")
    void testFilesInOtherLanguages() {
        assertThat(classifier.classify("wip", List.of("tests/test_score.py")).type())
                .isEqualTo(ActivityType.TEST);
        assertThat(classifier.classify("wip", List.of("src/score.spec.ts")).type())
                .isEqualTo(ActivityType.TEST);
        assertThat(classifier.classify("wip", List.of("__tests__/score.js")).type())
                .isEqualTo(ActivityType.TEST);
    }

    @Test
    @DisplayName("commit que so altera markdown e documentacao")
    void allMarkdownFilesMeansDocumentation() {
        ActivityClassifier.Classification classification =
                classifier.classify("ajustes", List.of("README.md", "docs/arquitetura.md"));

        assertThat(classification.type()).isEqualTo(ActivityType.DOCUMENTATION);
        assertThat(classification.classifiedBy()).isEqualTo("PATH");
    }

    @Test
    @DisplayName("mexer em Dockerfile ou workflow conta como deploy")
    void infrastructureFilesMeanDeploy() {
        assertThat(classifier.classify("ajustes", List.of("Dockerfile", "src/App.java")).type())
                .isEqualTo(ActivityType.DEPLOY);
        assertThat(classifier.classify("ajustes", List.of(".github/workflows/ci.yml")).type())
                .isEqualTo(ActivityType.DEPLOY);
    }

    @Test
    @DisplayName("palavra-chave em portugues, com ou sem acento, classifica o commit")
    void keywordsWorkInPortuguese() {
        assertThat(classifier.classify("corrige bug no calculo", List.of()).type())
                .isEqualTo(ActivityType.BUG_FIX);
        assertThat(classifier.classify("Implementa tela de histórico", List.of()).type())
                .isEqualTo(ActivityType.FEATURE);
        assertThat(classifier.classify("atualiza documentação da API", List.of()).type())
                .isEqualTo(ActivityType.DOCUMENTATION);
    }

    @Test
    @DisplayName("sem nenhum sinal, o commit continua sendo um commit")
    void fallbackIsPlainCommit() {
        ActivityClassifier.Classification classification = classifier.classify("wip", List.of());

        assertThat(classification.type()).isEqualTo(ActivityType.COMMIT);
        assertThat(classification.classifiedBy()).isEqualTo("FALLBACK");
    }

    @Test
    @DisplayName("mensagem nula ou vazia nao quebra o classificador")
    void nullMessageIsSafe() {
        assertThat(classifier.classify(null, null).type()).isEqualTo(ActivityType.COMMIT);
        assertThat(classifier.classify("", List.of()).type()).isEqualTo(ActivityType.COMMIT);
    }

    @Test
    @DisplayName("so busca os arquivos do commit quando a mensagem nao decide")
    void fileInspectionOnlyWhenMessageIsInconclusive() {
        assertThat(classifier.needsFileInspection("feat: novo endpoint")).isFalse();
        assertThat(classifier.needsFileInspection("corrige bug")).isFalse();
        assertThat(classifier.needsFileInspection("wip")).isTrue();
    }

    @Test
    @DisplayName("a primeira linha decide, mesmo com corpo longo no commit")
    void onlyFirstLineIsUsedForConventionalCommits() {
        String message = "fix: corrige validacao\n\nO problema acontecia quando o token expirava.";

        assertThat(classifier.classify(message, List.of()).type()).isEqualTo(ActivityType.BUG_FIX);
    }
}
