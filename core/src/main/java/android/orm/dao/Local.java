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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.orm.DAO;
import android.orm.Database;
import android.orm.Route;
import android.orm.dao.direct.Notifier;
import android.orm.dao.local.Transaction;
import android.orm.dao.local.Watch;
import android.orm.model.Mapper;
import android.orm.model.Observer;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Expression;
import android.orm.sql.Select;
import android.orm.sql.Table;
import android.orm.sql.Value;
import android.orm.util.Cancelable;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static android.orm.dao.direct.Read.afterRead;
import static android.orm.model.Observer.beforeRead;
import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;
import static android.orm.sql.Select.select;

public class Local extends Async implements DAO.Local {

    @NonNull
    private final SQLiteOpenHelper mHelper;
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

        mHelper = database.getDatabaseHelper(context);
        mResolver = context.getContentResolver();
        mHandler = new Handler();
        mNotifier = new Notifier.Immediate(mResolver);
    }

    @NonNull
    @Override
    public final DAO.Local.Access.Single at(@NonNull final Route.Item route,
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
    public final DAO.Local.Access.Some at(@NonNull final Route route, @NonNull final Object... arguments) {
        return new SomeAccess(this, mNotifier, route, arguments);
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Expression<V> expression) {
        return execute(new Task<>(mResolver, mHelper, expression));
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Transaction<V> transaction) {
        return execute(new Task<>(mResolver, mHelper, transaction));
    }

    @NonNull
    private <V> Cancelable watch(@NonNull final Route.Manager manager,
                                 @NonNull final Uri uri,
                                 @NonNull final Reading<V> reading,
                                 @NonNull final Plan.Read<V> plan,
                                 @NonNull final Select select,
                                 @NonNull final Result.Callback<? super V> callback) {
        return watch(new Watch<>(
                mHelper,
                manager,
                mHandler,
                mResolver,
                uri,
                reading,
                plan,
                select,
                callback
        ));
    }

    private static class SingleAccess extends SomeAccess implements DAO.Local.Access.Single {

        @NonNull
        private final android.orm.dao.Local mDAO;
        @NonNull
        private final Route.Item mRoute;
        @NonNull
        private final Object[] mArguments;

        private SingleAccess(@NonNull final android.orm.dao.Local dao,
                             @NonNull final Notifier notifier,
                             @NonNull final Route.Item route,
                             @NonNull final Object... arguments) {
            super(dao, notifier, route, arguments);

            mDAO = dao;
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
            return new QueryBuilder<>(mDAO, reading, mRoute, mArguments);
        }
    }

    private static class ManyAccess extends SomeAccess implements DAO.Local.Access.Many {

        @NonNull
        private final android.orm.dao.Local mDAO;
        @NonNull
        private final Route.Dir mRoute;
        @NonNull
        private final Object[] mArguments;

        private ManyAccess(@NonNull final android.orm.dao.Local dao,
                           @NonNull final Notifier notifier,
                           @NonNull final Route.Dir route,
                           @NonNull final Object... arguments) {
            super(dao, notifier, route, arguments);

            mDAO = dao;
            mRoute = route;
            mArguments = arguments;
        }

        @NonNull
        @Override
        public final <M> QueryBuilder<M> query(@NonNull final AggregateFunction<M> function) {
            return new QueryBuilder<>(mDAO, single(function), mRoute, mArguments);
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
            return new QueryBuilder<>(mDAO, reading, mRoute, mArguments);
        }
    }

    private static class SomeAccess extends DAO.Access.Write.Base<Result<Uri>, Result<Integer>, Result<Integer>> implements DAO.Local.Access.Some {

        @NonNull
        private final android.orm.dao.Local mDAO;
        @NonNull
        private final Route.Item mItemRoute;
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

        private SomeAccess(@NonNull final android.orm.dao.Local dao,
                           @NonNull final Notifier notifier,
                           @NonNull final Route route,
                           @NonNull final Object... arguments) {
            super();

            mDAO = dao;
            mItemRoute = route.getItemRoute();
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

                private final Uri mUri = route.createUri(arguments);

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
        protected final <M> Result<Uri> insert(@NonNull final M model,
                                               @NonNull final Plan.Write plan) {
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
                                                   @NonNull final M model,
                                                   @NonNull final Plan.Write plan) {
            return afterUpdate(
                    plan.isEmpty() ?
                            Result.<Integer>nothing() :
                            mDAO.execute(new android.orm.dao.direct.Update(mTable, mWhere.and(where), plan)).map(mChangeNotify),
                    model
            );
        }

        @NonNull
        @Override
        public final Result<Integer> delete(@NonNull final Select.Where where) {
            return mDAO.execute(new android.orm.dao.direct.Delete(mTable, mWhere.and(where))).map(mChangeNotify);
        }

        @NonNull
        private static <M, V> Result<V> afterCreate(@NonNull final Result<V> result,
                                                    @NonNull final M model) {
            return (model instanceof Observer.Write) ?
                    result.map(new Function<V, V>() {
                        @NonNull
                        @Override
                        public V invoke(@NonNull final V value) {
                            Observer.afterCreate(model);
                            return value;
                        }
                    }) :
                    result;
        }

        @NonNull
        private static <M, V> Result<V> afterUpdate(@NonNull final Result<V> result,
                                                    @NonNull final M model) {
            return (model instanceof Observer.Write) ?
                    result.map(new Function<V, V>() {
                        @NonNull
                        @Override
                        public V invoke(@NonNull final V value) {
                            Observer.afterUpdate(model);
                            return value;
                        }
                    }) :
                    result;
        }

    }

    private static class QueryBuilder<V> implements DAO.Local.Query.Builder.Refreshable<V> {

        @NonNull
        private final android.orm.dao.Local mDAO;
        @NonNull
        private final Reading<V> mReading;
        @NonNull
        private final Select.Where mDefault;
        @NonNull
        private final Route.Manager mRouteManager;
        @NonNull
        private final Uri mURI;

        private final Function<Producer<Maybe<V>>, Maybe<V>> mAfterRead = afterRead();

        @NonNull
        private Select.Builder mSelect;
        @Nullable
        private V mValue;

        private QueryBuilder(@NonNull final android.orm.dao.Local dao,
                             @NonNull final Reading<V> reading,
                             @NonNull final Route route,
                             @NonNull final Object... arguments) {
            super();

            mDAO = dao;
            mReading = reading;
            mDefault = route.getWhere(arguments);
            mRouteManager = route.getManager();
            mURI = route.createUri(arguments);

            mSelect = select(route.getTable());
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
            final Result<V> result;

            if (plan.isEmpty()) {
                result = (mValue == null) ? Result.<V>nothing() : Result.something(mValue);
            } else {
                result = mDAO.execute(new android.orm.dao.direct.Read<>(plan, mSelect.build())).flatMap(mAfterRead);
            }

            return result;
        }

        @NonNull
        @Override
        public final Cancelable watch(@NonNull final Result.Callback<? super V> callback) {
            beforeRead(mValue);
            final Plan.Read<V> plan = (mValue == null) ?
                    mReading.preparePlan() :
                    mReading.preparePlan(mValue);
            if (plan.isEmpty()) {
                throw new IllegalArgumentException("Nothing will be watched");
            }

            return mDAO.watch(mRouteManager, mURI, mReading, plan, mSelect.build(), callback);
        }
    }

    private static class Task<V> implements Producer<Maybe<V>> {

        @NonNull
        private final ContentResolver mResolver;
        @NonNull
        private final SQLiteOpenHelper mHelper;
        @NonNull
        private final Transaction<V> mTransaction;

        private Task(@NonNull final ContentResolver resolver,
                     @NonNull final SQLiteOpenHelper helper,
                     @NonNull final Expression<V> expression) {
            this(resolver, helper, new Transaction<V>() {
                @NonNull
                @Override
                protected Maybe<V> run(@NonNull final DAO.Direct dao) {
                    return dao.execute(expression);
                }
            });
        }

        private Task(@NonNull final ContentResolver resolver,
                     @NonNull final SQLiteOpenHelper helper,
                     @NonNull final Transaction<V> transaction) {
            super();

            mResolver = resolver;
            mHelper = helper;
            mTransaction = transaction;
        }

        @NonNull
        @Override
        public final Maybe<V> produce() {
            final Maybe<V> result;

            final SQLiteDatabase database = mHelper.getWritableDatabase();
            database.beginTransaction();
            try {
                result = mTransaction.execute(mResolver, database);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }

            return result;
        }
    }
}
