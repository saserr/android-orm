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
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.math.BigDecimal;
import java.util.Collection;

import static android.text.TextUtils.isEmpty;
import static java.math.BigDecimal.ZERO;

public final class Validations {

    @NonNls
    private static final String IS_EMPTY = "Values cannot be empty";

    private interface Errors {
        @NonNls
        String NOT_POSITIVE = "number is not positive";
        @NonNls
        String NOT_NEGATIVE = "number is not negative";
    }

    public static final Validation<Object> IsNothing = new Validation.Base<Object>() {

        @NonNls
        private static final String Error = "value is something";

        @Override
        public boolean isValid(@NonNull final Maybe<?> value) {
            return value.isNothing();
        }

        @Override
        public void validate(@NonNull final Maybe<?> value) {
            if (!isValid(value)) {
                throw new Failure(Error);
            }
        }
    };

    public static final Validation<Object> IsSomething = new Validation.Base<Object>() {

        @NonNls
        private static final String Error = "value is nothing";

        @Override
        public boolean isValid(@NonNull final Maybe<?> value) {
            return value.isSomething();
        }

        @Override
        public void validate(@NonNull final Maybe<?> value) {
            if (!isValid(value)) {
                throw new Failure(Error);
            }
        }
    };

    public static final Validation<Object> IsNull = new Validation.Value<Object>("value is not null") {
        @Override
        protected boolean isValid(@Nullable final Object value) {
            return value == null;
        }
    };

    public static final Validation<Object> IsNotNull = new Validation.Value<Object>("value is null") {
        @Override
        protected boolean isValid(@Nullable final Object value) {
            return value != null;
        }
    };

    public static final Validation<CharSequence> IsNotEmpty = new Validation.Value<CharSequence>("text is empty") {
        @Override
        public boolean isValid(@Nullable final CharSequence value) {
            return (value != null) && !isEmpty(value.toString().trim());
        }
    };

    @NonNull
    public static <V> Validation<V> IsEqualTo(@NonNls @NonNull final V value) {
        return new Validation.Value<V>("value is not equal to " + value) {
            @Override
            public boolean isValid(@Nullable final V other) {
                return (other != null) && value.equals(other);
            }
        };
    }

    @NonNull
    public static <V> Validation<V> IsNotEqualTo(@NonNls @NonNull final V value) {
        return new Validation.Value<V>("value is equal to " + value) {
            @Override
            public boolean isValid(@Nullable final V other) {
                return (other != null) && !value.equals(other);
            }
        };
    }

    @NonNull
    public static <V extends Comparable<V>> Validation<V> IsLessThan(@NonNls @NonNull final V value) {
        return new Validation.Value<V>("value is greater than or equal to " + value) {
            @Override
            public boolean isValid(@Nullable final V other) {
                return (other != null) && (other.compareTo(value) < 0);
            }
        };
    }

    @NonNull
    public static <V extends Comparable<V>> Validation<V> IsLessOrEqualThan(@NonNls @NonNull final V value) {
        return new Validation.Value<V>("value is greater than " + value) {
            @Override
            public boolean isValid(@Nullable final V other) {
                return (other != null) && (other.compareTo(value) <= 0);
            }
        };
    }

    @NonNull
    public static <V extends Comparable<V>> Validation<V> IsGreaterThan(@NonNls @NonNull final V value) {
        return new Validation.Value<V>("value is less than or equal to " + value) {
            @Override
            public boolean isValid(@Nullable final V other) {
                return (other != null) && (other.compareTo(value) > 0);
            }
        };
    }

