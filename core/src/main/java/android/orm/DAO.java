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

import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.orm.dao.ErrorHandler;
import android.orm.dao.Result;
import android.orm.dao.direct.Notifier;
import android.orm.dao.direct.Savepoint;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Expression;
import android.orm.sql.Select;
import android.orm.sql.Statement;
import android.orm.sql.Value;
import android.orm.util.Cancelable;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.orm.model.Observer.beforeCreate;
import static android.orm.model.Observer.beforeUpdate;
import static android.orm.model.Plans.write;
import static android.orm.util.Maybes.something;

public final class DAO {

    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

    @NonNull
    public static Direct direct(@NonNull final SQLiteDatabase database,
                                @NonNull final Notifier notifier) {
        return new android.orm.dao.Direct(database, notifier);
    }

    @NonNull
    public static Local local(@NonNull final Context context, @NonNull final Database database) {
        return local(context, database, DEFAULT_EXECUTOR);
    }

    @NonNull
    public static Local local(@NonNull final Context context,
                              @NonNull final Database database,
                              @NonNull final ExecutorService executor) {
        return new android.orm.dao.Local(context, database, executor);
    }

    @NonNull
    public static Remote remote(@NonNull final Context context) {
        return remote(context, DEFAULT_EXECUTOR);
    }

    @NonNull
    public static Remote remote(@NonNull final Context context,
                                @NonNull final ExecutorService executor) {
        return new android.orm.dao.Remote(context, executor);
    }

    public interface Direct {

        @NonNull
        Access.Single at(@NonNull final Route.Item route, @NonNull final Object... arguments);

        @NonNull
        Access.Many at(@NonNull final Route.Dir route, @NonNull final Object... arguments);

        @NonNull
        Access.Some at(@NonNull final Route route, @NonNull final Object... arguments);

        void execute(@NonNull final Statement statement);

        @NonNull
        <V> Maybe<V> execute(@NonNull final Expression<V> expression);

        @NonNull
        Savepoint savepoint(@NonNls @NonNull final String name);

        interface Exists extends DAO.Access.Exists<Maybe<Boolean>> {
        }

        final class Query {

            public interface Single extends DAO.Access.Read.Single<Maybe<Boolean>> {

                @NonNull
                @Override
                <M> Builder<M> query(@NonNull final Value.Read<M> value);

                @NonNull
                @Override
                <M> Builder.Refreshable<M> query(@NonNull final Mapper.Read<M> mapper);

                @NonNull
                @Override
                <M> Builder.Refreshable<M> query(@NonNull final Reading.Single<M> reading);
            }

            public interface Many extends DAO.Access.Read.Many<Maybe<Boolean>> {

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
            }

            public interface Builder<V> extends DAO.Access.Query.Builder<V, Maybe<V>> {

                @NonNull
                @Override
                Builder<V> with(@Nullable final Select.Where where);

                @NonNull
                @Override
                Builder<V> with(@Nullable final Select.Order order);

                @NonNull
                Builder<V> with(@Nullable final Select.Limit limit);

                @NonNull
                Builder<V> with(@Nullable final Select.Offset offset);

                interface Refreshable<V> extends Builder<V>, DAO.Access.Query.Builder.Refreshable<V, Maybe<V>> {

                    @NonNull
                    @Override
                    Builder.Refreshable<V> with(@Nullable final Select.Where where);

                    @NonNull
                    @Override
                    Builder.Refreshable<V> with(@Nullable final Select.Order order);

                    @NonNull
                    @Override
                    Builder.Refreshable<V> with(@Nullable final Select.Limit limit);

                    @NonNull
                    @Override
                    Builder.Refreshable<V> with(@Nullable final Select.Offset offset);

                    @NonNull
                    @Override
                    Builder.Refreshable<V> using(@Nullable final V v);
                }
            }

            private Query() {
                super();
            }
        }

        final class Read {

            public interface Single extends Exists, Query.Single {
            }

            public interface Many extends Exists, Query.Many {
            }

            private Read() {
                super();
            }
        }

        interface Insert extends DAO.Access.Insert<Maybe<Uri>> {
        }

        interface Update extends DAO.Access.Update<Maybe<Integer>> {
        }

        interface Delete extends DAO.Access.Delete<Maybe<Integer>> {
        }

        interface Write extends Insert, Update, Delete, DAO.Access.Write<Maybe<Uri>, Maybe<Integer>, Maybe<Integer>> {
        }

        final class Access {

            public interface Some extends Exists, Write {
            }

            public interface Single extends Read.Single, Some {
            }

            public interface Many extends Read.Many, Some {
            }

            private Access() {
                super();
            }
        }
    }

    public interface Local extends Async {

