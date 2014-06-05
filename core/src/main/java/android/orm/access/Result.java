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

package android.orm.access;

import android.orm.util.Consumer;
import android.orm.util.Either;
import android.orm.util.Eithers;
import android.orm.util.Function;
import android.orm.util.Future;
import android.orm.util.Futures;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.orm.util.Promise;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

public class Result<V> {

    private static final String TAG = Result.class.getSimpleName();
    private static final Result<Object> Nothing = singleton(Maybes.nothing());

    @NonNull
    private final Future<Maybe<V>> mFuture;
    @Nullable
    private final ErrorHandler mErrorHandler;

    public Result(@NonNull final Future<Maybe<V>> future) {
        this(future, null);
    }

    public Result(@NonNull final Future<Maybe<V>> future, @Nullable final ErrorHandler handler) {
        super();

        mFuture = future;
        mErrorHandler = handler;
    }

    @NonNull
    public final Future<Maybe<V>> getFuture() {
        return mFuture;
    }

    @NonNull
    public final <T> Result<T> map(@NonNull final Function<? super V, ? extends T> function) {
        return new Result<>(mFuture.map(Maybes.map(function)), mErrorHandler);
    }

    @NonNull
    public final <T> Result<T> flatMap(@NonNull final Function<? super V, Maybe<T>> function) {
        return new Result<>(mFuture.map(Maybes.flatMap(function)), mErrorHandler);
    }

    @NonNull
    public final Result<V> onComplete(@Nullable final Handler handler,
                                      @NonNull final Callback<? super V> callback) {
        mFuture.onComplete(handler, new Forward<>(callback, mErrorHandler));
        return this;
    }

    @NonNull
    public final Result<V> onComplete(@NonNull final Callback<? super V> callback) {
        mFuture.onComplete(new Forward<>(callback, mErrorHandler));
        return this;
    }

    @NonNull
    public final Result<V> onComplete(@NonNull final Observer<? super V> observer) {
        return onComplete(new Callback<V>() {
            @Override
            public void onResult(@NonNull final Maybe<V> value) {
                try {
                    if (value.isSomething()) {
                        observer.onSomething(value.get());
                    } else {
                        observer.onNothing();
                    }
                } catch (final Throwable cause) {
                    Log.e(TAG, "Result observer failed", cause); //NON-NLS
                }
            }
        });
    }

    @NonNull
    public final Result<V> onComplete(@NonNull final Runnable runnable) {
        return onComplete(new Run<V>(true, true, runnable));
    }

    @NonNull
    public final <T> Result<T> onComplete(@NonNull final Producer<Result<T>> producer) {
        final Promise<Maybe<T>> promise = new Promise<>();
        onComplete(null, new CompleteWith<>(promise, producer, true, true));
        return new Result<>(promise.getFuture(), mErrorHandler);
    }

    @NonNull
    public final Result<V> onSomething(@NonNull final Consumer<? super V> consumer) {
        return onComplete(new Observer<V>() {

            @Override
            public void onSomething(@Nullable final V value) {
                try {
                    consumer.consume(value);
                } catch (final Throwable cause) {
                    Log.e(TAG, "Consumer failed", cause); //NON-NLS
                }
            }

            @Override
            public void onNothing() {/* do nothing */}
        });
    }

    @NonNull
    public final Result<V> onSomething(@NonNull final Runnable runnable) {
        return onComplete(new Run<V>(true, false, runnable));
    }

    @NonNull
    public final <T> Result<T> onSomething(@NonNull final Producer<Result<T>> producer) {
        final Promise<Maybe<T>> promise = new Promise<>();
        onComplete(null, new CompleteWith<>(promise, producer, true, false));
        return new Result<>(promise.getFuture(), mErrorHandler);
    }

    @NonNull
    public final Result<V> onNothing(@NonNull final Runnable runnable) {
        return onComplete(new Run<V>(false, true, runnable));
    }

    @NonNull
    public final <T> Result<T> onNothing(@NonNull final Producer<Result<T>> producer) {
        final Promise<Maybe<T>> promise = new Promise<>();
        onComplete(null, new CompleteWith<>(promise, producer, false, true));
        return new Result<>(promise.getFuture(), mErrorHandler);
    }

