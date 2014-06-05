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

package android.orm.dao;

import android.orm.util.Function;
import android.orm.util.Future;
import android.orm.util.Promise;
import android.support.annotation.NonNull;
import android.util.Log;

public class Task<V, T> implements Runnable {

    private static final String TAG = Task.class.getSimpleName();

    @NonNull
    private final V mValue;
    @NonNull
    private final Function<V, T> mFunction;

    @NonNull
    private final Promise<T> mPromise = new Promise<>();

    public Task(@NonNull final V value, @NonNull final Function<V, T> function) {
        super();

        mValue = value;
        mFunction = function;
    }

    public final Future<T> getFuture() {
        return mPromise.getFuture();
    }

    @Override
    public final void run() {
        try {
            mPromise.success(mFunction.invoke(mValue));
        } catch (final Throwable error) {
            mPromise.failure(error);
            Log.e(TAG, "There was a problem executing the task", error); //NON-NLS
        }
    }
}
