package com.devmetrics.common.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normaliza texto para as heuristicas de classificacao: minusculo e sem acento.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
