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

import java.lang.ref.SoftReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public interface Lazy<V> {

    @NonNull
    V get();

    abstract class Volatile<V> implements Lazy<V> {

        @NonNull
        private volatile SoftReference<V> mValue = new SoftReference<>(null);

        protected Volatile() {
            super();
        }

        @NonNull
        protected abstract V produce();

        @NonNull
        @Override
        public final V get() {
            if (mValue.get() == null) {
                mValue = new SoftReference<>(produce());
            }

            return mValue.get();
        }
    }

    abstract class Synchronized<V> {

        @NonNull
        private SoftReference<V> mValue = new SoftReference<>(null);
        @NonNull
        private final Lock mLock = new ReentrantLock();

        protected Synchronized() {
            super();
        }

        @NonNull
        protected abstract V produce();

        @NonNull
        public final V get() {
            final V value;

            mLock.lock();
            try {
                final V v = mValue.get();
                if (v == null) {
                    value = produce();
                    mValue = new SoftReference<>(value);
                } else {
                    value = v;
                }
            } finally {
                mLock.unlock();
            }

            return value;
        }
    }
}
