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

package android.orm;

import android.content.ContentProvider;
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
import android.orm.route.Match;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;

import static android.util.Log.DEBUG;

public class BaseContentProvider extends ContentProvider {

    private static final String TAG = BaseContentProvider.class.getSimpleName();

    @NonNull
    private final Database mDatabase;
    @NonNull
    private final Route.Manager mRouteManager;

    @NonNull
    private SQLiteOpenHelper mHelper;
    @NonNull
    private ContentResolver mContentResolver;

    public BaseContentProvider(@NonNull final Database database,
                               @NonNull final Route.Manager manager) {
        super();

        mDatabase = database;
        mRouteManager = manager;
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

    @Nullable
    @Override
    public final String getType(@NonNull final Uri uri) {
        String result = null;

        final Match match = mRouteManager.match(uri);
        if (match != null) {
            result = match.getContentType();
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

    @NonNull
    private Match match(@NonNls @NonNull final Uri uri) {
        final Match match = mRouteManager.match(uri);
        if (match == null) {
            throw new SQLException("Unknown URI " + uri);
        }
        return match;
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
