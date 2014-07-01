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

package android.orm.dao;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.orm.DAO;
import android.orm.Model;
import android.orm.Route;
import android.orm.dao.async.Observer;
import android.orm.dao.remote.Apply;
import android.orm.dao.remote.Watch;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.util.Cancelable;
import android.orm.util.Function;
import android.orm.util.Functions;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static android.orm.dao.direct.Read.afterRead;
import static android.orm.model.Observer.beforeCreate;
import static android.orm.model.Observer.beforeRead;
import static android.orm.model.Observer.beforeUpdate;
import static android.orm.model.Plans.single;
import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;

public class Remote extends Async implements DAO.Remote {

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final Handler mHandler;

    @NonNull
    private final android.orm.dao.remote.Transaction.Executor mTransactionExecutor;

    public Remote(@NonNull final Context context, @NonNull final ExecutorService executor) {
        super(executor);

        mResolver = context.getContentResolver();
        mHandler = new Handler();

        final Apply apply = new Apply(mResolver);
        mTransactionExecutor = new android.orm.dao.remote.Transaction.Executor() {
            @NonNull
            @Override
            public Result<Transaction.Remote.CommitResult> execute(@NonNull final String authority,
                                                                   @NonNull final Collection<Producer<ContentProviderOperation>> batch) {
                return android.orm.dao.Remote.this.execute(Pair.create(authority, batch), apply);
            }
        };
    }

    @NonNull
    @Override
    public final DAO.Remote.Access.Single at(@NonNull final Route.Item route,
                                             @NonNull final Object... arguments) {
        return new SingleAccess(this, mResolver, route, arguments);
    }

    @NonNull
    @Override
    public final DAO.Remote.Access.Many at(@NonNull final Route.Dir route,
                                           @NonNull final Object... arguments) {
        return new ManyAccess(this, mResolver, route, arguments);
    }

    @NonNull
    @Override
    public final DAO.Remote.Access.Some at(@NonNull final Route route, @NonNull final Object... arguments) {
        return new SomeAccess(this, mResolver, route, arguments);
    }

    @NonNull
    @Override
    public final Transaction.Remote transaction() {
        return new android.orm.dao.remote.Transaction(mTransactionExecutor);
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Function<ContentResolver, Maybe<V>> function) {
        return execute(mResolver, function);
    }

    @NonNull
    private Cancelable notify(@NonNull final Route.Manager manager,
                              @NonNull final Uri uri,
                              @NonNull final Runnable runnable) {
        return register(new Observer(manager, mResolver, uri, new Runnable() {
            @Override
            public void run() {
                mHandler.post(runnable);
            }
        }));
    }

    @NonNull
    private <V> Cancelable watch(@NonNull final Route.Manager manager,
                                 @NonNull final Uri uri,
                                 @NonNull final Reading<V> reading,
                                 @NonNull final android.orm.dao.remote.Read.Arguments<V> arguments,
                                 @NonNull final Result.Callback<? super V> callback) {
        return register(new Observer(
                manager,
                mResolver,
                uri,
                new Watch<>(
                        mHandler,
                        reading,
                        new android.orm.dao.remote.Read<V>(mResolver, uri),
                        arguments,
                        callback
                )
        ));
    }

    @NonNull
    private <V, T> Result<T> execute(@NonNull final V value,
                                     @NonNull final Function<V, Maybe<T>> function) {
        return execute(new Task<>(value, function));
    }

    private static class SingleAccess extends SomeAccess implements DAO.Remote.Access.Single {

        private SingleAccess(@NonNull final Remote dao,
                             @NonNull final ContentResolver resolver,
                             @NonNull final Route.Item route,
                             @NonNull final Object... arguments) {
            super(dao, resolver, route, arguments);
        }

        @NonNull
        @Override
        public final <M extends Model> Result<M> query(@NonNull final M model) {
            return query(Model.toInstance(model)).map(Functions.constant(model));
        }

