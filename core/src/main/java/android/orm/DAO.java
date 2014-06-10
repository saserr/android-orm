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

import android.content.Context;
import android.net.Uri;
import android.orm.access.ErrorHandler;
import android.orm.access.Result;
import android.orm.dao.Watcher;
import android.orm.model.Mapper;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Value;
import android.orm.sql.statement.Select;
import android.orm.util.Cancelable;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.orm.util.Promise;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

public abstract class DAO {

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
    private final ExecutorService mExecutor;

    private final AtomicReference<ErrorHandler> mErrorHandler = new AtomicReference<>();
    private final Semaphore mSemaphore = new Semaphore(1);
    private final Collection<Watcher> mWatchers = new ArrayList<>();

    @State
    private int mState = State.INITIALIZED;

    protected DAO(@NonNull final ExecutorService executor) {
        super();

        mExecutor = executor;
    }

    @NonNull
    public abstract Access.Single at(@NonNull final Route.Item route,
                                     @NonNull final Object... arguments);

    @NonNull
    public abstract Access.Many at(@NonNull final Route.Dir route,
                                   @NonNull final Object... arguments);

    @NonNull
    public abstract Access.Some at(@NonNull final Route route, @NonNull final Object... arguments);

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
                        for (final Watcher watcher : mWatchers) {
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
                        for (final Watcher watcher : mWatchers) {
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
                        for (final Watcher watcher : mWatchers) {
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
    protected final <V> Result<V> execute(@NonNull final Producer<Maybe<V>> producer) {
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

        final Promise<Maybe<V>> promise = new Promise<>();
        mExecutor.execute(new Task<>(promise, producer));
        return new Result<>(promise.getFuture(), mErrorHandler.get());
    }

    @NonNull
    protected final Cancelable watch(@NonNull final Watcher watcher) {
        try {
            mSemaphore.acquire();
            try {
                switch (mState) {
                    case State.STARTED:
                        watcher.start();
                        //noinspection fallthrough
                    case State.INITIALIZED:
                    case State.PAUSED:
                        mWatchers.add(watcher);
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

        return cancelable(watcher);
    }

    @NonNull
    private Cancelable cancelable(@NonNull final Watcher watcher) {
        return new Cancelable() {
            @Override
            public void cancel() {
                try {
                    mSemaphore.acquire();
                    try {
                        mWatchers.remove(watcher);
                        watcher.stop();
                    } finally {
                        mSemaphore.release();
                    }
                } catch (final InterruptedException ex) {
                    Log.e(TAG, "Thread interrupted while stopping a watcher", ex); //NON-NLS
                }
            }
        };
    }

    @NonNull
    public static Local local(@NonNull final Context context,
                              @NonNull final Database database) {
        return local(context, database, DEFAULT_EXECUTOR);
    }

    @NonNull
    public static Local local(@NonNull final Context context,
                              @NonNull final Database database,
                              @NonNull final ExecutorService executor) {
        return new android.orm.dao.Local(context, database, executor);
    }

    @NonNull
    public static Remote remote(@NonNull final Context context) {
        return remote(context, DEFAULT_EXECUTOR);
    }

    @NonNull
    public static Remote remote(@NonNull final Context context,
                                @NonNull final ExecutorService executor) {
        return new android.orm.dao.Remote(context, executor);
    }

    public abstract static class Local extends DAO {

        protected Local(@NonNull final ExecutorService executor) {
            super(executor);
        }

        @NonNull
        @Override
        public abstract Access.Single at(@NonNull final Route.Item route,
                                         @NonNull final Object... arguments);

        @NonNull
        @Override
        public abstract Access.Many at(@NonNull final Route.Dir route,
                                       @NonNull final Object... arguments);

        @NonNull
        public abstract android.orm.dao.local.Transaction.Begin transaction();

        public static final class Access {

            public interface Query<V> extends DAO.Access.Query<V> {

                @NonNull
                @Override
                Query<V> where(@Nullable final Select.Where where);

                @NonNull
                @Override
                Query<V> order(@Nullable final Select.Order order);

                @NonNull
                Query<V> limit(final int limit);

                interface Refreshable<V> extends Query<V>, DAO.Access.Query.Refreshable<V> {

                    @NonNull
                    @Override
                    Access.Query.Refreshable<V> where(@Nullable final Select.Where where);

                    @NonNull
                    @Override
                    Access.Query.Refreshable<V> order(@Nullable final Select.Order order);

                    @NonNull
                    @Override
                    Access.Query.Refreshable<V> limit(final int limit);

                    @NonNull
                    @Override
                    Access.Query.Refreshable<V> using(@Nullable final V v);
                }
            }

            public interface Single extends DAO.Access.Single {

                @NonNull
                @Override
                <M> Query<M> query(@NonNull final Value.Read<M> value);

                @NonNull
                @Override
                <M> Query.Refreshable<M> query(@NonNull final Mapper.Read<M> mapper);

                @NonNull
                @Override
                <M> Query.Refreshable<M> query(@NonNull final Reading.Single<M> reading);
            }

            public interface Many extends DAO.Access.Many {

                @NonNull
                @Override
                <M> Query<M> query(@NonNull final AggregateFunction<M> function);

                @NonNull
                @Override
                <M> Query.Refreshable<List<M>> query(@NonNull final Value.Read<M> value);

                @NonNull
                @Override
                <M> Query.Refreshable<List<M>> query(@NonNull final Mapper.Read<M> mapper);

                @NonNull
                @Override
                <M> Query.Refreshable<M> query(@NonNull final Reading.Many<M> reading);
            }

            private Access() {
                super();
            }
        }
    }

    public abstract static class Remote extends DAO {

        protected Remote(@NonNull final ExecutorService executor) {
            super(executor);
        }

        @NonNull
        public abstract android.orm.dao.remote.Transaction transaction();
    }

    public static final class Access {

        public interface Insert extends android.orm.Access.Insert<Result<Uri>> {
        }

        public interface Update extends android.orm.Access.Update<Result<Integer>> {
        }

        public interface Delete extends android.orm.Access.Delete<Result<Integer>> {
        }

        public interface Write extends Insert, Update, Delete, android.orm.Access.Write<Result<Uri>, Result<Integer>, Result<Integer>> {
            abstract class Base extends android.orm.Access.Write.Base<Result<Uri>, Result<Integer>, Result<Integer>> implements Write {
                protected Base() {
                    super();
                }
            }
        }

        public interface Exists extends android.orm.Access.Exists<Result<Boolean>> {
        }

        public interface Query<V> extends android.orm.Access.Query.Builder<V, Result<V>>, android.orm.Access.Watchable<V> {

            @NonNull
            @Override
            Query<V> where(@Nullable final Select.Where where);

            @NonNull
            @Override
            Query<V> order(@Nullable final Select.Order order);

            interface Refreshable<V> extends Query<V> {

                @NonNull
                @Override
                Refreshable<V> where(@Nullable final Select.Where where);

                @NonNull
                @Override
                Refreshable<V> order(@Nullable final Select.Order order);

                @NonNull
                Refreshable<V> using(@Nullable final V v);
            }
        }

        public interface Some extends Exists, Write {
        }

        public interface Single extends Exists, Write, android.orm.Access.Read.Single<Result<Boolean>> {

            @NonNull
            @Override
            <M> Query<M> query(@NonNull final Value.Read<M> value);

            @NonNull
            @Override
            <M> Query.Refreshable<M> query(@NonNull final Mapper.Read<M> mapper);

            @NonNull
            @Override
            <M> Query.Refreshable<M> query(@NonNull final Reading.Single<M> reading);
        }

        public interface Many extends Exists, Write, android.orm.Access.Read.Many<Result<Boolean>> {

            @NonNull
            @Override
            <M> Query<M> query(@NonNull final AggregateFunction<M> function);

            @NonNull
            @Override
            <M> Query.Refreshable<List<M>> query(@NonNull final Value.Read<M> value);

            @NonNull
            @Override
            <M> Query.Refreshable<List<M>> query(@NonNull final Mapper.Read<M> mapper);

            @NonNull
            @Override
            <M> Query.Refreshable<M> query(@NonNull final Reading.Many<M> reading);
        }

        private Access() {
            super();
        }
    }

    @IntDef({State.INITIALIZED, State.STARTED, State.PAUSED, State.STOPPED})
    private @interface State {
        int INITIALIZED = 0;
        int STARTED = 1;
        int PAUSED = 2;
        int STOPPED = 3;
    }

    private static class Task<V> implements Runnable {

        @NonNull
        private final Promise<Maybe<V>> mPromise;
        @NonNull
        private final Producer<Maybe<V>> mProducer;

        private Task(@NonNull final Promise<Maybe<V>> promise,
                     @NonNull final Producer<Maybe<V>> producer) {
            super();

            mPromise = promise;
            mProducer = producer;
        }

        @Override
        public final void run() {
            try {
                mPromise.success(mProducer.produce());
            } catch (final Throwable error) {
                Log.e(TAG, "DAO operation has been aborted", error); //NON-NLS
                mPromise.failure(error);
            }
        }
    }
}
