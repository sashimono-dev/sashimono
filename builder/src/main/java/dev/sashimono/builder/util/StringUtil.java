package dev.sashimono.builder.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringUtil {

    public static String camelToCapitalisedKebabCase(final String words) {
        final String delimiter = "-";
        return Stream.of(words.trim().split(delimiter + "|(?=\\p{Upper})")).filter(w -> !w.isEmpty())
                .map(w -> w.substring(0, 1).toUpperCase() + w.substring(1)).collect(Collectors.joining(delimiter));
    }
}