    @NonNull
    public static <V extends Comparable<V>> Validation<V> IsGreaterOrEqualThan(@NonNls @NonNull final V value) {
        return new Validation.Value<V>("value is less than " + value) {
            @Override
            public boolean isValid(@Nullable final V other) {
                return (other != null) && (other.compareTo(value) >= 0);
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

        return new Validation.Value<V>("value is not one of " + plain) {
            @Override
            public boolean isValid(@Nullable final V value) {
                return (value != null) && values.contains(value);
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

        return new Validation.Value<V>("value is one of " + plain) {
            @Override
            public boolean isValid(@Nullable final V value) {
                return (value != null) && !values.contains(value);
            }
        };
    }

    @NonNull
    @SafeVarargs
    public static <V> Validation<V> all(@NonNull final Validation<? super V>... validations) {
        return new All<>(validations);
    }

    @NonNull
    @SafeVarargs
    public static <V> Validation<V> any(@NonNull final Validation<? super V>... validations) {
        return new Any<>(validations);
    }

    @NonNull
    public static <V> Validation<V> name(@NonNls @NonNull final String name,
                                         @NonNull final Validation<V> validation) {
        return new Name<>(name, validation);
    }

    @NonNull
    public static <M, V> Validation<M> extend(@NonNull final Validation<? super V> validation,
                                              @NonNull final Lens.Read<M, Maybe<V>> lens) {
        return new Extension<>(validation, lens);
    }

    @NonNull
    public static <V> Validation<V> combine(@NonNull final Validation<? super V> validation,
                                            @NonNull final Validation.Callback<V> callback) {
        return new Combination<>(validation, callback);
    }

    @NonNull
    public static <V, T> Validation<T> convert(@NonNull final Validation<? super V> validation,
                                               @NonNull final Function<Maybe<T>, Maybe<V>> converter) {
        return new Conversion<>(validation, converter);
    }

    public static final class OnLong {

        private static final Long ZERO = 0L;

        public static final Validation<Long> IsPositive = new Validation.Value<Long>(Errors.NOT_POSITIVE) {
            @Override
            public boolean isValid(@Nullable final Long value) {
                return (value != null) && (value.compareTo(ZERO) > 0);
            }
        };

        public static final Validation<Long> IsNegative = new Validation.Value<Long>(Errors.NOT_NEGATIVE) {
            @Override
            public boolean isValid(@Nullable final Long value) {
                return (value != null) && (value.compareTo(ZERO) < 0);
            }
        };

        private OnLong() {
            super();
        }
    }

    public static final class OnDouble {

        private static final Double ZERO = 0.0D;

        public static final Validation<Double> IsPositive = new Validation.Value<Double>(Errors.NOT_POSITIVE) {
            @Override
            public boolean isValid(@Nullable final Double value) {
                return (value != null) && (value.compareTo(ZERO) > 0);
            }
        };

        public static final Validation<Double> IsNegative = new Validation.Value<Double>(Errors.NOT_NEGATIVE) {
            @Override
            public boolean isValid(@Nullable final Double value) {
                return (value != null) && (value.compareTo(ZERO) < 0);
            }
        };

        private OnDouble() {
            super();
        }
    }

    public static final class OnBigDecimal {

        public static final Validation<BigDecimal> IsPositive = new Validation.Value<BigDecimal>(Errors.NOT_POSITIVE) {
            @Override
            public boolean isValid(@Nullable final BigDecimal value) {
                return (value != null) && (value.compareTo(ZERO) > 0);
            }
        };

        public static final Validation<BigDecimal> IsNegative = new Validation.Value<BigDecimal>(Errors.NOT_NEGATIVE) {
            @Override
            public boolean isValid(@Nullable final BigDecimal value) {
                return (value != null) && (value.compareTo(ZERO) < 0);
            }
        };

        private OnBigDecimal() {
            super();
        }
    }

    private static class All<V> extends Validation.Base<V> {

        @NonNull
        private final Validation<? super V>[] mValidations;
        private final int mLength;

        @SafeVarargs
        private All(@NonNull final Validation<? super V>... validations) {
            super();

            mValidations = validations;
            mLength = validations.length;
        }

        @Override
        public final boolean isValid(@NonNull final Maybe<? extends V> value) {
            boolean valid = true;

            for (int i = 0; (i < mLength) && valid; i++) {
                valid = mValidations[i].isValid(value);
            }

            return valid;
        }

        @Override
        public final void validate(@NonNull final Maybe<? extends V> value) {
            for (int i = 0; i < mLength; i++) {
                mValidations[i].validate(value);
            }
        }
    }

    private static class Any<V> extends Validation.Base<V> {

        @NonNull
        private final Validation<? super V>[] mValidations;
        private final int mLength;

        @SafeVarargs
        private Any(@NonNull final Validation<? super V>... validations) {
            super();

            if (validations.length < 1) {
                throw new IllegalArgumentException("Any requires at least one validations");
            }

            mValidations = validations;
            mLength = validations.length;
        }

        @Override
        public final boolean isValid(@NonNull final Maybe<? extends V> value) {
            boolean valid = false;

            for (int i = 0; (i < mLength) && !valid; i++) {
                valid = mValidations[i].isValid(value);
            }

            return valid;
        }

        @Override
        public final void validate(@NonNull final Maybe<? extends V> value) {

            try {
                mValidations[0].validate(value);
            } catch (final Failure failure) {
                boolean invalid = true;

                for (int i = 0; (i < mLength) && invalid; i++) {
                    try {
                        mValidations[i].validate(value);
                        invalid = false;
                    } catch (final Failure ignored) {/* do nothing */}
                }

                if (invalid) {
                    throw failure;
                }
            }
        }
    }

    private static class Name<V> implements Validation<V> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Validation<V> mValidation;

        private Name(@NonNls @NonNull final String name, @NonNull final Validation<V> validation) {
            super();

            mName = name;
            mValidation = validation;
        }

        @Override
        public final boolean isValid(@NonNull final Maybe<? extends V> value) {
            return mValidation.isValid(value);
        }

        @Override
        public final void validate(@NonNull final Maybe<? extends V> value) {
            try {
                mValidation.validate(value);
            } catch (final Failure ex) {
                throw new Failure(mName + ": " + ex.getMessage(), ex);
            }
        }

        @NonNull
        @Override
        public final <T extends V> Validation<T> and(@NonNull final Validation<T> other) {
            return new Name<>(mName, mValidation.and(other));
        }

        @NonNull
        @Override
        public final <T extends V> Validation<T> or(@NonNull final Validation<T> other) {
            return new Name<>(mName, mValidation.or(other));
        }

        @NonNull
        @Override
        public final Validation<V> name(@NonNls @NonNull final String name) {
            return new Name<>(name, mValidation);
        }

        @NonNull
        @Override
        public final <M> Validation<M> on(@NonNull final Lens.Read<M, Maybe<V>> lens) {
            return new Name<>(mName, mValidation.on(lens));
        }

        @NonNull
        @Override
        public final <T extends V> Validation<T> with(@NonNull final Callback<T> callback) {
            return new Name<>(mName, mValidation.with(callback));
        }

        @NonNull
        @Override
        public final <T> Validation<T> map(@NonNull final Function<? super T, ? extends V> converter) {
            return new Name<>(mName, mValidation.map(converter));
        }

        @NonNull
        @Override
        public final <T> Validation<T> flatMap(@NonNull final Function<? super T, Maybe<V>> converter) {
            return new Name<>(mName, mValidation.flatMap(converter));
        }

        @NonNull
        @Override
        public final <T> Validation<T> convert(@NonNull final Function<Maybe<T>, Maybe<V>> converter) {
            return new Name<>(mName, mValidation.convert(converter));
        }
    }

    private static class Extension<M, V> extends Validation.Base<M> {

        @NonNull
        private final Validation<? super V> mValidation;
        @NonNull
        private final Lens.Read<M, Maybe<V>> mLens;

        private Extension(@NonNull final Validation<? super V> validation,
                          @NonNull final Lens.Read<M, Maybe<V>> lens) {
            super();

            mValidation = validation;
            mLens = lens;
        }

        @Override
        public final boolean isValid(@NonNull final Maybe<? extends M> value) {
            return mValidation.isValid(Lenses.get(value, mLens));
        }

        @Override
        public final void validate(@NonNull final Maybe<? extends M> value) {
            mValidation.validate(Lenses.get(value, mLens));
        }
    }

    private static class Combination<V> extends Validation.Base<V> {

        @NonNull
        private final Validation<? super V> mValidation;
        @NonNull
        private final Callback<V> mCallback;

        private Combination(@NonNull final Validation<? super V> validation,
                            @NonNull final Callback<V> callback) {
            super();

            mValidation = validation;
            mCallback = callback;
        }

        @Override
        public final boolean isValid(@NonNull final Maybe<? extends V> value) {
            final boolean valid = mValidation.isValid(value);

            if (valid) {
                mCallback.onValid(Maybes.safeCast(value));
            } else {
                mCallback.onInvalid(Maybes.safeCast(value));
            }

            return valid;
        }

        @Override
        public final void validate(@NonNull final Maybe<? extends V> value) {
            try {
                mValidation.validate(value);
                mCallback.onValid(Maybes.safeCast(value));
            } catch (final Failure failure) {
                mCallback.onInvalid(Maybes.safeCast(value));
                throw failure;
            }
        }
    }

    private static class Conversion<V, T> extends Validation.Base<T> {

        @NonNull
        private final Validation<? super V> mValidation;
        @NonNull
        private final Function<Maybe<T>, Maybe<V>> mConverter;

        private Conversion(@NonNull final Validation<? super V> validation,
                           @NonNull final Function<Maybe<T>, Maybe<V>> converter) {
            super();

            mValidation = validation;
            mConverter = converter;
        }

        @Override
        public final boolean isValid(@NonNull final Maybe<? extends T> value) {
            return mValidation.isValid(mConverter.invoke(Maybes.safeCast(value)));
        }

        @Override
        public final void validate(@NonNull final Maybe<? extends T> value) {
            mValidation.validate(mConverter.invoke(Maybes.safeCast(value)));
        }
    }

    private Validations() {
        super();
    }
}
