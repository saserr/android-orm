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

package android.orm;

import android.orm.access.Result;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.model.Readings;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Value;
import android.orm.sql.statement.Select;
import android.orm.util.Cancelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import static android.orm.model.Observer.beforeCreate;
import static android.orm.model.Observer.beforeUpdate;
import static android.orm.model.Plans.write;
import static android.orm.util.Maybes.something;

public final class Access {

    public interface Watchable<V> {
        @NonNull
        Cancelable watch(@NonNull final Result.Callback<? super V> callback);
    }

    public interface Exists {

        @NonNull
        Result<Boolean> exists();

        @NonNull
        Result<Boolean> exists(@NonNull final Select.Where where);
    }

    public static final class Query {

        public interface Single {

            @NonNull
            <M> Builder<M> query(@NonNull final Value.Read<M> value);

            @NonNull
            <M> Builder.Refreshable<M> query(@NonNull final Mapper.Read<M> mapper);

            @NonNull
            <M> Builder.Refreshable<M> query(@NonNull final Reading.Single<M> reading);
        }

        public interface Many {

            @NonNull
            <M> Builder<M> query(@NonNull final AggregateFunction<M> function);

            @NonNull
            <M> Builder.Refreshable<List<M>> query(@NonNull final Value.Read<M> value);

            @NonNull
            <M> Builder.Refreshable<List<M>> query(@NonNull final Mapper.Read<M> mapper);

            @NonNull
            <M> Builder.Refreshable<M> query(@NonNull final Reading.Many<M> reading);
        }

        public interface Builder<V> {

            @NonNull
            Builder<V> where(@Nullable final Select.Where where);

            @NonNull
            Builder<V> order(@Nullable final Select.Order order);

            @NonNull
            Result<V> execute();

            interface Refreshable<V> extends Builder<V> {
                @NonNull
                Refreshable<V> using(@Nullable final V v);
            }
        }

        private Query() {
            super();
        }
    }

    public static final class Read {

        public interface Builder<V> extends Watchable<V>, Query.Builder<V> {

            @NonNull
            @Override
            Builder<V> where(@Nullable final Select.Where where);

            @NonNull
            @Override
            Builder<V> order(@Nullable final Select.Order order);

            interface Refreshable<V> extends Builder<V>, Query.Builder.Refreshable<V> {

                @NonNull
                @Override
                Read.Builder.Refreshable<V> where(@Nullable final Select.Where where);

                @NonNull
                @Override
                Read.Builder.Refreshable<V> order(@Nullable final Select.Order order);

                @NonNull
                @Override
                Read.Builder.Refreshable<V> using(@Nullable final V v);
            }
        }

        public interface Single extends Exists, Query.Single {

            @NonNull
            @Override
            <M> Builder<M> query(@NonNull final Value.Read<M> value);

            @NonNull
            @Override
            <M> Builder.Refreshable<M> query(@NonNull final Mapper.Read<M> mapper);

            @NonNull
            @Override
            <M> Builder.Refreshable<M> query(@NonNull final Reading.Single<M> reading);

            abstract class Base implements Single {

                @NonNull
                @Override
                public final Result<Boolean> exists() {
                    return exists(Select.Where.None);
                }

                @NonNull
                @Override
                public final <M> Builder<M> query(@NonNull final Value.Read<M> value) {
                    return query(Readings.single(value));
                }

                @NonNull
                @Override
                public final <M> Builder.Refreshable<M> query(@NonNull final Mapper.Read<M> mapper) {
                    return query(Readings.single(mapper));
                }
            }
        }

        public interface Many extends Exists, Query.Many {

            @NonNull
            @Override
            <M> Builder<M> query(@NonNull final AggregateFunction<M> function);

            @NonNull
            @Override
            <M> Builder.Refreshable<List<M>> query(@NonNull final Value.Read<M> value);

            @NonNull
            @Override
            <M> Builder.Refreshable<List<M>> query(@NonNull final Mapper.Read<M> mapper);

            @NonNull
            @Override
            <M> Builder.Refreshable<M> query(@NonNull final Reading.Many<M> reading);

            abstract class Base implements Many {

                @NonNull
                @Override
                public final Result<Boolean> exists() {
                    return exists(Select.Where.None);
                }

                @NonNull
                @Override
                public final <M> Builder.Refreshable<List<M>> query(@NonNull final Value.Read<M> value) {
                    return query(Readings.list(value));
                }

