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

package android.orm.remote.watch.executor;

import android.content.ContentResolver;
import android.net.Uri;
import android.orm.remote.Route;
import android.orm.remote.watch.Dispatcher;
import android.orm.remote.watch.Executor;
import android.orm.remote.watch.Observer;
import android.orm.util.Cancelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public abstract class DispatcherExecutor implements Executor {

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
    public final android.orm.remote.watch.Session session(@NonNull final ContentResolver resolver) {
        return new Session(this, resolver);
    }

    @NonNull
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

    private static class Session implements android.orm.remote.watch.Session {

        @NonNls
        private static final String UNKNOWN_STATE = "Unknown state: ";

        @NonNull
        private final DispatcherExecutor mExecutor;
        @NonNull
        private final ContentResolver mResolver;

        private final Lock mLock = new ReentrantLock();
        private final Collection<Registration> mRegistrations = new ArrayList<>();
        @State
        private int mState = State.INITIALIZED;

        private Session(@NonNull final DispatcherExecutor executor,
                        @NonNull final ContentResolver resolver) {
            super();

            mExecutor = executor;
            mResolver = resolver;
        }

        @Override
        public final boolean isStarted() {
            final boolean result;

            mLock.lock();
            try {
                result = mState == State.STARTED;
            } finally {
                mLock.unlock();
            }

            return result;
        }

        @Override
        public final void start() {
            mLock.lock();
            try {
                switch (mState) {
                    case State.INITIALIZED:
                    case State.PAUSED:
                    case State.STOPPED:
                        for (final Registration registration : mRegistrations) {
                            registration.start();
                        }
                        mState = State.STARTED;
                        break;
                    case State.STARTED:
                            /* do nothing */
                        break;
                    default:
                        throw new UnsupportedOperationException(UNKNOWN_STATE + mState);
                }
            } finally {
                mLock.unlock();
            }
        }

        @Override
        public final void pause() {
            mLock.lock();
            try {
                switch (mState) {
                    case State.STARTED:
                        for (final Registration registration : mRegistrations) {
                            registration.stop();
                        }
                        mState = State.PAUSED;
                        break;
                    case State.INITIALIZED:
                    case State.PAUSED:
                    case State.STOPPED:
                            /* do nothing */
                        break;
                    default:
                        throw new UnsupportedOperationException(UNKNOWN_STATE + mState);
                }
            } finally {
                mLock.unlock();
            }
        }

        @Override
        public final void stop() {
            mLock.lock();
            try {
                switch (mState) {
                    case State.STARTED:
                    case State.INITIALIZED:
                    case State.PAUSED:
                        for (final Registration registration : mRegistrations) {
                            registration.stop();
                        }
                        mRegistrations.clear();
                        mState = State.STOPPED;
                        break;
                    case State.STOPPED:
                            /* do nothing */
                        break;
                    default:
                        throw new UnsupportedOperationException(UNKNOWN_STATE + mState);
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
            final Cancelable cancelable;

            mLock.lock();
            try {
                final Registration registration = new Registration(mExecutor, mResolver, route, uri, observer);
                if (mState == State.STARTED) {
                    registration.start();
                }
                mRegistrations.add(registration);

                cancelable = new Cancelable() {
                    @Override
                    public void cancel() {
                        mLock.lock();
                        try {
                            registration.stop();
                            mRegistrations.remove(registration);
                        } finally {
                            mLock.unlock();
                        }
                    }
                };
            } finally {
                mLock.unlock();
            }

            return cancelable;
        }

        @Retention(SOURCE)
        @IntDef({State.INITIALIZED, State.STARTED, State.PAUSED, State.STOPPED})
        private @interface State {
            int INITIALIZED = 0;
            int STARTED = 1;
            int PAUSED = 2;
            int STOPPED = 3;
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
