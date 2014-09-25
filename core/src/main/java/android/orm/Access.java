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

import android.orm.dao.Result;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Where;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

public final class Access {

    public static final class Direct {

        public interface Exists extends Access.Exists<Maybe<Boolean>> {
        }

        public static final class Query {

            public interface Single extends Access.Query.Single {

                @NonNull
                <M extends Model> Maybe<M> query(@NonNull final M model);

                @NonNull
                <M extends Instance.Readable> Maybe<M> query(@NonNull final M model);

                @NonNull
                @Override
                <M> Builder.Single<M> query(@NonNull final Value.Read<M> value);

                @NonNull
                @Override
                <M> Builder.Single.Refreshable<M> query(@NonNull final Mapper.Read<M> mapper);

                @NonNull
                @Override
                <M> Builder.Single.Refreshable<M> query(@NonNull final Reading.Single<M> reading);
            }

            public interface Many extends Access.Query.Many {

                @NonNull
                @Override
                <M> Builder.Many<M> query(@NonNull final AggregateFunction<M> function);

                @NonNull
                @Override
                <M> Builder.Many.Refreshable<List<M>> query(@NonNull final Value.Read<M> value);

                @NonNull
                @Override
                <M> Builder.Many.Refreshable<List<M>> query(@NonNull final Mapper.Read<M> mapper);

                @NonNull
                @Override
                <M> Builder.Many.Refreshable<M> query(@NonNull final Reading.Many<M> reading);
            }

            public static final class Builder {

                public interface Single<V> extends Access.Query.Builder.Single<V, Maybe<V>> {

                    @NonNull
                    @Override
                    Single<V> with(@Nullable final Where where);

                    interface Refreshable<V> extends Single<V>, Access.Query.Builder.Single.Refreshable<V, Maybe<V>> {

                        @NonNull
                        @Override
                        Single.Refreshable<V> with(@Nullable final Where where);

                        @NonNull
                        @Override
                        Single.Refreshable<V> using(@Nullable final V v);
                    }
                }

                public interface Many<V> extends Access.Query.Builder.Many<V, Maybe<V>>, Single<V> {

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Where where);

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Order order);

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Limit limit);

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Offset offset);

