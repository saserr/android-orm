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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DispatcherPerObserver extends DispatcherExecutor {

    private final Lock mLock = new ReentrantLock();
    private final List<Dispatcher> mDispatchers = new ArrayList<>();

    public DispatcherPerObserver() {
        super();
    }

    public DispatcherPerObserver(final long removalDelay, @NonNull final TimeUnit removalDelayUnit) {
        super(removalDelay, removalDelayUnit);
    }

    @NonNull
    @Override
    protected final Dispatcher get(@NonNull final ContentResolver resolver,
                                   @NonNull final Route route,
                                   @NonNull final Uri uri) {
        Dispatcher dispatcher = null;

        mLock.lock();
        try {
            final int size = mDispatchers.size();
            for (int i = 0; (i < size) && (dispatcher == null); i++) {
                final Dispatcher current = mDispatchers.get(i);
                if (current.isEmpty()) {
                    dispatcher = current;
                }
            }

            if (dispatcher == null) {
                final Manager manager = new Manager.PerObserver(resolver);
                dispatcher = new Dispatcher(manager);
                dispatcher.start();
                mDispatchers.add(dispatcher);
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
            mDispatchers.remove(dispatcher);
        } finally {
            mLock.unlock();
        }
    }
}
