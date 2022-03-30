package com.mycompany.app;

import org.graalvm.polyglot.HostAccess;

import java.util.Objects;

@FunctionalInterface
public interface GraalFunction<T, R> {
    /**
     * Applies this function to the given argument.
     *
     * @param value the function argument
     * @return the function result
     */
    @HostAccess.Export
    R apply(T value);

    /**
     * Returns a composed function that first applies the {@code before}
     * function to its input, and then applies this function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V>    the type of input to the {@code before} function, and to the
     *               composed function
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before} function and then applies this function
     * @throws NullPointerException if before is null
     */
    default <V> GraalFunction<V, R> compose(GraalFunction<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V value) -> apply(before.apply(value));
    }

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V>   the type of output of the {@code after} function, and of the
     *              composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    default <V> GraalFunction<T, V> andThen(GraalFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T value) -> after.apply(apply(value));
    }

    /**
     * Returns a function that always returns its input argument.
     *
     * @param <T> the type of the input and output objects to the function
     * @return a function that always returns its input argument
     */
    static <T> GraalFunction<T, T> identity() {
        return value -> value;
    }
}
