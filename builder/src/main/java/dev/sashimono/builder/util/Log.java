package dev.sashimono.builder.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Extremely simple log implementation, basically just appends a timestamp and category
 * to the beginning of each time.
 *
 */
public class Log {

    private static final boolean DEBUG = Boolean.getBoolean("debug");

    private final String category;

    public static Log of(String category) {
        return new Log(category);
    }

    public static Log of(Class<?> category) {
        return new Log(category.getName());
    }

    Log(String category) {
        this.category = category;
    }

    public void info(String message) {
        System.out.println(
                "[" + category + "] " + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()) + " " + message);
    }

    public void infof(String message, Object... params) {
        info(String.format(message, params));
    }

    public void error(String message) {
        System.err.println(
                "[" + category + "] " + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()) + " ERROR "
                        + message);
    }

    public void errorf(String message, Object... params) {
        error(String.format(message, params));
    }

    public void debug(String message) {
        if (!DEBUG) {
            return;
        }
        System.out.println(
                "[" + category + "] " + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()) + " DEBUG "
                        + message);
    }

    public void debugf(String message, Object... params) {
        if (!DEBUG) {
            return;
        }
        debug(String.format(message, params));
    }
}
