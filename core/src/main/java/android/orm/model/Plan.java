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
import android.orm.sql.Writer;
import android.orm.util.Function;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;

import static android.orm.model.Plans.EmptyWrite;
import static java.util.Arrays.asList;

public final class Plan {

    public abstract static class Read<M> implements Reader<M> {

        @NonNull
        private final Select.Projection mProjection;

        protected Read(@NonNull final Select.Projection projection) {
            super();

            mProjection = projection;
        }

        @NonNull
        public final Select.Projection getProjection() {
            return mProjection;
        }

        public final boolean isEmpty() {
            return mProjection.isEmpty();
        }

        @NonNull
        @Override
        public final <T> Reader<Pair<M, T>> and(@NonNull final Reader<T> other) {
            return Readers.compose(this, other);
        }

        @NonNull
        public final <T> Read<Pair<M, T>> and(@NonNull final Read<T> other) {
            return Plans.compose(this, other);
        }

        @NonNull
        @Override
        public final <N> Read<N> map(@NonNull final Function<? super M, ? extends N> converter) {
            return convert(Maybes.map(converter));
        }

        @NonNull
        @Override
        public final <N> Read<N> flatMap(@NonNull final Function<? super M, Maybe<N>> converter) {
            return convert(Maybes.flatMap(converter));
        }

        @NonNull
        @Override
        public final <N> Read<N> convert(@NonNull final Function<Maybe<M>, Maybe<N>> converter) {
            return Plans.convert(this, converter);
        }
    }

    public abstract static class Write implements Writer {

        protected Write() {
            super();
        }

        public abstract boolean isEmpty();

        @NonNull
        public final Write and(@NonNull final Write other) {
            return other.isEmpty() ? this : (isEmpty() ? other : Plans.compose(asList(this, other)));
        }

        @NonNull
        public static <M> Builder<M> builder() {
            return new Builder<>();
        }

        public static class Builder<M> {

            @NonNull
            private final Collection<Function<M, Write>> mEntries;

            public Builder() {
                super();

                mEntries = new ArrayList<>();
            }

            public Builder(@NonNull final Builder<M> builder) {
                super();

                mEntries = new ArrayList<>(builder.mEntries);
            }

            @NonNull
            public final Builder<M> put(@NonNull final Writer writer) {
                return put(Builder.<M>entry(writer));
            }

            @NonNull
            public final <V> Builder<M> put(@NonNull final Value.Write<V> value,
                                            @NonNull final Lens.Read<M, Maybe<V>> lens) {
                return put(entry(value, lens));
            }

            @NonNull
            public final <V> Builder<M> put(@NonNull final Mapper.Write<V> mapper,
                                            @NonNull final Lens.Read<M, Maybe<V>> lens) {
                return put(entry(mapper, lens));
            }

            @NonNull
            public final Write build(@NonNull final M model) {
                return build(model, mEntries);
            }

            @NonNull
            private Builder<M> put(@NonNull final Function<M, Write> entry) {
                mEntries.add(entry);
                return this;
            }

            @NonNull
            private static <M> Function<M, Write> entry(@NonNull final Writer writer) {
                return new Function<M, Write>() {
                    @NonNull
                    @Override
                    public Write invoke(@NonNull final M ignored) {
                        return Plans.write(writer);
                    }
                };
            }

            @NonNull
            private static <M, V> Function<M, Write> entry(@NonNull final Value.Write<V> value,
                                                           @NonNull final Lens.Read<M, Maybe<V>> lens) {
                return new Function<M, Write>() {
                    @NonNull
                    @Override
                    public Write invoke(@NonNull final M model) {
                        final Maybe<V> v = lens.get(model);
                        return Plans.write((v == null) ? Maybes.<V>something(null) : v, value);
                    }
                };
            }

            @NonNull
            private static <M, V> Function<M, Write> entry(@NonNull final Mapper.Write<V> mapper,
                                                           @NonNull final Lens.Read<M, Maybe<V>> lens) {
                return new Function<M, Write>() {
                    @NonNull
                    @Override
                    public Write invoke(@NonNull final M model) {
                        final Write plan;

                        final Maybe<V> value = lens.get(model);
                        if ((value != null) && value.isSomething()) {
                            final V v = value.get();
                            plan = (v == null) ? EmptyWrite : mapper.prepareWrite(v);
                        } else {
                            plan = EmptyWrite;
                        }

                        return plan;
                    }
                };
            }

            @NonNull
            private static <M> Write build(@NonNull final M model,
                                           @NonNull final Collection<Function<M, Write>> entries) {
                final Collection<Write> plans = new ArrayList<>(entries.size());

                for (final Function<M, Write> entry : entries) {
                    plans.add(entry.invoke(model));
                }

                return Plans.compose(plans);
            }
        }
    }

    private Plan() {
        super();
    }
}
