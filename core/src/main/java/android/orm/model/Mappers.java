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

import android.orm.sql.Reader;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Values;
import android.orm.sql.Writer;
import android.orm.sql.Writers;
import android.orm.util.Converter;
import android.orm.util.Converters;
import android.orm.util.Function;
import android.orm.util.Lazy;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.util.Arrays;

import static android.orm.util.Maybes.something;

public final class Mappers {

    @NonNull
    public static <M extends Instance.Readable> Mapper.Read<M> read(@NonNull final Producer<M> producer) {
        return new InstanceRead<>(producer);
    }

    @NonNull
    public static <V> Mapper.Read<V> read(@NonNull final Value.Read<V> value) {
        return new ValueRead<>(value);
    }

    @NonNull
    public static <M extends Instance.Writable> Mapper.Write<M> write(@NonNls @NonNull final String name) {
        return new InstanceWrite<>(name);
    }

    @NonNull
    public static <V> Mapper.Write<V> write(@NonNull final Value.Write<V> value) {
        return new ValueWrite<>(value);
    }

    @NonNull
    public static <M extends Instance.ReadWrite> Mapper.ReadWrite<M> mapper(@NonNull final Producer<M> producer) {
        final Mapper.Read<M> read = read(producer);
        return combine(read, Mappers.<M>write(read.getName()));
    }

    @NonNull
    public static <V> Mapper.ReadWrite<V> mapper(@NonNull final Value.ReadWrite<V> value) {
        return combine(read(value), write(value));
    }

    @NonNull
    public static <M, N> Mapper.Read<Pair<M, N>> compose(@NonNull final Mapper.Read<M> first,
                                                         @NonNull final Mapper.Read<N> second) {
        return new ReadComposition<>(first, second);
    }

    @NonNull
    public static <M> Mapper.Write<M> compose(@NonNull final Mapper.Write<M> mapper,
                                              @NonNull final Value constant) {
        return new ConstantComposition<>(mapper, constant);
    }

    @NonNull
    public static <M, N> Mapper.Write<Pair<M, N>> compose(@NonNull final Mapper.Write<M> first,
                                                          @NonNull final Mapper.Write<N> second) {
        return new WriteComposition<>(first, second);
    }

    @NonNull
    public static <M, N> Mapper.Read<N> convert(@NonNull final Mapper.Read<M> mapper,
                                                @NonNull final Converter<M, N> converter) {
        return new ReadConversion<>(mapper, converter);
    }

    @NonNull
    public static <M, N> Mapper.Write<N> convert(@NonNull final Mapper.Write<M> mapper,
                                                 @NonNull final Function<? super N, ? extends M> converter) {
        return new WriteConversion<>(mapper, converter);
    }

    @NonNull
    public static <M> Mapper.ReadWrite<M> combine(@NonNull final Mapper.Read<M> read,
                                                  @NonNull final Mapper.Write<M> write) {
        return new Combination<>(read, write);
    }

    private static class InstanceRead<M extends Instance.Readable> extends Mapper.Read.Base<M> {

        @NonNull
        private final Producer<M> mProducer;

        private final Lazy<M> mCreateBlueprint = new Lazy.Volatile<M>() {
            @NonNull
            @Override
            protected M produce() {
                return mProducer.produce();
            }
        };

        private final Lazy<String> mName = new Lazy.Volatile<String>() {
            @NonNull
            @Override
            protected String produce() {
                return mCreateBlueprint.get().getName();
            }
        };

        @NonNull
        private final Lazy<Reader.Element.Create<M>> mCreateReader = new Lazy.Volatile<Reader.Element.Create<M>>() {
            @NonNull
            @Override
            protected Reader.Element.Create<M> produce() {
                final Select.Projection projection = mCreateBlueprint.get().prepareRead().getProjection();
                return Plan.Read.from(projection, mProducer);
            }
        };

        private InstanceRead(@NonNull final Producer<M> producer) {
            super();

            mProducer = producer;
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName.get();
        }

        @NonNull
        @Override
        public final Reader.Element.Create<M> prepareReader() {
            return mCreateReader.get();
        }

        @NonNull
        @Override
        public final Reader.Element.Update<M> prepareReader(@NonNull final M model) {
            return Plan.Read.from(model);
        }
    }

    private static class ValueRead<V> extends Mapper.Read.Base<V> {

        @NonNull
        private final Value.Read<V> mValue;
        @NonNull
        private final Reader.Element.Create<V> mReader;

        private ValueRead(@NonNull final Value.Read<V> value) {
            super();

            mValue = value;
            mReader = Plan.Read.from(value);
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mValue.getName();
        }

        @NonNull
        @Override
        public final Reader.Element.Create<V> prepareReader() {
            return mReader;
        }

        @NonNull
        @Override
        public final Reader.Element.Create<V> prepareReader(@NonNull final V v) {
            return mReader;
        }
    }

    private static class InstanceWrite<M extends Instance.Writable> extends Mapper.Write.Base<M> {

        @NonNls
        @NonNull
        private final String mName;

        private InstanceWrite(@NonNls @NonNull final String name) {
            super();

            mName = name;
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @NonNull
        @Override
        public final Writer prepareWriter(@NonNull final Maybe<M> value) {
            final M model = value.getOrElse(null);
            return (model == null) ? Writer.Empty : model.prepareWriter();
        }
    }

    private static class ValueWrite<V> extends Mapper.Write.Base<V> {

        @NonNull
        private final Value.Write<V> mValue;

        private ValueWrite(@NonNull final Value.Write<V> value) {
            super();

            mValue = value;
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mValue.getName();
        }

