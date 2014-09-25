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

package android.orm.sql;

import android.orm.sql.fragment.Order;
import android.orm.util.Lazy;
import android.orm.util.Legacy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.util.SparseArray;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.orm.sql.Columns.number;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

public class Table<K> {

    public static final Column<Long> ROW_ID = number("_ROWID_").asNotNull();

    private static final Set<Column<?>> NO_COLUMNS = emptySet();
    private static final SparseArray<Set<Column<?>>> NO_COLUMNS_BY_VERSION = new SparseArray<>();
    private static final List<ForeignKey<?>> NO_FOREIGN_KEYS = emptyList();
    private static final SparseArray<List<ForeignKey<?>>> NO_FOREIGN_KEYS_BY_VERSION = new SparseArray<>();

    // TODO logging

    @NonNls
    @NonNull
    private final String mName;
    private final int mVersion;
    @NonNull
    private final SparseArray<Set<Column<?>>> mColumnsByVersion;
    @NonNull
    private final Lazy<Pair<Pair<Integer, Integer>, SparseArray<Set<Column<?>>>>> mColumnsAtVersion;
    @NonNull
    private final SparseArray<List<ForeignKey<?>>> mForeignKeysByVersion;
    @NonNull
    private final Lazy<Pair<Pair<Integer, Integer>, SparseArray<List<ForeignKey<?>>>>> mForeignKeysAtVersion;
    @Nullable
    private final Order mOrder;
    @Nullable
    private final PrimaryKey<K> mPrimaryKey;

    private Table(@NonNls @NonNull final String name, final int version) {
        this(name, version, NO_COLUMNS_BY_VERSION, new ColumnsAtVersion(NO_COLUMNS_BY_VERSION), NO_FOREIGN_KEYS_BY_VERSION, new ForeignKeysAtVersion(NO_FOREIGN_KEYS_BY_VERSION), null, null);
    }

    private Table(@NonNls @NonNull final String name,
                  final int version,
                  @NonNull final SparseArray<Set<Column<?>>> columnsByVersion,
                  @NonNull final SparseArray<List<ForeignKey<?>>> foreignKeysByVersion,
                  @NonNull final Lazy<Pair<Pair<Integer, Integer>, SparseArray<List<ForeignKey<?>>>>> foreignKeysAtVersion,
                  @Nullable final Order order,
                  @Nullable final PrimaryKey<K> primaryKey) {
        this(name, version, columnsByVersion, new ColumnsAtVersion(columnsByVersion), foreignKeysByVersion, foreignKeysAtVersion, order, primaryKey);
    }

    private Table(@NonNls @NonNull final String name,
                  final int version,
                  @NonNull final SparseArray<Set<Column<?>>> columnsByVersion,
                  @NonNull final Lazy<Pair<Pair<Integer, Integer>, SparseArray<Set<Column<?>>>>> columnsAtVersion,
                  @NonNull final SparseArray<List<ForeignKey<?>>> foreignKeysByVersion,
                  @Nullable final Order order,
                  @Nullable final PrimaryKey<K> primaryKey) {
        this(name, version, columnsByVersion, columnsAtVersion, foreignKeysByVersion, new ForeignKeysAtVersion(foreignKeysByVersion), order, primaryKey);
    }

    private Table(@NonNls @NonNull final String name,
                  final int version,
                  @NonNull final SparseArray<Set<Column<?>>> columnsByVersion,
                  @NonNull final Lazy<Pair<Pair<Integer, Integer>, SparseArray<Set<Column<?>>>>> columnsAtVersion,
                  @NonNull final SparseArray<List<ForeignKey<?>>> foreignKeysByVersion,
                  @NonNull final Lazy<Pair<Pair<Integer, Integer>, SparseArray<List<ForeignKey<?>>>>> foreignKeysAtVersion,
                  @Nullable final Order order,
                  @Nullable final PrimaryKey<K> primaryKey) {
        super();

        mName = name;
        mVersion = version;
        mColumnsByVersion = columnsByVersion;
        mColumnsAtVersion = columnsAtVersion;
        mForeignKeysByVersion = foreignKeysByVersion;
        mForeignKeysAtVersion = foreignKeysAtVersion;
        mOrder = order;
        mPrimaryKey = primaryKey;
    }

