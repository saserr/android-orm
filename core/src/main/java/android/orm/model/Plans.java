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

import android.content.ContentValues;
import android.orm.sql.Readable;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.sql.statement.Select;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.orm.util.Producers.constant;
import static android.util.Log.DEBUG;

public final class Plans {

    private static final String TAG = Plan.Read.class.getSimpleName();

    public static final Plan.Write EmptyWrite = new Plan.Write() {

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void write(@Value.Write.Operation final int operation,
                          @NonNull final Writable output) {/* do nothing */}
    };

    private static final Plan.Read<Object> EmptyRead = new Plan.Read<Object>(Select.Projection.Nothing) {
        @NonNull
        @Override
        public Producer<Maybe<Object>> read(@NonNull final Readable input) {
            return constant(nothing());
        }
    };

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Plan.Read<V> emptyRead() {
        return (Plan.Read<V>) EmptyRead;
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
                new Plan.Read<List<V>>(reading.getProjection()) {
                    @NonNull
                    @Override
                    public Producer<Maybe<List<V>>> read(@NonNull final Readable input) {
                        final List<V> values = new ArrayList<>(input.size());
                        Many.read(name, reading, input, values);
                        return constant(something(values));
                    }
                };
    }

    @NonNull
    public static <V> Plan.Read<Set<V>> set(@NonNls @NonNull final String name,
                                            @NonNull final Reading.Item.Create<V> reading) {
        return reading.isEmpty() ?
                Plans.<Set<V>>emptyRead() :
                new Plan.Read<Set<V>>(reading.getProjection()) {
                    @NonNull
                    @Override
                    public Producer<Maybe<Set<V>>> read(@NonNull final Readable input) {
                        final Set<V> values = new HashSet<>(input.size());
                        Many.read(name, reading, input, values);
                        return constant(something(values));
                    }
                };
    }

    @NonNull
    public static <K, V> Plan.Read<Map<K, V>> map(@NonNls @NonNull final String name,
                                                  @NonNull final Reading.Item.Create<K> key,
                                                  @NonNull final Reading.Item.Create<V> value) {
        return (key.isEmpty() || value.isEmpty()) ?
                Plans.<Map<K, V>>emptyRead() :
                new Many.Map<K, V>(name, key, value) {
                    @NonNull
                    @Override
                    public Producer<Maybe<Map<K, V>>> read(@NonNull final Readable input) {
                        return eager(new HashMap<K, V>(input.size()), input);
                    }
                };
    }

    @NonNull
    public static <V> Plan.Read<SparseArray<V>> sparseArray(@NonNls @NonNull final String name,
                                                            @NonNull final Reading.Item.Create<Integer> key,
                                                            @NonNull final Reading.Item.Create<V> value) {
        return (key.isEmpty() || value.isEmpty()) ?
                Plans.<SparseArray<V>>emptyRead() :
                new Many.SparseArray<V>(name, key, value) {
                    @NonNull
                    @Override
                    public Producer<Maybe<SparseArray<V>>> read(@NonNull final Readable input) {
                        return eager(new SparseArray<V>(input.size()), input);
                    }
                };
    }

    @NonNull
    public static <V, C extends Collection<V>> Plan.Read<C> many(@NonNls @NonNull final String name,
                                                                 @NonNull final C values,
                                                                 @NonNull final Reading.Item.Create<V> reading) {
        return reading.isEmpty() ?
                Plans.<C>emptyRead() :
                new Many.Collection<V, C>(name, reading) {
                    @NonNull
                    @Override
                    public Producer<Maybe<C>> read(@NonNull final Readable input) {
                        return lazy(values, input);
                    }
                };
    }

    @NonNull
    public static <K, V> Plan.Read<Map<K, V>> many(@NonNls @NonNull final String name,
                                                   @NonNull final Map<K, V> values,
                                                   @NonNull final Reading.Item.Create<K> key,
                                                   @NonNull final Reading.Item.Create<V> value) {
        return (key.isEmpty() || value.isEmpty()) ?
                Plans.<Map<K, V>>emptyRead() :
                new Many.Map<K, V>(name, key, value) {
                    @NonNull
                    @Override
                    public Producer<Maybe<Map<K, V>>> read(@NonNull final Readable input) {
                        return lazy(values, input);
                    }
                };
    }

    @NonNull
    public static <V> Plan.Read<SparseArray<V>> many(@NonNls @NonNull final String name,
                                                     @NonNull final SparseArray<V> values,
                                                     @NonNull final Reading.Item.Create<Integer> key,
                                                     @NonNull final Reading.Item.Create<V> value) {
        return (key.isEmpty() || value.isEmpty()) ?
                Plans.<SparseArray<V>>emptyRead() :
                new Many.SparseArray<V>(name, key, value) {
                    @NonNull
                    @Override
                    public Producer<Maybe<SparseArray<V>>> read(@NonNull final Readable input) {
                        return lazy(values, input);
                    }
                };
    }

