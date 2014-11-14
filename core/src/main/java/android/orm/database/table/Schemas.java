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

package android.orm.database.table;

import android.database.SQLException;
import android.orm.sql.Column;
import android.orm.sql.Statement;
import android.orm.sql.Statements;
import android.orm.sql.Table;
import android.orm.sql.table.Check;
import android.orm.sql.table.ForeignKey;
import android.orm.sql.table.PrimaryKey;
import android.orm.sql.table.UniqueKey;
import android.orm.util.Legacy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static android.orm.sql.Statements.compose;
import static android.orm.sql.Statements.copyData;
import static android.orm.sql.Statements.createTable;
import static android.orm.sql.Statements.dropTable;
import static android.orm.sql.Statements.renameTable;
import static java.util.Collections.unmodifiableSet;

public final class Schemas {

    @NonNull
    public static Schema create(@NonNls @NonNull final String name) {
        return new Create(name);
    }

    @NonNull
    public static Schema update(@NonNull final Table<?> table) {
        return new Update(table);
    }

    private static class Create extends Base {

        private final Set<Column<?>> mColumns = new HashSet<>();

        private Create(@NonNls @NonNull final String name) {
            super(name);
        }

        @Override
        public final void update(@Nullable final Column<?> before,
                                 @Nullable final Column<?> after) {
            if (before != null) {
                if (!mColumns.remove(before)) {
                    throw new SQLException(UNKNOWN_COLUMN + before + IN_TABLE + getName());
                }
            }

            if (after != null) {
                mColumns.add(after);
            }
        }

        @NonNull
        @Override
        public final Table<?> table() {
            return new Table<>(getName(), mColumns, getChecks(), getForeignKeys(), getUniqueKeys(), getPrimaryKey());
        }

        @NonNull
        @Override
        public final Statement statement(final int version) {
            if (mColumns.isEmpty()) {
                throw new SQLException(
                        getName() + " table cannot be created" +
                                " because it has no columns at version " + version
                );
            }

            return createTable(table());
        }
    }

    private static class Update extends Base {

        @NonNull
        private final Table<?> mTable;

        @NonNull
        private final Set<Column<?>> mColumns;
        @NonNull
        private final Map<Column<?>, Column<?>> mToCopy;

        private Update(@NonNull final Table<?> table) {
            super(table);

            mTable = table;

            mColumns = new HashSet<>(table.getColumns());
            mToCopy = new HashMap<>(mColumns.size());
            for (final Column<?> column : mColumns) {
                mToCopy.put(column, column);
            }
        }

        @Override
        public final void update(@Nullable final Column<?> before,
                                 @Nullable final Column<?> after) {
            if (before != null) {
                final Column<?> key = mToCopy.containsKey(before) ?
                        before :
                        findKeyForValue(before);

                if (key == null) {
                    throw new SQLException(UNKNOWN_COLUMN + before + IN_TABLE + getName());
                }

                if (after == null) {
                    mToCopy.remove(key);
                } else {
                    mToCopy.put(key, after);
                }

                mColumns.remove(before);
            }

            if (after != null) {
                mColumns.add(after);
            }
        }

        @NonNull
        @Override
        public final Table<?> table() {
            return new Table<>(getName(), mColumns, getChecks(), getForeignKeys(), getUniqueKeys(), getPrimaryKey());
        }

        @NonNull
        @Override
        public final Statement statement(final int version) {
            @NonNls final String oldName = mTable.getName();
            @NonNls final String newName = getName();

            if (mColumns.isEmpty()) {
                throw new SQLException(
                        newName + " table cannot be updated" +
                                " because it has no columns at version " + version
                );
            }
            final Statement result;

            final Set<Check> checks = getChecks();
            final Set<ForeignKey<?>> foreignKeys = getForeignKeys();
            final Set<UniqueKey<?>> uniqueKeys = getUniqueKeys();
            final PrimaryKey<?> primaryKey = getPrimaryKey();
            if (mColumns.equals(mTable.getColumns()) &&
                    Legacy.equals(primaryKey, mTable.getPrimaryKey()) &&
                    checks.equals(mTable.getChecks()) &&
                    foreignKeys.equals(mTable.getForeignKeys()) &&
                    uniqueKeys.equals(mTable.getUniqueKeys())) {
                result = oldName.equals(newName) ?
                        Statements.NOTHING :
                        renameTable(oldName, newName);
            } else {
                @NonNls final String temporaryName = "temp." + newName;
                final Table<?> temporary = new Table<>(temporaryName, mColumns, checks, foreignKeys, uniqueKeys, primaryKey);
                final Table<?> newTable = new Table<>(newName, mColumns, checks, foreignKeys, uniqueKeys, primaryKey);

                final Collection<Pair<String, String>> toTemporary = new ArrayList<>(mColumns.size());
                final Collection<Pair<String, String>> fromTemporary = new ArrayList<>(mColumns.size());
                for (final Map.Entry<Column<?>, Column<?>> pair : mToCopy.entrySet()) {
                    final Column<?> before = pair.getKey();
                    final Column<?> after = pair.getValue();
                    toTemporary.add(Pair.create(before.getName(), after.getName()));
                    fromTemporary.add(Pair.create(after.getName(), after.getName()));
                }

                result = compose(
                        // defer foreign keys integrity check until end of the transaction
                        Statements.statement("pragma defer_foreign_keys=on;"),
                        // create a temporary table
                        createTable(temporary),
                        // copy data to temporary table
                        copyData(oldName, temporaryName, toTemporary),
                        // drop the original table
                        dropTable(oldName),
                        // recreate the original table with remaining columns
                        createTable(newTable),
                        // copy data from temporary table
                        copyData(temporaryName, newName, fromTemporary),
                        // drop the temporary table
                        dropTable(temporaryName)
                );
            }

            return result;
        }

