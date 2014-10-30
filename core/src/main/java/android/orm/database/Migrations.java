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
import android.orm.util.Lazy;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
        final String name = table.getName();
        return atVersion(
                version,
                createTable(name, table.getPrimaryKey(), table.getForeignKeys(), table.getColumns()),
                dropTable(name)
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
    public static <V> Migration createIndex(final int version,
                                            @NonNls @NonNull final String table,
                                            @NonNull final Column<V> column) {
        @NonNls final String name = table + '_' + column.getName() + "_index";
        return createIndex(version, name, table, column);
    }

    @NonNull
    public static <V> Migration createIndex(final int version,
                                            @NonNls @NonNull final String name,
                                            @NonNls @NonNull final String table,
                                            @NonNull final Column<V> column) {
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
        private final Iterable<Migration> mUp;
        @NonNull
        private final Lazy<Collection<Migration>> mDown;

        private Composition(@NonNull final Collection<Migration> migrations) {
            super();

            mUp = new ArrayList<>(migrations);
            mDown = reverse(migrations);
        }

        @Override
        public final void create(@NonNull final DAO.Direct dao, final int version) {
            for (final Migration migration : mUp) {
                migration.create(dao, version);
            }
        }

        @Override
        public final void upgrade(@NonNull final DAO.Direct dao,
                                  final int oldVersion,
                                  final int newVersion) {
            for (final Migration migration : mUp) {
                migration.upgrade(dao, oldVersion, newVersion);
            }
        }

        @Override
        public final void downgrade(@NonNull final DAO.Direct dao,
                                    final int oldVersion,
                                    final int newVersion) {
            for (final Migration migration : mDown.get()) {
                migration.downgrade(dao, oldVersion, newVersion);
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

    private Migrations() {
        super();
    }
}
