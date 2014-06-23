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

import java.util.Collection;
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
        return new Many<>(value, Plans.list(value.getName(), Reading.Item.Create.from(value)));
    }

    @NonNull
    public static <M> Reading.Many<List<M>> list(@NonNull final Mapper.Read<M> mapper) {
        return new Many<>(mapper, Plans.list(mapper.getName(), mapper.prepareRead()));
    }

    @NonNull
    public static <V> Reading.Many<Set<V>> set(@NonNull final Value.Read<V> value) {
        return new Many<>(value, Plans.set(value.getName(), Reading.Item.Create.from(value)));
    }

    @NonNull
    public static <M> Reading.Many<Set<M>> set(@NonNull final Mapper.Read<M> mapper) {
        return new Many<>(mapper, Plans.set(mapper.getName(), mapper.prepareRead()));
    }

    @NonNull
    public static <M> Reading.Many<Set<M>> difference(@NonNull final Reading.Many<Set<M>> reading,
                                                      @NonNull final Set<M> subtrahend) {
        return new Subtraction<>(reading, subtrahend);
    }

    @NonNull
    public static <K, V> Reading.Many<Map<K, V>> map(@NonNull final Value.Read<K> key,
                                                     @NonNull final Value.Read<V> value) {
        return new MapReading<>(key, value);
    }

    @NonNull
    public static <K, V> Reading.Many<Map<K, V>> map(@NonNull final Mapper.Read<K> key,
                                                     @NonNull final Value.Read<V> value) {
        return new MapReading<>(key, value);
    }

    @NonNull
    public static <K, V> Reading.Many<Map<K, V>> map(@NonNull final Value.Read<K> key,
                                                     @NonNull final Mapper.Read<V> value) {
        return new MapReading<>(key, value);
    }

    @NonNull
    public static <K, V> Reading.Many<Map<K, V>> map(@NonNull final Mapper.Read<K> key,
                                                     @NonNull final Mapper.Read<V> value) {
        return new MapReading<>(key, value);
    }

    @NonNull
    public static <V> Reading.Many<SparseArray<V>> sparseArray(@NonNull final Value.Read<Integer> key,
                                                               @NonNull final Value.Read<V> value) {
        return new SparseArrayReading<>(key, value);
    }

    @NonNull
    public static <V> Reading.Many<SparseArray<V>> sparseArray(@NonNull final Mapper.Read<Integer> key,
                                                               @NonNull final Value.Read<V> value) {
        return new SparseArrayReading<>(key, value);
    }

    @NonNull
    public static <V> Reading.Many<SparseArray<V>> sparseArray(@NonNull final Value.Read<Integer> key,
                                                               @NonNull final Mapper.Read<V> value) {
        return new SparseArrayReading<>(key, value);
    }

    @NonNull
    public static <V> Reading.Many<SparseArray<V>> sparseArray(@NonNull final Mapper.Read<Integer> key,
                                                               @NonNull final Mapper.Read<V> value) {
        return new SparseArrayReading<>(key, value);
    }

    @NonNull
    public static <V, T> Reading.Single<Pair<V, T>> convert(@NonNull final Reading.Single<V> first,
                                                            @NonNull final Reading.Single<T> second) {
        return new SingleComposition<>(first, second);
    }

    @NonNull
    public static <V, T> Reading.Many<Pair<V, T>> convert(@NonNull final Reading.Many<V> first,
                                                          @NonNull final Reading.Many<T> second) {
        return new ManyComposition<>(first, second);
    }

    @NonNull
    public static <V, T> Reading.Single<T> convert(@NonNull final Reading.Single<V> reading,
                                                   @NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
        return new SingleConversion<>(reading, converter);
    }

    @NonNull
    public static <V, T> Reading.Many<T> convert(@NonNull final Reading.Many<V> reading,
                                                 @NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
        return new ManyConversion<>(reading, converter);
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

    private static class Many<V, C extends Collection<V>> extends Reading.Many.Base<C> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Item.Create<V> mReading;
        @NonNull
        private final Plan.Read<C> mCreate;

        private Many(@NonNull final Value.Read<V> value,
                     @NonNull final Plan.Read<C> create) {
            super();

            mName = value.getName();
            mReading = Item.Create.from(value);
            mCreate = create;
        }

        private Many(@NonNull final Mapper.Read<V> mapper,
                     @NonNull final Plan.Read<C> create) {
            super();

            mName = mapper.getName();
            mReading = mapper.prepareRead();
            mCreate = create;
        }

        @NonNull
        @Override
        public final Plan.Read<C> preparePlan() {
            return mCreate;
        }

        @NonNull
        @Override
        public final Plan.Read<C> preparePlan(@NonNull final C values) {
            return Plans.many(mName, values, mReading);
        }
    }

    private static class Subtraction<M> extends Reading.Many.Base<Set<M>> {

        @NonNull
        private final Many<Set<M>> mReading;
        @NonNull
        private final Function<Set<M>, Set<M>> mSubtract;

        private Subtraction(@NonNull final Many<Set<M>> reading, @NonNull final Set<M> subtrahend) {
            super();

            mReading = reading;
            mSubtract = new Function<Set<M>, Set<M>>() {
                @NonNull
                @Override
                public Set<M> invoke(@NonNull final Set<M> minuend) {
                    minuend.removeAll(subtrahend);
                    return minuend;
                }
            };
        }

        @NonNull
        @Override
        public final Plan.Read<Set<M>> preparePlan() {
            return Plans.eagerly(mReading.preparePlan().map(mSubtract));
        }

        @NonNull
        @Override
        public final Plan.Read<Set<M>> preparePlan(@NonNull final Set<M> minuend) {
            return mReading.preparePlan(minuend).map(mSubtract);
        }
    }

    private static class MapReading<K, V> extends Reading.Many.Base<Map<K, V>> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Item.Create<K> mKey;
        @NonNull
        private final Item.Create<V> mValue;
        @NonNull
        private final Plan.Read<Map<K, V>> mCreate;

        private MapReading(@NonNull final Value.Read<K> key,
                           @NonNull final Value.Read<V> value) {
            this(
                    '(' + key.getName() + ", " + value.getName() + ')',
                    Item.Create.from(key),
                    Item.Create.from(value)
            );
        }

        private MapReading(@NonNull final Mapper.Read<K> key,
                           @NonNull final Value.Read<V> value) {
            this(
                    '(' + key.getName() + ", " + value.getName() + ')',
                    key.prepareRead(),
                    Item.Create.from(value)
            );
        }

        private MapReading(@NonNull final Value.Read<K> key,
                           @NonNull final Mapper.Read<V> value) {
            this(
                    '(' + key.getName() + ", " + value.getName() + ')',
                    Item.Create.from(key),
                    value.prepareRead()
            );
        }

        private MapReading(@NonNull final Mapper.Read<K> key,
                           @NonNull final Mapper.Read<V> value) {
            this(
                    '(' + key.getName() + ", " + value.getName() + ')',
                    key.prepareRead(),
                    value.prepareRead()
            );
        }

        private MapReading(@NonNls @NonNull final String name,
                           @NonNull final Item.Create<K> key,
                           @NonNull final Item.Create<V> value) {
            super();

            mName = name;
            mKey = key;
            mValue = value;
            mCreate = Plans.map(name, key, value);
        }

        @NonNull
        @Override
        public final Plan.Read<Map<K, V>> preparePlan() {
            return mCreate;
        }

        @NonNull
        @Override
        public final Plan.Read<Map<K, V>> preparePlan(@NonNull final Map<K, V> values) {
            return Plans.many(mName, values, mKey, mValue);
        }
    }

    private static class SparseArrayReading<V> extends Reading.Many.Base<SparseArray<V>> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Item.Create<Integer> mKey;
        @NonNull
        private final Item.Create<V> mValue;
        @NonNull
        private final Plan.Read<SparseArray<V>> mCreate;

        private SparseArrayReading(@NonNull final Value.Read<Integer> key,
                                   @NonNull final Value.Read<V> value) {
            this(
                    '(' + key.getName() + ", " + value.getName() + ')',
                    Item.Create.from(key),
                    Item.Create.from(value)
            );
        }

        private SparseArrayReading(@NonNull final Mapper.Read<Integer> key,
                                   @NonNull final Value.Read<V> value) {
            this(
                    '(' + key.getName() + ", " + value.getName() + ')',
                    key.prepareRead(),
                    Item.Create.from(value)
            );
        }

        private SparseArrayReading(@NonNull final Value.Read<Integer> key,
                                   @NonNull final Mapper.Read<V> value) {
            this(
                    '(' + key.getName() + ", " + value.getName() + ')',
                    Item.Create.from(key),
                    value.prepareRead()
            );
        }

        private SparseArrayReading(@NonNull final Mapper.Read<Integer> key,
                                   @NonNull final Mapper.Read<V> value) {
            this(
                    '(' + key.getName() + ", " + value.getName() + ')',
                    key.prepareRead(),
                    value.prepareRead()
            );
        }

        private SparseArrayReading(@NonNls @NonNull final String name,
                                   @NonNull final Item.Create<Integer> key,
                                   @NonNull final Item.Create<V> value) {
            super();

            mName = name;
            mKey = key;
            mValue = value;
            mCreate = Plans.sparseArray(name, key, value);
        }

        @NonNull
        @Override
        public final Plan.Read<SparseArray<V>> preparePlan() {
            return mCreate;
        }

        @NonNull
        @Override
        public final Plan.Read<SparseArray<V>> preparePlan(@NonNull final SparseArray<V> values) {
            return Plans.many(mName, values, mKey, mValue);
        }
    }

    private static class SingleComposition<V, T> extends Reading.Single.Base<Pair<V, T>> {

        @NonNull
        private final Reading.Single<V> mFirst;
        @NonNull
        private final Reading.Single<T> mSecond;

        private SingleComposition(@NonNull final Reading.Single<V> first,
                                  @NonNull final Reading.Single<T> second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @NonNull
        @Override
        public final Plan.Read<Pair<V, T>> preparePlan() {
            return Plans.eagerly(mFirst.preparePlan().and(mSecond.preparePlan()));
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

    private static class ManyComposition<V, T> extends Reading.Many.Base<Pair<V, T>> {

        @NonNull
        private final Reading.Many<V> mFirst;
        @NonNull
        private final Reading.Many<T> mSecond;

        private ManyComposition(@NonNull final Reading.Many<V> first,
                                @NonNull final Reading.Many<T> second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @NonNull
        @Override
        public final Plan.Read<Pair<V, T>> preparePlan() {
            return Plans.eagerly(mFirst.preparePlan().and(mSecond.preparePlan()));
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

        private SingleConversion(@NonNull final Reading.Single<V> reading,
                                 @NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
            super();

            mReading = reading;
            mConverter = converter;
            mFrom = Converters.from(converter);
        }

        @NonNull
        @Override
        public final Plan.Read<T> preparePlan() {
            return Plans.eagerly(mReading.preparePlan().convert(mFrom));
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

    private static class ManyConversion<T, V> extends Reading.Many.Base<T> {

        @NonNull
        private final Reading.Many<V> mReading;
        @NonNull
        private final Converter<Maybe<V>, Maybe<T>> mConverter;
        @NonNull
        private final Function<Maybe<V>, Maybe<T>> mFrom;

        private ManyConversion(@NonNull final Reading.Many<V> reading,
                               @NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
            super();

            mReading = reading;
            mConverter = converter;
            mFrom = Converters.from(converter);
        }

        @NonNull
        @Override
        public final Plan.Read<T> preparePlan() {
            return Plans.eagerly(mReading.preparePlan().convert(mFrom));
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
