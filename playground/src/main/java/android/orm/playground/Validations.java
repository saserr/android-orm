/*
 * Copyright 2014 the original author or authors
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

package android.orm.playground;

import android.content.Context;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import org.jetbrains.annotations.NonNls;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static java.lang.System.arraycopy;
import static java.util.Collections.singletonList;

public final class Validations {

    public static final Validation.Localized<Object> IsRequired = localize(android.orm.util.Validations.IsRequired, R.string.android_orm_error_required);
    public static final Validation.Localized<CharSequence> IsNotEmpty = localize(android.orm.util.Validations.IsNotEmpty, R.string.android_orm_error_empty);

    @NonNull
    public static <V> Validation.Localized<V> localize(@NonNull final android.orm.util.Validation<V> validation,
                                                       @StringRes final int id,
                                                       @NonNull final Object... arguments) {
        return new Localize<>(validation, id, arguments);
    }

    @NonNull
    public static <V> Validation.Localized<V> IsEqualTo(@NonNull final V value) {
        return localize(android.orm.util.Validations.IsEqualTo(value), R.string.android_orm_error_not_equal, value);
    }

    @NonNull
    public static <V> Validation.Localized<V> IsNotEqualTo(@NonNull final V value) {
        return localize(android.orm.util.Validations.IsNotEqualTo(value), R.string.android_orm_error_equal, value);
    }

    @NonNull
    public static <V extends Comparable<V>> Validation.Localized<V> IsLessThan(@NonNull final V value) {
        return localize(android.orm.util.Validations.IsLessThan(value), R.string.android_orm_error_greater_or_equal, value);
    }

    @NonNull
    public static <V extends Comparable<V>> Validation.Localized<V> IsLessOrEqualThan(@NonNull final V value) {
        return localize(android.orm.util.Validations.IsLessOrEqualThan(value), R.string.android_orm_error_greater, value);
    }

    @NonNull
    public static <V extends Comparable<V>> Validation.Localized<V> IsGreaterThan(@NonNull final V value) {
        return localize(android.orm.util.Validations.IsGreaterThan(value), R.string.android_orm_error_less_or_equal, value);
    }

    @NonNull
    public static <V extends Comparable<V>> Validation.Localized<V> IsGreaterOrEqualThan(@NonNull final V value) {
        return localize(android.orm.util.Validations.IsGreaterOrEqualThan(value), R.string.android_orm_error_less, value);
    }

    @NonNull
    public static <V> Validation.Localized<V> IsOneOf(@NonNull final Collection<V> values) {
        @NonNls
        final StringBuilder html = new StringBuilder();

        html.append("<ul>");
        for (final V value : values) {
            html.append("<li>").append(value).append("</li>");
        }
        html.append("</ul>");

        return localize(android.orm.util.Validations.IsOneOf(values), R.string.android_orm_error_not_one_of, html.toString());
    }

    @NonNull
    public static <V> Validation.Localized<V> IsNotOneOf(@NonNull final Collection<V> values) {
        @NonNls
        final StringBuilder html = new StringBuilder();

        html.append("<ul>");
        for (final V value : values) {
            html.append("<li>").append(value).append("</li>");
        }
        html.append("</ul>");

        return localize(android.orm.util.Validations.IsNotOneOf(values), R.string.android_orm_error_one_of, html.toString());
    }

    public static final class OnLong {

        public static final Validation.Localized<Long> IsPositive = localize(android.orm.util.Validations.OnLong.IsPositive, R.string.android_orm_error_not_positive);
        public static final Validation.Localized<Long> IsNegative = localize(android.orm.util.Validations.OnLong.IsNegative, R.string.android_orm_error_not_negative);

        private OnLong() {
            super();
        }
    }

    public static final class OnDouble {

        public static final Validation.Localized<Double> IsPositive = localize(android.orm.util.Validations.OnDouble.IsPositive, R.string.android_orm_error_not_positive);
        public static final Validation.Localized<Double> IsNegative = localize(android.orm.util.Validations.OnDouble.IsNegative, R.string.android_orm_error_not_negative);

        private OnDouble() {
            super();
        }
    }

    public static final class OnBigDecimal {

        public static final Validation.Localized<BigDecimal> IsPositive = localize(android.orm.util.Validations.OnBigDecimal.IsPositive, R.string.android_orm_error_not_positive);
        public static final Validation.Localized<BigDecimal> IsNegative = localize(android.orm.util.Validations.OnBigDecimal.IsNegative, R.string.android_orm_error_not_negative);

        private OnBigDecimal() {
            super();
        }
    }

    private static class Localize<V> extends Validation.Localized.Base<V> {

        @NonNull
        private final android.orm.util.Validation<V> mValidation;
        @StringRes
        private final int mId;
        @NonNull
        private final Object[] mArguments;

        private Localize(@NonNull final android.orm.util.Validation<V> validation,
                         @StringRes final int id,
                         @NonNull final Object... arguments) {
            super();

            mValidation = validation;
            mId = id;
            mArguments = arguments;
        }

        @NonNull
        @Override
        protected final List<String> getErrorMessages(@NonNull final String name,
                                                      @NonNull final Context context) {
            final Object[] newArguments = new Object[mArguments.length + 1];
            newArguments[0] = name;

            if (mArguments.length > 0) {
                arraycopy(mArguments, 0, newArguments, 1, mArguments.length);
            }

            return singletonList(context.getString(mId, newArguments));
        }

        @NonNull
        @Override
        public final <T extends V> Result<T> validate(@NonNull final T value) {
            return mValidation.validate(value);
        }

        @NonNull
        @Override
        public final <T extends V> Result<Maybe<T>> validate(@NonNull final Maybe<T> value) {
            return mValidation.validate(value);
        }
    }

    private Validations() {
        super();
    }
}
