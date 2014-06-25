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

import android.database.SQLException;
import android.orm.DAO;
import android.orm.dao.async.Watch;
import android.orm.util.Cancelable;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.orm.util.Promise;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public abstract class Async implements DAO.Async {

    // TODO logging

    private static final String TAG = DAO.class.getSimpleName();

    private static final Interrupted Interrupted = new Interrupted("Interrupted");
    @NonNls
    private static final String UNKNOWN_STATE = "Unknown state: ";
    @NonNls
    private static final String DAO_STOPPED = "DAO is stopped";
    @NonNls
    private static final String EXECUTE_TASK_ON_NON_STARTED_DAO = "Task has been executed on a non-started DAO. For now this is allowed, but that might change in future!";

    @NonNull
    private final ExecutorService mExecutor;

    private final AtomicReference<ErrorHandler> mErrorHandler = new AtomicReference<>();
    private final Semaphore mSemaphore = new Semaphore(1);
    private final Collection<Watch> mWatches = new ArrayList<>();

    @State
    private int mState = State.INITIALIZED;

    protected Async(@NonNull final ExecutorService executor) {
        super();

        mExecutor = executor;
    }

    @Override
    public final void setErrorHandler(@Nullable final ErrorHandler handler) {
        mErrorHandler.set(handler);
    }

    @Override
    public final void start() {
        try {
            mSemaphore.acquire();
            try {
                switch (mState) {
                    case State.INITIALIZED:
                    case State.PAUSED:
                        for (final Watch watch : mWatches) {
                            watch.start();
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

    @Override
    public final void pause() {
        try {
            mSemaphore.acquire();
            try {
                switch (mState) {
                    case State.STARTED:
                        for (final Watch watch : mWatches) {
                            watch.stop();
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

    @Override
    public final void stop() {
        try {
            mSemaphore.acquire();
            try {
                switch (mState) {
                    case State.STARTED:
                        for (final Watch watch : mWatches) {
                            watch.stop();
                        }
                        //noinspection fallthrough
                    case State.INITIALIZED:
                    case State.PAUSED:
                        mWatches.clear();
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
                    Log.w(TAG, EXECUTE_TASK_ON_NON_STARTED_DAO, new Throwable());
                }
            } finally {
                mSemaphore.release();
            }
        } catch (final InterruptedException ignored) {
        }

        final Promise<Maybe<V>> promise = new Promise<>();
        final Cancelable cancelable = cancelable(mExecutor.submit(new Task<>(promise, producer)));
        return new Result<>(promise.getFuture(), cancelable, mErrorHandler.get());
    }

    @NonNull
    protected final Cancelable watch(@NonNull final Watch watch) {
        try {
            mSemaphore.acquire();
            try {
                switch (mState) {
                    case State.STARTED:
                        watch.start();
                        //noinspection fallthrough
                    case State.INITIALIZED:
                    case State.PAUSED:
                        mWatches.add(watch);
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
    private Cancelable cancelable(@NonNull final Watch watch) {
        return new Cancelable() {
            @Override
            public void cancel() {
                try {
                    mSemaphore.acquire();
                    try {
                        mWatches.remove(watch);
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

    public static void interruptIfNecessary() {
        if (Thread.interrupted()) {
            throw Interrupted;
        }
    }

    @NonNull
    private static Cancelable cancelable(@NonNull final Future<?> future) {
        return new Cancelable() {
            @Override
            public void cancel() {
                future.cancel(true);
            }
        };
    }

    public static class Interrupted extends SQLException {

        private static final long serialVersionUID = 1316911609066835294L;

        private Interrupted(@NonNls @NonNull final String error) {
            super(error);
        }
    }

    @Retention(SOURCE)
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
            } catch (final Interrupted error) {
                Log.w(TAG, "DAO operation has been interrupted", error); //NON-NLS
            } catch (final Throwable error) {
                Log.e(TAG, "DAO operation has been aborted", error); //NON-NLS
                mPromise.failure(error);
            }
        }
    }
}
