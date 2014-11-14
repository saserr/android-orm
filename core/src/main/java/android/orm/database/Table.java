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

import android.orm.DAO;
import android.orm.Database;
import android.orm.database.table.Check;
import android.orm.database.table.ForeignKey;
import android.orm.database.table.PrimaryKey;
import android.orm.database.table.Schema;
import android.orm.database.table.Schemas;
import android.orm.database.table.UniqueKey;
import android.orm.sql.Column;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.orm.sql.Statements.dropTable;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.util.Log.INFO;
import static java.util.Collections.unmodifiableSet;

public class Table<K> extends Value.ReadWrite.Base<Map<String, Object>> {

    private static final String TAG = Table.class.getSimpleName();

    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Set<Column<?>> mColumns;
    @NonNull
    private final Set<Check> mChecks;
    @NonNull
    private final Set<ForeignKey<?>> mForeignKeys;
    @NonNull
    private final Set<UniqueKey<?>> mUniqueKeys;
    @Nullable
    private final PrimaryKey<K> mPrimaryKey;
    @NonNull
    private final Select.Projection mProjection;

    public Table(@NonNls @NonNull final String name,
                 @NonNull final Collection<Column<?>> columns,
                 @NonNull final Collection<Check> checks,
                 @NonNull final Collection<ForeignKey<?>> foreignKeys,
                 @NonNull final Collection<UniqueKey<?>> uniqueKeys,
                 @Nullable final PrimaryKey<K> primaryKey) {
        super();

        mName = name;
        mColumns = new HashSet<>(columns);
        mChecks = new HashSet<>(checks);
        mForeignKeys = new HashSet<>(foreignKeys);
        mUniqueKeys = new HashSet<>(uniqueKeys);
        mPrimaryKey = primaryKey;

        Select.Projection projection = null;
        for (final Column<?> column : columns) {
            projection = (projection == null) ?
                    column.getProjection() :
                    projection.and(column.getProjection());
        }
        mProjection = projection;
    }

    @NonNls
    @NonNull
    @Override
    public final String getName() {
        return mName;
    }

    @NonNull
    public final Set<Column<?>> getColumns() {
        return unmodifiableSet(mColumns);
    }

    @NonNull
    public final Set<Check> getChecks() {
        return unmodifiableSet(mChecks);
    }

    @NonNull
    public final Set<ForeignKey<?>> getForeignKeys() {
        return unmodifiableSet(mForeignKeys);
    }

    @NonNull
    public final Set<UniqueKey<?>> getUniqueKeys() {
        return unmodifiableSet(mUniqueKeys);
    }

    @Nullable
    public final PrimaryKey<K> getPrimaryKey() {
        return mPrimaryKey;
    }

    @NonNull
    @Override
    public final Select.Projection getProjection() {
        return mProjection;
    }

