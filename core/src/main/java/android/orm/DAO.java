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

import android.content.ContentValues;
import android.content.Context;
import android.orm.dao.ErrorHandler;
import android.orm.dao.Executor;
import android.orm.dao.Result;
import android.orm.dao.Transaction;
import android.orm.sql.Column;
import android.orm.sql.Expression;
import android.orm.sql.Statement;
import android.orm.sql.fragment.Predicate;
import android.orm.sql.table.PrimaryKey;
import android.orm.sql.table.UniqueKey;
import android.orm.util.Lazy;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.concurrent.ExecutorService;

import static android.orm.dao.direct.Executors.many;
import static android.orm.dao.direct.Executors.single;
import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Values.RowId;
import static android.orm.sql.Writables.writable;
import static android.orm.util.Maybes.something;
import static java.lang.Runtime.getRuntime;

public final class DAO {

    private static final Predicate.ComplexPart.WithNull<Long> WHERE_ROW_ID = Predicate.on(RowId);

    @NonNull
    public static Executor.Direct.Single.Factory<android.orm.sql.Executor, Long> byRowId(@NonNls @NonNull final String table,
                                                                                         final long rowId) {
        final Predicate predicate = WHERE_ROW_ID.isEqualTo(rowId);
        final ContentValues onInsert = new ContentValues();
        RowId.write(Insert, something(rowId), writable(onInsert));

        return new Executor.Direct.Single.Factory<android.orm.sql.Executor, Long>() {
            @NonNull
            @Override
            public Executor.Direct.Single<Long> create(@NonNull final android.orm.sql.Executor executor) {
                return single(executor, table, predicate, onInsert, RowId);
            }
        };
    }

    @NonNull
    public static Executor.Direct.Many.Factory<android.orm.sql.Executor, Long> byRowId(@NonNls @NonNull final String table) {
        return new Executor.Direct.Many.Factory<android.orm.sql.Executor, Long>() {
            @NonNull
            @Override
            public Executor.Direct.Many<Long> create(@NonNull final android.orm.sql.Executor executor) {
                return many(executor, table, RowId);
            }
        };
    }

    @NonNull
    public static <K> Executor.Direct.Single.Factory<android.orm.sql.Executor, K> byPrimaryKey(@NonNls @NonNull final String table,
                                                                                               @NonNull final PrimaryKey<K> key,
                                                                                               @Nullable final K value) {
        final Predicate predicate = (value == null) ?
                Predicate.on(key).isNull() :
                Predicate.on(key).isEqualTo(value);
        final ContentValues onInsert = new ContentValues();
        key.write(Insert, something(value), writable(onInsert));

        return new Executor.Direct.Single.Factory<android.orm.sql.Executor, K>() {
            @NonNull
            @Override
            public Executor.Direct.Single<K> create(@NonNull final android.orm.sql.Executor executor) {
                return single(executor, table, predicate, onInsert, key);
            }
        };
    }

    @NonNull
    public static <K> Executor.Direct.Many.Factory<android.orm.sql.Executor, K> byPrimaryKey(@NonNls @NonNull final String table,
                                                                                             @NonNull final PrimaryKey<K> key) {
        return new Executor.Direct.Many.Factory<android.orm.sql.Executor, K>() {
            @NonNull
            @Override
            public Executor.Direct.Many<K> create(@NonNull final android.orm.sql.Executor executor) {
                return many(executor, table, key);
            }
        };
    }

    @NonNull
    public static <V> Executor.Direct.Single.Factory<android.orm.sql.Executor, V> byUniqueColumn(@NonNls @NonNull final String table,
                                                                                                 @NonNull final Column<V> column,
                                                                                                 @Nullable final V value) {
        if (!column.isUnique()) {
            throw new IllegalArgumentException("Column must be unique");
        }

        return byUnique(table, UniqueKey.on(column), value);
    }

