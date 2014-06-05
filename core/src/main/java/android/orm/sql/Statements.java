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

package android.orm.sql;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;

import static android.orm.sql.Helper.escape;
import static android.util.Log.DEBUG;
import static java.util.Collections.singleton;

public final class Statements {

    public static final Statement NOTHING = new Statement() {
        @Override
        public void execute(@NonNull final SQLiteDatabase database) {/* do nothing */}
    };

    private static final String TAG = Statement.class.getSimpleName();
    @NonNls
    private static final String ALTER_TABLE = "alter table ";
    @NonNls
    private static final String NO_COLUMNS = "Columns cannot be empty";

    @NonNull
    public static <K> Statement createTable(@NonNls @NonNull final String name,
                                            @NonNull final Collection<Column<?>> columns,
                                            @Nullable final PrimaryKey<K> primaryKey) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException(NO_COLUMNS);
        }

        @NonNls final StringBuilder result = new StringBuilder();
        result.append("create table ").append(escape(name)).append(" (\n");

        for (final Column<?> column : columns) {
            result.append(column.toSQL()).append(",\n");
        }

        if (primaryKey != null) {
            result.append(primaryKey.toSQL()).append(",\n");
        }

        final int length = result.length();
        return statement(result.replace(length - 2, length, ");").toString());
    }

    @NonNull
    public static <V> Statement createIndex(@NonNls @NonNull final String table,
                                            @NonNull final Column<V> column) {
        @NonNls final String name = table + '_' + column.getName() + "_index";
        return createIndex(name, table, column);
    }

    @NonNull
    public static <V> Statement createIndex(@NonNls @NonNull final String name,
                                            @NonNls @NonNull final String table,
                                            @NonNull final Column<V> column) {
        return createIndex(column.isUnique(), name, table, column);
    }

    @NonNull
    public static Statement createIndex(final boolean isUnique,
                                        @NonNls @NonNull final String name,
                                        @NonNls @NonNull final String table,
                                        @NonNull final Column<?>... columns) {
        final StringBuilder projection = new StringBuilder().append('(');

        for (final Column<?> column : columns) {
            projection.append(escape(column.getName())).append(", ");
        }
        final int length = projection.length();
        projection.replace(length - 2, length, ")");

        return statement("create " + (isUnique ? "unique " : "") + "index " + escape(name)
                + "\non " + escape(table) + ' ' + projection + ';');
    }

    @NonNull
    public static Statement copyData(@NonNls @NonNull final String fromTable,
                                     @NonNls @NonNull final String toTable,
                                     @NonNull final Collection<Pair<String, String>> columns) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException(NO_COLUMNS);
        }

        final StringBuilder from = new StringBuilder();
        final StringBuilder to = new StringBuilder().append('(');

        for (final Pair<String, String> pair : columns) {
            from.append(escape(pair.first)).append(", ");
            to.append(escape(pair.second)).append(", ");
        }
        from.delete(from.length() - 2, from.length());
        to.replace(to.length() - 2, to.length(), ")");

        return statement("insert into " + escape(toTable) + ' ' + to +
                "\nselect " + from +
                "\nfrom " + escape(fromTable) + ';');
    }

    @NonNull
    public static Statement renameTable(@NonNls @NonNull final String oldName,
                                        @NonNls @NonNull final String newName) {
        return new RenameTable(oldName, newName);
    }

    @NonNull
    public static <V> Statement addColumn(@NonNls @NonNull final String name,
                                          @NonNull final Column<V> column) {
        return statement(ALTER_TABLE + escape(name) + " add column " + column.toSQL() + ';');
    }

    @NonNull
    public static <K, V> Statement renameColumn(@NonNull final Table<K> table,
                                                @NonNls @NonNull final String oldName,
                                                @NonNull final Column<V> column) {
        return new RenameColumn<>(table, oldName, column);
    }

    @NonNull
    public static <K> Statement alterColumns(@NonNull final Table<K> table,
                                             @NonNull final Collection<Pair<String, Column<?>>> columns) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException(NO_COLUMNS);
        }

        final String original = table.getName();
        final PrimaryKey<K> primaryKey = table.getPrimaryKey();
        final Collection<Column<?>> remaining = new ArrayList<>(columns.size());
        final Collection<Pair<String, String>> toTemporary = new ArrayList<>(columns.size());
        final Collection<Pair<String, String>> fromTemporary = new ArrayList<>(columns.size());
        for (final Pair<String, Column<?>> pair : columns) {
            final Column<?> column = pair.second;
            remaining.add(column);
            toTemporary.add(Pair.create(pair.first, column.getName()));
            fromTemporary.add(Pair.create(column.getName(), column.getName()));
        }

        @NonNls final String temporary = "temp." + original;

        return compose(
                // defer foreign keys integrity check until end of the transaction
                statement("pragma defer_foreign_keys=on;"),
                // create a temporary table
                createTable(temporary, remaining, primaryKey),
                // copy data to temporary table
                copyData(original, temporary, toTemporary),
                // drop the original table
                dropTable(original),
                // recreate the original table with remaining columns
                createTable(original, remaining, primaryKey),
                // copy data from temporary table
                copyData(temporary, original, fromTemporary),
                // drop the temporary table
                dropTable(temporary)
        );
    }

    @NonNull
    public static Statement dropTable(@NonNls @NonNull final String name) {
        return statement("drop table " + escape(name) + ';');
    }

    @NonNull
    public static Statement statement(@NonNls @NonNull final String sql) {
        return new Statement() {
            @Override
            public void execute(@NonNull final SQLiteDatabase database) {
                if (Log.isLoggable(TAG, DEBUG)) {
                    Log.d(TAG, "Executing SQL statement:\n" + sql); //NON-NLS
                }
                database.execSQL(sql);
            }
        };
    }

    @NonNull
    public static Statement compose(@NonNull final Statement... statements) {
        return new Composition(statements);
    }

    @NonNull
    public static Statement fail(@NonNls @NonNull final String error) {
        return new Failure(error);
    }

    private static class RenameTable implements Statement {

        @NonNls
        private static final String TABLE_CHECK = "select 1 as \"exists\"" +
                "\nfrom sqlite_master" +
                "\nwhere type='table' and name=?" +
                "\nlimit 1;";

        @NonNls
        @NonNull
        private final String mOldName;
        @NonNls
        @NonNull
        private final String mNewName;
        @NonNull
        private final Statement mStatement;

        private RenameTable(@NonNls @NonNull final String oldName,
                            @NonNls @NonNull final String newName) {
            super();

            mOldName = oldName;
            mNewName = newName;

            mStatement = statement(ALTER_TABLE + escape(mOldName) + " rename to " + escape(mNewName) + ';');
        }

        @Override
        public final void execute(@NonNull final SQLiteDatabase database) {
            if (containsTable(database, mOldName)) {
                mStatement.execute(database);
            } else if (containsTable(database, mNewName)) {
                Log.w(TAG, "Table '" + mOldName + "' is probably already renamed to '" + mNewName + "! Skipping table rename"); //NON-NLS
            } else {
                throw new SQLException("Table '" + mOldName + "' is missing! Cannot rename it to '" + mNewName + '\'');
            }
        }

        private static boolean containsTable(@NonNull final SQLiteDatabase database,
                                             @NonNls @NonNull final String name) {
            final boolean exists;

            final Cursor cursor = database.rawQuery(TABLE_CHECK, new String[]{name});
            try {
                exists = cursor.getCount() > 0;
            } finally {
                cursor.close();
            }

            return exists;
        }
    }

    private static class RenameColumn<K, V> implements Statement {

        @NonNls
        private static final String TABLE_INFO = "pragma table_info(?);";

        @NonNull
        private final Table<K> mTable;
        @NonNls
        @NonNull
        private final String mOldName;
        @NonNull
        private final Column<V> mColumn;
        @NonNull
        private final Statement mStatement;

        private RenameColumn(@NonNls @NonNull final Table<K> table,
                             @NonNls @NonNull final String oldName,
                             @NonNull final Column<V> column) {
            super();

            mTable = table;
            mOldName = oldName;
            mColumn = column;

            mStatement = alterColumns(mTable, singleton(Pair.<String, Column<?>>create(mOldName, mColumn)));
        }

        @Override
        public final void execute(@NonNull final SQLiteDatabase database) {
            final String table = mTable.getName();
            final String newName = mColumn.getName();

            if (containsColumn(database, table, mOldName)) {
                mStatement.execute(database);
            } else if (containsColumn(database, table, newName)) {
                Log.w(TAG, "Column '" + mOldName + "' in table '" + table + "' is probably already renamed to '" + newName + "! Skipping column rename"); //NON-NLS
            } else {
                throw new SQLException("Column '" + mOldName + "' in table '" + table + "' is missing! Cannot rename it to '" + newName + '\'');
            }
        }

        private static boolean containsColumn(@NonNull final SQLiteDatabase database,
                                              @NonNls @NonNull final String table,
                                              @NonNls @NonNull final String name) {
            boolean exists = false;

            final Cursor cursor = database.rawQuery(TABLE_INFO, new String[]{table});
            try {
                if (cursor.moveToFirst()) {
                    do {
                        exists = name.equals(cursor.getString(cursor.getColumnIndex("name"))); //NON-NLS
                    } while (!exists && cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }

            return exists;
        }
    }

    private static class Composition implements Statement {

        @NonNull
        private final Statement[] mStatements;

        private Composition(@NonNull final Statement... statements) {
            super();

            mStatements = statements;
        }

        @Override
        public final void execute(@NonNull final SQLiteDatabase database) {
            for (final Statement statement : mStatements) {
                statement.execute(database);
            }
        }
    }

    private static class Failure implements Statement {

        @NonNls
        @NonNull
        private final String mError;

        private Failure(@NonNls @NonNull final String error) {
            super();

            mError = error;
        }

        @Override
        public final void execute(@NonNull final SQLiteDatabase database) {
            throw new SQLException(mError);
        }
    }

    private Statements() {
        super();
    }
}
