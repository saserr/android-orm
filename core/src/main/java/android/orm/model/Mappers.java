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

import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import static android.orm.model.Plans.EmptyWrite;

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
        return combine(value, value);
    }

    @NonNull
    public static <M> Mapper.ReadWrite<M> combine(@NonNull final Value.Read<M> read,
                                                  @NonNull final Value.Write<M> write) {
        return combine(read(read), write(write));
    }

    @NonNull
    public static <M> Mapper.ReadWrite<M> combine(@NonNull final Value.Read<M> read,
                                                  @NonNull final Mapper.Write<M> write) {
        return combine(read(read), write);
    }

    @NonNull
    public static <M> Mapper.ReadWrite<M> combine(@NonNull final Mapper.Read<M> read,
                                                  @NonNull final Value.Write<M> write) {
        return combine(read, write(write));
    }

    @NonNull
    public static <M> Mapper.ReadWrite<M> combine(@NonNull final Mapper.Read<M> read,
                                                  @NonNull final Mapper.Write<M> write) {
        return new Composition<>(read, write);
    }

    private static class InstanceRead<M extends Instance.Readable> extends Mapper.Read.Base<M> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Select.Projection mProjection;
        @NonNull
        private final Reading.Item.Create<M> mCreate;

        private InstanceRead(@NonNull final Producer<M> producer) {
            super();

            final M model = producer.produce();
            mName = model.name();
            mProjection = model.projection();
            mCreate = Reading.Item.Create.from(mProjection, producer);
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
        public final Reading.Item.Create<M> prepareRead() {
            return mCreate;
        }

        @NonNull
        @Override
        public final Reading.Item.Update<M> prepareRead(@NonNull final M model) {
            return Reading.Item.Update.from(model);
        }
    }

    private static class ValueRead<V> extends Mapper.Read.Base<V> {

        @NonNull
        private final Value.Read<V> mValue;
        @NonNull
        private final Reading.Item.Create<V> mReading;

        private ValueRead(@NonNull final Value.Read<V> value) {
            super();

            mValue = value;
            mReading = Reading.Item.Create.from(value);
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mValue.getName();
        }

        @NonNull
        @Override
        public final Select.Projection getProjection() {
            return mValue.getProjection();
        }

        @NonNull
        @Override
        public final Reading.Item.Create<V> prepareRead() {
            return mReading;
        }

        @NonNull
        @Override
        public final Reading.Item.Create<V> prepareRead(@NonNull final V v) {
            return mReading;
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
        public final Plan.Write prepareWrite(@NonNull final Maybe<M> value) {
            final M model = value.getOrElse(null);
            return (model == null) ? EmptyWrite : model.prepareWrite();
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
        public final Plan.Write prepareWrite(@NonNull final Maybe<V> model) {
            return Plans.write(model, mValue);
        }
    }

    private static class Composition<M> extends Mapper.ReadWrite.Base<M> {

        @NonNull
        private final Mapper.Read<M> mRead;
        @NonNull
        private final Mapper.Write<M> mWrite;

        private Composition(@NonNull final Mapper.Read<M> read,
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
        public final Select.Projection getProjection() {
            return mRead.getProjection();
        }

        @NonNull
        @Override
        public final Reading.Item.Create<M> prepareRead() {
            return mRead.prepareRead();
        }

        @NonNull
        @Override
        public final Reading.Item<M> prepareRead(@NonNull final M model) {
            return mRead.prepareRead(model);
        }

        @NonNull
        @Override
        public final Plan.Write prepareWrite(@NonNull final Maybe<M> value) {
            return mWrite.prepareWrite(value);
        }
    }

    private Mappers() {
        super();
    }
}
