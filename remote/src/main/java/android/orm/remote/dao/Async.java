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

package android.orm.remote.dao;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.net.Uri;
import android.orm.Access;
import android.orm.Remote;
import android.orm.dao.ErrorHandler;
import android.orm.dao.Executor;
import android.orm.dao.Result;
import android.orm.dao.async.ExecutionContext;
import android.orm.reactive.Route;
import android.orm.reactive.Watchers;
import android.orm.remote.dao.direct.Apply;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import static android.orm.dao.async.Executors.create;

public class Async implements Remote.Async {

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final ExecutionContext mExecutionContext;
    @NonNull
    private final Apply mApply;

    public Async(@NonNull final ContentResolver resolver,
                 @NonNull final ExecutorService executor) {
        super();

        mResolver = resolver;
        mExecutionContext = new ExecutionContext(executor);
        mApply = new Apply(resolver);
    }

    @Override
    public final void setErrorHandler(@Nullable final ErrorHandler handler) {
        mExecutionContext.setErrorHandler(handler);
    }

    @NonNull
    @Override
    public final Access.Async.Single<Uri> at(@NonNull final Route.Single route,
                                             @NonNull final Object... arguments) {
        return access(Remote.at(route, arguments));
    }

    @NonNull
    @Override
    public final Access.Async.Many<Uri> at(@NonNull final Route.Many route,
                                           @NonNull final Object... arguments) {
        return access(Remote.at(route, arguments));
    }

    @NonNull
    @Override
    public final Access.Async.Many<Uri> at(@NonNull final Uri uri) {
        return access(Remote.at(uri));
    }

    @NonNull
    @Override
    public final <K> Access.Async.Single<K> access(@NonNull final Executor.Direct.Single.Factory<ContentResolver, K> factory) {
        final Executor.Async.Single<K> executor = create(mExecutionContext, factory.create(mResolver));
        return new android.orm.dao.async.Access.Single<>(executor);
    }

    @NonNull
    @Override
    public final <K> Access.Async.Many<K> access(@NonNull final Executor.Direct.Many.Factory<ContentResolver, K> factory) {
        final Executor.Async.Many<K> executor = create(mExecutionContext, factory.create(mResolver));
        return new android.orm.dao.async.Access.Many<>(executor);
    }

    @NonNull
    @Override
    public final Transaction<Result<Transaction.CommitResult>> transaction() {
        return new Transaction<Result<Transaction.CommitResult>>() {
            @NonNull
            @Override
            protected Result<Transaction.CommitResult> commit(@NonNls @Nullable final String authority,
                                                              @NonNull final Collection<Producer<ContentProviderOperation>> batch) {
                return ((authority == null) || batch.isEmpty()) ?
                        Result.<Transaction.CommitResult>nothing() :
                        mExecutionContext.execute(new ExecutionContext.Task<Transaction.CommitResult>() {
                            @NonNull
                            @Override
                            public Maybe<Transaction.CommitResult> run() {
                                return mApply.invoke(Pair.create(authority, batch));
                            }
                        });
            }
        };
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
                return Remote.at(route, arguments).create(mResolver);
            }

            @NonNull
            @Override
            protected Executor.Direct.Many<?> executor(@NonNull final Route.Many route,
                                                       @NonNull final Object... arguments) {
                return Remote.at(route, arguments).create(mResolver);
            }
        };
    }
}
