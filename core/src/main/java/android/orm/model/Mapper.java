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

import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.util.Converter;
import android.orm.util.Function;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import static android.orm.model.Mappers.combine;
import static android.orm.util.Converters.from;
import static android.orm.util.Converters.to;
import static android.orm.util.Maybes.something;

public final class Mapper {

    public interface Read<M> {

        @NonNls
        @NonNull
        String getName();

        @NonNull
        Select.Projection getProjection();

        @NonNull
        Reading.Item.Create<M> prepareRead();

        @NonNull
        Reading.Item<M> prepareRead(@NonNull final M m);

        @NonNull
        <V> Read<Pair<M, V>> and(@NonNull final Value.Read<V> other);

        @NonNull
        <N> Read<Pair<M, N>> and(@NonNull final Read<N> other);

        @NonNull
        <N> Read<N> map(@NonNull final Converter<Maybe<M>, Maybe<N>> converter);

        abstract class Base<M> implements Read<M> {

            @NonNull
            @Override
            public final <V> Read<Pair<M, V>> and(@NonNull final Value.Read<V> other) {
                return new Composition<>(this, other);
            }

            @NonNull
            @Override
            public final <N> Read<Pair<M, N>> and(@NonNull final Read<N> other) {
                return new Composition<>(this, other);
            }

            @NonNull
            @Override
            public final <N> Read<N> map(@NonNull final Converter<Maybe<M>, Maybe<N>> converter) {
                return new Converted<>(this, converter);
            }

            private static class Composition<M, N> extends Base<Pair<M, N>> {

                @NonNull
                private final Read<M> mFirst;
                @NonNull
                private final Read<N> mSecond;
                @NonNls
                @NonNull
                private final String mName;
                @NonNull
                private final Select.Projection mProjection;

                private Composition(@NonNull final Read<M> first, @NonNull final Value.Read<N> second) {
                    this(first, Mappers.read(second));
                }

                private Composition(@NonNull final Read<M> first, @NonNull final Read<N> second) {
                    super();

                    mFirst = first;
                    mSecond = second;
                    mName = '(' + mFirst.getName() + ", " + mSecond.getName() + ')';
                    mProjection = first.getProjection().and(second.getProjection());
                }

                @NonNls
                @NonNull
                @Override
                public final String getName() {
                    return mName;
                }

                @NonNull
                @Override
                public final Select.Projection getProjection() {
                    return mProjection;
                }

                @NonNull
                @Override
                public final Reading.Item.Create<Pair<M, N>> prepareRead() {
                    return mFirst.prepareRead().and(mSecond.prepareRead());
                }

                @NonNull
                @Override
                public final Reading.Item<Pair<M, N>> prepareRead(@NonNull final Pair<M, N> pair) {
                    final Reading.Item<M> first = (pair.first == null) ?
                            mFirst.prepareRead() :
                            mFirst.prepareRead(pair.first);
                    final Reading.Item<N> second = (pair.second == null) ?
                            mSecond.prepareRead() :
                            mSecond.prepareRead(pair.second);
                    return first.and(second);
                }
            }

            private static class Converted<M, N> extends Base<N> {

                @NonNull
                private final Read<M> mRead;
                @NonNull
                private final Converter<Maybe<M>, Maybe<N>> mConverter;
                @NonNull
                private final Function<Maybe<M>, Maybe<N>> mFrom;
                @NonNls
                @NonNull
                private final String mName;
                @NonNull
                private final Select.Projection mProjection;

                private Converted(@NonNull final Read<M> read,
                                  @NonNull final Converter<Maybe<M>, Maybe<N>> converter) {
                    super();

                    mRead = read;
                    mConverter = converter;
                    mFrom = from(mConverter);
                    mName = read.getName();
                    mProjection = mRead.getProjection();
                }

                @NonNls
                @NonNull
                @Override
                public final String getName() {
                    return mName;
                }

                @NonNull
                @Override
                public final Select.Projection getProjection() {
                    return mProjection;
                }

                @NonNull
                @Override
                public final Reading.Item.Create<N> prepareRead() {
                    return mRead.prepareRead().map(mFrom);
                }

                @NonNull
                @Override
                public final Reading.Item<N> prepareRead(@NonNull final N model) {
                    final M value = mConverter.to(something(model)).getOrElse(null);
                    return ((value == null) ? mRead.prepareRead() : mRead.prepareRead(value)).map(mFrom);
                }
            }
        }

        class Builder<M> {

            @NonNls
            @NonNull
            private final String mName;
            private final Reading.Item.Builder<M> mReading;

            public Builder(@NonNls @NonNull final String name,
                           @NonNull final Producer<M> producer) {
                this(value(name, producer));
            }

