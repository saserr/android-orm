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
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.util.Converter;
import android.orm.util.Converters;
import android.orm.util.Function;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

public final class Mapper {

    public interface Read<M> {

        @NonNls
        @NonNull
        String getName();

        @NonNull
        Reader.Element.Create<M> prepareReader();

        @NonNull
        Reader.Element<M> prepareReader(@NonNull final M m);

        @NonNull
        <V> Read<Pair<M, V>> and(@NonNull final Value.Read<V> other);

        @NonNull
        <N> Read<Pair<M, N>> and(@NonNull final Read<N> other);

        @NonNull
        <N> Read<N> map(@NonNull final Converter<M, N> converter);

        abstract class Base<M> implements Read<M> {

            @NonNull
            @Override
            public final <V> Read<Pair<M, V>> and(@NonNull final Value.Read<V> other) {
                return and(Mappers.read(other));
            }

            @NonNull
            @Override
            public final <N> Read<Pair<M, N>> and(@NonNull final Read<N> other) {
                return Mappers.compose(this, other);
            }

            @NonNull
            @Override
            public final <N> Read<N> map(@NonNull final Converter<M, N> converter) {
                return Mappers.convert(this, converter);
            }
        }

        class Builder<M> {

            @NonNls
            @NonNull
            private final String mName;
            private final Plan.Read.Builder<M> mReader;

            public Builder(@NonNls @NonNull final String name,
                           @NonNull final Producer<M> producer) {
                super();

                mName = name;
                mReader = Plan.Read.builder(name, producer);
            }

            public Builder(@NonNull final Value.Read<M> value) {
                super();

                mName = value.getName();
                mReader = new Plan.Read.Builder<>(value);
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Value.Read<V> value,
                                             @NonNull final Lens.Write<M, V> lens) {
                mReader.with(value, Maybes.lift(lens));
                return this;
            }

            @NonNull
            public final <N> Builder<M> with(@NonNull final Read<N> mapper,
                                             @NonNull final Lens.ReadWrite<M, N> lens) {
                mReader.with(mapper, Maybes.lift(lens));
                return this;
            }

            @NonNull
            public final Read<M> build() {
                return build(mName, new Plan.Read.Builder<>(mReader));
            }

            @NonNull
            private static <M> Read<M> build(@NonNls @NonNull final String name,
                                             @NonNull final Plan.Read.Builder<M> reader) {
                return new Base<M>() {

                    private final Reader.Element.Create<M> mCreate = reader.build();

                    @NonNls
                    @NonNull
                    @Override
                    public String getName() {
                        return name;
                    }

                    @NonNull
                    @Override
                    public Reader.Element.Create<M> prepareReader() {
                        return mCreate;
                    }

                    @NonNull
                    @Override
                    public Reader.Element.Update<M> prepareReader(@NonNull final M model) {
                        return reader.build(model);
                    }
                };
            }
        }
    }

    public interface Write<M> {

        @NonNls
        @NonNull
        String getName();

        @NonNull
        Writer prepareWriter(@NonNull final Maybe<M> value);

        @NonNull
        Write<M> and(@NonNull final Value other);

        @NonNull
        <V> Write<Pair<M, V>> and(@NonNull final Value.Write<V> other);

        @NonNull
        <N> Write<Pair<M, N>> and(@NonNull final Write<N> other);

        @NonNull
        <N> Write<N> mapFrom(@NonNull final Function<? super N, ? extends M> converter);

        abstract class Base<M> implements Write<M> {

            @NonNull
            @Override
            public final Write<M> and(@NonNull final Value other) {
                return Mappers.compose(this, other);
            }

            @NonNull
            @Override
            public final <V> Write<Pair<M, V>> and(@NonNull final Value.Write<V> other) {
                return and(Mappers.write(other));
            }

            @NonNull
            @Override
            public final <N> Write<Pair<M, N>> and(@NonNull final Write<N> other) {
                return Mappers.compose(this, other);
            }

            @NonNull
            @Override
            public final <N> Write<N> mapFrom(@NonNull final Function<? super N, ? extends M> converter) {
                return Mappers.convert(this, converter);
            }
        }

        class Builder<M> {

            @NonNls
            @NonNull
            private final String mName;
            private final Plan.Write.Builder<M> mWriter = new Plan.Write.Builder<>();

            public Builder(@NonNls @NonNull final String name) {
                super();

                mName = name;
            }

            @NonNull
            public final Builder<M> with(@NonNull final Writer writer) {
                mWriter.with(writer);
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Value.Write<V> value,
                                             @NonNull final Lens.Read<M, V> lens) {
                mWriter.with(value, Maybes.lift(lens));
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Write<V> mapper,
                                             @NonNull final Lens.Read<M, V> lens) {
                mWriter.with(mapper, Maybes.lift(lens));
                return this;
            }

