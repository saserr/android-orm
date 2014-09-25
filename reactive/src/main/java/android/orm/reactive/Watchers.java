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

package android.orm.reactive;

import android.content.ContentResolver;
import android.net.Uri;
import android.orm.dao.Result;
import android.orm.model.Mapper;
import android.orm.model.Reading;
import android.orm.reactive.watch.Executor;
import android.orm.reactive.watch.Observable;
import android.orm.reactive.watch.Observer;
import android.orm.reactive.watch.Session;
import android.orm.reactive.watch.Watchable;
import android.orm.reactive.watch.Watcher;
import android.orm.reactive.watch.executor.DispatcherPerObserverExecutor;
import android.orm.reactive.watch.executor.DispatcherPerTableExecutor;
import android.orm.reactive.watch.executor.DispatcherPerUriExecutor;
import android.orm.reactive.watch.executor.LimitedSizeExecutor;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Value;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Where;
import android.orm.util.Cancelable;
import android.orm.util.Lazy;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;
import static java.lang.Runtime.getRuntime;

public abstract class Watchers {

    @NonNls
    private static final String ERROR_STOPPED = "DAO is stopped";

    @NonNull
    private final Session mSession;

    private final Handler mHandler = new Handler();
    private final AtomicBoolean mStopped = new AtomicBoolean(false);

    protected Watchers(@NonNull final ContentResolver resolver,
                       @NonNull final Executor executor) {
        super();

        mSession = executor.session(resolver);
    }

    @NonNull
    protected abstract android.orm.dao.Executor.Direct.Single<?> executor(@NonNull final Route.Single route,
                                                                          @NonNull final Object... arguments);

    @NonNull
    protected abstract android.orm.dao.Executor.Direct.Many<?> executor(@NonNull final Route.Many route,
                                                                        @NonNull final Object... arguments);

    @NonNull
    public final Access.Single at(@NonNull final Route.Single route,
                                  @NonNull final Object... arguments) {
        return new Access.Single(executor(route, arguments), mHandler) {

            private final Uri mUri = route.createUri(arguments);

            @NonNull
            @Override
            public Cancelable onChange(@NonNull final Observer observer) {
                return execute(route, mUri, observer);
            }
        };
    }

    @NonNull
    public final Access.Many at(@NonNull final Route.Many route,
                                @NonNull final Object... arguments) {
        return new Access.Many(executor(route, arguments), mHandler) {

            private final Uri mUri = route.createUri(arguments);

            @NonNull
            @Override
            public Cancelable onChange(@NonNull final Observer observer) {
                return execute(route, mUri, observer);
            }
        };
    }

    public final void start() {
        if (mStopped.get()) {
            throw new UnsupportedOperationException(ERROR_STOPPED);
        }

        mSession.start();
    }

    public final void pause() {
        if (mStopped.get()) {
            throw new UnsupportedOperationException(ERROR_STOPPED);
        }

        mSession.pause();
    }

    public final void stop() {
        if (!mStopped.getAndSet(true)) {
            mSession.stop();
        }
    }

    @NonNull
    private Cancelable execute(@NonNull final Route route,
                               @NonNull final Uri uri,
                               @NonNull final Observer observer) {
        if (mStopped.get()) {
            throw new UnsupportedOperationException(ERROR_STOPPED);
        }

        return mSession.submit(route, uri, observer);
    }

    public interface Executors {

        Lazy<Executor> SingleThread = new Lazy.Volatile<Executor>() {
            @NonNull
            @Override
            protected Executor produce() {
                return new LimitedSizeExecutor(1, 1);
            }
        };

        Lazy<Executor> ThreadPerCore = new Lazy.Volatile<Executor>() {
            @NonNull
            @Override
            protected Executor produce() {
                final int processors = getRuntime().availableProcessors();
                return new LimitedSizeExecutor(processors, processors);
            }
        };

        Lazy<Executor> ThreadPerTable = new Lazy.Volatile<Executor>() {
            @NonNull
            @Override
            protected Executor produce() {
                return new DispatcherPerTableExecutor();
            }
        };

