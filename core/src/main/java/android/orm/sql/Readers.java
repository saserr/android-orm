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

package android.orm.sql;

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

import static android.orm.util.Maybes.something;
import static android.util.Log.DEBUG;

public final class Readers {

    private static final String TAG = Reader.class.getSimpleName();

    @NonNull
    public static <V> Reader.Collection<V> single(@NonNls @NonNull final String name,
                                                  @NonNull final Reader.Element<V> element) {
        return new Single<>(name, element);
    }

    @NonNull
    public static <V> Reader.Collection<List<V>> list(@NonNls @NonNull final String name,
                                                      @NonNull final Reader.Element.Create<V> element) {
        return new Many<V, List<V>>(name, element) {

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
    public static <V> Reader.Collection<Set<V>> set(@NonNls @NonNull final String name,
                                                    @NonNull final Reader.Element.Create<V> element) {
        return new Many<V, Set<V>>(name, element) {

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
    public static <K, V> Reader.Collection<Map<K, V>> map(@NonNls @NonNull final String name,
                                                          @NonNull final Reader.Element.Create<K> key,
                                                          @NonNull final Reader.Element.Create<V> value) {
        return new Many<Pair<K, V>, Map<K, V>>(name, key.and(value)) {

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
    public static <V> Reader.Collection<SparseArray<V>> sparseArray(@NonNls @NonNull final String name,
                                                                    @NonNull final Reader.Element.Create<Integer> key,
                                                                    @NonNull final Reader.Element.Create<V> value) {
        return new Many<Pair<Integer, V>, SparseArray<V>>(name, key.and(value)) {

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
    public static <V, T> Reader.Collection<Pair<V, T>> compose(@NonNull final Reader.Collection<V> first,
                                                               @NonNull final Reader.Collection<T> second) {
        return new Composition<>(first, second);
    }

    @NonNull
    public static <V, T> Reader.Collection<T> convert(@NonNull final Reader.Collection<V> reader,
                                                      @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
        return new Conversion<>(reader, converter);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Reader.Collection<V> safeCast(@NonNull final Reader.Collection<? extends V> reader) {
        return (Reader.Collection<V>) reader;
    }

    private static class Single<V> extends Reader.Collection.Base<V> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Reader.Element<V> mElement;

        private Single(@NonNls @NonNull final String name,
                       @NonNull final Reader.Element<V> element) {
            super();

            mName = name;
            mElement = element;
        }

        @NonNull
        @Override
        public final Select.Projection getProjection() {
            return mElement.getProjection();
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
                result = mElement.read(input);
                if (size > 1) {
                    Log.w(TAG, "Reading a single '" + mName + "' from cursor that contains multiple ones. Please consider from list/set item"); //NON-NLS
                }
            } else {
                result = Producers.constant(Maybes.<V>nothing());
            }

            return result;
        }
    }

    private abstract static class Many<V, C> extends Reader.Collection.Base<C> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Reader.Element.Create<V> mElement;

        private Many(@NonNls @NonNull final String name,
                     @NonNull final Reader.Element.Create<V> element) {
            super();

            mName = name;
            mElement = element;
        }

        @NonNull
        protected abstract C create(final int size);

        protected abstract void add(@NonNull final C to, @NonNull final V v);

        @NonNull
        @Override
        public final Select.Projection getProjection() {
            return mElement.getProjection();
        }

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
                    final V model = mElement.read(input).produce().getOrElse(null);
                    if (model != null) {
                        add(result, model);
                    }
                } while (input.next());
            }

            return Producers.constant(something(result));
        }
    }

    private static class Composition<V, T> extends Reader.Collection.Base<Pair<V, T>> {

        @NonNull
        private final Reader.Collection<V> mFirst;
        @NonNull
        private final Reader.Collection<T> mSecond;
        @NonNull
        private final Select.Projection mProjection;

        private Composition(@NonNull final Reader.Collection<V> first,
                            @NonNull final Reader.Collection<T> second) {
            super();

            mFirst = first;
            mSecond = second;
            mProjection = mFirst.getProjection().and(mSecond.getProjection());
        }

        @NonNull
        @Override
        public final Select.Projection getProjection() {
            return mProjection;
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

    private static class Conversion<V, T> extends Reader.Collection.Base<T> {

        @NonNull
        private final Reader.Collection<V> mReader;
        @NonNull
        private final Function<Maybe<V>, Maybe<T>> mConverter;

        private Conversion(@NonNull final Reader.Collection<V> reader,
                           @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
            super();

            mReader = reader;
            mConverter = converter;
        }

        @NonNull
        @Override
        public final Select.Projection getProjection() {
            return mReader.getProjection();
        }

        @NonNull
        @Override
        public final Producer<Maybe<T>> read(@NonNull final Readable input) {
            return Producers.convert(mReader.read(input), mConverter);
        }
    }

    private Readers() {
        super();
    }
}