        @NonNull
        @Override
        public final <M extends Instance.Readable> Result<M> query(@NonNull final M model) {
            beforeRead(model);
            final Plan.Read<M> plan = single(model.getName(), Reading.Item.Update.from(model));
            return read(model, plan);
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
            return super.query(reading);
        }
    }

    private static class ManyAccess extends SomeAccess implements DAO.Remote.Access.Many {

        private ManyAccess(@NonNull final Remote dao,
                           @NonNull final ContentResolver resolver,
                           @NonNull final Route.Dir route,
                           @NonNull final Object... arguments) {
            super(dao, resolver, route, arguments);
        }

        @NonNull
        @Override
        public final <M> QueryBuilder<M> query(@NonNull final AggregateFunction<M> function) {
            return query(single(function));
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
            return super.query(reading);
        }
    }

    private static class SomeAccess extends DAO.Access.Write.Base<Result<Uri>, Result<Integer>, Result<Integer>> implements DAO.Remote.Access.Some {

        @NonNull
        private final Remote mDAO;
        @NonNull
        private final Route.Manager mRouteManager;
        @NonNull
        private final Uri mUri;
        @NonNull
        private final android.orm.dao.remote.Exists mExists;
        @NonNull
        private final android.orm.dao.remote.Read<Object> mRead;
        @NonNull
        private final Function<Writer, Maybe<Uri>> mInsert;
        @NonNull
        private final android.orm.dao.remote.Update mUpdate;
        @NonNull
        private final android.orm.dao.remote.Delete mDelete;

        private SomeAccess(@NonNull final Remote dao,
                           @NonNull final ContentResolver resolver,
                           @NonNull final Route route,
                           @NonNull final Object... arguments) {
            super();

            mDAO = dao;
            mRouteManager = route.getManager();
            mUri = route.createUri(arguments);

            mExists = new android.orm.dao.remote.Exists(resolver, mUri);
            mRead = new android.orm.dao.remote.Read<>(resolver, mUri);
            mInsert = new android.orm.dao.remote.Insert(resolver, mUri);
            mUpdate = new android.orm.dao.remote.Update(resolver, mUri);
            mDelete = new android.orm.dao.remote.Delete(resolver, mUri);
        }

        @NonNull
        @Override
        public final Result<Boolean> exists() {
            return exists(Select.Where.None);
        }

        @NonNull
        @Override
        public final Result<Boolean> exists(@NonNull final Select.Where where) {
            return mDAO.execute(where, mExists);
        }

        @NonNull
        @Override
        protected final <M> Result<Uri> insert(@Nullable final M model,
                                               @NonNull final Plan.Write plan) {
            beforeCreate(model);
            return afterCreate(
                    plan.isEmpty() ? Result.<Uri>nothing() : mDAO.execute(plan, mInsert),
                    model
            );
        }

        @NonNull
        @Override
        protected final <M> Result<Integer> update(@NonNull final Select.Where where,
                                                   @Nullable final M model,
                                                   @NonNull final Plan.Write plan) {
            beforeUpdate(model);
            return afterUpdate(
                    plan.isEmpty() ? Result.<Integer>nothing() : mDAO.execute(Pair.<Writer, Select.Where>create(plan, where), mUpdate),
                    model
            );
        }

        @NonNull
        @Override
        public final Result<Integer> delete(@NonNull final Select.Where where) {
            return mDAO.execute(where, mDelete);
        }

        @NonNull
        @Override
        public final Cancelable onChange(@NonNull final Runnable runnable) {
            return mDAO.notify(mRouteManager, mUri, runnable);
        }

        @NonNull
        private static <M, V> Result<V> afterCreate(@NonNull final Result<V> result,
                                                    @Nullable final M model) {
            return (model instanceof android.orm.model.Observer.Write) ?
                    result.map(new Function<V, V>() {
                        @NonNull
                        @Override
                        public V invoke(@NonNull final V value) {
                            android.orm.model.Observer.afterCreate(model);
                            return value;
                        }
                    }) :
                    result;
        }