            @NonNull
            public final Write<M> build() {
                return build(mName, new Plan.Write.Builder<>(mWriter));
            }

            @NonNull
            private static <M> Write<M> build(@NonNls @NonNull final String name,
                                              @NonNull final Plan.Write.Builder<M> writer) {
                return new Base<M>() {

                    @NonNls
                    @NonNull
                    @Override
                    public String getName() {
                        return name;
                    }

                    @NonNull
                    @Override
                    public Writer prepareWriter(@NonNull final Maybe<M> value) {
                        return writer.build(value);
                    }
                };
            }
        }
    }

    public interface ReadWrite<M> extends Read<M>, Write<M> {

        @NonNull
        @Override
        ReadWrite<M> and(@NonNull final Value other);

        @NonNull
        <V> ReadWrite<Pair<M, V>> and(@NonNull final Value.ReadWrite<V> other);

        @NonNull
        <N> ReadWrite<Pair<M, N>> and(@NonNull final ReadWrite<N> other);

        @NonNull
        @Override
        <N> ReadWrite<N> map(@NonNull final Converter<M, N> converter);

        abstract class Base<M> implements ReadWrite<M> {

            protected Base() {
                super();
            }

            @NonNull
            @Override
            public final <V> Read<Pair<M, V>> and(@NonNull final Value.Read<V> other) {
                return and(Mappers.read(other));
            }

            @NonNull
            @Override
            public final ReadWrite<M> and(@NonNull final Value other) {
                return Mappers.combine(
                        this,
                        Mappers.compose(this, other)
                );
            }

            @NonNull
            @Override
            public final <V> Write<Pair<M, V>> and(@NonNull final Value.Write<V> other) {
                return and(Mappers.write(other));
            }

            @NonNull
            @Override
            public final <V> ReadWrite<Pair<M, V>> and(@NonNull final Value.ReadWrite<V> other) {
                return and(Mappers.mapper(other));
            }

            @NonNull
            @Override
            public final <N> Read<Pair<M, N>> and(@NonNull final Read<N> other) {
                return Mappers.compose(this, other);
            }

            @NonNull
            @Override
            public final <N> Write<Pair<M, N>> and(@NonNull final Write<N> other) {
                return Mappers.compose(this, other);
            }

            @NonNull
            @Override
            public final <N> ReadWrite<Pair<M, N>> and(@NonNull final ReadWrite<N> other) {
                return Mappers.combine(
                        Mappers.compose((Read<M>) this, other),
                        Mappers.compose((Write<M>) this, other)
                );
            }

            @NonNull
            @Override
            public final <N> Write<N> mapFrom(@NonNull final Function<? super N, ? extends M> converter) {
                return Mappers.convert(this, converter);
            }

            @NonNull
            @Override
            public final <N> ReadWrite<N> map(@NonNull final Converter<M, N> converter) {
                return Mappers.combine(
                        Mappers.convert(this, converter),
                        Mappers.convert(this, Converters.to(converter))
                );
            }
        }

        class Builder<M> {

            @NonNull
            private final Read.Builder<M> mRead;
            @NonNull
            private final Write.Builder<M> mWrite;

            public Builder(@NonNls @NonNull final String name,
                           @NonNull final Producer<M> producer) {
                super();

                mRead = new Read.Builder<>(name, producer);
                mWrite = new Write.Builder<>(name);
            }

            public Builder(@NonNull final Value.Read<M> value) {
                super();

                mRead = new Read.Builder<>(value);
                mWrite = new Write.Builder<>(value.getName());
            }

            @NonNull
            public final Builder<M> with(@NonNull final Writer writer) {
                mWrite.with(writer);
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Value.Read<V> value,
                                             @NonNull final Lens.Write<M, V> lens) {
                mRead.with(value, lens);
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Value.Write<V> value,
                                             @NonNull final Lens.Read<M, V> lens) {
                mWrite.with(value, lens);
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Value.ReadWrite<V> value,
                                             @NonNull final Lens.ReadWrite<M, V> lens) {
                mRead.with(value, lens);
                mWrite.with(value, lens);
                return this;
            }

            @NonNull
            public final <N> Builder<M> with(@NonNull final Read<N> mapper,
                                             @NonNull final Lens.ReadWrite<M, N> lens) {
                mRead.with(mapper, lens);
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Write<V> mapper,
                                             @NonNull final Lens.Read<M, V> lens) {
                mWrite.with(mapper, lens);
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final ReadWrite<V> mapper,
                                             @NonNull final Lens.ReadWrite<M, V> lens) {
                mRead.with(mapper, lens);
                mWrite.with(mapper, lens);
                return this;
            }

            @NonNull
            public final ReadWrite<M> build() {
                return Mappers.combine(mRead.build(), mWrite.build());
            }
        }
    }

    private Mapper() {
        super();
    }
}
