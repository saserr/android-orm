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

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.orm.DAO;
import android.orm.Database;
import android.orm.Route;
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
import android.orm.sql.Table;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.List;

import static android.orm.dao.direct.Read.afterRead;
import static android.orm.model.Observer.afterCreate;
import static android.orm.model.Observer.afterUpdate;
import static android.orm.model.Observer.beforeCreate;
import static android.orm.model.Observer.beforeRead;
import static android.orm.model.Observer.beforeUpdate;
import static android.orm.model.Plans.write;
import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;
import static android.orm.sql.Select.select;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public abstract class Direct implements Transaction.Direct {

    @NonNull
    private final Notifier mNotifier;

    protected Direct(@NonNull final Notifier notifier) {
        super();

        mNotifier = notifier;
    }

    @NonNull
    @Override
    public final DAO.Direct.Access.Single at(@NonNull final Route.Item route,
                                             @NonNull final Object... arguments) {
        return new SingleAccess(this, mNotifier, route, arguments);
    }

    @NonNull
    @Override
    public final DAO.Direct.Access.Many at(@NonNull final Route.Dir route,
                                           @NonNull final Object... arguments) {
        return new ManyAccess(this, mNotifier, route, arguments);
    }

    @NonNull
    @Override
    public final DAO.Direct.Access.Some at(@NonNull final Route route, @NonNull final Object... arguments) {
        return new SomeAccess(this, mNotifier, route, arguments);
    }

    private static class SingleAccess extends BaseAccess<Uri> implements DAO.Direct.Access.Single {

        @NonNull
        private final Direct mDAO;
        @NonNull
        private final Notifier mNotifier;
        @NonNull
        private final Route.Item mRoute;
        @NonNull
        private final Object[] mArguments;

        private SingleAccess(@NonNull final Direct dao,
                             @NonNull final Notifier notifier,
                             @NonNull final Route.Item route,
                             @NonNull final Object... arguments) {
            super(dao, notifier, route, arguments);

            mDAO = dao;
            mNotifier = notifier;
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

        @NonNull
        @Override
        public final <M extends Instance.Writable> Maybe<Uri> save(@NonNull final M model) {
            return save(model, write(model));
        }

        @NonNull
        @Override
        public final Maybe<Uri> save(@NonNull final Writer writer) {
            return save(null, write(writer));
        }

        @NonNull
        @Override
        public final <M> Maybe<Uri> save(@Nullable final M model,
                                         @NonNull final Value.Write<M> value) {
            return save(model, write(something(model), value));
        }

        @NonNull
        @Override
        public final <M> Maybe<Uri> save(@NonNull final M model,
                                         @NonNull final Mapper.Write<M> mapper) {
            return save(model, mapper.prepareWrite(something(model)));
        }

        @NonNull
        @Override
        protected final Maybe<Uri> update(@NonNull final Route.Item route,
                                          @NonNull final Table<?> table,
                                          @NonNull final Select.Where where,
                                          @NonNull final Plan.Write plan,
                                          @NonNull final ContentValues additional) {
            final Maybe<Uri> result = mDAO.execute(new android.orm.dao.direct.Update.Single(route, where, plan, additional));

            if (result.isSomething()) {
                final Uri uri = result.get();
                if (uri == null) {
                    notifyChange();
                } else {
                    mNotifier.notifyChange(uri);
                }
            }

            return result;
        }

        @NonNull
        protected final <M> Maybe<Uri> save(@Nullable final M model,
                                            @NonNull final Plan.Write plan) {
            final Maybe<Uri> result;

            if (plan.isEmpty()) {
                result = nothing();
            } else {
                final Boolean exists = exists().getOrElse(null);
                result = ((exists == null) || !exists) ?
                        insert(model, plan) :
                        update(model, plan);
            }

            return result;
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

    private static class SomeAccess extends BaseAccess<Integer> implements DAO.Direct.Access.Some {

        @NonNull
        private final Direct mDAO;

        private SomeAccess(@NonNull final Direct dao,
                           @NonNull final Notifier notifier,
                           @NonNull final Route route,
                           @NonNull final Object... arguments) {
            super(dao, notifier, route, arguments);

            mDAO = dao;
        }

        @NonNull
        @Override
        protected Maybe<Integer> update(@NonNull final Route.Item route,
                                        @NonNull final Table<?> table,
                                        @NonNull final Select.Where where,
                                        @NonNull final Plan.Write plan,
                                        @NonNull final ContentValues additional) {
            final Maybe<Integer> updated = mDAO.execute(new android.orm.dao.direct.Update.Many(table, where, plan));

            if (updated.getOrElse(null) != null) {
                notifyChange();
            }

            return updated;
        }
    }

    private abstract static class BaseAccess<U> extends DAO.Access.Write.Base<Maybe<Uri>, Maybe<U>, Maybe<Integer>> implements DAO.Direct.Exists {

        @NonNull
        private final Direct mDAO;
        @NonNull
        private final Notifier mNotifier;
        @NonNull
        private final Route.Item mItemRoute;
        @NonNull
        private final Table<?> mTable;
        @NonNull
        private final Uri mUri;
        @NonNull
        private final ContentValues mOnInsert;
        @NonNull
        private final Select.Where mWhere;

        private BaseAccess(@NonNull final Direct dao,
                           @NonNull final Notifier notifier,
                           @NonNull final Route route,
                           @NonNull final Object... arguments) {
            super();

            mDAO = dao;
            mNotifier = notifier;
            mItemRoute = route.getItemRoute();
            mTable = route.getTable();
            mUri = route.createUri(arguments);
            mOnInsert = route.createValues(arguments);
            mWhere = route.getWhere(arguments);
        }

        @NonNull
        protected abstract Maybe<U> update(@NonNull final Route.Item route,
                                           @NonNull final Table<?> table,
                                           @NonNull final Select.Where where,
                                           @NonNull final Plan.Write plan,
                                           @NonNull final ContentValues additional);

        @NonNull
        @Override
        public final Maybe<Boolean> exists() {
            return exists(Select.Where.None);
        }

        @NonNull
        @Override
        public final Maybe<Boolean> exists(@NonNull final Select.Where where) {
            return mDAO.execute(new android.orm.dao.direct.Exists(mTable, mWhere.and(where)));
        }

        @NonNull
        @Override
        protected final <M> Maybe<Uri> insert(@Nullable final M model,
                                              @NonNull final Plan.Write plan) {
            final Maybe<Uri> result;

            if (plan.isEmpty()) {
                result = nothing();
            } else {
                beforeCreate(model);
                result = mDAO.execute(new android.orm.dao.direct.Insert(mItemRoute, plan, mOnInsert));
                if (result.isSomething()) {
                    final Uri uri = result.get();
                    if (uri != null) {
                        afterCreate(model);
                        mNotifier.notifyChange(uri);
                    }
                }
            }

            return result;
        }

        @NonNull
        @Override
        protected final <M> Maybe<U> update(@NonNull final Select.Where where,
                                            @Nullable final M model,
                                            @NonNull final Plan.Write plan) {
            final Maybe<U> result;

            if (plan.isEmpty()) {
                result = nothing();
            } else {
                beforeUpdate(model);
                result = update(mItemRoute, mTable, mWhere.and(where), plan, mOnInsert);
                if (result.getOrElse(null) != null) {
                    afterUpdate(model);
                }
            }

            return result;
        }

        @NonNull
        @Override
        public final Maybe<Integer> delete(@NonNull final Select.Where where) {
            final Maybe<Integer> result = mDAO.execute(new android.orm.dao.direct.Delete(mTable, mWhere.and(where)));

            if (result.isSomething()) {
                final Integer deleted = result.get();
                if ((deleted != null) && (deleted > 0)) {
                    notifyChange();
                }
            }

            return result;
        }

        protected final void notifyChange() {
            mNotifier.notifyChange(mUri);
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
                result = (mValue == null) ? Maybes.<V>nothing() : something(mValue);
            } else {
                result = mDAO.execute(new android.orm.dao.direct.Read<>(plan, mSelect.build())).flatMap(mAfterRead);
            }

            return result;
        }
    }

    @NonNull
    public static Transaction.Direct create(@NonNull final SQLiteDatabase database,
                                            @NonNull final Notifier notifier) {
        return new Direct(notifier) {

            @NonNull
            @Override
            public Savepoint savepoint(@NonNls @NonNull final String name) {
                return new Savepoint(name) {
                    @Override
                    public void rollback() {
                        rollback(database);
                    }
                };
            }

            @Override
            public void execute(@NonNull final Statement statement) {
                statement.execute(database);
            }

            @NonNull
            @Override
            public <V> Maybe<V> execute(@NonNull final Expression<V> expression) {
                return expression.execute(database);
            }
        };
    }

    @NonNull
    public static DAO.Direct create(@NonNull final Context context,
                                    @NonNull final Database database) {
        return new Direct(new Notifier.Immediate(context.getContentResolver())) {

            private final SQLiteOpenHelper mHelper = database.getDatabaseHelper(context);

            @NonNull
            @Override
            public Savepoint savepoint(@NonNls @NonNull final String name) {
                throw new UnsupportedOperationException("savepoint");
            }

            @Override
            public void execute(@NonNull final Statement statement) {
                final SQLiteDatabase db = mHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    statement.execute(db);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

            @NonNull
            @Override
            public <V> Maybe<V> execute(@NonNull final Expression<V> expression) {
                Maybe<V> result = nothing();

                final SQLiteDatabase db = mHelper.getWritableDatabase();
                db.beginTransaction();
                try {
                    result = expression.execute(db);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                return result;
            }
        };
    }
}
