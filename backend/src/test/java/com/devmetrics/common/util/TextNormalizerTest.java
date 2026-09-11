package com.devmetrics.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    @Test
    @DisplayName("normaliza para minusculo sem acento")
    void normalizesText() {
        assertThat(TextNormalizer.normalize("Correção de Bug")).isEqualTo("correcao de bug");
        assertThat(TextNormalizer.normalize("  DOCUMENTAÇÃO  ")).isEqualTo("documentacao");
    }

    @Test
    @DisplayName("entrada nula vira string vazia")
    void nullIsSafe() {
        assertThat(TextNormalizer.normalize(null)).isEmpty();
    }
}
