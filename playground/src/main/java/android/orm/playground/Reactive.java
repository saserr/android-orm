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

package android.orm.playground;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.orm.Access;
import android.orm.DAO;
import android.orm.Database;
import android.orm.dao.ErrorHandler;
import android.orm.dao.Executor;
import android.orm.dao.Result;
import android.orm.playground.reactive.Transaction;
import android.orm.remote.Route;
import android.orm.sql.Expression;
import android.orm.sql.Statement;
import android.orm.sql.fragment.Where;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutorService;

import static android.orm.DAO.Executors.Default;
import static android.orm.dao.direct.Executors.many;
import static android.orm.dao.direct.Executors.single;

public final class Reactive {

    @NonNull
    public static Executor.Direct.Single.Factory<android.orm.sql.Executor, Uri> at(@NonNull final Route.Single route,
                                                                                   @NonNull final Object... arguments) {
        return new Executor.Direct.Single.Factory<android.orm.sql.Executor, Uri>() {
            @NonNull
            @Override
            public Executor.Direct.Single<Uri> create(@NonNull final android.orm.sql.Executor executor) {
                final String table = route.getTable();
                final Where where = route.createWhere(arguments);
                final ContentValues onInsert = route.createValues(arguments);
                final Route.Single key = route.getSingleRoute();
                return single(executor, table, where, onInsert, key);
            }
        };
    }

    @NonNull
    public static Executor.Direct.Many.Factory<android.orm.sql.Executor, Uri> at(@NonNull final Route.Many route,
                                                                                 @NonNull final Object... arguments) {
        return new Executor.Direct.Many.Factory<android.orm.sql.Executor, Uri>() {
            @NonNull
            @Override
            public Executor.Direct.Many<Uri> create(@NonNull final android.orm.sql.Executor executor) {
                final String table = route.getTable();
                final Where where = route.createWhere(arguments);
                final ContentValues onInsert = route.createValues(arguments);
                final Route.Single key = route.getSingleRoute();
                return many(executor, table, where, onInsert, key);
            }
        };
    }

    @NonNull
    public static Async create(@NonNull final Context context, @NonNull final Database database) {
        return create(context, database, Default.get());
    }

    @NonNull
    public static Async create(@NonNull final Context context,
                               @NonNull final Database database,
                               @NonNull final ExecutorService executor) {
        final DAO.Direct dao = android.orm.dao.Direct.create(context, database);
        final ContentResolver resolver = context.getContentResolver();
        return new android.orm.playground.reactive.Async(dao, resolver, executor);
    }

    public interface Direct extends android.orm.sql.Executor {

        @NonNull
        Access.Direct.Single<Uri> at(@NonNull final Route.Single route,
                                     @NonNull final Object... arguments);

        @NonNull
        Access.Direct.Many<Uri> at(@NonNull final Route.Many route,
                                   @NonNull final Object... arguments);

        @NonNull
        Access.Direct.Single<Uri> access(@NonNull final Uri uri,
                                         @NonNull final Executor.Direct.Single.Factory<? super Direct, Uri> factory);

        @NonNull
        Access.Direct.Many<Uri> access(@NonNull final Uri uri,
                                       @NonNull final Executor.Direct.Many.Factory<? super Direct, Uri> factory);

        @NonNull
        <V> Maybe<V> execute(@NonNull final Transaction.Direct<V> transaction);
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
        Access.Async.Single<Uri> access(@NonNull final Uri uri,
                                        @NonNull final Executor.Direct.Single.Factory<? super Direct, Uri> factory);

        @NonNull
        Access.Async.Many<Uri> access(@NonNull final Uri uri,
                                      @NonNull final Executor.Direct.Many.Factory<? super Direct, Uri> factory);

        @NonNull
        Result<Void> execute(@NonNull final Statement statement);

        @NonNull
        <V> Result<V> execute(@NonNull final Expression<V> expression);

        @NonNull
        <V> Result<V> execute(@NonNull final Transaction.Direct<V> transaction);
    }

    private Reactive() {
        super();
    }
}
