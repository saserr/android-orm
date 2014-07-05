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
import android.net.Uri;
import android.orm.dao.ErrorHandler;
import android.orm.dao.Result;
import android.orm.dao.Transaction;
import android.orm.dao.async.Observer;
import android.orm.dao.async.executor.DispatcherPerObserver;
import android.orm.dao.async.executor.DispatcherPerTable;
import android.orm.dao.async.executor.DispatcherPerUri;
import android.orm.dao.async.executor.FixedSize;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Expression;
import android.orm.sql.Select;
import android.orm.sql.Statement;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.util.Cancelable;
import android.orm.util.Function;
import android.orm.util.Lazy;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.orm.model.Plans.write;
import static android.orm.util.Maybes.something;
import static java.lang.Runtime.getRuntime;

public final class DAO {

    public interface TaskExecutors {

        Lazy<ExecutorService> SingleThread = new Lazy.Volatile<ExecutorService>() {
            @NonNull
            @Override
            protected ExecutorService produce() {
                return Executors.newSingleThreadExecutor();
            }
        };

        Lazy<ExecutorService> ThreadPerCore = new Lazy.Volatile<ExecutorService>() {
            @NonNull
            @Override
            protected ExecutorService produce() {
                return Executors.newFixedThreadPool(getRuntime().availableProcessors());
            }
        };

        Lazy<ExecutorService> CacheThreads = new Lazy.Volatile<ExecutorService>() {
            @NonNull
            @Override
            protected ExecutorService produce() {
                return Executors.newCachedThreadPool();
            }
        };
    }

    public interface ObserverExecutors {

        Lazy<Observer.Executor> SingleThread = new Lazy.Volatile<Observer.Executor>() {
            @NonNull
            @Override
            protected Observer.Executor produce() {
                return new FixedSize(1, 1);
            }
        };

        Lazy<Observer.Executor> ThreadPerCore = new Lazy.Volatile<Observer.Executor>() {
            @NonNull
            @Override
            protected Observer.Executor produce() {
                final int processors = getRuntime().availableProcessors();
                return new FixedSize(processors, processors);
            }
        };

        Lazy<Observer.Executor> ThreadPerTable = new Lazy.Volatile<Observer.Executor>() {
            @NonNull
            @Override
            protected Observer.Executor produce() {
                return new DispatcherPerTable();
            }
        };

        Lazy<Observer.Executor> ThreadPerUri = new Lazy.Volatile<Observer.Executor>() {
            @NonNull
            @Override
            protected Observer.Executor produce() {
                return new DispatcherPerUri();
            }
        };

        Lazy<Observer.Executor> ThreadPerObserver = new Lazy.Volatile<Observer.Executor>() {
            @NonNull
            @Override
            protected Observer.Executor produce() {
                return new DispatcherPerObserver();
            }
        };
    }

    private static final Lazy<ExecutorService> DEFAULT_TASK_EXECUTOR = TaskExecutors.CacheThreads;
    private static final Lazy<Observer.Executor> DEFAULT_OBSERVER_EXECUTOR = ObserverExecutors.ThreadPerObserver;

    @NonNull
    public static Direct direct(@NonNull final Context context, @NonNull final Database database) {
        return android.orm.dao.Direct.create(context, database);
    }

    @NonNull
    public static Local local(@NonNull final Context context, @NonNull final Database database) {
        return local(context, database, DEFAULT_TASK_EXECUTOR.get(), DEFAULT_OBSERVER_EXECUTOR.get());
    }

    @NonNull
    public static Local local(@NonNull final Context context,
                              @NonNull final Database database,
                              @NonNull final ExecutorService executor) {
        return local(context, database, executor, DEFAULT_OBSERVER_EXECUTOR.get());
    }

    @NonNull
    public static Local local(@NonNull final Context context,
                              @NonNull final Database database,
                              @NonNull final Observer.Executor executor) {
        return local(context, database, DEFAULT_TASK_EXECUTOR.get(), executor);
    }

