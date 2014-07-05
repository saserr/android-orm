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

import android.support.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

public class SameThreadExecutorService extends AbstractExecutorService {

    private volatile boolean mStopped = false;

    @Override
    public final void shutdown() {
        mStopped = true;
    }

    @NonNull
    @Override
    public final List<Runnable> shutdownNow() {
        mStopped = true;
        return Collections.emptyList();
    }

    @Override
    public final boolean isShutdown() {
        return mStopped;
    }

    @Override
    public final boolean isTerminated() {
        return mStopped;
    }

    @Override
    public final boolean awaitTermination(final long timeout, @NotNull final TimeUnit unit) {
        shutdown();
        return true;
    }

    @Override
    public final void execute(@NotNull final Runnable command) {
        if (!mStopped) {
            command.run();
        }
    }
}
