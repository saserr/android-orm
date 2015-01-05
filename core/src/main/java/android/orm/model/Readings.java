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

package android.orm.model;

import android.orm.sql.Reader;
import android.orm.sql.Readers;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.util.Converter;
import android.orm.util.Converters;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.orm.util.Producers;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.util.SparseArray;

import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.orm.util.Maybes.something;

public final class Readings {

    @NonNull
    public static <V> Reading.Single<V> single(@NonNull final Value.Read<V> value) {
        return new Single<>(Mappers.read(value));
    }

    @NonNull
    public static <M> Reading.Single<M> single(@NonNull final Mapper.Read<M> mapper) {
        return new Single<>(mapper);
    }

    @NonNull
    public static <V> Reading.Many<List<V>> list(@NonNull final Value.Read<V> value) {
        return new Many<>(Readers.list(value.getName(), Plan.Read.from(value)));
    }

    @NonNull
    public static <M> Reading.Many<List<M>> list(@NonNull final Mapper.Read<M> mapper) {
        return new Many<>(Readers.list(mapper.getName(), mapper.prepareReader()));
    }

    @NonNull
    public static <V> Reading.Many<Set<V>> set(@NonNull final Value.Read<V> value) {
        return new Many<>(Readers.set(value.getName(), Plan.Read.from(value)));
    }

    @NonNull
    public static <M> Reading.Many<Set<M>> set(@NonNull final Mapper.Read<M> mapper) {
        return new Many<>(Readers.set(mapper.getName(), mapper.prepareReader()));
    }

    @NonNull
    public static <M> Reading.Many<Set<M>> difference(@NonNull final Reading.Many<Set<M>> reading,
                                                      @NonNull final Set<M> subtrahend) {
        return new Many<>(reading.prepareReader().map(new Function<Set<M>, Set<M>>() {
            @NonNull
            @Override
            public Set<M> invoke(@NonNull final Set<M> minuend) {
                minuend.removeAll(subtrahend);
                return minuend;
            }
        }));
    }

    @NonNull
    public static <K, V> Reading.Many<Map<K, V>> map(@NonNull final Value.Read<K> key,
                                                     @NonNull final Value.Read<V> value) {
        return new Many<>(Readers.map(
                '(' + key.getName() + ", " + value.getName() + ')',
                Plan.Read.from(key),
                Plan.Read.from(value)
        ));
    }

    @NonNull
    public static <K, V> Reading.Many<Map<K, V>> map(@NonNull final Mapper.Read<K> key,
                                                     @NonNull final Value.Read<V> value) {
        return new Many<>(Readers.map(
                '(' + key.getName() + ", " + value.getName() + ')',
                key.prepareReader(),
                Plan.Read.from(value)
        ));
    }

    @NonNull
    public static <K, V> Reading.Many<Map<K, V>> map(@NonNull final Value.Read<K> key,
                                                     @NonNull final Mapper.Read<V> value) {
        return new Many<>(Readers.map(
                '(' + key.getName() + ", " + value.getName() + ')',
                Plan.Read.from(key),
                value.prepareReader()
        ));
    }

    @NonNull
    public static <K, V> Reading.Many<Map<K, V>> map(@NonNull final Mapper.Read<K> key,
                                                     @NonNull final Mapper.Read<V> value) {
        return new Many<>(Readers.map(
                '(' + key.getName() + ", " + value.getName() + ')',
                key.prepareReader(),
                value.prepareReader()
        ));
    }

    @NonNull
    public static <V> Reading.Many<SparseArray<V>> sparseArray(@NonNull final Value.Read<Integer> key,
                                                               @NonNull final Value.Read<V> value) {
        return new Many<>(Readers.sparseArray(
                '(' + key.getName() + ", " + value.getName() + ')',
                Plan.Read.from(key),
                Plan.Read.from(value)
        ));
    }

    @NonNull
    public static <V> Reading.Many<SparseArray<V>> sparseArray(@NonNull final Mapper.Read<Integer> key,
                                                               @NonNull final Value.Read<V> value) {
        return new Many<>(Readers.sparseArray(
                '(' + key.getName() + ", " + value.getName() + ')',
                key.prepareReader(),
                Plan.Read.from(value)
        ));
    }

    @NonNull
    public static <V> Reading.Many<SparseArray<V>> sparseArray(@NonNull final Value.Read<Integer> key,
                                                               @NonNull final Mapper.Read<V> value) {
        return new Many<>(Readers.sparseArray(
                '(' + key.getName() + ", " + value.getName() + ')',
                Plan.Read.from(key),
                value.prepareReader()
        ));
    }

    @NonNull
    public static <V> Reading.Many<SparseArray<V>> sparseArray(@NonNull final Mapper.Read<Integer> key,
                                                               @NonNull final Mapper.Read<V> value) {
        return new Many<>(Readers.sparseArray(
                '(' + key.getName() + ", " + value.getName() + ')',
                key.prepareReader(),
                value.prepareReader()
        ));
    }

