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
import android.orm.remote.Route;
import android.orm.remote.dao.direct.Apply;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import static android.orm.dao.Result.nothing;
import static android.orm.dao.async.Executors.create;
import static android.orm.remote.dao.Executors.many;
import static android.orm.remote.dao.Executors.single;

public class Async implements Remote.Async {

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final ExecutionContext mExecutionContext;

    public Async(@NonNull final ContentResolver resolver,
                 @NonNull final ExecutorService executor) {
        super();

        mResolver = resolver;
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
        final Executor.Direct.Single<Uri> executor = single(mResolver, route, arguments);
        return new android.orm.dao.async.Access.Single<>(create(mExecutionContext, executor));
    }

    @NonNull
    @Override
    public final Access.Async.Many<Uri> at(@NonNull final Route.Many route,
                                           @NonNull final Object... arguments) {
        final Executor.Direct.Many<Uri> executor = many(mResolver, route, arguments);
        return new android.orm.dao.async.Access.Many<>(create(mExecutionContext, executor));
    }

    @NonNull
    @Override
    public final Access.Async.Many<Uri> at(@NonNull final Uri uri) {
        final Executor.Direct.Many<Uri> executor = many(mResolver, uri);
        return new android.orm.dao.async.Access.Many<>(create(mExecutionContext, executor));
    }

    @NonNull
    @Override
    public final Transaction<Result<Transaction.CommitResult>> transaction() {
        return new Transaction<Result<Transaction.CommitResult>>() {
            @NonNull
            @Override
            protected Result<Transaction.CommitResult> commit(@NonNls @Nullable final String authority,
                                                              @NonNull final Collection<Producer<ContentProviderOperation>> batch) {
                final Result<CommitResult> result;

                if ((authority == null) || batch.isEmpty()) {
                    result = nothing();
                } else {
                    final Apply apply = Apply.Pool.borrow();
                    apply.init(mResolver, authority, batch);
                    result = mExecutionContext.execute(apply);
                }

                return result;
            }
        };
    }
}
