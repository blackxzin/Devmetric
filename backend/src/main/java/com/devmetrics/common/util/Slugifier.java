package com.devmetrics.common.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugifier {

    private Slugifier() {
    }

    public static String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        // "#" e "+" viram palavra para "C#" e "C++" nao colidirem com "C".
        String expanded = value.replace("#", "sharp").replace("+", "plus");
        String normalized = Normalizer.normalize(expanded, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