    @NonNull
    public static <V, T> Reading.Single<Pair<V, T>> compose(@NonNull final Reading.Single<V> first,
                                                            @NonNull final Reading.Single<T> second) {
        return new SingleComposition<>(first, second);
    }

    @NonNull
    public static <V, T> Reading.Many<Pair<V, T>> compose(@NonNull final Reading.Many<V> first,
                                                          @NonNull final Reading.Many<T> second) {
        return new Many<>(first.prepareReader().and(second.prepareReader()));
    }

    @NonNull
    public static <V, T> Reading.Single<T> convert(@NonNull final Reading.Single<V> reading,
                                                   @NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
        return new SingleConversion<>(reading, converter);
    }

    @NonNull
    public static <V, T> Reading.Many<T> convert(@NonNull final Reading.Many<V> reading,
                                                 @NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
        return new Many<>(reading.prepareReader().convert(Converters.from(converter)));
    }

    private static class Single<V> extends Reading.Single.Base<V> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Mapper.Read<V> mMapper;
        @NonNull
        private final Reader.Collection<V> mCreate;

        private Single(@NonNull final Mapper.Read<V> mapper) {
            super();

            mName = mapper.getName();
            mMapper = mapper;
            mCreate = Readers.single(mName, mapper.prepareReader());
        }

        @NonNull
        @Override
        public final Reader.Collection<V> prepareReader() {
            return mCreate;
        }

        @NonNull
        @Override
        public final Reader.Collection<V> prepareReader(@NonNull final V model) {
            return Readers.single(mName, mMapper.prepareReader(model));
        }
    }

    private static class Many<V> extends Reading.Many.Base<V> {

        @NonNull
        private final Reader.Collection<V> mReader;


        private Many(@NonNull final Reader.Collection<V> reader) {
            super();

            mReader = reader;
        }

        @NonNull
        @Override
        public final Reader.Collection<V> prepareReader() {
            return mReader;
        }
    }

    private static class SingleComposition<V, T> extends Reading.Single.Base<Pair<V, T>> {

        @NonNull
        private final Reading.Single<V> mFirst;
        @NonNull
        private final Reading.Single<T> mSecond;
        @NonNull
        private final Reader.Collection<Pair<V, T>> mCreate;

        private SingleComposition(@NonNull final Reading.Single<V> first,
                                  @NonNull final Reading.Single<T> second) {
            super();

            mFirst = first;
            mSecond = second;
            mCreate = new Eagerly<>(first.prepareReader().and(second.prepareReader()));
        }

        @NonNull
        @Override
        public final Reader.Collection<Pair<V, T>> prepareReader() {
            return mCreate;
        }

        @NonNull
        @Override
        public final Reader.Collection<Pair<V, T>> prepareReader(@NonNull final Pair<V, T> pair) {
            final Reader.Collection<Pair<V, T>> result;

            final V v = pair.first;
            final T t = pair.second;
            if ((v == null) && (t == null)) {
                result = prepareReader();
            } else {
                final Reader.Collection<V> reader1 = (v == null) ?
                        mFirst.prepareReader() :
                        mFirst.prepareReader(v);
                final Reader.Collection<T> reader2 = (t == null) ?
                        mSecond.prepareReader() :
                        mSecond.prepareReader(t);
                result = reader1.and(reader2);
            }

            return result;
        }
    }

    private static class SingleConversion<T, V> extends Reading.Single.Base<T> {

        @NonNull
        private final Reading.Single<V> mReading;
        @NonNull
        private final Converter<Maybe<V>, Maybe<T>> mConverter;
        @NonNull
        private final Function<Maybe<V>, Maybe<T>> mFrom;
        @NonNull
        private final Reader.Collection<T> mCreate;

        private SingleConversion(@NonNull final Reading.Single<V> reading,
                                 @NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
            super();

            mReading = reading;
            mConverter = converter;
            mFrom = Converters.from(converter);
            mCreate = new Eagerly<>(reading.prepareReader().convert(mFrom));
        }

        @NonNull
        @Override
        public final Reader.Collection<T> prepareReader() {
            return mCreate;
        }

        @NonNull
        @Override
        public final Reader.Collection<T> prepareReader(@NonNull final T t) {
            final Reader.Collection<T> result;

            final Maybe<V> value = mConverter.to(something(t));
            if (value.isSomething()) {
                final V v = value.get();
                result = (v == null) ?
                        prepareReader() :
                        mReading.prepareReader(v).convert(mFrom);
            } else {
                result = prepareReader();
            }

            return result;
        }
    }

    private static class Eagerly<V> extends Reader.Collection.Base<V> {

        @NonNull
        private final Reader.Collection<V> mReader;

        private Eagerly(@NonNull final Reader.Collection<V> reader) {
            super();

            mReader = reader;
        }

        @NonNull
        @Override
        public final Select.Projection getProjection() {
            return mReader.getProjection();
        }

        @NonNull
        @Override
        public final Producer<Maybe<V>> read(@NonNull final android.orm.sql.Readable input) {
            return Producers.constant(mReader.read(input).produce());
        }
    }

    private Readings() {
        super();
    }
}
