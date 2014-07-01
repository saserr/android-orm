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

package android.orm.model;

import android.orm.sql.Value;
import android.orm.util.Converter;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.orm.model.Instances.instance;
import static android.orm.util.Maybes.something;

public final class Binding {

    public interface Read<V> {

        @NonNull
        Maybe<V> get();

        @NonNull
        Instance.Writable bindTo(@NonNull final Value.Write<V> value);

        @NonNull
        Instance.Writable bindTo(@NonNull final Mapper.Write<V> mapper);

        @NonNull
        <T> Read<T> mapTo(@NonNull final Function<? super V, ? extends T> converter);

        @NonNull
        <T> Read<T> flatMapTo(@NonNull final Function<? super V, Maybe<T>> converter);

        @NonNull
        <T> Read<T> convertTo(@NonNull final Function<Maybe<V>, Maybe<T>> converter);

        abstract class Base<V> implements Read<V> {

            @NonNull
            @Override
            public final Instance.Writable bindTo(@NonNull final Value.Write<V> value) {
                return instance(this, value);
            }

            @NonNull
            @Override
            public final Instance.Writable bindTo(@NonNull final Mapper.Write<V> mapper) {
                return instance(this, mapper);
            }

            @NonNull
            @Override
            public final <T> Read<T> mapTo(@NonNull final Function<? super V, ? extends T> converter) {
                return Bindings.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Read<T> flatMapTo(@NonNull final Function<? super V, Maybe<T>> converter) {
                return Bindings.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Read<T> convertTo(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
                return Bindings.convert(this, converter);
            }
        }
    }

    public interface Write<V> {

        void set(@Nullable final V v);

        void set(@NonNull final Maybe<V> v);

        @NonNull
        Instance.Readable bindTo(@NonNull final Value.Read<V> value);

        @NonNull
        Instance.Readable bindTo(@NonNull final Mapper.Read<V> mapper);

        @NonNull
        <T> Write<T> mapFrom(@NonNull final Function<? super T, ? extends V> converter);

        @NonNull
        <T> Write<T> flatMapFrom(@NonNull final Function<? super T, Maybe<V>> converter);

        @NonNull
        <T> Write<T> convertFrom(@NonNull final Function<Maybe<T>, Maybe<V>> converter);

        abstract class Base<V> implements Write<V> {

            @Override
            public final void set(@Nullable final V value) {
                set(something(value));
            }

            @NonNull
            @Override
            public final Instance.Readable bindTo(@NonNull final Value.Read<V> value) {
                return instance(this, value);
            }

            @NonNull
            @Override
            public final Instance.Readable bindTo(@NonNull final Mapper.Read<V> mapper) {
                return instance(this, mapper);
            }

            @NonNull
            @Override
            public final <T> Write<T> mapFrom(@NonNull final Function<? super T, ? extends V> converter) {
                return Bindings.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Write<T> flatMapFrom(@NonNull final Function<? super T, Maybe<V>> converter) {
                return Bindings.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Write<T> convertFrom(@NonNull final Function<Maybe<T>, Maybe<V>> converter) {
                return Bindings.convert(this, converter);
            }
        }
    }

    public interface ReadWrite<V> extends Read<V>, Write<V> {

        @NonNull
        Instance.ReadWrite bindTo(@NonNull final Value.ReadWrite<V> value);

        @NonNull
        Instance.ReadWrite bindTo(@NonNull final Mapper.ReadWrite<V> mapper);

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
            public final Instance.Writable bindTo(@NonNull final Value.Write<V> value) {
                return instance(this, value);
            }

            @NonNull
            @Override
            public final Instance.Readable bindTo(@NonNull final Value.Read<V> value) {
                return instance(this, value);
            }

            @NonNull
            @Override
            public final Instance.ReadWrite bindTo(@NonNull final Value.ReadWrite<V> value) {
                return instance(this, value);
            }

            @NonNull
            @Override
            public final Instance.Writable bindTo(@NonNull final Mapper.Write<V> mapper) {
                return instance(this, mapper);
            }

            @NonNull
            @Override
            public final Instance.Readable bindTo(@NonNull final Mapper.Read<V> mapper) {
                return instance(this, mapper);
            }

            @NonNull
            @Override
            public final Instance.ReadWrite bindTo(@NonNull final Mapper.ReadWrite<V> mapper) {
                return instance(this, mapper);
            }

            @NonNull
            @Override
            public final <T> Read<T> mapTo(@NonNull final Function<? super V, ? extends T> converter) {
                return Bindings.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Write<T> mapFrom(@NonNull final Function<? super T, ? extends V> converter) {
                return Bindings.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> ReadWrite<T> map(@NonNull final Converter<V, T> converter) {
                return Bindings.convert(this, Maybes.lift(converter));
            }

            @NonNull
            @Override
            public final <T> Read<T> flatMapTo(@NonNull final Function<? super V, Maybe<T>> converter) {
                return Bindings.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Write<T> flatMapFrom(@NonNull final Function<? super T, Maybe<V>> converter) {
                return Bindings.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Read<T> convertTo(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
                return Bindings.convert(this, converter);
            }

            @NonNull
            @Override
            public final <T> Write<T> convertFrom(@NonNull final Function<Maybe<T>, Maybe<V>> converter) {
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
