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

import android.orm.sql.fragment.ConflictResolution;
import android.orm.sql.fragment.Order;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.Set;

import static android.orm.sql.Helper.escape;

public class PrimaryKey<V> extends Value.ReadWrite.Base<V> implements Fragment {

    @NonNull
    private final Value.ReadWrite<V> mValue;
    @NonNls
    @NonNull
    private final String mSQL;

    private PrimaryKey(@NonNull final Value.ReadWrite<V> value,
                       @Nullable final Order.Type order,
                       @Nullable final ConflictResolution resolution) {
        super();

        final Map<String, String> projection = value.getProjection().asMap();
        if (projection.isEmpty()) {
            throw new IllegalArgumentException("Value must reference something");
        }

        mValue = value;
        mSQL = "primary key (" + toSQL(projection.keySet()) +
                ((order == null) ? "" : order.toSQL()) + ')' +
                ((resolution == null) ? "" : (" on conflict " + resolution.toSQL()));
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
        return mSQL;
    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        boolean result = this == object;

        if (!result && (object != null) && (getClass() == object.getClass())) {
            final PrimaryKey<?> other = (PrimaryKey<?>) object;
            result = mSQL.equals(other.mSQL);
        }

        return result;
    }

    @Override
    public final int hashCode() {
        return mSQL.hashCode();
    }

    @NonNls
    @NonNull
    @Override
    public final String toString() {
        return mValue.getName();
    }

    @NonNull
    public static <V> PrimaryKey<V> primaryKey(@NonNull final Value.ReadWrite<V> value) {
        return new PrimaryKey<>(value, null, null);
    }

    @NonNull
    public static <V> PrimaryKey<V> primaryKey(@NonNull final Value.ReadWrite<V> value,
                                               @NonNull final ConflictResolution resolution) {
        return new PrimaryKey<>(value, null, resolution);
    }

    @NonNls
    @NonNull
    /* package */ static String toSQL(@NonNull final Set<String> projection) {
        final StringBuilder result = new StringBuilder();

        for (final String column : projection) {
            result.append(escape(column)).append(", ");
        }
        final int length = result.length();
        result.delete(length - 2, length);

        return result.toString();
    }
}
