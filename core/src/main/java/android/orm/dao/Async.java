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

import android.content.Context;
import android.database.SQLException;
import android.net.Uri;
import android.orm.DAO;
import android.orm.Route;
import android.orm.dao.async.Observer;
import android.orm.dao.async.Session;
import android.orm.util.Cancelable;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.orm.util.Promise;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Async implements DAO.Async {

    // TODO logging

    private static final String TAG = DAO.class.getSimpleName();

    private static final Interrupted Interrupted = new Interrupted("Interrupted");
    @NonNls
    private static final String ERROR_STOPPED = "DAO is stopped";

    @NonNull
    private final ExecutorService mTasks;
    @NonNull
    private final Session mObservers;

    private final AtomicBoolean mStopped = new AtomicBoolean(false);
    private final AtomicReference<ErrorHandler> mErrorHandler = new AtomicReference<>();

    protected Async(@NonNull final Context context,
                    @NonNull final ExecutorService tasks,
                    @NonNull final Observer.Executor observers) {
        super();

        mTasks = tasks;
        mObservers = observers.session(context.getContentResolver());
    }

    @Override
    public final void setErrorHandler(@Nullable final ErrorHandler handler) {
        if (mStopped.get()) {
            throw new UnsupportedOperationException(ERROR_STOPPED);
        }

        mErrorHandler.set(handler);
    }

    @Override
    public final void start() {
        if (mStopped.get()) {
            throw new UnsupportedOperationException(ERROR_STOPPED);
        }

        mObservers.start();
    }

    @Override
    public final void pause() {
        if (mStopped.get()) {
            throw new UnsupportedOperationException(ERROR_STOPPED);
        }

        mObservers.pause();
    }

    @Override
    public final void stop() {
        if (!mStopped.getAndSet(true)) {
            mObservers.stop();
            mErrorHandler.set(null);
        }
    }

    @NonNull
    protected final <V> Result<V> execute(@NonNull final Producer<Maybe<V>> producer) {
        if (mStopped.get()) {
            throw new UnsupportedOperationException(ERROR_STOPPED);
        }

        final Promise<Maybe<V>> promise = new Promise<>();
        final Cancelable cancelable = cancelable(mTasks.submit(new Task<>(promise, producer)));
        return new Result<>(promise.getFuture(), cancelable, mErrorHandler.get());
    }

    @NonNull
    protected final Cancelable execute(@NonNull final Route route,
                                       @NonNull final Uri uri,
                                       @NonNull final Observer observer) {
        if (mStopped.get()) {
            throw new UnsupportedOperationException(ERROR_STOPPED);
        }

        return mObservers.submit(route, uri, observer);
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
