package com.devmetrics.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicProfileIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("perfil so fica publico quando o dono ativa, e nunca expoe e-mail")
    void profileIsOptInAndHidesEmail() throws Exception {
        String token = register();
        String username = "dev-" + UUID.randomUUID().toString().substring(0, 8);
        asJson(token, post("/api/v1/activities"), """
                {"type":"TEST","title":"primeiro teste"}""").andExpect(status().isCreated());

        // sem username nao da para publicar
        asJson(token, patch("/api/v1/users/me"), """
                {"publicProfile":true}""").andExpect(status().isBadRequest());

        asJson(token, patch("/api/v1/users/me"), """
                {"username":"%s"}""".formatted(username)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/public/u/" + username)).andExpect(status().isNotFound());

        asJson(token, patch("/api/v1/users/me"), """
                {"publicProfile":true}""")
                .andExpect(jsonPath("$.data.publicProfile").value(true));

        mvc.perform(get("/api/v1/public/u/" + username.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.totalActivities").value(1))
                .andExpect(jsonPath("$.data.calendar.days.length()").value(365))
                .andExpect(content().string(not(containsString("@teste.com"))));

        mvc.perform(get("/api/v1/public/u/" + username + "/badge.svg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/svg+xml")))
                .andExpect(content().string(containsString("<svg")))
                .andExpect(content().string(containsString("@" + username)));
    }

    @Test
    @DisplayName("username e unico sem diferenciar maiusculas")
    void usernameIsUnique() throws Exception {
        String username = "unico-" + UUID.randomUUID().toString().substring(0, 8);
        asJson(register(), patch("/api/v1/users/me"), """
                {"username":"%s"}""".formatted(username)).andExpect(status().isOk());
        asJson(register(), patch("/api/v1/users/me"), """
                {"username":"%s"}""".formatted(username.toUpperCase()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USERNAME_ALREADY_USED"));
    }

    @Test
    @DisplayName("perfil inexistente e badge inexistente respondem 404")
    void unknownProfileIs404() throws Exception {
        mvc.perform(get("/api/v1/public/u/ninguem-aqui")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/public/u/ninguem-aqui/badge.svg")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("login demo responde 404 quando o modo demo esta desligado")
    void demoDisabledByDefault() throws Exception {
        mvc.perform(post("/api/v1/auth/demo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("DEMO_DISABLED"));
    }
}
