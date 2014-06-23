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

package android.orm.dao.local;

import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.orm.Route;
import android.orm.dao.Local;
import android.orm.dao.Result;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Value;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;
import static android.orm.util.Maybes.nothing;
import static android.util.Log.INFO;

public interface Transaction<V> {

    @NonNull
    <T> Transaction<T> andThen(@NonNull final Action<? super V, ? extends T> action);

    @NonNull
    Result<V> execute();

    @NonNull
    Transaction<V> savepoint(@NonNls @NonNull final String name);

    interface Exists extends android.orm.DAO.Access.Exists<Transaction<Boolean>> {
    }

    final class Query {

        public interface Single extends android.orm.DAO.Access.Query.Single {

            @NonNull
            @Override
            <M> Builder<M> query(@NonNull final Value.Read<M> value);

            @NonNull
            @Override
            <M> Builder<M> query(@NonNull final Mapper.Read<M> mapper);

            @NonNull
            @Override
            <M> Builder<M> query(@NonNull final Reading.Single<M> reading);
        }

        public interface Many extends android.orm.DAO.Access.Query.Many {

            @NonNull
            @Override
            <M> Builder<M> query(@NonNull final AggregateFunction<M> function);

            @NonNull
            @Override
            <M> Builder<List<M>> query(@NonNull final Value.Read<M> value);

            @NonNull
            @Override
            <M> Builder<List<M>> query(@NonNull final Mapper.Read<M> mapper);

            @NonNull
            @Override
            <M> Builder<M> query(@NonNull final Reading.Many<M> reading);
        }

        public interface Builder<V> extends android.orm.DAO.Access.Query.Builder<V, Result<V>> {

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

            @NonNull
            <T> Transaction<T> andThen(@NonNull final Action<? super V, ? extends T> action);
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

    interface Insert extends android.orm.DAO.Access.Insert<Transaction<Uri>> {
    }

    interface Update extends android.orm.DAO.Access.Update<Transaction<Integer>> {
    }

    interface Delete extends android.orm.DAO.Access.Delete<Transaction<Integer>> {
    }

    interface Write extends Insert, Update, Delete, android.orm.DAO.Access.Write<Transaction<Uri>, Transaction<Integer>, Transaction<Integer>> {
    }

    final class Access {

        public interface Single extends Read.Single, Write {
        }

        public interface Many extends Read.Many, Write {
        }

        public interface Some extends Exists, Write {
        }

        private Access() {
            super();
        }
    }

    class Begin {

        private static final String TAG = Transaction.class.getSimpleName();

        @NonNull
        private final Executor mExecutor;

        public Begin(@NonNull final Context context, @NonNull final Local local) {
            super();

            mExecutor = new Executor() {
                @NonNull
                @Override
                public <V> Result<V> execute(@NonNull final Executable<V> executable) {
                    final Notifier<V> notifier = new Notifier<>(context);
                    final DAO dao = new DAO(notifier);

                    return local.execute(new Function<SQLiteDatabase, Maybe<V>>() {
                        @NonNull
                        @Override
                        public Maybe<V> invoke(@NonNull final SQLiteDatabase database) {
                            Maybe<V> result = nothing();

                            try {
                                result = notifier.invoke(executable.execute(dao, database));
                            } catch (final Action.Abort ignored) {
                                if (Log.isLoggable(TAG, INFO)) {
                                    Log.i(TAG, "Transaction has been rolled back"); //NON-NLS
                                }
                            }

                            return result;
                        }
                    });
                }
            };
        }

        @NonNull
        public final Access.Single at(@NonNull final Route.Item route,
                                      @NonNull final Object... arguments) {
            return new SingleAccess(mExecutor, route, arguments);
        }

        @NonNull
        public final Access.Many at(@NonNull final Route.Dir route,
                                    @NonNull final Object... arguments) {
            return new ManyAccess(mExecutor, route, arguments);
        }

        @NonNull
        public final Access.Some at(@NonNull final Route route,
                                    @NonNull final Object... arguments) {
            return new SomeAccess(mExecutor, route, arguments);
        }

        private static class Notifier<V> implements Function<Maybe<V>, Maybe<V>>, android.orm.dao.local.Notifier {

            @NonNull
            private final ContentResolver mResolver;

            private final List<Uri> mUris = new ArrayList<>();

            private Notifier(@NonNull final Context context) {
                super();

                mResolver = context.getContentResolver();
            }

            @Override
            public final void notifyChange(@NonNull final Uri uri) {
                mUris.add(uri);
            }

            public final void clear() {
                mUris.clear();
            }

            @NonNull
            @Override
            public final Maybe<V> invoke(@NonNull final Maybe<V> value) {
                for (final Uri uri : mUris) {
                    mResolver.notifyChange(uri, null);
                }
                return value;
            }
        }

