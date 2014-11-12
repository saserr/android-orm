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

import android.orm.sql.Readable;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.sql.column.ConflictResolution;
import android.orm.sql.column.Reference;
import android.orm.sql.fragment.Constraint;
import android.orm.util.Lazy;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.Set;

public class PrimaryKey<V> extends Value.ReadWrite.Base<V> implements Constraint {

    @NonNull
    private final Value.ReadWrite<V> mValue;
    @NonNls
    @NonNull
    private final Lazy<String> mSQL;

    private PrimaryKey(@NonNull final Value.ReadWrite<V> value,
                       @Nullable final ConflictResolution resolution) {
        super();

        final Map<String, String> projection = value.getProjection().asMap();
        if (projection.isEmpty()) {
            throw new IllegalArgumentException("Value must reference something");
        }

        mValue = value;
        mSQL = new SQL(mValue, resolution);
    }

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
    public final PrimaryKey<V> onConflict(@NonNull final ConflictResolution resolution) {
        return new PrimaryKey<>(mValue, resolution);
    }

    @NonNull
    @Override
    public final Maybe<V> read(@NonNull final Readable input) {
        return mValue.read(input);
    }

    @Override
    public final void write(@NonNull final Operation operation,
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
            final PrimaryKey<?> other = (PrimaryKey<?>) object;
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

    @NonNull
    public static <V> PrimaryKey<V> on(@NonNull final Value.ReadWrite<V> value) {
        return new PrimaryKey<>(value, null);
    }

    private static class SQL extends Lazy.Volatile<String> {

        @NonNull
        private final Set<String> mKey;
        @Nullable
        private final ConflictResolution mResolution;

        private SQL(@NonNull final Value.Read<?> value,
                    @Nullable final ConflictResolution resolution) {
            super();

            mKey = value.getProjection().asMap().keySet();
            if (mKey.isEmpty()) {
                throw new IllegalArgumentException("Key must reference something");
            }

            mResolution = resolution;
        }

        @NonNls
        @NonNull
        @Override
        protected final String produce() {
            return "primary key (" + Reference.toSQL(mKey) + ')' +
                    ((mResolution == null) ? "" : (" on conflict " + mResolution.toSQL()));
        }
    }
}
