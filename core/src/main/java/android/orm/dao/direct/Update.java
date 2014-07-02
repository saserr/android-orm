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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.orm.Route;
import android.orm.model.Plan;
import android.orm.sql.Expression;
import android.orm.sql.Helper;
import android.orm.sql.Select;
import android.orm.sql.Table;
import android.orm.sql.Value;
import android.orm.sql.Writables;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import static android.orm.util.Maybes.nothing;
import static android.util.Log.INFO;

public final class Update {

    private static final String TAG = Update.class.getSimpleName();

    public static class Single implements Expression<Uri> {

        @NonNull
        private final Route.Item mRoute;
        @NonNls
        @NonNull
        private final String mTable;
        @NonNull
        private final Select.Where mWhere;
        @NonNull
        private final Plan.Write mPlan;
        @NonNull
        private final ContentValues mAdditional;

        public Single(@NonNull final Route.Item route,
                      @NonNull final Select.Where where,
                      @NonNull final Plan.Write plan,
                      @NonNull final ContentValues additional) {
            super();

            mRoute = route;
            mTable = Helper.escape(route.getTable().getName());
            mWhere = where;
            mPlan = plan;
            mAdditional = additional;
        }

        @NonNull
        @Override
        public final Maybe<Uri> execute(@NonNull final SQLiteDatabase database) {
            final ContentValues values = new ContentValues();
            mPlan.write(Value.Write.Operation.Update, Writables.writable(values));
            final int updated = update(database, mTable, mWhere, values);

            final Maybe<Uri> result;

            if (updated > 1) {
                throw new SQLException("More than one row was updated");
            }

            if (updated > 0) {
                values.putAll(mAdditional);
                result = new ReadUri(mRoute, mWhere, mAdditional).execute(database);

                if (result.isNothing()) {
                    throw new SQLException("Couldn't create item uri after update");
                }
            } else {
                result = nothing();
            }

            return result;
        }
    }

    public static class Many implements Expression<Integer> {

        @NonNls
        @NonNull
        private final String mTable;
        @NonNls
        @NonNull
        private final Select.Where mWhere;
        @NonNull
        private final Plan.Write mPlan;

        public Many(@NonNull final Table<?> table,
                    @NonNull final Select.Where where,
                    @NonNull final Plan.Write plan) {
            super();

            mTable = Helper.escape(table.getName());
            mWhere = where;
            mPlan = plan;
        }

        @NonNull
        @Override
        public final Maybe<Integer> execute(@NonNull final SQLiteDatabase database) {
            final ContentValues values = new ContentValues();
            mPlan.write(Value.Write.Operation.Update, Writables.writable(values));
            final int updated = update(database, mTable, mWhere, values);
            return (updated > 0) ? Maybes.something(updated) : Maybes.<Integer>nothing();
        }
    }

    private static int update(@NonNull final SQLiteDatabase database,
                              @NonNls @NonNull final String table,
                              @NonNull final Select.Where where,
                              @NonNull final ContentValues values) {
        final int updated;

        if (values.size() > 0) {
            updated = database.update(table, values, where.toSQL(), null);
        } else {
            updated = 0;
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Nothing was updated"); //NON-NLS
            }
        }

        return updated;
    }

    private Update() {
        super();
    }
}
