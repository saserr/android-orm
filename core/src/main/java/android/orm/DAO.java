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

package android.orm;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.orm.access.ErrorHandler;
import android.orm.access.Result;
import android.orm.dao.Task;
import android.orm.dao.operation.Apply;
import android.orm.dao.operation.Delete;
import android.orm.dao.operation.Exists;
import android.orm.dao.operation.Insert;
import android.orm.dao.operation.Parse;
import android.orm.dao.operation.Query;
import android.orm.dao.operation.Read;
import android.orm.dao.operation.Update;
import android.orm.dao.operation.Watch;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Observer;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Readable;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.sql.statement.Select;
import android.orm.util.Cancelable;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static android.orm.model.Observer.afterRead;
import static android.orm.model.Observer.beforeRead;
import static android.orm.model.Readings.single;

public class DAO {

    // TODO logging

    private static final String TAG = DAO.class.getSimpleName();
    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

    @NonNls
    private static final String UNKNOWN_STATE = "Unknown state: ";
    @NonNls
    private static final String DAO_STOPPED = "DAO is stopped";
    @NonNls
    private static final String EXECUTE_TASK_ON_NON_STARTED = "Task has been executed on a non-started DAO. For now this is allowed, but that might change in future!";

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final Handler mHandler;
    @NonNull
    private final ExecutorService mExecutor;

    @NonNull
    private final Apply mApply;

    private final AtomicReference<ErrorHandler> mErrorHandler = new AtomicReference<>();
    private final Semaphore mSemaphore = new Semaphore(1);
    private final Collection<Watch<?>> mWatchers = new ArrayList<>();

    @State
    private int mState = State.INITIALIZED;

    public DAO(@NonNull final Context context) {
        this(context, DEFAULT_EXECUTOR);
    }

    public DAO(@NonNull final Context context,
               @NonNull final ExecutorService executor) {
        super();

        mResolver = context.getContentResolver();
        mHandler = new Handler();
        mExecutor = executor;

        mApply = new Apply(mResolver);
    }

    public final void setErrorHandler(@Nullable final ErrorHandler handler) {
        mErrorHandler.set(handler);
    }

    public final void start() {
        try {
            mSemaphore.acquire();
            try {
                switch (mState) {
                    case State.INITIALIZED:
                    case State.PAUSED:
                        for (final Watch<?> watcher : mWatchers) {
                            watcher.start();
                        }
                        mState = State.STARTED;
                        break;
                    case State.STARTED:
                        /* do nothing */
                        break;
                    case State.STOPPED:
                        throw new UnsupportedOperationException(DAO_STOPPED);
                    default:
                        throw new UnsupportedOperationException(UNKNOWN_STATE + mState);
                }
            } finally {
                mSemaphore.release();
            }
        } catch (final InterruptedException ex) {
            Log.e(TAG, "Thread interrupted while starting DAO", ex); //NON-NLS
        }
    }

    public final void pause() {
        try {
            mSemaphore.acquire();
            try {
                switch (mState) {
                    case State.STARTED:
                        for (final Watch<?> watcher : mWatchers) {
                            watcher.stop();
                        }
                        mState = State.PAUSED;
                        break;
                    case State.INITIALIZED:
                    case State.PAUSED:
                        /* do nothing */
                        break;
                    case State.STOPPED:
                        throw new UnsupportedOperationException(DAO_STOPPED);
                    default:
                        throw new UnsupportedOperationException(UNKNOWN_STATE + mState);
                }
            } finally {
                mSemaphore.release();
            }
        } catch (final InterruptedException ex) {
            Log.e(TAG, "Thread interrupted while pausing DAO", ex); //NON-NLS
        }
    }

    public final void stop() {
        try {
            mSemaphore.acquire();
            try {
                switch (mState) {
                    case State.STARTED:
                        for (final Watch<?> watcher : mWatchers) {
                            watcher.stop();
                        }
                        //noinspection fallthrough
                    case State.INITIALIZED:
                    case State.PAUSED:
                        mWatchers.clear();
                        mState = State.STOPPED;
                        break;
                    case State.STOPPED:
                        /* do nothing */
                        break;
                    default:
                        throw new UnsupportedOperationException(UNKNOWN_STATE + mState);
                }
            } finally {
                mSemaphore.release();
            }

            mErrorHandler.set(null);
        } catch (final InterruptedException ex) {
            Log.e(TAG, "Thread interrupted while stopping DAO", ex); //NON-NLS
        }
    }

