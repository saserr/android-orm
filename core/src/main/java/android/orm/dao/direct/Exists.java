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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.orm.sql.Expression;
import android.orm.sql.fragment.Predicate;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public class Exists implements Expression<Boolean> {

    public static final ObjectPool<Exists> Pool = new ObjectPool<Exists>() {
        @NonNull
        @Override
        protected Exists produce(@NonNull final Receipt<Exists> receipt) {
            return new Exists(receipt);
        }
    };

    private static final String[] PROJECTION = {"1"};
    private static final String SINGLE = "1";

    @NonNull
    private final ObjectPool.Receipt<Exists> mReceipt;

    @NonNls
    private String mTable;
    private Predicate mPredicate;

    private Exists(@NonNull final ObjectPool.Receipt<Exists> receipt) {
        super();

        mReceipt = receipt;
    }

    public final void init(@NonNls @NonNull final String table,
                           @NonNull final Predicate predicate) {
        mTable = table;
        mPredicate = predicate;
    }

    @NonNull
    @Override
    public final Maybe<Boolean> execute(@NonNull final SQLiteDatabase database) {
        final Maybe<Boolean> result;

        try {
            final Cursor cursor = database.query(mTable, PROJECTION, mPredicate.toSQL(), null, null, null, null, SINGLE);
            try {
                result = Maybes.something(cursor.getCount() > 0);
            } finally {
                cursor.close();
            }
        } finally {
            mTable = null;
            mPredicate = null;
            mReceipt.yield();
        }

        return result;
    }
}