    @NonNull
    public static <M extends Instance.Writable> Plan.Write write(@NonNull final M model) {
        return model.prepareWrite();
    }

    @NonNull
    public static <V> Plan.Write write(@NonNull final Value.Write<V> value,
                                       @NonNull final Instance.Getter<V> getter) {
        return write(getter.get(), value);
    }

    @NonNull
    public static <V> Plan.Write write(@NonNull final Mapper.Write<V> mapper,
                                       @NonNull final Instance.Getter<V> getter) {
        return mapper.prepareWrite(getter.get());
    }

    @NonNull
    public static <V> Plan.Write write(@NonNull final Maybe<V> model,
                                       @NonNull final Value.Write<V> value) {
        return new Plan.Write() {

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public void write(@Value.Write.Operation final int operation,
                              @NonNull final Writable output) {
                value.write(operation, model, output);
            }
        };
    }

    @NonNull
    public static Plan.Write write(@NonNull final ContentValues values) {
        return new Plan.Write() {

            private final boolean mIsEmpty = values.size() > 0;

            @Override
            public boolean isEmpty() {
                return mIsEmpty;
            }

            @Override
            public void write(@Value.Write.Operation final int operation,
                              @NonNull final Writable output) {
                output.putAll(values);
            }
        };
    }

    @NonNull
    public static Plan.Write compose(@NonNull final Iterable<Plan.Write> plans) {
        boolean isEmpty = true;

        for (final Plan.Write plan : plans) {
            isEmpty = isEmpty && plan.isEmpty();
        }

        return isEmpty ? EmptyWrite : new Composition(plans);
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
                result = constant(Maybes.<V>nothing());
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

        protected abstract void copy(@NonNull final List<V> from, @NonNull final C to);

        @NonNull
        public final Producer<Maybe<C>> eager(@NonNull final C result,
                                              @NonNull final Readable input) {
            final List<V> values = new ArrayList<>(input.size());
            read(mName, mReading, input, values);
            copy(values, result);
            return constant(something(result));
        }

        @NonNull
        public final Producer<Maybe<C>> lazy(@NonNull final C result,
                                             @NonNull final Readable input) {
            final List<V> values = new ArrayList<>(input.size());
            read(mName, mReading, input, values);
            return Reading.Item.lazy(result, new Runnable() {
                @Override
                public void run() {
                    copy(values, result);
                }
            });
        }

        private static <V, C extends java.util.Collection<V>> void read(@NonNls @NonNull final String name,
                                                                        @NonNull final Reading.Item.Create<V> reading,
                                                                        @NonNull final Readable input,
                                                                        @NonNull final C values) {
            @NonNls final int size = input.size();
            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, "Rows in input for " + name + ": " + size); //NON-NLS
            }

            if (input.start()) {
                do {
                    final V model = reading.read(input).produce().getOrElse(null);
                    if (model != null) {
                        values.add(model);
                    }
                } while (input.next());
            }
        }

        private abstract static class Collection<V, C extends java.util.Collection<V>> extends Many<V, C> {

            private Collection(@NonNls @NonNull final String name,
                               @NonNull final Reading.Item.Create<V> reading) {
                super(name, reading);
            }

            @Override
            protected final void copy(@NonNull final List<V> from, @NonNull final C to) {
                to.clear();
                to.addAll(from);
            }
        }

        private abstract static class Map<K, V> extends Many<Pair<K, V>, java.util.Map<K, V>> {

            private Map(@NonNls @NonNull final String name,
                        @NonNull final Reading.Item.Create<K> key,
                        @NonNull final Reading.Item.Create<V> value) {
                super(name, key.and(value));
            }

            @Override
            protected final void copy(@NonNull final List<Pair<K, V>> from,
                                      @NonNull final java.util.Map<K, V> to) {
                for (final Pair<K, V> pair : from) {
                    to.put(pair.first, pair.second);
                }
            }
        }

        private abstract static class SparseArray<V> extends Many<Pair<Integer, V>, android.util.SparseArray<V>> {

            private SparseArray(@NonNls @NonNull final String name,
                                @NonNull final Reading.Item.Create<Integer> key,
                                @NonNull final Reading.Item.Create<V> value) {
                super(name, key.and(value));
            }

            @Override
            protected final void copy(@NonNull final List<Pair<Integer, V>> from,
                                      @NonNull final android.util.SparseArray<V> to) {
                for (final Pair<Integer, V> pair : from) {
                    to.put(pair.first, pair.second);
                }
            }
        }
    }

    private static class Composition extends Plan.Write {

        @NonNull
        private final Iterable<Plan.Write> mPlans;

        private Composition(@NonNull final Iterable<Plan.Write> plans) {
            super();

            mPlans = plans;
        }

        @Override
        public final boolean isEmpty() {
            return false;
        }

        @Override
        public final void write(@Value.Write.Operation final int operation,
                                @NonNull final Writable output) {
            for (final Plan.Write plan : mPlans) {
                plan.write(operation, output);
            }
        }
    }

    private Plans() {
        super();
    }
}
