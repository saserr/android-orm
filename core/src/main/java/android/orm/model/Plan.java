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

import android.orm.sql.Readable;
import android.orm.sql.Reader;
import android.orm.sql.Readers;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Values;
import android.orm.sql.Writer;
import android.orm.util.Function;
import android.orm.util.Lens;
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

import static android.orm.util.Lenses.get;
import static android.orm.util.Maybes.something;
import static android.util.Log.DEBUG;

public final class Plan {

    public static final class Read {

        private static final String TAG = Read.class.getSimpleName();

        @NonNull
        public static <V> Reader<V> single(@NonNls @NonNull final String name,
                                           @NonNull final Reading.Item<V> reading) {
            return reading.isEmpty() ? Readers.<V>empty() : new Single<>(name, reading);
        }

        @NonNull
        public static <V> Reader<List<V>> list(@NonNls @NonNull final String name,
                                               @NonNull final Reading.Item.Create<V> reading) {
            return reading.isEmpty() ?
                    Readers.<List<V>>empty() :
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
        public static <V> Reader<Set<V>> set(@NonNls @NonNull final String name,
                                             @NonNull final Reading.Item.Create<V> reading) {
            return reading.isEmpty() ?
                    Readers.<Set<V>>empty() :
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
        public static <K, V> Reader<Map<K, V>> map(@NonNls @NonNull final String name,
                                                   @NonNull final Reading.Item.Create<K> key,
                                                   @NonNull final Reading.Item.Create<V> value) {
            return (key.isEmpty() || value.isEmpty()) ?
                    Readers.<Map<K, V>>empty() :
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
        public static <V> Reader<SparseArray<V>> sparseArray(@NonNls @NonNull final String name,
                                                             @NonNull final Reading.Item.Create<Integer> key,
                                                             @NonNull final Reading.Item.Create<V> value) {
            return (key.isEmpty() || value.isEmpty()) ?
                    Readers.<SparseArray<V>>empty() :
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

        private static class Single<V> extends Reader.Base<V> {

            @NonNls
            @NonNull
            private final String mName;
            @NonNull
            private final Reading.Item<V> mReading;

            private Single(@NonNls @NonNull final String name, @NonNull final Reading.Item<V> reading) {
                super();

                mName = name;
                mReading = reading;
            }

            @NonNull
            @Override
            public final Select.Projection getProjection() {
                return mReading.getProjection();
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

        private abstract static class Many<V, C> extends Reader.Base<C> {

            @NonNls
            @NonNull
            private final String mName;
            @NonNull
            private final Reading.Item.Create<V> mReading;

            private Many(@NonNls @NonNull final String name,
                         @NonNull final Reading.Item.Create<V> reading) {
                super();

                mName = name;
                mReading = reading;
            }

            @NonNull
            protected abstract C create(final int size);

            protected abstract void add(@NonNull final C to, @NonNull final V v);

            @NonNull
            @Override
            public final Select.Projection getProjection() {
                return mReading.getProjection();
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
                        final V model = mReading.read(input).produce().getOrElse(null);
                        if (model != null) {
                            add(result, model);
                        }
                    } while (input.next());
                }

                return Producers.constant(something(result));
            }
        }

        private Read() {
            super();
        }
    }

    public static final class Write {

        public static class Builder<M> {

            @NonNull
            private final Writer.Builder<Maybe<M>> mWriter;

            public Builder() {
                super();

                mWriter = new Writer.Builder<>();
            }

            public Builder(@NonNull final Builder<M> builder) {
                super();

                mWriter = new Writer.Builder<>(builder.mWriter);
            }

            @NonNull
            public final Builder<M> with(@NonNull final Writer writer) {
                mWriter.with(writer);
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Value.Write<V> value,
                                             @NonNull final Lens.Read<M, Maybe<V>> lens) {
                mWriter.with(factory(value, lens));
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Mapper.Write<V> mapper,
                                             @NonNull final Lens.Read<M, Maybe<V>> lens) {
                mWriter.with(factory(mapper, lens));
                return this;
            }

            @NonNull
            public final Writer build(@NonNull final Maybe<M> model) {
                return mWriter.build(model);
            }

            @NonNull
            private static <M, V> Function<Maybe<M>, Writer> factory(@NonNull final Value.Write<V> value,
                                                                     @NonNull final Lens.Read<M, Maybe<V>> lens) {
                return new Function<Maybe<M>, Writer>() {
                    @NonNull
                    @Override
                    public Writer invoke(@NonNull final Maybe<M> model) {
                        return Values.value(value, get(model, lens));
                    }
                };
            }

            @NonNull
            private static <M, V> Function<Maybe<M>, Writer> factory(@NonNull final Mapper.Write<V> mapper,
                                                                     @NonNull final Lens.Read<M, Maybe<V>> lens) {
                return new Function<Maybe<M>, Writer>() {
                    @NonNull
                    @Override
                    public Writer invoke(@NonNull final Maybe<M> model) {
                        return mapper.prepareWrite(get(model, lens));
                    }
                };
            }
        }

        private Write() {
            super();
        }
    }

    private Plan() {
        super();
    }
}
