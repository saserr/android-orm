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

package android.orm.sql.table;

import android.orm.sql.Fragment;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.sql.column.Reference;
import android.orm.sql.fragment.ConflictResolution;
import android.orm.sql.fragment.Constraint;
import android.orm.util.Lazy;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.Set;

public abstract class Uniqueness<V> extends Value.ReadWrite.Base<V> implements Constraint {

    @NonNull
    private final Value.ReadWrite<V> mValue;
    @NonNls
    @NonNull
    private final Lazy<String> mSQL;

    protected Uniqueness(@NonNull final Type type,
                         @NonNull final Value.ReadWrite<V> value,
                         @Nullable final ConflictResolution resolution) {
        super();

        mValue = value;
        mSQL = new SQL(type, mValue, resolution);
    }

    @NonNull
    public abstract Uniqueness<V> onConflict(@NonNull final ConflictResolution resolution);

    @NonNls
    @NonNull
    @Override
    public final String getName() {
        return mValue.getName();
    }

    @NonNull
    @Override
    public final Select.Projection getProjection() {
        return mValue.getProjection();
    }

    @NonNull
    @Override
    public final Maybe<V> read(@NonNull final android.orm.sql.Readable input) {
        return mValue.read(input);
    }

    @Override
    public final void write(@NonNull final Value.Write.Operation operation,
                            @NonNull final Maybe<V> value,
                            @NonNull final Writable output) {
        mValue.write(operation, value, output);
    }

    @NonNls
    @NonNull
    @Override
    public final String toSQL() {
        return mSQL.get();
    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        boolean result = this == object;

        if (!result && (object != null) && (getClass() == object.getClass())) {
            final Uniqueness<?> other = (Uniqueness<?>) object;
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
    public final String toString() {
        return mValue.getName();
    }

    private static class SQL extends Lazy.Volatile<String> {

        @NonNull
        private final Type mType;
        @NonNull
        private final Set<String> mKey;
        @Nullable
        private final ConflictResolution mResolution;

        private SQL(@NonNull final Type type,
                    @NonNull final Value.Read<?> value,
                    @Nullable final ConflictResolution resolution) {
            super();

            mType = type;
            mResolution = resolution;

            mKey = value.getProjection().asMap().keySet();
            if (mKey.isEmpty()) {
                throw new IllegalArgumentException("Key must reference something");
            }
        }

        @NonNls
        @NonNull
        @Override
        protected final String produce() {
            return mType.toSQL() + " (" + Reference.toSQL(mKey) + ')' +
                    ((mResolution == null) ? "" : (" on conflict " + mResolution.toSQL()));
        }
    }

    public enum Type implements Fragment {

        PrimaryKey("primary key"),
        UniqueKey("unique");

        @NonNls
        @NonNull
        private final String mSQL;

        Type(@NonNls @NonNull final String sql) {
            mSQL = sql;
        }

        @NonNls
        @NonNull
        @Override
        public final String toSQL() {
            return mSQL;
        }
    }
}
