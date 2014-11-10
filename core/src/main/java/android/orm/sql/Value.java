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

package android.orm.sql;

import android.orm.util.Converter;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.orm.util.Producers;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import static android.orm.util.Maybes.something;

public final class Value {

    public interface Read<V> {

        @NonNls
        @NonNull
        String getName();

        @NonNull
        Select.Projection getProjection();

        @NonNull
        Maybe<V> read(@NonNull final Readable input);

        @NonNull
        <T> Read<Pair<V, T>> and(@NonNull final Read<T> other);

        @NonNull
        <T> Read<T> mapTo(@NonNull final Function<? super V, ? extends T> converter);

        @NonNull
        <T> Read<T> flatMapTo(@NonNull final Function<? super V, Maybe<T>> converter);

        @NonNull
        <T> Read<T> convertTo(@NonNull final Function<Maybe<V>, Maybe<T>> converter);

        abstract class Base<V> implements Read<V> {

            @NonNull
            @Override
            public final <T> Read<Pair<V, T>> and(@NonNull final Read<T> other) {
                return Values.compose(this, other);
            }

            @NonNull
            @Override
            public final <T> Read<T> mapTo(@NonNull final Function<? super V, ? extends T> converter) {
                return Values.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Read<T> flatMapTo(@NonNull final Function<? super V, Maybe<T>> converter) {
                return Values.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Read<T> convertTo(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
                return Values.convert(this, converter);
            }
        }
    }

    public interface Constant extends Writer {

        @NonNls
        @NonNull
        String getName();

        @NonNull
        Constant and(@NonNull final Constant other);

        @NonNull
        <V> Write<V> and(@NonNull final Write<V> other);

        abstract class Base implements Constant {

            @NonNull
            @Override
            public final Constant and(@NonNull final Constant other) {
                return Values.compose(this, other);
            }

            @NonNull
            @Override
            public final <V> Write<V> and(@NonNull final Write<V> other) {
                return Values.compose(this, other);
            }
        }
    }

    public interface Write<V> {

        @NonNls
        @NonNull
        String getName();

        @NonNull
        Constant write(@Nullable final V value);

        @NonNull
        Constant write(@NonNull final Producer<V> producer);

        void write(@NonNull final Operation operation,
                   @NonNull final Maybe<V> value,
                   @NonNull final Writable output);

        @NonNull
        <T> Write<Pair<V, T>> and(@NonNull final Write<T> other);

        @NonNull
        Write<V> and(@NonNull final Constant other);

        @NonNull
        <T> Write<T> mapFrom(@NonNull final Function<? super T, ? extends V> converter);

        @NonNull
        <T> Write<T> flatMapFrom(@NonNull final Function<? super T, Maybe<V>> converter);

        @NonNull
        <T> Write<T> convertFrom(@NonNull final Function<Maybe<T>, Maybe<V>> converter);

        abstract class Base<V> implements Write<V> {

            @NonNull
            @Override
            public final Constant write(@Nullable final V value) {
                return Values.constant(this, Producers.constant(something(value)));
            }

            @NonNull
            @Override
            public final Constant write(@NonNull final Producer<V> producer) {
                return Values.constant(this, Maybes.lift(producer));
            }

            @NonNull
            @Override
            public final <T> Write<Pair<V, T>> and(@NonNull final Write<T> other) {
                return Values.compose(this, other);
            }

            @NonNull
            @Override
            public final Write<V> and(@NonNull final Constant other) {
                return Values.compose(other, this);
            }

            @NonNull
            @Override
            public final <T> Write<T> mapFrom(@NonNull final Function<? super T, ? extends V> converter) {
                return Values.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Write<T> flatMapFrom(@NonNull final Function<? super T, Maybe<V>> converter) {
                return Values.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Write<T> convertFrom(@NonNull final Function<Maybe<T>, Maybe<V>> converter) {
                return Values.convert(this, converter);
            }
        }

        enum Operation {
            Insert, Update, Visit
        }
    }

    public interface ReadWrite<V> extends Read<V>, Write<V> {

        @NonNull
        <T> ReadWrite<Pair<V, T>> and(@NonNull final ReadWrite<T> other);

        @NonNull
        @Override
        ReadWrite<V> and(@NonNull final Constant other);

        @NonNull
        <T> ReadWrite<T> map(@NonNull final Converter<V, T> converter);

        @NonNull
        <T> ReadWrite<T> convert(@NonNull final Converter<Maybe<V>, Maybe<T>> converter);

        abstract class Base<V> implements ReadWrite<V> {

            @NonNull
            @Override
            public final Constant write(@Nullable final V value) {
                return Values.constant(this, Producers.constant(something(value)));
            }

            @NonNull
            @Override
            public final Constant write(@NonNull final Producer<V> producer) {
                return Values.constant(this, Maybes.lift(producer));
            }

            @NonNull
            @Override
            public final <T> Read<Pair<V, T>> and(@NonNull final Read<T> other) {
                return Values.compose(this, other);
            }

            @NonNull
            @Override
            public final ReadWrite<V> and(@NonNull final Constant other) {
                return Values.combine(this, Values.compose(other, this));
            }

            @NonNull
            @Override
            public final <T> Write<Pair<V, T>> and(@NonNull final Write<T> other) {
                return Values.compose(this, other);
            }

            @NonNull
            @Override
            public final <T> ReadWrite<Pair<V, T>> and(@NonNull final ReadWrite<T> other) {
                return Values.compose(this, other);
            }

            @NonNull
            @Override
            public final <T> Read<T> mapTo(@NonNull final Function<? super V, ? extends T> converter) {
                return Values.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> Write<T> mapFrom(@NonNull final Function<? super T, ? extends V> converter) {
                return Values.convert(this, Maybes.map(converter));
            }

            @NonNull
            @Override
            public final <T> ReadWrite<T> map(@NonNull final Converter<V, T> converter) {
                return Values.convert(this, Maybes.lift(converter));
            }

            @NonNull
            @Override
            public final <T> Read<T> flatMapTo(@NonNull final Function<? super V, Maybe<T>> converter) {
                return Values.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Write<T> flatMapFrom(@NonNull final Function<? super T, Maybe<V>> converter) {
                return Values.convert(this, Maybes.flatMap(converter));
            }

            @NonNull
            @Override
            public final <T> Read<T> convertTo(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
                return Values.convert(this, converter);
            }

            @NonNull
            @Override
            public final <T> Write<T> convertFrom(@NonNull final Function<Maybe<T>, Maybe<V>> converter) {
                return Values.convert(this, converter);
            }

            @NonNull
            @Override
            public final <T> ReadWrite<T> convert(@NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
                return Values.convert(this, converter);
            }
        }
    }

    private Value() {
        super();
    }
}
