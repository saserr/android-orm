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

import android.database.SQLException;
import android.orm.sql.column.ConflictResolution;
import android.orm.sql.column.Default;
import android.orm.sql.column.NotNull;
import android.orm.sql.column.Reference;
import android.orm.sql.column.Unique;
import android.orm.sql.column.Validation;
import android.orm.sql.column.Validations;
import android.orm.sql.fragment.Constraint;
import android.orm.util.Lazy;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.column.Validations.compose;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.orm.util.Validations.IsNotNull;

public class Column<V> extends Value.ReadWrite.Base<V> implements Fragment {

    private static final Unique UNIQUE = new Unique();
    private static final NotNull NOT_NULL = new NotNull();

    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Type<V> mType;
    @Nullable
    private final NotNull mNotNull;
    @Nullable
    private final Unique mUnique;
    @Nullable
    private final Default<V> mDefault;
    @Nullable
    private final Reference<V> mReference;
    @Nullable
    private final Validation<V> mValidation;
    @NonNull
    private final Select.Projection mProjection;
    @NonNull
    private final Lazy<String> mSQL;
    private final boolean mNullable;

    private Column(@NonNls @NonNull final String name,
                   @NonNull final Type<V> type,
                   @Nullable final NotNull notNull,
                   @Nullable final Unique unique,
                   @Nullable final Default<V> defaultValue,
                   @Nullable final Reference<V> reference,
                   @Nullable final Validation<V> validation) {
        super();

        mName = name;
        mType = type;
        mNotNull = notNull;
        mUnique = unique;
        mDefault = defaultValue;
        mReference = reference;
        mValidation = validation;

        mProjection = Select.projection(name, null);
        mSQL = sql(name, type, notNull, unique, defaultValue, reference);
        mNullable = mNotNull == null;
    }

    @NonNls
    @NonNull
    @Override
    public final String getName() {
        return mName;
    }

    @NonNull
    public final Type<V> getType() {
        return mType;
    }

    @NonNull
    @Override
    public final Select.Projection getProjection() {
        return mProjection;
    }

    public final boolean isNullable() {
        return mNullable;
    }

    public final boolean isUnique() {
        return mUnique != null;
    }

    @NonNull
    public final Value.Read<V> as(@NonNls @NonNull final String name) {
        return Columns.readAs(name, this);
    }

    @NonNull
    @Override
    public final Maybe<V> read(@NonNull final Readable input) {
        final Maybe<V> result = mType.read(input, mName);

        if (mValidation != null) {
            mValidation.afterRead(mName, result);
        }

        return result;
    }

    @Override
    public final void write(@NonNull final Value.Write.Operation operation,
                            @NonNull final Maybe<V> value,
                            @NonNull final Writable output) {
        Maybe<V> result = value;
        if ((operation == Insert) && value.isNothing()) {
            if (mDefault == null) {
                if (!mNullable) {
                    throw new SQLException("Required column " + mName + " is missing");
                }

                result = nothing();
            } else {
                result = something(mDefault.get());
            }
        }

        if (mValidation != null) {
            mValidation.beforeWrite(operation, mName, result);
        }

        if (result.isSomething()) {
            final V v = result.get();
            if (v == null) {
                output.putNull(mName);
            } else {
                mType.write(output, mName, v);
            }
        }
    }

    @NonNull
    public final Column<V> asNotNull() {
        final Validation<V> isNotNull = Validations.convert(IsNotNull.name(mName));
        final Validation<V> validation = (mValidation == null) ?
                isNotNull :
                compose(mValidation, isNotNull);
        return new Column<>(mName, mType, NOT_NULL, mUnique, mDefault, mReference, validation);
    }

    @NonNull
    public final Column<V> asNotNull(@NonNull final ConflictResolution onConflict) {
        final Validation<V> isNotNull = Validations.convert(IsNotNull.name(mName));
        final Validation<V> validation = (mValidation == null) ?
                isNotNull :
                compose(mValidation, isNotNull);
        final NotNull notNull = new NotNull(onConflict);
        return new Column<>(mName, mType, notNull, mUnique, mDefault, mReference, validation);
    }

    @NonNull
    public final Column<V> asUnique() {
        return new Column<>(mName, mType, mNotNull, UNIQUE, mDefault, mReference, mValidation);
    }

    @NonNull
    public final Column<V> asUnique(@NonNull final ConflictResolution onConflict) {
        final Unique unique = new Unique(onConflict);
        return new Column<>(mName, mType, mNotNull, unique, mDefault, mReference, mValidation);
    }

    @NonNull
    public final Column<V> withDefault(@Nullable final V value) {
        final Default<V> defaultValue = new Default<>(mType, value);
        return new Column<>(mName, mType, mNotNull, mUnique, defaultValue, mReference, mValidation);
    }

    @NonNull
    public final Column<V> withDefault(@Nullable final Producer<V> producer) {
        final Default<V> defaultValue = new Default<>(producer);
        return new Column<>(mName, mType, mNotNull, mUnique, defaultValue, mReference, mValidation);
    }

    @NonNull
    public final Column<V> references(@NonNull final Reference<V> reference) {
        return new Column<>(mName, mType, mNotNull, mUnique, mDefault, reference, mValidation);
    }

    @NonNull
    public final Column<V> as(@NonNull final Validation<? super V> validation) {
        return new Column<>(mName, mType, mNotNull, mUnique, mDefault, mReference,
                (mValidation == null) ?
                        Validations.safeCast(validation) :
                        compose(mValidation, validation));
    }

    @NonNull
    public final Column<V> check(@NonNull final android.orm.util.Validation<? super V> validation) {
        return as(Validations.convert(validation.name(mName)));
    }

    @NonNls
    @NonNull
    @Override
    public final String toSQL() {
        return mSQL.get();
    }

    @NonNull
    public final V fromString(@NonNls @NonNull final String value) {
        return mType.fromString(value);
    }

    @NonNls
    @NonNull
    public final String toString(@NonNull final V value) {
        return mType.toString(value);
    }

    @NonNls
    @NonNull
    public final String escape(@NonNull final V value) {
        return mType.escape(value);
    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        boolean result = this == object;

        if (!result && (object != null) && (getClass() == object.getClass())) {
            final Column<?> other = (Column<?>) object;
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
        return mName;
    }

    @NonNull
    public static <V> Column<V> column(@NonNls @NonNull final String name,
                                       @NonNull final Type<V> type) {
        return new Column<>(name, type, null, null, null, null, null);
    }

    @NonNull
    private static <V> Lazy<String> sql(@NonNls @NonNull final String name,
                                        @NonNull final Type<V> type,
                                        @NonNull final Constraint... constraints) {
        return new Lazy.Volatile<String>() {
            @NonNls
            @NonNull
            @Override
            protected String produce() {
                @NonNls final StringBuilder result = new StringBuilder();

                result.append(Helper.escape(name))
                        .append(' ')
                        .append(type.toSQL());

                for (final Constraint constraint : constraints) {
                    if (constraint != null) {
                        final String sql = constraint.toSQL();
                        if (sql != null) {
                            result.append(' ').append(sql);
                        }
                    }
                }

                return result.toString();
            }
        };
    }
}
