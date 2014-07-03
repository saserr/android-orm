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

package android.orm.dao.async.executor;

import android.content.ContentResolver;
import android.net.Uri;
import android.orm.Route;
import android.orm.dao.async.Dispatcher;
import android.orm.dao.async.Observer;
import android.orm.util.Cancelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public abstract class DispatcherExecutor implements Observer.Executor {

    private static final long DEFAULT_REMOVAL_DELAY = 60;
    private static final TimeUnit DEFAULT_REMOVAL_DELAY_UNIT = TimeUnit.SECONDS;

    private final long mRemovalDelay;
    @NonNull
    private final TimeUnit mRemovalDelayUnit;

    protected DispatcherExecutor() {
        this(DEFAULT_REMOVAL_DELAY, DEFAULT_REMOVAL_DELAY_UNIT);
    }

    protected DispatcherExecutor(final long removalDelay,
                                 @NonNull final TimeUnit removalDelayUnit) {
        super();

        mRemovalDelay = removalDelay;
        mRemovalDelayUnit = removalDelayUnit;
    }

    @NonNull
    protected abstract Dispatcher get(@NonNull final ContentResolver resolver,
                                      @NonNull final Route route,
                                      @NonNull final Uri uri);

    protected abstract void stop(@NonNull final Dispatcher dispatcher,
                                 @NonNull final Route route,
                                 @NonNull final Uri uri);

    @NonNull
    @Override
    public final android.orm.dao.async.Session session(@NonNull final ContentResolver resolver) {
        return new Session(this, resolver);
    }

    private Cancelable submit(@NonNull final ContentResolver resolver,
                              @NonNull final Route route,
                              @NonNull final Uri uri,
                              @NonNull final Observer observer) {
        final Dispatcher dispatcher = get(resolver, route, uri);
        final Cancelable submission = dispatcher.submit(route, uri, observer);
        return new Cancelable() {
            @Override
            public void cancel() {
                submission.cancel();
                if (dispatcher.isEmpty()) {
                    scheduleRemoval(dispatcher, route, uri);
                }
            }
        };
    }

    private void scheduleRemoval(@NonNull final Dispatcher dispatcher,
                                 @NonNull final Route route,
                                 @NonNull final Uri uri) {
        dispatcher.schedule(mRemovalDelay, mRemovalDelayUnit, new Runnable() {
            @Override
            public void run() {
                if (dispatcher.isEmpty()) {
                    stop(dispatcher, route, uri);
                }
            }
        });
    }

    private static class Session extends android.orm.dao.async.Session.Base {

        @NonNull
        private final DispatcherExecutor mExecutor;
        @NonNull
        private final ContentResolver mResolver;

        private final Collection<Registration> mRegistrations = new ArrayList<>();

        private Session(@NonNull final DispatcherExecutor executor,
                        @NonNull final ContentResolver resolver) {
            super();

            mExecutor = executor;
            mResolver = resolver;
        }

        @Override
        protected final void onStart() {
            for (final Registration registration : mRegistrations) {
                registration.start();
            }
        }

        @Override
        protected final void onPause() {
            for (final Registration registration : mRegistrations) {
                registration.stop();
            }
        }

        @Override
        protected final void onStop() {
            onPause();
            mRegistrations.clear();
        }

        @NonNull
        @Override
        protected final Cancelable submit(final boolean started,
                                          @NonNull final Route route,
                                          @NonNull final Uri uri,
                                          @NonNull final Observer observer) {
            final Registration registration = new Registration(mExecutor, mResolver, route, uri, observer);
            if (started) {
                registration.start();
            }
            mRegistrations.add(registration);

            return new Cancelable() {
                @Override
                public void cancel() {
                    registration.stop();
                    mRegistrations.remove(registration);
                }
            };
        }
    }

    private static class Registration {

        @NonNull
        private final DispatcherExecutor mExecutor;
        @NonNull
        private final ContentResolver mResolver;
        @NonNull
        private final Route mRoute;
        @NonNull
        private final Uri mUri;
        @NonNull
        private final Observer mObserver;

        @Nullable
        private Cancelable mCancelable;

        private Registration(@NonNull final DispatcherExecutor executor,
                             @NonNull final ContentResolver resolver,
                             @NonNull final Route route,
                             @NonNull final Uri uri,
                             @NonNull final Observer observer) {
            super();

            mExecutor = executor;
            mResolver = resolver;
            mRoute = route;
            mUri = uri;
            mObserver = observer;
        }

        public final void start() {
            if (mCancelable != null) {
                mCancelable.cancel();
            }

            mCancelable = mExecutor.submit(mResolver, mRoute, mUri, mObserver);
        }

        public final void stop() {
            if (mCancelable != null) {
                mCancelable.cancel();
                mCancelable = null;
            }
        }
    }
}
