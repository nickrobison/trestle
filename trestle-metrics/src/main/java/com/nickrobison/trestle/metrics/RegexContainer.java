package com.nickrobison.trestle.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by nrobison on 3/20/17.
 */
class RegexContainer<T> {
    private static final Logger logger = LoggerFactory.getLogger(RegexContainer.class);
    private final Pattern regex;
    private final T content;

    RegexContainer(Pattern regex, T content) {
        this.regex = regex;
        this.content = content;
    }

    static <T>Optional<RegexContainer<T>> checkAndCreate(String maybeRegex, T content) {
        if (maybeRegex.startsWith("/") && maybeRegex.endsWith("/")) {
            try {
                final Pattern regex = Pattern.compile(maybeRegex.substring(1, maybeRegex.length() - 1));
                return Optional.of(new RegexContainer<>(regex, content));
            } catch (PatternSyntaxException e) {
                logger.error("Unable to compile regex {}", maybeRegex, e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    Optional<T> match(String metricName) {
        if (regex.matcher(metricName).find()) {
            return Optional.of(this.content);
        }
        return Optional.empty();
    }
}
