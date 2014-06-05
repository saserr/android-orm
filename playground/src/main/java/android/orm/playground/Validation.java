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

import java.util.ArrayList;
import java.util.List;

import static android.orm.util.Validations.invalid;

public interface Validation<V> extends android.orm.util.Validation<V> {

    @NonNull
    <T extends V> Validation<T> and(@NonNull final Validation<T> second);

    @NonNull
    Localized<V> localize(@StringRes final int id, @NonNull final Object... arguments);

    interface Named<V> extends Localized<V> {

        @NonNull
        <T extends V> Named<T> and(@NonNull final Named<T> second);

        @NonNull
        @Override
        Named<V> localize(@StringRes final int id, @NonNull final Object... arguments);

        @NonNull
        @Override
        Named<V> name(@NonNull final String name);

        @NonNull
        List<String> getErrorMessages(@NonNull final Context context);

        abstract class Base<V> implements Named<V> {

            @NonNull
            @Override
            public final <T extends V> android.orm.util.Validation<T> and(@NonNull final android.orm.util.Validation<T> other) {
                return android.orm.util.Validations.compose(this, other);
            }

            @NonNull
            @Override
            public final <T extends V> Validation<T> and(@NonNull final Validation<T> second) {
                return new Validation.Base.Composition<>(this, second);
            }

            @NonNull
            @Override
            public final <T extends V> Localized<T> and(@NonNull final Localized<T> second) {
                return new Localized.Base.Composition<>(this, second);
            }

            @NonNull
            @Override
            public final <T extends V> Named<T> and(@NonNull final Named<T> second) {
                return new Composition<>(this, second);
            }

            private static class Composition<V, T extends V> extends Base<T> {

                @NonNull
                private final Named<V> mFirst;
                @NonNull
                private final Named<T> mSecond;

                private Composition(@NonNull final Named<V> first,
                                    @NonNull final Named<T> second) {
                    super();

                    mFirst = first;
                    mSecond = second;
                }

                @NonNull
                @Override
                public final List<String> getErrorMessages(@NonNull final Context context) {
                    final List<String> errors1 = mFirst.getErrorMessages(context);
                    final List<String> errors2 = mSecond.getErrorMessages(context);
                    final List<String> errors = new ArrayList<>(errors1.size() + errors2.size());

                    errors.addAll(errors1);
                    errors.addAll(errors2);

                    return errors;
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

                @NonNull
                @Override
                public final Named<T> localize(@StringRes final int id,
                                               @NonNull final Object... arguments) {
                    return mFirst.localize(id, arguments).and(mSecond.localize(id, arguments));
                }

                @NonNull
                @Override
                public final Named<T> name(@NonNull final String name) {
                    return mFirst.name(name).and(mSecond.name(name));
                }
            }
        }
    }

    interface Localized<V> extends Validation<V> {

        @NonNull
        <T extends V> Localized<T> and(@NonNull final Localized<T> second);

        @NonNull
        @Override
        Named<V> name(@NonNull final String name);

        abstract class Base<V> implements Localized<V> {

            @NonNull
            protected abstract List<String> getErrorMessages(@NonNull final String name,
                                                             @NonNull final Context context);

            @NonNull
            @Override
            public final <T extends V> android.orm.util.Validation<T> and(@NonNull final android.orm.util.Validation<T> other) {
                return android.orm.util.Validations.compose(this, other);
            }

            @NonNull
            @Override
            public final <T extends V> Validation<T> and(@NonNull final Validation<T> second) {
                return new Validation.Base.Composition<>(this, second);
            }

            @NonNull
            @Override
            public final Localized<V> localize(@StringRes final int id,
                                               @NonNull final Object... arguments) {
                return Validations.localize(this, id, arguments);
            }

            @NonNull
            @Override
            public final <T extends V> Localized<T> and(@NonNull final Localized<T> second) {
                return new Composition<>(this, second);
            }

            @NonNull
            @Override
            public final Named<V> name(@NonNull final String name) {
                return new Name<>(this, name);
            }

            private static class Composition<V, T extends V> implements Localized<T> {

                @NonNull
                private final Localized<V> mFirst;
                @NonNull
                private final Localized<T> mSecond;

                private Composition(@NonNull final Localized<V> first,
                                    @NonNull final Localized<T> second) {
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

                @NonNull
                @Override
                public final <U extends T> android.orm.util.Validation<U> and(@NonNull final android.orm.util.Validation<U> other) {
                    return android.orm.util.Validations.compose(this, other);
                }

                @NonNull
                @Override
                public final <U extends T> Validation<U> and(@NonNull final Validation<U> second) {
                    return new Validation.Base.Composition<>(this, second);
                }

                @NonNull
                @Override
                public final <U extends T> Localized<U> and(@NonNull final Localized<U> second) {
                    return new Composition<>(this, second);
                }

                @NonNull
                @Override
                public final Localized<T> localize(@StringRes final int id,
                                                   @NonNull final Object... arguments) {
                    return Validations.localize(this, id, arguments);
                }

                @NonNull
                @Override
                public final Named<T> name(@NonNull final String name) {
                    return mFirst.name(name).and(mSecond.name(name));
                }
            }

            private static class Name<V> extends Named.Base<V> {

                @NonNull
                private final Localized.Base<V> mValidation;
                @NonNull
                private final String mName;

                private Name(@NonNull final Localized.Base<V> validation,
                             @NonNull final String name) {
                    super();

                    mValidation = validation;
                    mName = name;
                }

                @NonNull
                @Override
                public final List<String> getErrorMessages(@NonNull final Context context) {
                    return mValidation.getErrorMessages(mName, context);
                }

                @NonNull
                @Override
                public final <T extends V> Result<T> validate(@NonNull final T value) {
                    Result<T> result = mValidation.validate(value);

                    if (result.isInvalid()) {
                        result = invalid(mName + ": " + ((Result.Invalid<T>) result).getError());
                    }

                    return result;
                }

                @NonNull
                @Override
                public final <T extends V> Result<Maybe<T>> validate(@NonNull final Maybe<T> value) {
                    Result<Maybe<T>> result = mValidation.validate(value);

                    if (result.isInvalid()) {
                        result = invalid(mName + ": " + ((Result.Invalid<Maybe<T>>) result).getError());
                    }

                    return result;
                }

                @NonNull
                @Override
                public final Named<V> localize(@StringRes final int id, @NonNull final Object... arguments) {
                    return mValidation.localize(id, arguments).name(mName);
                }

                @NonNull
                @Override
                public final Named<V> name(@NonNull final String name) {
                    return mValidation.name(name);
                }
            }
        }
    }

    abstract class Base<V> implements Validation<V> {

        @NonNull
        @Override
        public final <T extends V> android.orm.util.Validation<T> and(@NonNull final android.orm.util.Validation<T> other) {
            return android.orm.util.Validations.compose(this, other);
        }

        @NonNull
        @Override
        public <T extends V> Validation<T> and(@NonNull final Validation<T> second) {
            return new Composition<>(this, second);
        }

        @NonNull
        @Override
        public final Localized<V> localize(@StringRes final int id, @NonNull final Object... arguments) {
            return Validations.localize(this, id, arguments);
        }

        @NonNull
        @Override
        public final android.orm.util.Validation<V> name(@NonNls @NonNull final String name) {
            return android.orm.util.Validations.name(name, this);
        }

        private static class Composition<V, T extends V> extends Base<T> {

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
    }
}
