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
import android.orm.sql.Helper;
import android.orm.sql.Table;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Where;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;

public class Exists implements Expression<Boolean> {

    private static final String[] PROJECTION = {"1"};
    private static final String SINGLE = "1";

    @NonNull
    private final Table<?> mTable;
    @NonNull
    private final Where mWhere;

    public Exists(@NonNull final Table<?> table, @NonNull final Where where) {
        super();

        mTable = table;
        mWhere = where;
    }

    @NonNull
    @Override
    public final Maybe<Boolean> execute(@NonNull final SQLiteDatabase database) {
        final String table = Helper.escape(mTable.getName());
        final String where = mWhere.toSQL();
        final Order order = mTable.getOrder();
        final Maybe<Boolean> result;

        final Cursor cursor = database.query(table, PROJECTION, where, null, null, null, (order == null) ? null : order.toSQL(), SINGLE);
        try {
            result = Maybes.something(cursor.getCount() > 0);
        } finally {
            cursor.close();
        }

        return result;
    }
}
