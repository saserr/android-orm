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

import android.orm.sql.statement.Select;
import android.orm.util.Lazy;
import android.orm.util.Legacy;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.util.SparseArray;

import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.orm.sql.Columns.number;
import static android.orm.sql.statement.Select.Order.Type.Ascending;
import static android.orm.sql.statement.Select.order;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

public class Table extends Value.ReadWrite.Base<Map<String, Object>> {

    public static final Column<Long> ROW_ID = number("_ROWID_").asNotNull();

    private static final Set<Column<?>> NO_COLUMNS = emptySet();
    private static final SparseArray<Set<Column<?>>> NO_COLUMNS_BY_VERSION = new SparseArray<>();
    private static final Select.Order DEFAULT_ORDER = order(ROW_ID, Ascending);

    // TODO logging

    @NonNls
    @NonNull
    private final String mName;
    private final int mVersion;
    @NonNull
    private final SparseArray<Set<Column<?>>> mColumnsByVersion;
    @NonNull
    private final Lazy<Set<Column<?>>> mColumns;
    @NonNull
    private final Lazy<Pair<Pair<Integer, Integer>, SparseArray<Set<Column<?>>>>> mColumnsAtVersion;
    @Nullable
    private final Select.Projection mProjection;
    @NonNull
    private final Select.Order mOrder;
    @NonNull
    private final Column<?> mPrimaryKey;

    private Table(@NonNls @NonNull final String name,
                  final int version) {
        this(name, version, NO_COLUMNS_BY_VERSION, null, null, null);
    }

    private Table(@NonNls @NonNull final String name,
                  final int version,
                  @NonNull final SparseArray<Set<Column<?>>> columnsByVersion,
                  @Nullable final Select.Projection projection,
                  @Nullable final Select.Order order,
                  @Nullable final Column<?> primaryKey) {
        this(name, version, columnsByVersion, columns(columnsByVersion), columnsAtVersion(columnsByVersion), projection, order, primaryKey);
    }

    private Table(@NonNls @NonNull final String name,
                  final int version,
                  @NonNull final SparseArray<Set<Column<?>>> columnsByVersion,
                  @NonNull final Lazy<Set<Column<?>>> columns,
                  @NonNull final Lazy<Pair<Pair<Integer, Integer>, SparseArray<Set<Column<?>>>>> columnsAtVersion,
                  @Nullable final Select.Projection projection,
                  @Nullable final Select.Order order,
                  @Nullable final Column<?> primaryKey) {
        super();

        mName = name;
        mVersion = version;
        mColumnsByVersion = columnsByVersion;
        mColumns = columns;
        mColumnsAtVersion = columnsAtVersion;
        mProjection = projection;
        mOrder = (order == null) ? DEFAULT_ORDER : order;
        mPrimaryKey = (primaryKey == null) ? ROW_ID : primaryKey;
    }

    @NonNls
    @NonNull
    @Override
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
    @Override
    public final Select.Projection getProjection() {
        return (mProjection == null) ? Select.Projection.All : mProjection;
    }

    @NonNull
    public final Select.Order getOrder() {
        return mOrder;
    }

    @NonNull
    public final Column<?> getPrimaryKey() {
        return mPrimaryKey;
    }

    @NonNull
    @Override
    public final Maybe<Map<String, Object>> read(@NonNull final Readable input) {
        final Set<Column<?>> columns = mColumns.get();
        final Map<String, Object> map = new HashMap<>(columns.size());

        for (final Column<?> column : columns) {
            final Maybe<?> value = column.read(input);
            if (value.isSomething()) {
                map.put(column.getName(), value.get());
            }
        }

        return (map.isEmpty()) ?
                Maybes.<Map<String, Object>>nothing() :
                something(map);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void write(@Value.Write.Operation final int operation,
                            @NonNull final Maybe<Map<String, Object>> values,
                            @NonNull final Writable output) {
        final Set<Column<?>> columns = mColumns.get();
        if (values.isSomething()) {
            final Map<String, Object> map = values.get();
            for (final Column<?> column : columns) {
                final String name = column.getName();
                final Maybe<Object> value = something(
                        ((map != null) && map.containsKey(name)) ?
                                map.get(name) :
                                null
                );
                ((Value.Write<Object>) column).write(operation, value, output);
            }
        } else {
            for (final Column<?> column : columns) {
                ((Value.Write<Object>) column).write(operation, nothing(), output);
            }
        }
    }

    @NonNull
    public final <V> Table with(@NonNull final Column<V> column) {
        return with(mVersion, column);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public final <V> Table with(final int version, @NonNull final Column<V> column) {
        if (version < mVersion) {
            throw new IllegalArgumentException("Version is less than table version " + mVersion);
        }

        final Select.Projection projection = (mProjection == null) ?
                column.getProjection() :
                mProjection.and(column.getProjection());
        final Column<?> primaryKey = column.isPrimaryKey() ? column : mPrimaryKey;
        final SparseArray<Set<Column<?>>> columnsByVersion = Legacy.clone(mColumnsByVersion);
        Set<Column<?>> atVersion = columnsByVersion.get(version);
        if (atVersion == null) {
            atVersion = new HashSet<>();
            columnsByVersion.put(version, atVersion);
        }
        atVersion.add(column);

        return new Table(mName,
                mVersion,
                columnsByVersion,
                projection,
                mOrder,
                primaryKey);
    }

    public final Table with(@NonNull final Select.Order order) {
        return new Table(mName,
                mVersion,
                mColumnsByVersion,
                mColumns,
                mColumnsAtVersion,
                mProjection,
                order,
                mPrimaryKey);
    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        boolean result = this == object;

        if (!result && (object != null) && (getClass() == object.getClass())) {
            final Table other = (Table) object;
            result = mName.equals(other.mName) && mColumns.get().equals(other.mColumns.get());
        }

        return result;
    }

    @Override
    public final int hashCode() {
        return mName.hashCode();
    }

    @NonNull
    public static Table table(@NonNls @NonNull final String name,
                              final int version) {
        return new Table(name, version);
    }

    @NonNull
    private static Lazy<Set<Column<?>>> columns(@NonNull final SparseArray<Set<Column<?>>> columnsByVersion) {
        return new Lazy.Volatile<Set<Column<?>>>() {
            @NonNull
            @Override
            protected Set<Column<?>> produce() {
                final Set<Column<?>> result = new HashSet<>();

                final int size = columnsByVersion.size();
                for (int i = 0; i < size; i++) {
                    result.addAll(columnsByVersion.valueAt(i));
                }

                return result;
            }
        };
    }

    @NonNull
    private static Lazy<Pair<Pair<Integer, Integer>, SparseArray<Set<Column<?>>>>> columnsAtVersion(@NonNull final SparseArray<Set<Column<?>>> columnsByVersion) {
        return new Lazy.Volatile<Pair<Pair<Integer, Integer>, SparseArray<Set<Column<?>>>>>() {
            @NonNull
            @Override
            protected Pair<Pair<Integer, Integer>, SparseArray<Set<Column<?>>>> produce() {
                final int size = columnsByVersion.size();
                final SparseArray<Set<Column<?>>> result = new SparseArray<>(size);
                int min = Integer.MAX_VALUE;
                int max = Integer.MIN_VALUE;

                for (int i = 0; i < size; i++) {
                    final int version = columnsByVersion.keyAt(i);
                    if (version < min) {
                        min = version;
                    }
                    if (max < version) {
                        max = version;
                    }
                }

                for (int i = min; i <= max; i++) {
                    final Set<Column<?>> columns = columnsByVersion.get(i);
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
        };
    }
}
