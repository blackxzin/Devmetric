package com.devmetrics.gitlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GitLabServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("commit e do usuario quando e-mail ou nome do autor batem, sem diferenciar caixa")
    void matchesCommitAuthorByEmailOrName() throws Exception {
        JsonNode me = mapper.readTree("""
                {"username":"pedro","name":"Pedro Dev","email":"Pedro@Mail.com","commit_email":null}""");
        Set<String> identities = GitLabService.identities(me);

        assertThat(GitLabService.isMine(mapper.readTree("""
                {"author_email":"pedro@mail.com","author_name":"x"}"""), identities)).isTrue();
        assertThat(GitLabService.isMine(mapper.readTree("""
                {"author_email":"outro@mail.com","author_name":"pedro dev"}"""), identities)).isTrue();
        assertThat(GitLabService.isMine(mapper.readTree("""
                {"author_email":"outro@mail.com","author_name":"Outra Pessoa"}"""), identities)).isFalse();
    }

    @Test
    @DisplayName("URL da API aceita base com ou sem barra final")
    void apiRoot() {
        assertThat(GitLabApiClient.apiRoot("https://gitlab.com")).isEqualTo("https://gitlab.com/api/v4");
        assertThat(GitLabApiClient.apiRoot("https://git.empresa.com/")).isEqualTo("https://git.empresa.com/api/v4");
    }
}
