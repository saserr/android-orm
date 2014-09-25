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
import static android.orm.sql.Types.Integer;

public class PrimaryKey<V> extends Value.ReadWrite.Base<V> implements Fragment {

    private final boolean mIsAlias;
    @NonNull
    private final Value.ReadWrite<V> mValue;
    @NonNls
    @NonNull
    private final String mSQL;

    private PrimaryKey(@NonNull final Column<V> column,
                       @Nullable final Order.Type order,
                       @Nullable final ConflictResolution resolution) {
        this(Integer.equals(column.getType()), column, order, resolution);
    }

    private PrimaryKey(@NonNull final Value.ReadWrite<V> value,
                       @Nullable final Order.Type order,
                       @Nullable final ConflictResolution resolution) {
        this(false, value, order, resolution);
    }

    private PrimaryKey(final boolean isAlias,
                       @NonNull final Value.ReadWrite<V> value,
                       @Nullable final Order.Type order,
                       @Nullable final ConflictResolution resolution) {
        super();

        final Map<String, String> projection = value.getProjection().asMap();
        if (projection.isEmpty()) {
            throw new IllegalArgumentException("Value must reference something");
        }

        mIsAlias = isAlias;
        mValue = value;
        mSQL = "primary key (" + toSQL(projection.keySet()) +
                ((order == null) ? "" : order.toSQL()) + ')' +
                ((resolution == null) ? "" : (" on conflict " + resolution.toSQL()));
    }

    public final boolean isAliasForRowId() {
        return mIsAlias;
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

    @NonNull
    public static <V> PrimaryKey<V> primaryKey(@NonNull final Value.ReadWrite<V> value) {
        return new PrimaryKey<>(value, null, null);
    }

    @NonNull
    public static <V> PrimaryKey<V> primaryKey(@NonNull final Value.ReadWrite<V> value,
                                               @NonNull final ConflictResolution resolution) {
        return new PrimaryKey<>(value, null, resolution);
    }

    @NonNull
    public static <V> PrimaryKey<V> primaryKey(@NonNull final Column<V> column) {
        return new PrimaryKey<>(column, null, null);
    }

    @NonNull
    public static <V> PrimaryKey<V> primaryKey(@NonNull final Column<V> column,
                                               @NonNull final ConflictResolution resolution) {
        return new PrimaryKey<>(column, null, resolution);
    }

    @NonNull
    public static PrimaryKey<Long> primaryKey(@NonNull final Column<Long> column,
                                              @NonNull final Order.Type order) {
        return new PrimaryKey<>(column, order, null);
    }

    @NonNull
    public static PrimaryKey<Long> primaryKey(@NonNull final Column<Long> column,
                                              @NonNull final Order.Type order,
                                              @NonNull final ConflictResolution resolution) {
        return new PrimaryKey<>(column, order, resolution);
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
