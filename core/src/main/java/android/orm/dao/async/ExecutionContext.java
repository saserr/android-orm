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
        final Promise<Maybe<V>> promise = new Promise<>();
        final Runnable runnable = runnable(promise, task);
        final Cancelable cancelable = cancelable(mExecutor.submit(runnable));
        return new Result<>(promise.getFuture(), cancelable, mErrorHandler.get());
    }

    @NonNull
    private static <V> Runnable runnable(@NonNull final Promise<Maybe<V>> promise,
                                         @NonNull final Task<V> task) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    promise.success(task.run());
                } catch (final Direct.Interrupted error) {
                    Log.w(TAG, "Async task has been interrupted", error); //NON-NLS
                } catch (final Throwable error) {
                    Log.e(TAG, "Async task has been aborted", error); //NON-NLS
                    promise.failure(error);
                }
            }
        };
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
}
