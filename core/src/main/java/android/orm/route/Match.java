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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.orm.Route;
import android.orm.dao.Async;
import android.orm.dao.direct.Insert;
import android.orm.model.Plans;
import android.orm.sql.Select;
import android.orm.sql.Table;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Helper.escape;

public class Match {

    @NonNull
    private final Route.Item mItemRoute;
    @NonNls
    @NonNull
    private final String mContentType;
    @NonNull
    private final ContentValues mOnInsert;
    @NonNull
    private final Select.Where mWhere;
    @Nullable
    private final String mOrder;
    @NonNull
    private final Table<?> mTable;
    @NonNls
    @NonNull
    private final String mTableName;

    public Match(@NonNull final Route.Item route,
                 @NonNls @NonNull final String contentType,
                 @NonNull final ContentValues onInsert,
                 @NonNull final Select.Where where,
                 @Nullable final Select.Order order) {
        super();

        mItemRoute = route;
        mContentType = contentType;
        mWhere = where;
        mOnInsert = onInsert;
        mOrder = (order == null) ? null : order.toSQL();

        mTable = route.getTable();
        mTableName = escape(mTable.getName());
    }

    @NonNull
    public final String getContentType() {
        return mContentType;
    }

    @NonNull
    public final Table<?> getTable() {
        return mTable;
    }

    @Nullable
    public final Cursor query(@NonNull final SQLiteDatabase database,
                              @Nullable final String[] projection,
                              @Nullable final String selection,
                              @Nullable final String[] arguments,
                              @Nullable final String sortOrder) {
        Async.interruptIfNecessary();

        final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(mTableName);
        final String where = mWhere.and(new Select.Where(selection)).toSQL();
        final String order = (sortOrder == null) ? mOrder : sortOrder;
        return builder.query(database, projection, where, arguments, null, null, order);
    }

    @Nullable
    public final Uri insert(@NonNull final SQLiteDatabase database,
                            @NonNull final ContentValues values) {
        Async.interruptIfNecessary();

        final Insert insert = new Insert(mItemRoute, Plans.write(values), mOnInsert);
        return insert.execute(database).getOrElse(null);
    }

    public final int update(@NonNull final SQLiteDatabase database,
                            @NonNull final ContentValues values,
                            @Nullable final String selection,
                            @Nullable final String... arguments) {
        Async.interruptIfNecessary();

        final String where = mWhere.and(new Select.Where(selection)).toSQL();
        return database.update(mTableName, values, where, arguments);
    }

    public final int delete(@NonNull final SQLiteDatabase database,
                            @Nullable final String selection,
                            @Nullable final String... arguments) {
        Async.interruptIfNecessary();

        final String where = mWhere.and(new Select.Where(selection)).toSQL();
        return database.delete(mTableName, where, arguments);
    }
}
