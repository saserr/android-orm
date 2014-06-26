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

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.orm.Route;
import android.orm.route.Match;
import android.orm.sql.Table;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.concurrent.atomic.AtomicReference;

public class Observer {

    private final Notifier mNotifier;

    public Observer(@NonNull final Route.Manager manager,
                    @NonNull final ContentResolver resolver,
                    @NonNls @NonNull final Uri uri,
                    @NonNull final Runnable runnable) {
        super();

        mNotifier = new Notifier(manager, resolver, uri, runnable);
    }

    public final void start() {
        new Thread(mNotifier).start();
    }

    public final void stop() {
        mNotifier.stop();
    }

    private static class Notifier implements Runnable {

        private static final String TAG = Notifier.class.getSimpleName();

        @NonNull
        private final Route.Manager mRouteManager;
        @NonNull
        private final ContentResolver mResolver;
        @NonNls
        @NonNull
        private final Uri mUri;
        @NonNull
        private final Runnable mRunnable;
        @NonNull
        private final Table<?> mTable;

        private final AtomicReference<Looper> mLooper = new AtomicReference<>();

        private final ContentObserver mObserver = new ContentObserver(new Handler()) {

            @Override
            public void onChange(final boolean selfChange) {
                mRunnable.run();
            }

            @Override
            @SuppressWarnings("RefusedBequest")
            public void onChange(final boolean selfChange, @NonNls @NonNull final Uri uri) {
                final Match match = mRouteManager.match(uri);
                if (match == null) {
                    Log.w(TAG, "Received unexpected uri " + uri + "! No route connected to it."); //NON-NLS
                }

                if ((match == null) || mTable.equals(match.getTable())) {
                    mRunnable.run();
                }
            }
        };

        @SuppressWarnings("unchecked")
        private Notifier(@NonNull final Route.Manager manager,
                         @NonNull final ContentResolver resolver,
                         @NonNls @NonNull final Uri uri,
                         @NonNull final Runnable runnable) {
            super();

            mRouteManager = manager;
            mResolver = resolver;
            mUri = uri;
            mRunnable = runnable;

            final Match match = manager.match(uri);
            if (match == null) {
                throw new IllegalArgumentException("Unknown uri " + uri + "! No route connected to it."); //NON-NLS
            }
            mTable = match.getTable();
        }

        @Override
        public final void run() {
            Looper.prepare();

            final Looper current = Looper.myLooper();
            if (mLooper.compareAndSet(null, current)) {
                mRunnable.run();
                mResolver.registerContentObserver(mUri, true, mObserver);
                try {
                    Looper.loop();
                } finally {
                    mResolver.unregisterContentObserver(mObserver);
                }
            } else {
                current.quit();
                Log.w(TAG, "Notify thread has been unexpectedly started twice. Stopping the newer thread!"); //NON-NLS
            }
        }

        public final void stop() {
            final Looper current = mLooper.getAndSet(null);
            if (current != null) {
                current.quit();
            }
        }
    }
}