        @NonNull
        @Override
        public final Writer prepareWriter(@NonNull final Maybe<V> value) {
            return Values.value(mValue, value);
        }
    }

    private static class ReadComposition<M, N> extends Mapper.Read.Base<Pair<M, N>> {

        @NonNull
        private final Mapper.Read<M> mFirst;
        @NonNull
        private final Mapper.Read<N> mSecond;
        @NonNls
        @NonNull
        private final String mName;

        private ReadComposition(@NonNull final Mapper.Read<M> first,
                                @NonNull final Mapper.Read<N> second) {
            super();

            mFirst = first;
            mSecond = second;
            mName = '(' + first.getName() + ", " + second.getName() + ')';
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @NonNull
        @Override
        public final Reader.Element.Create<Pair<M, N>> prepareReader() {
            return mFirst.prepareReader().and(mSecond.prepareReader());
        }

        @NonNull
        @Override
        public final Reader.Element<Pair<M, N>> prepareReader(@NonNull final Pair<M, N> pair) {
            final Reader.Element<M> first = (pair.first == null) ?
                    mFirst.prepareReader() :
                    mFirst.prepareReader(pair.first);
            final Reader.Element<N> second = (pair.second == null) ?
                    mSecond.prepareReader() :
                    mSecond.prepareReader(pair.second);
            return first.and(second);
        }
    }

    private static class ConstantComposition<M> extends Mapper.Write.Base<M> {

        @NonNull
        private final Mapper.Write<M> mFirst;
        @NonNull
        private final Value mSecond;
        @NonNls
        @NonNull
        private final String mName;

        private ConstantComposition(@NonNull final Mapper.Write<M> first,
                                    @NonNull final Value second) {
            super();

            mFirst = first;
            mSecond = second;
            mName = '(' + first.getName() + ", " + second.getName() + ')';
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @NonNull
        @Override
        public final Writer prepareWriter(@NonNull final Maybe<M> value) {
            return Writers.compose(Arrays.asList(mFirst.prepareWriter(value), mSecond));
        }
    }

    private static class WriteComposition<M, N> extends Mapper.Write.Base<Pair<M, N>> {

        @NonNull
        private final Mapper.Write<M> mFirst;
        @NonNull
        private final Mapper.Write<N> mSecond;
        @NonNls
        @NonNull
        private final String mName;

        private WriteComposition(@NonNull final Mapper.Write<M> first,
                                 @NonNull final Mapper.Write<N> second) {
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
        public final Writer prepareWriter(@NonNull final Maybe<Pair<M, N>> value) {
            return Writers.compose(Arrays.asList(
                    mFirst.prepareWriter(first(value)),
                    mSecond.prepareWriter(second(value))
            ));
        }

        @NonNull
        private static <M, N> Maybe<M> first(@NonNull final Maybe<Pair<M, N>> value) {
            final Pair<M, N> pair = value.getOrElse(null);
            return something((pair == null) ? null : pair.first);
        }

        @NonNull
        private static <M, N> Maybe<N> second(@NonNull final Maybe<Pair<M, N>> value) {
            final Pair<M, N> pair = value.getOrElse(null);
            return something((pair == null) ? null : pair.second);
        }
    }

    private static class ReadConversion<M, N> extends Mapper.Read.Base<N> {

        @NonNull
        private final Mapper.Read<M> mRead;
        @NonNull
        private final Converter<M, N> mConverter;
        @NonNull
        private final Function<Maybe<M>, Maybe<N>> mFrom;
        @NonNls
        @NonNull
        private final String mName;

        private ReadConversion(@NonNull final Mapper.Read<M> read,
                               @NonNull final Converter<M, N> converter) {
            super();

            mRead = read;
            mConverter = converter;
            mFrom = Maybes.map(Converters.from(converter));
            mName = read.getName();
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @NonNull
        @Override
        public final Reader.Element.Create<N> prepareReader() {
            return mRead.prepareReader().convert(mFrom);
        }

        @NonNull
        @Override
        public final Reader.Element<N> prepareReader(@NonNull final N model) {
            return mRead.prepareReader(mConverter.to(model)).convert(mFrom);
        }
    }

    private static class WriteConversion<M, N> extends Mapper.Write.Base<N> {

        @NonNull
        private final Mapper.Write<M> mWrite;
        @NonNull
        private final Function<Maybe<N>, Maybe<M>> mConverter;
        @NonNls
        @NonNull
        private final String mName;

        private WriteConversion(@NonNull final Mapper.Write<M> write,
                                @NonNull final Function<? super N, ? extends M> converter) {
            super();

            mWrite = write;
            mConverter = Maybes.map(converter);
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
        public final Writer prepareWriter(@NonNull final Maybe<N> value) {
            return mWrite.prepareWriter(mConverter.invoke(value));
        }
    }

    private static class Combination<M> extends Mapper.ReadWrite.Base<M> {

        @NonNull
        private final Mapper.Read<M> mRead;
        @NonNull
        private final Mapper.Write<M> mWrite;

        private Combination(@NonNull final Mapper.Read<M> read,
                            @NonNull final Mapper.Write<M> write) {
            super();

            mRead = read;
            mWrite = write;
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mRead.getName();
        }

        @NonNull
        @Override
        public final Reader.Element.Create<M> prepareReader() {
            return mRead.prepareReader();
        }

        @NonNull
        @Override
        public final Reader.Element<M> prepareReader(@NonNull final M model) {
            return mRead.prepareReader(model);
        }

        @NonNull
        @Override
        public final Writer prepareWriter(@NonNull final Maybe<M> value) {
            return mWrite.prepareWriter(value);
        }
    }

    private Mappers() {
        super();
    }
}