    @NonNull
    public final Access.Single at(@NonNull final Route.Item route,
                                  @NonNull final Object... arguments) {
        return new SingleAccess(route, arguments);
    }

    @NonNull
    public final Access.Many at(@NonNull final Route.Dir route,
                                @NonNull final Object... arguments) {
        return new ManyAccess(route, arguments);
    }

    @NonNull
    public final Access.Write at(@NonNull final Route route, @NonNull final Object... arguments) {
        return new WriteAccess(route, arguments);
    }

    @NonNull
    public final Result<Integer> delete(@NonNull final Uri uri) {
        return delete(uri, Select.Where.None);
    }

    @NonNull
    public final Result<Integer> delete(@NonNull final Uri uri, @NonNull final Select.Where where) {
        return execute(where, new Delete(mResolver, uri));
    }

    @NonNull
    public final android.orm.dao.Transaction transaction() {
        return new Transaction();
    }

    @NonNull
    public final <V> Result<V> execute(@NonNull final Function<ContentResolver, Maybe<V>> function) {
        return execute(mResolver, function);
    }

    @NonNull
    private <V> Cancelable watch(@NonNull final Route.Manager manager,
                                 @NonNull final Uri uri,
                                 @NonNull final Reading<V> reading,
                                 @NonNull final Select.Where where,
                                 @Nullable final Select.Order order,
                                 @NonNull final Result.Callback<? super V> callback) {
        final Watch<V> watch = new Watch<>(
                manager,
                mHandler,
                mResolver,
                uri,
                reading,
                where,
                order,
                callback
        );

        try {
            mSemaphore.acquire();
            try {
                switch (mState) {
                    case State.STARTED:
                        watch.start();
                        //noinspection fallthrough
                    case State.INITIALIZED:
                    case State.PAUSED:
                        mWatchers.add(watch);
                        break;
                    case State.STOPPED:
                        throw new UnsupportedOperationException(DAO_STOPPED);
                    default:
                        throw new UnsupportedOperationException(UNKNOWN_STATE + mState);
                }
            } finally {
                mSemaphore.release();
            }
        } catch (final InterruptedException ex) {
            Log.e(TAG, "Thread interrupted while starting a watcher", ex); //NON-NLS
        }

        return cancelable(watch);
    }

    @NonNull
    private <V, T> Result<T> execute(@NonNull final V value,
                                     @NonNull final Function<V, Maybe<T>> function) {
        try {
            mSemaphore.acquire();
            try {
                if (mState != State.STARTED) {
                    Log.w(TAG, EXECUTE_TASK_ON_NON_STARTED, new Throwable());
                }
            } finally {
                mSemaphore.release();
            }
        } catch (final InterruptedException ignored) {
        }

        final Task<V, Maybe<T>> task = new Task<>(value, function);
        mExecutor.execute(task);
        return new Result<>(task.getFuture(), mErrorHandler.get());
    }

    @NonNull
    private <V> Cancelable cancelable(@NonNull final Watch<V> watch) {
        return new Cancelable() {
            @Override
            public void cancel() {
                try {
                    mSemaphore.acquire();
                    try {
                        mWatchers.remove(watch);
                        watch.stop();
                    } finally {
                        mSemaphore.release();
                    }
                } catch (final InterruptedException ex) {
                    Log.e(TAG, "Thread interrupted while stopping a watcher", ex); //NON-NLS
                }
            }
        };
    }

    public static final class Access {

        public interface Insert extends android.orm.Access.Insert<Result<Readable>> {
        }

        public interface Update extends android.orm.Access.Update<Result<Integer>> {
        }

        public interface Delete extends android.orm.Access.Delete<Result<Integer>> {
        }

        public interface Write extends Insert, Update, Delete, android.orm.Access.Write<Result<Readable>, Result<Integer>, Result<Integer>> {
        }

