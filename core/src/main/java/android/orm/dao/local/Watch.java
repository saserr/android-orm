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

package android.orm.dao.local;

import android.orm.DAO;
import android.orm.dao.Result;
import android.orm.dao.direct.Read;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.Select;
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
    private final DAO.Direct mDAO;
    @NonNull
    private final Handler mHandler;
    @NonNull
    private final Reading<T> mReading;
    @NonNull
    private final Select mSelect;
    @NonNull
    private final Result.Callback<V> mCallback;
    @NonNull
    private final AtomicReference<Plan.Read<T>> mPlan;

    private final Function<Producer<Maybe<T>>, Maybe<T>> mAfterRead = Read.afterRead();

    public Watch(@NonNull final DAO.Direct dao,
                 @NonNull final Handler handler,
                 @NonNull final Reading<T> reading,
                 @NonNull final Plan.Read<T> plan,
                 @NonNull final Select select,
                 @NonNull final Result.Callback<V> callback) {
        super();

        mDAO = dao;
        mHandler = handler;
        mReading = reading;
        mSelect = select;
        mCallback = callback;
        mPlan = new AtomicReference<>(plan);
    }

    @Override
    public final void onResult(@NonNull final Maybe<Producer<Maybe<T>>> value) {
        final Maybe<T> current = value.flatMap(mAfterRead);

        if (current.isSomething()) {
            final T t = current.get();
            if (t == null) {
                mPlan.set(mReading.preparePlan());
            } else {
                beforeRead(t);
                mPlan.set(mReading.preparePlan(t));
            }
        } else {
            mPlan.set(mReading.preparePlan());
        }

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
            promise.success(mDAO.execute(new Read<>(mPlan.get(), mSelect)));
        } catch (final Throwable error) {
            promise.failure(error);
        }
    }
}
