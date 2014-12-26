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

package android.orm.dao.async;

import android.orm.dao.Direct;
import android.orm.dao.ErrorHandler;
import android.orm.dao.Result;
import android.orm.util.Cancelable;
import android.orm.util.Maybe;
import android.orm.util.ObjectPool;
import android.orm.util.Promise;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class ExecutionContext {

    private static final String TAG = ExecutionContext.class.getSimpleName();

    @NonNull
    private final ExecutorService mExecutor;

    private final AtomicReference<ErrorHandler> mErrorHandler = new AtomicReference<>();

    public ExecutionContext(@NonNull final ExecutorService executor) {
        super();

        mExecutor = executor;
    }

    public final void setErrorHandler(@Nullable final ErrorHandler handler) {
        mErrorHandler.set(handler);
    }

    @NonNull
    public final <V> Result<V> execute(@NonNull final Task<V> task) {
        final Runnable runnable = Runnable.Pool.borrow();
        final Promise<Maybe<V>> promise = runnable.init(task);
        final Cancelable cancelable = cancelable(mExecutor.submit(runnable));
        return new Result<>(promise.getFuture(), cancelable, mErrorHandler.get());
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

    public interface Task<V> {
        @NonNull
        Maybe<V> run();
    }

    private static class Runnable implements java.lang.Runnable {

        public static final ObjectPool<Runnable> Pool = new ObjectPool<Runnable>() {
            @NonNull
            @Override
            protected Runnable produce(@NonNull final Receipt<Runnable> receipt) {
                return new Runnable(receipt);
            }
        };

        @NonNull
        private final ObjectPool.Receipt<Runnable> mReceipt;

        private Runnable(@NonNull final ObjectPool.Receipt<Runnable> receipt) {
            super();

            mReceipt = receipt;
        }

        private Task<?> mTask;
        private Promise<Maybe<?>> mPromise;

        @NonNull
        @SuppressWarnings("unchecked")
        public final <V> Promise<Maybe<V>> init(@NonNull final Task<V> task) {
            mTask = task;
            final Promise<Maybe<V>> promise = new Promise<>();
            mPromise = (Promise<Maybe<?>>) (Object) promise;
            return promise;
        }

        @Override
        public final void run() {
            try {
                mPromise.success(mTask.run());
            } catch (final Direct.Interrupted error) {
                Log.w(TAG, "Async task has been interrupted", error); //NON-NLS
            } catch (final Throwable error) {
                Log.e(TAG, "Async task has been aborted", error); //NON-NLS
                mPromise.failure(error);
            } finally {
                mTask = null;
                mPromise = null;
                mReceipt.yield();
            }
        }
    }
}
