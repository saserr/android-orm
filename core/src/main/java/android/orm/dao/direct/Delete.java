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
import android.orm.sql.fragment.Where;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public class Delete implements Expression<Integer> {

    @NonNls
    @NonNull
    private final String mTable;
    @NonNull
    private final Where mWhere;

    public Delete(@NonNls @NonNull final String table, @NonNull final Where where) {
        super();

        mTable = table;
        mWhere = where;
    }

    @NonNull
    @Override
    public final Maybe<Integer> execute(@NonNull final SQLiteDatabase database) {
        final int deleted = database.delete(mTable, mWhere.toSQL(), null);
        return (deleted > 0) ? Maybes.something(deleted) : Maybes.<Integer>nothing();
    }
}