        private static class First<V> implements Transaction<V>, Executable<V> {

            @NonNull
            private final Executor mExecutor;
            @NonNull
            private final Executable<V> mExecutable;

            private First(@NonNull final Executor executor,
                          @NonNull final Executable<V> executable) {
                super();

                mExecutor = executor;
                mExecutable = executable;
            }

            @NonNull
            @Override
            public final <T> Transaction<T> andThen(@NonNull final Action<? super V, ? extends T> action) {
                return new Next<>(mExecutor, this, action);
            }

            @NonNull
            @Override
            public final Result<V> execute() {
                return mExecutor.execute(this);
            }

            @NonNull
            @Override
            public final Maybe<V> execute(@NonNull final DAO dao,
                                          @NonNull final SQLiteDatabase database) {
                return mExecutable.execute(dao, database);
            }

            @NonNull
            @Override
            public final Transaction<V> savepoint(@NonNls @NonNull final String name) {
                return new Next<>(mExecutor, this, new Checkpoint<V>(name));
            }
        }

        private static class Next<V, T extends V, U> implements Transaction<U>, Executable<U> {

            @NonNull
            private final Executor mExecutor;
            @NonNull
            private final Executable<T> mExecutable;
            @NonNull
            private final Action<V, ? extends U> mAction;

            private Next(@NonNull final Executor executor,
                         @NonNull final Executable<T> executable,
                         @NonNull final Action<V, ? extends U> action) {
                super();

                mExecutor = executor;
                mExecutable = executable;
                mAction = action;
            }

            @NonNull
            @Override
            public final <W> Transaction<W> andThen(@NonNull final Action<? super U, ? extends W> action) {
                return new Next<>(mExecutor, this, action);
            }

            @NonNull
            @Override
            public final Result<U> execute() {
                return mExecutor.execute(this);
            }

            @NonNull
            @Override
            public final Maybe<U> execute(@NonNull final DAO dao,
                                          @NonNull final SQLiteDatabase database) {
                final Maybe<V> result = Maybes.<V>safeCast(mExecutable.execute(dao, database));
                final Statement<? extends U> next = mAction.onResult(dao, result);
                return Maybes.safeCast(next.execute(database));
            }

            @NonNull
            @Override
            public final Transaction<U> savepoint(@NonNls @NonNull final String name) {
                return new Next<>(mExecutor, this, new Checkpoint<U>(name));
            }
        }

        private static class SingleAccess extends SomeAccess implements Access.Single {

            @NonNull
            private final Executor mExecutor;
            @NonNull
            private final Route.Item mRoute;
            @NonNull
            private final Object[] mArguments;

            private SingleAccess(@NonNull final Executor executor,
                                 @NonNull final Route.Item route,
                                 @NonNull final Object... arguments) {
                super(executor, route, arguments);

                mExecutor = executor;
                mRoute = route;
                mArguments = arguments;
            }

            @NonNull
            @Override
            public final <M> QueryBuilder<M> query(@NonNull final Value.Read<M> value) {
                return query(single(value));
            }

            @NonNull
            @Override
            public final <M> QueryBuilder<M> query(@NonNull final Mapper.Read<M> mapper) {
                return query(single(mapper));
            }

            @NonNull
            @Override
            public final <M> QueryBuilder<M> query(@NonNull final Reading.Single<M> reading) {
                return new QueryBuilder<>(mExecutor, new Executable.Query.Single<>(reading, mRoute, mArguments));
            }
        }

        private static class ManyAccess extends SomeAccess implements Access.Many {

            @NonNull
            private final Executor mExecutor;
            @NonNull
            private final Route.Dir mRoute;
            @NonNull
            private final Object[] mArguments;

            private ManyAccess(@NonNull final Executor executor,
                               @NonNull final Route.Dir route,
                               @NonNull final Object... arguments) {
                super(executor, route, arguments);

                mExecutor = executor;
                mRoute = route;
                mArguments = arguments;
            }

            @NonNull
            @Override
            public final <M> QueryBuilder<M> query(@NonNull final AggregateFunction<M> function) {
                return new QueryBuilder<>(mExecutor, new Executable.Query.Aggregate<>(function, mRoute, mArguments));
            }

            @NonNull
            @Override
            public final <M> QueryBuilder<List<M>> query(@NonNull final Value.Read<M> value) {
                return query(list(value));
            }

            @NonNull
            @Override
            public final <M> QueryBuilder<List<M>> query(@NonNull final Mapper.Read<M> mapper) {
                return query(list(mapper));
            }

            @NonNull
            @Override
            public final <M> QueryBuilder<M> query(@NonNull final Reading.Many<M> reading) {
                return new QueryBuilder<>(mExecutor, new Executable.Query.Many<>(reading, mRoute, mArguments));
            }
        }

        private static class SomeAccess implements Access.Some {