            public Builder(@NonNull final Value.Read<M> value) {
                super();

                mName = value.getName();
                mReading = Reading.Item.builder(value);
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Value.Read<V> value,
                                             @NonNull final Lens.Write<M, V> lens) {
                mReading.with(value, Maybes.lift(lens));
                return this;
            }

            @NonNull
            public final <N> Builder<M> with(@NonNull final Read<N> mapper,
                                             @NonNull final Lens.ReadWrite<M, N> lens) {
                mReading.with(mapper, Maybes.lift(lens));
                return this;
            }

            @NonNull
            public final Read<M> build() {
                return build(mName, new Reading.Item.Builder<>(mReading));
            }

            @NonNull
            private static <M> Read<M> build(@NonNls @NonNull final String name,
                                             @NonNull final Reading.Item.Builder<M> reading) {
                return new Base<M>() {

                    private final Reading.Item.Create<M> mCreate = reading.build();
                    private final Select.Projection mProjection = mCreate.getProjection();

                    @NonNls
                    @NonNull
                    @Override
                    public String getName() {
                        return name;
                    }

                    @NonNull
                    @Override
                    public Select.Projection getProjection() {
                        return mProjection;
                    }

                    @NonNull
                    @Override
                    public Reading.Item.Create<M> prepareRead() {
                        return mCreate;
                    }

                    @NonNull
                    @Override
                    public Reading.Item.Update<M> prepareRead(@NonNull final M model) {
                        return reading.build(model);
                    }
                };
            }

