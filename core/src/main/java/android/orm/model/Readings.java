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
import android.support.annotation.NonNull;
import android.util.SparseArray;

import org.jetbrains.annotations.NonNls;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static class Single<V> implements Reading.Single<V> {

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

    private static class Many<V, C extends Collection<V>> implements Reading.Many<C> {

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

    private static class MapReading<K, V> implements Reading.Many<Map<K, V>> {

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

    private static class SparseArrayReading<V> implements Reading.Many<SparseArray<V>> {

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

    private Readings() {
        super();
    }
}
