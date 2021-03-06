package com.somnus.microservice.commons.core.support;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.*;

/**
 * A container object which may or may not contain a non-null value.
 * If a value is present, {@code isPresent()} will return {@code true} and
 * {@code get()} will return the value.
 *
 * <p>Additional methods that depend on the presence or absence of a contained
 * (return a default value if value not present) and
 * of code if the value is present).
 *
 * <p>This is a <a href="../lang/doc-files/ValueBased.html">value-based</a>
 * class; use of identity-sensitive operations (including reference equality
 * ({@code ==}), identity hash code, or synchronization) on instances of
 * {@code Optional} may have unpredictable results and should be avoided.
 *
 * @since 1.8
 */
public final class Optionally<T> {
    /**
     * Common instance for {@code empty()}.
     */
    private static final Optionally<?> EMPTY = new Optionally<>();

    /**
     * If non-null, the value; if null, indicates no value is present
     */
    private final T value;

    private Optionally() {
        this.value = null;
    }

    public static<T> Optionally<T> empty() {
        @SuppressWarnings("unchecked")
        Optionally<T> t = (Optionally<T>) EMPTY;
        return t;
    }

    /**
     * Constructs an instance with the value present.
     *
     * @param value the non-null value to be present
     * @throws NullPointerException if value is null
     */
    private Optionally(T value) {
        this.value = Objects.requireNonNull(value);
    }

    public static <T> Optionally<T> of(T value) {
        return new Optionally<>(value);
    }

    public static <T> Optionally<T> ofNullable(T value) {
        return value == null ? empty() : of(value);
    }

    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public T orElse(T other) {
        return value != null ? value : other;
    }

    /**
     * ??????????????????null??????????????????????????????true????????????consumer1
     * ??????????????????null??????????????????????????????false????????????consumer2
     * ?????????????????????null????????????????????????
     * @param predicate
     * @param consumer1
     * @param consumer2
     */
    public void ifElse(Predicate<? super T> predicate, Consumer<? super T> consumer1, Consumer<? super T> consumer2) {
        Objects.requireNonNull(predicate);
        if (value != null && predicate.test(value)){
            consumer1.accept(value);
        } else if (value != null && predicate.negate().test(value)){
            consumer2.accept(value);
        }
    }

    /**
     *  ??????????????????null??????????????????????????????true????????????lambda???????????????
     * @param predicate
     * @param consumer
     */
    public void ifTrue(Predicate<? super T> predicate, Consumer<? super T> consumer) {
        Objects.requireNonNull(predicate);
        if (value != null && predicate.test(value)){
            consumer.accept(value);
        }
    }

    /**
     *  ??????????????????null??????????????????????????????false????????????lambda???????????????
     * @param predicate
     * @param consumer
     */
    public void ifFalse(Predicate<? super T> predicate, Consumer<? super T> consumer) {
        Objects.requireNonNull(predicate);
        if (value != null && predicate.negate().test(value)){
            consumer.accept(value);
        }
    }

    /**
     * ?????????????????????????????????true????????????????????????(??????????????????????????????????????????)
     * @param exceptionSupplier
     * @param <X>
     * @throws X
     */
    public <X extends Throwable> void trueThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if(! Boolean.class.isInstance(value)){
            throw new IllegalArgumentException();
        }
        if (value instanceof Boolean && ((Boolean) value == true)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * ?????????????????????????????????false????????????????????????(??????????????????????????????????????????)
     * @param exceptionSupplier
     * @param <X>
     * @throws X
     */
    public <X extends Throwable> void falseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if(! Boolean.class.isInstance(value)){
            throw new IllegalArgumentException();
        }
        if (value instanceof Boolean && ((Boolean) value == false)) {
            throw exceptionSupplier.get();
        }
    }

    public <X extends Throwable> void trueThrow(Predicate<? super T> predicate, Supplier<? extends X> exceptionSupplier) throws X {
        Objects.requireNonNull(predicate);
        if (value != null && predicate.test(value)) {
            throw exceptionSupplier.get();
        }
    }

    public <X extends Throwable> void falseThrow(Predicate<? super T> predicate, Supplier<? extends X> exceptionSupplier) throws X {
        Objects.requireNonNull(predicate);
        if (value != null && predicate.negate().test(value)) {
            throw exceptionSupplier.get();
        }
    }

    /**
     * ???????????????null??????????????????
     * @param consumer
     */
    public void ifNull(Consumer<? super T> consumer) {
        if (value == null)
            consumer.accept(value);
    }

    /**
     * ???????????????null????????????????????????????????????????????????
     * @param supplier
     */
    public <R> R orElse(R r, Supplier<? extends R> supplier) {
        if (value != null)
            return supplier.get();

        return r;
    }

    /**
     * ???????????????null?????????????????????
     * ??????????????????null??????????????????true?????????supplier1
     * ??????????????????null??????????????????false?????????supplier2
     * @param <R>
     * @return
     */
    public <R> R orElse(Predicate<T> predicate, R r, Supplier<? extends R> supplier1, Supplier<? extends R> supplier2) {
        if (value != null && predicate.test(value)){
            return supplier1.get();
        } else if (value != null && predicate.negate().test(value)){
            return supplier2.get();
        }
        return r;
    }

    /**
     * ???????????????????????????String Collection Map???????????????????????????lambda???????????????
     * @param consumer
     */
    public void ifNotEmpty(Consumer<? super T> consumer) {
        if (value != null && value instanceof String && ((String) value).length() > 0) {
            consumer.accept(value);
        } else if (value != null && value instanceof Collection && ! ((Collection<?>) value).isEmpty()) {
            consumer.accept(value);
        } else if (value != null && value instanceof Map && ((Map<?,?>) value).size() > 0) {
            consumer.accept(value);
        }
    }

    /**
     * ???????????????????????????String Collection Map????????????????????????lambda???????????????
     * @param consumer
     */
    public void ifEmpty(Consumer<? super T> consumer) {
        if(value == null){
            consumer.accept(value);
        } else if (value instanceof String && ((String) value).length() == 0) {
            consumer.accept(value);
        } else if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
            consumer.accept(value);
        } else if (value instanceof Map && ((Map<?,?>) value).size() == 0) {
            consumer.accept(value);
        }
    }

    public Optionally<T> emptyMap(UnaryOperator<T> mapper) {
        Objects.requireNonNull(mapper);
        if(value == null){
            return Optionally.ofNullable(mapper.apply(value));
        } else if (value instanceof String && ((String) value).length() == 0) {
            return Optionally.ofNullable(mapper.apply(value));
        } else if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
            return Optionally.ofNullable(mapper.apply(value));
        } else if (value instanceof Map && ((Map<?,?>) value).size() == 0) {
            return Optionally.ofNullable(mapper.apply(value));
        } else{
            return Optionally.ofNullable(value);
        }
    }

    public <X extends Throwable> void existThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (value != null) {
            throw exceptionSupplier.get();
        }
    }

    public <X extends Throwable> Optionally<T> existThrow(Predicate<? super T> predicate, Supplier<? extends X> exceptionSupplier) throws X {
        Objects.requireNonNull(predicate);
        if (value != null && predicate.negate().test(value)) {
            throw exceptionSupplier.get();
        }else{
            return this;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Optionally)) {
            return false;
        }

        Optionally<?> other = (Optionally<?>) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value != null
            ? String.format("Optional[%s]", value)
            : "Optional.empty";
    }

}
