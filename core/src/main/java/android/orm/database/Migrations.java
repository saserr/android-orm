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

package android.orm.database;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.orm.sql.Column;
import android.orm.sql.Statement;
import android.orm.sql.Statements;
import android.orm.sql.Table;
import android.orm.util.Lazy;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.orm.sql.Statements.addColumn;
import static android.orm.sql.Statements.alterColumns;
import static android.orm.sql.Statements.createTable;
import static android.orm.sql.Statements.dropTable;
import static android.util.Log.INFO;

public final class Migrations {

    private static final String TAG = Migration.class.getSimpleName();

    @NonNull
    public static Migration atVersion(final int version,
                                      @NonNull final Statement upgrade,
                                      @NonNull final Statement downgrade) {
        return new AtVersion(version, upgrade, downgrade);
    }

    @NonNull
    public static <K> Migration migrate(@NonNull final Table<K> table) {
        return new TableMigration<>(table);
    }

    @NonNull
    public static <K> Migration renameTable(final int version,
                                            @NonNls @NonNull final String oldName,
                                            @NonNull final Table<K> table) {
        final String newName = table.getName();
        return atVersion(
                version,
                Statements.renameTable(oldName, newName),
                Statements.NOTHING
        );
    }

    @NonNull
    public static <K, V> Migration renameColumn(final int version,
                                                @NonNull final Table<K> table,
                                                @NonNls @NonNull final String oldName,
                                                @NonNull final Column<V> column) {
        return atVersion(
                version,
                Statements.renameColumn(table, oldName, column),
                Statements.NOTHING
        );
    }

    @NonNull
    public static Migration compose(@NonNull final Collection<Migration> migrations) {
        return new Composition(migrations);
    }

    private static class AtVersion extends Migration.Base {

        private final int mVersion;
        @NonNull
        private final Statement mUpgrade;
        @NonNull
        private final Statement mDowngrade;

        private AtVersion(final int version,
                          @NonNull final Statement upgrade,
                          @NonNull final Statement downgrade) {
            super();

            mVersion = version;
            mUpgrade = upgrade;
            mDowngrade = downgrade;
        }

        @Override
        public final void upgrade(@NonNull final Context context,
                                  @NonNull final SQLiteDatabase database,
                                  final int oldVersion,
                                  final int newVersion) {
            if ((oldVersion < mVersion) && (mVersion <= newVersion)) {
                mUpgrade.execute(database);
            }
        }

        @Override
        public final void downgrade(@NonNull final Context context,
                                    @NonNull final SQLiteDatabase database,
                                    final int oldVersion,
                                    final int newVersion) {
            if ((newVersion < mVersion) && (mVersion <= oldVersion)) {
                mDowngrade.execute(database);
            }
        }
    }

    private static class Composition implements Migration {

        @NonNull
        private final Iterable<Migration> mUp;
        @NonNull
        private final Lazy<Collection<Migration>> mDown;

        private Composition(@NonNull final Collection<Migration> migrations) {
            super();

            mUp = new ArrayList<>(migrations);
            mDown = reverse(migrations);
        }

        @Override
        public final void create(@NonNull final Context context,
                                 @NonNull final SQLiteDatabase database,
                                 final int version) {
            for (final Migration migration : mUp) {
                migration.create(context, database, version);
            }
        }

        @Override
        public final void upgrade(@NonNull final Context context,
                                  @NonNull final SQLiteDatabase database,
                                  final int oldVersion,
                                  final int newVersion) {
            for (final Migration migration : mUp) {
                migration.upgrade(context, database, oldVersion, newVersion);
            }
        }

        @Override
        public final void downgrade(@NonNull final Context context,
                                    @NonNull final SQLiteDatabase database,
                                    final int oldVersion,
                                    final int newVersion) {
            for (final Migration migration : mDown.get()) {
                migration.downgrade(context, database, oldVersion, newVersion);
            }
        }

        private static Lazy<Collection<Migration>> reverse(@NonNull final Collection<Migration> migrations) {
            return new Lazy.Volatile<Collection<Migration>>() {
                @NonNull
                @Override
                protected List<Migration> produce() {
                    final List<Migration> result = new ArrayList<>(migrations);
                    Collections.reverse(result);
                    return result;
                }
            };
        }
    }

    private static class TableMigration<K> implements Migration {

        @NonNull
        private final Table<K> mTable;

        private TableMigration(@NonNull final Table<K> table) {
            super();

            mTable = table;
        }

        @Override
        public final void create(@NonNull final Context context,
                                 @NonNull final SQLiteDatabase database,
                                 final int version) {
            final int createdAt = mTable.getVersion();

            if ((createdAt > 0) && (createdAt <= version)) {
                @NonNls final String name = mTable.getName();

                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, "Creating table " + name); //NON-NLS
                }

                final Set<Column<?>> columns = mTable.getColumns(version);

                if (columns.isEmpty()) {
                    throw new SQLException(
                            name + " table cannot be created because it has no columns at version " + version
                    );
                }

                createTable(name, columns, mTable.getPrimaryKey()).execute(database);
            }
        }

        @Override
        public final void upgrade(@NonNull final Context context,
                                  @NonNull final SQLiteDatabase database,
                                  final int oldVersion,
                                  final int newVersion) {
            final int createdAt = mTable.getVersion();

            if ((createdAt > 0) && (createdAt <= newVersion)) {
                if (oldVersion < createdAt) {
                    create(context, database, newVersion);
                } else {
                    @NonNls final String name = mTable.getName();

                    // find all columns that were added to table between old version (exclusive) and new version (inclusive)
                    final Collection<Column<?>> columns = new HashSet<>(mTable.getColumns(newVersion));
                    columns.removeAll(mTable.getColumns(oldVersion));

                    if (!columns.isEmpty()) {
                        if (Log.isLoggable(TAG, INFO)) {
                            Log.i(TAG, "Upgrading table " + name); //NON-NLS
                        }

                        // table is created
                        for (final Column<?> column : columns) {
                            addColumn(name, column).execute(database);
                        }
                    }
                }
            }
        }

        @Override
        public final void downgrade(@NonNull final Context context,
                                    @NonNull final SQLiteDatabase database,
                                    final int oldVersion,
                                    final int newVersion) {
            final int createdAt = mTable.getVersion();

            if ((createdAt > 0) && (createdAt <= oldVersion)) {
                @NonNls final String name = mTable.getName();

                if (newVersion < createdAt) {
                    if (Log.isLoggable(TAG, INFO)) {
                        Log.i(TAG, "Dropping table " + name); //NON-NLS
                    }

                    // table must be dropped
                    dropTable(name).execute(database);
                } else {
                    // find all columns that were added to table between old version (exclusive) and new version (inclusive)
                    final Set<Column<?>> remaining = mTable.getColumns(newVersion);
                    final Collection<Column<?>> columns = new HashSet<>(remaining);
                    columns.removeAll(mTable.getColumns(oldVersion));

                    if (!columns.isEmpty()) {
                        if (Log.isLoggable(TAG, INFO)) {
                            Log.i(TAG, "Downgrading table " + name); //NON-NLS
                        }

                        if (remaining.isEmpty()) {
                            throw new SQLException(
                                    name + " table cannot be downgraded because it has no columns at version " + newVersion
                            );
                        }

                        final List<Pair<String, Column<?>>> pairs = new ArrayList<>(columns.size());
                        for (final Column<?> column : remaining) {
                            pairs.add(Pair.<String, Column<?>>create(column.getName(), column));
                        }
                        alterColumns(mTable, pairs).execute(database);
                    }
                }
            }
        }
    }

    private Migrations() {
        super();
    }
}
