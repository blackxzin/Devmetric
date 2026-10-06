package com.devmetrics.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A garantia de seguranca mais importante do sistema: um usuario nunca le nem altera
 * dado de outro. Recurso alheio responde 404 (e nao 403) para nao revelar que existe.
 */
class UserIsolationIntegrationTest extends IntegrationTest {

    @Test
    @DisplayName("usuario B nao enxerga nem altera projeto e atividade do usuario A")
    void otherUsersDataIsInvisible() throws Exception {
        String alice = register();
        String bob = register();

        long projectId = idFrom(asJson(alice, post("/api/v1/projects"), """
                {"name":"projeto-da-alice"}""").andExpect(status().isCreated()));
        long activityId = idFrom(asJson(alice, post("/api/v1/activities"), """
                {"type":"TEST","title":"teste da alice","projectId":%d}""".formatted(projectId))
                .andExpect(status().isCreated()));

        as(alice, get("/api/v1/activities/" + activityId)).andExpect(status().isOk());

        as(bob, get("/api/v1/projects/" + projectId)).andExpect(status().isNotFound());
        as(bob, get("/api/v1/activities/" + activityId)).andExpect(status().isNotFound());
        asJson(bob, put("/api/v1/activities/" + activityId), """
                {"type":"BUG_FIX","title":"invadido"}""").andExpect(status().isNotFound());
        as(bob, post("/api/v1/projects/" + projectId + "/archive")).andExpect(status().isNotFound());
        as(bob, delete("/api/v1/activities/" + activityId)).andExpect(status().isNotFound());
        as(bob, delete("/api/v1/projects/" + projectId)).andExpect(status().isNotFound());

        as(bob, get("/api/v1/activities")).andExpect(jsonPath("$.data.length()").value(0));
        as(bob, get("/api/v1/projects")).andExpect(jsonPath("$.data.length()").value(0));

        // continua tudo la para a dona
        as(alice, get("/api/v1/activities/" + activityId)).andExpect(jsonPath("$.data.title").value("teste da alice"));
        as(alice, get("/api/v1/projects/" + projectId)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("rotas privadas exigem token")
    void privateRoutesRequireToken() throws Exception {
        mvc.perform(get("/api/v1/activities")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/dashboard/summary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/gitlab/status")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("dashboard do usuario reflete a atividade registrada")
    void dashboardReflectsActivity() throws Exception {
        String token = register();
        asJson(token, post("/api/v1/activities"), """
                {"type":"DOCUMENTATION","title":"README"}""").andExpect(status().isCreated());

        as(token, get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totals.activities").value(1))
                .andExpect(jsonPath("$.data.streak.current").value(1));
    }
}