        @Nullable
        private <V> Column<?> findKeyForValue(@NonNull final Column<V> column) {
            final Set<Map.Entry<Column<?>, Column<?>>> entries = mToCopy.entrySet();
            final Iterator<Map.Entry<Column<?>, Column<?>>> iterator = entries.iterator();
            Column<?> result = null;

            while (iterator.hasNext() && (result == null)) {
                final Map.Entry<Column<?>, Column<?>> entry = iterator.next();
                if (entry.getValue().equals(column)) {
                    result = entry.getKey();
                }
            }

            return result;
        }
    }

    private abstract static class Base implements Schema {

        @NonNls
        protected static final String UNKNOWN_COLUMN = "Unknown column ";
        @NonNls
        protected static final String IN_TABLE = " in table ";

        @NonNull
        private final Set<Check> mChecks;
        @NonNull
        private final Set<ForeignKey<?>> mForeignKey;
        @NonNull
        private final Set<UniqueKey<?>> mUniqueKeys;

        @NonNls
        @NonNull
        private String mName;
        @Nullable
        private PrimaryKey<?> mPrimaryKey;

        protected Base(@NonNls @NonNull final String name) {
            super();

            mName = name;
            mChecks = new HashSet<>();
            mForeignKey = new HashSet<>();
            mUniqueKeys = new HashSet<>();
        }

        protected Base(@NonNull final Table<?> table) {
            super();

            mName = table.getName();
            mChecks = new HashSet<>(table.getChecks());
            mForeignKey = new HashSet<>(table.getForeignKeys());
            mUniqueKeys = new HashSet<>(table.getUniqueKeys());
            mPrimaryKey = table.getPrimaryKey();
        }

        @NonNls
        @NonNull
        protected final String getName() {
            return mName;
        }

        @NonNull
        protected final Set<Check> getChecks() {
            return unmodifiableSet(mChecks);
        }

        @NonNull
        protected final Set<ForeignKey<?>> getForeignKeys() {
            return unmodifiableSet(mForeignKey);
        }

        @NonNull
        protected final Set<UniqueKey<?>> getUniqueKeys() {
            return unmodifiableSet(mUniqueKeys);
        }

        @Nullable
        protected final PrimaryKey<?> getPrimaryKey() {
            return mPrimaryKey;
        }

        @Override
        public final void rename(@NonNls @NonNull final String name) {
            mName = name;
        }

        @Override
        public final void update(@Nullable final Check before, @Nullable final Check after) {
            if (before != null) {
                if (!mChecks.remove(before)) {
                    throw new SQLException("Unknown check " + before + IN_TABLE + mName);
                }
            }

            if (after != null) {
                mChecks.add(after);
            }
        }

        @Override
        public final void update(@Nullable final ForeignKey<?> before,
                                 @Nullable final ForeignKey<?> after) {
            if (before != null) {
                if (!mForeignKey.remove(before)) {
                    throw new SQLException("Unknown foreign key " + before + IN_TABLE + mName);
                }
            }

            if (after != null) {
                mForeignKey.add(after);
            }
        }

        @Override
        public final void update(@Nullable final UniqueKey<?> before,
                                 @Nullable final UniqueKey<?> after) {
            if (before != null) {
                if (!mUniqueKeys.remove(before)) {
                    throw new SQLException("Unknown unique " + before + IN_TABLE + mName);
                }
            }

            if (after != null) {
                mUniqueKeys.add(after);
            }
        }

        @Override
        public final void with(@Nullable final PrimaryKey<?> primaryKey) {
            mPrimaryKey = primaryKey;
        }
    }

    private Schemas() {
        super();
    }
}
