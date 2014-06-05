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

package android.orm.util;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.concurrent.Semaphore;

import static android.orm.util.Futures.compose;
import static android.orm.util.Futures.deliver;

public class Promise<V> {

    private static final String TAG = Promise.class.getSimpleName();
    @NonNls
    private static final String COMPLETION_INTERRUPTED = "Completion of promise interrupted";

    @Nullable
    private V mValue;
    @Nullable
    private Throwable mError;
    @Nullable
    private Future.Callback<V> mCallback;

    private final Semaphore mSemaphore = new Semaphore(1);

    @NonNull
    private final Future<V> mFuture = new PromisedFuture<V>() {
        @Override
        public void onComplete(@Nullable final Handler handler,
                               @NonNull final Callback<? super V> callback) {
            try {
                register(deliver(handler, callback));
            } catch (final InterruptedException ex) {
                Log.e(TAG, "Registering callback with future interrupted", ex); //NON-NLS
            }
        }
    };

    @NonNull
    public final Future<V> getFuture() {
        return mFuture;
    }

    public final void success(@NonNull final V value) {
        try {
            Future.Callback<V> callback;

            mSemaphore.acquire();
            try {
                mError = null;
                mValue = value;
                callback = mCallback;
            } finally {
                mSemaphore.release();
            }

            if (callback != null) {
                callback.onResult(value);
            }
        } catch (final InterruptedException ex) {
            Log.e(TAG, COMPLETION_INTERRUPTED, ex);
        }
    }

    public final void failure(@NonNull final Throwable error) {
        try {
            Future.Callback<V> callback;

            mSemaphore.acquire();
            try {
                mError = error;
                mValue = null;
                callback = mCallback;
            } finally {
                mSemaphore.release();
            }

            if (callback != null) {
                callback.onError(error);
            }
        } catch (final InterruptedException ex) {
            Log.e(TAG, COMPLETION_INTERRUPTED, ex);
        }
    }

    public final void completeWith(@NonNull final Future<? extends V> future) {
        future.onComplete(null, new Completer<>(this));
    }

    private void register(@NonNull final Future.Callback<V> callback) throws InterruptedException {
        Throwable error;
        V value;

        mSemaphore.acquire();
        try {
            error = mError;
            value = mValue;
            mCallback = (mCallback == null) ? callback : compose(mCallback, callback);
        } finally {
            mSemaphore.release();
        }

        if ((error != null) || (value != null)) {
            if (error == null) {
                callback.onResult(value);
            } else {
                callback.onError(error);
            }
        }
    }

    private abstract static class PromisedFuture<V> extends Future<V> {

        @NonNull
        @Override
        public final <T> Future<T> map(@NonNull final Function<? super V, ? extends T> function) {
            final Promise<T> promise = new Promise<>();
            onComplete(new FutureMap<>(promise, function));
            return promise.getFuture();
        }

        @NonNull
        @Override
        public final <T> Future<T> flatMap(@NonNull final Function<? super V, Future<T>> function) {
            final Promise<T> promise = new Promise<>();
            onComplete(new FutureFlatMap<>(promise, function));
            return promise.getFuture();
        }

        private static class FutureMap<V, T> implements Callback<V> {

            @NonNull
            private final Promise<T> mPromise;
            @NonNull
            private final Function<? super V, ? extends T> mConverter;

            private FutureMap(@NonNull final Promise<T> promise,
                              @NonNull final Function<? super V, ? extends T> converter) {
                super();

                mPromise = promise;
                mConverter = converter;
            }

            @Override
            public final void onResult(@NonNull final V value) {
                try {
                    mPromise.success(mConverter.invoke(value));
                } catch (final Throwable error) {
                    Log.e(TAG, "Future conversion failed", error); //NON-NLS
                    mPromise.failure(error);
                }
            }

            @Override
            public final void onError(@NonNull final Throwable error) {
                mPromise.failure(error);
            }
        }

        private static class FutureFlatMap<V, T> implements Callback<V> {

            @NonNull
            private final Promise<T> mPromise;
            @NonNull
            private final Function<? super V, Future<T>> mFunction;

            private FutureFlatMap(@NonNull final Promise<T> promise,
                                  @NonNull final Function<? super V, Future<T>> function) {
                super();

                mPromise = promise;
                mFunction = function;
            }

            @Override
            public final void onResult(@NonNull final V value) {
                mPromise.completeWith(mFunction.invoke(value));
            }

            @Override
            public final void onError(@NonNull final Throwable error) {
                mPromise.failure(error);
            }
        }
    }

    private static class Completer<V> implements Future.Callback<V> {

        @NonNull
        private final Promise<? super V> mPromise;

        private Completer(@NonNull final Promise<? super V> promise) {
            super();

            mPromise = promise;
        }

        @Override
        public final void onResult(@NonNull final V value) {
            mPromise.success(value);
        }

        @Override
        public final void onError(@NonNull final Throwable error) {
            mPromise.failure(error);
        }
    }
}
