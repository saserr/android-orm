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
import android.orm.Route;
import android.orm.dao.async.Manager;
import android.orm.dao.async.Observer;
import android.orm.util.Cancelable;
import android.os.Handler;
import android.support.annotation.NonNull;

public class CurrentThreadObserverExecutor implements Observer.Executor {

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
    public final android.orm.dao.async.Session session(@NonNull final ContentResolver resolver) {
        return new Session(resolver, mFactory, mHandler);
    }

    private static class Session extends android.orm.dao.async.Session.Base {

        @NonNull
        private final ContentResolver mResolver;
        @NonNull
        private final Manager.Factory mFactory;
        @NonNull
        private final Handler mHandler;

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
        protected final void onStart() {
            mManager.onStart(mHandler);
        }

        @Override
        protected final void onPause() {
            mManager.onStop();
        }

        @Override
        protected final void onStop() {
            mManager.onStop();
            mManager = mFactory.create(mResolver);
        }

        @NonNull
        @Override
        protected final Cancelable submit(final boolean started,
                                          @NonNull final Route route,
                                          @NonNull final Uri uri,
                                          @NonNull final Observer observer) {
            return mManager.submit(route, uri, observer);
        }
    }
}
