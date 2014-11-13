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

import android.orm.database.Table;
import android.orm.sql.Fragment;
import android.orm.sql.Value;
import android.orm.util.Lazy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.Set;

import static android.orm.sql.Helper.escape;

public class ForeignKey<V> implements Table.Constraint {

    @NonNull
    private final Value.Read<V> mChildKey;
    @NonNls
    @NonNull
    private final String mParentTable;
    @Nullable
    private final Value.Read<V> mParentKey;
    @Nullable
    private final Action mOnDelete;
    @Nullable
    private final Action mOnUpdate;
    @NonNls
    @NonNull
    private final Lazy<String> mSQL;

    private ForeignKey(@NonNull final Value.Read<V> childKey,
                       @NonNls @NonNull final String parentTable,
                       @Nullable final Value.Read<V> parentKey,
                       @Nullable final Action onDelete,
                       @Nullable final Action onUpdate) {
        super();

        mChildKey = childKey;
        mParentTable = parentTable;
        mParentKey = parentKey;
        mOnDelete = onDelete;
        mOnUpdate = onUpdate;

        mSQL = new SQL(childKey, parentTable, parentKey, onDelete, onUpdate);
    }

    @NonNull
    public final Value.Read<V> getChildKey() {
        return mChildKey;
    }

    @NonNls
    @NonNull
    public final String getParentTable() {
        return mParentTable;
    }

    @Nullable
    public final Value.Read<V> getParentKey() {
        return mParentKey;
    }

    @NonNull
    public final ForeignKey<V> onDelete(@NonNull final Action action) {
        return new ForeignKey<>(mChildKey, mParentTable, mParentKey, action, mOnUpdate);
    }

    @NonNull
    public final ForeignKey<V> onUpdate(@NonNull final Action action) {
        return new ForeignKey<>(mChildKey, mParentTable, mParentKey, mOnDelete, action);
    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        boolean result = this == object;

        if (!result && (object != null) && (getClass() == object.getClass())) {
            final ForeignKey<?> other = (ForeignKey<?>) object;
            result = mSQL.get().equals(other.mSQL.get());
        }

        return result;
    }

    @Override
    public final int hashCode() {
        return mSQL.get().hashCode();
    }

    @NonNls
    @NonNull
    @Override
    public final String toSQL() {
        return mSQL.get();
    }

    @NonNls
    @NonNull
    @Override
    public final String toString() {
        return mChildKey.getName() + " -> " + mParentTable +
                ((mParentKey == null) ? "" : ('(' + mParentKey.getName() + ')'));
    }

    @NonNull
    public static <V> Factory<V> from(@NonNull final Value.Read<V> childKey) {
        return new Factory<>(childKey);
    }

    public static class Factory<V> {

        @NonNull
        private final Value.Read<V> mChildKey;

        public Factory(@NonNull final Value.Read<V> childKey) {
            super();

            mChildKey = childKey;
        }

        @NonNull
        public final ForeignKey<V> to(@NonNls @NonNull final String table) {
            return new ForeignKey<>(mChildKey, table, null, null, null);
        }

        @NonNull
        public final ForeignKey<V> to(@NonNls @NonNull final String table,
                                      @NonNull final Value.Read<V> parentKey) {
            return new ForeignKey<>(mChildKey, table, parentKey, null, null);
        }
    }

    public enum Action implements Fragment {

        SetNull("set null"),
        SetDefault("set default"),
        Cascade("cascade"),
        Restrict("restrict"),
        NoAction("no action");

        @NonNls
        @NonNull
        private final String mSQL;

        Action(@NonNls @NonNull final String sql) {
            mSQL = sql;
        }

        @NonNls
        @NonNull
        @Override
        public final String toSQL() {
            return mSQL;
        }
    }

    private static class SQL extends Lazy.Volatile<String> {

        @NonNull
        private final Set<String> mChildKey;
        @NonNls
        @NonNull
        private final String mParentTable;
        @Nullable
        private final Set<String> mParentKey;
        @Nullable
        private final Action mOnDelete;
        @Nullable
        private final Action mOnUpdate;

        private <V> SQL(@NonNull final Value.Read<V> childKey,
                        @NonNull final String parentTable,
                        @Nullable final Value.Read<V> parentKey,
                        @Nullable final Action onDelete,
                        @Nullable final Action onUpdate) {
            super();

            mParentTable = escape(parentTable);
            mOnDelete = onDelete;
            mOnUpdate = onUpdate;

            mChildKey = childKey.getProjection().asMap().keySet();
            if (mChildKey.isEmpty()) {
                throw new IllegalArgumentException("Child key must reference something");
            }

            final Map<String, String> parentKeyMap = (parentKey == null) ? null : parentKey.getProjection().asMap();
            if ((parentKeyMap != null) && parentKeyMap.isEmpty()) {
                throw new IllegalArgumentException("Parent cannot reference nothing");
            }
            mParentKey = (parentKeyMap == null) ? null : parentKeyMap.keySet();
        }

        @NonNls
        @NonNull
        @Override
        protected final String produce() {
            return "foreign key (" + PrimaryKey.toSQL(mChildKey) +
                    ") references " + mParentTable +
                    ((mParentKey == null) ? "" : ('(' + PrimaryKey.toSQL(mParentKey) + ')')) +
                    ((mOnDelete == null) ? "" : (" on delete " + mOnDelete.toSQL())) +
                    ((mOnUpdate == null) ? "" : (" on update " + mOnUpdate.toSQL()));
        }
    }
}
