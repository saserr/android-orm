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
import android.orm.util.Converter;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import static android.orm.util.Maybes.something;

public final class Binding {

    public interface Readable<V> {

        @NonNull
        Maybe<V> get();

        @NonNull
        <T> Readable<T> mapTo(@NonNull final Function<? super V, ? extends T> converter);

        @NonNull
        <T> Readable<T> flatMapTo(@NonNull final Function<? super V, Maybe<T>> converter);

        @NonNull
        <T> Readable<T> convertTo(@NonNull final Function<Maybe<V>, Maybe<T>> converter);

        abstract class Base<V> implements Readable<V> {

            @NonNull
            @Override
            public final <T> Readable<T> mapTo(@NonNull final Function<? super V, ? extends T> converter) {
                return Bindings.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Readable<T> flatMapTo(@NonNull final Function<? super V, Maybe<T>> converter) {
                return Bindings.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Readable<T> convertTo(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
                return Bindings.convert(this, converter);
            }
        }
    }

    public interface Writable<V> {

        void set(@Nullable final V v);

        void set(@NonNull final Maybe<V> v);

        @NonNull
        Writable<V> withDefault(@Nullable final V v);

        @NonNull
        <T> Writable<T> mapFrom(@NonNull final Function<? super T, ? extends V> converter);

        @NonNull
        <T> Writable<T> flatMapFrom(@NonNull final Function<? super T, Maybe<V>> converter);

        @NonNull
        <T> Writable<T> convertFrom(@NonNull final Function<Maybe<T>, Maybe<V>> converter);

        abstract class Base<V> implements Writable<V> {

            @Override
            public final void set(@Nullable final V value) {
                set(something(value));
            }

            @NonNull
            @Override
            public final Writable<V> withDefault(@Nullable final V value) {
                return new WithDefault<>(this, value);
            }

            @NonNull
            @Override
            public final <T> Writable<T> mapFrom(@NonNull final Function<? super T, ? extends V> converter) {
                return Bindings.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Writable<T> flatMapFrom(@NonNull final Function<? super T, Maybe<V>> converter) {
                return Bindings.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Writable<T> convertFrom(@NonNull final Function<Maybe<T>, Maybe<V>> converter) {
                return Bindings.convert(this, converter);
            }

            private static class WithDefault<V> extends Base<V> {

                @NonNull
                private final Writable<V> mBinding;
                @NonNull
                private final Maybe<V> mDefault;

                private WithDefault(@NonNull final Writable<V> binding, @Nullable final V value) {
                    super();

                    mBinding = binding;
                    mDefault = something(value);
                }

                @Override
                public final void set(@NonNull final Maybe<V> value) {
                    mBinding.set((value.getOrElse(null) == null) ? mDefault : value);
                }
            }
        }
    }

    public interface ReadWrite<V> extends Readable<V>, Writable<V> {

        @NonNull
        @Override
        ReadWrite<V> withDefault(@Nullable final V v);

        @NonNull
        <T> ReadWrite<T> map(@NonNull final Converter<V, T> converter);

        @NonNull
        <T> ReadWrite<T> convert(@NonNull final Converter<Maybe<V>, Maybe<T>> converter);

        abstract class Base<V> implements ReadWrite<V> {

            @Override
            public final void set(@Nullable final V value) {
                set(something(value));
            }

            @NonNull
            @Override
            public final ReadWrite<V> withDefault(@Nullable final V value) {
                return Bindings.combine(this, new Writable.Base.WithDefault<>(this, value));
            }

