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

package android.orm.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.orm.DAO;
import android.orm.Database;
import android.orm.Model;
import android.orm.Route;
import android.orm.dao.async.Observer;
import android.orm.dao.direct.Notifier;
import android.orm.dao.local.Watch;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Plan;
import android.orm.model.Plans;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Expression;
import android.orm.sql.Select;
import android.orm.sql.Table;
import android.orm.sql.Value;
import android.orm.util.Cancelable;
import android.orm.util.Function;
import android.orm.util.Functions;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static android.orm.dao.direct.Read.afterRead;
import static android.orm.model.Observer.beforeCreate;
import static android.orm.model.Observer.beforeRead;
import static android.orm.model.Observer.beforeUpdate;
import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;

public class Local extends Async implements DAO.Local {

    @NonNull
    private final DAO.Direct mDirectDAO;
    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final Handler mHandler;
    @NonNull
    private final Notifier mNotifier;

    public Local(@NonNull final Context context,
                 @NonNull final Database database,
                 @NonNull final ExecutorService executor) {
        super(executor);

        mDirectDAO = android.orm.dao.Direct.create(context, database);
        mResolver = context.getContentResolver();
        mHandler = new Handler();
        mNotifier = new Notifier.Immediate(mResolver);
    }

    @NonNull
    @Override
    public final DAO.Async.Access.Single at(@NonNull final Route.Item route,
                                            @NonNull final Object... arguments) {
        return new SingleAccess(this, mNotifier, route, arguments);
    }

    @NonNull
    @Override
    public final DAO.Local.Access.Many at(@NonNull final Route.Dir route,
                                          @NonNull final Object... arguments) {
        return new ManyAccess(this, mNotifier, route, arguments);
    }

    @NonNull
    @Override
    public final DAO.Async.Access.Some at(@NonNull final Route route, @NonNull final Object... arguments) {
        return new SomeAccess(this, mNotifier, route, arguments);
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Expression<V> expression) {
        return execute(new Task<>(mDirectDAO, expression));
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Transaction.Direct<V> transaction) {
        return execute(new Task<>(mDirectDAO, transaction));
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
                                 @NonNull final Plan.Read<V> plan,
                                 @NonNull final Select select,
                                 @NonNull final Result.Callback<? super V> callback) {
        return register(new Observer(
                manager,
                mResolver,
                uri,
                new Watch<>(mDirectDAO, mHandler, reading, plan, select, callback)
        ));
    }

    private static class SingleAccess extends SomeAccess implements DAO.Async.Access.Single {

        @NonNull
        private final Select mSelect;

        private SingleAccess(@NonNull final Local dao,
                             @NonNull final Notifier notifier,
                             @NonNull final Route.Item route,
                             @NonNull final Object... arguments) {
            super(dao, notifier, route, arguments);

            mSelect = select().with(Select.Limit.Single).build();
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
            final Plan.Read<M> plan = Plans.single(model.getName(), Reading.Item.Update.from(model));
            return read(model, plan, mSelect);
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
            return query(reading, Select.Limit.Single);
        }
    }

    private static class ManyAccess extends SomeAccess implements DAO.Local.Access.Many {


        private ManyAccess(@NonNull final Local dao,
                           @NonNull final Notifier notifier,
                           @NonNull final Route.Dir route,
                           @NonNull final Object... arguments) {
            super(dao, notifier, route, arguments);
        }

        @NonNull
        @Override
        public final <M> QueryBuilder<M> query(@NonNull final AggregateFunction<M> function) {
            return query(single(function), null);
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
            return query(reading, null);
        }
    }

    private static class SomeAccess extends DAO.Access.Write.Base<Result<Uri>, Result<Integer>, Result<Integer>> implements DAO.Async.Access.Some {

        @NonNull
        private final Local mDAO;
        @NonNull
        private final Route.Manager mRouteManager;
        @NonNull
        private final Route.Item mItemRoute;
        @NonNull
        private final Uri mUri;
        @NonNull
        private final Table<?> mTable;
        @NonNull
        private final ContentValues mOnInsert;
        @NonNull
        private final Select.Where mWhere;
        @NonNull
        private final Function<Uri, Uri> mInsertNotify;
        @NonNull
        private final Function<Integer, Integer> mChangeNotify;

        private SomeAccess(@NonNull final Local dao,
                           @NonNull final Notifier notifier,
                           @NonNull final Route route,
                           @NonNull final Object... arguments) {
            super();

            mDAO = dao;
            mRouteManager = route.getManager();
            mItemRoute = route.getItemRoute();
            mUri = route.createUri(arguments);
            mTable = route.getTable();
            mOnInsert = route.createValues(arguments);
            mWhere = route.getWhere(arguments);

            mInsertNotify = new Function<Uri, Uri>() {
                @NonNull
                @Override
                public Uri invoke(@NonNull final Uri uri) {
                    notifier.notifyChange(uri);
                    return uri;
                }
            };

            mChangeNotify = new Function<Integer, Integer>() {
                @NonNull
                @Override
                public Integer invoke(@NonNull final Integer changed) {
                    if (changed > 0) {
                        notifier.notifyChange(mUri);
                    }
                    return changed;
                }
            };
        }

        @NonNull
        @Override
        public final Result<Boolean> exists() {
            return exists(Select.Where.None);
        }