    @NonNull
    public static Local local(@NonNull final Context context,
                              @NonNull final Database database,
                              @NonNull final ExecutorService tasks,
                              @NonNull final Observer.Executor observers) {
        return new android.orm.dao.Local(context, database, tasks, observers);
    }

    @NonNull
    public static Remote remote(@NonNull final Context context) {
        return remote(context, DEFAULT_TASK_EXECUTOR.get(), DEFAULT_OBSERVER_EXECUTOR.get());
    }

    @NonNull
    public static Remote remote(@NonNull final Context context,
                                @NonNull final ExecutorService tasks) {
        return remote(context, tasks, DEFAULT_OBSERVER_EXECUTOR.get());
    }

    @NonNull
    public static Remote remote(@NonNull final Context context,
                                @NonNull final Observer.Executor observers) {
        return remote(context, DEFAULT_TASK_EXECUTOR.get(), observers);
    }

    @NonNull
    public static Remote remote(@NonNull final Context context,
                                @NonNull final ExecutorService tasks,
                                @NonNull final Observer.Executor observers) {
        return new android.orm.dao.Remote(context, tasks, observers);
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
        <V> Maybe<V> execute(@NonNull final Transaction.Direct<V> transaction);

        interface Exists extends DAO.Access.Exists<Maybe<Boolean>> {
        }

        final class Query {

            public interface Single extends DAO.Access.Query.Single {

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

            public interface Many extends DAO.Access.Query.Many {

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

                public interface Single<V> extends DAO.Access.Query.Builder.Single<V, Maybe<V>> {

                    @NonNull
                    @Override
                    Single<V> with(@Nullable final Select.Where where);

                    interface Refreshable<V> extends Single<V>, DAO.Access.Query.Builder.Single.Refreshable<V, Maybe<V>> {

                        @NonNull
                        @Override
                        Single.Refreshable<V> with(@Nullable final Select.Where where);

                        @NonNull
                        @Override
                        Single.Refreshable<V> using(@Nullable final V v);
                    }
                }

                public interface Many<V> extends DAO.Access.Query.Builder.Many<V, Maybe<V>>, Single<V> {

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Select.Where where);

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Select.Order order);

                    @NonNull
                    Many<V> with(@Nullable final Select.Limit limit);

                    @NonNull
                    Many<V> with(@Nullable final Select.Offset offset);

                    interface Refreshable<V> extends Many<V>, DAO.Access.Query.Builder.Many.Refreshable<V, Maybe<V>>, Single.Refreshable<V> {

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Select.Where where);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Select.Order order);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Select.Limit limit);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Select.Offset offset);

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

        final class Read {

            public interface Single extends Exists, Query.Single, DAO.Access.Read.Single<Maybe<Boolean>> {
            }

            public interface Many extends Exists, Query.Many, DAO.Access.Read.Many<Maybe<Boolean>> {
            }

            private Read() {
                super();
            }
        }

        interface Insert extends DAO.Access.Insert<Maybe<Uri>> {
        }

        interface Save {

            @NonNull
            <M extends Instance.Writable> Maybe<Pair<Value.Write.Operation, Uri>> save(@NonNull final M model);

            @NonNull
            Maybe<Pair<Value.Write.Operation, Uri>> save(@NonNull final Writer writer);

            @NonNull
            <M> Maybe<Pair<Value.Write.Operation, Uri>> save(@Nullable final M model,
                                                             @NonNull final Value.Write<M> value);

            @NonNull
            <M> Maybe<Pair<Value.Write.Operation, Uri>> save(@NonNull final M model,
                                                             @NonNull final Mapper.Write<M> mapper);
        }

        final class Update {

            public interface Single extends DAO.Access.Update<Maybe<Uri>> {
            }

            public interface Many extends DAO.Access.Update<Maybe<Integer>> {
            }

            private Update() {
                super();
            }
        }

        interface Delete extends DAO.Access.Delete<Maybe<Integer>> {
        }

        final class Write {

            public interface Single extends Insert, Save, Update.Single, Delete, DAO.Access.Write<Maybe<Uri>, Maybe<Uri>, Maybe<Integer>> {
            }

