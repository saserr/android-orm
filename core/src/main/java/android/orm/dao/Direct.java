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
import android.net.Uri;
import android.orm.DAO;
import android.orm.Route;
import android.orm.model.Mapper;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Statement;
import android.orm.sql.Table;
import android.orm.sql.Value;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import static android.orm.dao.local.Read.afterRead;
import static android.orm.model.Observer.afterCreate;
import static android.orm.model.Observer.afterUpdate;
import static android.orm.model.Observer.beforeRead;
import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;
import static android.orm.sql.statement.Select.select;

public class Direct implements DAO.Direct {

    @NonNull
    private final SQLiteDatabase mDatabase;
    @NonNull
    private final ContentResolver mResolver;

    public Direct(@NonNull final Context context, @NonNull final SQLiteDatabase database) {
        super();

        mDatabase = database;
        mResolver = context.getContentResolver();
    }

    @NonNull
    @Override
    public final DAO.Direct.Access.Single at(@NonNull final Route.Item route,
                                             @NonNull final Object... arguments) {
        return new SingleAccess(this, mResolver, route, arguments);
    }

    @NonNull
    @Override
    public final DAO.Direct.Access.Many at(@NonNull final Route.Dir route,
                                           @NonNull final Object... arguments) {
        return new ManyAccess(this, mResolver, route, arguments);
    }

    @NonNull
    @Override
    public final DAO.Direct.Access.Some at(@NonNull final Route route, @NonNull final Object... arguments) {
        return new SomeAccess(this, mResolver, route, arguments);
    }

    @Override
    public final void execute(@NonNull final Statement statement) {
        statement.execute(mDatabase);
    }

    @NonNull
    @Override
    public final <V> Maybe<V> execute(@NonNull final Function<SQLiteDatabase, Maybe<V>> function) {
        return function.invoke(mDatabase);
    }

    private static class SingleAccess extends SomeAccess implements DAO.Direct.Access.Single {

        @NonNull
        private final Direct mDAO;
        @NonNull
        private final Route.Item mRoute;
        @NonNull
        private final Object[] mArguments;

        private SingleAccess(@NonNull final Direct dao,
                             @NonNull final ContentResolver resolver,
                             @NonNull final Route.Item route,
                             @NonNull final Object... arguments) {
            super(dao, resolver, route, arguments);

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

    private static class ManyAccess extends SomeAccess implements DAO.Direct.Access.Many {

        @NonNull
        private final Direct mDAO;
        @NonNull
        private final Route.Dir mRoute;
        @NonNull
        private final Object[] mArguments;

        private ManyAccess(@NonNull final Direct dao,
                           @NonNull final ContentResolver resolver,
                           @NonNull final Route.Dir route,
                           @NonNull final Object... arguments) {
            super(dao, resolver, route, arguments);

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

    private static class SomeAccess extends DAO.Access.Write.Base<Maybe<Uri>, Maybe<Integer>, Maybe<Integer>> implements DAO.Direct.Access.Some {

        @NonNull
        private final Direct mDAO;
        @NonNull
        private final ContentResolver mResolver;
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

        private SomeAccess(@NonNull final Direct dao,
                           @NonNull final ContentResolver resolver,
                           @NonNull final Route route,
                           @NonNull final Object... arguments) {
            super();

            mDAO = dao;
            mResolver = resolver;
            mItemRoute = route.getItemRoute();
            mUri = route.createUri(arguments);
            mTable = route.getTable();
            mOnInsert = route.createValues(arguments);
            mWhere = route.getWhere(arguments);
        }

        @NonNull
        @Override
        public final Maybe<Boolean> exists() {
            return exists(Select.Where.None);
        }

        @NonNull
        @Override
        public final Maybe<Boolean> exists(@NonNull final Select.Where where) {
            return mDAO.execute(new android.orm.dao.local.Exists(mTable, mWhere.and(where)));
        }

        @NonNull
        @Override
        protected final <M> Maybe<Uri> insert(@NonNull final M model,
                                              @NonNull final Plan.Write plan) {
            final Maybe<Uri> result;

            if (plan.isEmpty()) {
                result = Maybes.nothing();
            } else {
                result = mDAO.execute(new android.orm.dao.local.Insert(mItemRoute, plan, mOnInsert));
                if (result.isSomething()) {
                    afterCreate(model);
                    final Uri uri = result.get();
                    if (uri != null) {
                        mResolver.notifyChange(uri, null);
                    }
                }
            }

            return result;
        }

        @NonNull
        @Override
        protected final <M> Maybe<Integer> update(@NonNull final Select.Where where,
                                                  @NonNull final M model,
                                                  @NonNull final Plan.Write plan) {
            final Maybe<Integer> result;

            if (plan.isEmpty()) {
                result = Maybes.nothing();
            } else {
                result = mDAO.execute(new android.orm.dao.local.Update(mTable, mWhere.and(where), plan));
                if (result.isSomething()) {
                    afterUpdate(model);
                    mResolver.notifyChange(mUri, null);
                }
            }

            return result;
        }

        @NonNull
        @Override
        public final Maybe<Integer> delete(@NonNull final Select.Where where) {
            final Maybe<Integer> result = mDAO.execute(new android.orm.dao.local.Delete(mTable, mWhere.and(where)));

            if (result.isSomething()) {
                mResolver.notifyChange(mUri, null);
            }

            return result;
        }
    }

    private static class QueryBuilder<V> implements DAO.Direct.Query.Builder.Refreshable<V> {

        @NonNull
        private final Direct mDAO;
        @NonNull
        private final Reading<V> mReading;
        @NonNull
        private final Select.Where mDefault;

        private final Function<Producer<Maybe<V>>, Maybe<V>> mAfterRead = afterRead();

        @NonNull
        private Select.Builder mSelect;
        @Nullable
        private V mValue;

        private QueryBuilder(@NonNull final Direct dao,
                             @NonNull final Reading<V> reading,
                             @NonNull final Route route,
                             @NonNull final Object... arguments) {
            super();

            mDAO = dao;
            mReading = reading;
            mDefault = route.getWhere(arguments);

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
        public final Maybe<V> execute() {
            beforeRead(mValue);
            final Plan.Read<V> plan = (mValue == null) ?
                    mReading.preparePlan() :
                    mReading.preparePlan(mValue);
            final Maybe<V> result;

            if (plan.isEmpty()) {
                result = (mValue == null) ? Maybes.<V>nothing() : Maybes.something(mValue);
            } else {
                result = mDAO.execute(new android.orm.dao.local.Read<>(plan, mSelect.build())).flatMap(mAfterRead);
            }

            return result;
        }
    }
}
