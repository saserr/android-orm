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
import android.orm.dao.async.Manager;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DispatcherPerUriExecutor extends DispatcherExecutor {

    @NonNull
    private final Manager.Factory mFactory;

    private final Lock mLock = new ReentrantLock();
    private final Map<Uri, Dispatcher> mDispatchers = new HashMap<>();

    public DispatcherPerUriExecutor() {
        this(Manager.Factory.Default);
    }

    public DispatcherPerUriExecutor(@NonNull final Manager.Factory factory) {
        super();

        mFactory = factory;
    }

    public DispatcherPerUriExecutor(final long removalDelay, @NonNull final TimeUnit removalDelayUnit) {
        this(removalDelay, removalDelayUnit, Manager.Factory.Default);
    }

    public DispatcherPerUriExecutor(final long removalDelay,
                                    @NonNull final TimeUnit removalDelayUnit,
                                    @NonNull final Manager.Factory factory) {
        super(removalDelay, removalDelayUnit);

        mFactory = factory;
    }

    @NonNull
    @Override
    protected final Dispatcher get(@NonNull final ContentResolver resolver,
                                   @NonNull final Route route,
                                   @NonNull final Uri uri) {
        final Dispatcher dispatcher;

        mLock.lock();
        try {
            if (mDispatchers.containsKey(uri)) {
                dispatcher = mDispatchers.get(uri);
            } else {
                dispatcher = new Dispatcher(mFactory.create(resolver));
                dispatcher.start();
                mDispatchers.put(uri, dispatcher);
            }
        } finally {
            mLock.unlock();
        }

        return dispatcher;
    }

    @Override
    protected final void stop(@NonNull final Dispatcher dispatcher,
                              @NonNull final Route route,
                              @NonNull final Uri uri) {
        dispatcher.stop();

        mLock.lock();
        try {
            mDispatchers.remove(uri);
        } finally {
            mLock.unlock();
        }
    }
}
