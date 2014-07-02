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
import android.orm.sql.Column;
import android.orm.sql.Expression;
import android.orm.sql.Helper;
import android.orm.sql.PrimaryKey;
import android.orm.sql.Select;
import android.orm.sql.Table;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Select.where;
import static android.orm.sql.Table.ROW_ID;
import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Writables.writable;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.util.Log.INFO;

public class Insert implements Expression<Uri> {

    private static final String TAG = Insert.class.getSimpleName();
    private static final Select.Where.Part<Long> WHERE_ROW_ID = where(ROW_ID);

    @NonNull
    private final Route.Item mRoute;
    @NonNull
    private final Table<?> mTable;
    @NonNls
    @NonNull
    private final String mTableName;
    @Nullable
    private final PrimaryKey<?> mPrimaryKey;
    @NonNull
    private final Plan.Write mPlan;
    @NonNull
    private final ContentValues mAdditional;

    public Insert(@NonNull final Route.Item route,
                  @NonNull final Plan.Write plan,
                  @NonNull final ContentValues additional) {
        super();

        mRoute = route;
        mTable = route.getTable();
        mTableName = Helper.escape(mTable.getName());
        mPrimaryKey = mTable.getPrimaryKey();
        mPlan = plan;
        mAdditional = additional;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public final Maybe<Uri> execute(@NonNull final SQLiteDatabase database) {
        final ContentValues values = new ContentValues(mAdditional);
        final Writable output = writable(values);
        mPlan.write(Insert, output);
        String nullColumn = null;
        if (values.size() <= 0) {
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "An empty row will be written"); //NON-NLS
            }

            for (final Column<?> column : mTable.getColumns(database.getVersion())) {
                if (column.isNullable()) {
                    nullColumn = Helper.escape(column.getName());
                    break;
                }
            }
        }

        final long id = database.insertOrThrow(mTableName, nullColumn, values);
        final Maybe<Uri> result;

        if (id > 0L) {
            if ((mPrimaryKey != null) && mPrimaryKey.isAliasForRowId()) {
                ((Value.Write<Long>) mPrimaryKey).write(Insert, something(id), output);
            }

            result = new ReadUri(mRoute, WHERE_ROW_ID.isEqualTo(id), values).execute(database);

            if (result.isNothing()) {
                throw new SQLException("Couldn't create item uri after insert");
            }
        } else {
            result = nothing();
        }

        return result;
    }
}
