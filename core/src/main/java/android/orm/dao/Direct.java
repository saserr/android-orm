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
import android.orm.Model;
import android.orm.Route;
import android.orm.dao.direct.Notifier;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Expression;
import android.orm.sql.Helper;
import android.orm.sql.Select;
import android.orm.sql.Statement;
import android.orm.sql.Table;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.util.Function;
import android.orm.util.Functions;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.UUID;

import static android.orm.dao.direct.Read.afterRead;
import static android.orm.model.Observer.afterCreate;
import static android.orm.model.Observer.afterUpdate;
import static android.orm.model.Observer.beforeCreate;
import static android.orm.model.Observer.beforeRead;
import static android.orm.model.Observer.beforeUpdate;
import static android.orm.model.Plans.single;
import static android.orm.model.Plans.write;
import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;
import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Value.Write.Operation.Update;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.util.Log.INFO;

public abstract class Direct implements DAO.Direct {

    private static final String TAG = Direct.class.getSimpleName();

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
        private final Select mSelect;

        private SingleAccess(@NonNull final Direct dao,
                             @NonNull final Notifier notifier,
                             @NonNull final Route.Item route,
                             @NonNull final Object... arguments) {
            super(dao, notifier, route, arguments);

            mDAO = dao;
            mNotifier = notifier;

            mSelect = select().with(Select.Limit.Single).build();
        }

        @NonNull
        @Override
        public final <M extends Model> Maybe<M> query(@NonNull final M model) {
            return query(Model.toInstance(model)).map(Functions.constant(model));
        }

        @NonNull
        @Override
        public final <M extends Instance.Readable> Maybe<M> query(@NonNull final M model) {
            beforeRead(model);
            final Plan.Read<M> plan = single(model.getName(), Reading.Item.Update.from(model));
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

        @NonNull
        @Override
        public final <M extends Instance.Writable> Maybe<Pair<Value.Write.Operation, Uri>> save(@NonNull final M model) {
            return save(model, write(model));
        }

        @NonNull
        @Override
        public final Maybe<Pair<Value.Write.Operation, Uri>> save(@NonNull final Writer writer) {
            return save(null, write(writer));
        }

        @NonNull
        @Override
        public final <M> Maybe<Pair<Value.Write.Operation, Uri>> save(@Nullable final M model,
                                                                      @NonNull final Value.Write<M> value) {
            return save(model, write(something(model), value));
        }

        @NonNull
        @Override
        public final <M> Maybe<Pair<Value.Write.Operation, Uri>> save(@NonNull final M model,
                                                                      @NonNull final Mapper.Write<M> mapper) {
            return save(model, mapper.prepareWrite(model));
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
        protected final <M> Maybe<Pair<Value.Write.Operation, Uri>> save(@Nullable final M model,
                                                                         @NonNull final Plan.Write plan) {
            final Maybe<Pair<Value.Write.Operation, Uri>> result;

            if (plan.isEmpty()) {
                result = nothing();
            } else {
                final Boolean exists = exists().getOrElse(null);
                result = ((exists == null) || !exists) ?
                        something(Insert).and(insert(model, plan)) :
                        something(Update).and(update(model, plan));
            }

            return result;
        }
    }

    @NonNull
    public static Direct create(@NonNull final SQLiteDatabase database,
                                @NonNull final Notifier notifier) {
        return new InsideTransaction(database, notifier);
    }

    @NonNull
    public static Direct create(@NonNull final Context context,
                                @NonNull final Database database) {
        return new OutsideTransaction(context, database);
    }

    private static class ManyAccess extends SomeAccess implements DAO.Direct.Access.Many {

        private ManyAccess(@NonNull final Direct dao,
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
        protected final <V> Maybe<V> read(@Nullable final V model,
                                          @NonNull final Plan.Read<V> plan,
                                          @NonNull final Select select) {
            final Maybe<V> result;

            if (plan.isEmpty()) {
                result = (model == null) ? Maybes.<V>nothing() : something(model);
            } else {
                final Function<Producer<Maybe<V>>, Maybe<V>> afterRead = afterRead();
                result = mDAO.execute(new android.orm.dao.direct.Read<>(plan, select)).flatMap(afterRead);
            }

            return result;
        }

        protected final void notifyChange() {
            mNotifier.notifyChange(mUri);
        }
    }

    private static class QueryBuilder<V> implements DAO.Direct.Query.Builder.Many.Refreshable<V> {

        @NonNull
        private final BaseAccess<?> mAccess;
        @NonNull
        private final Reading<V> mReading;
        @NonNull
        private final Select.Where mDefault;

        @NonNull
        private Select.Builder mSelect;
        @Nullable
        private V mValue;

        private QueryBuilder(@NonNull final BaseAccess<?> access,
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
        public final Maybe<V> execute() {
            beforeRead(mValue);
            final Plan.Read<V> plan = (mValue == null) ?
                    mReading.preparePlan() :
                    mReading.preparePlan(mValue);
            return mAccess.read(mValue, plan, mSelect.build());
        }
    }

    private static class InsideTransaction extends Direct {

        @NonNull
        private final SQLiteDatabase mDatabase;

        private InsideTransaction(@NonNull final SQLiteDatabase database,
                                  @NonNull final Notifier notifier) {
            super(notifier);

            mDatabase = database;
        }

        @Override
        public final void execute(@NonNull final Statement statement) {
            Async.interruptIfNecessary();
            statement.execute(mDatabase);
        }

        @NonNull
        @Override
        public final <V> Maybe<V> execute(@NonNull final Expression<V> expression) {
            Async.interruptIfNecessary();
            return expression.execute(mDatabase);
        }

        @NonNull
        @Override
        public final <V> Maybe<V> execute(@NonNull final Transaction.Direct<V> transaction) {
            Async.interruptIfNecessary();
            Maybe<V> result = nothing();
            @NonNls final String savepoint = Helper.escape(UUID.randomUUID().toString());
            mDatabase.execSQL("savepoint " + savepoint + ';'); //NON-NLS

            try {
                result = transaction.run(this);
            } catch (final Transaction.Rollback ignored) {
                mDatabase.execSQL("rollback transaction to savepoint " + savepoint + ';'); //NON-NLS
                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, "Subtransaction has been rolled back"); //NON-NLS
                }
            }

            return result;
        }
    }

    private static class OutsideTransaction extends Direct {

        @NonNull
        private final ContentResolver mResolver;
        @NonNull
        private final SQLiteOpenHelper mHelper;

        private OutsideTransaction(@NonNull final Context context,
                                   @NonNull final Database database) {
            super(new Notifier.Immediate(context.getContentResolver()));

            mResolver = context.getContentResolver();
            mHelper = database.getDatabaseHelper(context);
        }

        @Override
        public final void execute(@NonNull final Statement statement) {
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
        public final <V> Maybe<V> execute(@NonNull final Expression<V> expression) {
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

        @NonNull
        @Override
        public final <V> Maybe<V> execute(@NonNull final Transaction.Direct<V> transaction) {
            Maybe<V> result = nothing();

            final SQLiteDatabase db = mHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                final Notifier.Delayed notifier = new Notifier.Delayed(mResolver);
                result = transaction.run(Direct.create(db, notifier));
                notifier.sendAll();
                db.setTransactionSuccessful();
            } catch (final Transaction.Rollback ignored) {
                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, "Transaction has been rolled back"); //NON-NLS
                }
            } finally {
                db.endTransaction();
            }

            return result;
        }
    }
}