            @NonNull
            private static <M> Value.Read<M> value(@NonNls @NonNull final String name,
                                                   @NonNull final Producer<M> producer) {
                return new Value.Read.Base<M>() {

                    private final Producer<Maybe<M>> mProducer = Maybes.lift(producer);

                    @NonNls
                    @NonNull
                    @Override
                    public String getName() {
                        return name;
                    }

                    @NonNull
                    @Override
                    public Select.Projection getProjection() {
                        return Select.Projection.Nothing;
                    }

                    @NonNull
                    @Override
                    public Maybe<M> read(@NonNull final android.orm.sql.Readable input) {
                        return mProducer.produce();
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
        Plan.Write prepareWrite(@NonNull final Maybe<M> m);

        @NonNull
        <V> Write<Pair<M, V>> and(@NonNull final Value.Write<V> other);

        @NonNull
        <N> Write<Pair<M, N>> and(@NonNull final Write<N> other);

        @NonNull
        <N> Write<N> mapFrom(@NonNull final Function<Maybe<N>, Maybe<M>> converter);

        abstract class Base<M> implements Write<M> {

            @NonNull
            @Override
            public final <V> Write<Pair<M, V>> and(@NonNull final Value.Write<V> other) {
                return new Composition<>(this, other);
            }

            @NonNull
            @Override
            public final <N> Write<Pair<M, N>> and(@NonNull final Write<N> other) {
                return new Composition<>(this, other);
            }

            @NonNull
            @Override
            public final <N> Write<N> mapFrom(@NonNull final Function<Maybe<N>, Maybe<M>> converter) {
                return new Converted<>(this, converter);
            }

            private static class Composition<M, N> extends Base<Pair<M, N>> {

                @NonNull
                private final Write<M> mFirst;
                @NonNull
                private final Write<N> mSecond;
                @NonNls
                @NonNull
                private final String mName;

                private Composition(@NonNull final Write<M> first, @NonNull final Value.Write<N> second) {
                    this(first, Mappers.write(second));
                }

                private Composition(@NonNull final Write<M> first, @NonNull final Write<N> second) {
                    super();

                    mFirst = first;
                    mSecond = second;
                    mName = '(' + mFirst.getName() + ", " + mSecond.getName() + ')';
                }

                @NonNls
                @NonNull
                @Override
                public final String getName() {
                    return mName;
                }

                @NonNull
                @Override
                public final Plan.Write prepareWrite(@NonNull final Maybe<Pair<M, N>> value) {
                    final Plan.Write plan;

                    if (value.isSomething()) {
                        final Pair<M, N> pair = value.get();
                        plan = mFirst.prepareWrite(something((pair == null) ? null : pair.first))
                                .and(mSecond.prepareWrite(something((pair == null) ? null : pair.second)));
                    } else {
                        plan = mFirst.prepareWrite(Maybes.<M>nothing())
                                .and(mSecond.prepareWrite(Maybes.<N>nothing()));
                    }

                    return plan;
                }
            }

            private static class Converted<M, N> extends Base<N> {

                @NonNull
                private final Write<M> mWrite;
                @NonNull
                private final Function<Maybe<N>, Maybe<M>> mConverter;
                @NonNls
                @NonNull
                private final String mName;

                private Converted(@NonNull final Write<M> write,
                                  @NonNull final Function<Maybe<N>, Maybe<M>> converter) {
                    super();

                    mWrite = write;
                    mConverter = converter;
                    mName = write.getName();
                }

                @NonNls
                @NonNull
                @Override
                public final String getName() {
                    return mName;
                }

                @NonNull
                @Override
                public final Plan.Write prepareWrite(@NonNull final Maybe<N> value) {
                    return mWrite.prepareWrite(mConverter.invoke(value));
                }
            }
        }

        class Builder<M> {

            @NonNls
            @NonNull
            private final String mName;
            private final Plan.Write.Builder<M> mWrite = Plan.Write.builder();

            public Builder(@NonNls @NonNull final String name) {
                super();

                mName = name;
            }

            @NonNull
            public final Builder<M> with(@NonNull final Writer writer) {
                mWrite.put(writer);
                return this;
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Value.Write<V> value,
                                             @NonNull final Lens.Read<M, V> lens) {
                return with(Mappers.write(value), lens);
            }

            @NonNull
            public final <V> Builder<M> with(@NonNull final Write<V> mapper,
                                             @NonNull final Lens.Read<M, V> lens) {
                mWrite.put(mapper, Maybes.lift(lens));
                return this;
            }

            @NonNull
            public final Write<M> build() {
                return build(mName, new Plan.Write.Builder<>(mWrite));
            }

            @NonNull
            private static <M> Write<M> build(@NonNls @NonNull final String name,
                                              @NonNull final Plan.Write.Builder<M> write) {
                return new Base<M>() {

                    @NonNls
                    @NonNull
                    @Override
                    public String getName() {
                        return name;
                    }

                    @NonNull
                    @Override
                    public Plan.Write prepareWrite(@NonNull final Maybe<M> result) {
                        return write.build(result);
                    }
                };
            }
        }
    }

    public interface ReadWrite<M> extends Read<M>, Write<M> {

        @NonNull
        <V> ReadWrite<Pair<M, V>> and(@NonNull final Value.ReadWrite<V> other);

        @NonNull
        <N> ReadWrite<Pair<M, N>> and(@NonNull final ReadWrite<N> other);

        @NonNull
        @Override
        <N> ReadWrite<N> map(@NonNull final Converter<Maybe<M>, Maybe<N>> converter);

        abstract class Base<M> implements ReadWrite<M> {

            protected Base() {
                super();
            }

            @NonNull
            @Override
            public final <V> Read<Pair<M, V>> and(@NonNull final Value.Read<V> other) {
                return new Read.Base.Composition<>(this, other);
            }

            @NonNull
            @Override
            public final <V> Write<Pair<M, V>> and(@NonNull final Value.Write<V> other) {
                return new Write.Base.Composition<>(this, other);
            }

            @NonNull
            @Override
            public final <V> ReadWrite<Pair<M, V>> and(@NonNull final Value.ReadWrite<V> other) {
                return combine(
                        new Read.Base.Composition<>(this, other),
                        new Write.Base.Composition<>(this, other)
                );
            }

            @NonNull
            @Override
            public final <N> Read<Pair<M, N>> and(@NonNull final Read<N> other) {
                return new Read.Base.Composition<>(this, other);
            }

            @NonNull
            @Override
            public final <N> Write<Pair<M, N>> and(@NonNull final Write<N> other) {
                return new Write.Base.Composition<>(this, other);
            }

            @NonNull
            @Override
            public final <N> ReadWrite<Pair<M, N>> and(@NonNull final ReadWrite<N> other) {
                return combine(
                        new Read.Base.Composition<>(this, other),
                        new Write.Base.Composition<>(this, other)
                );
            }

            @NonNull
            @Override
            public final <N> Write<N> mapFrom(@NonNull final Function<Maybe<N>, Maybe<M>> converter) {
                return new Write.Base.Converted<>(this, converter);
            }

            @NonNull
            @Override
            public final <N> ReadWrite<N> map(@NonNull final Converter<Maybe<M>, Maybe<N>> converter) {
                return combine(
                        new Read.Base.Converted<>(this, converter),
                        new Write.Base.Converted<>(this, to(converter))
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

                mRead = read(name, producer);
                mWrite = write(name);
            }

            public Builder(@NonNull final Value.Read<M> value) {
                super();

                mRead = read(value);
                mWrite = write(value.getName());
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
                return combine(mRead.build(), mWrite.build());
            }
        }
    }

    @NonNull
    public static <M> Read.Builder<M> read(@NonNls @NonNull final String name,
                                           @NonNull final Producer<M> producer) {
        return new Read.Builder<>(name, producer);
    }

    @NonNull
    public static <M> Read.Builder<M> read(@NonNull final Value.Read<M> value) {
        return new Read.Builder<>(value);
    }

    @NonNull
    public static <M> Write.Builder<M> write(@NonNls @NonNull final String name) {
        return new Write.Builder<>(name);
    }

    private Mapper() {
        super();
    }
}
