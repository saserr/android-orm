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
import android.orm.database.table.Schema;
import android.orm.database.table.Schemas;
import android.orm.sql.Column;
import android.orm.sql.table.Check;
import android.orm.sql.table.ForeignKey;
import android.orm.sql.table.PrimaryKey;
import android.orm.sql.table.UniqueKey;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

import static android.orm.sql.Statements.dropTable;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.util.Log.INFO;

public interface Migration {

    void create(@NonNull final DAO.Direct dao, final int version);

    void upgrade(@NonNull final DAO.Direct dao, final int oldVersion, final int newVersion);

    void downgrade(@NonNull final DAO.Direct dao, final int oldVersion, final int newVersion);

    abstract class Base implements Migration {

        @Override
        public void create(@NonNull final DAO.Direct dao, final int version) {
            /* do nothing */
        }

        @Override
        public void upgrade(@NonNull final DAO.Direct dao,
                            final int oldVersion,
                            final int newVersion) {
            /* do nothing */
        }

        @Override
        public void downgrade(@NonNull final DAO.Direct dao,
                              final int oldVersion,
                              final int newVersion) {
            /* do nothing */
        }
    }

    class Table implements Migration {

        private static final String TAG = Table.class.getSimpleName();

        @NonNls
        @NonNull
        private final String mName;

        private final SparseArray<Revision> mRevisions = new SparseArray<>(1);

        public Table(@NonNls @NonNull final String name) {
            super();

            mName = name;
        }

        @Nullable
        public final android.orm.sql.Table<?> getTableAt(final int version) {
            final Schema schema = create(version);
            return (schema == null) ? null : schema.table();
        }

        @NonNull
        public final android.orm.sql.Table<?> getTableFor(@NonNull final Database database) {
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
        public final android.orm.database.table.Revision at(final int version) {
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

        private static class Revision implements android.orm.database.table.Revision {

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

            private void upgrade(@NonNull final Schema schema) {
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

            private void downgrade(@NonNull final Table migration,
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
