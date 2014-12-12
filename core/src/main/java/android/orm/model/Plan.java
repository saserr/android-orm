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
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Condition;
import android.orm.util.Function;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;

import static android.orm.util.Lenses.get;
import static java.util.Arrays.asList;

public final class Plan {

    public abstract static class Read<V> implements Reader<V> {

        @NonNull
        private final Select.Projection mProjection;

        protected Read(@NonNull final Select.Projection projection) {
            super();

            mProjection = projection;
        }

        @NonNull
        @Override
        public final Select.Projection getProjection() {
            return mProjection;
        }

        public final boolean isEmpty() {
            return mProjection.isEmpty();
        }

        @NonNull
        public final <T> Read<Pair<V, T>> and(@NonNull final Read<T> other) {
            return Plans.compose(this, other);
        }

        @NonNull
        public final <T> Read<T> map(@NonNull final Function<? super V, ? extends T> converter) {
            return convert(Maybes.map(converter));
        }

        @NonNull
        public final <T> Read<T> flatMap(@NonNull final Function<? super V, Maybe<T>> converter) {
            return convert(Maybes.flatMap(converter));
        }

        @NonNull
        public final <T> Read<T> convert(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
            return Plans.convert(this, converter);
        }
    }

    public abstract static class Write implements Writer {

        @NonNull
        private final Condition mOnUpdate;

        protected Write(@NonNull final Condition onUpdate) {
            super();

            mOnUpdate = onUpdate;
        }

        public abstract boolean isEmpty();

        @NonNull
        @Override
        public final Condition onUpdate() {
            return mOnUpdate;
        }

        @NonNull
        public final Write and(@NonNull final Write other) {
            return other.isEmpty() ?
                    this :
                    (isEmpty() ? other : Plans.compose(asList(this, other)));
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
            public final Write build(@NonNull final Maybe<M> model) {
                return build(model, mEntries);
            }

            @NonNull
            private Builder<M> put(@NonNull final Function<Maybe<M>, Write> entry) {
                mEntries.add(entry);
                return this;
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
            private static <M, V> Function<Maybe<M>, Write> entry(@NonNull final Value.Write<V> value,
                                                                  @NonNull final Lens.Read<M, Maybe<V>> lens) {
                return new Function<Maybe<M>, Write>() {
                    @NonNull
                    @Override
                    public Write invoke(@NonNull final Maybe<M> model) {
                        return Plans.write(get(model, lens), value);
                    }
                };
            }

            @NonNull
            private static <M, V> Function<Maybe<M>, Write> entry(@NonNull final Mapper.Write<V> mapper,
                                                                  @NonNull final Lens.Read<M, Maybe<V>> lens) {
                return new Function<Maybe<M>, Write>() {
                    @NonNull
                    @Override
                    public Write invoke(@NonNull final Maybe<M> model) {
                        return mapper.prepareWrite(get(model, lens));
                    }
                };
            }

            @NonNull
            private static <M> Write build(@NonNull final Maybe<M> model,
                                           @NonNull final Collection<Function<Maybe<M>, Write>> entries) {
                final Collection<Write> plans = new ArrayList<>(entries.size());

                for (final Function<Maybe<M>, Write> entry : entries) {
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
