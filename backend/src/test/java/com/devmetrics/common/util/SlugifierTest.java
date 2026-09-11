package com.devmetrics.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugifierTest {

    @Test
    @DisplayName("gera slug em minusculo com hifen")
    void basicSlug() {
        assertThat(Slugifier.slugify("Spring Boot")).isEqualTo("spring-boot");
        assertThat(Slugifier.slugify("Node.js")).isEqualTo("node-js");
        assertThat(Slugifier.slugify("GitHub Actions")).isEqualTo("github-actions");
    }

    @Test
    @DisplayName("C# e C++ nao colidem com C")
    void sharpAndPlusDoNotCollideWithC() {
        assertThat(Slugifier.slugify("C")).isEqualTo("c");
        assertThat(Slugifier.slugify("C#")).isEqualTo("csharp");
        assertThat(Slugifier.slugify("C++")).isEqualTo("cplusplus");
    }

    @Test
    @DisplayName("remove acentos")
    void removesAccents() {
        assertThat(Slugifier.slugify("Programação")).isEqualTo("programacao");
    }

    @Test
    @DisplayName("entrada vazia ou nula devolve string vazia")
    void emptyInput() {
        assertThat(Slugifier.slugify(null)).isEmpty();
        assertThat(Slugifier.slugify("   ")).isEmpty();
    }
}