            @NonNull
            private final Executor mExecutor;
            @NonNull
            private final Executable.Access.Some mAccess;

            private SomeAccess(@NonNull final Executor executor,
                               @NonNull final Route route,
                               @NonNull final Object... arguments) {
                super();

                mExecutor = executor;
                mAccess = new Executable.Access.Some(route, arguments);
            }

            @NonNull
            @Override
            public final Transaction<Boolean> exists() {
                return transaction(mAccess.exists());
            }

            @NonNull
            @Override
            public final Transaction<Boolean> exists(@NonNull final Select.Where where) {
                return transaction(mAccess.exists(where));
            }

            @NonNull
            @Override
            public final <M extends Instance.Writable> Transaction<Uri> insert(@NonNull final M model) {
                return transaction(mAccess.insert(model));
            }

            @NonNull
            @Override
            public final <M> Transaction<Uri> insert(@NonNull final M model,
                                                     @NonNull final Value.Write<M> value) {
                return transaction(mAccess.insert(model, value));
            }

            @NonNull
            @Override
            public final <M> Transaction<Uri> insert(@NonNull final M model,
                                                     @NonNull final Mapper.Write<M> mapper) {
                return transaction(mAccess.insert(model, mapper));
            }

            @NonNull
            @Override
            public final <M extends Instance.Writable> Transaction<Integer> update(@NonNull final M model) {
                return transaction(mAccess.update(model));
            }

            @NonNull
            @Override
            public final <M extends Instance.Writable> Transaction<Integer> update(@NonNull final Select.Where where,
                                                                                   @NonNull final M model) {
                return transaction(mAccess.update(where, model));
            }

            @NonNull
            @Override
            public final <M> Transaction<Integer> update(@NonNull final M model,
                                                         @NonNull final Value.Write<M> value) {
                return transaction(mAccess.update(model, value));
            }

            @NonNull
            @Override
            public final <M> Transaction<Integer> update(@NonNull final Select.Where where,
                                                         @NonNull final M model,
                                                         @NonNull final Value.Write<M> value) {
                return transaction(mAccess.update(where, model, value));
            }

            @NonNull
            @Override
            public final <M> Transaction<Integer> update(@NonNull final M model,
                                                         @NonNull final Mapper.Write<M> mapper) {
                return transaction(mAccess.update(model, mapper));
            }

            @NonNull
            @Override
            public final <M> Transaction<Integer> update(@NonNull final Select.Where where,
                                                         @NonNull final M model,
                                                         @NonNull final Mapper.Write<M> mapper) {
                return transaction(mAccess.update(where, model, mapper));
            }

            @NonNull
            @Override
            public final Transaction<Integer> delete() {
                return transaction(mAccess.delete());
            }

            @NonNull
            @Override
            public final Transaction<Integer> delete(@NonNull final Select.Where where) {
                return transaction(mAccess.delete(where));
            }

            private <V> Transaction<V> transaction(@NonNull final Executable<V> executable) {
                return new First<>(mExecutor, executable);
            }
        }

        private static class QueryBuilder<V> implements Query.Builder<V> {

            @NonNull
            private final Executor mExecutor;
            @NonNull
            private final Executable.Query.Builder<V> mQuery;

            private QueryBuilder(@NonNull final Executor executor,
                                 @NonNull final Executable.Query.Builder<V> query) {
                super();

                mExecutor = executor;
                mQuery = query;
            }

            @NonNull
            @Override
            public final QueryBuilder<V> with(@Nullable final Select.Where where) {
                mQuery.with(where);
                return this;
            }

            @NonNull
            @Override
            public final QueryBuilder<V> with(@Nullable final Select.Order order) {
                mQuery.with(order);
                return this;
            }

            @NonNull
            @Override
            public final QueryBuilder<V> with(@Nullable final Select.Limit limit) {
                mQuery.with(limit);
                return this;
            }

            @NonNull
            @Override
            public final QueryBuilder<V> with(@Nullable final Select.Offset offset) {
                mQuery.with(offset);
                return this;
            }

            @NonNull
            @Override
            public final <T> Transaction<T> andThen(@NonNull final Action<? super V, ? extends T> action) {
                return new Next<>(mExecutor, mQuery.execute(), action);
            }

            @NonNull
            @Override
            public final Result<V> execute() {
                return mExecutor.execute(mQuery.execute());
            }
        }

        private static class Checkpoint<V> extends Action<V, V> {

            @NonNls
            @NonNull
            private final String mName;

            private Checkpoint(@NonNull final String name) {
                super();

                mName = name;
            }

            @NonNull
            @Override
            public final Statement<V> onResult(@NonNull final DAO dao,
                                               @NonNull final Maybe<V> result) {
                return new Statement<>(new Savepoint<>(mName, result));
            }
        }
    }
}
