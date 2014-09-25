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

package android.orm.reactive.watch;

import android.net.Uri;
import android.orm.reactive.Route;
import android.orm.util.Cancelable;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Dispatcher {

    private static final String TAG = Dispatcher.class.getSimpleName();

    @NonNull
    private final Manager mManager;
    @NonNull
    private final Loop mLoop;

    public Dispatcher(@NonNull final Manager manager) {
        super();

        mManager = manager;
        mLoop = new Loop(manager);
    }

    public final boolean isEmpty() {
        return mManager.isEmpty();
    }

    public final int size() {
        return mManager.size();
    }

    public final void start() {
        new Thread(mLoop).start();
    }

    public final void stop() {
        mLoop.stop();
    }

    @NonNull
    public final Cancelable submit(@NonNull final Route route,
                                   @NonNull final Uri uri,
                                   @NonNull final Observer observer) {
        return mManager.submit(route, uri, observer);
    }

    public final boolean execute(@NonNull final Runnable command) {
        return mLoop.execute(command);
    }

    public final boolean schedule(final long delay,
                                  @NonNull final TimeUnit unit,
                                  @NonNull final Runnable command) {
        return mLoop.schedule(delay, unit, command);
    }

    private static class Loop implements Runnable {

        @NonNull
        private final Callback mCallback;

        private final AtomicReference<Handler> mHandler = new AtomicReference<>();

        private Loop(@NonNull final Callback callback) {
            super();

            mCallback = callback;
        }

        @Override
        public final void run() {
            Looper.prepare();

            final Looper looper = Looper.myLooper();
            final Handler current = new Handler(looper);
            if (mHandler.compareAndSet(null, current)) {
                mCallback.onStart(current);
                try {
                    Looper.loop();
                } finally {
                    mCallback.onStop();
                }
            } else {
                looper.quit();
                Log.w(TAG, "Loop thread has been unexpectedly started twice. Stopping the newer thread!"); //NON-NLS
            }
        }

        public final void stop() {
            final Handler current = mHandler.getAndSet(null);
            if (current != null) {
                current.getLooper().quit();
            }
        }

        public final boolean execute(@NonNull final Runnable command) {
            boolean success = false;

            final Handler current = mHandler.get();
            if (current != null) {
                success = current.post(command);
            }

            return success;
        }

        public final boolean schedule(final long delay,
                                      @NonNull final TimeUnit unit,
                                      @NonNull final Runnable command) {
            boolean success = false;

            final Handler current = mHandler.get();
            if (current != null) {
                success = current.postDelayed(command, unit.toMillis(delay));
            }

            return success;
        }
    }

    public interface Callback {

        void onStart(@NonNull final Handler handler);

        void onStop();
    }
}
