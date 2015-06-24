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

package android.orm.dao.direct;

import android.content.ContentValues;
import android.orm.dao.Executor;
import android.orm.sql.Reader;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Predicate;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Helper.escape;
import static android.orm.sql.Select.select;

public final class Executors {

    private static final ContentValues EMPTY = new ContentValues();

    @NonNull
    public static <K> Executor.Direct.Single<K> single(@NonNull final android.orm.sql.Executor executor,
                                                       @NonNls @NonNull final String table,
                                                       @NonNull final Predicate predicate,
                                                       @NonNull final ContentValues onInsert,
                                                       @NonNull final Value.Read<K> key) {
        return new Single<>(executor, escape(table), predicate, onInsert, key);
    }

    @NonNull
    public static <K> Executor.Direct.Many<K> many(@NonNull final android.orm.sql.Executor executor,
                                                   @NonNls @NonNull final String table,
                                                   @NonNull final Value.Read<K> key) {
        return new Many<>(executor, escape(table), Predicate.None, EMPTY, key);
    }

    @NonNull
    public static <K> Executor.Direct.Many<K> many(@NonNull final android.orm.sql.Executor executor,
                                                   @NonNls @NonNull final String table,
                                                   @NonNull final Predicate predicate,
                                                   @NonNull final ContentValues onInsert,
                                                   @NonNull final Value.Read<K> key) {
        return new Many<>(executor, escape(table), predicate, onInsert, key);
    }

    private static class Single<K> extends Some<K, K> implements Executor.Direct.Single<K> {

        @NonNull
        private final android.orm.sql.Executor mExecutor;
        @NonNls
        @NonNull
        private final String mTable;
        @NonNull
        private final Predicate mPredicate;
        @NonNull
        private final ContentValues mOnInsert;
        @NonNull
        private final Value.Read<K> mKey;

        private Single(@NonNull final android.orm.sql.Executor executor,
                       @NonNls @NonNull final String table,
                       @NonNull final Predicate predicate,
                       @NonNull final ContentValues onInsert,
                       @NonNull final Value.Read<K> key) {
            super(executor, table, predicate, onInsert, key);

            mExecutor = executor;
            mTable = table;
            mPredicate = predicate;
            mOnInsert = onInsert;
            mKey = key;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public final <M> Maybe<Producer<Maybe<M>>> query(@NonNull final Reader.Collection<M> reader,
                                                         @NonNull final Predicate predicate,
                                                         @Nullable final Order order,
                                                         @Nullable final Limit limit,
                                                         @Nullable final Offset offset) {
            final Select select = select(mTable)
                    .with(mPredicate.and(predicate))
                    .with(Limit.Single)
                    .build();
            final Query query = Query.Pool.borrow();
            query.init(reader, select);
            return (Maybe<Producer<Maybe<M>>>) (Object) mExecutor.execute(query);
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public final Maybe<K> update(@NonNull final Predicate predicate,
                                     @NonNull final Writer writer) {
            final Update.Single update = Update.Single.Pool.borrow();
            update.init(mTable, mPredicate.and(predicate), writer, mOnInsert, mKey);
            return (Maybe<K>) (Object) mExecutor.execute(update);
        }
    }

    private static class Many<K> extends Some<K, Integer> implements Executor.Direct.Many<K> {

        @NonNull
        private final android.orm.sql.Executor mExecutor;
        @NonNls
        @NonNull
        private final String mTable;
        @NonNull
        private final Predicate mPredicate;

        private Many(@NonNull final android.orm.sql.Executor executor,
                     @NonNls @NonNull final String table,
                     @NonNull final Predicate predicate,
                     @NonNull final ContentValues onInsert,
                     @NonNull final Value.Read<K> key) {
            super(executor, table, predicate, onInsert, key);

            mExecutor = executor;
            mTable = table;
            mPredicate = predicate;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public final <M> Maybe<Producer<Maybe<M>>> query(@NonNull final Reader.Collection<M> reader,
                                                         @NonNull final Predicate predicate,
                                                         @Nullable final Order order,
                                                         @Nullable final Limit limit,
                                                         @Nullable final Offset offset) {
            final Select select = select(mTable)
                    .with(mPredicate.and(predicate))
                    .with(order)
                    .with(limit)
                    .with(offset)
                    .build();
            final Query query = Query.Pool.borrow();
            query.init(reader, select);
            return (Maybe<Producer<Maybe<M>>>) (Object) mExecutor.execute(query);
        }

        @NonNull
        @Override
        public final Maybe<Integer> update(@NonNull final Predicate predicate,
                                           @NonNull final Writer writer) {
            final Update.Many update = Update.Many.Pool.borrow();
            update.init(mTable, mPredicate.and(predicate), writer);
            return mExecutor.execute(update);
        }
    }

    private abstract static class Some<K, U> implements Executor.Direct<K, U> {

        @NonNull
        private final android.orm.sql.Executor mExecutor;
        @NonNls
        @NonNull
        private final String mTable;
        @NonNull
        private final Predicate mPredicate;
        @NonNull
        private final ContentValues mOnInsert;
        @NonNull
        private final Value.Read<K> mKey;

        protected Some(@NonNull final android.orm.sql.Executor executor,
                       @NonNls @NonNull final String table,
                       @NonNull final Predicate predicate,
                       @NonNull final ContentValues onInsert,
                       @NonNull final Value.Read<K> key) {
            super();

            mExecutor = executor;
            mTable = table;
            mPredicate = predicate;
            mOnInsert = onInsert;
            mKey = key;
        }

        @NonNull
        @Override
        public final Maybe<Boolean> exists(@NonNull final Predicate predicate) {
            final Exists exists = Exists.Pool.borrow();
            exists.init(mTable, mPredicate.and(predicate));
            return mExecutor.execute(exists);
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public final Maybe<K> insert(@NonNull final Writer writer) {
            final Insert insert = Insert.Pool.borrow();
            insert.init(mTable, writer, mOnInsert, mKey);
            return (Maybe<K>) (Object) mExecutor.execute(insert);
        }

        @NonNull
        @Override
        public final Maybe<Integer> delete(@NonNull final Predicate predicate) {
            final Delete delete = Delete.Pool.borrow();
            delete.init(mTable, mPredicate.and(predicate));
            return mExecutor.execute(delete);
        }
    }

    private Executors() {
        super();
    }
}
