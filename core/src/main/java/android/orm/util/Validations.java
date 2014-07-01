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

    public static final Validation<Object> IsRequired = new Validation.Base<Object>() {

        @NonNls
        private static final String Error = "value is required";

        @Override
        public boolean isValid(@NonNull final Maybe<?> value) {
            return value.isSomething() && (value.get() != null);
        }

        @Override
        public void isValidOrThrow(@NonNull final Maybe<?> value) {
            if (!isValid(value)) {
                throw new Exception(Error);
            }
        }
    };

    public static final Validation<CharSequence> IsNotEmpty = new Validation.Value<CharSequence>("text is empty") {
        @Override
        public boolean isValid(@Nullable final CharSequence value) {
            return !isEmpty(value);
        }
    };

    @NonNull
    public static <V> Validation<V> IsEqualTo(@NonNls @Nullable final V value) {
        return new Validation.Value<V>("value is not equal to " + value) {
            @Override
            public boolean isValid(@Nullable final V other) {
                return (value == null) ? (other == null) : value.equals(other);
            }
        };
    }

    @NonNull
    public static <V> Validation<V> IsNotEqualTo(@NonNls @Nullable final V value) {
        return new Validation.Value<V>("value is equal to " + value) {
            @Override
            public boolean isValid(@Nullable final V other) {
                return (value == null) ? (other != null) : !value.equals(other);
            }
        };
    }

    @NonNull
    public static <V extends Comparable<V>> Validation<V> IsLessThan(@NonNls @NonNull final V value) {
        return new Validation.Value<V>("value is greater than or equal to " + value) {
            @Override
            public boolean isValid(@Nullable final V other) {
                return (other == null) || (other.compareTo(value) < 0);
            }
        };
    }

    @NonNull
    public static <V extends Comparable<V>> Validation<V> IsLessOrEqualThan(@NonNls @NonNull final V value) {
        return new Validation.Value<V>("value is greater than " + value) {
            @Override
            public boolean isValid(@Nullable final V other) {
                return (other == null) || (other.compareTo(value) <= 0);
            }
        };
    }

    @NonNull
    public static <V extends Comparable<V>> Validation<V> IsGreaterThan(@NonNls @NonNull final V value) {
        return new Validation.Value<V>("value is less than or equal to " + value) {
            @Override
            public boolean isValid(@Nullable final V other) {
                return (other == null) || (other.compareTo(value) > 0);
            }
        };
    }

    @NonNull
    public static <V extends Comparable<V>> Validation<V> IsGreaterOrEqualThan(@NonNls @NonNull final V value) {
        return new Validation.Value<V>("value is less than " + value) {
            @Override
            public boolean isValid(@Nullable final V other) {
                return (other == null) || (other.compareTo(value) >= 0);
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
                return values.contains(value);
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
                return !values.contains(value);
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

    public static final class OnLong {

        private static final Long ZERO = 0L;

        public static final Validation<Long> IsPositive = new Validation.Value<Long>(Errors.NOT_POSITIVE) {
            @Override
            public boolean isValid(@Nullable final Long value) {
                return (value == null) || (value.compareTo(ZERO) > 0);
            }
        };

        public static final Validation<Long> IsNegative = new Validation.Value<Long>(Errors.NOT_NEGATIVE) {
            @Override
            public boolean isValid(@Nullable final Long value) {
                return (value == null) || (value.compareTo(ZERO) < 0);
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
                return (value == null) || (value.compareTo(ZERO) > 0);
            }
        };

        public static final Validation<Double> IsNegative = new Validation.Value<Double>(Errors.NOT_NEGATIVE) {
            @Override
            public boolean isValid(@Nullable final Double value) {
                return (value == null) || (value.compareTo(ZERO) < 0);
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
                return (value == null) || (value.compareTo(ZERO) > 0);
            }
        };

        public static final Validation<BigDecimal> IsNegative = new Validation.Value<BigDecimal>(Errors.NOT_NEGATIVE) {
            @Override
            public boolean isValid(@Nullable final BigDecimal value) {
                return (value == null) || (value.compareTo(ZERO) < 0);
            }
        };

        private OnBigDecimal() {
            super();
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
        public final void isValidOrThrow(@NonNull final Maybe<? extends V> value) {
            try {
                mValidation.isValidOrThrow(value);
            } catch (final Exception ex) {
                throw new Exception(mName + ": " + ex.getMessage(), ex);
            }
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

    private static class Composition<V> extends Validation.Base<V> {

        @NonNull
        private final Validation<? super V> mFirst;
        @NonNull
        private final Validation<V> mSecond;

        private Composition(@NonNull final Validation<? super V> first,
                            @NonNull final Validation<V> second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @Override
        public final boolean isValid(@NonNull final Maybe<? extends V> value) {
            return mFirst.isValid(value) && mSecond.isValid(value);
        }

        @Override
        public final void isValidOrThrow(@NonNull final Maybe<? extends V> value) {
            mFirst.isValidOrThrow(value);
            mSecond.isValidOrThrow(value);
        }
    }

    private Validations() {
        super();
    }
}