    @NonNls
    @NonNull
    public final String getName() {
        return mName;
    }

    public final int getVersion() {
        return mVersion;
    }

    @NonNull
    public final Set<Column<?>> getColumns(final int version) {
        final Pair<Pair<Integer, Integer>, SparseArray<Set<Column<?>>>> data = mColumnsAtVersion.get();
        final int min = data.first.first;
        final int max = data.first.second;
        final SparseArray<Set<Column<?>>> columns = data.second;
        final Set<Column<?>> result;

        if (version < min) {
            result = NO_COLUMNS;
        } else {
            result = (version > max) ?
                    unmodifiableSet(columns.get(max, NO_COLUMNS)) :
                    unmodifiableSet(columns.get(version, NO_COLUMNS));
        }

        return result;
    }

    @NonNull
    public final List<ForeignKey<?>> getForeignKeys(final int version) {
        final Pair<Pair<Integer, Integer>, SparseArray<List<ForeignKey<?>>>> data = mForeignKeysAtVersion.get();
        final int min = data.first.first;
        final int max = data.first.second;
        final SparseArray<List<ForeignKey<?>>> foreignKeys = data.second;
        final List<ForeignKey<?>> result;

        if (version < min) {
            result = NO_FOREIGN_KEYS;
        } else {
            result = (version > max) ?
                    unmodifiableList(foreignKeys.get(max, NO_FOREIGN_KEYS)) :
                    unmodifiableList(foreignKeys.get(version, NO_FOREIGN_KEYS));
        }

        return result;
    }

    @Nullable
    public final Order getOrder() {
        return mOrder;
    }

    @Nullable
    public final PrimaryKey<K> getPrimaryKey() {
        return mPrimaryKey;
    }

    @NonNull
    public final <V> Table<K> with(@NonNull final Column<V> column) {
        return with(mVersion, column);
    }

    @NonNull
    public final <V> Table<K> with(final int version, @NonNull final Column<V> column) {
        if (version < mVersion) {
            throw new IllegalArgumentException("Version is less than table version " + mVersion);
        }

        final SparseArray<Set<Column<?>>> columns = Legacy.clone(mColumnsByVersion);
        Set<Column<?>> atVersion = columns.get(version);
        if (atVersion == null) {
            atVersion = new HashSet<>();
            columns.put(version, atVersion);
        }
        atVersion.add(column);

        return new Table<>(mName,
                mVersion,
                columns,
                mForeignKeysByVersion,
                mForeignKeysAtVersion,
                mOrder,
                mPrimaryKey);
    }

    public final Table<K> with(@NonNull final Order order) {
        return new Table<>(mName,
                mVersion,
                mColumnsByVersion,
                mColumnsAtVersion,
                mForeignKeysByVersion,
                mForeignKeysAtVersion,
                order,
                mPrimaryKey);
    }


    @NonNull
    public final <V> Table<V> with(@NonNull final PrimaryKey<V> primaryKey) {
        return new Table<>(mName,
                mVersion,
                mColumnsByVersion,
                mColumnsAtVersion,
                mForeignKeysByVersion,
                mForeignKeysAtVersion,
                mOrder,
                primaryKey);
    }

    @NonNull
    public final <V> Table<K> with(@NonNull final ForeignKey<V> foreignKey) {
        return with(mVersion, foreignKey);
    }

