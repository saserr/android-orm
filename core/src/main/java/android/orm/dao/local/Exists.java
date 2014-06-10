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
import android.orm.sql.Helper;
import android.orm.sql.Table;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;

public class Exists extends Function.Base<SQLiteDatabase, Maybe<Boolean>> {

    private static final String[] PROJECTION = {"1"};
    private static final String SINGLE = "1";

    @NonNull
    private final Table<?> mTable;
    @NonNull
    private final Select.Where mWhere;

    public Exists(@NonNull final Table<?> table, @NonNull final Select.Where where) {
        super();

        mTable = table;
        mWhere = where;
    }

    @NonNull
    @Override
    public final Maybe<Boolean> invoke(@NonNull final SQLiteDatabase database) {
        final String table = Helper.escape(mTable.getName());
        final String where = mWhere.toSQL();
        final String order = mTable.getOrder().toSQL();
        final Maybe<Boolean> result;

        final Cursor cursor = database.query(table, PROJECTION, where, null, null, null, order, SINGLE);
        try {
            result = Maybes.something(cursor.getCount() > 0);
        } finally {
            cursor.close();
        }

        return result;
    }
}
