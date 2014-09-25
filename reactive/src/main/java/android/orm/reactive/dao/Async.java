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

package android.orm.reactive.dao;

import android.content.ContentResolver;
import android.net.Uri;
import android.orm.Access;
import android.orm.DAO;
import android.orm.Reactive;
import android.orm.dao.ErrorHandler;
import android.orm.dao.Executor;
import android.orm.dao.Result;
import android.orm.reactive.Route;
import android.orm.reactive.Watchers;
import android.orm.sql.Expression;
import android.orm.sql.Statement;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutorService;

import static android.orm.dao.async.Executors.create;
import static android.orm.reactive.dao.Executors.create;

public class Async implements Reactive.Async {

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final Direct mDirectDAO;
    @NonNull
    private final android.orm.dao.async.Executor mExecutor;

    public Async(@NonNull final DAO.Direct dao,
                 @NonNull final ContentResolver resolver,
                 @NonNull final ExecutorService executor) {
        super();

        mResolver = resolver;
        mDirectDAO = new Direct.OutsideTransaction(dao, resolver);
        mExecutor = new android.orm.dao.async.Executor(executor);
    }

    @Override
    public final void setErrorHandler(@Nullable final ErrorHandler handler) {
        mExecutor.setErrorHandler(handler);
    }

    @NonNull
    @Override
    public final Access.Async.Single<Uri> at(@NonNull final Route.Single route,
                                             @NonNull final Object... arguments) {
        return access(route.createUri(arguments), Reactive.at(route, arguments));
    }

    @NonNull
    @Override
    public final Access.Async.Many<Uri> at(@NonNull final Route.Many route,
                                           @NonNull final Object... arguments) {
        return access(route.createUri(arguments), Reactive.at(route, arguments));
    }

    @NonNull
    @Override
    public final Access.Async.Single<Uri> access(@NonNull final Uri uri,
                                                 @NonNull final Executor.Direct.Single.Factory<? super Reactive.Direct, Uri> factory) {
        final Notifier notifier = mDirectDAO.getNotifier();
        final Executor.Direct.Single<Uri> executor = create(factory.create(mDirectDAO), notifier, uri);
        return new android.orm.dao.async.Access.Single<>(create(mExecutor, executor));
    }

    @NonNull
    @Override
    public final Access.Async.Many<Uri> access(@NonNull final Uri uri,
                                               @NonNull final Executor.Direct.Many.Factory<? super Reactive.Direct, Uri> factory) {
        final Notifier notifier = mDirectDAO.getNotifier();
        final Executor.Direct.Many<Uri> executor = create(factory.create(mDirectDAO), notifier, uri);
        return new android.orm.dao.async.Access.Many<>(create(mExecutor, executor));
    }

    @NonNull
    @Override
    public final Result<Void> execute(@NonNull final Statement statement) {
        return mExecutor.execute(new Producer<Maybe<Void>>() {
            @NonNull
            @Override
            public Maybe<Void> produce() {
                mDirectDAO.execute(statement);
                return Maybes.nothing();
            }
        });
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Expression<V> expression) {
        return mExecutor.execute(new Producer<Maybe<V>>() {
            @NonNull
            @Override
            public Maybe<V> produce() {
                return mDirectDAO.execute(expression);
            }
        });
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Transaction.Direct<V> transaction) {
        return mExecutor.execute(new Producer<Maybe<V>>() {
            @NonNull
            @Override
            public Maybe<V> produce() {
                return mDirectDAO.execute(transaction);
            }
        });
    }

    @NonNull
    @Override
    public final Watchers watchers() {
        return watchers(Watchers.Executors.Default.get());
    }

    @NonNull
    @Override
    public final Watchers watchers(@NonNull final android.orm.reactive.watch.Executor executor) {
        return new Watchers(mResolver, executor) {

            @NonNull
            @Override
            protected Executor.Direct.Single<?> executor(@NonNull final Route.Single route,
                                                         @NonNull final Object... arguments) {
                final Uri uri = route.createUri(arguments);
                final Notifier notifier = mDirectDAO.getNotifier();
                return create(Reactive.at(route, arguments).create(mDirectDAO), notifier, uri);
            }

            @NonNull
            @Override
            protected Executor.Direct.Many<?> executor(@NonNull final Route.Many route,
                                                       @NonNull final Object... arguments) {
                final Uri uri = route.createUri(arguments);
                final Notifier notifier = mDirectDAO.getNotifier();
                return create(Reactive.at(route, arguments).create(mDirectDAO), notifier, uri);
            }
        };
    }
}
