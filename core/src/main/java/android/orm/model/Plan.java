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
import android.orm.sql.Values;
import android.orm.sql.Writer;
import android.orm.util.Function;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.util.Pair;

import static android.orm.util.Lenses.get;

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
