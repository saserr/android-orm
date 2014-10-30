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
import android.orm.model.Plan;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Where;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Select.select;

public final class Executors {

    private static final ContentValues EMPTY = new ContentValues();

    @NonNull
    public static <K> Executor.Direct.Single<K> single(@NonNull final android.orm.sql.Executor executor,
                                                       @NonNls @NonNull final String table,
                                                       @NonNull final Where where,
                                                       @NonNull final ContentValues onInsert,
                                                       @NonNull final Value.Read<K> key) {
        return new Single<>(executor, table, where, onInsert, key);
    }

    @NonNull
    public static <K> Executor.Direct.Many<K> many(@NonNull final android.orm.sql.Executor executor,
                                                   @NonNls @NonNull final String table,
                                                   @NonNull final Value.Read<K> key) {
        return many(executor, table, Where.None, EMPTY, key);
    }

    @NonNull
    public static <K> Executor.Direct.Many<K> many(@NonNull final android.orm.sql.Executor executor,
                                                   @NonNls @NonNull final String table,
                                                   @NonNull final Where where,
                                                   @NonNull final ContentValues onInsert,
                                                   @NonNull final Value.Read<K> key) {
        return new Many<>(executor, table, where, onInsert, key);
    }

    private static class Single<K> extends Some<K, K> implements Executor.Direct.Single<K> {

        @NonNull
        private final android.orm.sql.Executor mExecutor;
        @NonNls
        @NonNull
        private final String mTable;
        @NonNull
        private final Where mWhere;
        @NonNull
        private final ContentValues mOnInsert;
        @NonNull
        private final Value.Read<K> mKey;

        private Single(@NonNull final android.orm.sql.Executor executor,
                       @NonNls @NonNull final String table,
                       @NonNull final Where where,
                       @NonNull final ContentValues onInsert,
                       @NonNull final Value.Read<K> key) {
            super(executor, table, where, onInsert, key);

            mExecutor = executor;
            mTable = table;
            mWhere = where;
            mOnInsert = onInsert;
            mKey = key;
        }

        @NonNull
        public final <M> Maybe<Producer<Maybe<M>>> query(@NonNull final Plan.Read<M> plan,
                                                         @NonNull final Where where,
                                                         @Nullable final Order order,
                                                         @Nullable final Limit limit,
                                                         @Nullable final Offset offset) {
            final Select select = select(mTable)
                    .with(mWhere.and(where))
                    .with(Limit.Single)
                    .build();
            return mExecutor.execute(new Query<>(plan, select));
        }

        @NonNull
        @Override
        public final Maybe<K> update(@NonNull final Where where,
                                     @NonNull final Plan.Write plan) {
            return plan.isEmpty() ?
                    Maybes.<K>nothing() :
                    mExecutor.execute(new Update.Single<>(mTable, mWhere.and(where), plan, mOnInsert, mKey));
        }
    }

    private static class Many<K> extends Some<K, Integer> implements Executor.Direct.Many<K> {

        @NonNull
        private final android.orm.sql.Executor mExecutor;
        @NonNls
        @NonNull
        private final String mTable;
        @NonNull
        private final Where mWhere;

        private Many(@NonNull final android.orm.sql.Executor executor,
                     @NonNls @NonNull final String table,
                     @NonNull final Where where,
                     @NonNull final ContentValues onInsert,
                     @NonNull final Value.Read<K> key) {
            super(executor, table, where, onInsert, key);

            mExecutor = executor;
            mTable = table;
            mWhere = where;
        }

        @NonNull
        public final <M> Maybe<Producer<Maybe<M>>> query(@NonNull final Plan.Read<M> plan,
                                                         @NonNull final Where where,
                                                         @Nullable final Order order,
                                                         @Nullable final Limit limit,
                                                         @Nullable final Offset offset) {
            final Select select = select(mTable)
                    .with(mWhere.and(where))
                    .with(order)
                    .with(limit)
                    .with(offset)
                    .build();
            return mExecutor.execute(new Query<>(plan, select));
        }

        @NonNull
        @Override
        public final Maybe<Integer> update(@NonNull final Where where,
                                           @NonNull final Plan.Write plan) {
            return plan.isEmpty() ?
                    Maybes.<Integer>nothing() :
                    mExecutor.execute(new Update.Many(mTable, mWhere.and(where), plan));
        }
    }

    private abstract static class Some<K, U> implements Executor.Direct<K, U> {

        @NonNull
        private final android.orm.sql.Executor mExecutor;
        @NonNls
        @NonNull
        private final String mTable;
        @NonNull
        private final Where mWhere;
        @NonNull
        private final ContentValues mOnInsert;
        @NonNull
        private final Value.Read<K> mKey;

        private Some(@NonNull final android.orm.sql.Executor executor,
                     @NonNls @NonNull final String table,
                     @NonNull final Where where,
                     @NonNull final ContentValues onInsert,
                     @NonNull final Value.Read<K> key) {
            super();

            mExecutor = executor;
            mTable = table;
            mWhere = where;
            mOnInsert = onInsert;
            mKey = key;
        }

        @NonNull
        @Override
        public final Maybe<Boolean> exists(@NonNull final Where where) {
            return mExecutor.execute(new Exists(mTable, mWhere.and(where)));
        }

        @NonNull
        @Override
        public final Maybe<K> insert(@NonNull final Plan.Write plan) {
            return plan.isEmpty() ?
                    Maybes.<K>nothing() :
                    mExecutor.execute(new Insert<>(mTable, plan, mOnInsert, mKey));
        }

        @NonNull
        @Override
        public final Maybe<Integer> delete(@NonNull final Where where) {
            return mExecutor.execute(new Delete(mTable, mWhere.and(where)));
        }
    }

    private Executors() {
        super();
    }
}
