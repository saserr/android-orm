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

package android.orm.reactive.watch;

import android.net.Uri;
import android.orm.reactive.Route;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;

public abstract class ContentObserver extends android.database.ContentObserver {

    private static final String TAG = ContentObserver.class.getSimpleName();

    @NonNls
    @NonNull
    private final String mTable;
    @NonNull
    private final Route.Manager mRouteManager;

    protected ContentObserver(@NonNull final Handler handler,
                              @NonNls @NonNull final String table,
                              @NonNull final Route.Manager manager) {
        super(handler);

        mTable = table;
        mRouteManager = manager;
    }

    protected abstract void onChange(@Nullable final Uri uri);

    @Override
    public final void onChange(final boolean selfChange) {
        onChange(null);
    }

    @Override
    @SuppressWarnings("RefusedBequest")
    public final void onChange(final boolean selfChange, @NonNls @NonNull final Uri uri) {
        final Route route = mRouteManager.get(uri);
        if (route == null) {
            Log.w(TAG, "Received unexpected uri " + uri + "! No route connected to it."); //NON-NLS
        }

        if ((route == null) || mTable.equals(route.getTable())) {
            onChange(uri);
        }
    }

    public static class SingleDispatch extends ContentObserver {

        @NonNull
        private final Observer mObserver;

        public SingleDispatch(@NonNull final Handler handler,
                              @NonNls @NonNull final String table,
                              @NonNull final Route.Manager manager,
                              @NonNull final Observer observer) {
            super(handler, table, manager);

            mObserver = observer;
        }

        @Override
        protected final void onChange(@Nullable final Uri uri) {
            mObserver.onChange(uri);
        }
    }

    public static class MultiDispatch extends ContentObserver {

        private final Collection<Observer> mObservers = new ArrayList<>();

        public MultiDispatch(@NonNull final Handler handler,
                             @NonNls @NonNull final String table,
                             @NonNull final Route.Manager manager) {
            super(handler, table, manager);
        }

        @Override
        protected final void onChange(@Nullable final Uri uri) {
            for (final Observer observer : mObservers) {
                observer.onChange(uri);
            }
        }

        public final boolean isEmpty() {
            return mObservers.isEmpty();
        }

        public final void add(@NonNull final Observer observer) {
            mObservers.add(observer);
        }

        public final void remove(@NonNull final Observer observer) {
            mObservers.remove(observer);
        }
    }
}