            public interface Many extends Insert, Update.Many, Delete, DAO.Access.Write<Maybe<Uri>, Maybe<Integer>, Maybe<Integer>> {
            }

            private Write() {
                super();
            }
        }

        final class Access {

            public interface Some extends Exists, Write.Many {
            }

            public interface Single extends Read.Single, Write.Single {
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
        @Override
        Access.Many at(@NonNull final Route.Dir route, @NonNull final Object... arguments);

        @NonNull
        <V> Result<V> execute(@NonNull final Expression<V> expression);

        @NonNull
        <V> Result<V> execute(@NonNull final Transaction.Direct<V> transaction);

        final class Query {

            public interface Many extends Async.Query.Many {

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

                public interface Many<V> extends Async.Query.Builder.Many<V> {

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Select.Where where);

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Select.Order order);

                    @NonNull
                    Many<V> with(@Nullable final Select.Limit limit);

                    @NonNull
                    Many<V> with(@Nullable final Select.Offset offset);

                    interface Refreshable<V> extends Many<V>, Async.Query.Builder.Many.Refreshable<V> {

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Select.Where where);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Select.Order order);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Select.Limit limit);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Select.Offset offset);

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

        final class Read {

            public interface Many extends Query.Many, Async.Read.Many {
            }

            private Read() {
                super();
            }
        }

        final class Access {

            public interface Many extends Read.Many, Async.Access.Many {
            }