        Lazy<Executor> ThreadPerUri = new Lazy.Volatile<Executor>() {
            @NonNull
            @Override
            protected Executor produce() {
                return new DispatcherPerUriExecutor();
            }
        };

        Lazy<Executor> ThreadPerObserver = new Lazy.Volatile<Executor>() {
            @NonNull
            @Override
            protected Executor produce() {
                return new DispatcherPerObserverExecutor();
            }
        };

        Lazy<Executor> Default = ThreadPerObserver;
    }

    public static final class Access {

        public abstract static class Single implements Observable {

            @NonNull
            private final android.orm.dao.Executor.Direct.Single<?> mExecutor;
            @NonNull
            private final Handler mHandler;

            protected Single(@NonNull final android.orm.dao.Executor.Direct.Single<?> executor,
                             @NonNull final Handler handler) {
                super();

                mExecutor = executor;
                mHandler = handler;
            }

            @NonNull
            public final <M> Query<M> watch(@NonNull final Value.Read<M> value) {
                return watch(single(value));
            }

            @NonNull
            public final <M> Query<M> watch(@NonNull final Mapper.Read<M> mapper) {
                return watch(single(mapper));
            }

            @NonNull
            public final <M> Query<M> watch(@NonNull final Reading.Single<M> reading) {
                return new Query<>(this, mExecutor, mHandler, reading);
            }
        }

        public abstract static class Many implements Observable {

            @NonNull
            private final android.orm.dao.Executor.Direct.Many<?> mExecutor;
            @NonNull
            private final Handler mHandler;

            protected Many(@NonNull final android.orm.dao.Executor.Direct.Many<?> executor,
                           @NonNull final Handler handler) {
                super();

                mExecutor = executor;
                mHandler = handler;
            }

            @NonNull
            public final <M> Query<M> watch(@NonNull final AggregateFunction<M> function) {
                return new Query<>(this, mExecutor, mHandler, single(function));
            }

            @NonNull
            public final <M> Query<List<M>> watch(@NonNull final Value.Read<M> value) {
                return watch(list(value));
            }

            @NonNull
            public final <M> Query<List<M>> watch(@NonNull final Mapper.Read<M> mapper) {
                return watch(list(mapper));
            }

            @NonNull
            public final <M> Query<M> watch(@NonNull final Reading.Many<M> reading) {
                return new Query<>(this, mExecutor, mHandler, reading);
            }
        }

        public static class Query<M> implements Watchable<M> {

            @NonNull
            private final Observable mObservable;
            @NonNull
            private final android.orm.dao.Executor.Direct<?, ?> mExecutor;
            @NonNull
            private final Handler mHandler;
            @NonNull
            private final Reading<M> mReading;

            @NonNull
            private Where mWhere = Where.None;
            @Nullable
            private Order mOrder;
            @Nullable
            private Limit mLimit;
            @Nullable
            private Offset mOffset;
            @Nullable
            private M mModel;

            public Query(@NonNull final Observable observable,
                         @NonNull final android.orm.dao.Executor.Direct<?, ?> executor,
                         @NonNull final Handler handler,
                         @NonNull final Reading<M> reading) {
                super();

                mObservable = observable;
                mExecutor = executor;
                mHandler = handler;
                mReading = reading;
            }

            @NonNull
            public final Query<M> with(@Nullable final Where where) {
                mWhere = (where == null) ? Where.None : where;
                return this;
            }

            @NonNull
            public final Query<M> with(@Nullable final Order order) {
                mOrder = order;
                return this;
            }

            @NonNull
            public final Query<M> with(@Nullable final Limit limit) {
                mLimit = limit;
                return this;
            }

            @NonNull
            public final Query<M> with(@Nullable final Offset offset) {
                mOffset = offset;
                return this;
            }

            @NonNull
            public final Query<M> using(@Nullable final M model) {
                mModel = model;
                return this;
            }

            @NonNull
            @Override
            public final Cancelable onChange(@NonNull final Result.Callback<? super M> callback) {
                return mObservable.onChange(new Watcher<>(mExecutor, mHandler, mModel, mReading, mWhere, mOrder, mLimit, mOffset, callback));
            }
        }

        private Access() {
            super();
        }
    }
}