                @NonNull
                @Override
                public final <M> Builder.Refreshable<List<M>> query(@NonNull final Mapper.Read<M> mapper) {
                    return query(Readings.list(mapper));
                }
            }
        }

        private Read() {
            super();
        }
    }

    public interface Insert<R> {

        @NonNull
        <M extends Instance.Writable> R insert(@NonNull final M model);

        @NonNull
        <M> R insert(@NonNull final M model, @NonNull final Value.Write<M> value);

        @NonNull
        <M> R insert(@NonNull final M model, @NonNull final Mapper.Write<M> mapper);
    }

    public interface Update<R> {

        @NonNull
        <M extends Instance.Writable> R update(@NonNull final M model);

        @NonNull
        <M extends Instance.Writable> R update(@NonNull final M model,
                                               @NonNull final Select.Where where);

        @NonNull
        <M> R update(@NonNull final M model, @NonNull final Value.Write<M> value);

        @NonNull
        <M> R update(@NonNull final M model,
                     @NonNull final Select.Where where,
                     @NonNull final Value.Write<M> value);

        @NonNull
        <M> R update(@NonNull final M model, @NonNull final Mapper.Write<M> mapper);

        @NonNull
        <M> R update(@NonNull final M model,
                     @NonNull final Select.Where where,
                     @NonNull final Mapper.Write<M> mapper);
    }

    public interface Delete<R> {

        @NonNull
        R delete();

        @NonNull
        R delete(@NonNull final Select.Where where);
    }

    public interface Write<I, U, D> extends Insert<I>, Update<U>, Delete<D> {

        abstract class Base<I, U, D> implements Write<I, U, D> {

            @NonNull
            protected abstract <M> I insert(@NonNull final M model,
                                            @NonNull final Plan.Write plan);

            @NonNull
            protected abstract <M> U update(@NonNull final M model,
                                            @NonNull final Select.Where where,
                                            @NonNull final Plan.Write plan);

            @NonNull
            @Override
            public final <M extends Instance.Writable> I insert(@NonNull final M model) {
                beforeCreate(model);
                return insert(model, write(model));
            }

            @NonNull
            @Override
            public final <M> I insert(@NonNull final M model,
                                      @NonNull final Value.Write<M> value) {
                beforeCreate(model);
                return insert(model, write(something(model), value));
            }

            @NonNull
            @Override
            public final <M> I insert(@NonNull final M model,
                                      @NonNull final Mapper.Write<M> mapper) {
                beforeCreate(model);
                return insert(model, mapper.prepareWrite(something(model)));
            }

            @NonNull
            protected final <M> U update(@NonNull final M model,
                                         @NonNull final Plan.Write plan) {
                beforeUpdate(model);
                return update(model, Select.Where.None, plan);
            }

            @NonNull
            @Override
            public final <M extends Instance.Writable> U update(@NonNull final M model) {
                beforeUpdate(model);
                return update(model, write(model));
            }

            @NonNull
            @Override
            public final <M extends Instance.Writable> U update(@NonNull final M model,
                                                                @NonNull final Select.Where where) {
                beforeUpdate(model);
                return update(model, where, write(model));
            }

            @NonNull
            @Override
            public final <M> U update(@NonNull final M model,
                                      @NonNull final Value.Write<M> value) {
                beforeUpdate(model);
                return update(model, write(something(model), value));
            }

            @NonNull
            @Override
            public final <M> U update(@NonNull final M model,
                                      @NonNull final Select.Where where,
                                      @NonNull final Value.Write<M> value) {
                beforeUpdate(model);
                return update(model, where, write(something(model), value));
            }

            @NonNull
            @Override
            public final <M> U update(@NonNull final M model,
                                      @NonNull final Mapper.Write<M> mapper) {
                beforeUpdate(model);
                return update(model, mapper.prepareWrite(something(model)));
            }

            @NonNull
            @Override
            public final <M> U update(@NonNull final M model,
                                      @NonNull final Select.Where where,
                                      @NonNull final Mapper.Write<M> mapper) {
                beforeUpdate(model);
                return update(model, where, mapper.prepareWrite(something(model)));
            }

            @NonNull
            @Override
            public final D delete() {
                return delete(Select.Where.None);
            }
        }
    }

    private Access() {
        super();
    }
}