    @NonNull
    @Override
    public final Maybe<Map<String, Object>> read(@NonNull final android.orm.sql.Readable input) {
        final Map<String, Object> result = new HashMap<>(mColumns.size());

        for (final Column<?> column : mColumns) {
            final Maybe<?> value = column.read(input);
            if (value.isSomething()) {
                result.put(column.getName(), value.get());
            }
        }

        return result.isEmpty() ? Maybes.<Map<String, Object>>nothing() : something(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void write(@NonNull final Operation operation,
                            @NonNull final Maybe<Map<String, Object>> value,
                            @NonNull final Writable output) {
        if (value.isSomething()) {
            final Map<String, Object> map = value.get();
            if (map == null) {
                final Maybe<Object> result = something(null);
                for (final Column<?> column : mColumns) {
                    ((Value.Write<Object>) column).write(operation, result, output);
                }
            } else {
                for (final Column<?> column : mColumns) {
                    final String name = column.getName();
                    ((Value.Write<Object>) column).write(
                            operation,
                            map.containsKey(name) ? something(map.get(name)) : nothing(),
                            output);
                }
            }
        }
    }

    @NonNull
    public static Builder table(@NonNls @NonNull final String name) {
        return new Builder(name);
    }

    public static class Builder {

        @NonNls
        @NonNull
        private final String mName;

        @NonNull
        private final Set<Column<?>> mColumns = new HashSet<>();
        @NonNull
        private final Set<Check> mChecks = new HashSet<>();
        @NonNull
        private final Set<ForeignKey<?>> mForeignKeys = new HashSet<>();
        @NonNull
        private final Set<UniqueKey<?>> mUniqueKeys = new HashSet<>();

        public Builder(@NonNls @NonNull final String name) {
            super();

            mName = name;
        }

        @NonNull
        public final Builder with(@NonNull final Column<?> column) {
            mColumns.add(column);
            return this;
        }

        @NonNull
        public final Builder with(@NonNull final Check check) {
            mChecks.add(check);
            return this;
        }

        @NonNull
        public final Builder with(@NonNull final ForeignKey<?> foreignKey) {
            mForeignKeys.add(foreignKey);
            return this;
        }

        @NonNull
        public final Builder with(@NonNull final UniqueKey<?> uniqueKey) {
            mUniqueKeys.add(uniqueKey);
            return this;
        }

        @NonNull
        public final Table<Long> build() {
            validate();
            return new Table<>(mName, mColumns, mChecks, mForeignKeys, mUniqueKeys, null);
        }

        @NonNull
        public final <K> Table<K> build(@NonNull final PrimaryKey<K> primaryKey) {
            validate();
            return new Table<>(mName, mColumns, mChecks, mForeignKeys, mUniqueKeys, primaryKey);
        }

        private void validate() {
            if (mColumns.isEmpty()) {
                throw new UnsupportedOperationException("Trying to create table without columns");
            }
        }
    }

    public interface Revision {

        @NonNull
        Revision rename(@NonNls @NonNull final String name);

        @NonNull
        Revision add(@NonNull final Column<?> column);

        @NonNull
        Revision update(@NonNull final Column<?> before, @NonNull final Column<?> after);

        @NonNull
        Revision remove(@NonNull final Column<?> column);

        @NonNull
        Revision add(@NonNull final Check check);

        @NonNull
        Revision remove(@NonNull final Check check);

        @NonNull
        Revision add(@NonNull final ForeignKey<?> foreignKey);

        @NonNull
        Revision remove(@NonNull final ForeignKey<?> foreignKey);

        @NonNull
        Revision add(@NonNull final UniqueKey<?> uniqueKey);

        @NonNull
        Revision remove(@NonNull final UniqueKey<?> uniqueKey);

        @NonNull
        Revision with(@NonNull final PrimaryKey<?> primaryKey);

        @NonNull
        Revision withoutPrimaryKey();
    }

    public static class Migration implements android.orm.database.Migration {

        @NonNls
        @NonNull
        private final String mName;

        private final SparseArray<Revision> mRevisions = new SparseArray<>(1);

        public Migration(@NonNls @NonNull final String name) {
            super();

            mName = name;
        }

        @Nullable
        public final Table<?> getTableAt(final int version) {
            final Schema schema = create(version);
            return (schema == null) ? null : schema.table();
        }

        @NonNull
        public final Table<?> getTableFor(@NonNull final Database database) {
            final int version = database.getVersion();
            final Schema schema = create(version);
            if (schema == null) {
                throw new IllegalArgumentException("Table " + mName +
                        "does not exist in database " + database.getName() +
                        " at version " + version);
            }

            return schema.table();
        }

        @NonNull
        public final Table.Revision at(final int version) {
            final Revision result;

            final int index = mRevisions.indexOfKey(version);
            if (index < 0) {
                result = new Revision(version);
                mRevisions.put(version, result);
            } else {
                result = mRevisions.valueAt(index);
            }

            return result;
        }

        @Override
        public final void create(@NonNull final DAO.Direct dao, final int version) {
            final Schema schema = create(version);
            if (schema != null) {
                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, "Creating table " + mName); //NON-NLS
                }
                dao.execute(schema.statement(version));
            }
        }

        @Override
        public final void upgrade(@NonNull final DAO.Direct dao,
                                  final int oldVersion,
                                  final int newVersion) {
            final int size = mRevisions.size();
            if (size > 0) {
                final int createAt = mRevisions.keyAt(0);
                if (createAt <= newVersion) {
                    if (oldVersion < createAt) {
                        create(dao, newVersion);
                    } else {
                        final Schema create = create(oldVersion);
                        if (create != null) {
                            if (Log.isLoggable(TAG, INFO)) {
                                Log.i(TAG, "Upgrading table " + mName); //NON-NLS
                            }

                            final Schema update = Schemas.update(create.table());
                            for (int i = mRevisions.indexOfKey(oldVersion) + 1; (i < size) && (mRevisions.keyAt(i) <= newVersion); i++) {
                                mRevisions.valueAt(i).upgrade(update);
                            }

                            dao.execute(update.statement(newVersion));
                        }
                    }
                }
            }
        }

        @Override
        public final void downgrade(@NonNull final DAO.Direct dao,
                                    final int oldVersion,
                                    final int newVersion) {
            final int size = mRevisions.size();
            if (size > 0) {
                final int createAt = mRevisions.keyAt(0);
                if (createAt <= oldVersion) {
                    if (newVersion < createAt) {
                        if (Log.isLoggable(TAG, INFO)) {
                            Log.i(TAG, "Dropping table " + mName); //NON-NLS
                        }

                        // table must be dropped
                        dao.execute(dropTable(mName));
                    } else {
                        final Schema create = create(oldVersion);
                        if (create != null) {
                            if (Log.isLoggable(TAG, INFO)) {
                                Log.i(TAG, "Downgrading table " + mName); //NON-NLS
                            }

                            final Schema update = Schemas.update(create.table());
                            for (int i = mRevisions.indexOfKey(oldVersion); (i >= 0) && (mRevisions.keyAt(i) > newVersion); i--) {
                                mRevisions.valueAt(i).downgrade(this, update);
                            }

                            dao.execute(update.statement(newVersion));
                        }
                    }
                }
            }
        }

        @Nullable
        private Schema create(final int version) {
            final int size = mRevisions.size();
            @org.jetbrains.annotations.Nullable final Schema result;

            if ((size == 0) || (version < mRevisions.keyAt(0))) {
                result = null;
            } else {
                result = Schemas.create(mName);
                for (int i = 0; (i < size) && (mRevisions.keyAt(i) <= version); i++) {
                    mRevisions.valueAt(i).upgrade(result);
                }
            }

            return result;
        }

        @NonNls
        @NonNull
        private String getNameBefore(final int version) {
            String result = null;

            for (int i = mRevisions.indexOfKey(version) - 1; (i >= 0) && (result == null); i--) {
                result = mRevisions.valueAt(i).getName();
            }

            return (result == null) ? mName : result;
        }

        @Nullable
        private PrimaryKey<?> getPrimaryKeyBefore(final int version) {
            PrimaryKey<?> result = null;

            for (int i = mRevisions.indexOfKey(version) - 1; (i >= 0) && (result == null); i++) {
                final Maybe<PrimaryKey<?>> primaryKey = mRevisions.valueAt(i).getPrimaryKey();
                if (primaryKey.isSomething()) {
                    result = primaryKey.get();
                }
            }

            return result;
        }

        private static class Revision implements Table.Revision {

            private final int mVersion;

            private final List<Pair<Column<?>, Column<?>>> mColumns = new ArrayList<>();
            private final List<Pair<Check, Check>> mChecks = new ArrayList<>();
            private final List<Pair<ForeignKey<?>, ForeignKey<?>>> mForeignKeys = new ArrayList<>();
            private final List<Pair<UniqueKey<?>, UniqueKey<?>>> mUniqueKeys = new ArrayList<>();

            @NonNls
            @Nullable
            private String mName;
            @NonNull
            private Maybe<PrimaryKey<?>> mPrimaryKey = nothing();

            private Revision(final int version) {
                super();

                mVersion = version;
            }

            @NonNls
            @Nullable
            public final String getName() {
                return mName;
            }

            @NonNull
            public final Maybe<PrimaryKey<?>> getPrimaryKey() {
                return mPrimaryKey;
            }

            @NonNull
            @Override
            public final Revision rename(@NonNls @NonNull final String name) {
                mName = name;
                return this;
            }

            @NonNull
            @Override
            public final Revision add(@NonNull final Column<?> column) {
                mColumns.add(Pair.<Column<?>, Column<?>>create(null, column));
                return this;
            }

            @NonNull
            @Override
            public final Revision update(@NonNull final Column<?> before,
                                         @NonNull final Column<?> after) {
                mColumns.add(Pair.<Column<?>, Column<?>>create(before, after));
                return this;
            }

            @NonNull
            @Override
            public final Revision remove(@NonNull final Column<?> column) {
                mColumns.add(Pair.<Column<?>, Column<?>>create(column, null));
                return this;
            }

            @NonNull
            @Override
            public final Revision add(@NonNull final Check check) {
                mChecks.add(Pair.<Check, Check>create(null, check));
                return this;
            }

            @NonNull
            @Override
            public final Revision remove(@NonNull final Check check) {
                mChecks.add(Pair.<Check, Check>create(check, null));
                return this;
            }

            @NonNull
            @Override
            public final Revision add(@NonNull final ForeignKey<?> foreignKey) {
                mForeignKeys.add(Pair.<ForeignKey<?>, ForeignKey<?>>create(null, foreignKey));
                return this;
            }

            @NonNull
            @Override
            public final Revision remove(@NonNull final ForeignKey<?> foreignKey) {
                mForeignKeys.add(Pair.<ForeignKey<?>, ForeignKey<?>>create(foreignKey, null));
                return this;
            }

            @NonNull
            @Override
            public final Revision add(@NonNull final UniqueKey<?> uniqueKey) {
                mUniqueKeys.add(Pair.<UniqueKey<?>, UniqueKey<?>>create(null, uniqueKey));
                return this;
            }

            @NonNull
            @Override
            public final Revision remove(@NonNull final UniqueKey<?> uniqueKey) {
                mUniqueKeys.add(Pair.<UniqueKey<?>, UniqueKey<?>>create(uniqueKey, null));
                return this;
            }

            @NonNull
            @Override
            public final Revision with(@NonNull final PrimaryKey<?> primaryKey) {
                mPrimaryKey = Maybes.<PrimaryKey<?>>something(primaryKey);
                return this;
            }

            @NonNull
            @Override
            public final Revision withoutPrimaryKey() {
                mPrimaryKey = something(null);
                return this;
            }

            public final void upgrade(@NonNull final Schema schema) {
                if (mName != null) {
                    schema.rename(mName);
                }

                for (final Pair<Column<?>, Column<?>> pair : mColumns) {
                    schema.update(pair.first, pair.second);
                }

                for (final Pair<Check, Check> pair : mChecks) {
                    schema.update(pair.first, pair.second);
                }

                for (final Pair<ForeignKey<?>, ForeignKey<?>> pair : mForeignKeys) {
                    schema.update(pair.first, pair.second);
                }

                for (final Pair<UniqueKey<?>, UniqueKey<?>> pair : mUniqueKeys) {
                    schema.update(pair.first, pair.second);
                }

                if (mPrimaryKey.isSomething()) {
                    schema.with(mPrimaryKey.get());
                }
            }

            public final void downgrade(@NonNull final Migration migration,
                                        @NonNull final Schema schema) {
                if (mName != null) {
                    schema.rename(migration.getNameBefore(mVersion));
                }

                for (final Pair<Column<?>, Column<?>> pair : mColumns) {
                    schema.update(pair.second, pair.first);
                }

                for (final Pair<Check, Check> pair : mChecks) {
                    schema.update(pair.second, pair.first);
                }

                for (final Pair<ForeignKey<?>, ForeignKey<?>> pair : mForeignKeys) {
                    schema.update(pair.second, pair.first);
                }

                for (final Pair<UniqueKey<?>, UniqueKey<?>> pair : mUniqueKeys) {
                    schema.update(pair.second, pair.first);
                }

                if (mPrimaryKey.isSomething()) {
                    schema.with(migration.getPrimaryKeyBefore(mVersion));
                }
            }
        }
    }
}
