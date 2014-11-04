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

package android.orm.test;

import android.content.ContentResolver;
import android.net.Uri;
import android.orm.remote.Route;
import android.orm.remote.watch.Executor;
import android.orm.remote.watch.Manager;
import android.orm.remote.watch.Observer;
import android.orm.util.Cancelable;
import android.os.Handler;
import android.support.annotation.NonNull;

public class CurrentThreadObserverExecutor implements Executor {

    @NonNull
    private final Manager.Factory mFactory;

    @NonNull
    private final Handler mHandler = new Handler();

    public CurrentThreadObserverExecutor() {
        this(Manager.Factory.Default);
    }

    public CurrentThreadObserverExecutor(@NonNull final Manager.Factory factory) {
        super();

        mFactory = factory;
    }

    @NonNull
    @Override
    public final android.orm.remote.watch.Session session(@NonNull final ContentResolver resolver) {
        return new Session(resolver, mFactory, mHandler);
    }

    private static class Session implements android.orm.remote.watch.Session {

        @NonNull
        private final ContentResolver mResolver;
        @NonNull
        private final Manager.Factory mFactory;
        @NonNull
        private final Handler mHandler;

        private boolean mStarted = false;
        @NonNull
        private Manager mManager;

        private Session(@NonNull final ContentResolver resolver,
                        @NonNull final Manager.Factory factory,
                        @NonNull final Handler handler) {
            super();

            mResolver = resolver;
            mFactory = factory;
            mHandler = handler;
            mManager = factory.create(resolver);
        }

        @Override
        public final boolean isStarted() {
            return mStarted;
        }

        @Override
        public final void start() {
            if (!mStarted) {
                mManager.onStart(mHandler);
                mStarted = true;
            }
        }

        @Override
        public final void pause() {
            if (mStarted) {
                mManager.onStop();
                mStarted = false;
            }
        }

        @Override
        public final void stop() {
            pause();
            mManager = mFactory.create(mResolver);
        }

        @NonNull
        @Override
        public final Cancelable submit(@NonNull final Route route,
                                       @NonNull final Uri uri,
                                       @NonNull final Observer observer) {
            return mManager.submit(route, uri, observer);
        }
    }
}
