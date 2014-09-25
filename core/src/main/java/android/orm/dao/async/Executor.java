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
import android.orm.util.Producer;
import android.orm.util.Promise;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class Executor {

    private static final String TAG = Executor.class.getSimpleName();

    @NonNull
    private final ExecutorService mExecutor;

    private final AtomicReference<ErrorHandler> mErrorHandler = new AtomicReference<>();

    public Executor(@NonNull final ExecutorService executor) {
        super();

        mExecutor = executor;
    }

    public final void setErrorHandler(@Nullable final ErrorHandler handler) {
        mErrorHandler.set(handler);
    }

    @NonNull
    public final <V> Result<V> execute(@NonNull final Producer<Maybe<V>> producer) {
        final Task<V> task = new Task<>(producer);
        final Cancelable cancelable = cancelable(mExecutor.submit(task));
        return new Result<>(task.getFuture(), cancelable, mErrorHandler.get());
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

    private static class Task<V> implements Runnable {

        @NonNull
        private final Producer<Maybe<V>> mProducer;

        @NonNull
        private final Promise<Maybe<V>> mPromise = new Promise<>();

        private Task(@NonNull final Producer<Maybe<V>> producer) {
            super();

            mProducer = producer;
        }

        @NonNull
        public final android.orm.util.Future<Maybe<V>> getFuture() {
            return mPromise.getFuture();
        }

        @Override
        public final void run() {
            try {
                mPromise.success(mProducer.produce());
            } catch (final Direct.Interrupted error) {
                Log.w(TAG, "Async operation has been interrupted", error); //NON-NLS
            } catch (final Throwable error) {
                Log.e(TAG, "Async operation has been aborted", error); //NON-NLS
                mPromise.failure(error);
            }
        }
    }
}