    @NonNull
    public static <V> Executor.Direct.Many.Factory<android.orm.sql.Executor, V> byUniqueColumn(@NonNls @NonNull final String table,
                                                                                               @NonNull final Column<V> column) {
        if (!column.isUnique()) {
            throw new IllegalArgumentException("Column must be unique");
        }

        return byUnique(table, UniqueKey.on(column));
    }

    public static <V> Executor.Direct.Single.Factory<android.orm.sql.Executor, V> byUnique(@NonNls @NonNull final String table,
                                                                                           @NonNull final UniqueKey<V> uniqueKey,
                                                                                           @Nullable final V value) {
        final Predicate predicate = (value == null) ?
                Predicate.on(uniqueKey).isNull() :
                Predicate.on(uniqueKey).isEqualTo(value);
        final ContentValues onInsert = new ContentValues();
        uniqueKey.write(Insert, something(value), writable(onInsert));

        return new Executor.Direct.Single.Factory<android.orm.sql.Executor, V>() {
            @NonNull
            @Override
            public Executor.Direct.Single<V> create(@NonNull final android.orm.sql.Executor executor) {
                return single(executor, table, predicate, onInsert, uniqueKey);
            }
        };
    }

    @NonNull
    public static <V> Executor.Direct.Many.Factory<android.orm.sql.Executor, V> byUnique(@NonNls @NonNull final String table,
                                                                                         @NonNull final UniqueKey<V> uniqueKey) {
        return new Executor.Direct.Many.Factory<android.orm.sql.Executor, V>() {
            @NonNull
            @Override
            public Executor.Direct.Many<V> create(@NonNull final android.orm.sql.Executor executor) {
                return many(executor, table, uniqueKey);
            }
        };
    }

    @NonNull
    public static Async create(@NonNull final Context context, @NonNull final Database database) {
        return create(context, database, Executors.Default.get());
    }

    @NonNull
    public static Async create(@NonNull final Context context,
                               @NonNull final Database database,
                               @NonNull final ExecutorService executor) {
        return new android.orm.dao.Async(android.orm.dao.Direct.create(context, database), executor);
    }

    public interface Executors {

        Lazy<ExecutorService> SingleThread = new Lazy.Volatile<ExecutorService>() {
            @NonNull
            @Override
            protected ExecutorService produce() {
                return java.util.concurrent.Executors.newSingleThreadExecutor();
            }
        };

        Lazy<ExecutorService> ThreadPerCore = new Lazy.Volatile<ExecutorService>() {
            @NonNull
            @Override
            protected ExecutorService produce() {
                return java.util.concurrent.Executors.newFixedThreadPool(getRuntime().availableProcessors());
            }
        };

        Lazy<ExecutorService> CacheThreads = new Lazy.Volatile<ExecutorService>() {
            @NonNull
            @Override
            protected ExecutorService produce() {
                return java.util.concurrent.Executors.newCachedThreadPool();
            }
        };

        Lazy<ExecutorService> Default = CacheThreads;
    }

    public interface Direct extends android.orm.sql.Executor {

        @NonNull
        <K> Access.Direct.Single<K> access(@NonNull final Executor.Direct.Single.Factory<? super Direct, K> factory);

        @NonNull
        <K> Access.Direct.Many<K> access(@NonNull final Executor.Direct.Many.Factory<? super Direct, K> factory);

        @NonNull
        <V> Maybe<V> execute(@NonNull final Transaction.Direct<V> transaction);
    }

    public interface Async {

        void setErrorHandler(@Nullable final ErrorHandler handler);

        @NonNull
        <K> Access.Async.Single<K> access(@NonNull final Executor.Direct.Single.Factory<? super Direct, K> factory);

        @NonNull
        <K> Access.Async.Many<K> access(@NonNull final Executor.Direct.Many.Factory<? super Direct, K> factory);

        @NonNull
        Result<Void> execute(@NonNull final Statement statement);

        @NonNull
        <V> Result<V> execute(@NonNull final Expression<V> expression);

        @NonNull
        <V> Result<V> execute(@NonNull final Transaction.Direct<V> transaction);
    }

    private DAO() {
        super();
    }
}
