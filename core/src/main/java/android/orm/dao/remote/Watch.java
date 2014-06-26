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

package android.orm.dao.remote;

import android.orm.dao.Result;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.util.Function;
import android.orm.util.Future;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.orm.util.Promise;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.atomic.AtomicReference;

import static android.orm.model.Observer.beforeRead;

public class Watch<V, T extends V> implements Future.Callback<Maybe<Producer<Maybe<T>>>>, Runnable {

    private static final String TAG = Watch.class.getSimpleName();

    @NonNull
    private final Handler mHandler;
    @NonNull
    private final Reading<T> mReading;
    @NonNull
    private final Read<T> mRead;
    @NonNull
    private final Read.Arguments<T> mDefault;
    @NonNull
    private final Result.Callback<V> mCallback;
    @NonNull
    private final AtomicReference<Read.Arguments<T>> mArguments;

    private final Function<Producer<Maybe<T>>, Maybe<T>> mAfterRead = android.orm.dao.direct.Read.afterRead();

    public Watch(@NonNull final Handler handler,
                 @NonNull final Reading<T> reading,
                 @NonNull final Read<T> read,
                 @NonNull final Read.Arguments<T> arguments,
                 @NonNull final Result.Callback<V> callback) {
        super();

        mHandler = handler;
        mReading = reading;
        mRead = read;
        mDefault = arguments;
        mCallback = callback;
        mArguments = new AtomicReference<>(arguments);
    }

    @Override
    public final void onResult(@NonNull final Maybe<Producer<Maybe<T>>> value) {
        final Maybe<T> current = value.flatMap(mAfterRead);
        final Plan.Read<T> plan;

        if (current.isSomething()) {
            final T t = current.get();
            if (t == null) {
                plan = mReading.preparePlan();
            } else {
                beforeRead(t);
                plan = mReading.preparePlan(t);
            }
        } else {
            plan = mReading.preparePlan();
        }

        mArguments.set(mDefault.copy(plan));
        mCallback.onResult(Maybes.<V>safeCast(current));
    }

    @Override
    public final void onError(@NonNull final Throwable error) {
        Log.w(TAG, "Error while querying", error); //NON-NLS
    }

    @Override
    public final void run() {
        final Promise<Maybe<Producer<Maybe<T>>>> promise = new Promise<>();
        promise.getFuture().onComplete(mHandler, this);
        try {
            promise.success(mRead.invoke(mArguments.get()));
        } catch (final Throwable error) {
            promise.failure(error);
        }
    }
}
