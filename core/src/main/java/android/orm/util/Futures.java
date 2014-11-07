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

import static java.lang.Thread.currentThread;

public final class Futures {

    private static final String TAG = Future.class.getSimpleName();
    @NonNls
    private static final String CALLBACK_FAILED = "Execution of the future callback failed";

    @NonNull
    public static <V> Future<V> success(@NonNull final V value) {
        return new Success<>(value);
    }

    @NonNull
    public static <V> Future<V> failure(@NonNull final Throwable error) {
        return new Failure<>(error);
    }

    @NonNull
    public static <V, T extends V> Future.Callback<T> compose(@NonNull final Future.Callback<V> first,
                                                              @NonNull final Future.Callback<V> second) {
        return new Composition<>(first, second);
    }

    @NonNull
    public static <V> Future.Callback<V> deliver(@Nullable final Handler handler,
                                                 @NonNull final Future.Callback<? super V> callback) {
        return new Deliver<>(handler, callback);
    }

    public static <V> void deliver(@Nullable final Handler handler,
                                   @NonNull final Future.Callback<? super V> callback,
                                   @NonNull final V value) {
        if ((handler == null) || currentThread().equals(handler.getLooper().getThread())) {
            try {
                callback.onResult(value);
            } catch (final Throwable cause) {
                Log.e(TAG, CALLBACK_FAILED, cause);
            }
        } else if (handler.getLooper().getThread().isAlive()) {
            handler.post(new DeliverResult<>(callback, value));
        }
    }

    public static void deliver(@Nullable final Handler handler,
                               @NonNull final Future.Callback<?> callback,
                               @NonNull final Throwable error) {
        if ((handler == null) || currentThread().equals(handler.getLooper().getThread())) {
            try {
                callback.onError(error);
            } catch (final Throwable cause) {
                Log.e(TAG, CALLBACK_FAILED, cause);
            }
        } else if (handler.getLooper().getThread().isAlive()) {
            handler.post(new DeliverError<>(callback, error));
        }
    }

    private static class Success<V> extends Future<V> {

        @NonNull
        private final V mValue;

        private Success(@NonNull final V value) {
            super();

            mValue = value;
        }

        @Override
        public final void onComplete(@Nullable final Handler handler,
                                     @NonNull final Callback<? super V> callback) {
            deliver(handler, callback, mValue);
        }

        @NonNull
        @Override
        public final <T> Future<T> map(@NonNull final Function<? super V, ? extends T> function) {
            return Futures.<T>success(function.invoke(mValue));
        }

        @NonNull
        @Override
        public final <T> Future<T> flatMap(@NonNull final Function<? super V, Future<T>> function) {
            return function.invoke(mValue);
        }
    }

    private static class Failure<V> extends Future<V> {

        @NonNull
        private final Throwable mError;

        private Failure(@NonNull final Throwable error) {
            super();

            mError = error;
        }

        @Override
        public final void onComplete(@Nullable final Handler handler,
                                     @NonNull final Callback<? super V> callback) {
            deliver(handler, callback, mError);
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public final <T> Future<T> map(@NonNull final Function<? super V, ? extends T> function) {
            return (Future<T>) this;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public final <T> Future<T> flatMap(@NonNull final Function<? super V, Future<T>> function) {
            return (Future<T>) this;
        }
    }

    private static class Deliver<V> implements Future.Callback<V> {

        @Nullable
        private final Handler mHandler;
        @NonNull
        private final Future.Callback<? super V> mCallback;

        private Deliver(@Nullable final Handler handler,
                        @NonNull final Future.Callback<? super V> callback) {
            super();

            mHandler = handler;
            mCallback = callback;
        }

        @Override
        public final void onResult(@NonNull final V value) {
            deliver(mHandler, mCallback, value);
        }

        @Override
        public final void onError(@NonNull final Throwable error) {
            deliver(mHandler, mCallback, error);
        }
    }

    private static class DeliverResult<V> implements Runnable {

        @NonNull
        private final Future.Callback<? super V> mCallback;
        @NonNull
        private final V mValue;

        private DeliverResult(@NonNull final Future.Callback<? super V> callback,
                              @NonNull final V value) {
            super();

            mCallback = callback;
            mValue = value;
        }

        @Override
        public final void run() {
            try {
                mCallback.onResult(mValue);
            } catch (final Throwable cause) {
                Log.e(TAG, CALLBACK_FAILED, cause);
            }
        }
    }

    private static class DeliverError<V> implements Runnable {

        @NonNull
        private final Future.Callback<V> mCallback;
        @NonNull
        private final Throwable mError;

        private DeliverError(@NonNull final Future.Callback<V> callback,
                             @NonNull final Throwable error) {
            super();

            mCallback = callback;
            mError = error;
        }

        @Override
        public final void run() {
            try {
                mCallback.onError(mError);
            } catch (final Throwable cause) {
                Log.e(TAG, CALLBACK_FAILED, cause);
            }
        }
    }

    private static class Composition<V, T extends V> implements Future.Callback<T> {

        @NonNull
        private final Future.Callback<V> mFirst;
        @NonNull
        private final Future.Callback<V> mSecond;

        private Composition(@NonNull final Future.Callback<V> first,
                            @NonNull final Future.Callback<V> second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @Override
        public final void onResult(@NonNull final T value) {
            try {
                mFirst.onResult(value);
            } catch (final Throwable cause) {
                Log.e(TAG, CALLBACK_FAILED, cause);
            } finally {
                try {
                    mSecond.onResult(value);
                } catch (final Throwable cause) {
                    Log.e(TAG, CALLBACK_FAILED, cause);
                }
            }
        }

        @Override
        public final void onError(@NonNull final Throwable error) {
            try {
                mFirst.onError(error);
            } catch (final Throwable cause) {
                Log.e(TAG, CALLBACK_FAILED, cause);
            } finally {
                try {
                    mSecond.onError(error);
                } catch (final Throwable cause) {
                    Log.e(TAG, CALLBACK_FAILED, cause);
                }
            }
        }
    }

    private Futures() {
        super();
    }
}