                    interface Refreshable<V> extends Many<V>, Access.Query.Builder.Many.Refreshable<V, Maybe<V>>, Single.Refreshable<V> {

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Where where);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Order order);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Limit limit);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Offset offset);

                        @NonNull
                        @Override
                        Many.Refreshable<V> using(@Nullable final V v);
                    }
                }

                private Builder() {
                    super();
                }
            }

            private Query() {
                super();
            }
        }

        public static final class Read {

            public interface Single extends Exists, Query.Single, Access.Read.Single<Maybe<Boolean>> {
            }

            public interface Many extends Exists, Query.Many, Access.Read.Many<Maybe<Boolean>> {
            }

            private Read() {
                super();
            }
        }

        public interface Insert<K> extends Access.Insert<Maybe<K>> {
        }

        public static final class Update {

            public interface Single<K> extends Access.Update<Maybe<K>> {
            }

            public interface Many extends Access.Update<Maybe<Integer>> {
            }

            private Update() {
                super();
            }
        }

        public interface Delete extends Access.Delete<Maybe<Integer>> {
        }

        public static final class Write {

            public interface Some<K> extends Insert<K>, Delete {
            }

            public interface Single<K> extends Update.Single<K>, Some<K> {
            }

            public interface Many<K> extends Update.Many, Some<K> {
            }

            private Write() {
                super();
            }
        }

        public interface Some<K> extends Exists, Write.Some<K>, Access.Some<Maybe<Boolean>, Maybe<K>, Maybe<Integer>> {
        }

        public interface Single<K> extends Read.Single, Write.Single<K>, Some<K>, Access.Single<Maybe<Boolean>, Maybe<K>, Maybe<K>, Maybe<Integer>> {
        }

        public interface Many<K> extends Read.Many, Write.Many<K>, Some<K>, Access.Many<Maybe<Boolean>, Maybe<K>, Maybe<Integer>, Maybe<Integer>> {
        }

        private Direct() {
            super();
        }
    }

    public static final class Async {

        public interface Exists extends Access.Exists<Result<Boolean>> {
        }

        public static final class Query {

            public interface Single extends Access.Query.Single {

                @NonNull
                <M extends Model> Result<M> query(@NonNull final M model);

                @NonNull
                <M extends Instance.Readable> Result<M> query(@NonNull final M model);

                @NonNull
                @Override
                <M> Builder.Single<M> query(@NonNull final Value.Read<M> value);

                @NonNull
                @Override
                <M> Builder.Single.Refreshable<M> query(@NonNull final Mapper.Read<M> mapper);

                @NonNull
                @Override
                <M> Builder.Single.Refreshable<M> query(@NonNull final Reading.Single<M> reading);
            }

            public interface Many extends Access.Query.Many {

                @NonNull
                @Override
                <M> Builder.Many<M> query(@NonNull final AggregateFunction<M> function);

                @NonNull
                @Override
                <M> Builder.Many.Refreshable<List<M>> query(@NonNull final Value.Read<M> value);

                @NonNull
                @Override
                <M> Builder.Many.Refreshable<List<M>> query(@NonNull final Mapper.Read<M> mapper);

                @NonNull
                @Override
                <M> Builder.Many.Refreshable<M> query(@NonNull final Reading.Many<M> reading);
            }

            public static final class Builder {

                public interface Single<V> extends Access.Query.Builder.Single<V, Result<V>> {

                    @NonNull
                    @Override
                    Single<V> with(@Nullable final Where where);

                    interface Refreshable<V> extends Single<V>, Access.Query.Builder.Single.Refreshable<V, Result<V>> {

                        @NonNull
                        @Override
                        Single.Refreshable<V> with(@Nullable final Where where);

                        @NonNull
                        @Override
                        Single.Refreshable<V> using(@Nullable final V v);
                    }
                }

                public interface Many<V> extends Access.Query.Builder.Many<V, Result<V>>, Single<V> {

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Where where);

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Order order);

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Limit limit);

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Offset offset);

                    interface Refreshable<V> extends Many<V>, Access.Query.Builder.Many.Refreshable<V, Result<V>>, Single.Refreshable<V> {

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Where where);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Order order);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Limit limit);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Offset offset);

                        @NonNull
                        @Override
                        Many.Refreshable<V> using(@Nullable final V v);
                    }
                }

                private Builder() {
                    super();
                }
            }

            private Query() {
                super();
            }
        }

        public static final class Read {

            public interface Single extends Exists, Query.Single, Access.Read.Single<Result<Boolean>> {
            }

            public interface Many extends Exists, Query.Many, Access.Read.Many<Result<Boolean>> {
            }

            private Read() {
                super();
            }
        }

        public interface Insert<K> extends Access.Insert<Result<K>> {
        }

        public static final class Update {

            public interface Single<K> extends Access.Update<Result<K>> {
            }

            public interface Many extends Access.Update<Result<Integer>> {
            }

            private Update() {
                super();
            }
        }

        public interface Delete extends Access.Delete<Result<Integer>> {
        }

        public static final class Write {

            public interface Some<K> extends Insert<K>, Delete {
            }

            public interface Single<K> extends Update.Single<K>, Some<K> {
            }

            public interface Many<K> extends Update.Many, Some<K> {
            }

            private Write() {
                super();
            }
        }

        public interface Some<K> extends Exists, Write.Some<K>, Access.Some<Result<Boolean>, Result<K>, Result<Integer>> {
        }

        public interface Single<K> extends Read.Single, Write.Single<K>, Some<K>, Access.Single<Result<Boolean>, Result<K>, Result<K>, Result<Integer>> {
        }

        public interface Many<K> extends Read.Many, Write.Many<K>, Some<K>, Access.Many<Result<Boolean>, Result<K>, Result<Integer>, Result<Integer>> {
        }

        private Async() {
            super();
        }
    }

    public interface Exists<R> {

        @NonNull
        R exists();

        @NonNull
        R exists(@NonNull final Where where);
    }

    public static final class Query {

        public interface Single {

            @NonNull
            <M> Builder.Single<M, ?> query(@NonNull final Value.Read<M> value);

            @NonNull
            <M> Builder.Single.Refreshable<M, ?> query(@NonNull final Mapper.Read<M> mapper);

            @NonNull
            <M> Builder.Single.Refreshable<M, ?> query(@NonNull final Reading.Single<M> reading);
        }

        public interface Many {

            @NonNull
            <M> Builder.Many<M, ?> query(@NonNull final AggregateFunction<M> function);

            @NonNull
            <M> Builder.Many<List<M>, ?> query(@NonNull final Value.Read<M> value);

            @NonNull
            <M> Builder.Many.Refreshable<List<M>, ?> query(@NonNull final Mapper.Read<M> mapper);

            @NonNull
            <M> Builder.Many.Refreshable<M, ?> query(@NonNull final Reading.Many<M> reading);
        }

        public static final class Builder {

            public interface Single<V, R> {

                @NonNull
                Single<V, R> with(@Nullable final Where where);

                @NonNull
                R execute();

                interface Refreshable<V, R> extends Single<V, R> {

                    @NonNull
                    @Override
                    Refreshable<V, R> with(@Nullable final Where where);

                    @NonNull
                    Refreshable<V, R> using(@Nullable final V v);
                }
            }

            public interface Many<V, R> extends Single<V, R> {

                @NonNull
                @Override
                Many<V, R> with(@Nullable final Where where);

                @NonNull
                Many<V, R> with(@Nullable final Order order);

                @NonNull
                Many<V, R> with(@Nullable final Limit limit);

                @NonNull
                Many<V, R> with(@Nullable final Offset offset);

                interface Refreshable<V, R> extends Many<V, R>, Single.Refreshable<V, R> {

                    @NonNull
                    @Override
                    Many.Refreshable<V, R> with(@Nullable final Where where);

                    @NonNull
                    @Override
                    Many.Refreshable<V, R> with(@Nullable final Order order);

                    @NonNull
                    @Override
                    Many.Refreshable<V, R> with(@Nullable final Limit limit);

                    @NonNull
                    @Override
                    Many.Refreshable<V, R> with(@Nullable final Offset offset);

                    @NonNull
                    @Override
                    Many.Refreshable<V, R> using(@Nullable final V v);
                }
            }

            private Builder() {
                super();
            }
        }

        private Query() {
            super();
        }
    }

    public static final class Read {

        public interface Single<E> extends Exists<E>, Query.Single {
        }

        public interface Many<E> extends Exists<E>, Query.Many {
        }

        private Read() {
            super();
        }
    }

    public interface Insert<R> {

        @NonNull
        <M extends Model> R insert(@NonNull final M model);

        @NonNull
        <M extends Instance.Writable> R insert(@NonNull final M model);

        @NonNull
        R insert(@NonNull final Writer writer);

        @NonNull
        <M> R insert(@Nullable final M model, @NonNull final Value.Write<M> value);

        @NonNull
        <M> R insert(@NonNull final M model, @NonNull final Mapper.Write<M> mapper);
    }

    public interface Update<R> {

        @NonNull
        <M extends Model> R update(@NonNull final M model);

        @NonNull
        <M extends Model> R update(@NonNull final Where where, @NonNull final M model);

        @NonNull
        <M extends Instance.Writable> R update(@NonNull final M model);

        @NonNull
        <M extends Instance.Writable> R update(@NonNull final Where where,
                                               @NonNull final M model);

        @NonNull
        R update(@NonNull final Writer writer);

        @NonNull
        R update(@NonNull final Where where, @NonNull final Writer writer);

        @NonNull
        <M> R update(@Nullable final M model, @NonNull final Value.Write<M> value);

        @NonNull
        <M> R update(@NonNull final Where where,
                     @Nullable final M model,
                     @NonNull final Value.Write<M> value);

        @NonNull
        <M> R update(@NonNull final M model, @NonNull final Mapper.Write<M> mapper);

        @NonNull
        <M> R update(@NonNull final Where where,
                     @NonNull final M model,
                     @NonNull final Mapper.Write<M> mapper);
    }

    public interface Delete<R> {

        @NonNull
        R delete();

        @NonNull
        R delete(@NonNull final Where where);
    }

    public interface Some<E, I, D> extends Exists<E>, Insert<I>, Delete<D> {
    }

    public interface Single<E, I, U, D> extends Read.Single<E>, Update<U>, Some<E, I, D> {
    }

    public interface Many<E, I, U, D> extends Read.Many<E>, Update<U>, Some<E, I, D> {
    }

    private Access() {
        super();
    }
}
