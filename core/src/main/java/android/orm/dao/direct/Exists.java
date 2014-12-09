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
import android.orm.sql.fragment.Condition;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public class Exists implements Expression<Boolean> {

    private static final String[] PROJECTION = {"1"};
    private static final String SINGLE = "1";

    @NonNls
    @NonNull
    private final String mTable;
    @NonNull
    private final Condition mCondition;

    public Exists(@NonNls @NonNull final String table, @NonNull final Condition condition) {
        super();

        mTable = table;
        mCondition = condition;
    }

    @NonNull
    @Override
    public final Maybe<Boolean> execute(@NonNull final SQLiteDatabase database) {
        final Maybe<Boolean> result;

        final Cursor cursor = database.query(mTable, PROJECTION, mCondition.toSQL(), null, null, null, null, SINGLE);
        try {
            result = Maybes.something(cursor.getCount() > 0);
        } finally {
            cursor.close();
        }

        return result;
    }
}
