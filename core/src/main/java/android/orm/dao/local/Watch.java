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

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.orm.Route;
import android.orm.access.Result;
import android.orm.dao.Watcher;
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
import android.orm.util.Promise;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.concurrent.atomic.AtomicReference;

import static android.orm.model.Observer.beforeRead;

public class Watch<V> implements Watcher {

    private final Watching<? super V, V> mWatching;

    public Watch(@NonNull final SQLiteOpenHelper helper,
                 @NonNull final Route.Manager manager,
                 @NonNull final Handler handler,
                 @NonNull final ContentResolver resolver,
                 @NonNls @NonNull final Uri uri,
                 @NonNull final Reading<V> reading,
                 @NonNull final Plan.Read<V> plan,
                 @NonNull final Select select,
                 @NonNull final Result.Callback<? super V> callback) {
        super();

        mWatching = new Watching<>(helper, manager, handler, resolver, uri, reading, plan, select, callback);
    }

    @Override
    public final void start() {
        new Thread(mWatching).start();
    }

    @Override
    public final void stop() {
        mWatching.stop();
    }

    private static class Watching<V, T extends V> implements Future.Callback<Maybe<Producer<Maybe<T>>>>, Runnable {

        private static final String TAG = Watching.class.getSimpleName();

        @NonNull
        private final SQLiteOpenHelper mHelper;
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
        private final Select mSelect;
        @NonNull
        private final Result.Callback<V> mCallback;

        @NonNull
        private final Table<?> mTable;
        @NonNull
        private final AtomicReference<Plan.Read<T>> mPlan;

        private final Function<Producer<Maybe<T>>, Maybe<T>> mAfterRead = Read.afterRead();
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
        private Watching(@NonNull final SQLiteOpenHelper helper,
                         @NonNull final Route.Manager manager,
                         @NonNull final Handler handler,
                         @NonNull final ContentResolver resolver,
                         @NonNls @NonNull final Uri uri,
                         @NonNull final Reading<T> reading,
                         @NonNull final Plan.Read<T> plan,
                         @NonNull final Select select,
                         @NonNull final Result.Callback<V> callback) {
            super();

            mHelper = helper;
            mRouteManager = manager;
            mHandler = handler;
            mResolver = resolver;
            mUri = uri;
            mReading = reading;
            mSelect = select;
            mCallback = callback;

            final Match match = manager.match(uri);
            if (match == null) {
                throw new IllegalArgumentException("Unknown uri " + uri + "! No route connected to it."); //NON-NLS
            }
            mTable = match.getTable();

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
            final Promise<Maybe<Producer<Maybe<T>>>> promise = new Promise<>();
            promise.getFuture().onComplete(mHandler, this);
            promise.success(new Read<>(mPlan.get(), mSelect).invoke(mHelper.getReadableDatabase()));
        }
    }
}
