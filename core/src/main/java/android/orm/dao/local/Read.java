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

package android.orm.dao.local;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.orm.model.Observer;
import android.orm.model.Plan;
import android.orm.sql.Helper;
import android.orm.sql.Table;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static android.orm.sql.Readables.readable;
import static android.util.Log.INFO;

public class Read<V> extends Function.Base<SQLiteDatabase, Maybe<Producer<Maybe<V>>>> {

    public static final int Single = 1;

    private static final Object AfterRead = new Function.Base<Producer<Maybe<Object>>, Maybe<Object>>() {
        @NonNull
        @Override
        public Maybe<Object> invoke(@NonNull final Producer<Maybe<Object>> producer) {
            final Maybe<Object> result = producer.produce();
            if (result.isSomething()) {
                Observer.afterRead(result.get());
            }
            return result;
        }
    };

    private static final String TAG = Read.class.getSimpleName();

    @NonNull
    private final Table<?> mTable;
    @NonNull
    private final Arguments<V> mArguments;

    public Read(@NonNull final Table<?> table, @NonNull final Arguments<V> arguments) {
        super();

        mTable = table;
        mArguments = arguments;
    }

    @NonNull
    @Override
    public final Maybe<Producer<Maybe<V>>> invoke(@NonNull final SQLiteDatabase database) {
        final String table = Helper.escape(mTable.getName());
        final Plan.Read<V> plan = mArguments.getPlan();
        final Select.Projection projection = plan.getProjection();
        final String where = mArguments.getWhere().toSQL();
        final Select.Order order = (mArguments.getOrder() == null) ? mTable.getOrder() : mArguments.getOrder();
        final String limit = mArguments.getLimit();

        final Maybe<Producer<Maybe<V>>> result;

        if (projection.isEmpty()) {
            result = Maybes.nothing();
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Nothing was queried"); //NON-NLS
            }
        } else {
            final Cursor cursor = database.query(table, projection.asArray(), where, null, null, null, order.toSQL(), limit);
            if (cursor == null) {
                result = Maybes.nothing();
            } else {
                try {
                    result = Maybes.something(plan.read(readable(cursor)));
                } finally {
                    cursor.close();
                }
            }
        }

        return result;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Function<Producer<Maybe<V>>, Maybe<V>> afterRead() {
        return (Function<Producer<Maybe<V>>, Maybe<V>>) AfterRead;
    }

    public static class Arguments<V> {

        @NonNull
        private final Plan.Read<V> mPlan;
        @NonNull
        private final Select.Where mWhere;
        @Nullable
        private final Select.Order mOrder;
        @Nullable
        private final Integer mLimit;

        public Arguments(@NonNull final Plan.Read<V> plan,
                         @NonNull final Select.Where where,
                         @Nullable final Select.Order order,
                         @Nullable final Integer limit) {
            super();

            mPlan = plan;
            mWhere = where;
            mOrder = order;
            mLimit = limit;
        }

        @NonNull
        public final <T> Arguments<T> copy(@NonNull final Plan.Read<T> plan) {
            return new Arguments<>(plan, mWhere, mOrder, mLimit);
        }

        @NonNull
        private Plan.Read<V> getPlan() {
            return mPlan;
        }

        @NonNull
        private Select.Where getWhere() {
            return mWhere;
        }

        @Nullable
        private Select.Order getOrder() {
            return mOrder;
        }

        @Nullable
        private String getLimit() {
            return ((mLimit != null) && (mLimit > 0)) ? String.valueOf(mLimit) : null;
        }
    }
}
