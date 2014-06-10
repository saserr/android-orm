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

import android.database.sqlite.SQLiteDatabase;
import android.orm.sql.Helper;
import android.orm.sql.Table;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;

public class Delete extends Function.Base<SQLiteDatabase, Maybe<Integer>> {

    @NonNull
    private final Table<?> mTable;
    @NonNull
    private final Select.Where mWhere;

    public Delete(@NonNull final Table<?> table, @NonNull final Select.Where where) {
        super();

        mTable = table;
        mWhere = where;
    }

    @NonNull
    @Override
    public final Maybe<Integer> invoke(@NonNull final SQLiteDatabase database) {
        final int deleted = database.delete(Helper.escape(mTable.getName()), mWhere.toSQL(), null);
        return (deleted > 0) ? Maybes.something(deleted) : Maybes.<Integer>nothing();
    }
}
