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

import android.database.sqlite.SQLiteDatabase;
import android.orm.sql.Expression;
import android.orm.sql.fragment.Condition;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public class Delete implements Expression<Integer> {

    public static final ObjectPool<Delete> Pool = new ObjectPool<Delete>() {
        @NonNull
        @Override
        protected Delete produce(@NonNull final Receipt<Delete> receipt) {
            return new Delete(receipt);
        }
    };

    @NonNull
    private final ObjectPool.Receipt<Delete> mReceipt;

    @NonNls
    private String mTable;
    private Condition mCondition;

    private Delete(@NonNull final ObjectPool.Receipt<Delete> receipt) {
        super();

        mReceipt = receipt;
    }

    public final void init(@NonNls @NonNull final String table,
                           @NonNull final Condition condition) {
        mTable = table;
        mCondition = condition;
    }

    @NonNull
    @Override
    public final Maybe<Integer> execute(@NonNull final SQLiteDatabase database) {
        final int deleted;

        try {
            deleted = database.delete(mTable, mCondition.toSQL(), null);
        } finally {
            mTable = null;
            mCondition = null;
            mReceipt.yield();
        }

        return (deleted > 0) ? Maybes.something(deleted) : Maybes.<Integer>nothing();
    }
}