    @NonNull
    public final <V> Table<K> with(final int version, @NonNull final ForeignKey<V> foreignKey) {
        if (version < mVersion) {
            throw new IllegalArgumentException("Version is less than table version " + mVersion);
        }

        final SparseArray<List<ForeignKey<?>>> foreignKeys = Legacy.clone(mForeignKeysByVersion);
        List<ForeignKey<?>> atVersion = foreignKeys.get(version);
        if (atVersion == null) {
            atVersion = new ArrayList<>();
            foreignKeys.put(version, atVersion);
        }
        atVersion.add(foreignKey);

        return new Table<>(mName,
                mVersion,
                mColumnsByVersion,
                mColumnsAtVersion,
                foreignKeys,
                mOrder,
                mPrimaryKey);

    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        boolean result = this == object;

        if (!result && (object != null) && (getClass() == object.getClass())) {
            final Table<?> other = (Table<?>) object;
            result = mName.equals(other.mName);
        }

        return result;
    }

    @Override
    public final int hashCode() {
        return mName.hashCode();
    }

    @NonNull
    public static Table<Long> table(@NonNls @NonNull final String name,
                                    final int version) {
        return new Table<>(name, version);
    }

    private static class ColumnsAtVersion extends Lazy.Volatile<Pair<Pair<Integer, Integer>, SparseArray<Set<Column<?>>>>> {

        @NonNull
        private final SparseArray<Set<Column<?>>> mColumnsByVersion;

        private ColumnsAtVersion(@NonNull final SparseArray<Set<Column<?>>> columnsByVersion) {
            super();

            mColumnsByVersion = columnsByVersion;
        }

        @NonNull
        @Override
        protected final Pair<Pair<Integer, Integer>, SparseArray<Set<Column<?>>>> produce() {
            final int size = mColumnsByVersion.size();
            final SparseArray<Set<Column<?>>> result = new SparseArray<>(size);
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            for (int i = 0; i < size; i++) {
                final int version = mColumnsByVersion.keyAt(i);
                if (version < min) {
                    min = version;
                }
                if (max < version) {
                    max = version;
                }
            }

            for (int i = min; i <= max; i++) {
                final Set<Column<?>> columns = mColumnsByVersion.get(i);
                if (columns != null) {
                    for (int version = i; version <= max; version++) {
                        Set<Column<?>> atVersion = result.get(version);
                        if (atVersion == null) {
                            atVersion = new HashSet<>(columns.size());
                            result.put(version, atVersion);
                        }
                        atVersion.addAll(columns);
                    }
                }
            }

            return Pair.create(Pair.create(min, max), result);
        }
    }

    private static class ForeignKeysAtVersion extends Lazy.Volatile<Pair<Pair<Integer, Integer>, SparseArray<List<ForeignKey<?>>>>> {

        @NonNull
        private final SparseArray<List<ForeignKey<?>>> mForeignKeysByVersion;

        private ForeignKeysAtVersion(@NonNull final SparseArray<List<ForeignKey<?>>> foreignKeysByVersion) {
            super();

            mForeignKeysByVersion = foreignKeysByVersion;
        }

        @NonNull
        @Override
        protected final Pair<Pair<Integer, Integer>, SparseArray<List<ForeignKey<?>>>> produce() {
            final int size = mForeignKeysByVersion.size();
            final SparseArray<List<ForeignKey<?>>> result = new SparseArray<>(size);
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            for (int i = 0; i < size; i++) {
                final int version = mForeignKeysByVersion.keyAt(i);
                if (version < min) {
                    min = version;
                }
                if (max < version) {
                    max = version;
                }
            }

            for (int i = min; i <= max; i++) {
                final List<ForeignKey<?>> foreignKeys = mForeignKeysByVersion.get(i);
                if (foreignKeys != null) {
                    for (int version = i; version <= max; version++) {
                        List<ForeignKey<?>> atVersion = result.get(version);
                        if (atVersion == null) {
                            atVersion = new ArrayList<>(foreignKeys.size());
                            result.put(version, atVersion);
                        }
                        atVersion.addAll(foreignKeys);
                    }
                }
            }

            return Pair.create(Pair.create(min, max), result);
        }
    }
}
