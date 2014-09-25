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

import android.content.ContentResolver;
import android.net.Uri;
import android.orm.reactive.Route;
import android.orm.sql.Table;
import android.orm.util.Cancelable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Manager implements Dispatcher.Callback {

    public abstract boolean isEmpty();

    public abstract int size();

    @NonNull
    public abstract Cancelable submit(@NonNull final Route route,
                                      @NonNull final Uri uri,
                                      @NonNull final Observer observer);

    public static class PerUri extends Manager {

        @NonNull
        private final ContentResolver mResolver;

        private final Lock mLock = new ReentrantLock();
        private final Map<Uri, ContentObserver.MultiDispatch> mObservers = new HashMap<>();
        private final List<Registration> mRegistrations = new ArrayList<>();
        @Nullable
        private Handler mHandler;

        public PerUri(@NonNull final ContentResolver resolver) {
            super();

            mResolver = resolver;
        }

        @Override
        public final boolean isEmpty() {
            final boolean result;

            mLock.lock();
            try {
                result = mRegistrations.isEmpty();
            } finally {
                mLock.unlock();
            }

            return result;
        }

        @Override
        public final int size() {
            final int result;

            mLock.lock();
            try {
                result = mRegistrations.size();
            } finally {
                mLock.unlock();
            }

            return result;
        }

        @Override
        public final void onStart(@NonNull final Handler handler) {
            mLock.lock();
            try {
                onStop();
                for (final Registration registration : mRegistrations) {
                    start(handler, registration);
                }
                mHandler = handler;
            } finally {
                mLock.unlock();
            }
        }

        @Override
        public final void onStop() {
            mLock.lock();
            try {
                mHandler = null;
                for (final ContentObserver observer : mObservers.values()) {
                    mResolver.unregisterContentObserver(observer);
                }
                mObservers.clear();
            } finally {
                mLock.unlock();
            }
        }

        @NonNull
        @Override
        public final Cancelable submit(@NonNull final Route route,
                                       @NonNull final Uri uri,
                                       @NonNull final Observer observer) {
            final Registration registration = new Registration(route, uri, observer);

            mLock.lock();
            try {
                mRegistrations.add(registration);
                if (mHandler != null) {
                    start(mHandler, registration);
                }
            } finally {
                mLock.unlock();
            }

            return new Cancelable() {
                @Override
                public void cancel() {
                    mLock.lock();
                    try {
                        mRegistrations.remove(registration);
                        stop(registration);
                    } finally {
                        mLock.unlock();
                    }
                }
            };
        }

        private void start(@NonNull final Handler handler,
                           @NonNull final Registration registration) {
            final Uri uri = registration.getUri();

            mLock.lock();
            try {
                if (mObservers.containsKey(uri)) {
                    registration.register(mObservers.get(uri));
                } else {
                    mObservers.put(uri, registration.multi(mResolver, handler));
                }
            } finally {
                mLock.unlock();
            }
        }

        private void stop(@NonNull final Registration registration) {
            final Uri uri = registration.getUri();

            mLock.lock();
            try {
                if (mObservers.containsKey(uri)) {
                    final ContentObserver.MultiDispatch observer = mObservers.get(uri);
                    registration.unregister(observer);
                    if (observer.isEmpty()) {
                        mResolver.unregisterContentObserver(observer);
                        mObservers.remove(uri);
                    }
                }
            } finally {
                mLock.unlock();
            }
        }
    }

    public static class PerObserver extends Manager {

        @NonNull
        private final ContentResolver mResolver;

        private final Lock mLock = new ReentrantLock();
        private final List<Registration> mRegistrations = new ArrayList<>();

        @Nullable
        private Handler mHandler;

        public PerObserver(@NonNull final ContentResolver resolver) {
            super();

            mResolver = resolver;
        }

        @Override
        public final boolean isEmpty() {
            final boolean result;

            mLock.lock();
            try {
                result = mRegistrations.isEmpty();
            } finally {
                mLock.unlock();
            }

            return result;
        }

        @Override
        public final int size() {
            final int result;

            mLock.lock();
            try {
                result = mRegistrations.size();
            } finally {
                mLock.unlock();
            }

            return result;
        }

        @Override
        public final void onStart(@NonNull final Handler handler) {
            mLock.lock();
            try {
                onStop();
                for (final Registration registration : mRegistrations) {
                    registration.single(mResolver, handler);
                }
                mHandler = handler;
            } finally {
                mLock.unlock();
            }
        }

        @Override
        public final void onStop() {
            mLock.lock();
            try {
                mHandler = null;
                for (final Registration registration : mRegistrations) {
                    registration.unregister(mResolver);
                }
            } finally {
                mLock.unlock();
            }
        }

        @NonNull
        @Override
        public final Cancelable submit(@NonNull final Route route,
                                       @NonNull final Uri uri,
                                       @NonNull final Observer observer) {
            final Registration registration = new Registration(route, uri, observer);

            mLock.lock();
            try {
                mRegistrations.add(registration);
                if (mHandler != null) {
                    registration.single(mResolver, mHandler);
                }
            } finally {
                mLock.unlock();
            }

            return new Cancelable() {
                @Override
                public void cancel() {
                    mLock.lock();
                    try {
                        registration.unregister(mResolver);
                        mRegistrations.remove(registration);
                    } finally {
                        mLock.unlock();
                    }
                }
            };
        }
    }

    public interface Factory {

        @NonNull
        Manager create(@NonNull final ContentResolver resolver);

        Factory Default = new Factory() {
            @NonNull
            @Override
            public Manager create(@NonNull final ContentResolver resolver) {
                return new PerObserver(resolver);
            }
        };
    }

    private static class Registration {

        @NonNull
        private final Table<?> mTable;
        @NonNull
        private final Route.Manager mRouteManager;
        @NonNull
        private final Uri mUri;
        @NonNull
        private final Observer mObserver;

        private final Runnable mFirstRun = new Runnable() {
            @Override
            public void run() {
                mObserver.onChange(null);
            }
        };

        @Nullable
        private ContentObserver.SingleDispatch mSingleDispatch;

        private Registration(@NonNull final Route route,
                             @NonNull final Uri uri,
                             @NonNull final Observer observer) {
            super();

            mTable = route.getTable();
            mRouteManager = route.getManager();
            mUri = uri;
            mObserver = observer;
        }

        @NonNull
        public final Uri getUri() {
            return mUri;
        }

        public final void single(@NonNull final ContentResolver resolver,
                                 @NonNull final Handler handler) {
            unregister(resolver);
            handler.post(mFirstRun);
            mSingleDispatch = new ContentObserver.SingleDispatch(handler, mTable, mRouteManager, mObserver);
            resolver.registerContentObserver(mUri, true, mSingleDispatch);
        }

        @NonNull
        public final ContentObserver.MultiDispatch multi(@NonNull final ContentResolver resolver,
                                                         @NonNull final Handler handler) {
            final ContentObserver.MultiDispatch observer = new ContentObserver.MultiDispatch(handler, mTable, mRouteManager);
            handler.post(mFirstRun);
            observer.add(mObserver);
            resolver.registerContentObserver(mUri, true, observer);
            return observer;
        }

        public final void unregister(@NonNull final ContentResolver resolver) {
            if (mSingleDispatch != null) {
                resolver.unregisterContentObserver(mSingleDispatch);
                mSingleDispatch = null;
            }
        }

        public final void register(@NonNull final ContentObserver.MultiDispatch observer) {
            observer.add(mObserver);
        }

        public final void unregister(@NonNull final ContentObserver.MultiDispatch observer) {
            observer.remove(mObserver);
        }
    }
}
