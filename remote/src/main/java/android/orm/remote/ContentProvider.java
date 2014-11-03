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

package android.orm.remote;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.orm.Database;
import android.orm.reactive.Route;
import android.orm.remote.provider.Match;
import android.orm.sql.fragment.Limit;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.text.MessageFormat;
import java.util.ArrayList;

import static android.util.Log.DEBUG;
import static java.lang.System.arraycopy;

public class ContentProvider extends android.content.ContentProvider {

    private static final String TAG = ContentProvider.class.getSimpleName();
    @NonNls
    private static final MessageFormat CONTENT_TYPE_FORMAT = new MessageFormat("vnd.android.cursor.{0}/vnd.{1}.{2}");

    @NonNull
    private final Database mDatabase;
    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Route.Manager[] mManagers;

    @NonNull
    private SQLiteOpenHelper mHelper;
    @NonNull
    private ContentResolver mContentResolver;

    public ContentProvider(@NonNull final Database database,
                           @NonNls @NonNull final String name,
                           @NonNull final Route.Manager... managers) {
        super();

        mDatabase = database;
        mName = name;
        mManagers = new Route.Manager[managers.length];
        arraycopy(managers, 0, mManagers, 0, mManagers.length);
    }

    @Override
    public final boolean onCreate() {
        final Context context = getContext();
        if (context == null) {
            throw new IllegalArgumentException("Context is null");
        }

        mHelper = mDatabase.getDatabaseHelper(context);
        mContentResolver = context.getContentResolver();

        return true;
    }

    @Nullable
    @Override
    public final Cursor query(@NonNls @NonNull final Uri uri,
                              @Nullable final String[] projection,
                              @Nullable final String selection,
                              @Nullable final String[] arguments,
                              @Nullable final String order) {
        final Cursor cursor;

        final SQLiteDatabase database = getDatabase(mHelper, false);
        if (database.inTransaction()) {
            cursor = query(database, uri, projection, selection, arguments, order);
        } else {
            database.beginTransaction();
            try {
                cursor = query(database, uri, projection, selection, arguments, order);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }

        return cursor;
    }

    @NonNls
    @Nullable
    @Override
    public final String getType(@NonNull final Uri uri) {
        String result = null;

        final Route route = route(uri);
        if (route != null) {
            final Limit limit = route.getLimit();
            @NonNls final String type = ((limit == null) || (limit.getAmount() > 1)) ? "dir" : "item";
            result = CONTENT_TYPE_FORMAT.format(new String[]{type, mName, route.getTable()});
        }

        return result;
    }

    @Nullable
    @Override
    public final Uri insert(@NonNls @NonNull final Uri uri, @NonNull final ContentValues values) {
        final Uri result;

        final SQLiteDatabase database = getDatabase(mHelper, true);
        if (database.inTransaction()) {
            result = insert(database, uri, values);
        } else {
            database.beginTransaction();
            try {
                result = insert(database, uri, values);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }

        return result;
    }

    @Override
    public final int update(@NonNls @NonNull final Uri uri,
                            @NonNull final ContentValues values,
                            @Nullable final String selection,
                            @Nullable final String[] arguments) {
        final int updated;

        final SQLiteDatabase database = getDatabase(mHelper, true);
        if (database.inTransaction()) {
            updated = update(database, uri, values, selection, arguments);
        } else {
            database.beginTransaction();
            try {
                updated = update(database, uri, values, selection, arguments);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }

        return updated;
    }

    @Override
    public final int delete(@NonNls @NonNull final Uri uri,
                            @Nullable final String selection,
                            @Nullable final String[] arguments) {
        final int deleted;

        final SQLiteDatabase database = getDatabase(mHelper, true);
        if (database.inTransaction()) {
            deleted = delete(database, uri, selection, arguments);
        } else {
            database.beginTransaction();
            try {
                deleted = delete(database, uri, selection, arguments);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }

        return deleted;
    }

    @NonNull
    @Override
    @SuppressWarnings("RefusedBequest")
    public final ContentProviderResult[] applyBatch(@NonNull final ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        final int size = operations.size();
        boolean writable = false;
        for (int i = 0; (i < size) && !writable; i++) {
            if (operations.get(i).isWriteOperation()) {
                writable = true;
            }
        }

        final ContentProviderResult[] results = new ContentProviderResult[size];

        final SQLiteDatabase database = getDatabase(mHelper, writable);
        database.beginTransaction();
        try {
            for (int i = 0; i < size; i++) {
                results[i] = operations.get(i).apply(this, results, i);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        return results;
    }

    @Nullable
    private Route route(@NonNls @NonNull final Uri uri) {
        Route route = null;

        final int length = mManagers.length;
        for (int i = 0; (i < length) && (route == null); i++) {
            route = mManagers[i].get(uri);
        }

        return route;
    }

    @NonNull
    private Match match(@NonNls @NonNull final Uri uri) {
        final Route route = route(uri);
        if (route == null) {
            throw new SQLException("Unknown URI " + uri);
        }
        return new Match(route, uri);
    }

    @Nullable
    private Cursor query(@NonNull final SQLiteDatabase database,
                         @NonNls @NonNull final Uri uri,
                         @Nullable final String[] projection,
                         @Nullable final String selection,
                         @Nullable final String[] arguments,
                         @Nullable final String order) {
        final Cursor cursor = match(uri).query(database, projection, selection, arguments, order);

        if (cursor == null) {
            Log.w(TAG, "Query at " + uri + " was unsuccessful."); //NON-NLS\
        } else {
            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, "Query at " + uri + " returned " + cursor.getCount() + " rows."); //NON-NLS
            }
            cursor.setNotificationUri(mContentResolver, uri);
        }

        return cursor;
    }

    @Nullable
    private Uri insert(@NonNull final SQLiteDatabase database,
                       @NonNls @NonNull final Uri uri,
                       @NonNull final ContentValues values) {
        final Uri result = match(uri).insert(database, values);

        if (result == null) {
            Log.w(TAG, "Insert at " + uri + " was unsuccessful."); //NON-NLS
        } else {
            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, "Insert at " + uri + " was successful."); //NON-NLS
            }
            mContentResolver.notifyChange(result, null);
        }

        return result;
    }

    private int update(@NonNull final SQLiteDatabase database,
                       @NonNls @NonNull final Uri uri,
                       @NonNull final ContentValues values,
                       @Nullable final String selection,
                       @Nullable final String[] arguments) {
        final int updated = match(uri).update(database, values, selection, arguments);

        if (updated > 0) {
            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, "Update at " + uri + " impacted " + updated + " rows."); //NON-NLS
            }
            mContentResolver.notifyChange(uri, null);
        } else {
            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, "Update at " + uri + " impacted no rows."); //NON-NLS
            }
        }

        return updated;
    }

    private int delete(@NonNull final SQLiteDatabase database,
                       @NonNls @NonNull final Uri uri,
                       @Nullable final String selection,
                       @Nullable final String[] arguments) {
        final int deleted = match(uri).delete(database, selection, arguments);

        if (deleted > 0) {
            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, "Delete at " + uri + " removed " + deleted + " rows."); //NON-NLS
            }
            mContentResolver.notifyChange(uri, null);
        } else {
            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, "Delete at " + uri + " removed no rows."); //NON-NLS
            }
        }

        return deleted;
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