        public interface Single extends android.orm.Access.Read.Single, Write {
        }

        public interface Many extends android.orm.Access.Read.Many, Write {
        }

        private static final class Read {
            private static class Builder<V> implements android.orm.Access.Read.Builder.Refreshable<V> {

                @NonNull
                private final ReadAccess mAccess;
                @NonNull
                private final Reading<V> mReading;

                @Nullable
                private V mValue;
                @NonNull
                private Select.Where mWhere = Select.Where.None;
                @Nullable
                private Select.Order mOrder;

                private Builder(@NonNull final ReadAccess access,
                                @NonNull final Reading<V> reading) {
                    super();

                    mAccess = access;
                    mReading = reading;
                }

                @NonNull
                @Override
                public final Builder<V> where(@Nullable final Select.Where where) {
                    mWhere = (where == null) ? Select.Where.None : where;
                    return this;
                }

                @NonNull
                @Override
                public final Builder<V> order(@Nullable final Select.Order order) {
                    mOrder = order;
                    return this;
                }

                @NonNull
                @Override
                public final Builder<V> using(@Nullable final V value) {
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
                        result = mAccess.query(plan, mWhere, mOrder);
                    }

                    return result;
                }

                @NonNull
                @Override
                public final Cancelable watch(@NonNull final Result.Callback<? super V> callback) {
                    return mAccess.watch(mReading, mWhere, mOrder, callback);
                }
            }

            private Read() {
                super();
            }
        }

