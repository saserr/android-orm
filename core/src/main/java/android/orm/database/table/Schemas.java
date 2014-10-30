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
import android.orm.database.Table;
import android.orm.sql.Column;
import android.orm.sql.ForeignKey;
import android.orm.sql.PrimaryKey;
import android.orm.sql.Statement;
import android.orm.sql.Statements;
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

        @NonNls
        @NonNull
        private String mName;

        private Create(@NonNls @NonNull final String name) {
            super();

            mName = name;
        }

        @NonNull
        @Override
        protected final String getName() {
            return mName;
        }

        @Override
        public final void rename(@NonNls @NonNull final String name) {
            mName = name;
        }

        @Override
        public final void update(@Nullable final Column<?> before,
                                 @Nullable final Column<?> after) {
            if (before != null) {
                if (!mColumns.remove(before)) {
                    throw new SQLException(UNKNOWN_COLUMN + before + IN_TABLE + mName);
                }
            }

            if (after != null) {
                mColumns.add(after);
            }
        }

        @NonNull
        @Override
        public final Table<?> table() {
            return new Table<>(mName, getPrimaryKey(), getForeignKey(), mColumns);
        }

        @NonNull
        @Override
        public final Statement statement(final int version) {
            if (mColumns.isEmpty()) {
                throw new SQLException(
                        mName + " table cannot be created" +
                                " because it has no columns at version " + version
                );
            }

            return createTable(mName, getPrimaryKey(), getForeignKey(), mColumns);
        }
    }

    private static class Update extends Base {

        @NonNull
        private final Table<?> mTable;

        @NonNull
        private final Set<Column<?>> mColumns;
        @NonNull
        private final Map<Column<?>, Column<?>> mToCopy;

        @NonNls
        @NonNull
        private Pair<String, String> mName;

        private Update(@NonNull final Table<?> table) {
            super(table.getPrimaryKey(), table.getForeignKeys());

            mTable = table;

            mColumns = new HashSet<>(table.getColumns());
            mToCopy = new HashMap<>(mColumns.size());
            for (final Column<?> column : mColumns) {
                mToCopy.put(column, column);
            }

            final String name = table.getName();
            mName = Pair.create(name, name);
        }

        @NonNull
        @Override
        protected final String getName() {
            return mName.second;
        }

        @Override
        public final void rename(@NonNls @NonNull final String name) {
            mName = Pair.create(mName.first, name);
        }

        @Override
        public final void update(@Nullable final Column<?> before,
                                 @Nullable final Column<?> after) {
            if (before != null) {
                final Column<?> key = mToCopy.containsKey(before) ?
                        before :
                        findKeyForValue(before);

                if (key == null) {
                    throw new SQLException(UNKNOWN_COLUMN + before + IN_TABLE + mName.second);
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
            return new Table<>(mName.second, getPrimaryKey(), getForeignKey(), mColumns);
        }

        @NonNull
        @Override
        public final Statement statement(final int version) {
            @NonNls final String oldName = mName.first;
            @NonNls final String newName = mName.second;

            if (mColumns.isEmpty()) {
                throw new SQLException(
                        newName + " table cannot be updated" +
                                " because it has no columns at version " + version
                );
            }
            final Statement result;

            final PrimaryKey<?> primaryKey = getPrimaryKey();
            final Set<ForeignKey<?>> foreignKeys = getForeignKey();
            if (mColumns.equals(mTable.getColumns()) &&
                    Legacy.equals(primaryKey, mTable.getPrimaryKey()) &&
                    foreignKeys.equals(mTable.getForeignKeys())) {
                result = oldName.equals(newName) ?
                        Statements.NOTHING :
                        renameTable(oldName, newName);
            } else {
                @NonNls final String temporary = "temp." + newName;

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
                        createTable(temporary, primaryKey, foreignKeys, mColumns),
                        // copy data to temporary table
                        copyData(oldName, temporary, toTemporary),
                        // drop the original table
                        dropTable(oldName),
                        // recreate the original table with remaining columns
                        createTable(newName, primaryKey, foreignKeys, mColumns),
                        // copy data from temporary table
                        copyData(temporary, newName, fromTemporary),
                        // drop the temporary table
                        dropTable(temporary)
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
        private final Set<ForeignKey<?>> mForeignKey;

        @Nullable
        private PrimaryKey<?> mPrimaryKey;

        protected Base() {
            super();

            mForeignKey = new HashSet<>();
        }

        protected Base(@Nullable final PrimaryKey<?> primaryKey,
                       @NonNull final Set<ForeignKey<?>> foreignKeys) {
            super();

            mPrimaryKey = primaryKey;
            mForeignKey = new HashSet<>(foreignKeys);
        }

        @NonNls
        @NonNull
        protected abstract String getName();

        @NonNull
        protected final Set<ForeignKey<?>> getForeignKey() {
            return unmodifiableSet(mForeignKey);
        }

        @Nullable
        protected final PrimaryKey<?> getPrimaryKey() {
            return mPrimaryKey;
        }

        @Override
        public final void update(@Nullable final ForeignKey<?> before,
                                 @Nullable final ForeignKey<?> after) {
            if (before != null) {
                if (!mForeignKey.remove(before)) {
                    throw new SQLException("Unknown foreign key " + before + IN_TABLE + getName());
                }
            }

            if (after != null) {
                mForeignKey.add(after);
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
