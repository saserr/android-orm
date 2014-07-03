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

import android.net.Uri;
import android.orm.Route;
import android.orm.util.Cancelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import java.lang.annotation.Retention;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public interface Session {

    boolean isStarted();

    boolean isPaused();

    boolean isStopped();

    void start();

    void pause();

    void stop();

    @NonNull
    Cancelable submit(@NonNull final Route route,
                      @NonNull final Uri uri,
                      @NonNull final Observer observer);

    abstract class Base implements Session {

        @NonNls
        private static final String UNKNOWN_STATE = "Unknown state: ";

        private final Lock mLock = new ReentrantLock();
        @State
        private int mState = State.INITIALIZED;

        protected Base() {
            super();
        }

        protected abstract void onStart();

        protected abstract void onPause();

        protected abstract void onStop();

        @NonNull
        protected abstract Cancelable submit(final boolean started,
                                             @NonNull final Route route,
                                             @NonNull final Uri uri,
                                             @NonNull final Observer observer);

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
        public final boolean isPaused() {
            final boolean result;

            mLock.lock();
            try {
                result = mState == State.PAUSED;
            } finally {
                mLock.unlock();
            }

            return result;
        }

        @Override
        public final boolean isStopped() {
            final boolean result;

            mLock.lock();
            try {
                result = mState == State.STOPPED;
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
                        onStart();
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
                        onPause();
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
                        onStop();
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
                cancelable = submit(mState == State.STARTED, route, uri, observer);
            } finally {
                mLock.unlock();
            }

            return new Cancelable() {
                @Override
                public void cancel() {
                    mLock.lock();
                    try {
                        cancelable.cancel();
                    } finally {
                        mLock.unlock();
                    }
                }
            };
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
}
