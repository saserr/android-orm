/*
 * Copyright 2013 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.orm.util;

import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import java.math.BigDecimal;
import java.util.Collection;

import static android.text.TextUtils.isEmpty;
import static java.math.BigDecimal.ZERO;

public final class Validations {

    @NonNls
    private static final String IS_EMPTY = "Values cannot be empty";

    private interface Errors {
        Validation.Result.Invalid<Object> NOT_POSITIVE = invalid("number is not positive");
        Validation.Result.Invalid<Object> NOT_NEGATIVE = invalid("number is not negative");
    }

    public static final Validation<Object> IsRequired = new Validation.Base<Object>() {

        private final Result.Invalid<Object> mInvalid = invalid("value is required");

        @NonNull
        @Override
        public <T> Result<T> validate(@NonNull final T value) {
            return valid(value);
        }

        @NonNull
        @Override
        public <T> Result<Maybe<T>> validate(@NonNull final Maybe<T> value) {
            return (value.getOrElse(null) == null) ?
                    Validations.<Maybe<T>>safeCast(mInvalid) :
                    valid(value);
        }
    };

    public static final Validation<CharSequence> IsNotEmpty = new Validation.Value<CharSequence>() {

        private final Result.Invalid<CharSequence> mInvalid = invalid("text is empty");

        @NonNull
        @Override
        public <T extends CharSequence> Result<T> validate(@NonNull final T value) {
            return isEmpty(value) ?
                    Validations.<T>safeCast(mInvalid) :
                    valid(value);
        }
    };

    @NonNull
    public static <V> Validation.Result.Valid<V> valid(@NonNull final V value) {
        return new Valid<>(value);
    }

    @NonNull
    public static <V> Validation.Result.Invalid<V> invalid(@NonNls @NonNull final String error) {
        return new Invalid<>(error);
    }

    @NonNull
    public static <V> Validation<V> IsEqualTo(@NonNull final V value) {
        return new Validation.Value<V>() {

            private final Result.Invalid<V> mInvalid = invalid("value is not equal to " + value);

            @NonNull
            @Override
            public <T extends V> Result<T> validate(@NonNull final T other) {
                return value.equals(other) ?
                        valid(other) :
                        Validations.<T>safeCast(mInvalid);
            }
        };
    }

    @NonNull
    public static <V> Validation<V> IsNotEqualTo(@NonNull final V value) {
        return new Validation.Value<V>() {

            private final Result.Invalid<V> mInvalid = invalid("value is equal to " + value);

            @NonNull
            @Override
            public <T extends V> Result<T> validate(@NonNull final T other) {
                return value.equals(other) ?
                        Validations.<T>safeCast(mInvalid) :
                        valid(other);
            }
        };
    }

    @NonNull
    public static <V extends Comparable<V>> Validation<V> IsLessThan(@NonNull final V value) {
        return new Validation.Value<V>() {

            private final Result.Invalid<V> mInvalid = invalid("value is greater than or equal to " + value);

            @NonNull
            @Override
            public <T extends V> Result<T> validate(@NonNull final T other) {
                return (other.compareTo(value) < 0) ?
                        valid(other) :
                        Validations.<T>safeCast(mInvalid);
            }
        };
    }

    @NonNull
    public static <V extends Comparable<V>> Validation<V> IsLessOrEqualThan(@NonNull final V value) {
        return new Validation.Value<V>() {

            private final Result.Invalid<V> mInvalid = invalid("value is greater than " + value);

            @NonNull
            @Override
            public <T extends V> Result<T> validate(@NonNull final T other) {
                return (other.compareTo(value) <= 0) ?
                        valid(other) :
                        Validations.<T>safeCast(mInvalid);
            }
        };
    }

    @NonNull
    public static <V extends Comparable<V>> Validation<V> IsGreaterThan(@NonNull final V value) {
        return new Validation.Value<V>() {

            private final Result.Invalid<V> mInvalid = invalid("value is less than or equal to " + value);

            @NonNull
            @Override
            public <T extends V> Result<T> validate(@NonNull final T other) {
                return (other.compareTo(value) > 0) ?
                        valid(other) :
                        Validations.<T>safeCast(mInvalid);
            }
        };
    }

    @NonNull
    public static <V extends Comparable<V>> Validation<V> IsGreaterOrEqualThan(@NonNull final V value) {
        return new Validation.Value<V>() {

            private final Result.Invalid<V> mInvalid = invalid("value is less than " + value);

            @NonNull
            @Override
            public <T extends V> Result<T> validate(@NonNull final T other) {
                return (other.compareTo(value) >= 0) ?
                        valid(other) :
                        Validations.<T>safeCast(mInvalid);
            }
        };
    }

    @NonNull
    public static <V> Validation<V> IsOneOf(@NonNull final Collection<V> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException(IS_EMPTY);
        }

        @NonNls
        final StringBuilder plain = new StringBuilder();
        plain.append('[');
        for (final V value : values) {
            plain.append(value).append(", ");
        }
        plain.replace(plain.length() - 2, plain.length(), "]");

        return new Validation.Value<V>() {

            private final Result.Invalid<V> mInvalid = invalid("value is not one of " + plain);

            @NonNull
            @Override
            public <T extends V> Result<T> validate(@NonNull final T value) {
                return (values.contains(value)) ?
                        valid(value) :
                        Validations.<T>safeCast(mInvalid);
            }
        };
    }

    @NonNull
    public static <V> Validation<V> IsNotOneOf(@NonNull final Collection<V> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException(IS_EMPTY);
        }

        @NonNls
        final StringBuilder plain = new StringBuilder();
        plain.append('[');
        for (final V value : values) {
            plain.append(value).append(", ");
        }
        plain.replace(plain.length() - 2, plain.length(), "]");

        return new Validation.Value<V>() {

            private final Result.Invalid<V> mInvalid = invalid("value is one of " + plain);

            @NonNull
            @Override
            public <T extends V> Result<T> validate(@NonNull final T value) {
                return (values.contains(value)) ?
                        Validations.<T>safeCast(mInvalid) :
                        valid(value);
            }
        };
    }

    @NonNull
    public static <V, T extends V> Validation<T> compose(@NonNull final Validation<V> first,
                                                         @NonNull final Validation<T> second) {
        return new Composition<>(first, second);
    }

    @NonNull
    public static <V> Validation<V> name(@NonNls @NonNull final String name,
                                         @NonNull final Validation<V> validation) {
        return new Name<>(name, validation);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Validation.Result<V> safeCast(@NonNull final Validation.Result<? extends V> result) {
        return (Validation.Result<V>) result;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Validation.Result.Invalid<V> safeCast(@NonNull final Validation.Result.Invalid<?> result) {
        return (Validation.Result.Invalid<V>) result;
    }

    public static final class OnLong {

        private static final Long ZERO = 0L;

        public static final Validation<Long> IsPositive = new Validation.Value<Long>() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public Result<Long> validate(@NonNull final Long value) {
                return (value.compareTo(ZERO) > 0) ?
                        valid(value) :
                        Validations.<Long>safeCast(Errors.NOT_POSITIVE);
            }
        };

        public static final Validation<Long> IsNegative = new Validation.Value<Long>() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public Result<Long> validate(@NonNull final Long value) {
                return (value.compareTo(ZERO) < 0) ?
                        valid(value) :
                        Validations.<Long>safeCast(Errors.NOT_NEGATIVE);
            }
        };

        private OnLong() {
            super();
        }
    }

    public static final class OnDouble {

        private static final Double ZERO = 0.0D;

        public static final Validation<Double> IsPositive = new Validation.Value<Double>() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public Result<Double> validate(@NonNull final Double value) {
                return (value.compareTo(ZERO) > 0) ?
                        valid(value) :
                        Validations.<Double>safeCast(Errors.NOT_POSITIVE);
            }
        };

        public static final Validation<Double> IsNegative = new Validation.Value<Double>() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public Result<Double> validate(@NonNull final Double value) {
                return (value.compareTo(ZERO) < 0) ?
                        valid(value) :
                        Validations.<Double>safeCast(Errors.NOT_NEGATIVE);
            }
        };

        private OnDouble() {
            super();
        }
    }

    public static final class OnBigDecimal {

        public static final Validation<BigDecimal> IsPositive = new Validation.Value<BigDecimal>() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public Result<BigDecimal> validate(@NonNull final BigDecimal value) {
                return (value.compareTo(ZERO) > 0) ?
                        valid(value) :
                        Validations.<BigDecimal>safeCast(Errors.NOT_POSITIVE);
            }
        };

        public static final Validation<BigDecimal> IsNegative = new Validation.Value<BigDecimal>() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public Result<BigDecimal> validate(@NonNull final BigDecimal value) {
                return (value.compareTo(ZERO) < 0) ?
                        valid(value) :
                        Validations.<BigDecimal>safeCast(Errors.NOT_NEGATIVE);
            }
        };

        private OnBigDecimal() {
            super();
        }
    }

    private static class Valid<V> extends Validation.Result.Valid<V> {

        @NonNull
        private final V mValue;

        private Valid(@NonNull final V value) {
            super();

            mValue = value;
        }

        @NonNull
        @Override
        public final V get() {
            return mValue;
        }

        @NonNull
        @Override
        public final <T> Valid<T> map(@NonNull final Function<? super V, ? extends T> function) {
            return Validations.<T>valid(function.invoke(mValue));
        }

        @NonNull
        @Override
        public final <T> Validation.Result<T> flatMap(@NonNull final Function<? super V, Validation.Result<T>> function) {
            return function.invoke(mValue);
        }

        @NonNull
        @Override
        public final <T extends V> Validation.Result<T> and(@NonNull final Validation.Result<T> second) {
            return second;
        }

        @NonNull
        @Override
        public final V or(@NonNull final V other) {
            return get();
        }

        @NonNull
        @Override
        public final <T extends V> Valid<V> or(@NonNull final Validation.Result<T> other) {
            return this;
        }
    }

    private static class Invalid<V> extends Validation.Result.Invalid<V> {

        @NonNls
        @NonNull
        private final String mError;

        private Invalid(@NonNls @NonNull final String error) {
            super();

            mError = error;
        }

        @NonNull
        @Override
        public final V get() {
            throw new Validation.Exception(mError);
        }

        @NonNls
        @NonNull
        @Override
        public final String getError() {
            return mError;
        }

        @NonNull
        @Override
        public final <T> Invalid<T> map(@NonNull final Function<? super V, ? extends T> function) {
            return safeCast(this);
        }

        @NonNull
        @Override
        public final <T> Invalid<T> flatMap(@NonNull final Function<? super V, Validation.Result<T>> function) {
            return safeCast(this);
        }

        @NonNull
        @Override
        public final <T extends V> Invalid<T> and(@NonNull final Validation.Result<T> second) {
            return second.isValid() ?
                    Validations.<T>safeCast(this) :
                    combine(this, (Invalid<T>) second);
        }

        @NonNull
        @Override
        public final V or(@NonNull final V other) {
            return other;
        }

        @NonNull
        @Override
        public final <T extends V> Validation.Result<V> or(@NonNull final Validation.Result<T> other) {
            return Validations.<V>safeCast(other);
        }

        private static <V, T extends V> Invalid<T> combine(@NonNull final Invalid<V> first,
                                                           @NonNull final Invalid<T> second) {
            return invalid(first.getError() + " and " + second.getError());
        }
    }

    private static class Name<V> implements Validation<V> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Validation<V> mValidation;

        private Name(@NonNull final String name, @NonNull final Validation<V> validation) {
            super();

            mName = name;
            mValidation = validation;
        }

        @NonNull
        @Override
        public final <T extends V> Result<T> validate(@NonNull final T value) {
            final Result<T> result;

            try {
                result = mValidation.validate(value);
            } catch (final Exception ex) {
                throw new Exception(mName + ": " + ex.getMessage(), ex);
            }

            return result;
        }

        @NonNull
        @Override
        public final <T extends V> Result<Maybe<T>> validate(@NonNull final Maybe<T> value) {
            final Result<Maybe<T>> result;

            try {
                result = mValidation.validate(value);
            } catch (final Exception ex) {
                throw new Exception(mName + ": " + ex.getMessage(), ex);
            }

            return result;
        }

        @NonNull
        @Override
        public final <T extends V> Validation<T> and(@NonNull final Validation<T> other) {
            return new Name<>(mName, mValidation.and(other));
        }

        @NonNull
        @Override
        public final Validation<V> name(@NonNls @NonNull final String name) {
            return new Name<>(name, mValidation);
        }
    }

    private static class Composition<V, T extends V> extends Validation.Base<T> {

        @NonNull
        private final Validation<V> mFirst;
        @NonNull
        private final Validation<T> mSecond;

        private Composition(@NonNull final Validation<V> first, @NonNull final Validation<T> second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @NonNull
        @Override
        public final <U extends T> Result<U> validate(@NonNull final U value) {
            return mFirst.validate(value).and(mSecond.validate(value));
        }

        @NonNull
        @Override
        public final <U extends T> Result<Maybe<U>> validate(@NonNull final Maybe<U> value) {
            return mFirst.validate(value).and(mSecond.validate(value));
        }
    }

    private Validations() {
        super();
    }
}
