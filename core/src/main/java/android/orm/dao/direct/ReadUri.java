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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.orm.Route;
import android.orm.sql.Expression;
import android.orm.sql.Helper;
import android.orm.sql.Select;
import android.orm.sql.Table;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Readables.combine;
import static android.orm.sql.Readables.readable;
import static android.orm.sql.Select.select;
import static android.orm.util.Legacy.getKeys;
import static android.orm.util.Maybes.nothing;
import static android.util.Log.DEBUG;
import static android.util.Log.INFO;

public class ReadUri implements Expression<Uri> {

    private static final String TAG = ReadUri.class.getSimpleName();

    @NonNull
    private final Route.Item mItemRoute;
    @NonNull
    private final Table<?> mTable;
    @NonNls
    @NonNull
    private final String mTableName;
    @NonNull
    private final Select.Projection mProjection;
    @NonNull
    private final Select.Where mWhere;
    @NonNull
    private final ContentValues mAdditional;

    public ReadUri(@NonNull final Route.Item route,
                   @NonNull final Select.Where where,
                   @NonNull final ContentValues additional) {
        super();

        mItemRoute = route;
        mTable = route.getTable();
        mTableName = Helper.escape(mTable.getName());
        mProjection = route.getProjection().without(getKeys(additional));
        mWhere = where;
        mAdditional = additional;
    }

    @NonNull
    @Override
    public final Maybe<Uri> execute(@NonNull final SQLiteDatabase database) {
        final Maybe<Uri> result;

        if (mProjection.isEmpty()) {
            result = mItemRoute.read(readable(mAdditional));
        } else {
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Creation of item uri requires a query!"); //NON-NLS
                if (Log.isLoggable(TAG, DEBUG)) {
                    Log.d(TAG, "Missing arguments for item uri: " + mProjection); //NON-NLS
                }
            }

            final Cursor cursor = select(mTable)
                    .with(mWhere)
                    .with(Select.Limit.Single)
                    .build()
                    .execute(mProjection, database);
            if (cursor == null) {
                Log.e(TAG, "Couldn't create an item uri. Querying table " + mTableName + " failed!"); //NON-NLS
                result = nothing();
            } else {
                try {
                    if (cursor.moveToFirst()) {
                        result = mItemRoute.read(combine(readable(cursor), readable(mAdditional)));
                    } else {
                        Log.e(TAG, "Couldn't create an item uri. Querying table " + mTableName + " for " + mWhere.toSQL() + " returned no results!"); //NON-NLS
                        result = nothing();
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        return result;
    }
}
