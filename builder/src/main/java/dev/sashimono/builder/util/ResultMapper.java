package dev.sashimono.builder.util;

/**
 * Maps one result type to another. Used when types have multiple different representations.
 *
 * @param <F> The original type
 * @param <T> The new type
 */
public interface ResultMapper<F, T> {

    T map(F val);
}
