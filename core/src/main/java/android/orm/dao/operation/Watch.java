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

package android.orm.dao.operation;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.orm.Route;
import android.orm.access.Result;
import android.orm.dao.Task;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.route.Match;
import android.orm.sql.Table;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Future;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.concurrent.atomic.AtomicReference;

import static android.orm.model.Observer.afterRead;
import static android.orm.model.Observer.beforeRead;

public class Watch<V> {

    private final Watching<? super V, V> mWatching;

    public Watch(@NonNull final Route.Manager manager,
                 @NonNull final Handler handler,
                 @NonNull final ContentResolver resolver,
                 @NonNls @NonNull final Uri uri,
                 @NonNull final Reading<V> reading,
                 @NonNull final Select.Where where,
                 @Nullable final Select.Order order,
                 @NonNull final Result.Callback<? super V> callback) {
        super();

        mWatching = new Watching<>(manager, handler, resolver, uri, reading, where, order, callback);
    }

    public final void start() {
        new Thread(mWatching).start();
    }

    public final void stop() {
        mWatching.stop();
    }

    private static class Watching<V, T extends V> implements Future.Callback<Maybe<Producer<Maybe<T>>>>, Runnable {

        private static final String TAG = Watching.class.getSimpleName();
        private static final Object PRODUCE = new Function.Base<Producer<Maybe<Object>>, Maybe<Object>>() {
            @NonNull
            @Override
            public Maybe<Object> invoke(@NonNull final Producer<Maybe<Object>> producer) {
                final Maybe<Object> result = producer.produce();
                if (result.isSomething()) {
                    afterRead(result.get());
                }
                return result;
            }
        };

        @NonNull
        private final Route.Manager mRouteManager;
        @NonNull
        private final Handler mHandler;
        @NonNull
        private final ContentResolver mResolver;
        @NonNls
        @NonNull
        private final Uri mUri;
        @NonNull
        private final Reading<T> mReading;
        @NonNull
        private final Select.Where mWhere;
        @Nullable
        private final Select.Order mOrder;
        @NonNull
        private final Result.Callback<V> mCallback;

        @NonNull
        private final Table<?> mTable;
        @NonNull
        private final AtomicReference<Plan.Read<T>> mPlan;
        @NonNull
        private final Function<Query.Arguments<T>, Maybe<Producer<Maybe<T>>>> mQuery;
        @NonNull
        private final Function<Producer<Maybe<T>>, Maybe<T>> mProduce;

        private final AtomicReference<Looper> mLooper = new AtomicReference<>();

        private final ContentObserver mObserver = new ContentObserver(new Handler()) {

            @Override
            public void onChange(final boolean selfChange) {
                query();
            }

            @Override
            @SuppressWarnings("RefusedBequest")
            public void onChange(final boolean selfChange, @NonNls @NonNull final Uri uri) {
                final Match match = mRouteManager.match(uri);
                if (match == null) {
                    Log.w(TAG, "Received unexpected uri " + uri + "! No route connected to it."); //NON-NLS
                }

                if ((match == null) || mTable.equals(match.getTable())) {
                    query();
                }
            }
        };

        @SuppressWarnings("unchecked")
        private Watching(@NonNull final Route.Manager manager,
                         @NonNull final Handler handler,
                         @NonNull final ContentResolver resolver,
                         @NonNls @NonNull final Uri uri,
                         @NonNull final Reading<T> reading,
                         @NonNull final Select.Where where,
                         @Nullable final Select.Order order,
                         @NonNull final Result.Callback<V> callback) {
            super();

            mRouteManager = manager;
            mHandler = handler;
            mResolver = resolver;
            mUri = uri;
            mReading = reading;
            mWhere = where;
            mOrder = order;
            mCallback = callback;

            final Match match = manager.match(uri);
            if (match == null) {
                throw new IllegalArgumentException("Unknown uri " + uri + "! No route connected to it."); //NON-NLS
            }
            mTable = match.getTable();

            mPlan = new AtomicReference<>(reading.preparePlan());
            mQuery = new Query<T>(resolver, uri).compose(new Read<T>());

            mProduce = (Function<Producer<Maybe<T>>, Maybe<T>>) PRODUCE;
        }

        @Override
        public void onResult(@NonNull final Maybe<Producer<Maybe<T>>> value) {
            final Maybe<T> current = value.flatMap(mProduce);

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
        public void onError(@NonNull final Throwable error) {
            Log.w(TAG, "Error while querying for watcher", error); //NON-NLS
        }

        @Override
        public final void run() {
            Looper.prepare();

            final Looper current = Looper.myLooper();
            if (mLooper.compareAndSet(null, current)) {
                query();
                mResolver.registerContentObserver(mUri, true, mObserver);
                try {
                    Looper.loop();
                } finally {
                    mResolver.unregisterContentObserver(mObserver);
                }
            } else {
                current.quit();
                Log.w(TAG, "Watch thread has been unexpectedly started twice. Stopping the newer thread!"); //NON-NLS
            }
        }

        public final void stop() {
            final Looper current = mLooper.getAndSet(null);
            if (current != null) {
                current.quit();
            }
        }

        private void query() {
            final Query.Arguments<T> arguments = new Query.Arguments<>(mPlan.get(), mWhere, mOrder);
            final Task<Query.Arguments<T>, Maybe<Producer<Maybe<T>>>> task = new Task<>(arguments, mQuery);
            task.getFuture().onComplete(mHandler, this);
            task.run();
        }
    }
}
