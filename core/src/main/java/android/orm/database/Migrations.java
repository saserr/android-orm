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
import android.orm.sql.Column;
import android.orm.sql.Statement;
import android.orm.sql.Statements;
import android.orm.sql.Table;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.List;

import static android.orm.sql.Statements.createTable;
import static android.orm.sql.Statements.dropTable;

public final class Migrations {

    @NonNull
    public static Migration atVersion(final int version,
                                      @NonNull final Statement upgrade,
                                      @NonNull final Statement downgrade) {
        return new AtVersion(version, upgrade, downgrade);
    }

    @NonNull
    public static Migration create(final int version, @NonNls @NonNull final Table<?> table) {
        return atVersion(
                version,
                createTable(table),
                dropTable(table.getName())
        );
    }

    @NonNull
    public static Migration renameTable(final int version,
                                        @NonNls @NonNull final String oldName,
                                        @NonNls @NonNull final String newName) {
        return atVersion(
                version,
                Statements.renameTable(oldName, newName),
                Statements.renameTable(newName, oldName)
        );
    }

    @NonNull
    public static Migration createIndex(final int version,
                                        @NonNls @NonNull final String table,
                                        @NonNull final Column<?> column) {
        @NonNls final String name = table + '_' + column.getName() + "_index";
        return createIndex(version, name, table, column);
    }

    @NonNull
    public static Migration createIndex(final int version,
                                        @NonNls @NonNull final String name,
                                        @NonNls @NonNull final String table,
                                        @NonNull final Column<?> column) {
        return createIndex(version, column.isUnique(), name, table, column);
    }

    @NonNull
    public static Migration createIndex(final int version,
                                        final boolean isUnique,
                                        @NonNls @NonNull final String name,
                                        @NonNls @NonNull final String table,
                                        @NonNull final Column<?>... columns) {
        return atVersion(
                version,
                Statements.createIndex(isUnique, name, table, columns),
                Statements.dropIndex(name)
        );
    }

    @NonNull
    public static Migration compose(@NonNull final List<Migration> migrations) {
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
        public final void upgrade(@NonNull final DAO.Direct dao,
                                  final int oldVersion,
                                  final int newVersion) {
            if ((oldVersion < mVersion) && (mVersion <= newVersion)) {
                dao.execute(mUpgrade);
            }
        }

        @Override
        public final void downgrade(@NonNull final DAO.Direct dao,
                                    final int oldVersion,
                                    final int newVersion) {
            if ((newVersion < mVersion) && (mVersion <= oldVersion)) {
                dao.execute(mDowngrade);
            }
        }
    }

    private static class Composition implements Migration {

        @NonNull
        private final List<Migration> mMigrations;

        private Composition(@NonNull final List<Migration> migrations) {
            super();

            mMigrations = new ArrayList<>(migrations);
        }

        @Override
        public final void create(@NonNull final DAO.Direct dao, final int version) {
            for (final Migration migration : mMigrations) {
                migration.create(dao, version);
            }
        }

        @Override
        public final void upgrade(@NonNull final DAO.Direct dao,
                                  final int oldVersion,
                                  final int newVersion) {
            for (final Migration migration : mMigrations) {
                migration.upgrade(dao, oldVersion, newVersion);
            }
        }

        @Override
        public final void downgrade(@NonNull final DAO.Direct dao,
                                    final int oldVersion,
                                    final int newVersion) {
            final int size = mMigrations.size();
            for (int i = size - 1; i >= 0; i--) {
                mMigrations.get(i).downgrade(dao, oldVersion, newVersion);
            }
        }
    }

    private Migrations() {
        super();
    }
}