        @NonNull
        Access.Single at(@NonNull final Route.Item route, @NonNull final Object... arguments);

        @NonNull
        Access.Many at(@NonNull final Route.Dir route, @NonNull final Object... arguments);

        @NonNull
        Access.Some at(@NonNull final Route route, @NonNull final Object... arguments);

        @NonNull
        <V> Result<V> execute(@NonNull final Expression<V> expression);

        @NonNull
        <V> Result<V> execute(@NonNull final android.orm.dao.local.Transaction<V> transaction);

        final class Query {

            public interface Single extends Async.Query.Single {

                @NonNull
                @Override
                <M> Builder<M> query(@NonNull final Value.Read<M> value);

                @NonNull
                @Override
                <M> Builder.Refreshable<M> query(@NonNull final Mapper.Read<M> mapper);

                @NonNull
                @Override
                <M> Builder.Refreshable<M> query(@NonNull final Reading.Single<M> reading);
            }

            public interface Many extends Async.Query.Many {

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
            }

            public interface Builder<V> extends Async.Query.Builder<V> {

                @NonNull
                @Override
                Builder<V> with(@Nullable final Select.Where where);

                @NonNull
                @Override
                Builder<V> with(@Nullable final Select.Order order);

                @NonNull
                Builder<V> with(@Nullable final Select.Limit limit);

                @NonNull
                Builder<V> with(@Nullable final Select.Offset offset);

                interface Refreshable<V> extends Builder<V>, Async.Query.Builder.Refreshable<V> {

                    @NonNull
                    @Override
                    Builder.Refreshable<V> with(@Nullable final Select.Where where);

                    @NonNull
                    @Override
                    Builder.Refreshable<V> with(@Nullable final Select.Order order);

                    @NonNull
                    @Override
                    Builder.Refreshable<V> with(@Nullable final Select.Limit limit);

                    @NonNull
                    @Override
                    Builder.Refreshable<V> with(@Nullable final Select.Offset offset);

                    @NonNull
                    @Override
                    Builder.Refreshable<V> using(@Nullable final V v);
                }
            }

            private Query() {
                super();
            }
        }

        final class Read {

            public interface Single extends Query.Single, Async.Read.Single {
            }

            public interface Many extends Query.Many, Async.Read.Many {
            }

            private Read() {
                super();
            }
        }

        final class Access {

            public interface Some extends Async.Access.Some {
            }

            public interface Single extends Read.Single, Async.Access.Single, Some {
            }

            public interface Many extends Read.Many, Async.Access.Many, Some {
            }

            private Access() {
                super();
            }
        }
    }

    public interface Remote extends Async {

        @NonNull
        Access.Single at(@NonNull final Route.Item route, @NonNull final Object... arguments);

        @NonNull
        Access.Many at(@NonNull final Route.Dir route, @NonNull final Object... arguments);

        @NonNull
        Access.Some at(@NonNull final Route route, @NonNull final Object... arguments);

        @NonNull
        android.orm.dao.remote.Transaction transaction();

        @NonNull
        <V> Result<V> execute(@NonNull final Function<ContentResolver, Maybe<V>> function);
    }

    public interface Async {

        @NonNull
        Access.Single at(@NonNull final Route.Item route, @NonNull final Object... arguments);

        @NonNull
        Access.Many at(@NonNull final Route.Dir route, @NonNull final Object... arguments);

        @NonNull
        Access.Some at(@NonNull final Route route, @NonNull final Object... arguments);

        void start();

        void pause();

        void stop();

        void setErrorHandler(@Nullable final ErrorHandler handler);

        interface Exists extends DAO.Access.Exists<Result<Boolean>> {
        }

        final class Query {

            public interface Single extends DAO.Access.Read.Single<Result<Boolean>> {

                @NonNull
                @Override
                <M> Builder<M> query(@NonNull final Value.Read<M> value);

                @NonNull
                @Override
                <M> Builder.Refreshable<M> query(@NonNull final Mapper.Read<M> mapper);

                @NonNull
                @Override
                <M> Builder.Refreshable<M> query(@NonNull final Reading.Single<M> reading);
            }

            public interface Many extends DAO.Access.Read.Many<Result<Boolean>> {

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
            }

            public interface Builder<V> extends DAO.Access.Query.Builder<V, Result<V>>, DAO.Access.Watchable<V> {

                @NonNull
                @Override
                Builder<V> with(@Nullable final Select.Where where);

                @NonNull
                @Override
                Builder<V> with(@Nullable final Select.Order order);

                interface Refreshable<V> extends Builder<V>, DAO.Access.Query.Builder.Refreshable<V, Result<V>> {

                    @NonNull
                    @Override
                    Builder.Refreshable<V> with(@Nullable final Select.Where where);

