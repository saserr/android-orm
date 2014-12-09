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

package android.orm.remote.watch;

import android.net.Uri;
import android.orm.dao.Executor;
import android.orm.dao.Result;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.fragment.Condition;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.util.Function;
import android.orm.util.Future;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.orm.util.Promise;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.atomic.AtomicReference;

import static android.orm.dao.direct.Query.afterRead;
import static android.orm.model.Observer.beforeRead;

public class Watcher<R, M extends R> implements Future.Callback<Maybe<Producer<Maybe<M>>>>, Observer {

    private static final String TAG = Watcher.class.getSimpleName();

    @NonNull
    private final Executor.Direct<?, ?> mExecutor;
    @NonNull
    private final Handler mHandler;
    @NonNull
    private final Reading<M> mReading;
    @NonNull
    private final Condition mCondition;
    @Nullable
    private final Order mOrder;
    @Nullable
    private final Limit mLimit;
    @Nullable
    private final Offset mOffset;
    @NonNull
    private final Result.Callback<R> mCallback;
    @NonNull
    private final AtomicReference<Plan.Read<M>> mPlan;

    @SuppressWarnings("unchecked")
    private final Function<Producer<Maybe<M>>, Maybe<M>> mAfterRead = afterRead();

    public Watcher(@NonNull final Executor.Direct<?, ?> executor,
                   @NonNull final Handler handler,
                   @Nullable final M model,
                   @NonNull final Reading<M> reading,
                   @NonNull final Condition condition,
                   @Nullable final Order order,
                   @Nullable final Limit limit,
                   @Nullable final Offset offset,
                   @NonNull final Result.Callback<R> callback) {
        super();

        mExecutor = executor;
        mHandler = handler;
        mReading = reading;
        mCondition = condition;
        mOrder = order;
        mLimit = limit;
        mOffset = offset;
        mCallback = callback;

        final Plan.Read<M> plan = (model == null) ?
                reading.preparePlan() :
                reading.preparePlan(model);
        mPlan = new AtomicReference<>(plan);
    }

    @Override
    public final void onResult(@NonNull final Maybe<Producer<Maybe<M>>> result) {
        final Maybe<M> current = result.flatMap(mAfterRead);
        final M model = current.getOrElse(null);

        if (model == null) {
            mPlan.set(mReading.preparePlan());
        } else {
            beforeRead(model);
            mPlan.set(mReading.preparePlan(model));
        }

        mCallback.onResult(Maybes.<R>safeCast(current));
    }

    @Override
    public final void onError(@NonNull final Throwable error) {
        Log.w(TAG, "Error while querying", error); //NON-NLS
    }

    @Override
    public final void onChange(@Nullable final Uri uri) {
        final Promise<Maybe<Producer<Maybe<M>>>> promise = new Promise<>();
        promise.getFuture().onComplete(mHandler, this);
        try {
            promise.success(mExecutor.query(mPlan.get(), mCondition, mOrder, mLimit, mOffset));
        } catch (final Throwable error) {
            promise.failure(error);
        }
    }
}