        @NonNull
        @Override
        public final Result<Boolean> exists(@NonNull final Select.Where where) {
            return mDAO.execute(new android.orm.dao.direct.Exists(mTable, mWhere.and(where)));
        }

        @NonNull
        @Override
        protected final <M> Result<Uri> insert(@Nullable final M model,
                                               @NonNull final Plan.Write plan) {
            beforeCreate(model);
            return afterCreate(
                    plan.isEmpty() ?
                            Result.<Uri>nothing() :
                            mDAO.execute(new android.orm.dao.direct.Insert(mItemRoute, plan, mOnInsert)).map(mInsertNotify),
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
                    plan.isEmpty() ?
                            Result.<Integer>nothing() :
                            mDAO.execute(new android.orm.dao.direct.Update.Many(mTable, mWhere.and(where), plan)).map(mChangeNotify),
                    model
            );
        }

        @NonNull
        @Override
        public final Result<Integer> delete(@NonNull final Select.Where where) {
            return mDAO.execute(new android.orm.dao.direct.Delete(mTable, mWhere.and(where))).map(mChangeNotify);
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
        protected final Select.Builder select() {
            return Select.select(mTable).with(mWhere);
        }

        @NonNull
        protected final <V> QueryBuilder<V> query(@NonNull final Reading<V> reading,
                                                  @Nullable final Select.Limit limit) {
            return new QueryBuilder<>(this, reading, mWhere, limit);
        }

        @NonNull
        protected final <V> Result<V> read(@Nullable final V model,
                                           @NonNull final Plan.Read<V> plan,
                                           @NonNull final Select select) {
            final Result<V> result;

            if (plan.isEmpty()) {
                result = (model == null) ? Result.<V>nothing() : Result.something(model);
            } else {
                final Function<Producer<Maybe<V>>, Maybe<V>> afterRead = afterRead();
                result = mDAO.execute(new android.orm.dao.direct.Read<>(plan, select)).flatMap(afterRead);
            }

            return result;
        }

        @NonNull
        protected final <V> Cancelable watch(@NonNull final Reading<V> reading,
                                             @NonNull final Plan.Read<V> plan,
                                             @NonNull final Select select,
                                             @NonNull final Result.Callback<? super V> callback) {
            return mDAO.watch(mRouteManager, mUri, reading, plan, select, callback);
        }

        @NonNull
        protected final Cancelable notify(@NonNull final Runnable runnable) {
            return mDAO.notify(mRouteManager, mUri, runnable);
        }
    }

    private static class QueryBuilder<V> implements DAO.Local.Query.Builder.Many.Refreshable<V> {

        @NonNull
        private final SomeAccess mAccess;
        @NonNull
        private final Reading<V> mReading;
        @NonNull
        private final Select.Where mDefault;

        @NonNull
        private Select.Builder mSelect;
        @Nullable
        private V mValue;

        private QueryBuilder(@NonNull final SomeAccess access,
                             @NonNull final Reading<V> reading,
                             @NonNull final Select.Where where,
                             @Nullable final Select.Limit limit) {
            super();

            mAccess = access;
            mReading = reading;
            mDefault = where;

            mSelect = mAccess.select().with(limit);
        }

        @NonNull
        @Override
        public final QueryBuilder<V> with(@Nullable final Select.Where where) {
            mSelect = mSelect.with((where == null) ? mDefault : mDefault.and(where));
            return this;
        }

        @NonNull
        @Override
        public final QueryBuilder<V> with(@Nullable final Select.Order order) {
            mSelect = mSelect.with(order);
            return this;
        }

        @NonNull
        @Override
        public final QueryBuilder<V> with(@Nullable final Select.Limit limit) {
            mSelect = mSelect.with(limit);
            return this;
        }

        @NonNull
        @Override
        public final QueryBuilder<V> with(@Nullable final Select.Offset offset) {
            mSelect = mSelect.with(offset);
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
            return mAccess.read(mValue, plan, mSelect.build());
        }

        @NonNull
        @Override
        public final Cancelable onChange(@NonNull final Result.Callback<? super V> callback) {
            beforeRead(mValue);
            final Plan.Read<V> plan = (mValue == null) ?
                    mReading.preparePlan() :
                    mReading.preparePlan(mValue);
            if (plan.isEmpty()) {
                throw new IllegalArgumentException("Nothing will be watched");
            }

            return mAccess.watch(mReading, plan, mSelect.build(), callback);
        }

        @Override
        public final Cancelable onChange(@NonNull final Runnable runnable) {
            return mAccess.notify(runnable);
        }
    }

    private static class Task<V> implements Producer<Maybe<V>> {

        @NonNull
        private final DAO.Direct mDAO;
        @NonNull
        private final Transaction.Direct<V> mTransaction;

        private Task(@NonNull final DAO.Direct dao,
                     @NonNull final Expression<V> expression) {
            this(dao, new Transaction.Direct<V>() {
                @NonNull
                @Override
                public Maybe<V> run(@NonNull final DAO.Direct dao) {
                    return dao.execute(expression);
                }
            });
        }

        private Task(@NonNull final DAO.Direct dao,
                     @NonNull final Transaction.Direct<V> transaction) {
            super();

            mDAO = dao;
            mTransaction = transaction;
        }

        @NonNull
        @Override
        public final Maybe<V> produce() {
            return mDAO.execute(mTransaction);
        }
    }
}
