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
import android.orm.sql.Value;
import android.orm.sql.column.Reference;
import android.orm.sql.fragment.Constraint;
import android.orm.util.Lazy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.Set;

public class ForeignKey<V> implements Constraint {

    @NonNull
    private final Value.Read<V> mChildKey;
    @NonNls
    @NonNull
    private final Reference<V> mReference;
    @NonNls
    @NonNull
    private final Lazy<String> mSQL;

    protected ForeignKey(@NonNull final Value.Read<V> childKey,
                         @NonNull final Reference<V> reference) {
        super();

        mChildKey = childKey;
        mReference = reference;

        mSQL = new SQL(childKey, mReference);
    }

    @NonNull
    public final ForeignKey<V> onDelete(@NonNull final Reference.Action action) {
        return new ForeignKey<>(mChildKey, mReference.onDelete(action));
    }

    @NonNull
    public final ForeignKey<V> onUpdate(@NonNull final Reference.Action action) {
        return new ForeignKey<>(mChildKey, mReference.onUpdate(action));
    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        boolean result = this == object;

        if (!result && (object != null) && (getClass() == object.getClass())) {
            final ForeignKey<?> other = (ForeignKey<?>) object;
            result = mSQL.get().equals(other.toSQL());
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
        return mChildKey.getName() + " -> " + mReference;
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
        public final ForeignKey<V> to(@NonNls @NonNull final Table<V> table) {
            final String parent = table.getName();
            final PrimaryKey<V> parentKey = table.getPrimaryKey();
            final Reference<V> reference = (parentKey == null) ?
                    Reference.<V>to(parent) :
                    Reference.to(parent, parentKey);

            return new ForeignKey<>(mChildKey, reference);
        }

        @NonNull
        public final ForeignKey<V> to(@NonNls @NonNull final String table) {
            return new ForeignKey<>(mChildKey, Reference.<V>to(table));
        }

        @NonNull
        public final ForeignKey<V> to(@NonNls @NonNull final String table,
                                      @NonNull final Value.Read<V> key) {
            return new ForeignKey<>(mChildKey, Reference.to(table, key));
        }
    }

    private static class SQL extends Lazy.Volatile<String> {

        @NonNull
        private final Set<String> mChildKey;
        @NonNls
        @NonNull
        private final Reference<?> mReference;

        private <V> SQL(@NonNull final Value.Read<V> childKey,
                        @NonNull final Reference<V> reference) {
            super();

            mChildKey = childKey.getProjection().asMap().keySet();
            if (mChildKey.isEmpty()) {
                throw new IllegalArgumentException("Child key must reference something");
            }

            mReference = reference;
        }

        @NonNls
        @NonNull
        @Override
        protected final String produce() {
            return "foreign key (" + Reference.toSQL(mChildKey) + ") " + mReference.toSQL();
        }
    }
}
