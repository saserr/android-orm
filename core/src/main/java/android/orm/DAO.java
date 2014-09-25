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
import android.orm.sql.PrimaryKey;
import android.orm.sql.Statement;
import android.orm.sql.Table;
import android.orm.sql.Value;
import android.orm.sql.fragment.Where;
import android.orm.util.Lazy;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutorService;

import static android.orm.dao.direct.Executors.many;
import static android.orm.dao.direct.Executors.single;
import static android.orm.sql.Table.ROW_ID;
import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Writables.writable;
import static android.orm.sql.fragment.Where.where;
import static android.orm.util.Maybes.something;
import static java.lang.Runtime.getRuntime;

public final class DAO {

    private static final Where.SimplePart<Long> WHERE_ROW_ID = where(ROW_ID);

    @NonNull
    public static Executor.Direct.Single.Factory<android.orm.sql.Executor, Long> byRowId(@NonNull final Table<?> table,
                                                                                         final long rowId) {
        return new Executor.Direct.Single.Factory<android.orm.sql.Executor, Long>() {
            @NonNull
            @Override
            public Executor.Direct.Single<Long> create(@NonNull final android.orm.sql.Executor executor) {
                final Where where = WHERE_ROW_ID.isEqualTo(rowId);
                final ContentValues onInsert = new ContentValues();
                ROW_ID.write(Insert, something(rowId), writable(onInsert));
                return single(executor, table, where, onInsert, ROW_ID);
            }
        };
    }

    @NonNull
    public static Executor.Direct.Many.Factory<android.orm.sql.Executor, Long> byRowId(@NonNull final Table<?> table) {
        return new Executor.Direct.Many.Factory<android.orm.sql.Executor, Long>() {
            @NonNull
            @Override
            public Executor.Direct.Many<Long> create(@NonNull final android.orm.sql.Executor executor) {
                return many(executor, table, ROW_ID);
            }
        };
    }

    @NonNull
    public static <K> Executor.Direct.Single.Factory<android.orm.sql.Executor, K> byPrimaryKey(@NonNull final Table<K> table,
                                                                                               @NonNull final K key) {
        return new Executor.Direct.Single.Factory<android.orm.sql.Executor, K>() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public Executor.Direct.Single<K> create(@NonNull final android.orm.sql.Executor executor) {
                final PrimaryKey<K> primaryKey = table.getPrimaryKey();
                final Executor.Direct.Single<K> result;

                if (primaryKey == null) {
                    final Where where = where((Column<K>) ROW_ID).isEqualTo(key);
                    final ContentValues onInsert = new ContentValues();
                    ((Value.Write<K>) ROW_ID).write(Insert, something(key), writable(onInsert));
                    result = single(executor, table, where, onInsert, (Value.Read<K>) ROW_ID);
                } else {
                    final Where where = where(primaryKey).isEqualTo(key);
                    final ContentValues onInsert = new ContentValues();
                    primaryKey.write(Insert, something(key), writable(onInsert));
                    result = single(executor, table, where, onInsert, primaryKey);
                }

                return result;
            }
        };
    }

    @NonNull
    public static <K> Executor.Direct.Many.Factory<android.orm.sql.Executor, K> byPrimaryKey(@NonNull final Table<K> table) {
        return new Executor.Direct.Many.Factory<android.orm.sql.Executor, K>() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public Executor.Direct.Many<K> create(@NonNull final android.orm.sql.Executor executor) {
                final PrimaryKey<K> primaryKey = table.getPrimaryKey();
                final Value.Read<K> key = (primaryKey == null) ? (Value.Read<K>) ROW_ID : primaryKey;
                return many(executor, table, key);
            }
        };
    }

    @NonNull
    public static <K, V> Executor.Direct.Single.Factory<android.orm.sql.Executor, V> byUniqueColumn(@NonNull final Table<K> table,
                                                                                                    @NonNull final Column<V> column,
                                                                                                    @NonNull final V value) {
        if (!column.isUnique()) {
            throw new IllegalArgumentException("Column must be unique");
        }

        return new Executor.Direct.Single.Factory<android.orm.sql.Executor, V>() {
            @NonNull
            @Override
            public Executor.Direct.Single<V> create(@NonNull final android.orm.sql.Executor executor) {
                final Where where = where(column).isEqualTo(value);
                final ContentValues onInsert = new ContentValues();
                column.write(Insert, something(value), writable(onInsert));
                return single(executor, table, where, onInsert, column);
            }
        };
    }

    @NonNull
    public static <K, V> Executor.Direct.Many.Factory<android.orm.sql.Executor, V> byUniqueColumn(@NonNull final Table<K> table,
                                                                                                  @NonNull final Column<V> column) {
        if (!column.isUnique()) {
            throw new IllegalArgumentException("Column must be unique");
        }

        return new Executor.Direct.Many.Factory<android.orm.sql.Executor, V>() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public Executor.Direct.Many<V> create(@NonNull final android.orm.sql.Executor executor) {
                return many(executor, table, column);
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
