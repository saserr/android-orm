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
import android.orm.sql.Writer;
import android.orm.util.Function;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;

import static android.orm.util.Maybes.nothing;
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
            private final Collection<Function<Maybe<M>, Write>> mEntries;

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
                mEntries.add(Builder.<M>entry(writer));
                return this;
            }

            @NonNull
            public final <V> Builder<M> put(@NonNull final Mapper.Write<V> mapper,
                                            @NonNull final Lens.Read<M, Maybe<V>> lens) {
                mEntries.add(entry(mapper, lens));
                return this;
            }

            @NonNull
            public final Write build(@NonNull final Maybe<M> result) {
                return build(result, mEntries);
            }

            @NonNull
            private static <M> Function<Maybe<M>, Write> entry(@NonNull final Writer writer) {
                return new Function<Maybe<M>, Write>() {
                    @NonNull
                    @Override
                    public Write invoke(@NonNull final Maybe<M> ignored) {
                        return Plans.write(writer);
                    }
                };
            }

            @NonNull
            private static <M, V> Function<Maybe<M>, Write> entry(@NonNull final Mapper.Write<V> mapper,
                                                                  @NonNull final Lens.Read<M, Maybe<V>> lens) {
                return new Function<Maybe<M>, Write>() {
                    @NonNull
                    @Override
                    public Write invoke(@NonNull final Maybe<M> value) {
                        final Maybe<V> result;

                        if (value.isSomething()) {
                            final M model = value.get();
                            result = (model == null) ? Maybes.<V>something(null) : lens.get(model);
                        } else {
                            result = nothing();
                        }

                        return mapper.prepareWrite(
                                (result == null) ? Maybes.<V>something(null) : result
                        );
                    }
                };
            }

            @NonNull
            private static <M> Write build(@NonNull final Maybe<M> result,
                                           @NonNull final Collection<Function<Maybe<M>, Write>> entries) {
                final Collection<Write> plans = new ArrayList<>(entries.size());

                for (final Function<Maybe<M>, Write> entry : entries) {
                    final Write plan = entry.invoke(result);
                    if (!plan.isEmpty()) {
                        plans.add(plan);
                    }
                }

                return Plans.compose(plans);
            }
        }
    }

    private Plan() {
        super();
    }
}
