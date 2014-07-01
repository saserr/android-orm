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

import android.orm.util.Converter;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

    private Binding() {
        super();
    }
}