    @NonNull
    public final <T> Result<Pair<V, T>> and(@NonNull final Result<T> other) {
        final Future<Maybe<Pair<V, T>>> future = mFuture.and(other.mFuture).map(Maybes.<V, T>liftPair());
        return new Result<>(future, mErrorHandler);
    }

    @NonNull
    public final <T> Result<Either<V, T>> or(@NonNull final Result<T> other) {
        final Promise<Maybe<Either<V, T>>> promise = new Promise<>();

        promise.completeWith(mFuture.map(Maybes.map(Eithers.<V, T>leftLift())));
        promise.completeWith(other.mFuture.map(Maybes.map(Eithers.<V, T>rightLift())));

        return new Result<>(promise.getFuture(), mErrorHandler);
    }

    @NonNull
    public static <V> Result<V> something(@Nullable final V value) {
        return singleton(Maybes.something(value));
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Result<V> nothing() {
        return (Result<V>) Nothing;
    }

    @NonNull
    public static <V> Result<V> singleton(@NonNull final Maybe<? extends V> result) {
        return new Result<>(Futures.success(Maybes.safeCast(result)), null);
    }

    public interface Observer<V> {

        void onSomething(@Nullable final V v);

        void onNothing();
    }

    public interface Callback<V> {

        void onResult(@NonNull final Maybe<V> v);

        abstract class WithPrevious<V> implements Callback<V> {

            @Nullable
            private Maybe<V> mPrevious;

            protected abstract void onResult(@Nullable final Maybe<V> previous,
                                             @NonNull final Maybe<V> current);

            @Override
            public final void onResult(@NonNull final Maybe<V> current) {
                try {
                    onResult(mPrevious, current);
                } finally {
                    mPrevious = current;
                }
            }
        }

        abstract class WhenDifferent<V> extends WithPrevious<V> {

            protected abstract void onChange(@NonNull final Maybe<V> v);

            @Override
            protected final void onResult(@Nullable final Maybe<V> previous,
                                          @NonNull final Maybe<V> current) {
                if ((previous == null) || !previous.equals(current)) {
                    onChange(current);
                }
            }
        }
    }

    private static class Run<V> implements Callback<V> {

        private final boolean mOnSomething;
        private final boolean mOnNothing;
        @NonNull
        private final Runnable mRunnable;

        private Run(final boolean onSomething,
                    final boolean onNothing,
                    @NonNull final Runnable runnable) {
            super();

            mOnSomething = onSomething;
            mOnNothing = onNothing;
            mRunnable = runnable;
        }

        @Override
        public final void onResult(@NonNull final Maybe<V> value) {
            if ((value.isSomething() && mOnSomething) || (value.isNothing() && mOnNothing)) {
                mRunnable.run();
            }
        }
    }

    private static class Forward<V> implements Future.Callback<Maybe<V>> {

        @NonNull
        private final Callback<V> mCallback;
        @Nullable
        private final ErrorHandler mErrorHandler;

        @SuppressWarnings("unchecked")
        private Forward(@NonNull final Callback<? super V> callback,
                        @Nullable final ErrorHandler handler) {
            super();

            mCallback = (Callback<V>) callback;
            mErrorHandler = handler;
        }

        @Override
        public final void onResult(@NonNull final Maybe<V> value) {
            mCallback.onResult(value);
        }

        @Override
        public final void onError(@NonNull final Throwable error) {
            if (mErrorHandler != null) {
                mErrorHandler.onError(error);
            }
        }
    }

    private static class CompleteWith<V, T> implements Callback<V> {

        @NonNull
        private final Promise<Maybe<T>> mPromise;
        @NonNull
        private final Producer<Result<T>> mProducer;
        private final boolean mOnSomething;
        private final boolean mOnNothing;

        private CompleteWith(@NonNull final Promise<Maybe<T>> promise,
                             @NonNull final Producer<Result<T>> producer,
                             final boolean onSomething,
                             final boolean onNothing) {
            super();

            mPromise = promise;
            mProducer = producer;
            mOnSomething = onSomething;
            mOnNothing = onNothing;
        }

        @Override
        public final void onResult(@NonNull final Maybe<V> value) {
            if ((value.isSomething() && mOnSomething) || (value.isNothing() && mOnNothing)) {
                try {
                    mPromise.completeWith(mProducer.produce().getFuture());
                } catch (final Throwable error) {
                    Log.e(TAG, "Result producer failed", error); //NON-NLS
                    mPromise.failure(error);
                }
            }
        }
    }
}
