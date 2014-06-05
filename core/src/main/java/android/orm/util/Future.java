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
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

public abstract class Future<V> {

    private static final String TAG = Future.class.getSimpleName();
    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());

    protected Future() {
        super();
    }

    public abstract void onComplete(@Nullable final Handler handler,
                                    @NonNull final Callback<? super V> callback);

    @NonNull
    public abstract <T> Future<T> map(@NonNull final Function<? super V, ? extends T> function);

    @NonNull
    public abstract <T> Future<T> flatMap(@NonNull final Function<? super V, Future<T>> function);

    public final void onComplete(@NonNull final Callback<? super V> callback) {
        onComplete(getHandler(), callback);
    }

    @NonNull
    public final <T> Future<Pair<V, T>> and(@NonNull final Future<? extends T> other) {
        return flatMap(new Function.Base<V, Future<Pair<V, T>>>() {
            @NonNull
            @Override
            public Future<Pair<V, T>> invoke(@NonNull final V v) {
                return other.map(new Base<T, Pair<V, T>>() {
                    @NonNull
                    @Override
                    public Pair<V, T> invoke(@NonNull final T t) {
                        return Pair.create(v, t);
                    }
                });
            }
        });
    }

    @NonNull
    private static Handler getHandler() {
        final Handler result;

        final Looper looper = Looper.myLooper();
        if (looper == null) {
            Log.w(TAG, "Future callback registered on a non-looper thread. Using main thread to execute task, which might not be optimal!", new Throwable()); //NON-NLS
            result = MAIN_THREAD;
        } else {
            result = new Handler();
        }

        return result;
    }

    public interface Callback<V> {

        void onResult(@NonNull final V v);

        void onError(@NonNull final Throwable error);
    }
}
