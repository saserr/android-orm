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

package android.orm.remote.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.orm.dao.direct.Insert;
import android.orm.remote.Route;
import android.orm.remote.route.Path;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Condition;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Order;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Helper.escape;

public class Match {

    public static final ObjectPool<Match> Pool = new ObjectPool<Match>() {
        @NonNull
        @Override
        protected Match produce(@NonNull final Receipt<Match> receipt) {
            return new Match(receipt);
        }
    };

    @NonNull
    private final ObjectPool.Receipt<Match> mReceipt;

    private Route.Single mSingleRoute;
    private ContentValues mOnInsert;
    private Condition mCondition;
    @NonNls
    private String mTable;
    private String mOrder;
    private String mLimit;

    private Match(@NonNull final ObjectPool.Receipt<Match> receipt) {
        super();

        mReceipt = receipt;
    }

    public final void init(@NonNull final Route route, @NonNull final Uri uri) {
        mSingleRoute = route.getSingleRoute();
        final Path path = route.getPath();
        mCondition = path.createCondition(uri);
        mOnInsert = path.createValues(uri);
        mTable = escape(route.getTable());

        final Order order = route.getOrder();
        mOrder = (order == null) ? null : order.toSQL();

        final Limit limit = route.getLimit();
        mLimit = (limit == null) ? null : limit.toSQL();
    }

    @Nullable
    public final Cursor query(@NonNull final SQLiteDatabase database,
                              @Nullable final String[] projection,
                              @Nullable final String selection,
                              @Nullable final String[] arguments,
                              @Nullable final String order) {
        final Cursor result;

        try {
            final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables(mTable);
            final String where = mCondition.and(new Condition(selection)).toSQL();
            result = builder.query(database, projection, where, arguments, null, null, (order == null) ? mOrder : order, mLimit);
        } finally {
            clean();
            mReceipt.yield();
        }

        return result;
    }

    @Nullable
    public final Uri insert(@NonNull final SQLiteDatabase database,
                            @NonNull final ContentValues values) {
        @org.jetbrains.annotations.Nullable final Uri result;

        try {
            if (values.size() > 0) {
                final Write.Values plan = Write.Values.Pool.borrow();
                plan.init(values);
                final Insert insert = Insert.Pool.borrow();
                insert.init(mTable, plan, mOnInsert, mSingleRoute);
                result = (Uri) insert.execute(database).getOrElse(null);
            } else {
                result = null;
            }
        } finally {
            clean();
            mReceipt.yield();
        }

        return result;
    }

    public final int update(@NonNull final SQLiteDatabase database,
                            @NonNull final ContentValues values,
                            @Nullable final String selection,
                            @Nullable final String... arguments) {
        final int result;

        try {
            final String where = mCondition.and(new Condition(selection)).toSQL();
            result = database.update(mTable, values, where, arguments);
        } finally {
            clean();
            mReceipt.yield();
        }

        return result;
    }

    public final int delete(@NonNull final SQLiteDatabase database,
                            @Nullable final String selection,
                            @Nullable final String... arguments) {
        final int result;

        try {
            final String where = mCondition.and(new Condition(selection)).toSQL();
            result = database.delete(mTable, where, arguments);
        } finally {
            clean();
            mReceipt.yield();
        }

        return result;
    }

    private void clean() {
        mSingleRoute = null;
        mCondition = null;
        mOnInsert = null;
        mTable = null;
        mOrder = null;
        mLimit = null;
    }

    private static final class Write {

        public static class Values implements Writer {

            public static final ObjectPool<Values> Pool = new ObjectPool<Values>() {
                @NonNull
                @Override
                protected Values produce(@NonNull final Receipt<Values> receipt) {
                    return new Values(receipt);
                }
            };

            @NonNull
            private final ObjectPool.Receipt<Values> mReceipt;

            private ContentValues mValues;

            private Values(@NonNull final ObjectPool.Receipt<Values> receipt) {
                super();

                mReceipt = receipt;
            }

            public final void init(@NonNull final ContentValues values) {
                mValues = values;
            }

            @NonNull
            @Override
            public final Condition onUpdate() {
                return Condition.None;
            }

            @Override
            public final void write(@NonNull final Value.Write.Operation operation,
                                    @NonNull final Writable output) {
                try {
                    output.putAll(mValues);
                } finally {
                    mValues = null;
                    mReceipt.yield();
                }
            }
        }

        private Write() {
            super();
        }
    }
}
