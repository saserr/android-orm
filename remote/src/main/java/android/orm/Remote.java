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

package android.orm;

import android.content.ContentResolver;
import android.net.Uri;
import android.orm.dao.ErrorHandler;
import android.orm.dao.Executor;
import android.orm.dao.Result;
import android.orm.reactive.Route;
import android.orm.reactive.Watchers;
import android.orm.remote.dao.Executors;
import android.orm.remote.dao.Transaction;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutorService;

import static android.orm.DAO.Executors.Default;

public final class Remote {

    @NonNull
    public static Executor.Direct.Single.Factory<ContentResolver, Uri> at(@NonNull final Route.Single route,
                                                                          @NonNull final Object... arguments) {
        return new Executor.Direct.Single.Factory<ContentResolver, Uri>() {
            @NonNull
            @Override
            public Executor.Direct.Single<Uri> create(@NonNull final ContentResolver resolver) {
                return Executors.create(resolver, route, arguments);
            }
        };
    }

    @NonNull
    public static Executor.Direct.Many.Factory<ContentResolver, Uri> at(@NonNull final Route.Many route,
                                                                        @NonNull final Object... arguments) {
        return new Executor.Direct.Many.Factory<ContentResolver, Uri>() {
            @NonNull
            @Override
            public Executor.Direct.Many<Uri> create(@NonNull final ContentResolver resolver) {
                return Executors.create(resolver, route, arguments);
            }
        };
    }

    @NonNull
    public static Executor.Direct.Many.Factory<ContentResolver, Uri> at(@NonNull final Uri uri) {
        return new Executor.Direct.Many.Factory<ContentResolver, Uri>() {
            @NonNull
            @Override
            public Executor.Direct.Many<Uri> create(@NonNull final ContentResolver resolver) {
                return Executors.create(resolver, uri);
            }
        };
    }

    @NonNull
    public static Async create(@NonNull final ContentResolver resolver) {
        return create(resolver, Default.get());
    }

    @NonNull
    public static Async create(@NonNull final ContentResolver resolver,
                               @NonNull final ExecutorService executor) {
        return new android.orm.remote.dao.Async(resolver, executor);
    }

    public interface Direct {

        @NonNull
        Access.Direct.Single<Uri> at(@NonNull final Route.Single route,
                                     @NonNull final Object... arguments);

        @NonNull
        Access.Direct.Many<Uri> at(@NonNull final Route.Many route,
                                   @NonNull final Object... arguments);

        @NonNull
        <K> Access.Direct.Single<K> access(@NonNull final Executor.Direct.Single.Factory<ContentResolver, K> factory);

        @NonNull
        <K> Access.Direct.Many<K> access(@NonNull final Executor.Direct.Many.Factory<ContentResolver, K> factory);

        @NonNull
        Transaction<Maybe<Transaction.CommitResult>> transaction();
    }

    public interface Async {

        void setErrorHandler(@Nullable final ErrorHandler handler);

        @NonNull
        Access.Async.Single<Uri> at(@NonNull final Route.Single route,
                                    @NonNull final Object... arguments);

        @NonNull
        Access.Async.Many<Uri> at(@NonNull final Route.Many route,
                                  @NonNull final Object... arguments);

        @NonNull
        <K> Access.Async.Single<K> access(@NonNull final Executor.Direct.Single.Factory<ContentResolver, K> factory);

        @NonNull
        <K> Access.Async.Many<K> access(@NonNull final Executor.Direct.Many.Factory<ContentResolver, K> factory);

        @NonNull
        Transaction<Result<Transaction.CommitResult>> transaction();

        @NonNull
        Watchers watchers();

        @NonNull
        Watchers watchers(@NonNull final android.orm.reactive.watch.Executor executor);
    }

    private Remote() {
        super();
    }
}
