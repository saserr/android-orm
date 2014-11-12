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

package android.orm.sql.column;

import android.orm.sql.Fragment;
import android.orm.sql.Value;
import android.orm.sql.fragment.Constraint;
import android.orm.util.Lazy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.Set;

import static android.orm.sql.Helper.escape;

public class Reference<V> implements Constraint {

    @NonNls
    @NonNull
    private final String mTable;
    @Nullable
    private final Value.Read<V> mKey;
    @Nullable
    private final Action mOnDelete;
    @Nullable
    private final Action mOnUpdate;
    @NonNls
    @NonNull
    private final Lazy<String> mSQL;

    private Reference(@NonNls @NonNull final String table,
                      @Nullable final Value.Read<V> key,
                      @Nullable final Action onDelete,
                      @Nullable final Action onUpdate) {
        super();

        mTable = table;
        mKey = key;
        mOnDelete = onDelete;
        mOnUpdate = onUpdate;

        mSQL = new SQL(table, key, onDelete, onUpdate);
    }

    @NonNull
    public final Reference<V> onDelete(@NonNull final Action action) {
        return new Reference<>(mTable, mKey, action, mOnUpdate);
    }

    @NonNull
    public final Reference<V> onUpdate(@NonNull final Action action) {
        return new Reference<>(mTable, mKey, mOnDelete, action);
    }

    @NonNls
    @NonNull
    @Override
    public final String toSQL() {
        return mSQL.get();
    }

    @NonNull
    public static <V> Reference<V> to(@NonNls @NonNull final String table) {
        return new Reference<>(table, null, null, null);
    }

    @NonNull
    public static <V> Reference<V> to(@NonNls @NonNull final String table,
                                      @NonNull final Value.Read<V> key) {
        return new Reference<>(table, key, null, null);
    }

    @NonNls
    @NonNull
    @Override
    public final String toString() {
        return mTable + ((mKey == null) ? "" : ('(' + mKey.getName() + ')'));
    }

    @NonNls
    @NonNull
    public static String toSQL(@NonNull final Set<String> projection) {
        final StringBuilder result = new StringBuilder();

        for (final String column : projection) {
            result.append(escape(column)).append(", ");
        }
        final int length = result.length();
        result.delete(length - 2, length);

        return result.toString();
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
        private final String mTable;
        @Nullable
        private final Set<String> mKey;
        @Nullable
        private final Action mOnDelete;
        @Nullable
        private final Action mOnUpdate;

        private <V> SQL(@NonNull final String table,
                        @Nullable final Value.Read<V> key,
                        @Nullable final Action onDelete,
                        @Nullable final Action onUpdate) {
            super();

            mTable = table;
            mOnDelete = onDelete;
            mOnUpdate = onUpdate;

            final Map<String, String> keyMap = (key == null) ? null : key.getProjection().asMap();
            mKey = ((keyMap == null) || keyMap.isEmpty()) ?
                    null :
                    keyMap.keySet();
        }

        @NonNls
        @NonNull
        @Override
        protected final String produce() {
            return "references " + mTable +
                    ((mKey == null) ? "" : ('(' + toSQL(mKey) + ')')) +
                    ((mOnDelete == null) ? "" : (" on delete " + mOnDelete.toSQL())) +
                    ((mOnUpdate == null) ? "" : (" on update " + mOnUpdate.toSQL()));
        }
    }
}
