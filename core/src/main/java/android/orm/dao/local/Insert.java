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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.orm.Route;
import android.orm.model.Plan;
import android.orm.sql.Helper;
import android.orm.sql.PrimaryKey;
import android.orm.sql.Table;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Readables.combine;
import static android.orm.sql.Readables.readable;
import static android.orm.sql.Table.ROW_ID;
import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Writables.writable;
import static android.orm.sql.statement.Select.select;
import static android.orm.sql.statement.Select.where;
import static android.orm.util.Legacy.getKeys;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.util.Log.DEBUG;
import static android.util.Log.INFO;

public class Insert extends Function.Base<SQLiteDatabase, Maybe<Uri>> {

    private static final String TAG = Insert.class.getSimpleName();
    private static final Select.Where.Part<Long> WHERE_ROW_ID = where(ROW_ID);

    @NonNull
    private final Route.Item mItemRoute;
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

        mItemRoute = route;
        mTable = route.getTable();
        mTableName = Helper.escape(mTable.getName());
        mPrimaryKey = mTable.getPrimaryKey();
        mPlan = plan;
        mAdditional = additional;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public final Maybe<Uri> invoke(@NonNull final SQLiteDatabase database) {
        final ContentValues values = new ContentValues(mAdditional);
        final Writable output = writable(values);
        mPlan.write(Insert, output);
        if ((values.size() <= 0) && Log.isLoggable(TAG, INFO)) {
            Log.i(TAG, "An empty row will be written"); //NON-NLS
        }

        final long id = database.insertOrThrow(mTableName, null, values);
        final Maybe<Uri> result;

        if (id > 0L) {
            if ((mPrimaryKey != null) && mPrimaryKey.isAliasForRowId()) {
                ((Value.Write<Long>) mPrimaryKey).write(Insert, something(id), output);
            }
            @NonNls final Select.Projection projection = mItemRoute.getProjection().without(getKeys(values));
            if (projection.isEmpty()) {
                result = something(mItemRoute.createUri(readable(values)));
            } else {
                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, "Creation of item URI after insert requires a query!"); //NON-NLS
                    if (Log.isLoggable(TAG, DEBUG)) {
                        Log.d(TAG, "Missing arguments for item URI: " + projection); //NON-NLS
                    }
                }

                final Select.Where where = WHERE_ROW_ID.isEqualTo(id);
                final Cursor cursor = select(mTable)
                        .with(where)
                        .with(Select.Limit.Single)
                        .build()
                        .execute(projection, database);
                if (cursor == null) {
                    Log.e(TAG, "Couldn't create an item uri after insert. Querying table " + mTableName + " failed!"); //NON-NLS
                    result = something(null);
                } else {
                    try {
                        if (cursor.moveToFirst()) {
                            result = something(mItemRoute.createUri(combine(readable(cursor), readable(values))));
                        } else {
                            Log.e(TAG, "Couldn't create an item uri after insert. Querying table " + mTableName + " for " + where.toSQL() + " returned no results!"); //NON-NLS
                            result = something(null);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        } else {
            result = nothing();
        }

        return result;
    }

    public static class Notify extends Function.Base<Uri, Uri> {

        @NonNull
        private final Notifier mNotifier;

        public Notify(@NonNull final Notifier notifier) {
            super();

            mNotifier = notifier;
        }

        @NonNull
        @Override
        public final Uri invoke(@NonNull final Uri uri) {
            mNotifier.notifyChange(uri);
            return uri;
        }
    }
}
