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
import android.orm.remote.watch.Manager;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DispatcherPerTableExecutor extends DispatcherExecutor {

    @NonNull
    private final Manager.Factory mFactory;

    private final Lock mLock = new ReentrantLock();
    private final Map<String, Dispatcher> mDispatchers = new HashMap<>();

    public DispatcherPerTableExecutor() {
        this(Manager.Factory.Default);
    }

    public DispatcherPerTableExecutor(@NonNull final Manager.Factory factory) {
        super();

        mFactory = factory;
    }

    public DispatcherPerTableExecutor(final long removalDelay, @NonNull final TimeUnit removalDelayUnit) {
        this(removalDelay, removalDelayUnit, Manager.Factory.Default);
    }

    public DispatcherPerTableExecutor(final long removalDelay,
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
        final String table = route.getTable();
        final Dispatcher dispatcher;

        mLock.lock();
        try {
            if (mDispatchers.containsKey(table)) {
                dispatcher = mDispatchers.get(table);
            } else {
                dispatcher = new Dispatcher(mFactory.create(resolver));
                dispatcher.start();
                mDispatchers.put(table, dispatcher);
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
            mDispatchers.remove(route.getTable());
        } finally {
            mLock.unlock();
        }
    }
}