            @NonNull
            @Override
            public final <T> Readable<T> mapTo(@NonNull final Function<? super V, ? extends T> converter) {
                return Bindings.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Writable<T> mapFrom(@NonNull final Function<? super T, ? extends V> converter) {
                return Bindings.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> ReadWrite<T> map(@NonNull final Converter<V, T> converter) {
                return Bindings.convert(this, Maybes.lift(converter));
            }

            @NonNull
            @Override
            public final <T> Readable<T> flatMapTo(@NonNull final Function<? super V, Maybe<T>> converter) {
                return Bindings.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Writable<T> flatMapFrom(@NonNull final Function<? super T, Maybe<V>> converter) {
                return Bindings.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Readable<T> convertTo(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
                return Bindings.convert(this, converter);
            }

            @NonNull
            @Override
            public final <T> Writable<T> convertFrom(@NonNull final Function<Maybe<T>, Maybe<V>> converter) {
                return Bindings.convert(this, converter);
            }

            @NonNull
            @Override
            public final <T> ReadWrite<T> convert(@NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
                return Bindings.convert(this, converter);
            }
        }
    }

    public interface Validated<V> extends Writable<V> {

        @NonNull
        Validation.Result<Maybe<V>> get(@NonNull final Context context);

        @NonNull
        String getName(@NonNull final Context context);

        void setErrors(@NonNull final List<String> errors);

        @NonNull
        <T> Validated<T> convert(@NonNull final Converter<V, T> converter);

        @NonNull
        Validated<V> checkThat(@NonNull final Validation.Localized<? super V> validation);

        @NonNull
        Validated<V> withDefault(@Nullable final V v);

        interface Converter<V, T> {

            @NonNull
            Validation.Result<T> to(@NonNull final V v);

            @NonNull
            V from(@NonNull final T t);

            @NonNull
            List<String> getErrorMessages(@NonNull final String name,
                                          @NonNull final Context context);
        }

        abstract class Base<V> implements Validated<V> {

            @Override
            public final void set(@Nullable final V value) {
                set(something(value));
            }

            @NonNull
            @Override
            public final <T> Writable<T> mapFrom(@NonNull final Function<? super T, ? extends V> converter) {
                return Bindings.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Writable<T> flatMapFrom(@NonNull final Function<? super T, Maybe<V>> converter) {
                return Bindings.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Writable<T> convertFrom(@NonNull final Function<Maybe<T>, Maybe<V>> converter) {
                return Bindings.convert(this, converter);
            }

            @NonNull
            @Override
            public final <T> Validated<T> convert(@NonNull final Converter<V, T> converter) {
                return Bindings.convert(this, converter);
            }

            @NonNull
            @Override
            public final Validated<V> checkThat(@NonNull final Validation.Localized<? super V> validation) {
                return new Checked<>(this, validation);
            }

            @NonNull
            @Override
            public final Validated<V> withDefault(@Nullable final V value) {
                return new WithDefault<>(this, value);
            }

            private static class Checked<V, T extends V> extends Validated.Base<T> {

                @NonNull
                private final Validated<T> mBinding;
                @NonNull
                private final Validation.Localized<V> mValidation;

                private Checked(@NonNull final Validated<T> binding,
                                @NonNull final Validation.Localized<V> validation) {
                    super();

                    mBinding = binding;
                    mValidation = validation;
                }

                @NonNull
                @Override
                public final Validation.Result<Maybe<T>> get(@NonNull final Context context) {
                    final Validation.Result<Maybe<T>> value = mBinding.get(context);
                    final Validation.Result<Maybe<T>> result;

                    if (value.isValid()) {
                        result = mValidation.validate(value.get());
                        if (result.isInvalid()) {
                            setErrors(mValidation.name(getName(context)).getErrorMessages(context));
                        }
                    } else {
                        result = value;
                    }

                    return result;
                }

                @Override
                public final void set(@NonNull final Maybe<T> value) {
                    mBinding.set(value);
                }

                @NonNull
                @Override
                public final String getName(@NonNull final Context context) {
                    return mBinding.getName(context);
                }

                @Override
                public final void setErrors(@NonNull final List<String> errors) {
                    mBinding.setErrors(errors);
                }
            }

            private static class WithDefault<V> extends Validated.Base<V> {

                @NonNull
                private final Validated<V> mBinding;
                @NonNull
                private final Maybe<V> mDefault;

                private WithDefault(@NonNull final Validated<V> binding, @Nullable final V value) {
                    super();

                    mBinding = binding;
                    mDefault = something(value);
                }

                @NonNull
                @Override
                public final Validation.Result<Maybe<V>> get(@NonNull final Context context) {
                    return mBinding.get(context);
                }

                @Override
                public final void set(@NonNull final Maybe<V> value) {
                    mBinding.set((value.getOrElse(null) == null) ? mDefault : value);
                }

                @NonNull
                @Override
                public final String getName(@NonNull final Context context) {
                    return mBinding.getName(context);
                }

                @Override
                public final void setErrors(@NonNull final List<String> errors) {
                    mBinding.setErrors(errors);
                }
            }
        }
    }

    private Binding() {
        super();
    }
}
