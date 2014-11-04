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

package android.orm.playground.reactive;

import android.content.ContentResolver;
import android.net.Uri;
import android.orm.Access;
import android.orm.DAO;
import android.orm.dao.ErrorHandler;
import android.orm.dao.Executor;
import android.orm.dao.Result;
import android.orm.dao.async.ExecutionContext;
import android.orm.playground.Reactive;
import android.orm.remote.Route;
import android.orm.sql.Expression;
import android.orm.sql.Statement;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutorService;

import static android.orm.dao.async.Executors.create;

public class Async implements Reactive.Async {

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final Direct mDirectDAO;
    @NonNull
    private final ExecutionContext mExecutionContext;

    public Async(@NonNull final DAO.Direct dao,
                 @NonNull final ContentResolver resolver,
                 @NonNull final ExecutorService executor) {
        super();

        mResolver = resolver;
        mDirectDAO = new Direct.OutsideTransaction(dao, resolver);
        mExecutionContext = new ExecutionContext(executor);
    }

    @Override
    public final void setErrorHandler(@Nullable final ErrorHandler handler) {
        mExecutionContext.setErrorHandler(handler);
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
        final Executor.Direct.Single<Uri> executor = Executors.create(factory.create(mDirectDAO), notifier, uri);
        return new android.orm.dao.async.Access.Single<>(create(mExecutionContext, executor));
    }

    @NonNull
    @Override
    public final Access.Async.Many<Uri> access(@NonNull final Uri uri,
                                               @NonNull final Executor.Direct.Many.Factory<? super Reactive.Direct, Uri> factory) {
        final Notifier notifier = mDirectDAO.getNotifier();
        final Executor.Direct.Many<Uri> executor = Executors.create(factory.create(mDirectDAO), notifier, uri);
        return new android.orm.dao.async.Access.Many<>(create(mExecutionContext, executor));
    }

    @NonNull
    @Override
    public final Result<Void> execute(@NonNull final Statement statement) {
        return mExecutionContext.execute(new ExecutionContext.Task<Void>() {
            @NonNull
            @Override
            public Maybe<Void> run() {
                mDirectDAO.execute(statement);
                return Maybes.nothing();
            }
        });
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Expression<V> expression) {
        return mExecutionContext.execute(new ExecutionContext.Task<V>() {
            @NonNull
            @Override
            public Maybe<V> run() {
                return mDirectDAO.execute(expression);
            }
        });
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Transaction.Direct<V> transaction) {
        return mExecutionContext.execute(new ExecutionContext.Task<V>() {
            @NonNull
            @Override
            public Maybe<V> run() {
                return mDirectDAO.execute(transaction);
            }
        });
    }
}
