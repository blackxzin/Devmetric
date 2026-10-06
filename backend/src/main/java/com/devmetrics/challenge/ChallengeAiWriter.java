package com.devmetrics.challenge;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.devmetrics.challenge.domain.ChallengeTrigger;
import com.devmetrics.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reescreve o desafio do dia com o Claude, usando o mesmo contexto que as regras ja montam.
 *
 * As regras continuam decidindo O QUE sugerir (gatilho + template); a IA so deixa o texto
 * especifico para a stack e o projeto do usuario. Qualquer falha (sem chave, timeout,
 * recusa, resposta estranha) devolve vazio e o texto das regras e usado: a IA nunca
 * pode quebrar o dashboard.
 */
@Component
public class ChallengeAiWriter {

    private static final Logger log = LoggerFactory.getLogger(ChallengeAiWriter.class);
    private static final int MAX_LENGTH = 400;

    private static final String SYSTEM = """
            Voce escreve o "Desafio de Hoje" de uma plataforma que ajuda devs em inicio de carreira
            a manter constancia. Recebe um desafio base escolhido por regras e o contexto do dev.
            Reescreva o desafio para ficar concreto para a stack e o projeto dele.
            Regras: portugues do Brasil, no maximo 2 frases, tarefa pequena (15 a 45 minutos),
            termine com o tempo estimado entre parenteses. Tom de convite, nunca de cobranca;
            nunca mencione dias sem atividade nem prazos. Responda so com o texto do desafio.""";

    private final AnthropicClient client;
    private final String model;

    public ChallengeAiWriter(AppProperties properties) {
        AppProperties.Ai ai = properties.ai();
        if (ai != null && ai.isConfigured()) {
            this.client = AnthropicOkHttpClient.builder()
                    .apiKey(ai.anthropicApiKey())
                    // Roda dentro do GET /challenges/today: melhor cair nas regras do que travar a tela.
                    .timeout(Duration.ofSeconds(20))
                    .maxRetries(1)
                    .build();
            this.model = ai.model();
        } else {
            this.client = null;
            this.model = null;
        }
    }

    public boolean enabled() {
        return client != null;
    }

    public Optional<String> rewrite(String ruleText, ChallengeTrigger trigger, ChallengeGenerator.Context context,
                                    List<String> technologies, String lastProject) {
        if (client == null) {
            return Optional.empty();
        }
        String prompt = """
                Desafio base (gatilho %s): %s

                Contexto do dev:
                - tecnologias mais usadas: %s
                - projeto mais recente: %s
                - sequencia atual: %d dias
                - testes nos ultimos 14 dias: %d
                - documentacoes nos ultimos 14 dias: %d
                - tecnologias novas no ultimo mes: %d""".formatted(
                trigger.name(), ruleText,
                technologies.isEmpty() ? "nenhuma registrada" : String.join(", ", technologies),
                lastProject == null ? "nenhum" : lastProject,
                context.currentStreak(), context.testsInWindow(), context.docsInWindow(),
                context.newTechnologiesInMonth());

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(2048L)
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .system(SYSTEM)
                .addUserMessage(prompt)
                .build();
        try {
            Message response = client.messages().create(params);
            if (!response.stopReason().map(StopReason.END_TURN::equals).orElse(false)) {
                return Optional.empty(); // recusa, corte por max_tokens etc.: fica o texto das regras
            }
            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(TextBlock::text)
                    .collect(Collectors.joining())
                    .strip();
            return text.isEmpty() || text.length() > MAX_LENGTH ? Optional.empty() : Optional.of(text);
        } catch (AnthropicException ex) {
            log.warn("Claude indisponivel para o desafio, usando texto das regras: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
