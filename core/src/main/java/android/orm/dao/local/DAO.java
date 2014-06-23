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

import android.content.ContentValues;
import android.net.Uri;
import android.orm.Route;
import android.orm.model.Mapper;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
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
import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;
import static android.orm.sql.statement.Select.select;
import static android.orm.util.Functions.compose;

public class DAO {

    @NonNull
    private final Notifier mNotifier;

    public DAO(@NonNull final Notifier notifier) {
        super();

        mNotifier = notifier;
    }

    @NonNull
    public final Access.Single at(@NonNull final Route.Item route,
                                  @NonNull final Object... arguments) {
        return new SingleAccess(mNotifier, route, arguments);
    }

    @NonNull
    public final Access.Many at(@NonNull final Route.Dir route,
                                @NonNull final Object... arguments) {
        return new ManyAccess(mNotifier, route, arguments);
    }

    @NonNull
    public final Access.Some at(@NonNull final Route route, @NonNull final Object... arguments) {
        return new SomeAccess(mNotifier, route, arguments);
    }

    public interface Exists extends android.orm.DAO.Access.Exists<Statement<Boolean>> {
    }

    public static final class Query {

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

        public interface Builder<V> extends android.orm.DAO.Access.Query.Builder<V, Statement<V>> {

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
        }

        private Query() {
            super();
        }
    }

    public static final class Read {

        public interface Single extends Exists, Query.Single {
        }

        public interface Many extends Exists, Query.Many {
        }

        private Read() {
            super();
        }
    }

    public interface Insert extends android.orm.DAO.Access.Insert<Statement<Uri>> {
    }

    public interface Update extends android.orm.DAO.Access.Update<Statement<Integer>> {
    }

    public interface Delete extends android.orm.DAO.Access.Delete<Statement<Integer>> {
    }

    public interface Write extends Insert, Update, Delete, android.orm.DAO.Access.Write<Statement<Uri>, Statement<Integer>, Statement<Integer>> {
        abstract class Base extends android.orm.DAO.Access.Write.Base<Statement<Uri>, Statement<Integer>, Statement<Integer>> implements Write {
            protected Base() {
                super();
            }
        }
    }

    public static final class Access {

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

    private static class SingleAccess extends SomeAccess implements Access.Single {

        @NonNull
        private final Route.Item mRoute;
        @NonNull
        private final Object[] mArguments;

        private SingleAccess(@NonNull final Notifier notifier,
                             @NonNull final Route.Item route,
                             @NonNull final Object... arguments) {
            super(notifier, route, arguments);

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
            return new QueryBuilder<>(reading, mRoute, mArguments);
        }
    }

    private static class ManyAccess extends SomeAccess implements Access.Many {

        @NonNull
        private final Route.Dir mRoute;
        @NonNull
        private final Object[] mArguments;


        private ManyAccess(@NonNull final Notifier notifier,
                           @NonNull final Route.Dir route,
                           @NonNull final Object... arguments) {
            super(notifier, route, arguments);

            mRoute = route;
            mArguments = arguments;
        }

        @NonNull
        @Override
        public final <M> QueryBuilder<M> query(@NonNull final AggregateFunction<M> function) {
            return new QueryBuilder<>(single(function), mRoute, mArguments);
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
            return new QueryBuilder<>(reading, mRoute, mArguments);
        }
    }

    private static class SomeAccess extends Write.Base implements Access.Some {

        @NonNull
        private final Route.Item mItemRoute;
        @NonNull
        private final Table<?> mTable;
        @NonNull
        private final Select.Where mWhere;
        @NonNull
        private final ContentValues mOnInsert;
        @NonNull
        private final Function<Maybe<Uri>, Maybe<Uri>> mInsertNotify;
        @NonNull
        private final Function<Maybe<Integer>, Maybe<Integer>> mUpdateNotify;

        private SomeAccess(@NonNull final Notifier notifier,
                           @NonNull final Route route,
                           @NonNull final Object... arguments) {
            super();

            mItemRoute = route.getItemRoute();
            mTable = route.getTable();
            mOnInsert = route.createValues(arguments);
            mWhere = route.getWhere(arguments);
            mInsertNotify = Maybes.map(new android.orm.dao.local.Insert.Notify(notifier));
            mUpdateNotify = Maybes.map(new android.orm.dao.local.Update.Notify(notifier, route.createUri(arguments)));
        }

        @NonNull
        @Override
        public final Statement<Boolean> exists() {
            return exists(Select.Where.None);
        }

        @NonNull
        @Override
        public final Statement<Boolean> exists(@NonNull final Select.Where where) {
            return new Statement<>(new android.orm.dao.local.Exists(mTable, mWhere.and(where)));
        }

        @NonNull
        @Override
        protected final <M> Statement<Uri> insert(@NonNull final M model,
                                                  @NonNull final Plan.Write plan) {
            final Statement<Uri> statement = new Statement<>(compose(new android.orm.dao.local.Insert(mItemRoute, plan, mOnInsert), mInsertNotify));
            afterCreate(model);
            return statement;
        }

        @NonNull
        @Override
        protected final <M> Statement<Integer> update(@NonNull final Select.Where where,
                                                      @NonNull final M model,
                                                      @NonNull final Plan.Write plan) {
            final Statement<Integer> statement = new Statement<>(compose(new android.orm.dao.local.Update(mTable, mWhere.and(where), plan), mUpdateNotify));
            afterUpdate(model);
            return statement;
        }

        @NonNull
        @Override
        public final Statement<Integer> delete(@NonNull final Select.Where where) {
            return new Statement<>(compose(new android.orm.dao.local.Delete(mTable, mWhere.and(where)), mUpdateNotify));
        }
    }

    private static class QueryBuilder<V> implements Query.Builder<V> {

        @NonNull
        private final Select.Where mDefault;
        @NonNull
        private final Plan.Read<V> mPlan;

        private final Function<Producer<Maybe<V>>, Maybe<V>> mAfterRead = afterRead();

        @NonNull
        private Select.Builder mSelect;

        private QueryBuilder(@NonNull final Reading<V> reading,
                             @NonNull final Route route,
                             @NonNull final Object... arguments) {
            super();

            mDefault = route.getWhere(arguments);
            mPlan = reading.preparePlan();

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
        public final Statement<V> execute() {
            return new Statement<>(new android.orm.dao.local.Read<>(mPlan, mSelect.build())).flatMap(mAfterRead);
        }
    }
}
