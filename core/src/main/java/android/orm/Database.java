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

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.orm.dao.Direct;
import android.orm.database.IntegrityCheck;
import android.orm.database.IntegrityChecks;
import android.orm.database.Migration;
import android.orm.database.Migrations;
import android.orm.sql.Executor;
import android.orm.sql.Expression;
import android.orm.sql.Statement;
import android.orm.util.Function;
import android.orm.util.Legacy;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.text.TextUtils.isEmpty;
import static android.util.Log.DEBUG;
import static android.util.Log.INFO;

public class Database {

    private static final String TAG = Database.class.getSimpleName();
    private static final Function<SQLiteDatabase, DAO.Direct> DEFAULT_FACTORY =
            new Function<SQLiteDatabase, DAO.Direct>() {
                @NonNull
                @Override
                public DAO.Direct invoke(@NonNull final SQLiteDatabase database) {
                    return new Direct.InsideTransaction(database);
                }
            };

    private static final Semaphore sSemaphore = new Semaphore(1);
    private static final Map<String, Helper> sHelpers = new HashMap<>();

    @NonNls
    @Nullable
    private final String mName;
    private final int mVersion;
    @NonNull
    private final IntegrityCheck mCheck;
    @NonNull
    private final Function<SQLiteDatabase, DAO.Direct> mFactory;

    private final List<Migration> mMigrations = new ArrayList<>();

    public Database(@NonNls @Nullable final String name, final int version) {
        this(name, version, IntegrityChecks.None, DEFAULT_FACTORY);
    }

    public Database(@NonNls @Nullable final String name,
                    final int version,
                    @NonNull final IntegrityCheck check) {
        this(name, version, check, DEFAULT_FACTORY);
    }

    public Database(@NonNls @Nullable final String name,
                    final int version,
                    @NonNull final Function<SQLiteDatabase, DAO.Direct> factory) {
        this(name, version, IntegrityChecks.None, factory);
    }

    public Database(@NonNls @Nullable final String name,
                    final int version,
                    @NonNull final IntegrityCheck check,
                    @NonNull final Function<SQLiteDatabase, DAO.Direct> factory) {
        super();

        mName = isEmpty(name) ? null : name;
        mVersion = version;
        mCheck = check;
        mFactory = factory;
    }

    @NonNls
    @Nullable
    public final String getName() {
        return mName;
    }

    public final int getVersion() {
        return mVersion;
    }

    @NonNull
    public final Helper getHelper(@NonNull final Context context) {
        final Helper result;

        sSemaphore.acquireUninterruptibly();
        try {
            if (!sHelpers.containsKey(mName)) {
                final Migration migration = Migrations.compose(mMigrations);
                final Helper helper = new Helper(context, this, mCheck, mVersion, migration, mFactory);
                sHelpers.put(mName, helper);
            }
            result = sHelpers.get(mName);
        } finally {
            sSemaphore.release();
        }

        return result;
    }

    @NonNull
    public final Database migrate(@NonNull final Migration migration) {
        sSemaphore.acquireUninterruptibly();
        try {
            if (sHelpers.containsKey(mName)) {
                throw new UnsupportedOperationException("Migration added too late! Database has been already migrated.");
            }

            mMigrations.add(migration);
        } finally {
            sSemaphore.release();
        }

        return this;
    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        boolean result = this == object;

        if (!result && (object != null) && (getClass() == object.getClass())) {
            final Database other = (Database) object;
            result = ((mName == null) && (other.mName == null)) ||
                    ((mName != null) && mName.equals(other.mName));
        }

        return result;
    }

    @Override
    public final int hashCode() {
        return (mName == null) ? 0 : mName.hashCode();
    }

    public static class Helper extends SQLiteOpenHelper implements Executor {

        @NonNls
        private static final String TURN_ON_FOREIGN_KEYS_CHECK = "pragma foreign_keys=on;";

        @NonNull
        private final IntegrityCheck mCheck;
        private final int mVersion;
        @NonNull
        private final Migration mMigration;
        @NonNull
        private final Function<SQLiteDatabase, DAO.Direct> mFactory;
        @NonNls
        @NonNull
        private final String mName;

        private Helper(@NonNull final Context context,
                       @NonNull final Database database,
                       @NonNull final IntegrityCheck check,
                       final int version,
                       @NonNull final Migration migration,
                       @NonNull final Function<SQLiteDatabase, DAO.Direct> factory) {
            super(context, database.getName(), null, database.getVersion());

            mCheck = check;
            mVersion = version;
            mMigration = migration;
            mFactory = factory;

            final String name = database.getName();
            mName = (name == null) ? "<memory>" : name;

            if (Log.isLoggable(TAG, DEBUG)) {
                Log.d(TAG, "Creating SQLite connection to database " + mName); //NON-NLS
            }
        }

        @Override
        public final void execute(@NonNull final Statement statement) {
            final SQLiteDatabase database = getWritableDatabase();
            database.beginTransaction();
            try {
                statement.execute(database);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }

        @NonNull
        @Override
        public final <V> Maybe<V> execute(@NonNull final Expression<V> expression) {
            final SQLiteDatabase database = getWritableDatabase();
            final Maybe<V> result;

            database.beginTransaction();
            try {
                result = expression.execute(database);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }

            return result;
        }

        @Override
        public final void onConfigure(@NonNull final SQLiteDatabase database) {
            mCheck.check(database);
            if (!database.isReadOnly()) {
                if (SDK_INT >= JELLY_BEAN) {
                    database.setForeignKeyConstraintsEnabled(true);
                } else {
                    database.execSQL(TURN_ON_FOREIGN_KEYS_CHECK);
                }
            }
        }

        @Override
        public final void onCreate(@NonNull final SQLiteDatabase database) {
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Creating database " + mName + " at version " + mVersion); //NON-NLS
            }

            try {
                mMigration.create(mFactory.invoke(database), mVersion);
            } catch (final SQLException cause) {
                @NonNls final String message = "There was a problem creating database " + mName + " at version " + mVersion;
                Log.e(TAG, message, cause);
                throw Legacy.wrap(message, cause);
            }
        }

        @Override
        public final void onUpgrade(@NonNull final SQLiteDatabase database,
                                    final int oldVersion,
                                    final int newVersion) {
            if (oldVersion > newVersion) {
                onDowngrade(database, oldVersion, newVersion);
            } else if (oldVersion < newVersion) {
                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, "Upgrading database " + mName + " from version " + oldVersion + " to version " + newVersion); //NON-NLS
                }

                try {
                    mMigration.upgrade(mFactory.invoke(database), oldVersion, newVersion);
                } catch (final SQLException cause) {
                    @NonNls final String message = "There was a problem updating database " + mName +
                            " from version " + oldVersion + " to version " + newVersion;
                    Log.e(TAG, message, cause);
                    throw Legacy.wrap(message, cause);
                }
            }
        }

        @Override
        public final void onDowngrade(@NonNull final SQLiteDatabase database,
                                      final int oldVersion,
                                      final int newVersion) {
            if (oldVersion < newVersion) {
                onUpgrade(database, oldVersion, newVersion);
            } else if (oldVersion > newVersion) {
                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, "Downgrading database " + mName + " from version " + oldVersion + " to version " + newVersion); //NON-NLS
                }

                try {
                    mMigration.downgrade(mFactory.invoke(database), oldVersion, newVersion);
                } catch (final SQLException cause) {
                    @NonNls final String message = "There was a problem downgrading database " + mName +
                            " from version " + oldVersion + " to version " + newVersion;
                    Log.e(TAG, message, cause);
                    throw Legacy.wrap(message, cause);
                }
            }
        }
    }
}
