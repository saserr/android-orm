/*
 * Copyright 2013 the original author or authors
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

package android.orm.route;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.orm.Route;
import android.orm.sql.Column;
import android.orm.sql.Table;
import android.orm.sql.Value;
import android.orm.sql.statement.Select;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Helper.escape;
import static android.orm.sql.Readables.combine;
import static android.orm.sql.Readables.readable;
import static android.orm.sql.Table.ROW_ID;
import static android.orm.sql.Types.Integer;
import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Writables.writable;
import static android.orm.sql.statement.Select.where;
import static android.orm.util.Legacy.getKeys;
import static android.orm.util.Maybes.something;
import static android.util.Log.DEBUG;
import static android.util.Log.INFO;

public class Match {

    private static final String TAG = Match.class.getSimpleName();
    private static final Select.Where.Part<Long> WHERE_ROW_ID = where(ROW_ID);
    private static final String SINGLE_VALUE = "1";

    @NonNull
    private final Route.Item mItemMapping;
    @NonNls
    @NonNull
    private final String mContentType;
    @NonNull
    private final Table mTable;
    @NonNls
    @NonNull
    private final String mTableName;
    @NonNull
    private final ContentValues mOnInsert;
    @NonNull
    private final Select.Where mWhere;
    @Nullable
    private final String mOrder;
    @NonNull
    private final Column<?> mPrimaryKey;
    private final boolean mIsIntegerPrimaryKey;

    public Match(@NonNull final Route.Item itemMapping,
                 @NonNls @NonNull final String contentType,
                 @NonNull final Table table,
                 @NonNull final ContentValues onInsert,
                 @NonNull final Select.Where where,
                 @Nullable final Select.Order order) {
        super();

        mItemMapping = itemMapping;
        mContentType = contentType;
        mTable = table;
        mTableName = escape(mTable.getName());
        mWhere = where;
        mOnInsert = onInsert;
        mOrder = (order == null) ? null : order.toSQL();
        mPrimaryKey = table.getPrimaryKey();
        mIsIntegerPrimaryKey = Integer.equals(mPrimaryKey.getType());
    }

    @NonNull
    public final String getContentType() {
        return mContentType;
    }

    @NonNull
    public final Table getTable() {
        return mTable;
    }

    @Nullable
    public final Cursor query(@NonNull final SQLiteOpenHelper helper,
                              @Nullable final String[] projection,
                              @Nullable final String where,
                              @Nullable final String[] whereArguments,
                              @Nullable final String order) {
        return query(getDatabase(helper, false), projection, where, whereArguments, order, null);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public final Uri insert(@NonNull final SQLiteOpenHelper helper,
                            @NonNull final ContentValues values) {
        final ContentValues newValues = new ContentValues(mOnInsert);
        newValues.putAll(values);

        @org.jetbrains.annotations.Nullable final Uri result;

        final SQLiteDatabase database = getDatabase(helper, true);
        database.beginTransaction();
        try {
            final long id = database.insertOrThrow(mTableName, null, newValues);

            if (id > 0L) {
                if (mIsIntegerPrimaryKey) {
                    ((Value.Write<Long>) mPrimaryKey).write(Insert, something(id), writable(newValues));
                }

                @NonNls final Select.Projection projection = mItemMapping.getProjection().without(getKeys(newValues));
                if (projection.isEmpty()) {
                    result = mItemMapping.createUri(readable(newValues));
                } else {
                    if (Log.isLoggable(TAG, INFO)) {
                        Log.i(TAG, "Creation of item URI after insert requires a query!"); //NON-NLS
                        if (Log.isLoggable(TAG, DEBUG)) {
                            Log.d(TAG, "Missing arguments for item URI: " + projection); //NON-NLS
                        }
                    }

                    final String where = WHERE_ROW_ID.isEqualTo(id).toSQL();
                    final Cursor cursor = query(database, projection.asArray(), where, null, null, SINGLE_VALUE);
                    if (cursor == null) {
                        Log.e(TAG, "Couldn't create an item uri after insert. Querying table " + mTableName + " failed!"); //NON-NLS
                        result = null;
                    } else {
                        try {
                            if (cursor.moveToFirst()) {
                                result = mItemMapping.createUri(combine(readable(cursor), readable(newValues)));
                            } else {
                                Log.e(TAG, "Couldn't create an item uri after insert. Querying table " + mTableName + " for " + where + " returned no results!"); //NON-NLS
                                result = null;
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                }
            } else {
                Log.e(TAG, "Inserting a value into table " + mTableName + " failed."); //NON-NLS
                result = null;
            }

            if (result != null) {
                database.setTransactionSuccessful();
            }
        } finally {
            database.endTransaction();
        }

        return result;
    }

    public final int update(@NonNull final SQLiteOpenHelper helper,
                            @NonNull final ContentValues values,
                            @Nullable final String selection,
                            @Nullable final String... arguments) {
        final String where = createWhere(selection);
        return getDatabase(helper, true).update(mTableName, values, where, arguments);
    }

    public final int delete(@NonNull final SQLiteOpenHelper helper,
                            @Nullable final String selection,
                            @Nullable final String... arguments) {
        final String where = createWhere(selection);
        return getDatabase(helper, true).delete(mTableName, where, arguments);
    }

    @Nullable
    private Cursor query(@NonNull final SQLiteDatabase database,
                         @Nullable final String[] projection,
                         @Nullable final String selection,
                         @Nullable final String[] arguments,
                         @Nullable final String sortOrder,
                         @Nullable final String limit) {
        final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(mTableName);
        final String where = createWhere(selection);
        final String order = (sortOrder == null) ? mOrder : sortOrder;
        return builder.query(database, projection, where, arguments, null, null, order, limit);
    }

    @Nullable
    private String createWhere(@NonNls @Nullable final String selection) {
        return mWhere.and(new Select.Where(selection)).toSQL();
    }

    @NonNull
    private static SQLiteDatabase getDatabase(@NonNull final SQLiteOpenHelper helper,
                                              final boolean writable) {
        final SQLiteDatabase database = writable ?
                helper.getWritableDatabase() :
                helper.getReadableDatabase();
        if (database == null) {
            throw new SQLException("Couldn't access database");
        }
        return database;
    }
}
