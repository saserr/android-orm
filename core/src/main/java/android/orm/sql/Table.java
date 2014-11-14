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

import android.orm.sql.table.Check;
import android.orm.sql.table.ForeignKey;
import android.orm.sql.table.PrimaryKey;
import android.orm.sql.table.UniqueKey;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static java.util.Collections.unmodifiableSet;

public class Table<K> extends Value.ReadWrite.Base<Map<String, Object>> {

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

        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Cannot create table without columns");
        }

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
    public final Maybe<Map<String, Object>> read(@NonNull final Readable input) {
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
            return new Table<>(mName, mColumns, mChecks, mForeignKeys, mUniqueKeys, null);
        }

        @NonNull
        public final <K> Table<K> build(@NonNull final PrimaryKey<K> primaryKey) {
            return new Table<>(mName, mColumns, mChecks, mForeignKeys, mUniqueKeys, primaryKey);
        }
    }
}
