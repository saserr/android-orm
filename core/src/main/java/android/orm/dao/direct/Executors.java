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
import android.orm.sql.fragment.Condition;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
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
                                                       @NonNull final Condition condition,
                                                       @NonNull final ContentValues onInsert,
                                                       @NonNull final Value.Read<K> key) {
        return new Single<>(executor, escape(table), condition, onInsert, key);
    }

    @NonNull
    public static <K> Executor.Direct.Many<K> many(@NonNull final android.orm.sql.Executor executor,
                                                   @NonNls @NonNull final String table,
                                                   @NonNull final Value.Read<K> key) {
        return new Many<>(executor, escape(table), Condition.None, EMPTY, key);
    }

    @NonNull
    public static <K> Executor.Direct.Many<K> many(@NonNull final android.orm.sql.Executor executor,
                                                   @NonNls @NonNull final String table,
                                                   @NonNull final Condition condition,
                                                   @NonNull final ContentValues onInsert,
                                                   @NonNull final Value.Read<K> key) {
        return new Many<>(executor, escape(table), condition, onInsert, key);
    }

    private static class Single<K> extends Some<K, K> implements Executor.Direct.Single<K> {

        @NonNull
        private final android.orm.sql.Executor mExecutor;
        @NonNls
        @NonNull
        private final String mTable;
        @NonNull
        private final Condition mCondition;
        @NonNull
        private final ContentValues mOnInsert;
        @NonNull
        private final Value.Read<K> mKey;

        private Single(@NonNull final android.orm.sql.Executor executor,
                       @NonNls @NonNull final String table,
                       @NonNull final Condition condition,
                       @NonNull final ContentValues onInsert,
                       @NonNull final Value.Read<K> key) {
            super(executor, table, condition, onInsert, key);

            mExecutor = executor;
            mTable = table;
            mCondition = condition;
            mOnInsert = onInsert;
            mKey = key;
        }

        @NonNull
        @Override
        public final <M> Maybe<Producer<Maybe<M>>> query(@NonNull final Plan.Read<M> plan,
                                                         @NonNull final Condition condition,
                                                         @Nullable final Order order,
                                                         @Nullable final Limit limit,
                                                         @Nullable final Offset offset) {
            final Select select = select(mTable)
                    .with(mCondition.and(condition))
                    .with(Limit.Single)
                    .build();
            return mExecutor.execute(new Query<>(plan, select));
        }

        @NonNull
        @Override
        public final Maybe<K> update(@NonNull final Condition condition,
                                     @NonNull final Plan.Write plan) {
            return plan.isEmpty() ?
                    Maybes.<K>nothing() :
                    mExecutor.execute(new Update.Single<>(mTable, mCondition.and(condition), plan, mOnInsert, mKey));
        }
    }

    private static class Many<K> extends Some<K, Integer> implements Executor.Direct.Many<K> {

        @NonNull
        private final android.orm.sql.Executor mExecutor;
        @NonNls
        @NonNull
        private final String mTable;
        @NonNull
        private final Condition mCondition;

        private Many(@NonNull final android.orm.sql.Executor executor,
                     @NonNls @NonNull final String table,
                     @NonNull final Condition condition,
                     @NonNull final ContentValues onInsert,
                     @NonNull final Value.Read<K> key) {
            super(executor, table, condition, onInsert, key);

            mExecutor = executor;
            mTable = table;
            mCondition = condition;
        }

        @NonNull
        @Override
        public final <M> Maybe<Producer<Maybe<M>>> query(@NonNull final Plan.Read<M> plan,
                                                         @NonNull final Condition condition,
                                                         @Nullable final Order order,
                                                         @Nullable final Limit limit,
                                                         @Nullable final Offset offset) {
            final Select select = select(mTable)
                    .with(mCondition.and(condition))
                    .with(order)
                    .with(limit)
                    .with(offset)
                    .build();
            return mExecutor.execute(new Query<>(plan, select));
        }

        @NonNull
        @Override
        public final Maybe<Integer> update(@NonNull final Condition condition,
                                           @NonNull final Plan.Write plan) {
            return plan.isEmpty() ?
                    Maybes.<Integer>nothing() :
                    mExecutor.execute(new Update.Many(mTable, mCondition.and(condition), plan));
        }
    }

    private abstract static class Some<K, U> implements Executor.Direct<K, U> {

        @NonNull
        private final android.orm.sql.Executor mExecutor;
        @NonNls
        @NonNull
        private final String mTable;
        @NonNull
        private final Condition mCondition;
        @NonNull
        private final ContentValues mOnInsert;
        @NonNull
        private final Value.Read<K> mKey;

        protected Some(@NonNull final android.orm.sql.Executor executor,
                       @NonNls @NonNull final String table,
                       @NonNull final Condition condition,
                       @NonNull final ContentValues onInsert,
                       @NonNull final Value.Read<K> key) {
            super();

            mExecutor = executor;
            mTable = table;
            mCondition = condition;
            mOnInsert = onInsert;
            mKey = key;
        }

        @NonNull
        @Override
        public final Maybe<Boolean> exists(@NonNull final Condition condition) {
            return mExecutor.execute(new Exists(mTable, mCondition.and(condition)));
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
        public final Maybe<Integer> delete(@NonNull final Condition condition) {
            return mExecutor.execute(new Delete(mTable, mCondition.and(condition)));
        }
    }

    private Executors() {
        super();
    }
}
