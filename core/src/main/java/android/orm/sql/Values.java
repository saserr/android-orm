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

import android.orm.sql.fragment.Condition;
import android.orm.util.Converter;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Types.Integer;
import static android.orm.util.Converters.from;
import static android.orm.util.Converters.to;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public final class Values {

    public static final Value.ReadWrite<Long> RowId = value("_ROWID_", Integer);

    @NonNull
    public static <V> Value.ReadWrite<V> value(@NonNls @NonNull final String name,
                                               @NonNull final Type<V> type) {
        return new NamedType<>(name, type);
    }

    @NonNull
    public static <V> Value.Constant constant(@NonNull final Value.Write<V> value,
                                              @NonNull final Producer<Maybe<V>> producer) {
        return new WriteConstant<>(value, producer);
    }

    @NonNull
    public static <V, T> Value.Read<Pair<V, T>> compose(@NonNull final Value.Read<V> first,
                                                        @NonNull final Value.Read<T> second) {
        return new ReadComposition<>(first, second);
    }

    @NonNull
    public static Value.Constant compose(@NonNull final Value.Constant first,
                                         @NonNull final Value.Constant second) {
        return new ConstantComposition(first, second);
    }

    @NonNull
    public static <V> Value.Write<V> compose(@NonNull final Value.Constant first,
                                             @NonNull final Value.Write<V> second) {
        return new ConstantWriteComposition<>(first, second);
    }

    @NonNull
    public static <V, T> Value.Write<Pair<V, T>> compose(@NonNull final Value.Write<V> first,
                                                         @NonNull final Value.Write<T> second) {
        return new WriteComposition<>(first, second);
    }

    @NonNull
    public static <V, T> Value.ReadWrite<Pair<V, T>> compose(@NonNull final Value.ReadWrite<V> first,
                                                             @NonNull final Value.ReadWrite<T> second) {
        return combine(new ReadComposition<>(first, second), new WriteComposition<>(first, second));
    }

    @NonNull
    public static <V, T> Value.Read<T> convert(@NonNull final Value.Read<V> value,
                                               @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
        return new ReadConversion<>(value, converter);
    }

    @NonNull
    public static <V, T> Value.Write<T> convert(@NonNull final Value.Write<V> value,
                                                @NonNull final Function<Maybe<T>, Maybe<V>> converter) {
        return new WriteConversion<>(value, converter);
    }

    @NonNull
    public static <V, T> Value.ReadWrite<T> convert(@NonNull final Value.ReadWrite<V> value,
                                                    @NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
        return combine(
                new ReadConversion<>(value, from(converter)),
                new WriteConversion<>(value, to(converter))
        );
    }

    @NonNull
    public static <V> Value.ReadWrite<V> combine(@NonNull final Value.Read<V> read,
                                                 @NonNull final Value.Write<V> write) {
        return new Combine<>(read, write);
    }

    private static class NamedType<V> extends Value.ReadWrite.Base<V> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Type<V> mType;
        @NonNull
        private final Select.Projection mProjection;

        private NamedType(@NonNls @NonNull final String name, @NonNull final Type<V> type) {
            super();

            mName = name;
            mType = type;
            mProjection = Select.projection(mName, null);
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
        public final Maybe<V> read(@NonNull final Readable input) {
            return mType.read(input, mName);
        }

        @Override
        public final void write(@NonNull final Operation operation,
                                @NonNull final Maybe<V> value,
                                @NonNull final Writable output) {
            if (value.isSomething()) {
                final V v = value.get();
                if (v == null) {
                    output.putNull(mName);
                } else {
                    mType.write(output, mName, v);
                }
            }
        }
    }

    private static class WriteConstant<V> extends Value.Constant.Base {

        @NonNull
        private final Value.Write<V> mValue;
        @NonNull
        private final Producer<Maybe<V>> mProducer;

        private WriteConstant(@NonNull final Value.Write<V> value,
                              @NonNull final Producer<Maybe<V>> producer) {
            super();

            mValue = value;
            mProducer = producer;
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mValue.getName();
        }

        @NonNull
        @Override
        public final Condition onUpdate() {
            return Condition.None;
        }

        @Override
        public final void write(@NonNull final Value.Write.Operation operation,
                                @NonNull final Writable output) {
            mValue.write(operation, mProducer.produce(), output);
        }
    }

    private static class ReadComposition<V, T> extends Value.Read.Base<Pair<V, T>> {

        @NonNull
        private final Value.Read<V> mFirst;
        @NonNull
        private final Value.Read<T> mSecond;
        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Select.Projection mProjection;

        private ReadComposition(@NonNull final Value.Read<V> first,
                                @NonNull final Value.Read<T> second) {
            super();

            mFirst = first;
            mSecond = second;
            mName = '(' + mFirst.getName() + ", " + mSecond.getName() + ')';
            mProjection = mFirst.getProjection().and(mSecond.getProjection());
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
        public final Maybe<Pair<V, T>> read(@NonNull final Readable input) {
            return mFirst.read(input).and(mSecond.read(input));
        }
    }

    private static class ConstantComposition extends Value.Constant.Base {

        @NonNull
        private final Value.Constant mFirst;
        @NonNull
        private final Value.Constant mSecond;
        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Condition mOnUpdate;

        private ConstantComposition(@NonNull final Value.Constant first,
                                    @NonNull final Value.Constant second) {
            super();

            mFirst = first;
            mSecond = second;
            mName = '(' + first.getName() + ", " + second.getName() + ')';
            mOnUpdate = first.onUpdate().and(second.onUpdate());
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @NonNull
        @Override
        public final Condition onUpdate() {
            return mOnUpdate;
        }

        @Override
        public final void write(@NonNull final Value.Write.Operation operation,
                                @NonNull final Writable output) {
            mFirst.write(operation, output);
            mSecond.write(operation, output);
        }
    }

    private static class ConstantWriteComposition<V> extends Value.Write.Base<V> {

        @NonNull
        private final Value.Constant mFirst;
        @NonNull
        private final Value.Write<V> mSecond;
        @NonNls
        @NonNull
        private final String mName;

        private ConstantWriteComposition(@NonNull final Value.Constant first,
                                         @NonNull final Value.Write<V> second) {
            super();

            mFirst = first;
            mSecond = second;
            mName = '(' + first.getName() + ", " + second.getName() + ')';
        }

        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @Override
        public final void write(@NonNull final Operation operation,
                                @NonNull final Maybe<V> value,
                                @NonNull final Writable output) {
            mFirst.write(operation, output);
            mSecond.write(operation, value, output);
        }
    }

    private static class WriteComposition<V, T> extends Value.Write.Base<Pair<V, T>> {

        @NonNull
        private final Value.Write<V> mFirst;
        @NonNull
        private final Value.Write<T> mSecond;
        @NonNls
        @NonNull
        private final String mName;

        private WriteComposition(@NonNull final Value.Write<V> first,
                                 @NonNull final Value.Write<T> second) {
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

        @Override
        public final void write(@NonNull final Operation operation,
                                @NonNull final Maybe<Pair<V, T>> value,
                                @NonNull final Writable output) {
            final Maybe<V> value1;
            final Maybe<T> value2;

            if (value.isSomething()) {
                final Pair<V, T> pair = value.get();
                value1 = something((pair == null) ? null : pair.first);
                value2 = something((pair == null) ? null : pair.second);
            } else {
                value1 = nothing();
                value2 = nothing();
            }

            mFirst.write(operation, value1, output);
            mSecond.write(operation, value2, output);
        }
    }

    private static class ReadConversion<V, T> extends Value.Read.Base<T> {

        @NonNull
        private final Value.Read<V> mValue;
        @NonNull
        private final Function<Maybe<V>, Maybe<T>> mConverter;
        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Select.Projection mProjection;

        private ReadConversion(@NonNull final Value.Read<V> value,
                               @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
            super();

            mValue = value;
            mConverter = converter;
            mName = value.getName();
            mProjection = mValue.getProjection();
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
        public final Maybe<T> read(@NonNull final Readable input) {
            return mConverter.invoke(mValue.read(input));
        }
    }

    private static class WriteConversion<V, T> extends Value.Write.Base<T> {

        @NonNull
        private final Value.Write<V> mWrite;
        @NonNull
        private final Function<Maybe<T>, Maybe<V>> mConverter;
        @NonNls
        @NonNull
        private final String mName;

        private WriteConversion(@NonNull final Value.Write<V> value,
                                @NonNull final Function<Maybe<T>, Maybe<V>> converter) {
            super();

            mWrite = value;
            mConverter = converter;
            mName = value.getName();
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @Override
        public final void write(@NonNull final Operation operation,
                                @NonNull final Maybe<T> value,
                                @NonNull final Writable output) {
            mWrite.write(operation, mConverter.invoke(value), output);
        }
    }

    private static class Combine<V> extends Value.ReadWrite.Base<V> {

        @NonNull
        private final Value.Read<V> mRead;
        @NonNull
        private final Value.Write<V> mWrite;

        private Combine(@NonNull final Value.Read<V> read, @NonNull final Value.Write<V> write) {
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
        public final Select.Projection getProjection() {
            return mRead.getProjection();
        }

        @NonNull
        @Override
        public final Maybe<V> read(@NonNull final Readable input) {
            return mRead.read(input);
        }

        @Override
        public final void write(@NonNull final Operation operation,
                                @NonNull final Maybe<V> value,
                                @NonNull final Writable output) {
            mWrite.write(operation, value, output);
        }
    }

    private Values() {
        super();
    }
}
