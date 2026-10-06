package com.devmetrics.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GitHubWebhookIntegrationTest extends IntegrationTest {

    private static final String PUSH = """
            {"sender":{"id":987654321},"repository":{"id":1,"full_name":"x/y"},
             "commits":[{"id":"abc","message":"test: cobre o login","distinct":true,
                         "author":{"username":"alguem"},"added":["src/test/LoginTest.java"]}]}""";

    @Test
    @DisplayName("webhook sem assinatura ou com assinatura errada e recusado")
    void rejectsBadSignature() throws Exception {
        mvc.perform(post("/api/v1/github/webhook").header("X-GitHub-Event", "push")
                        .contentType(MediaType.APPLICATION_JSON).content(PUSH))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/github/webhook").header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", "sha256=" + "0".repeat(64))
                        .contentType(MediaType.APPLICATION_JSON).content(PUSH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("webhook assinado de quem nao usa o DevMetrics e aceito e ignorado")
    void acceptsSignedPushFromUnknownSender() throws Exception {
        mvc.perform(post("/api/v1/github/webhook").header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", sign(PUSH))
                        .contentType(MediaType.APPLICATION_JSON).content(PUSH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activitiesCreated").value(0));
    }

    @Test
    @DisplayName("evento ping assinado responde 200")
    void acceptsPing() throws Exception {
        String ping = "{\"zen\":\"Keep it logically awesome.\"}";
        mvc.perform(post("/api/v1/github/webhook").header("X-GitHub-Event", "ping")
                        .header("X-Hub-Signature-256", sign(ping))
                        .contentType(MediaType.APPLICATION_JSON).content(ping))
                .andExpect(status().isOk());
    }

    private static String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("segredo-de-teste".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
