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

import android.orm.sql.Readable;
import android.orm.sql.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.orm.util.Producers;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.util.Log.DEBUG;

public final class Plans {

    private static final String TAG = Plan.Read.class.getSimpleName();

    private static final Plan.Read<Object> EmptyRead = new Plan.Read<Object>(Select.Projection.Nothing) {

        private final Producer<Maybe<Object>> mNothing = Producers.constant(nothing());

        @NonNull
        @Override
        public Producer<Maybe<Object>> read(@NonNull final Readable input) {
            return mNothing;
        }
    };

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Plan.Read<V> emptyRead() {
        return (Plan.Read<V>) EmptyRead;
    }

    @NonNull
    public static <V> Plan.Read<V> eagerly(@NonNull final Plan.Read<V> plan) {
        return new Plan.Read<V>(plan.getProjection()) {
            @NonNull
            @Override
            public Producer<Maybe<V>> read(@NonNull final Readable input) {
                return Producers.constant(plan.read(input).produce());
            }
        };
    }

    @NonNull
    public static <V> Plan.Read<V> single(@NonNls @NonNull final String name,
                                          @NonNull final Reading.Item<V> reading) {
        return reading.isEmpty() ? Plans.<V>emptyRead() : new Single<>(name, reading);
    }

    @NonNull
    public static <V> Plan.Read<List<V>> list(@NonNls @NonNull final String name,
                                              @NonNull final Reading.Item.Create<V> reading) {
        return reading.isEmpty() ?
                Plans.<List<V>>emptyRead() :
                new Many<V, List<V>>(name, reading) {

                    @NonNull
                    @Override
                    protected List<V> create(final int size) {
                        return new ArrayList<>(size);
                    }

                    @Override
                    protected void add(@NonNull final List<V> to, @NonNull final V value) {
                        to.add(value);
                    }
                };
    }

    @NonNull
    public static <V> Plan.Read<Set<V>> set(@NonNls @NonNull final String name,
                                            @NonNull final Reading.Item.Create<V> reading) {
        return reading.isEmpty() ?
                Plans.<Set<V>>emptyRead() :
                new Many<V, Set<V>>(name, reading) {

                    @NonNull
                    @Override
                    protected Set<V> create(final int size) {
                        return new HashSet<>(size);
                    }

                    @Override
                    protected void add(@NonNull final Set<V> to, @NonNull final V value) {
                        to.add(value);
                    }
                };
    }

    @NonNull
    public static <K, V> Plan.Read<Map<K, V>> map(@NonNls @NonNull final String name,
                                                  @NonNull final Reading.Item.Create<K> key,
                                                  @NonNull final Reading.Item.Create<V> value) {
        return (key.isEmpty() || value.isEmpty()) ?
                Plans.<Map<K, V>>emptyRead() :
                new Many<Pair<K, V>, Map<K, V>>(name, key.and(value)) {

                    @NonNull
                    @Override
                    protected Map<K, V> create(final int size) {
                        return new HashMap<>(size);
                    }

                    @Override
                    protected void add(@NonNull final Map<K, V> to,
                                       @NonNull final Pair<K, V> pair) {
                        to.put(pair.first, pair.second);
                    }
                };
    }

    @NonNull
    public static <V> Plan.Read<SparseArray<V>> sparseArray(@NonNls @NonNull final String name,
                                                            @NonNull final Reading.Item.Create<Integer> key,
                                                            @NonNull final Reading.Item.Create<V> value) {
        return (key.isEmpty() || value.isEmpty()) ?
                Plans.<SparseArray<V>>emptyRead() :
                new Many<Pair<Integer, V>, SparseArray<V>>(name, key.and(value)) {

                    @NonNull
                    @Override
                    protected SparseArray<V> create(final int size) {
                        return new SparseArray<>(size);
                    }

                    @Override
                    protected void add(@NonNull final SparseArray<V> to,
                                       @NonNull final Pair<Integer, V> pair) {
                        to.put(pair.first, pair.second);
                    }
                };
    }

    @NonNull
    public static <V, T> Plan.Read<Pair<V, T>> compose(@NonNull final Plan.Read<V> first,
                                                       @NonNull final Plan.Read<T> second) {
        return new ReadComposition<>(first, second);
    }

    @NonNull
    public static <V, T> Plan.Read<T> convert(@NonNull final Plan.Read<V> plan,
                                              @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
        return new Conversion<>(plan, converter);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Plan.Read<V> safeCast(@NonNull final Plan.Read<? extends V> plan) {
        return (Plan.Read<V>) plan;
    }

    private static class Single<V> extends Plan.Read<V> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Reading.Item<V> mReading;

        private Single(@NonNls @NonNull final String name, @NonNull final Reading.Item<V> reading) {
            super(reading.getProjection());

            mName = name;
            mReading = reading;
        }

        @NonNull
        @Override
        public final Producer<Maybe<V>> read(@NonNull final Readable input) {
            final Producer<Maybe<V>> result;

            @NonNls final int size = input.size();
            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, "Rows in input for " + mName + ": " + size); //NON-NLS
            }

            if (input.start()) {
                result = mReading.read(input);
                if (size > 1) {
                    Log.w(TAG, "Reading a single '" + mName + "' from cursor that contains multiple ones. Please consider from list/set item"); //NON-NLS
                }
            } else {
                result = Producers.constant(Maybes.<V>nothing());
            }

            return result;
        }
    }

    private abstract static class Many<V, C> extends Plan.Read<C> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Reading.Item.Create<V> mReading;

        private Many(@NonNls @NonNull final String name,
                     @NonNull final Reading.Item.Create<V> reading) {
            super(reading.getProjection());

            mName = name;
            mReading = reading;
        }

        @NonNull
        protected abstract C create(final int size);

        protected abstract void add(@NonNull final C to, @NonNull final V v);

        @NonNull
        @Override
        public final Producer<Maybe<C>> read(@NonNull final Readable input) {
            final C result = create(input.size());

            @NonNls final int size = input.size();
            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, "Rows in input for " + mName + ": " + size); //NON-NLS
            }

            if (input.start()) {
                do {
                    final V model = mReading.read(input).produce().getOrElse(null);
                    if (model != null) {
                        add(result, model);
                    }
                } while (input.next());
            }

            return Producers.constant(something(result));
        }
    }

    private static class ReadComposition<V, T> extends Plan.Read<Pair<V, T>> {

        @NonNull
        private final Plan.Read<V> mFirst;
        @NonNull
        private final Plan.Read<T> mSecond;

        private ReadComposition(@NonNull final Plan.Read<V> first,
                                @NonNull final Plan.Read<T> second) {
            super(first.getProjection().and(second.getProjection()));

            mFirst = first;
            mSecond = second;
        }

        @NonNull
        @Override
        public final Producer<Maybe<Pair<V, T>>> read(@NonNull final Readable input) {
            return Producers.convert(
                    Producers.compose(mFirst.read(input), mSecond.read(input)),
                    Maybes.<V, T>liftPair()
            );
        }
    }

    private static class Conversion<V, T> extends Plan.Read<T> {

        @NonNull
        private final Plan.Read<V> mPlan;
        @NonNull
        private final Function<Maybe<V>, Maybe<T>> mConverter;

        private Conversion(@NonNull final Plan.Read<V> plan,
                           @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
            super(plan.getProjection());

            mPlan = plan;
            mConverter = converter;
        }

        @NonNull
        @Override
        public final Producer<Maybe<T>> read(@NonNull final Readable input) {
            return Producers.convert(mPlan.read(input), mConverter);
        }
    }

    private Plans() {
        super();
    }
}