                    @NonNull
                    @Override
                    Builder.Refreshable<V> with(@Nullable final Select.Order order);

                    @NonNull
                    @Override
                    Builder.Refreshable<V> using(@Nullable final V v);
                }
            }

            private Query() {
                super();
            }
        }

        final class Read {

            public interface Single extends Exists, Query.Single {
            }

            public interface Many extends Exists, Query.Many {
            }

            private Read() {
                super();
            }
        }

        interface Insert extends DAO.Access.Insert<Result<Uri>> {
        }

        interface Update extends DAO.Access.Update<Result<Integer>> {
        }

        interface Delete extends DAO.Access.Delete<Result<Integer>> {
        }

        interface Write extends Insert, Update, Delete, DAO.Access.Write<Result<Uri>, Result<Integer>, Result<Integer>> {
        }

        final class Access {

            public interface Some extends Exists, Write {
            }

            public interface Single extends Read.Single, Some {
            }

            public interface Many extends Read.Many, Some {
            }

            private Access() {
                super();
            }
        }
    }

    public static final class Access {

        public interface Watchable<V> {
            @NonNull
            Cancelable watch(@NonNull final Result.Callback<? super V> callback);
        }

        public interface Exists<R> {

            @NonNull
            R exists();

            @NonNull
            R exists(@NonNull final Select.Where where);
        }

        public static final class Query {

            public interface Single {

                @NonNull
                <M> Builder<M, ?> query(@NonNull final Value.Read<M> value);

                @NonNull
                <M> Builder<M, ?> query(@NonNull final Mapper.Read<M> mapper);

                @NonNull
                <M> Builder<M, ?> query(@NonNull final Reading.Single<M> reading);
            }

            public interface Many {

                @NonNull
                <M> Builder<M, ?> query(@NonNull final AggregateFunction<M> function);

                @NonNull
                <M> Builder<List<M>, ?> query(@NonNull final Value.Read<M> value);

                @NonNull
                <M> Builder<List<M>, ?> query(@NonNull final Mapper.Read<M> mapper);

                @NonNull
                <M> Builder<M, ?> query(@NonNull final Reading.Many<M> reading);
            }

            public interface Builder<V, R> {

                @NonNull
                Builder<V, R> with(@Nullable final Select.Where where);

                @NonNull
                Builder<V, R> with(@Nullable final Select.Order order);

                @NonNull
                R execute();

                interface Refreshable<V, R> extends Builder<V, R> {

                    @NonNull
                    @Override
                    Refreshable<V, R> with(@Nullable final Select.Where where);

                    @NonNull
                    @Override
                    Refreshable<V, R> with(@Nullable final Select.Order order);

                    @NonNull
                    Refreshable<V, R> using(@Nullable final V v);
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
            <M extends Instance.Writable> R update(@NonNull final Select.Where where,
                                                   @NonNull final M model);

            @NonNull
            <M> R update(@NonNull final M model, @NonNull final Value.Write<M> value);

            @NonNull
            <M> R update(@NonNull final Select.Where where,
                         @NonNull final M model,
                         @NonNull final Value.Write<M> value);

            @NonNull
            <M> R update(@NonNull final M model, @NonNull final Mapper.Write<M> mapper);

            @NonNull
            <M> R update(@NonNull final Select.Where where,
                         @NonNull final M model,
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
                protected abstract <M> U update(@NonNull final Select.Where where,
                                                @NonNull final M model,
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
                @Override
                public final <M extends Instance.Writable> U update(@NonNull final M model) {
                    beforeUpdate(model);
                    return update(model, write(model));
                }

                @NonNull
                @Override
                public final <M extends Instance.Writable> U update(@NonNull final Select.Where where,
                                                                    @NonNull final M model) {
                    beforeUpdate(model);
                    return update(where, model, write(model));
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
                public final <M> U update(@NonNull final Select.Where where,
                                          @NonNull final M model,
                                          @NonNull final Value.Write<M> value) {
                    beforeUpdate(model);
                    return update(where, model, write(something(model), value));
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
                public final <M> U update(@NonNull final Select.Where where,
                                          @NonNull final M model,
                                          @NonNull final Mapper.Write<M> mapper) {
                    beforeUpdate(model);
                    return update(where, model, mapper.prepareWrite(something(model)));
                }

                @NonNull
                @Override
                public final D delete() {
                    return delete(Select.Where.None);
                }

                @NonNull
                private <M> U update(@NonNull final M model, @NonNull final Plan.Write plan) {
                    return update(Select.Where.None, model, plan);
                }
            }
        }

        private Access() {
            super();
        }
    }

    private DAO() {
        super();
    }
}