        @NonNull
        private static <M, V> Result<V> afterUpdate(@NonNull final Result<V> result,
                                                    @Nullable final M model) {
            return (model instanceof android.orm.model.Observer.Write) ?
                    result.map(new Function<V, V>() {
                        @NonNull
                        @Override
                        public V invoke(@NonNull final V value) {
                            android.orm.model.Observer.afterUpdate(model);
                            return value;
                        }
                    }) :
                    result;
        }

        @NonNull
        protected final <V> QueryBuilder<V> query(@NonNull final Reading<V> reading) {
            return new QueryBuilder<>(this, reading);
        }

        @NonNull
        @SuppressWarnings("unchecked")
        protected final <V> Result<V> read(@Nullable final V model, @NonNull final Plan.Read<V> plan) {
            final Result<V> result;

            if (plan.isEmpty()) {
                result = Result.something(model);
            } else {
                final android.orm.dao.remote.Read<V> read = (android.orm.dao.remote.Read<V>) mRead;
                final android.orm.dao.remote.Read.Arguments<V> arguments = new android.orm.dao.remote.Read.Arguments<>(plan, Select.Where.None, null);
                final Function<Producer<Maybe<V>>, Maybe<V>> afterRead = afterRead();
                result = mDAO.execute(arguments, read).flatMap(afterRead);
            }

            return result;
        }

        @NonNull
        protected final <V> Cancelable watch(@NonNull final Reading<V> reading,
                                             @NonNull final android.orm.dao.remote.Read.Arguments<V> arguments,
                                             @NonNull final Result.Callback<? super V> callback) {
            return mDAO.watch(mRouteManager, mUri, reading, arguments, callback);
        }

        @NonNull
        protected final Cancelable notify(@NonNull final Runnable runnable) {
            return mDAO.notify(mRouteManager, mUri, runnable);
        }
    }

    private static class QueryBuilder<V> implements DAO.Remote.Query.Builder.Many.Refreshable<V> {

        @NonNull
        private final SomeAccess mAccess;
        @NonNull
        private final Reading<V> mReading;

        @NonNull
        private Select.Where mWhere = Select.Where.None;
        @Nullable
        private Select.Order mOrder;
        @Nullable
        private V mValue;

        private QueryBuilder(@NonNull final SomeAccess access, @NonNull final Reading<V> reading) {
            super();

            mAccess = access;
            mReading = reading;
        }

        @NonNull
        @Override
        public final QueryBuilder<V> with(@Nullable final Select.Where where) {
            mWhere = (where == null) ? Select.Where.None : where;
            return this;
        }

        @NonNull
        @Override
        public final QueryBuilder<V> with(@Nullable final Select.Order order) {
            mOrder = order;
            return this;
        }

        @NonNull
        @Override
        public final QueryBuilder<V> using(@Nullable final V value) {
            mValue = value;
            return this;
        }

        @NonNull
        @Override
        public final Result<V> execute() {
            beforeRead(mValue);
            final Plan.Read<V> plan = (mValue == null) ?
                    mReading.preparePlan() :
                    mReading.preparePlan(mValue);
            return mAccess.read(mValue, plan);
        }

        @NonNull
        @Override
        public final Cancelable onChange(@NonNull final Result.Callback<? super V> callback) {
            beforeRead(mValue);
            final Plan.Read<V> plan = (mValue == null) ?
                    mReading.preparePlan() :
                    mReading.preparePlan(mValue);
            final android.orm.dao.remote.Read.Arguments<V> arguments = new android.orm.dao.remote.Read.Arguments<>(plan, mWhere, mOrder);
            return mAccess.watch(mReading, arguments, callback);
        }

        @Override
        public final Cancelable onChange(@NonNull final Runnable runnable) {
            return mAccess.notify(runnable);
        }
    }

    private static class Task<V, T> implements Producer<Maybe<T>> {

        @NonNull
        private final V mValue;
        @NonNull
        private final Function<V, Maybe<T>> mFunction;


        private Task(@NonNull final V value, @NonNull final Function<V, Maybe<T>> function) {
            super();

            mFunction = function;
            mValue = value;
        }

        @NonNull
        @Override
        public final Maybe<T> produce() {
            return mFunction.invoke(mValue);
        }
    }
}
