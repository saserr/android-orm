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

package android.orm.util;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public abstract class ObjectPool<E> {

    private static final String TAG = ObjectPool.class.getSimpleName();
    private static final int START_CAPACITY = 10;
    private static final int MAX_SIZE = 100;

    private final Semaphore mSemaphore = new Semaphore(1);
    private final List<Receipt<E>> mReceipts = new ArrayList<>(START_CAPACITY);

    protected ObjectPool() {
        super();
    }

    @NonNull
    protected abstract E produce(@NonNull final Receipt<E> receipt);

    @NonNull
    public final E borrow() {
        Receipt<E> result;

        try {
            mSemaphore.acquire();
            try {
                result = mReceipts.isEmpty() ?
                        new LazyReceipt<>(this) :
                        mReceipts.remove(mReceipts.size() - 1);
            } finally {
                mSemaphore.release();
            }
        } catch (final InterruptedException ex) {
            Log.w(TAG, "Interrupted while borrowing an element from the object pool! Will borrow a new element", ex); //NON-NLS
            result = new LazyReceipt<>(this);
        }

        return result.get();
    }

    private void yield(@NonNull final Receipt<E> receipt) {
        try {
            mSemaphore.acquire();
            try {
                if (mReceipts.size() < MAX_SIZE) {
                    mReceipts.add(receipt);
                }
            } finally {
                mSemaphore.release();
            }
        } catch (final InterruptedException ex) {
            Log.w(TAG, "Interrupted while returning an element to the object pool! Will not return the element", ex); //NON-NLS
        }
    }

    public interface Receipt<E> {

        @NonNull
        E get();

        void yield();
    }

    private static class LazyReceipt<E> extends Lazy.Volatile<E> implements Receipt<E> {

        @NonNull
        private final ObjectPool<E> mPool;

        private LazyReceipt(@NonNull final ObjectPool<E> pool) {
            super();

            mPool = pool;
        }

        @NonNull
        @Override
        protected final E produce() {
            return mPool.produce(this);
        }

        @Override
        public final void yield() {
            mPool.yield(this);
        }
    }
}