            private Access() {
                super();
            }
        }
    }

    public interface Remote extends Async {

        @NonNull
        Transaction.Remote transaction();

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

            public interface Single extends DAO.Access.Query.Single {

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

            public interface Many extends DAO.Access.Query.Many {

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

                public interface Single<V> extends DAO.Access.Query.Builder.Single<V, Result<V>>, DAO.Access.Watchable<V> {

                    @NonNull
                    @Override
                    Single<V> with(@Nullable final Select.Where where);

                    interface Refreshable<V> extends Single<V>, DAO.Access.Query.Builder.Single.Refreshable<V, Result<V>> {

                        @NonNull
                        @Override
                        Single.Refreshable<V> with(@Nullable final Select.Where where);

                        @NonNull
                        @Override
                        Single.Refreshable<V> using(@Nullable final V v);
                    }
                }

                public interface Many<V> extends DAO.Access.Query.Builder.Many<V, Result<V>>, Single<V> {

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Select.Where where);

                    @NonNull
                    @Override
                    Many<V> with(@Nullable final Select.Order order);

                    interface Refreshable<V> extends Many<V>, DAO.Access.Query.Builder.Many.Refreshable<V, Result<V>>, Single.Refreshable<V> {

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Select.Where where);

                        @NonNull
                        @Override
                        Many.Refreshable<V> with(@Nullable final Select.Order order);

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

        final class Read {

            public interface Single extends Exists, Query.Single, DAO.Access.Read.Single<Result<Boolean>> {
            }

            public interface Many extends Exists, Query.Many, DAO.Access.Read.Many<Result<Boolean>> {
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

            public interface Some extends Exists, Write, DAO.Access.Notifiable {
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

        public interface Notifiable {
            Cancelable onChange(@NonNull final Observer observer);
        }

        public interface Watchable<V> {
            Cancelable onChange(@NonNull final Result.Callback<? super V> callback);
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
                    Single<V, R> with(@Nullable final Select.Where where);

                    @NonNull
                    R execute();

                    interface Refreshable<V, R> extends Single<V, R> {

                        @NonNull
                        @Override
                        Refreshable<V, R> with(@Nullable final Select.Where where);

                        @NonNull
                        Refreshable<V, R> using(@Nullable final V v);
                    }
                }

                public interface Many<V, R> extends Single<V, R> {

                    @NonNull
                    @Override
                    Many<V, R> with(@Nullable final Select.Where where);

                    @NonNull
                    Many<V, R> with(@Nullable final Select.Order order);

                    interface Refreshable<V, R> extends Many<V, R>, Single.Refreshable<V, R> {

                        @NonNull
                        @Override
                        Many.Refreshable<V, R> with(@Nullable final Select.Where where);

                        @NonNull
                        @Override
                        Many.Refreshable<V, R> with(@Nullable final Select.Order order);

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
            <M extends Model> R update(@NonNull final Select.Where where, @NonNull final M model);

            @NonNull
            <M extends Instance.Writable> R update(@NonNull final M model);

            @NonNull
            <M extends Instance.Writable> R update(@NonNull final Select.Where where,
                                                   @NonNull final M model);

            @NonNull
            R update(@NonNull final Writer writer);

            @NonNull
            R update(@NonNull final Select.Where where, @NonNull final Writer writer);

            @NonNull
            <M> R update(@Nullable final M model, @NonNull final Value.Write<M> value);

            @NonNull
            <M> R update(@NonNull final Select.Where where,
                         @Nullable final M model,
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
                protected abstract <M> I insert(@Nullable final M model,
                                                @NonNull final Plan.Write plan);

                @NonNull
                protected abstract <M> U update(@NonNull final Select.Where where,
                                                @Nullable final M model,
                                                @NonNull final Plan.Write plan);

                @NonNull
                @Override
                public final <M extends Model> I insert(@NonNull final M model) {
                    return insert(Model.toInstance(model));
                }

                @NonNull
                @Override
                public final <M extends Instance.Writable> I insert(@NonNull final M model) {
                    return insert(model, write(model));
                }

                @NonNull
                @Override
                public final I insert(@NonNull final Writer writer) {
                    return insert(null, write(writer));
                }

                @NonNull
                @Override
                public final <M> I insert(@Nullable final M model,
                                          @NonNull final Value.Write<M> value) {
                    return insert(model, write(something(model), value));
                }

                @NonNull
                @Override
                public final <M> I insert(@NonNull final M model,
                                          @NonNull final Mapper.Write<M> mapper) {
                    return insert(model, mapper.prepareWrite(model));
                }

                @NonNull
                @Override
                public final <M extends Model> U update(@NonNull final M model) {
                    return update(Model.toInstance(model));
                }

                @NonNull
                @Override
                public final <M extends Model> U update(@NonNull final Select.Where where,
                                                        @NonNull final M model) {
                    return update(where, Model.toInstance(model));
                }

                @NonNull
                @Override
                public final <M extends Instance.Writable> U update(@NonNull final M model) {
                    return update(model, write(model));
                }

                @NonNull
                @Override
                public final <M extends Instance.Writable> U update(@NonNull final Select.Where where,
                                                                    @NonNull final M model) {
                    return update(where, model, write(model));
                }

                @NonNull
                @Override
                public final U update(@NonNull final Writer writer) {
                    return update((Void) null, write(writer));
                }

                @NonNull
                @Override
                public final U update(@NonNull final Select.Where where,
                                      @NonNull final Writer writer) {
                    return update(where, null, write(writer));
                }

                @NonNull
                @Override
                public final <M> U update(@Nullable final M model,
                                          @NonNull final Value.Write<M> value) {
                    return update(model, write(something(model), value));
                }

                @NonNull
                @Override
                public final <M> U update(@NonNull final Select.Where where,
                                          @Nullable final M model,
                                          @NonNull final Value.Write<M> value) {
                    return update(where, model, write(something(model), value));
                }

                @NonNull
                @Override
                public final <M> U update(@NonNull final M model,
                                          @NonNull final Mapper.Write<M> mapper) {
                    return update(model, mapper.prepareWrite(model));
                }

                @NonNull
                @Override
                public final <M> U update(@NonNull final Select.Where where,
                                          @NonNull final M model,
                                          @NonNull final Mapper.Write<M> mapper) {
                    return update(where, model, mapper.prepareWrite(model));
                }

                @NonNull
                @Override
                public final D delete() {
                    return delete(Select.Where.None);
                }

                @NonNull
                protected final <M> U update(@Nullable final M model,
                                             @NonNull final Plan.Write plan) {
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