        private Access() {
            super();
        }
    }

    private class SingleAccess extends android.orm.Access.Read.Single.Base implements Access.Single {

        @NonNull
        private final ReadAccess mReadAccess;
        @NonNull
        private final WriteAccess mWriteAccess;

        private SingleAccess(@NonNull final Route.Item route, @NonNull final Object... arguments) {
            super();

            mReadAccess = new ReadAccess(route, arguments);
            mWriteAccess = new WriteAccess(route, arguments);
        }

        @NonNull
        @Override
        public final Result<Boolean> exists(@NonNull final Select.Where where) {
            return mReadAccess.exists(where);
        }

        @NonNull
        @Override
        public final <M> android.orm.Access.Read.Builder.Refreshable<M> query(@NonNull final Reading.Single<M> reading) {
            return new Access.Read.Builder<>(mReadAccess, reading);
        }

        @NonNull
        @Override
        public final <M extends Instance.Writable> Result<Readable> insert(@NonNull final M model) {
            return mWriteAccess.insert(model);
        }

        @NonNull
        @Override
        public final <M> Result<Readable> insert(@NonNull final M model,
                                                 @NonNull final Value.Write<M> value) {
            return mWriteAccess.insert(model, value);
        }

        @NonNull
        @Override
        public final <M> Result<Readable> insert(@NonNull final M model,
                                                 @NonNull final Mapper.Write<M> mapper) {
            return mWriteAccess.insert(model, mapper);
        }

        @NonNull
        @Override
        public final <M extends Instance.Writable> Result<Integer> update(@NonNull final M model) {
            return mWriteAccess.update(model);
        }

        @NonNull
        @Override
        public final <M extends Instance.Writable> Result<Integer> update(@NonNull final M model,
                                                                          @NonNull final Select.Where where) {
            return mWriteAccess.update(model, where);
        }

        @NonNull
        @Override
        public final <M> Result<Integer> update(@NonNull final M model,
                                                @NonNull final Value.Write<M> value) {
            return mWriteAccess.update(model, value);
        }

        @NonNull
        @Override
        public final <M> Result<Integer> update(@NonNull final M model,
                                                @NonNull final Select.Where where,
                                                @NonNull final Value.Write<M> value) {
            return mWriteAccess.update(model, where, value);
        }

        @NonNull
        @Override
        public final <M> Result<Integer> update(@NonNull final M model,
                                                @NonNull final Mapper.Write<M> mapper) {
            return mWriteAccess.update(model, mapper);
        }

        @NonNull
        @Override
        public final <M> Result<Integer> update(@NonNull final M model,
                                                @NonNull final Select.Where where,
                                                @NonNull final Mapper.Write<M> mapper) {
            return mWriteAccess.update(model, where, mapper);
        }

        @NonNull
        @Override
        public final Result<Integer> delete() {
            return mWriteAccess.delete();
        }

        @NonNull
        @Override
        public final Result<Integer> delete(@NonNull final Select.Where where) {
            return mWriteAccess.delete(where);
        }
    }

    private class ManyAccess extends android.orm.Access.Read.Many.Base implements Access.Many {

        @NonNull
        private final ReadAccess mReadAccess;
        @NonNull
        private final WriteAccess mWriteAccess;

        private ManyAccess(@NonNull final Route.Dir route, @NonNull final Object... arguments) {
            super();

            mReadAccess = new ReadAccess(route, arguments);
            mWriteAccess = new WriteAccess(route, arguments);
        }

        @NonNull
        @Override
        public final Result<Boolean> exists(@NonNull final Select.Where where) {
            return mReadAccess.exists(where);
        }

        @NonNull
        @Override
        public final <M> android.orm.Access.Read.Builder<M> query(@NonNull final AggregateFunction<M> function) {
            return new Access.Read.Builder<>(mReadAccess, single(function));
        }

        @NonNull
        @Override
        public final <M> android.orm.Access.Read.Builder.Refreshable<M> query(@NonNull final Reading.Many<M> reading) {
            return new Access.Read.Builder<>(mReadAccess, reading);
        }

        @NonNull
        @Override
        public final <M extends Instance.Writable> Result<Readable> insert(@NonNull final M model) {
            return mWriteAccess.insert(model);
        }

        @NonNull
        @Override
        public final <M> Result<Readable> insert(@NonNull final M model,
                                                 @NonNull final Value.Write<M> value) {
            return mWriteAccess.insert(model, value);
        }

        @NonNull
        @Override
        public final <M> Result<Readable> insert(@NonNull final M model,
                                                 @NonNull final Mapper.Write<M> mapper) {
            return mWriteAccess.insert(model, mapper);
        }

        @NonNull
        @Override
        public final <M extends Instance.Writable> Result<Integer> update(@NonNull final M model) {
            return mWriteAccess.update(model);
        }

        @NonNull
        @Override
        public final <M extends Instance.Writable> Result<Integer> update(@NonNull final M model,
                                                                          @NonNull final Select.Where where) {
            return mWriteAccess.update(model, where);
        }

        @NonNull
        @Override
        public final <M> Result<Integer> update(@NonNull final M model,
                                                @NonNull final Value.Write<M> value) {
            return mWriteAccess.update(model, value);
        }

        @NonNull
        @Override
        public final <M> Result<Integer> update(@NonNull final M model,
                                                @NonNull final Select.Where where,
                                                @NonNull final Value.Write<M> value) {
            return mWriteAccess.update(model, where, value);
        }

        @NonNull
        @Override
        public final <M> Result<Integer> update(@NonNull final M model,
                                                @NonNull final Mapper.Write<M> mapper) {
            return mWriteAccess.update(model, mapper);
        }

        @NonNull
        @Override
        public final <M> Result<Integer> update(@NonNull final M model,
                                                @NonNull final Select.Where where,
                                                @NonNull final Mapper.Write<M> mapper) {
            return mWriteAccess.update(model, where, mapper);
        }

        @NonNull
        @Override
        public final Result<Integer> delete() {
            return mWriteAccess.delete();
        }

        @NonNull
        @Override
        public final Result<Integer> delete(@NonNull final Select.Where where) {
            return mWriteAccess.delete(where);
        }
    }

    private static final Object PRODUCE = new Function.Base<Producer<Maybe<Object>>, Maybe<Object>>() {
        @NonNull
        @Override
        public Maybe<Object> invoke(@NonNull final Producer<Maybe<Object>> producer) {
            final Maybe<Object> result = producer.produce();
            if (result.isSomething()) {
                afterRead(result.get());
            }
            return result;
        }
    };

    private class ReadAccess {

        @NonNull
        private final Route.Manager mRouteManager;
        @NonNull
        private final Uri mUri;
        @NonNull
        private final Exists mExists;
        @NonNull
        private final Object mQuery;

        private ReadAccess(@NonNull final Route route, @NonNull final Object... arguments) {
            super();

            mRouteManager = route.getManager();
            mUri = route.createUri(arguments);

            mExists = new Exists(mResolver, mUri);
            mQuery = new Query<>(mResolver, mUri).compose(new Read<>());
        }

        @NonNull
        public final <V> Cancelable watch(@NonNull final Reading<V> reading,
                                          @NonNull final Select.Where where,
                                          @Nullable final Select.Order order,
                                          @NonNull final Result.Callback<? super V> callback) {
            return DAO.this.watch(mRouteManager, mUri, reading, where, order, callback);
        }

        @NonNull
        public final Result<Boolean> exists(@NonNull final Select.Where where) {
            return execute(where, mExists);
        }

        @NonNull
        @SuppressWarnings("unchecked")
        public final <M> Result<M> query(@NonNull final Plan.Read<M> plan,
                                         @NonNull final Select.Where where,
                                         @Nullable final Select.Order order) {
            final Function<Query.Arguments<M>, Maybe<Producer<Maybe<M>>>> query = (Function<Query.Arguments<M>, Maybe<Producer<Maybe<M>>>>) mQuery;
            final Function<Producer<Maybe<M>>, Maybe<M>> produce = (Function<Producer<Maybe<M>>, Maybe<M>>) PRODUCE;
            return execute(new Query.Arguments<>(plan, where, order), query).flatMap(produce);
        }
    }

    private class WriteAccess extends Access.Write.Base<Result<Readable>, Result<Integer>, Result<Integer>> implements Access.Write {

        @NonNull
        private final Function<Writer, Maybe<Readable>> mInsert;
        @NonNull
        private final Update mUpdate;
        @NonNull
        private final Delete mDelete;

        private WriteAccess(@NonNull final Route route, @NonNull final Object... arguments) {
            super();

            final Uri uri = route.createUri(arguments);

            mInsert = new Insert(mResolver, uri).compose(new Parse(route.getItemRoute()));
            mUpdate = new Update(mResolver, uri);
            mDelete = new Delete(mResolver, uri);
        }

        @NonNull
        @Override
        protected final <M> Result<Readable> insert(@NonNull final M model,
                                                    @NonNull final Plan.Write plan) {
            return afterCreate(
                    plan.isEmpty() ? Result.<Readable>nothing() : execute(plan, mInsert),
                    model
            );
        }

        @NonNull
        @Override
        protected final <M> Result<Integer> update(@NonNull final M model,
                                                   @NonNull final Select.Where where,
                                                   @NonNull final Plan.Write plan) {
            return afterUpdate(
                    plan.isEmpty() ? Result.<Integer>nothing() : execute(Pair.<Writer, Select.Where>create(plan, where), mUpdate),
                    model
            );
        }

        @NonNull
        @Override
        public final Result<Integer> delete(@NonNull final Select.Where where) {
            return execute(where, mDelete);
        }
    }

    @NonNull
    private static <M, V> Result<V> afterCreate(@NonNull final Result<V> result,
                                                @NonNull final M model) {
        return (model instanceof Observer.Write) ?
                result.map(new Function.Base<V, V>() {
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
                result.map(new Function.Base<V, V>() {
                    @NonNull
                    @Override
                    public V invoke(@NonNull final V value) {
                        Observer.afterUpdate(model);
                        return value;
                    }
                }) :
                result;
    }

    private class Transaction extends android.orm.dao.Transaction {

        private Transaction() {
            super(mApply);
        }

        @NonNull
        @Override
        protected final <V, T> android.orm.access.Result<T> execute(@NonNull final V value,
                                                                    @NonNull final Function<V, Maybe<T>> function) {
            return DAO.this.execute(value, function);
        }
    }

    @IntDef({State.INITIALIZED, State.STARTED, State.PAUSED, State.STOPPED})
    private @interface State {
        int INITIALIZED = 0;
        int STARTED = 1;
        int PAUSED = 2;
        int STOPPED = 3;
    }
}
