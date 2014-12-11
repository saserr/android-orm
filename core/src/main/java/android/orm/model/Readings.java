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

import android.orm.sql.Value;
import android.orm.util.Converter;
import android.orm.util.Converters;
import android.orm.util.Function;
import android.orm.util.Maybe;
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
        return new Single<>(value);
    }

    @NonNull
    public static <M> Reading.Single<M> single(@NonNull final Mapper.Read<M> mapper) {
        return new Single<>(mapper);
    }

    @NonNull
    public static <V> Reading.Many<List<V>> list(@NonNull final Value.Read<V> value) {
        return new Many<>(Plans.list(value.getName(), Reading.Item.Create.from(value)));
    }

    @NonNull
    public static <M> Reading.Many<List<M>> list(@NonNull final Mapper.Read<M> mapper) {
        return new Many<>(Plans.list(mapper.getName(), mapper.prepareRead()));
    }

    @NonNull
    public static <V> Reading.Many<Set<V>> set(@NonNull final Value.Read<V> value) {
        return new Many<>(Plans.set(value.getName(), Reading.Item.Create.from(value)));
    }

    @NonNull
    public static <M> Reading.Many<Set<M>> set(@NonNull final Mapper.Read<M> mapper) {
        return new Many<>(Plans.set(mapper.getName(), mapper.prepareRead()));
    }

    @NonNull
    public static <M> Reading.Many<Set<M>> difference(@NonNull final Reading.Many<Set<M>> reading,
                                                      @NonNull final Set<M> subtrahend) {
        return new Many<>(reading.preparePlan().map(new Function<Set<M>, Set<M>>() {
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
        return new Many<>(Plans.map(
                '(' + key.getName() + ", " + value.getName() + ')',
                Reading.Item.Create.from(key),
                Reading.Item.Create.from(value)
        ));
    }

    @NonNull
    public static <K, V> Reading.Many<Map<K, V>> map(@NonNull final Mapper.Read<K> key,
                                                     @NonNull final Value.Read<V> value) {
        return new Many<>(Plans.map(
                '(' + key.getName() + ", " + value.getName() + ')',
                key.prepareRead(),
                Reading.Item.Create.from(value)
        ));
    }

    @NonNull
    public static <K, V> Reading.Many<Map<K, V>> map(@NonNull final Value.Read<K> key,
                                                     @NonNull final Mapper.Read<V> value) {
        return new Many<>(Plans.map(
                '(' + key.getName() + ", " + value.getName() + ')',
                Reading.Item.Create.from(key),
                value.prepareRead()
        ));
    }

    @NonNull
    public static <K, V> Reading.Many<Map<K, V>> map(@NonNull final Mapper.Read<K> key,
                                                     @NonNull final Mapper.Read<V> value) {
        return new Many<>(Plans.map(
                '(' + key.getName() + ", " + value.getName() + ')',
                key.prepareRead(),
                value.prepareRead()
        ));
    }

    @NonNull
    public static <V> Reading.Many<SparseArray<V>> sparseArray(@NonNull final Value.Read<Integer> key,
                                                               @NonNull final Value.Read<V> value) {
        return new Many<>(Plans.sparseArray(
                '(' + key.getName() + ", " + value.getName() + ')',
                Reading.Item.Create.from(key),
                Reading.Item.Create.from(value)
        ));
    }

    @NonNull
    public static <V> Reading.Many<SparseArray<V>> sparseArray(@NonNull final Mapper.Read<Integer> key,
                                                               @NonNull final Value.Read<V> value) {
        return new Many<>(Plans.sparseArray(
                '(' + key.getName() + ", " + value.getName() + ')',
                key.prepareRead(),
                Reading.Item.Create.from(value)
        ));
    }

    @NonNull
    public static <V> Reading.Many<SparseArray<V>> sparseArray(@NonNull final Value.Read<Integer> key,
                                                               @NonNull final Mapper.Read<V> value) {
        return new Many<>(Plans.sparseArray(
                '(' + key.getName() + ", " + value.getName() + ')',
                Reading.Item.Create.from(key),
                value.prepareRead()
        ));
    }

    @NonNull
    public static <V> Reading.Many<SparseArray<V>> sparseArray(@NonNull final Mapper.Read<Integer> key,
                                                               @NonNull final Mapper.Read<V> value) {
        return new Many<>(Plans.sparseArray(
                '(' + key.getName() + ", " + value.getName() + ')',
                key.prepareRead(),
                value.prepareRead()
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
        return new Many<>(first.preparePlan().and(second.preparePlan()));
    }

    @NonNull
    public static <V, T> Reading.Single<T> convert(@NonNull final Reading.Single<V> reading,
                                                   @NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
        return new SingleConversion<>(reading, converter);
    }

    @NonNull
    public static <V, T> Reading.Many<T> convert(@NonNull final Reading.Many<V> reading,
                                                 @NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
        return new Many<>(reading.preparePlan().convert(Converters.from(converter)));
    }

    private static class Single<V> extends Reading.Single.Base<V> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Mapper.Read<V> mMapper;
        @NonNull
        private final Plan.Read<V> mCreate;

        private Single(@NonNull final Value.Read<V> value) {
            this(Mappers.read(value));
        }

        private Single(@NonNull final Mapper.Read<V> mapper) {
            super();

            mName = mapper.getName();
            mMapper = mapper;
            mCreate = Plans.single(mName, mapper.prepareRead());
        }

        @NonNull
        @Override
        public final Plan.Read<V> preparePlan() {
            return mCreate;
        }

        @NonNull
        @Override
        public final Plan.Read<V> preparePlan(@NonNull final V model) {
            return Plans.single(mName, mMapper.prepareRead(model));
        }
    }

    private static class Many<V> extends Reading.Many.Base<V> {

        @NonNull
        private final Plan.Read<V> mPlan;


        private Many(@NonNull final Plan.Read<V> create) {
            super();

            mPlan = create;
        }

        @NonNull
        @Override
        public final Plan.Read<V> preparePlan() {
            return mPlan;
        }
    }

    private static class SingleComposition<V, T> extends Reading.Single.Base<Pair<V, T>> {

        @NonNull
        private final Reading.Single<V> mFirst;
        @NonNull
        private final Reading.Single<T> mSecond;
        @NonNull
        private final Plan.Read<Pair<V, T>> mCreate;

        private SingleComposition(@NonNull final Reading.Single<V> first,
                                  @NonNull final Reading.Single<T> second) {
            super();

            mFirst = first;
            mSecond = second;
            mCreate = Plans.eagerly(first.preparePlan().and(second.preparePlan()));
        }

        @NonNull
        @Override
        public final Plan.Read<Pair<V, T>> preparePlan() {
            return mCreate;
        }

        @NonNull
        @Override
        public final Plan.Read<Pair<V, T>> preparePlan(@NonNull final Pair<V, T> pair) {
            final Plan.Read<Pair<V, T>> result;

            final V v = pair.first;
            final T t = pair.second;
            if ((v == null) && (t == null)) {
                result = preparePlan();
            } else {
                final Plan.Read<V> plan1 = (v == null) ?
                        mFirst.preparePlan() :
                        mFirst.preparePlan(v);
                final Plan.Read<T> plan2 = (t == null) ?
                        mSecond.preparePlan() :
                        mSecond.preparePlan(t);
                result = plan1.and(plan2);
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
        private final Plan.Read<T> mCreate;

        private SingleConversion(@NonNull final Reading.Single<V> reading,
                                 @NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
            super();

            mReading = reading;
            mConverter = converter;
            mFrom = Converters.from(converter);
            mCreate = Plans.eagerly(reading.preparePlan().convert(mFrom));
        }

        @NonNull
        @Override
        public final Plan.Read<T> preparePlan() {
            return mCreate;
        }

        @NonNull
        @Override
        public final Plan.Read<T> preparePlan(@NonNull final T t) {
            final Plan.Read<T> result;

            final Maybe<V> value = mConverter.to(something(t));
            if (value.isSomething()) {
                final V v = value.get();
                result = (v == null) ? preparePlan() : mReading.preparePlan(v).convert(mFrom);
            } else {
                result = preparePlan();
            }

            return result;
        }
    }

    private Readings() {
        super();
    }
}
